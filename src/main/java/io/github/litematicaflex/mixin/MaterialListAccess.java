package io.github.litematicaflex.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Set;

@Mixin(value=MaterialListBase.class,remap=false)
public interface MaterialListAccess {
    @Accessor("ignored") Set<MaterialListEntry> flex$ignoredEntries();
}
