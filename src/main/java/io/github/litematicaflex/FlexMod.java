package io.github.litematicaflex;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.*;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import fi.dy.masa.litematica.data.DataManager;
import io.github.litematicaflex.api.*;
import io.github.litematicaflex.gui.FlexConfigScreen;
import io.github.litematicaflex.gui.FlexText;
import io.github.litematicaflex.runtime.FlexRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class FlexMod implements ModInitializer, IKeybindProvider {
    public static final ConfigHotkey OPEN=new ConfigHotkey("打开 Flex 设置","LEFT_ALT,F");
    public static final ConfigHotkey STRICT=new ConfigHotkey("切换严格复核","LEFT_ALT,R");
    public static final ConfigHotkey TOGGLE=new ConfigHotkey("切换 Flex 总开关","");
    public static final ConfigHotkey SUMMARY=new ConfigHotkey("显示校验统计","");
    private static final List<ConfigHotkey> HOTKEYS=List.of(OPEN,STRICT,TOGGLE,SUMMARY);

    /** Apply reloaded keys as well as defaults for keys omitted from an imported configuration. */
    public static void reloadHotkeys() {
        for(var hotkey:HOTKEYS) {
            String fallback=hotkey==OPEN?"LEFT_ALT,F":hotkey==STRICT?"LEFT_ALT,R":"";
            hotkey.setHotkeyStringValue(FlexRuntime.STORE.editable().hotkeys.getOrDefault(hotkey.getName(),fallback));
        }
        InputEventHandler.getKeybindManager().updateUsedKeys();
    }

    @Override public void onInitialize() {
        FabricLoader.getInstance().getEntrypoints("litematica-flex",FlexExtension.class).forEach(extension -> extension.register(FlexApi.INSTANCE));
        InitializationHandler.getInstance().registerInitializationHandler(() -> {
            FlexRuntime.STORE.load();
            FlexRuntime.clearTemporaryReview();
            FlexRuntime.publish();
            for(var hotkey:HOTKEYS) {
                var saved=FlexRuntime.STORE.editable().hotkeys.get(hotkey.getName());
                if(saved!=null)hotkey.setHotkeyStringValue(saved);
                hotkey.setValueChangeCallback(c -> FlexRuntime.STORE.editable().hotkeys.put(c.getName(),c.getHotkeyStringValue()));
            }
            Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo("litematica_flex","Litematica Flex Alpha ver.",FlexConfigScreen::new));
            RenderEventHandler.getInstance().registerInGameGuiRenderer(new io.github.litematicaflex.gui.FlexHud());
            InputEventHandler.getKeybindManager().registerKeybindProvider(this);
            OPEN.getKeybind().setCallback((action,key) -> { GuiBase.openGui(new FlexConfigScreen()); return true; });
            STRICT.getKeybind().setCallback((action,key) -> {
                var profile=FlexRuntime.STORE.editable().global; profile.strictReview=!profile.strictReview; FlexRuntime.changed();
                message("Flex 严格复核："+(profile.strictReview?"开启":"关闭")); return true;
            });
            TOGGLE.getKeybind().setCallback((action,key) -> {
                var profile=FlexRuntime.STORE.editable().global; profile.enabled=!profile.enabled; FlexRuntime.changed(); return true;
            });
            SUMMARY.getKeybind().setCallback((action,key) -> { showSummary(); return true; });
            TickHandler.getInstance().registerClientTickHandler(new fi.dy.masa.malilib.interfaces.IClientTickHandler() {
                private int ticks;
                @Override public void onClientTick(Minecraft client) {
                    if (++ticks % 20 == 0) FlexRuntime.publish();
                }
            });
        });
    }
    @Override public void addKeysToMap(IKeybindManager manager) { HOTKEYS.forEach(key -> manager.addKeybindToMap(key.getKeybind())); }
    @Override public void addHotkeys(IKeybindManager manager) { manager.addHotkeysForCategory("Litematica Flex Alpha ver.",FlexText.tr("Flex 快捷键"),HOTKEYS); }

    public static void showSummary() {
        var placement=DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        if(placement==null) { message("Flex：请先选中投影并运行校验。"); return; }
        var verifier=placement.getSchematicVerifier();
        var summary=(VerificationSummary)verifier;
        message("Flex / "+placement.getName()+"：严格一致 "+summary.flexExactCount()+"，接受替代 "+summary.flexSubstitutionCount()+"，临时占位 "+summary.flexTemporaryCount()+"，完成合计 "+(summary.flexExactCount()+summary.flexSubstitutionCount()));
    }
    private static void message(String text) {
        var player=Minecraft.getInstance().player;
        if(player!=null)fi.dy.masa.malilib.util.InfoUtils.sendVanillaMessage(Component.literal(FlexText.tr(text)));
    }
}
