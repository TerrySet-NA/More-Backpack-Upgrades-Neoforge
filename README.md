# More Backpack Upgrades (更多背包升级)

More Backpack Upgrades is an addon for Sophisticated Backpacks that bridges the gap between your portable storage and your Refined Storage network.

(更多背包升级 是 Sophisticated Backpacks (精妙背包) 的附属模组，它架起了你的便携背包与 Refined Storage (精致存储) 网络之间的桥梁。)

Forget about manually emptying your backpack after a mining trip. With these new upgrades, your items are teleported directly into your digital storage system, no matter where you are or across dimensions!

(忘掉每次挖矿归来都要手动清空背包的繁琐吧。有了这些全新的升级卡，无论你身处何地或跨越维度，物品都能直接传送进你的数字存储系统中！)

## ✨ Features (功能特性) 
This mod adds two powerful "Dimensional" upgrades. To link an upgrade to your network, simply Shift + Right-Click on a Refined Storage Controller with the upgrade item in hand.

(本模组添加了兩种强大的“次元”升级卡。要将升级卡绑定到你的网络，只需手持升级卡对着 Refined Storage 的 控制器 (Controller) 按 Shift + 右键 即可。)

## ⚠️ General Upgrade Rules (通用升级规则)
- **Strictly One per Type (同类型唯一):** Only one Dimensional Upgrade of each type can be installed in a single backpack.  
  *(每个背包仅限安装一个同类型的次元升级。)*
- **Tier Incompatibility (无法兼容下属版本):** Mutually exclusive with vanilla/standard counterparts and lower tiers (cannot install basic or advanced versions alongside the dimensional version).  
  *(与该类型的原版升级及其下属低级版本互斥，无法同时共存安装。)*

## 🧲 Dimensional Magnet Upgrade (次元磁吸升级)

* **Function (功能):** Pulls items from the ground directly into your linked RS system.
*(将地面上的掉落物直接吸入绑定的 RS 系统中。)*
* **Smart Fallback & Priority (智能回退与优先级):** Features a priority toggle (RS Network First ⇄ Backpack First). If the RS network is full or offline, items will safely fall back to the backpack without loss.
*(支持在 GUI 中一键切换【RS 网络优先】或【精妙背包优先】。当网络已满或离线时，会自动降级存入背包，防止物品丢失。)*
* **Configurable (可配置):** Customizable range and advanced filter settings (whitelist/blacklist/backpack contents match).
*(支持自定义吸附范围以及进阶过滤器设置（白名单/黑名单/背包匹配）。)*

## 📥 Dimensional Pickup Upgrade (次元拾取升级)

* **Function (功能):** Items you pick up normally (by walking over them) are instantly routed into your linked RS system.
*(你正常拾取（走过）的物品会瞬间发送到绑定的 RS 系统中。)*
* **Smart Fallback & Priority (智能回退与优先级):** Toggleable between RS Network First and Backpack First. When prioritizing RS, full networks route items to the backpack; when prioritizing backpack, overflow automatically streams back to RS.
*(支持【RS 网络优先】与【背包优先】双向切换。网络优先时，溢出物品存入背包；背包优先时，背包塞满后的多余物资自动传送回 RS。)*
* **Anti-Conflict (防冲突机制):** Automatically coordinates with the Dimensional Magnet Upgrade to prevent redundant processing.
*(背包内同时安装磁吸升级时，会自动协同避免重复路由。)*

## 🍖 Dimensional Feeding Upgrade (次元喂食升级)

* **Function (功能):** Automatically feeds the player by retrieving food directly from the linked RS network across dimensions.
*(当饥饿度下降时，跨维度直接从绑定的 RS 网络中调取食物自动喂食玩家。)*
* **Priority Routing (优先级路由):** Choose whether to consume food from the remote RS network first or from your backpack's local storage.
*(可自由选择优先消耗远程 RS 网络中的储备粮，还是优先消耗随身背包中携带的食物。)*
* **Configurable (可配置):** Set hunger thresholds, food filters, and feeding conditions.
*(支持设置喂食饥饿度阈值、食物过滤器与自动进食策略。)*

## 🔄 Dimensional Refill Upgrade (次元补货升级)

* **Function (功能):** Keeps target slots (such as torches, building blocks, ammunition, or tools) constantly restocked directly from the linked RS network.
*(当主副手或快捷栏的物料（火把、建筑方块、箭矢等）消耗时，直接跨维度从绑定的 RS 网络补齐。)*
* **Seamless Supply (无缝补给):** Never run out of building materials or arrows while exploring or constructing.
*(探险或大型建筑时无需手动翻箱倒柜，随身物料时刻保持满额状态。)*
* **Smart Priority (智能优先级):** Configurable to draw supplies from the remote RS network or local backpack first.
*(支持自由切换优先从 RS 调货补齐，或是优先消耗背包内备用物资。)*

## 📦 Dimensional Restock Upgrade (次元取货升级)

* **Function (功能):** Takes items from target external containers (chests, machines) or non-linked RS networks with a single Shift-Right-Click.
*(Shift+右键外部箱子、容器或非记录的 RS 网络时，快速提取目标物品。)*

## 🗳️ Dimensional Deposit Upgrade (次元卸货升级)

* **Function (功能):** Shift-Right-Click external containers or different RS networks to rapidly deposit filtered materials.
*(Shift+右键外部容器或不同的 RS 网络时，快速卸下战利品与物资。)*

## 🌊 Dimensional Pump Upgrade (次元液泵升级)

* **Function (功能):** Pumps fluids into or dispenses fluids out of your linked RS network across dimensions (in-world sources, fluid pipes, tanks, or player hand items).
*(直接在世界中抽吸/排放液体，与绑定的 RS 存储网络进行跨维度的流体交互（支持世界水源、储罐管道与手持容器）。)*
* **Virtual Remote Tank (虚拟无限储罐):** Even if your backpack has **no tank upgrade installed**, the pump upgrade can utilize your entire RS network as a massive virtual fluid reservoir!
*(即便随身背包**没有安装任何储罐升级**，也能直接把整套基地 RS 网络当成无限随身储罐直接抽水与倒水！)*
* **Comprehensive Control (全方位模式控制):** Features in/out direction toggles, world/container/hand interaction switches, and RS-First vs Backpack-First fluid routing.
*(完整继承官方抽入/排出方向、世界方块/手持容器/相邻储罐交互开关，并提供 RS 网络与随身储罐的优先级分流。)*

## 🧪 Dimensional Alchemy Upgrade (次元炼金升级)

* **Function (功能):** Automatically detects player or nearby entity states (such as low health, on fire, drowning, falling, mining, or suffering debuffs) and consumes potions, splash potions, golden apples, or milk directly from the linked RS network.
*(智能监测玩家或周围实体的生理状态（残血、着火、溺水、坠落、挖掘、获得负面效果等），跨维度直接从绑定的 RS 网络中调取药水、喷溅药水、金苹果或牛奶自动使用。)*
* **Zero Inventory Clutter (无损空瓶/铁桶回收):** When potions or milk are consumed, empty glass bottles and iron buckets are automatically recycled back into your linked RS network instead of cluttering your backpack.
*(喝完药水或牛奶后产生的空玻璃瓶与铁桶会**优先自动回传至基地 RS 网络**，完全不占随身背包格子。)*
* **Intelligent Priority (智能优先级):** Configurable to consume potions from the remote RS network first (freeing up backpack inventory slots) or from the backpack first.
*(支持设置优先消耗远端 RS 网络的药水库存（身上无需携带任何沉重的药水瓶），亦可设置为优先消耗背包随身携带的消耗品。)*

## 🛠️ Requirements (前置需求) 
**To use this mod, you need the following installed (要使用此模组，你需要安装以下内容) :**

- **Minecraft 1.21.1 Neoforge**
- **Sophisticated Backpacks**
- **Sophisticated Core**
- **Refined Storage**

## 📝 Configuration (配置)
**You can configure the range of the Dimensional Magnet Upgrade in the config file (你可以在配置文件中调整次元磁吸升级的范围) :**
```
config/MoreBackpackUpgrades-common.toml
```

## 📝 Credits & License (鳴謝與授權)
- **Original Author (原作者)**: 空枝霁雨 (KongZhiJiYu)
- **Original Project (原專案)**: More Backpack Upgrades ([Modrinth](https://modrinth.com/mod/more-backpack-upgrades))
- **License (授權)**: LGPL-2.1
- **Modifications (修改)**: 本版本包含基於原專案的修改與調整 (Modified by TerrySet)。
