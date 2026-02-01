# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

GregTech CEu: Modern (GTM) 是一个 Minecraft 模组，将 GregTech CE Unofficial 移植到现代 Minecraft 版本。支持 MinecraftForge 1.20.1 和 NeoForge 1.21.1+。

- **Mod ID**: `gtceu`
- **Maven Group**: `com.gregtechceu.gtceu`
- **Java 版本**: 17

## 构建命令

```bash
# 构建模组
./gradlew build

# 运行开发客户端
./gradlew runClient

# 运行游戏测试
./gradlew runGameTestServer

# 格式化代码（提交前必须执行）
./gradlew spotlessApply

# 生成覆盖率报告
./gradlew jacocoTestReport
```

## 代码风格

Spotless 强制执行格式化。导入顺序：
1. `com.gregtechceu`
2. `com.lowdragmc`、`net`
3. 其他包
4. `java`
5. `javax`
6. 静态导入

使用 `spotless:off` / `spotless:on` 注释可禁用特定代码块的格式化。

## 架构

### 入口点
`GTCEu.java` 是主模组类（`@Mod("gtceu")`）。使用 `DistExecutor` 路由到 `ClientProxy` 或 `CommonProxy`。

### 源代码集
- `main` - 核心模组代码
- `client` - 仅客户端的渲染/UI
- `test` - GameTest 测试
- `extra` / `clientExtra` - 仅开发环境的模组

### 包结构 (`src/main/java/com/gregtechceu/gtceu/`)
- `api/` - 附属模组开发者的公共 API（机器、配方、材料、能力）
- `common/` - 共享游戏逻辑（方块、物品、机器、覆盖板、配方）
- `client/` - 客户端渲染和 UI
- `config/` - 配置处理
- `data/` - 数据生成
- `integration/` - 模组集成（JEI、REI、EMI、AE2、Create、KubeJS 等）
- `forge/` - Forge 特定实现

### 关键架构模式
- **代理模式**: 通过 `ClientProxy`/`CommonProxy` 进行端特定初始化
- **能力系统**: Forge 能力用于方块实体交互
- **机器特性**: 可组合的行为（配方逻辑、物品栏、能量）
- **覆盖板系统**: 方块侧面的模块化附件
- **配方系统**: 基于类型的配方管理与集中查找

## 测试

使用 Minecraft 的 GameTest 框架。测试需要在 `src/test/resources/data/gtceu/structures/` 中有结构模板。

```java
@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class MyTest {
    @GameTest(template = "empty")
    public static void testSomething(GameTestHelper helper) {
        // 测试逻辑
        helper.succeed();
    }
}
```

游戏内运行测试: `/test run gtceu:<batch>` 或 `/test runAll`

## IDE 设置

**必需**: IntelliJ IDEA 的 [Lombok 插件](https://plugins.jetbrains.com/plugin/6317-lombok)
**推荐**: [Minecraft Development 插件](https://plugins.jetbrains.com/plugin/8327-minecraft-development)

## 模组集成

本模组集成了：JEI/REI/EMI（配方查看器）、AE2、Create、KubeJS、Curios、ComputerCraft、FTB 系列模组、GameStages。使用 `GTCEu.Mods` 类进行运行时检测。

## 待办事项 (TODO)

---

### 整合包定制功能

以下是为空岛整合包定制的新功能：

#### 低压蒸汽矿物筛子

| 项目 | 设计 |
|------|------|
| **名称** | 低压蒸汽矿物筛子 (LP Steam Ore Sieve) |
| **定位** | 空岛整合包中，从手动筛网到电力自动化的过渡 |
| **机器形态** | 单方块机器 |
| **能源** | 蒸汽，LP 约 6 mB/t，HP 约 12 mB/t |
| **槽位** | 3 输入槽（原料+筛网+电路） + 1 输出槽 |
| **自动化** | 支持物品管道输入/输出 |
| **配方来源** | 启动时将 Ex Nihilo: Sequentia 配方转换为 GTCEu 配方 |
| **筛网机制** | 可更换，不消耗耐久，筛网决定可筛材料和产出 |
| **速度机制** | 机器等级决定速度 |
| **音效** | 使用模组现有音效 |
| **基类** | SimpleSteamMachine |

**Ex Nihilo: Sequentia 配方系统：**
- 配方类型: `exnihilosequentia:sifting`
- 筛网类型: string, flint, iron, diamond, emerald, netherite
- 配方结构: `{ input, result, rolls: [{ chance, mesh }] }`

**实现进度：**

| 步骤 | 状态 | 说明 |
|------|------|------|
| 1. 创建 Ex Nihilo 集成模块 | ✅ 完成 | `integration/exnihilo/ExNihiloModule.java` |
| 2. 创建配方转换器骨架 | ✅ 完成 | `integration/exnihilo/SieveRecipeLoader.java` |
| 3. 创建 GTRecipeType | ✅ 完成 | `SIEVE_RECIPES` 在 `GTRecipeTypes.java` |
| 4. 注册机器 | ✅ 完成 | `STEAM_ORE_SIEVE` 在 `GTMachines.java` |
| 5. 添加合成配方 | ✅ 完成 | `MetaTileEntityLoader.java` |
| 6. 添加本地化 | ✅ 完成 | `MachineLang.java` + `zh_cn.json` |
| 7. 生成模型文件 | ✅ 完成 | blockstates + models |

**相关 Commit：**
- `e28d76ac5` - Add Steam Ore Sieve machine with Ex Nihilo: Sequentia integration

**未完成事项：**
- [ ] 配方转换器 - 将 ENS sifting 配方转换为 GT SIEVE_RECIPES（见下方技术方案）
- [ ] 矿物筛独有纹理图 - 替换默认蒸汽机器纹理

**后续扩展计划：**
- [ ] 高压蒸汽矿物筛子（已注册，需测试）
- [ ] 不同电压的电力矿物筛子
- [ ] 多方块结构版本（高级特性）
- [ ] GTCEu 材料制成的筛网（需修改 Ex Nihilo: Sequentia）

#### ENS 配方转换器技术方案

**ENS 版本**: 5.0.0 (Forge 1.20.1)

**ENS 公共 API**:
```java
// 获取所有筛滤配方
List<SiftingRecipe> recipes = ExNihiloRegistries.SIEVE_REGISTRY.getRecipeList();

// SiftingRecipe 结构
recipe.getInput()       // Ingredient - 输入材料
recipe.getDrop()        // ItemStack - 输出物品
recipe.isWaterlogged()  // boolean - 干/湿筛
recipe.getRolls()       // List<MeshWithChance> - 筛网概率列表

// MeshWithChance 结构
roll.getMesh()          // MeshType 枚举 (STRING, FLINT, IRON, DIAMOND, EMERALD, NETHERITE)
roll.getChance()        // float 概率 (0.0 - 1.0)
```

**配方加载时机**: ENS 在 `RecipesUpdatedEvent`（客户端）和 `ServerStartingEvent`（服务端）时加载配方

**实现步骤**:
1. 在 `build.gradle` 添加 ENS 作为可选编译依赖
2. 创建事件监听器，在 ENS 加载配方后执行转换
3. 遍历 `ExNihiloRegistries.SIEVE_REGISTRY.getRecipeList()`
4. 将每个 `SiftingRecipe` 转换为 GTM 的 `SIEVE_RECIPES` 格式

**转换逻辑伪代码**:
```java
for (SiftingRecipe recipe : ExNihiloRegistries.SIEVE_REGISTRY.getRecipeList()) {
    for (MeshWithChance roll : recipe.getRolls()) {
        GTRecipeTypes.SIEVE_RECIPES.recipeBuilder(generateId(recipe, roll))
            .inputItems(recipe.getInput())
            .notConsumable(getMeshItem(roll.getMesh()))
            .chancedOutput(recipe.getDrop(), (int)(roll.getChance() * 10000), 0)
            .duration(200)
            .EUt(4)
            .save(consumer);
    }
}
```

**关键类位置** (ENS 源码):
- `novamachina.exnihilosequentia.common.registries.ExNihiloRegistries`
- `novamachina.exnihilosequentia.common.registries.SiftingRegistry`
- `novamachina.exnihilosequentia.world.item.crafting.SiftingRecipe`
- `novamachina.exnihilosequentia.world.item.crafting.MeshWithChance`
- `novamachina.exnihilosequentia.world.item.MeshType`

---

### 原模组遗留问题

#### 未实现的功能

- [ ] 成就系统 - 伤害相关成就 (`EntityDamageUtil.java:57,84,104`)
- [ ] 成就系统 - 披风解锁成就 (`GTCapes.java:32`)
- [ ] Central Monitor 中央监控器 (`MiscRecipeLoader.java:284`)
- [ ] Access Interface 访问接口 (`MetaTileEntityLoader.java:412`)
- [ ] Crafting Station 合成站 (`MetaTileEntityLoader.java:734`)
- [ ] Replication System 复制系统 (`MetaTileEntityLoader.java:851`)
- [ ] Clipboard 剪贴板 (`CraftingRecipeLoader.java:99`)
- [ ] Foam Sprayer 泡沫喷射器 (`AssemblerRecipeLoader.java:82`)
- [ ] 更多针对特定状况的药物 (`GTItems.java:2103`)

### 配方相关

- [ ] 食物 → 甲烷配方 (`SeparationRecipes.java:83`)
- [ ] 其他类型泥土的配方 (`SeparationRecipes.java:121`)
- [ ] 石英台阶与板材冲突修复 (`StoneMachineRecipes.java:255`)
- [ ] Purpur 材料 (`StoneMachineRecipes.java:328`)
- [ ] 海晶石材料 (`StoneMachineRecipes.java:345`)
- [ ] 回收配方时间和能耗调整 (`RecyclingRecipes.java:43`)
- [ ] 沥青配方 (`MachineRecipeLoader.java:339`)
- [ ] 石头类型标签 (`MachineRecipeLoader.java:1265`)
- [ ] 更多食物配方 (`MachineRecipeLoader.java:1350`)
- [ ] 硝基苯燃料平衡性调整 (`FuelRecipes.java:316`)
- [ ] 燃料配方清理 (`FuelRecipes.java:56`)

### 材料危害属性

- [ ] 乙醇中毒效果 (`OrganicChemistryMaterials.java:415`)
- [ ] 氢氟酸中毒效果 (`FirstDegreeMaterials.java:1216`)
- [ ] 四氧化锇中毒效果 (`FirstDegreeMaterials.java:1470`)
- [ ] 其他材料危害属性 (`ElementMaterials.java:172`)
- [ ] 放射性废料输出 (`AirScrubberRecipes.java:25`)

### 地图集成 (FIXME)

- [ ] Xaero 小地图第二层渲染透明问题 (`OreVeinElementRenderer.java:60`)
- [ ] 客户端缓存 hack 修复 (`GTClientCache.java:91`)
- [ ] 等待 Xaero API 发布后迁移 Mixin (`WorldMapSessionMixin.java`, `MinimapFBORendererMixin.java`, `HighlighterRegistryMixin.java`)
- [ ] JourneyMap 方块绘制问题 (`JourneymapRenderer.java:158`)

### UI/显示问题

- [ ] 蒸汽物品总线背景蒸汽化 (`SteamItemBusPartMachine.java:42`)
- [ ] HPCA 组件自定义显示名称 (`HPCAComponentPartMachine.java:115`)
- [ ] 便携扫描仪电压/EU 显示明确化 (`PortableScannerBehavior.java:338`)
- [ ] 盔甲第二材料颜色使用 (`GTArmorItem.java:42`)
- [ ] 虚空覆盖板网格行为确定 (`ItemVoidingCover.java:121`, `FluidVoidingCover.java:125`)

### 其他技术问题

- [ ] DummyMachineBlockEntity 代理参数修复 (`DummyMachineBlockEntity.java:38`)
- [ ] 物品管道纹理添加 (`ItemPipeType.java:89`)
- [ ] 流体管道纹理添加 (`FluidPipeType.java:93`)
- [ ] AE2 KeyStorage 定期清理 (`KeyStorage.java:30`)
- [ ] 酿造逻辑改为静态配方 (`BreweryLogic.java:43`)
- [ ] 控制器外观方块 CTM 问题 (`GTResearchMachines.java:200`)
- [ ] CC:Tweaked 中央监控器物品传输 (`CentralMonitorPeripheral.java:47`)
- [ ] 矿脉元素泛化到所有层类型 (`OreVeinElement.java:32`)
- [ ] 战利品表额外掷骰 (`ChestGenHooks.java:87`)

---

## 开发日志

### 2026-02-01 阶段性成果总结

#### 已完成

1. **蒸汽矿物筛子机器框架**
   - LP/HP 两个版本已注册，可在游戏中放置
   - 配方类型 `SIEVE_RECIPES` 已创建
   - 合成配方、本地化、模型文件均已完成
   - 相关 commit: `e28d76ac5`

2. **Mod 元数据定制 (BingoTech Fork)**
   - `gradle.properties`: 更新描述、URL、issue tracker
   - `mods.toml`: 更新作者信息为 "Bingo888, Claude, ..."
   - JAR 文件名: `gtceu-1.20.1-7.5.0.jar`

3. **ENS 集成研究** (完成 API 分析)
   - 确认 ENS 5.0.0 Forge 1.20.1 版本兼容
   - 找到公共 API: `ExNihiloRegistries.SIEVE_REGISTRY.getRecipeList()`
   - 记录了完整的配方结构和加载时机
   - 技术方案已写入上方 "ENS 配方转换器技术方案" 章节

#### 尝试但未成功

- **运行时配方转换器** (已回滚)
  - 尝试通过反射解析 ENS JSON 配方 - 失败
  - 原因: 配方类型名称错误、反射访问复杂
  - 新方案: 使用 ENS 公共 API 直接获取配方对象

#### 下一步工作

1. 在 `build.gradle` 添加 ENS 作为可选编译依赖
2. 创建事件监听器调用 ENS API
3. 实现配方转换逻辑
4. 测试配方在游戏中的显示和执行

---

### 2026-02-01 KubeJS 接口与 EMI 概率显示修复

#### 需求变更

用户决定放弃静态配方方案，改为提供 KubeJS 接口让整合包作者自定义配方：
- 3 输入槽：原料（消耗）、筛网（不消耗）、编程电路（不消耗）
- 1 输出槽：支持概率输出
- 每个配方单一产物，通过编程电路区分同原料+筛网的不同产出

#### 实现修改

1. **GTRecipeTypes.java** - 修改槽位配置
   ```java
   // 从 6 输出改为 1 输出
   .setMaxIOSize(3, 1, 0, 0)
   ```

2. **删除 SieveRecipes.java** - 移除静态配方文件

3. **GTRecipes.java** - 移除 `SieveRecipes.init()` 调用

#### EMI 概率不显示问题

**症状**：
- 配方 JSON 中 chance 值正确（如 1000 = 10%）
- 游戏内概率功能正常工作
- 但 EMI 配方查看器中不显示概率百分比和 tooltip

**调试过程**：
1. 检查导出的配方 JSON - chance 值正确
2. 对比研磨机（Macerator）配方 - 概率显示正常
3. 分析 GTRecipeWidget、ItemRecipeCapability、Content 等类
4. 发现研磨机有 `.setEUIO(IO.IN)` 而矿物筛没有

**根本原因**：
缺少 `.setEUIO(IO.IN)` 配置导致 UI 槽位绑定失败，即使是蒸汽机器也需要此配置。

**修复**：
```java
public final static GTRecipeType SIEVE_RECIPES = register("ore_sieve", STEAM)
        .setMaxIOSize(3, 1, 0, 0)
        .setEUIO(IO.IN)  // 关键！添加这行修复 EMI 概率显示
        .setProgressBar(GuiTextures.PROGRESS_BAR_SIFT, UP_TO_DOWN)
        .setSound(new ExistingSoundEntry(SoundEvents.SAND_PLACE, SoundSource.BLOCKS));
```

#### 创建的 SKILL 文件

1. **gtm-sieve-kubejs** (`~/.claude/skills/gtm-sieve-kubejs/SKILL.md`)
   - 面向整合包作者
   - KubeJS 配方添加指南
   - 包含完整示例和参数说明

2. **gtm-recipe-type-guide** (`~/.claude/skills/gtm-recipe-type-guide/SKILL.md`)
   - 面向 Mod 开发者
   - GTRecipeType 必要配置清单
   - 记录 `.setEUIO(IO.IN)` 的重要性

#### 关键经验教训

| 配置项 | 必要性 | 说明 |
|--------|--------|------|
| `.setMaxIOSize()` | 必须 | 定义槽位数量 |
| `.setEUIO(IO.IN)` | **必须** | 即使蒸汽机器也需要，否则 EMI 概率不显示 |
| `.setProgressBar()` | 推荐 | 进度条纹理 |
| `.setSound()` | 推荐 | 机器音效 |

#### KubeJS 配方示例

```js
ServerEvents.recipes(event => {
    event.recipes.gtceu.ore_sieve("gravel_flint_0")
        .itemInputs("minecraft:gravel")
        .notConsumable("exnihilosequentia:flint_mesh")
        .circuit(0)
        .chancedOutput("minecraft:flint", 1000, 0)  // 10% 概率
        .duration(200)
        .EUt(4)
})
