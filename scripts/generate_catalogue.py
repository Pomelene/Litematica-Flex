"""Build explicit vanilla groups from an existing official client JAR; no downloads or digest checks."""
import json
import pathlib
import sys
import zipfile

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
          'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry',
         'pale_oak', 'poplar', 'bamboo', 'crimson', 'warped']

def generate(jar):
    with zipfile.ZipFile(jar) as archive:
        prefix = 'assets/minecraft/blockstates/'
        ids = {name[len(prefix):-5] for name in archive.namelist()
               if name.startswith(prefix) and name.endswith('.json')}
    groups = []

    def group(key, title, category, members):
        members = sorted(set(members) & ids)
        if len(members) > 1:
            groups.append(dict(id=key, title=title, category=category,
                               members=['minecraft:' + value for value in members]))

    color_names = {'wool': '羊毛', 'wool_stairs': '羊毛楼梯', 'wool_slab': '羊毛半砖',
                   'concrete': '混凝土', 'concrete_stairs': '混凝土楼梯', 'concrete_slab': '混凝土半砖',
                   'carpet': '地毯', 'concrete_powder': '混凝土粉末', 'terracotta': '陶瓦（含未染色）',
                   'glazed_terracotta': '带釉陶瓦', 'stained_glass': '玻璃（含普通玻璃）',
                   'stained_glass_pane': '玻璃板（含普通玻璃板）', 'bed': '床',
                   'shulker_box': '潜影盒', 'candle': '蜡烛', 'candle_cake': '蛋糕上的蜡烛',
                   'banner': '立式旗帜', 'wall_banner': '墙式旗帜'}
    for suffix, title in color_names.items():
        members = [c + '_' + suffix for c in COLORS]
        members += {'stained_glass': ['glass'], 'stained_glass_pane': ['glass_pane'],
                    'terracotta': ['terracotta'], 'shulker_box': ['shulker_box'],
                    'candle': ['candle'], 'candle_cake': ['candle_cake']}.get(suffix, [])
        group('color.' + suffix, title, '颜色', members)

    wood_names = {'planks': '木板', 'stairs': '木楼梯', 'slab': '木半砖', 'log': '原木与菌柄',
                  'wood': '木头与菌核', 'stripped_log': '去皮原木与菌柄', 'stripped_wood': '去皮木头与菌核',
                  'fence': '木栅栏', 'fence_gate': '栅栏门', 'door': '木门', 'trapdoor': '木活板门',
                  'button': '木按钮', 'pressure_plate': '木压力板', 'sign': '立式告示牌',
                  'wall_sign': '墙式告示牌', 'hanging_sign': '悬挂告示牌',
                  'wall_hanging_sign': '墙式悬挂告示牌', 'shelf': '木置物架'}
    for suffix, title in wood_names.items():
        stripped = suffix.startswith('stripped_')
        base = suffix.removeprefix('stripped_')
        members = []
        for wood in WOODS:
            actual = {'log': 'stem', 'wood': 'hyphae'}.get(base, base) if wood in ('crimson', 'warped') else base
            if wood == 'bamboo' and base == 'log': actual = 'block'
            members.append(('stripped_' if stripped else '') + wood + '_' + actual)
        if base in ('planks', 'stairs', 'slab'):
            members.append('bamboo_mosaic' + ('' if base == 'planks' else '_' + base))
        group('wood.' + suffix, title, '木材', members)
    group('wood.all', '木材互换（同形状，不限树种与加工形式）', '木材',
          [member.removeprefix('minecraft:') for entry in groups if entry['category'] == '木材' for member in entry['members']])

    stone_sets = {
        'stone': ('石头与石砖', ['stone', 'smooth_stone', 'cobblestone', 'mossy_cobblestone', 'stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks']),
        'granite': ('花岗岩', ['granite', 'polished_granite']),
        'diorite': ('闪长岩', ['diorite', 'polished_diorite']),
        'andesite': ('安山岩', ['andesite', 'polished_andesite']),
        'deepslate': ('深板岩', ['deepslate', 'cobbled_deepslate', 'polished_deepslate', 'deepslate_bricks', 'deepslate_tiles', 'cracked_deepslate_bricks', 'cracked_deepslate_tiles', 'chiseled_deepslate']),
        'tuff': ('凝灰岩', ['tuff', 'polished_tuff', 'tuff_bricks', 'chiseled_tuff', 'chiseled_tuff_bricks']),
        'blackstone': ('黑石', ['blackstone', 'polished_blackstone', 'polished_blackstone_bricks', 'cracked_polished_blackstone_bricks', 'chiseled_polished_blackstone']),
        'sandstone': ('砂岩', ['sandstone', 'cut_sandstone', 'smooth_sandstone', 'chiseled_sandstone']),
        'red_sandstone': ('红砂岩', ['red_sandstone', 'cut_red_sandstone', 'smooth_red_sandstone', 'chiseled_red_sandstone']),
        'nether_brick': ('下界砖', ['nether_bricks', 'red_nether_bricks', 'cracked_nether_bricks', 'chiseled_nether_bricks']),
        'prismarine': ('海晶石', ['prismarine', 'prismarine_bricks', 'dark_prismarine']),
        'quartz': ('石英', ['quartz_block', 'smooth_quartz', 'chiseled_quartz_block', 'quartz_bricks', 'quartz_pillar']),
        'purpur': ('紫珀', ['purpur_block', 'purpur_pillar']),
        'end_stone': ('末地石', ['end_stone', 'end_stone_bricks']),
        'brick': ('红砖', ['bricks']), 'mud_brick': ('泥砖', ['mud_bricks']),
        'resin_brick': ('树脂砖', ['resin_bricks', 'chiseled_resin_bricks']),
        'sulfur': ('硫磺建筑材料', ['sulfur', 'polished_sulfur', 'sulfur_bricks', 'chiseled_sulfur']),
        'cinnabar': ('辰砂建筑材料', ['cinnabar', 'polished_cinnabar', 'cinnabar_bricks', 'chiseled_cinnabar']),
        'basalt': ('玄武岩', ['basalt', 'polished_basalt', 'smooth_basalt'])
    }
    all_stone = []
    for key, (title, full) in stone_sets.items():
        members = list(full)
        for block in full:
            stem = block.removesuffix('s') if block.endswith(('bricks', 'tiles')) else block
            if stem.endswith('_block'): stem = stem[:-6]
            members += [stem + '_' + shape for shape in ('stairs', 'slab', 'wall')]
        all_stone += members
        # Keep original IDs for existing config files, but do not mix shapes in the visible list.
        group('stone.' + key, title + '（旧版系列规则）', '兼容', members)
        visible = [(key, title, full)]
        if key == 'stone':
            visible = [
                ('stone', '石头与平滑石头', ['stone', 'smooth_stone']),
                ('cobblestone', '圆石与苔石', ['cobblestone', 'mossy_cobblestone']),
                ('stone_brick', '石砖变种', ['stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks'])
            ]
        for family, family_title, bases in visible:
            forms = list(bases)
            for block in bases:
                stem = block.removesuffix('s') if block.endswith(('bricks', 'tiles')) else block
                if stem.endswith('_block'): stem = stem[:-6]
                forms += [stem + '_' + shape for shape in ('stairs', 'slab', 'wall')]
            for shape, label in [('full', '完整方块'), ('stairs', '楼梯'), ('slab', '半砖'), ('wall', '墙')]:
                selected = bases if shape == 'full' else [i for i in forms if i.endswith('_' + shape)]
                group('stone.' + family + '.' + shape, family_title + ' / ' + label, '石材', selected)
    group('stone.all', '全系列石材互认（保持形状）', '石材', all_stone)
    for shape, title in [('full', '完整石材'), ('stairs', '石材楼梯'), ('slab', '石材半砖'), ('wall', '石材墙')]:
        selected = [i for i in all_stone if (not i.endswith(('_stairs', '_slab', '_wall'))) == (shape == 'full')
                    and (shape == 'full' or i.endswith('_' + shape))]
        group('stone.shape.' + shape, title + '（跨石材系列）', '石材', selected)


    for shape, title in [('stairs', '所有楼梯'), ('slab', '所有半砖'), ('wall', '所有墙'),
                         ('fence', '所有栅栏'), ('fence_gate', '所有栅栏门'),
                         ('door', '所有门'), ('trapdoor', '所有活板门')]:
        group('shape.' + shape, title + '（跨材料）', '跨材料形状', [i for i in ids if i.endswith('_' + shape)])

    # Oxidation and wax are independent axes. Only their combined preset permits both axes to change.
    copper = [i for i in ids if ('copper' in i or 'lightning_rod' in i) and i not in ('copper_ore', 'deepslate_copper_ore', 'raw_copper_block')]
    axes = {}
    for i in copper:
        wax = i.startswith('waxed_'); core = i.removeprefix('waxed_'); stage = 'fresh'
        for prefix in ('exposed_', 'weathered_', 'oxidized_'):
            if core.startswith(prefix): stage = prefix[:-1]; core = core[len(prefix):]; break
        if core == 'copper': core = 'copper_block'
        axes[i] = (core, wax, stage)
    for core in sorted({v[0] for v in axes.values()}):
        for wax in (False, True):
            group(f'copper.oxidation.{core}.{str(wax).lower()}', '铜氧化阶段 / ' + core, '铜材', [i for i,v in axes.items() if v[:2] == (core,wax)])
        for stage in ('fresh','exposed','weathered','oxidized'):
            group(f'copper.wax.{core}.{stage}', '铜涂蜡差异 / ' + core, '铜材', [i for i,v in axes.items() if v[0] == core and v[2] == stage])
        group('copper.combined.' + core, '铜氧化与涂蜡 / ' + core, '铜材', [i for i,v in axes.items() if v[0] == core])

    group('nature.leaves', '所有树叶', '自然', [i for i in ids if i.endswith('_leaves')])
    group('nature.poplar_leaves', '杨木叶颜色', '自然', ['red_poplar_leaves','orange_poplar_leaves','yellow_poplar_leaves'])
    group('nature.sapling', '树苗', '自然', [i for i in ids if i.endswith('_sapling') and not i.startswith('potted_')])
    group('nature.potted', '盆栽植物', '自然', [i for i in ids if i.startswith('potted_')])
    group('nature.soil', '地面土壤', '自然', ['dirt','coarse_dirt','rooted_dirt','grass_block','podzol','mycelium','mud','packed_mud'])
    group('nature.sand', '沙与红沙', '自然', ['sand','red_sand'])
    group('nature.ice', '冰种类', '自然', ['ice','packed_ice','blue_ice','frosted_ice'])
    group('nature.sponge', '海绵干湿', '自然', ['sponge','wet_sponge'])
    group('nature.moss', '苔藓块', '自然', ['moss_block','pale_moss_block'])
    group('nature.moss_carpet', '苔藓地毯', '自然', ['moss_carpet','pale_moss_carpet'])
    group('nature.small_flowers', '单格花', '自然', ['dandelion','golden_dandelion','poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy','cornflower','lily_of_the_valley','wither_rose','torchflower'])
    group('nature.tall_flowers', '双格花', '自然', ['sunflower','lilac','rose_bush','peony','pitcher_plant'])
    group('nature.short_grass', '矮草与蕨', '自然', ['short_grass','fern','short_dry_grass'])
    group('nature.tall_grass', '高草与大型蕨', '自然', ['tall_grass','large_fern','tall_dry_grass'])
    for shape in ('coral','coral_block','coral_fan','coral_wall_fan'):
        for dead in (False,True):
            members = [('dead_' if dead else '') + kind + '_' + shape for kind in ('tube','brain','bubble','fire','horn')]
            group(f'nature.{shape}.{dead}', ('死' if dead else '活') + '珊瑚 / ' + shape, '自然', members)
        group('nature.coral_life.'+shape, '珊瑚种类与死活 / '+shape, '自然', [('dead_' if dead else '')+kind+'_'+shape for dead in (False,True) for kind in ('tube','brain','bubble','fire','horn')])
    for ore in ('coal','iron','copper','gold','redstone','lapis','diamond','emerald'):
        group('nature.ore.'+ore, '矿石基底 / '+ore, '自然', [ore+'_ore','deepslate_'+ore+'_ore'])
    group('functional.anvil', '铁砧损坏程度', '功能', ['anvil','chipped_anvil','damaged_anvil'])
    group('functional.froglight', '蛙明灯种类', '功能', ['ochre_froglight','verdant_froglight','pearlescent_froglight'])
    group('functional.lighting', '完整照明方块', '功能', ['glowstone','sea_lantern','shroomlight','ochre_froglight','verdant_froglight','pearlescent_froglight'])
    group('functional.skull', '地面头颅', '功能', [i for i in ids if i.endswith(('_head','_skull')) and '_wall_' not in i])
    group('functional.wall_skull', '墙式头颅', '功能', [i for i in ids if i.endswith(('_head','_skull')) and '_wall_' in i])
    return dict(version=1, minecraft='26.3', groups=groups)

if __name__ == '__main__':
    target = pathlib.Path(__file__).resolve().parents[1] / 'src/main/resources/assets/litematica_flex/catalogue.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    data = generate(sys.argv[1])
    target.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Generated',len(data['groups']),'explicit groups')
