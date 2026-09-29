package io.github.litematicaflex.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import io.github.litematicaflex.FlexMod;
import io.github.litematicaflex.config.RuleProfile;
import io.github.litematicaflex.config.ProfileLibrary;
import io.github.litematicaflex.config.HudPosition;
import io.github.litematicaflex.runtime.FlexRuntime;
import io.github.litematicaflex.api.FlexApi;
import io.github.litematicaflex.rules.BlacklistPresets;
import java.awt.Desktop;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

/** Replacement rules share one column; shared state constraints live on the options page. */
public final class FlexConfigScreen extends GuiConfigsBase {
    private static final List<String> PAGES=List.of("预设方案","黑名单","自定义配置","选项","屏幕显示");
    private String page="预设方案",libraryName="我的方案";
    private RuleProfile profile;
    private final List<IConfigBase> options=new ArrayList<>();
    private ConfigString profileNameOption;
    private boolean advanced,redstoneDetailsExpanded,pendingRebuild,pendingRefresh,resetScroll=true;
    private String category="木材",inputError;

    public FlexConfigScreen() {
        super(10,96,"litematica_flex",null,"Litematica Flex Alpha ver. · Alpha1.1.2 · 26.3");
        normalizeStoredGroups();profile=FlexRuntime.STORE.editable().global;buildOptions();
    }
    @Override public void initGui() {
        flush();if(pendingRefresh)apply();clearOptions();setListPosition(10,listY());reCreateListWidget();super.initGui();
        int width=(getScreenWidth()-26)/PAGES.size(),x=10;
        for(String title:PAGES) {
            button(x,25,width,(page.equals(title)?"§e":"")+FlexText.tr(title),() -> {flush();page=title;resetScroll=true;rebuild();});x+=width+2;
        }
        addLabel(10,52,getScreenWidth()-20,12,0xFFB0D8FF,FlexText.tr("全局方案：所有投影和子区域统一应用"));
        if(page.equals("预设方案")) {
            var categories=List.of("木材","石材","颜色","铜材","自然","功能","跨种类","扩展");
            var categoryPicker=new WidgetDropDownList<String>(10,97,140,18,140,8,categories.stream().map(FlexText::tr).toList()) {
                @Override protected void setSelectedEntry(int index){super.setSelectedEntry(index);if(index>=0 && index<categories.size() && !category.equals(categories.get(index))){category=categories.get(index);pendingRebuild=true;resetScroll=true;}}
            };
            categoryPicker.setSelectedEntry(FlexText.tr(category));addWidget(categoryPicker);
            var names=new ArrayList<>(FlexRuntime.STORE.editable().savedProfiles.keySet());
            if(!names.isEmpty()) {
                var picker=new WidgetDropDownList<String>(155,97,145,18,120,6,names) {
                    @Override protected void setSelectedEntry(int index) {
                        super.setSelectedEntry(index);
                        if(getSelectedEntry()!=null){libraryName=getSelectedEntry();if(profileNameOption!=null)profileNameOption.setStringValue(libraryName);}
                    }
                };
                picker.setSelectedEntry(libraryName);addWidget(picker);
            }
            int actionWidth=(getScreenWidth()-28)/5;
            button(10,73,actionWidth,"保存方案",this::saveProfile);
            button(12+actionWidth,73,actionWidth,"载入方案",this::loadProfile);
            button(14+2*actionWidth,73,actionWidth,advanced?"收起细分":"展开细分",() -> {flush();advanced=!advanced;rebuild();});
            button(16+3*actionWidth,73,actionWidth,"本类全开",() -> batchCategory(true));
            button(18+4*actionWidth,73,actionWidth,"本类全关",() -> batchCategory(false));
        } else if(page.equals("黑名单")) {
            button(10,73,155,redstoneDetailsExpanded?"收起红石细项":"展开红石细项",() -> {
                flush();redstoneDetailsExpanded=!redstoneDetailsExpanded;resetScroll=true;rebuild();
            });
        } else if(page.equals("自定义配置"))initFilePage();
        int bottom=getScreenHeight()-24;
        button(10,bottom,100,"应用并刷新",() -> {if(apply())rebuild();});
        button(115,bottom,100,"重新载入文件",this::reload);
        button(getScreenWidth()-70,bottom,60,"完成",() -> {if(apply())closeGui(true);});
    }
    private void initFilePage() {
        addLabel(10,79,getScreenWidth()-20,12,0xFFFFFFFF,FlexText.tr("本地配置：config/litematica-flex.json"));
        button(10,100,100,"打开配置目录",() -> openPath(false));
        button(115,100,100,"打开配置文件",() -> openPath(true));
        button(220,100,100,"添加示例方案",this::addExample);
        String[] help={"在文本编辑器中修改注册名，保存后点击“重新载入文件”。",
            "global：全局规则；savedProfiles：本地命名方案。",
            "customGroups：组内互认，例如 stone 与 andesite。",
            "replacements：原理图注册名 → 允许替代的注册名列表。",
            "strictBlocks：严格排除；temporaryTargets：占位材料标记。",
            "使用完整注册名，例如 minecraft:stone。",
            "格式错误保留当前有效规则；保存时保留 .bak 备份。"};
        int y=130;
        for(String line:help){if(y>getScreenHeight()-43)break;addLabel(10,y,getScreenWidth()-20,12,0xFFB0B0B0,FlexText.tr(line));y+=17;}
    }
    private void translatedMessage(MessageType type,String text){addMessage(type,FlexText.tr(text));}
    private void button(int x,int y,int width,String title,Runnable action){addButton(new ButtonGeneric(x,y,width,18,FlexText.tr(title)),(b,mouse) -> action.run());}
    private void flush(){getListWidget().applyPendingModifications();}
    private void rebuild(){
        int scroll=resetScroll?0:getListWidget().getScrollbar().getValue();resetScroll=false;
        pendingRebuild=false;clearOptions();buildOptions();setListPosition(10,listY());reCreateListWidget();initGui();
        getListWidget().getScrollbar().setValue(scroll);
    }
    private void rebind() { profile=FlexRuntime.STORE.editable().global; }
    private boolean apply() {
        flush();
        if(inputError!=null){translatedMessage(MessageType.ERROR,inputError);return false;}
        pendingRefresh=false;
        try { FlexRuntime.changed();InputEventHandler.getKeybindManager().updateUsedKeys();return true; }
        catch(RuntimeException e){translatedMessage(MessageType.ERROR,"配置未保存："+e.getMessage());return false;}
    }
    private void reload() {
        // Deliberately do not save first: the user may have edited this file externally.
        if(!FlexRuntime.STORE.load()) {
            translatedMessage(MessageType.ERROR,"载入失败，保留当前规则："+FlexRuntime.STORE.lastError());return;
        }
        FlexRuntime.clearTemporaryReview();normalizeStoredGroups();FlexMod.reloadHotkeys();rebind();rebuild();
        try { FlexRuntime.changed();translatedMessage(MessageType.SUCCESS,"已重新载入并刷新配置。"); }
        catch(RuntimeException e){translatedMessage(MessageType.ERROR,e.getMessage());}
    }
    private void saveProfile() {
        flush();
        if(libraryName.isBlank()){translatedMessage(MessageType.ERROR,"请填写方案名称。");return;}
        var saved=ProfileLibrary.copy(profile);saved.strictReview=false;
        FlexRuntime.STORE.editable().savedProfiles.put(libraryName,saved);
        if(apply())translatedMessage(MessageType.SUCCESS,"已保存方案："+libraryName);
        rebuild();
    }
    private void loadProfile() {
        flush();
        var saved=FlexRuntime.STORE.editable().savedProfiles.get(libraryName);
        if(saved==null){translatedMessage(MessageType.ERROR,"没有这个方案，请填写已保存的名称。");return;}
        var copy=ProfileLibrary.copy(saved);copy.strictReview=false;
        FlexRuntime.STORE.editable().global=copy;
        rebind();rebuild();apply();
    }
    private void addExample() {
        var example=new RuleProfile();
        example.customGroups.add(new LinkedHashSet<>(List.of("minecraft:stone","minecraft:andesite","minecraft:diorite")));
        example.replacements.put("minecraft:oak_planks",List.of("minecraft:spruce_planks"));
        FlexRuntime.STORE.editable().savedProfiles.putIfAbsent("自定义示例",example);
        if(apply())translatedMessage(MessageType.SUCCESS,"已添加 savedProfiles / 自定义示例，未启用或覆盖现有规则。");
    }
    private void openPath(boolean file) {
        if(!Files.exists(FlexRuntime.STORE.path()) && !apply())return;
        try {
            if(!Desktop.isDesktopSupported())throw new IllegalStateException("系统不支持打开文件，请手动打开游戏实例的 config 目录。");
            Desktop.getDesktop().open((file?FlexRuntime.STORE.path():FlexRuntime.STORE.path().getParent()).toFile());
        } catch(Exception e){translatedMessage(MessageType.ERROR,"无法打开："+e.getMessage());}
    }
    @Override protected void onSettingsChanged(){InputEventHandler.getKeybindManager().updateUsedKeys();}
    @Override public void removed(){super.removed();apply();}
    @Override public List<ConfigOptionWrapper> getConfigs(){return ConfigOptionWrapper.createFor(options);}
    private int listY(){return page.equals("自定义配置")?getScreenHeight()+30:page.equals("预设方案")?120:page.equals("黑名单")?98:74;}
    @Override protected int getBrowserWidth(){return Math.max(80,getScreenWidth()-20);}
    @Override protected int getBrowserHeight(){return page.equals("自定义配置")?0:Math.max(30,getScreenHeight()-getListY()-38);}
    @Override protected int getConfigWidth(){return Math.min(180,Math.max(65,getBrowserWidth()/3));}
    @Override protected boolean useKeybindSearch(){return page.equals("选项");}
    @Override protected WidgetListConfigOptions createListWidget(int x,int y){return new FlexOptionList(x,y,getBrowserWidth(),getBrowserHeight(),getConfigWidth(),useKeybindSearch(),this);}
    @Override public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick){boolean handled=super.onMouseClicked(event,doubleClick);finishChanges();return handled;}

    private ConfigBoolean bool(String name,boolean value,Consumer<Boolean> change) {
        var config=new ConfigBoolean(name,value,FlexText.tr("修改后自动保存并刷新投影及已启动的校验。"));
        config.setPrettyName(FlexText.tr(name));config.setValueChangeCallback(c -> {change.accept(c.getBooleanValue());pendingRefresh=true;});options.add(config);return config;
    }
    private ConfigBoolean kind(String name,boolean value,Consumer<Boolean> change) {
        if(profile.allReplacements || !profile.enabled || !FlexRuntime.STORE.editable().global.enabled || profile.strictReview){var option=new FlexOptionList.LockedBoolean(name,value);options.add(option);return option;}
        return bool(name,value,change);
    }
    private boolean enabled(String key){return profile.enabledGroups.stream().anyMatch(g -> key.equals(g)||key.startsWith(g+"."));}
    private boolean inheritedStoneShape(String key) {
        if(!key.startsWith("stone.") || key.startsWith("stone.shape."))return false;
        String shape=key.substring(key.lastIndexOf('.')+1);
        return profile.enabledGroups.contains("stone.shape."+shape);
    }
    private void group(String key,String label) {
        String shape=key.substring(key.lastIndexOf('.')+1);
        boolean shapeParent=key.startsWith("stone.shape.");
        boolean partial=!enabled(key) && (shapeParent
            ? FlexRuntime.CATALOGUE.groups().stream().anyMatch(g -> g.category().equals("石材") && !g.id().startsWith("stone.shape.") && g.id().endsWith("."+shape) && enabled(g.id()))
            : profile.enabledGroups.stream().anyMatch(id -> id.startsWith(key+".")));
        var option=kind(FlexText.tr(label)+(partial?FlexText.tr("（部分开启）"):""),inheritedStoneShape(key)||enabled(key)||partial,value -> {
            if(inheritedStoneShape(key)) {
                profile.enabledGroups.remove("stone.shape."+shape);
                FlexRuntime.CATALOGUE.groups().stream().filter(g -> g.category().equals("石材") && !g.id().startsWith("stone.shape.") && g.id().endsWith("."+shape)).forEach(g -> profile.enabledGroups.add(g.id()));
            }
            if(value)profile.enabledGroups.add(key);
            else {
                var parents=profile.enabledGroups.stream().filter(id -> key.startsWith(id+".")).toList();
                for(String parent:parents){profile.enabledGroups.remove(parent);FlexRuntime.CATALOGUE.groups().stream().map(g -> g.id()).filter(id -> id.startsWith(parent+".")&&!id.equals(key)).forEach(profile.enabledGroups::add);}
                profile.enabledGroups.removeIf(id -> id.equals(key)||id.startsWith(key+"."));
                if(shapeParent)FlexRuntime.CATALOGUE.groups().stream().filter(g -> g.category().equals("石材") && g.id().endsWith("."+shape)).forEach(g -> profile.enabledGroups.remove(g.id()));
            }
            pendingRebuild=true;
        });
        var members=FlexRuntime.CATALOGUE.groups().stream().filter(g -> g.id().equals(key)||g.id().startsWith(key+".")).flatMap(g -> g.members().stream()).distinct().sorted().toList();
        var excluded=new HashSet<>(BlacklistPresets.effective(profile));excluded.addAll(BlacklistPresets.effective(FlexRuntime.STORE.editable().global));
        long protectedCount=members.stream().filter(excluded::contains).count();
        option.setComment(FlexText.tr("独立组内互认，不会与其他开启的组自动合并。黑名单和统一状态限制始终优先。\n"
            + (profile.allReplacements?"当前全部种类模式，此组设置保留但不参与判断。\n":"")
            + "成员 "+members.size()+" 个；当前黑名单排除 "+protectedCount+" 个。\n"
            + String.join(", ",members.stream().limit(20).toList())+(members.size()>20?" …":"")));
    }
    private void batchCategory(boolean value) {
        if(profile.allReplacements || !profile.enabled || !FlexRuntime.STORE.editable().global.enabled || profile.strictReview) {
            translatedMessage(MessageType.WARNING,"请先启用替换，并切换到按种类替换。");return;
        }
        flush();
        var keys=new LinkedHashSet<String>();
        if(category.equals("跨种类")){profile.allowPlankLogReplacement=value;profile.hardnessMatching=value;}
        else if(category.equals("铜材")){keys.add("copper.oxidation");keys.add("copper.wax");}
        else if(category.equals("扩展"))FlexApi.INSTANCE.rules().forEach(r -> keys.add(r.id()));
        else FlexRuntime.CATALOGUE.groups().stream().filter(g -> g.category().equals(category) && !g.id().endsWith(".all") && (!category.equals("石材") || g.id().startsWith("stone.shape."))).forEach(g -> keys.add(g.id()));
        if(value)profile.enabledGroups.addAll(keys);
        else {
            String prefix=switch(category){case "木材" -> "wood";case "石材" -> "stone";case "颜色" -> "color";case "铜材" -> "copper";case "自然" -> "nature";case "功能" -> "functional";default -> "";};
            profile.enabledGroups.removeIf(id -> keys.contains(id) || (!prefix.isEmpty() && (id.equals(prefix)||id.startsWith(prefix+"."))));
        }
        apply();rebuild();
    }
    private void text(String name,String value,Consumer<String> change) {
        var config=new ConfigString(name,value);config.setPrettyName(FlexText.tr(name));config.setValueChangeCallback(c -> {if(name.equals("方案名称")){change.accept(c.getStringValue());return;}change.accept(c.getStringValue());pendingRefresh=true;});options.add(config);
        if(name.equals("方案名称"))profileNameOption=config;
    }
    private void normalizeStoredGroups() {
        var config=FlexRuntime.STORE.editable();FlexRuntime.normalizeGroups(config.global);
        config.savedProfiles.values().forEach(FlexRuntime::normalizeGroups);
    }
    private void finishChanges() {
        flush();
        if(pendingRefresh)apply();
        if(pendingRebuild)rebuild();
    }
    @Override public boolean onKeyTyped(net.minecraft.client.input.KeyEvent event){boolean handled=super.onKeyTyped(event);finishChanges();return handled;}
    private void buildOptions() {
        options.clear();if(page.equals("自定义配置"))return;
        if(page.equals("黑名单")){blacklistOptions();return;}
        if(page.equals("屏幕显示")){hudOptions();return;}
        if(page.equals("预设方案")) {
            bool("启用替换（全局总开关）",profile.enabled,v -> {profile.enabled=v;pendingRebuild=true;})
                .setComment(FlexText.tr("关闭后停止全部替换和状态忽略，立即恢复原始判断。所有投影和子区域统一使用此设置。"));
            var mode=new ConfigOptionList("替换模式",profile.allReplacements?ReplacementMode.ALL:ReplacementMode.KINDS);
            mode.setPrettyName(FlexText.tr("替换模式"));mode.setValueChangeCallback(c -> {profile.allReplacements=c.getStringValue().equals("ALL");pendingRefresh=true;pendingRebuild=true;});options.add(mode);
            if(category.equals("跨种类")) {
                bool("木板 ↔ 原木／去皮原木",profile.allowPlankLogReplacement,v -> profile.allowPlankLogReplacement=v)
                    .setComment(FlexText.tr("默认关闭；开启允许这两类互换。全部替换模式也遵守此限制，黑名单和统一状态限制始终优先。"));
                kind("硬度相近的同形状方块",profile.hardnessMatching,v -> profile.hardnessMatching=v);
            } else if(category.equals("铜材")) {
                group("copper.oxidation","同类铜材：允许氧化差异");group("copper.wax","同类铜材：允许涂蜡差异");
                if(advanced)for(var entry:FlexRuntime.CATALOGUE.groups())if(entry.category().equals("铜材"))group(entry.id(),FlexText.group(entry));
            } else if(category.equals("扩展")) {
                for(var rule:FlexApi.INSTANCE.rules())group(rule.id(),"扩展 / "+rule.id());
            } else for(var entry:FlexRuntime.CATALOGUE.groups()) {
                if(!entry.category().equals(category) || entry.id().endsWith(".all"))continue;
                if(category.equals("石材") && !advanced && !entry.id().startsWith("stone.shape."))continue;
                group(entry.id(),FlexText.group(entry));
            }
            text("方案名称",libraryName,v -> libraryName=v);
            return;
        }
        bool("临时暂停替换（严格复核）",profile.strictReview,v -> profile.strictReview=v)
            .setComment(FlexText.tr("开启后暂停校验、渲染、轻松放置和材料清单中的全部替换；关闭后恢复。重启游戏自动关闭。"));
        bool("应用于校验器",profile.verification,v -> profile.verification=v);
        bool("应用于投影渲染",profile.rendering,v -> profile.rendering=v);
        bool("应用于轻松放置和放置限制",profile.placement,v -> profile.placement=v);
        bool("应用于材料清单",profile.materialList,v -> profile.materialList=v);
        bool("标记已接受的替代方块",profile.showAcceptedOverlay,v -> profile.showAcceptedOverlay=v);
        bool("保持方块形状",!profile.allowCrossShape,v -> profile.allowCrossShape=!v);
        stateOptions();
        var selection=new ConfigOptionList("选材方式",Selection.valueOf(profile.selection));selection.setPrettyName(FlexText.tr("选材方式"));selection.setValueChangeCallback(c -> {profile.selection=c.getStringValue();pendingRefresh=true;});options.add(selection);
        var tolerance=new ConfigDouble("硬度差容差",profile.hardnessTolerance,0,100);tolerance.setPrettyName(FlexText.tr("硬度差容差"));tolerance.setValueChangeCallback(c -> {profile.hardnessTolerance=(float)c.getDoubleValue();pendingRefresh=true;});options.add(tolerance);
        for(var key:List.of(FlexMod.OPEN,FlexMod.STRICT,FlexMod.TOGGLE,FlexMod.SUMMARY)){key.setPrettyName(FlexText.tr(key.getName()));options.add(key);}
    }
    private void hudOptions() {
        var config=FlexRuntime.STORE.editable();
        var position=new ConfigOptionList("HUD 位置",HudPosition.valueOf(config.hudPosition));
        position.setPrettyName(FlexText.tr("HUD 位置"));
        position.setComment(FlexText.tr("选择四角或关闭；边距可用于避开 Sodium、MiniHUD 等信息层。底部位置会避开原版快捷栏。"));
        position.setValueChangeCallback(c -> {config.hudPosition=c.getStringValue();pendingRefresh=true;});
        options.add(position);
        var x=new ConfigInteger("水平边距",config.hudOffsetX,0,500);
        x.setPrettyName(FlexText.tr("水平边距"));
        x.setComment(FlexText.tr("从所选屏幕角向内移动的像素数。"));
        x.setValueChangeCallback(c -> {config.hudOffsetX=c.getIntegerValue();pendingRefresh=true;});
        options.add(x);
        var y=new ConfigInteger("垂直边距",config.hudOffsetY,0,500);
        y.setPrettyName(FlexText.tr("垂直边距"));
        y.setComment(FlexText.tr("从屏幕顶部或快捷栏上方安全区域向内移动的像素数。"));
        y.setValueChangeCallback(c -> {config.hudOffsetY=c.getIntegerValue();pendingRefresh=true;});
        options.add(y);
    }
    private void blacklistOptions() {
        bool("启用本范围黑名单",profile.blacklistEnabled,v -> {profile.blacklistEnabled=v;pendingRebuild=true;})
            .setComment(FlexText.tr("任一端命中就禁止替换与状态忽略；完全一致仍正常。全局黑名单始终优先，当前投影不能绕过。"));
        for(var preset:BlacklistPresets.ALL) {
            if(preset.id().startsWith("redstone."))continue;
            blacklistPreset(preset,"保护 / "+preset.title(),false);
            if(preset==BlacklistPresets.REDSTONE_ALL && redstoneDetailsExpanded) {
                for(var detail:BlacklistPresets.REDSTONE_DETAILS)
                    blacklistPreset(detail,"  ↳ "+detail.title(),profile.blacklistPresets.contains("redstone"));
            }
        }
        idList("自定义保护方块 ID",profile.blacklistBlocks,ids -> profile.blacklistBlocks=ids);
        if(!profile.strictBlocks.isEmpty())idList("旧配置严格排除（始终有效）",profile.strictBlocks,ids -> profile.strictBlocks=ids);
    }
    private void blacklistPreset(BlacklistPresets.Preset preset,String label,boolean coveredByAll) {
        boolean selected=profile.blacklistPresets.contains(preset.id());
        ConfigBoolean option;
        if(coveredByAll) {
            option=new FlexOptionList.LockedBoolean(label,selected,
                "红石全部保护已开启，细项设置暂不参与判断；关闭全部保护后可分别选择。");
            options.add(option);
        } else option=bool(label,selected,value -> {
            var ids=new LinkedHashSet<>(profile.blacklistPresets);
            if(value)ids.add(preset.id());else ids.remove(preset.id());
            profile.blacklistPresets=ids;
            if(preset==BlacklistPresets.REDSTONE_ALL)pendingRebuild=true;
        });
        String explanation=FlexText.tr("受启用黑名单开关控制。包含：\n")+
            String.join(", ",new TreeSet<>(preset.blocks()));
        if(coveredByAll)explanation+="\n"+FlexText.tr("红石全部保护已开启，细项设置暂不参与判断；关闭全部保护后可分别选择。");
        if(preset.id().startsWith("redstone."))explanation+="\n"+FlexText.tr("其他开启的预设也可能保护这里的方块。");
        if(preset==BlacklistPresets.REDSTONE_ALL)explanation+="\n"+
            FlexText.tr("全部保护开启时，细项灰显；关闭后按细项保护。")+"\n"+
            FlexText.tr("容器等其它预设仍可能保护同一方块。");
        option.setComment(explanation);
    }
    private void idList(String name,Set<String> values,Consumer<Set<String>> change) {
        var config=new ConfigStringList(name,com.google.common.collect.ImmutableList.copyOf(new TreeSet<>(values)),FlexText.tr("打开列表编辑器，每行填写一个方块注册名。例如 minecraft:obsidian；也可编辑同一个本地 JSON。"));
        config.setPrettyName(FlexText.tr(name));config.setValueChangeCallback(c -> {
            var ids=parseBlockIds(String.join(",",c.getStrings()));
            if(ids!=null){change.accept(ids);pendingRefresh=true;}
        });options.add(config);
    }
    private Set<String> parseBlockIds(String text) {
        var ids=new LinkedHashSet<String>();
        for(String part:text.split("[,;，；\\s]+")) {
            if(part.isBlank())continue;
            var id=net.minecraft.resources.Identifier.tryParse(part.contains(":")?part:"minecraft:"+part);
            if(id==null || !net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) {
                inputError="无效或未注册方块 ID："+part;translatedMessage(MessageType.ERROR,inputError);return null;
            }
            ids.add(id.toString());
        }
        inputError=null;return ids;
    }
    private enum ReplacementMode implements IConfigOptionListEntry {
        KINDS("按种类替换"),ALL("全部种类替换");
        private final String label;ReplacementMode(String label){this.label=label;}
        @Override public String getStringValue(){return name();}
        @Override public String getDisplayName(){return FlexText.tr(label);}
        @Override public IConfigOptionListEntry cycle(boolean forward){return this==KINDS?ALL:KINDS;}
        @Override public IConfigOptionListEntry fromString(String value){return valueOf(value);}
    }
    private void preserve(String name,List<String> keys) {
        bool(name,keys.stream().noneMatch(profile.ignoredProperties::contains),value -> {
            if(value)profile.ignoredProperties.removeAll(keys);else profile.ignoredProperties.addAll(keys);
        }).setComment(FlexText.tr("统一作用于所有替换及种类替换；关闭表示忽略该状态差异。"));
    }
    private void stateOptions() {
        preserve("保留朝向",List.of("facing","rotation","orientation"));
        preserve("保留轴向",List.of("axis"));
        preserve("保留上下位置",List.of("half"));
        preserve("保留半砖上下和双半砖状态",List.of("type"));
        preserve("保留含水状态",List.of("waterlogged"));
        preserve("保留楼梯连接形状",List.of("shape"));
        preserve("保留四向连接",List.of("north","east","south","west","up"));
        preserve("保留生长年龄",List.of("age","stage"));
        preserve("保留红石运行状态",List.of("power","powered","lit","triggered"));
        preserve("保留门开关",List.of("open"));
        preserve("保留树叶距离和持久状态",List.of("distance","persistent"));
        preserve("保留蜡烛数量",List.of("candles"));
    }
    private enum Selection implements IConfigOptionListEntry {
        HELD_FIRST("手持优先"),ORIGINAL_FIRST("原材料优先"),FIXED_ONLY("仅固定映射");
        private final String label;Selection(String label){this.label=label;}
        @Override public String getStringValue(){return name();}
        @Override public String getDisplayName(){return FlexText.tr(label);}
        @Override public IConfigOptionListEntry cycle(boolean forward){return values()[Math.floorMod(ordinal()+(forward?1:-1),values().length)];}
        @Override public IConfigOptionListEntry fromString(String value){return valueOf(value);}
    }
}
