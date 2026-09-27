package io.github.litematicaflex.runtime;

import io.github.litematicaflex.rules.MatchResult;
import net.minecraft.core.BlockPos;

/** Thread-local render context lives outside Mixins, so it never depends on target constructors. */
public final class RenderMatchContext {
    private static final ThreadLocal<BlockPos> POSITION=new ThreadLocal<>();
    private static final ThreadLocal<MatchResult> OVERLAY=new ThreadLocal<>();
    private RenderMatchContext() {}
    public static void position(BlockPos pos){POSITION.set(pos.immutable());}
    public static BlockPos position(){return POSITION.get();}
    public static void overlay(MatchResult result){if(result==null)OVERLAY.remove();else OVERLAY.set(result);}
    public static MatchResult overlay(){return OVERLAY.get();}
    public static void clear(){POSITION.remove();OVERLAY.remove();}
}
