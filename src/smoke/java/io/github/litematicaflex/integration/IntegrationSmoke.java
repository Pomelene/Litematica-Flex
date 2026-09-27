package io.github.litematicaflex.integration;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Opt-in development check: transform every integration target before starting any graphics subsystem. */
public final class IntegrationSmoke implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        if (!Boolean.getBoolean("litematica.flex.smoke")) return;
        String[] targets = {
            "fi.dy.masa.litematica.schematic.verifier.SchematicVerifier",
            "fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo",
            "fi.dy.masa.litematica.util.EasyPlaceUtils",
            "fi.dy.masa.litematica.util.WorldUtils",
            "fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement",
            "fi.dy.masa.litematica.materials.MaterialListUtils"
        };
        try {
            for (String target : targets) {
                Class.forName(target,false,Thread.currentThread().getContextClassLoader());
                System.out.println("Flex integration transformed: " + target);
            }
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            var catalogue=new io.github.litematicaflex.rules.BlockCatalogue();
            int states=0;
            for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                for(var state:block.getStateDefinition().getPossibleStates()) { catalogue.describe(state);states++; }
            }
            System.out.println("Flex runtime catalogue described "+states+" vanilla block states.");
            io.github.litematicaflex.runtime.RenderMatchContext.position(net.minecraft.core.BlockPos.ZERO);
            io.github.litematicaflex.runtime.RenderMatchContext.overlay(io.github.litematicaflex.rules.MatchResult.substitute("shape.full"));
            if(io.github.litematicaflex.runtime.RenderMatchContext.overlay()==null)throw new IllegalStateException("Missing render context");
            io.github.litematicaflex.runtime.RenderMatchContext.clear();
            RendererRegression.run();
            PlacementRegression.run();
            CatalogueAudit.run();
            System.out.println("Flex integration smoke passed (no game window opened).");
            System.exit(0);
        } catch (Throwable e) {
            // Headless test failures must not open Fabric's native error dialog.
            e.printStackTrace();System.exit(1);
        }
    }
}
