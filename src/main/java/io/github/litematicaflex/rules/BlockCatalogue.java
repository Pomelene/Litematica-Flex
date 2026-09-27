package io.github.litematicaflex.rules;

import com.google.gson.Gson;
import io.github.litematicaflex.api.BlockDescription;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Explicit vanilla membership plus conservative runtime shape classification for modded blocks. */
public final class BlockCatalogue {
    public record Group(String id, String title, String category, List<String> members) {}
    private record Document(int version, String minecraft, List<Group> groups) {}
    private final List<Group> groups;
    private final Map<String, Set<String>> memberships;
    private final ConcurrentHashMap<BlockState, BlockDescription> descriptions = new ConcurrentHashMap<>();
    private static final Set<String> FUNCTIONAL = Set.of("piston", "sticky_piston", "observer", "dispenser", "dropper",
            "hopper", "redstone_block", "redstone_lamp", "slime_block", "honey_block", "tnt", "target",
            "note_block", "soul_sand", "soul_soil", "magma_block", "potent_sulfur", "budding_amethyst",
            "creaking_heart", "sculk_sensor", "calibrated_sculk_sensor", "sculk_shrieker", "sculk_catalyst",
            "sponge", "wet_sponge", "ice", "packed_ice", "blue_ice", "frosted_ice", "tinted_glass");

    public BlockCatalogue() {
        try (InputStream stream = BlockCatalogue.class.getResourceAsStream("/assets/litematica_flex/catalogue.json")) {
            if (stream == null) throw new IllegalStateException("Missing built-in catalogue");
            groups = List.copyOf(new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Document.class).groups());
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
        Map<String, Set<String>> map = new HashMap<>();
        for (Group group : groups) for (String member : group.members()) {
            map.computeIfAbsent(member, ignored -> new HashSet<>()).add(group.id());
        }
        Map<String, Set<String>> frozen = new HashMap<>();
        map.forEach((key,value) -> frozen.put(key, Set.copyOf(value)));
        memberships = Map.copyOf(frozen);
    }

    public List<Group> groups() { return groups; }
    public BlockDescription describe(BlockState state) { return descriptions.computeIfAbsent(state, this::create); }

    private BlockDescription create(BlockState state) {
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String id = key.toString();
        Set<String> names = new HashSet<>(memberships.getOrDefault(id, Set.of()));
        String shape = shape(state);
        if (!shape.startsWith("other:")) names.add("shape." + shape);
        Map<String,String> properties = new HashMap<>();
        state.getValues().forEach(value -> properties.put(value.property().getName(), value.valueName()));
        boolean functional = state.hasBlockEntity() || FUNCTIONAL.contains(key.getPath())
                || key.getPath().startsWith("infested_") || key.getPath().contains("copper_bulb");
        return new BlockDescription(id, shape, names, properties,
                state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), state.isAir(), state.liquid(), functional);
    }

    private String shape(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof StairBlock) return "stairs";
        if (block instanceof SlabBlock) return "slab";
        if (block instanceof WallBlock) return "wall";
        if (block instanceof FenceGateBlock) return "fence_gate";
        if (block instanceof FenceBlock) return "fence";
        if (block instanceof TrapDoorBlock) return "trapdoor";
        if (block instanceof DoorBlock) return "door";
        if (block instanceof IronBarsBlock) return "pane";
        if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) return "full";
        // Classes keep unrelated thin shapes apart, even if the material name is similar.
        return "other:" + block.getClass().getName();
    }
}
