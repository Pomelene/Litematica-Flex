# Litematica Flex Alpha ver.

Litematica Flex 是 Litematica 的轻量附加模组，让相近方块的匹配更灵活。例如，轻松放置和原理图校验可以按规则将不同颜色的混凝土或染色玻璃视为等价。

Litematica Flex is a lightweight Litematica addon that makes block matching more flexible. For example, Easy Place and schematic verification can treat different colors of concrete or stained glass as equivalent when the rules allow it.

版本 **Alpha1.1.0**。这是 Minecraft 26.3 的 Fabric 客户端模组，依赖 Litematica 0.29.x、MaLiLib 0.30.x、Fabric Loader 0.19.5+ 和 Java 25。

Version **Alpha1.1.0**. This is a Fabric client mod for Minecraft 26.3. It requires Litematica 0.29.x, MaLiLib 0.30.x, Fabric Loader 0.19.5+, and Java 25.

界面会跟随 Minecraft 当前语言自动显示简体中文或英文；预设组、提示、HUD 和快捷键名称都有对应翻译。切换游戏语言后重新打开 Flex 设置即可看到新语言，配置文件中的规则 ID 不变。

The interface follows Minecraft's selected language and supports Simplified Chinese and English for preset groups, messages, the HUD, and hotkey names. Reopen Flex settings after changing the game language. Rule IDs in the configuration file do not change.

严格复核只在当前游戏会话中暂停替换；重启游戏会自动退出严格复核，避免上次的临时暂停继续阻断替换。

Strict review pauses substitutions only for the current game session. Restarting the game turns it off automatically so a temporary pause cannot silently carry over.

安装 `build/libs/litematica-flex-26.3-Alpha1.1.0.jar`。左 Alt + F 打开设置，左 Alt + R 临时严格复核。

Install `build/libs/litematica-flex-26.3-Alpha1.1.0.jar`. Press Left Alt + F to open settings and Left Alt + R to toggle temporary strict review.

## 设置与优先级 / Settings and priority

| 页面 / Page | 内容 / Contents |
| --- | --- |
| 预设方案<br>Presets | 顶部是替换总开关；选择按种类或全部种类替换，并管理分类、独立小组、批量开关和命名方案。<br>Use the main substitution switch, choose category-based or all-block substitution, and manage categories, independent groups, batch toggles, and named profiles. |
| 黑名单<br>Blocklist | 黑名单开关、保护预设、逐行注册名编辑器。<br>Blocklist switch, protection presets, and a registry-ID editor with one ID per line. |
| 自定义配置<br>Custom configuration | 打开同一个本地 JSON，编辑全局等价组与定向映射，然后重新载入。<br>Open the local JSON file, edit global equivalence groups and directional mappings, then reload it. |
| 选项<br>Options | 统一形状和状态限制、各功能通道、选材与快捷键。<br>Shared shape and state constraints, feature channels, material selection, and hotkeys. |
| 屏幕显示<br>Screen display | HUD 四角位置或关闭，以及水平、垂直边距；底部位置避开原版快捷栏。<br>Place the HUD in any corner or turn it off, and adjust horizontal and vertical margins; bottom positions leave room for the vanilla hotbar. |

完全一致始终正常。其他匹配先检查总开关、严格复核、黑名单及统一限制，再按模式匹配。全部种类模式下种类规则灰显且不参与；切回按种类后原选择保留。没有单独的“允许功能方块”开关，重要方块通过黑名单集中保护。

Exact matches always remain valid. Other matches first pass the main switch, strict review, blocklist, and shared constraints before the selected substitution mode is evaluated. In all-block mode, category rules are disabled but their selections are preserved for when you switch back. Important functional blocks are protected through the blocklist rather than a separate permission switch.

木材分为木板、原木、木头、去皮原木、去皮木头、楼梯、半砖等独立组。本类全开只开启各组，不合并它们。石材提供完整块、楼梯、半砖、墙的跨系列组，细分可限制到具体石材系列。木板与原木互换是默认关闭的跨种类规则，全部种类模式也遵守它。

Wood is split into independent groups such as planks, logs, wood, stripped logs, stripped wood, stairs, and slabs. Enabling all groups in a category does not merge them. Stone offers cross-series groups for full blocks, stairs, slabs, and walls, with finer controls for individual series. Plank-to-log substitution is a separate cross-category rule that is off by default, even in all-block mode.

规则控件修改后自动保存并刷新：重新建立投影网格、清除选材缓存，重新启动已运行过的校验。材料清单的已显示总数仍需在 Litematica 手动重新计算。文本与列表修改在提交时生效。GUI 仍需游戏内检查布局和操作体验。

Changing a rule control saves it and refreshes the schematic renderer, material-selection cache, and any verifier that has already run. Existing material-list totals still need to be recalculated in Litematica. Text and list edits take effect when submitted. GUI layout and interaction still need in-game checks.

所有投影与子区域统一使用全局方案。旧配置中的 `placements` 和 `regions` 字段在读取时忽略，并在下次保存时删除。全局总开关和黑名单始终优先。

All placements and subregions use one global profile. Legacy `placements` and `regions` fields are ignored when read and removed on the next save. The global switch and blocklist always take priority.

Litematica 材料清单新增“显示替代模式”，打开可搜索、滚动的只读替代材料列表。每行展示需求方块图标、名称、已覆盖／所需数量与分配的替代材料，悬停可查看完整文字。它用 Litematica 的库存计数接口读取背包、潜影盒／收纳袋和按原模组设置启用的末影箱缓存；若其他模组已提高原材料清单中的可用数量，也会保留这部分基数。每件物品只分配给一条需求，原材料优先。估算按物品类型进行，具体状态与服务器放置结果仍由校验器和实际放置判断。原材料清单的数量与其他模组接入的数据不会被 Flex 改写。

Litematica's material list gains a “Show substitutions” button that opens a searchable, scrollable, read-only estimate. Each row shows the required block's icon and name, covered versus needed quantity, and allocated substitutes; hover for full text. It reads inventory counts through Litematica, including shulker boxes and bundles, plus the ender-chest cache when enabled in Litematica. If another mod has increased the available count for an original material, that count is retained. Each item is allocated to only one requirement, with exact materials taking priority. This is an item-type estimate; the verifier and actual placement still determine block-state and server results. Flex does not rewrite the original material list or data supplied to it by other mods.

校验器新增“Flex 校验明细”，用可搜索、滚动的列表展示完全一致、合规替代、替代材料状态错误及各接受规则的数量。世界内 HUD 在看向未合规的原理图方块时提示轻松放置的本地选材阻断原因；它不能代替服务器返回的放置结果。

The verifier gains “Flex verification details,” a searchable, scrollable list of exact matches, accepted substitutes, substitute state errors, and counts by acceptance rule. When you look at a schematic block that is not accepted, the in-world HUD shows a local reason why Easy Place cannot select a material. It cannot diagnose the server's placement response.

HUD 默认显示在左上角。若与 Sodium、MiniHUD 或其他信息层重叠，可在“屏幕显示”页手动选择其他角落并调整边距，或将位置设为“关闭”。旧配置的 `showHud: false` 会读取为“关闭”，保存时改用 `hudPosition` 字段。

The HUD appears at the top left by default. If it overlaps Sodium, MiniHUD, or another overlay, choose a different corner and adjust the margins on the “Screen display” page, or set its position to “Off.” A legacy `showHud: false` setting loads as “Off”; subsequent saves use `hudPosition` instead.

## 黑名单 / Blocklist

任一端命中黑名单，就禁止替换及忽略状态，但原方块状态完全一致仍正常。黑名单同时作用于投影、校验、轻松放置及材料规划。默认开启以下保护：

If either block is on the blocklist, substitutions and ignored-state matching are disabled; an exact block-and-state match remains valid. The blocklist applies to rendering, verification, Easy Place, and material planning. These protections are enabled by default:

- 红石核心：红石块、红石线、中继器、比较器、观察者、活塞、漏斗等。<br>Redstone components: redstone blocks and dust, repeaters, comparators, observers, pistons, hoppers, and others.
- 粘液块与蜂蜜块。<br>Slime blocks and honey blocks.
- 黑曜石与哭泣黑曜石。<br>Obsidian and crying obsidian.
- 容器与加工设备，包括所有潜影盒。<br>Containers and workstations, including all shulker boxes.

可选冰/海绵/气泡柱材料及特殊功能方块预设。每个预设悬停可查看完整 ID；自定义列表每行一个注册名。GUI 验证注册名，配置文件允许未安装模组的合法 ID；这些 ID 在未注册时不会匹配。

Optional presets protect ice, sponge, and bubble-column materials, as well as special functional blocks. Hover over a preset to see its full IDs; enter one registry ID per line in the custom list. The GUI validates IDs against registered blocks. The configuration file can contain valid IDs from mods that are not installed, but those IDs cannot match until registered.
