# Litematica Flex Alpha ver. 变更记录 / Changelog

## Alpha1.1.0

- 设置新增“屏幕显示”页：HUD 可放在左上、右上、左下、右下，或完全关闭，并可调整水平与垂直边距。<br>The new “Screen display” page places the HUD in any corner or turns it off, with adjustable horizontal and vertical margins.
- HUD 根据翻译后的实际文本宽度定位，长文字在窄屏上截断；底部位置为原版快捷栏预留空间。<br>The HUD is positioned using the measured width of translated text. Long lines are truncated on narrow screens, and bottom positions leave room for the vanilla hotbar.
- 旧配置的 `showHud: false` 自动迁移为 `hudPosition: "OFF"`，保存时移除旧字段。<br>Legacy `showHud: false` settings migrate to `hudPosition: "OFF"`; the old field is removed on save.

## Alpha1.0.1

- 所有投影与子区域统一使用全局规则；移除旧的 `placements`、`regions` 配置字段。<br>All placements and subregions share global rules; legacy `placements` and `regions` configuration fields are removed.
- 新增轻松放置本地诊断，提示总开关、严格复核、黑名单、状态限制或背包选材等阻断原因。<br>Local Easy Place diagnostics explain blockers such as the main switch, strict review, blocklist, state constraints, and inventory selection.
- 在 Litematica 校验器中增加 Flex 校验明细，区分合规替代、临时占位、替代材料状态错误，并统计接受原因。<br>Flex verification details distinguish accepted substitutes, temporary materials, and substitute state errors, and count acceptance reasons.
- 在材料清单中增加“显示替代模式”：按当前规则估算背包及已启用的末影箱缓存材料，优先分配原材料，每件物品仅计一次，不改写原清单。<br>The material list gains “Show substitutions,” which estimates inventory and enabled ender-chest-cache materials under current rules. Exact materials are allocated first, each item is counted once, and the original list is unchanged.
- 材料与校验明细改为可滚动、可搜索的 Litematica 风格列表；补充方块图标、颜色提示和完整文字悬停说明。<br>Material and verification details use searchable, scrollable Litematica-style lists with block icons, color cues, and full text on hover.
- 设置、提示、HUD 与快捷键名称跟随 Minecraft 当前语言切换，提供简体中文和英文。<br>Settings, messages, the HUD, and hotkey names follow Minecraft's selected language, with Simplified Chinese and English support.

## Alpha1.0.0

- 补上默认旧版轻松放置路径 `WorldUtils.doEasyPlaceAction`；与新版路径共享目标、物品查询及背包切换逻辑。<br>The default legacy Easy Place path, `WorldUtils.doEasyPlaceAction`, is covered and shares target selection, item lookup, and inventory switching with the newer path.
- 真实加载并检查两套 Litematica 接入点，执行两套实际注入的替代物品查询回归。<br>Both Litematica integration paths are loaded and checked, with regression checks for their actual injected substitute-item queries.
- 用户可见名称统一为 Litematica Flex Alpha ver.，版本为 Alpha1.0.0。<br>The user-facing name becomes Litematica Flex Alpha ver., version Alpha1.0.0.

## 0.1.6

- 总开关移到预设顶部；替换模式二选一，分类规则按独立小组显示并支持批量开启。<br>The main switch moves to the top of Presets. Substitution mode becomes a two-way choice, and category rules appear as independent groups with batch controls.
- 木材总开关迁移为独立加工形式小组，石材拆为完整块、楼梯、半砖、墙；细分可退出形状总组。<br>The wood-wide switch becomes independent processing-form groups. Stone splits into full blocks, stairs, slabs, and walls, with finer choices that can override a shape-wide group.
- 新增独立黑名单页，支持保护预设及逐行方块 ID 编辑；黑名单先于全部匹配，且全局保护不能被投影覆盖。<br>A dedicated blocklist page adds protection presets and a line-by-line block-ID editor. The blocklist is checked before matching, and placement settings cannot bypass global protection.
- 功能方块保护统一交给黑名单，移除重复的功能方块许可开关。<br>Functional-block protection moves into the blocklist, removing the redundant permission switch.
- 修改规则自动保存并重建投影网格、清除选材缓存、重新校验；显示配置来源并支持恢复继承。<br>Rule changes save automatically, rebuild schematic meshes, clear the material-selection cache, and rerun verification. The UI shows the configuration source and supports restoring inheritance.
- 新增黑名单优先级、全局停用、旧配置迁移和全方块审计检查。<br>Checks are added for blocklist precedence, global disablement, legacy configuration migration, and an audit of all blocks.

## 0.1.5

- 材料合规但状态不符的替代方块按黄色状态错误渲染，并归入校验器的状态错误。<br>A substitute with an allowed material but incorrect state renders as a yellow state error and is counted as a verifier state mismatch.
- 木板与原木、去皮原木默认隔离，新增独立互换选项，所有替换也遵守该限制。<br>Planks are separate from logs and stripped logs by default. A dedicated cross-substitution option is added, and even all-block mode respects this restriction.
- 轻松放置的物品查询使用选中的替代状态，补充背包到安全快捷栏槽的切换。<br>Easy Place item lookup uses the selected substitute state and can move an inventory item into a safe hotbar slot.
- 尚未启动校验时 HUD 显示提示，避免三个零被误读为实时统计。<br>The HUD prompts the player when verification has not started, so three zeros are not mistaken for live statistics.

## 0.1.4

- 所有替换与种类替换合并为一列，总开关启用后种类选项灰显、暂停生效，关闭后恢复原选择。<br>All-block and category substitutions share one control area. Category options become inactive and gray in all-block mode, while their previous selections remain available afterward.
- 朝向、上下位置、含水等统一限制移到“选项”，取消独立匹配条件页。<br>Shared constraints such as orientation, upper/lower position, and waterlogging move to Options; the separate matching-conditions page is removed.
- 配置与命名方案仅提供本地保存和读取入口。<br>Configuration and named profiles use local save and load only.

## 0.1.3

- 预设主界面改为颜色、木材、石材等材质互换总开关，细分规则默认收起。<br>The Presets page gains material-category switches for colors, wood, stone, and others, with detailed rules collapsed by default.
- 石材总开关包含同形状的跨石材系列替代，例如平滑石下半砖与石砖下半砖；木材总开关仍不跨到石材。<br>The stone-wide switch accepts same-shape substitutions across stone series, such as a smooth-stone bottom slab and stone-brick bottom slab. The wood switch never extends into stone.
- 左侧分类去掉容易误认成开关的方框，显示被匹配条件覆盖时的恢复入口。<br>The category list loses boxes that looked like switches and gains a way to restore settings overridden by matching conditions.
- 增加真实半砖状态、物品选材、渲染和校验接受的运行时回归用例。<br>Runtime regression cases cover real slab states, item selection, rendering, and verifier acceptance.

## 0.1.2

- 修复加载原理图时 Flex 渲染位置上下文未初始化导致的空指针。<br>Fix a null pointer caused by an uninitialized Flex render-position context when loading a schematic.
- 开发验证移入独立源集，正式 JAR 不含测试入口；验证失败不打开错误弹窗。<br>Development checks move to a separate source set, keeping test entrypoints out of the production JAR; failed checks no longer open an error dialog.
- 石材拆为系列与形状的小项，石头、圆石、石砖分开；旧配置仍兼容。<br>Stone presets split by series and shape, separating stone, cobblestone, and stone bricks while retaining compatibility with older configuration files.
- 新增独立“匹配条件”页，注明补充或替代材料预设，以及保留的状态限制和严格排除。<br>A separate Matching conditions page clarifies supplemental and overriding material presets, preserved state constraints, and strict exclusions.
- 减少固定空白，关闭非快捷键页多余的快捷键搜索行。<br>Reduce fixed empty space and hide the unnecessary hotkey search row outside the hotkey page.

## 0.1.1

- 设置重排为“预设方案 / 自定义配置 / 选项”，使用 MaLiLib 原有控件。<br>Settings are reorganized into Presets, Custom configuration, and Options using MaLiLib controls.
- 分类支持批量开关和小项调整；已保存方案可从下拉列表选择。<br>Categories support batch toggles and individual rules; saved profiles can be selected from a dropdown.
- 全部设置和命名方案保存在 `config/litematica-flex.json`，兼容第一版文件。<br>All settings and named profiles are saved in `config/litematica-flex.json`, with compatibility for the first configuration format.
- 自定义规则改为文件编辑入口，提供示例方案与重新载入。<br>Custom rules use a file-editing entry point with an example profile and reload option.
- 添加备份、错误提示和外部编辑保护；重载同步快捷键并刷新投影与校验。<br>Add backups, error messages, and protection against overwriting external edits. Reloading synchronizes hotkeys and refreshes rendering and verification.
- 修复切换页签时待提交文本编辑丢失的问题。<br>Fix loss of pending text edits when switching tabs.
