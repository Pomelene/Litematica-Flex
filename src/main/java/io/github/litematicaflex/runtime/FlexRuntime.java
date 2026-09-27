package io.github.litematicaflex.runtime;

import io.github.litematicaflex.config.*;
import io.github.litematicaflex.rules.*;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.*;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class FlexRuntime {
    public enum Channel { VERIFICATION, RENDERING, PLACEMENT, MATERIALS }
    public static final ConfigurationStore STORE = new ConfigurationStore();
    public static final BlockCatalogue CATALOGUE = new BlockCatalogue();
    private static final RuleEngine ENGINE = new RuleEngine();
    private static volatile ProfileSnapshot snapshot = new ProfileSnapshot(new RuleProfile().copy(), Map.of(), List.of(), List.of());

    private FlexRuntime() {}

    public static String placementKey(SchematicPlacement placement) {
        return String.valueOf(placement.getSchematicFile()) + "|" + placement.getName() + "|" + placement.getOrigin().toShortString();
    }

    public static RuleProfile profile(BlockPos position, String placement) { return snapshot.resolve(position, placement); }

    public static String profileSource(BlockPos position,String placement) {
        var resolved=snapshot.resolve(position,placement);
        if(resolved==snapshot.global())return "全局";
        return snapshot.regions().stream().anyMatch(r -> r.profile()==resolved)?"区域独立":"投影独立";
    }

    public static MatchResult match(BlockState expected, BlockState actual, BlockPos position, String placement, Channel channel) {
        if (expected == actual) return MatchResult.EXACT;
        RuleProfile profile = profile(position, placement);
        if (!active(profile, channel)) return MatchResult.REJECTED;
        return matchWithProfile(expected,actual,profile,channel);
    }

    public static MatchResult matchWithProfile(BlockState expected, BlockState actual, RuleProfile profile, Channel channel) {
        if (expected == actual) return MatchResult.EXACT;
        if (!active(profile, channel)) return MatchResult.REJECTED;
        if (excluded(expected,actual,profile)) return MatchResult.REJECTED;
        var result = ENGINE.compare(CATALOGUE.describe(expected), CATALOGUE.describe(actual), profile);
        if (result.accepted() && profile.temporaryTargets.contains(CATALOGUE.describe(actual).id())) return MatchResult.substitute("temporary");
        return result;
    }

    public static boolean materialMatches(BlockState expected,BlockState actual,BlockPos pos,String placement,Channel channel) {
        var profile=profile(pos,placement);
        return active(profile,channel) && !excluded(expected,actual,profile) && ENGINE.materialsMatch(CATALOGUE.describe(expected),CATALOGUE.describe(actual),profile);
    }

    public static boolean excluded(BlockState expected,BlockState actual,RuleProfile profile) {
        return globallyExcluded(expected,actual) || BlacklistPresets.excludes(profile,CATALOGUE.describe(expected).id())
            || BlacklistPresets.excludes(profile,CATALOGUE.describe(actual).id());
    }

    private static boolean globallyExcluded(BlockState expected,BlockState actual) {
        return BlacklistPresets.excludes(snapshot.global(),CATALOGUE.describe(expected).id())
            || BlacklistPresets.excludes(snapshot.global(),CATALOGUE.describe(actual).id());
    }

    public static boolean active(RuleProfile p, Channel channel) {
        return snapshot.global().enabled && !snapshot.global().strictReview && p.enabled && !p.strictReview && switch (channel) {
            case VERIFICATION -> p.verification;
            case RENDERING -> p.rendering;
            case PLACEMENT -> p.placement;
            case MATERIALS -> p.materialList;
        };
    }

    /** Call on the client thread after a configuration/placement change. */
    public static void publish() {
        FlexConfiguration config = STORE.editable();
        Map<String,RuleProfile> profiles = new LinkedHashMap<>();
        config.placements.forEach((key,value) -> profiles.put(key, effective(value)));
        List<ProfileSnapshot.Bounds> bounds = new ArrayList<>();
        var manager = DataManager.getSchematicPlacementManager();
        List<SchematicPlacement> placements = new ArrayList<>(manager.getAllSchematicsPlacements());
        var selected = manager.getSelectedSchematicPlacement();
        if (selected != null) { placements.remove(selected); placements.addFirst(selected); }
        for (var placement : placements) {
            if (!placement.isEnabled()) continue;
            for (var box : placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values()) {
                BlockPos a = box.getPos1(), b = box.getPos2();
                if (a == null || b == null) continue;
                bounds.add(new ProfileSnapshot.Bounds(placementKey(placement), Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),
                        Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ())));
            }
        }
        List<ProfileSnapshot.Region> regions = new ArrayList<>();
        for (var r : config.regions) regions.add(new ProfileSnapshot.Region(new ProfileSnapshot.Bounds(r.placement,r.minX,r.minY,r.minZ,r.maxX,r.maxY,r.maxZ),effective(r.profile)));
        snapshot = new ProfileSnapshot(effective(config.global),Map.copyOf(profiles),List.copyOf(bounds),List.copyOf(regions));
    }

    private static RuleProfile effective(RuleProfile original) {
        RuleProfile p = original.copy();
        normalizeGroups(p);
        if (p.enabledGroups.contains("copper.oxidation") && p.enabledGroups.contains("copper.wax")) {
            Set<String> groups = new HashSet<>(p.enabledGroups); groups.add("copper.combined"); p.enabledGroups = Set.copyOf(groups);
        }
        return p.copy();
    }

    /** Expand old broad material switches into independent, visible kind rules. */
    public static void normalizeGroups(RuleProfile p) {
        var ids=new LinkedHashSet<>(p.enabledGroups);
        if(ids.remove("wood.all") || ids.remove("wood")) {
            CATALOGUE.groups().stream().filter(g -> g.category().equals("木材") && !g.id().equals("wood.all")).forEach(g -> ids.add(g.id()));
            ids.remove("wood");ids.remove("wood.all");
        }
        if(ids.remove("stone.all") || ids.remove("stone")) {
            for(String shape:List.of("full","stairs","slab","wall"))ids.add("stone.shape."+shape);
            ids.remove("stone");ids.remove("stone.all");
        }
        p.enabledGroups=ids;
    }

    public static void changed() {
        STORE.save(); publish();
        SchematicWorldRefresher.INSTANCE.updateAll();
        var mc = Minecraft.getInstance();
        if(mc.level!=null && SchematicWorldHandler.getSchematicWorld()!=null) {
            // Dispose cached meshes and pending compilation so old normalized reads cannot survive a rule change.
            fi.dy.masa.litematica.render.LitematicaRenderer.getInstance().loadRenderers(null);
            fi.dy.masa.litematica.materials.MaterialCache.getInstance().clearCache();
        }
        for (var placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            var verifier = placement.getSchematicVerifier();
            if ((verifier.isActive() || verifier.isFinished() || verifier.isPaused()) && mc.level != null && SchematicWorldHandler.getSchematicWorld() != null) {
                fi.dy.masa.litematica.scheduler.TaskScheduler.getInstanceClient().removeTask(verifier);
                verifier.startVerification(mc.level, SchematicWorldHandler.getSchematicWorld(), placement, null);
            }
            // Material caches must be recomputed by the user after mappings change.
        }
    }
}
