<h3 align="center">
  <img src="./src/main/resources/assets/leaves/icon.png" width="100" alt="Logo" style="border-radius: 15px;"/><br/>
  LeavesHack-Addon for <a href="https://meteorclient.com">Meteor Client</a>
</h3>

<p align="center">
    <a href="https://github.com/MrBZBZ/LeavesHack/stargazers"><img src="https://img.shields.io/github/stars/MrBZBZ/LeavesHack?colorA=363a4f&colorB=b7bdf8&style=for-the-badge" alt=""></a>
    <a href="https://github.com/MrBZBZ/LeavesHack/issues"><img src="https://img.shields.io/github/issues/MrBZBZ/LeavesHack?colorA=363a4f&colorB=f5a97f&style=for-the-badge" alt=""></a>
    <a href="https://github.com/MrBZBZ/LeavesHack/contributors"><img src="https://img.shields.io/github/contributors/MrBZBZ/LeavesHack?colorA=363a4f&colorB=a6da95&style=for-the-badge" alt=""></a>
  <a href="https://github.com/MrBZBZ/LeavesHack/releases"><img src="https://img.shields.io/github/downloads/MrBZBZ/LeavesHack/total?style=for-the-badge&labelColor=363a4f&color=f5c2e7" alt=""></a>
</p>

<p align="center">
  <i>致力于改善无政府服务器的生存体验</i><br/>
  <i>适用Grim反作弊</i>
</p>

bilibili: https://space.bilibili.com/1932462042  
神秘群聊欢迎吹水：1076456572  
点击链接加入群聊【LeavesHack-user-group】：https://qm.qq.com/q/YwajlOYRGw
## 注意事项
- 这个东西是我玩3c3u写的，开源只是顺手，我想写什么就写什么，readme的更新有一定滞后性，我尽量把每个模块的教程写上
- 懒得升级了，什么时候2b2t的客户端从1.21.1升到更高版本再升级
- Meteor本身不支持中文渲染，可以通过下载现代化UI和模组菜单，打开现代化UI的文本引擎再关闭Meteor的CustomFont解决
- 理论上AntiAntiXray可以绕过其他服务器的假矿或其他反矿透插件，如果腐竹不给开挂，而且你使用这个插件导致被封号等等后果请自负
- 如果你觉得这个插件不错可以请我吃个肠粉，咕咕嘎嘎
  <div align="center">
  <img src="114514.jpg" style="border-radius: 15px;" alt="神秘二维码" width="400">
  </div>
## 功能
截至目前更新的功能，基本上默认参数就能在3c使用：  

|        模块         | 描述                                    | 注意事项                                |  
|:-----------------:|---------------------------------------|-------------------------------------|  
|   AntiAntiXray    | 雷达扫描矿透                                | 不装 Baritone 开功能崩端                   |  
|       Aura        | 可在 3c3u 使用的不卡脚的杀戮                     | 无                                   |  
|    AutoAnchor     | 自动重生锚                                 | 无                                   |
|   AutoArmorPlus   | 自动穿甲（主要为了和 FireworkElytraFly 联动自动穿鞘翅） | 无                                   |  
|     AutoCity      | 自动挖脚（顺手的事，记住咱是生存端）                    | 无                                   |  
|    AutoCrystal    | 自动水晶                                  | 无                                   |
|     AutoLogin     | 自动登录                                  | 无                                   |  
|  AutoPlaceBlock   | 自定义范围方块放置                             | 无                                   |  
| AutoRefreshTrade  | 自动刷新村民交易的附魔书                          | 无                                   |  
|     AutoTorch     | 自动放火把 (白天可用 + 无光区域渲染)                 | 无                                   |  
|     AutoTree      | 自动种树                                  | 左键选择目标方块，手持树苗时工作                    |  
| FireworkElytraFly | 在鞘翅飞行时自动使用烟花 (含甲飞)                    | 无                                   |  
|    LegitNoFall    | 自动落地水                                 | 95% 的成功率，自动检测下界，不要在没有副手图腾的情况下信任这个模块 |  
|     NukerPlus     | 绕过 3c3u 的 Nuker                       | 无                                   |  
|    ModuleList     | 重写模块列表渲染                              | 无                                   |  
|    PacketMine     | 绕过 3c3u 的发包挖掘（可双挖）                    | 无                                   |  
|   PistonCrystal   | 活塞水晶                                  | 无                                   |
|      Printer      | 投影打印机                                 | 测试中，不装投影开功能崩端                       |  
|   ScaffoldPlus    | 自动搭路 (可自适应半砖)                         | 放下半砖的时候不稳定，斜着走的时候慢一点不然容易坠机          |

# 部分教程：
## AntiAntiXray  
- **高效矿石扫描**  
  在可配置范围内扫描方块，步长配置为2时速度更快

- **支持矿石类型**
    - 铁矿
    - 金矿
    - 钻石
    - 青金石  
（我只添加了我常用的）
- **3D 可视化**  
  扫描到的矿石以透明立方体标记，方便观察。

- **Baritone 集成（可选）**  
  可将矿石位置传递给 Baritone 自动挖矿。

- **实时进度显示**  
  在聊天框显示扫描进度百分比。

<img src="screen.png">

| 设置                            | 类型 | 说明                                          | 默认值   |
|-------------------------------|----|---------------------------------------------|-------|
| Range                         | 整数 | 玩家周围扫描半径（越大越慢，我推荐12）                        | 15    |
| Step                          | 整数 | 遍历步长（如果要扫假矿必须拉1，拉2交叉扫速度更快但是缺失精度，只能在假区块环境使用） | 2     |
| Baritone                      | 布尔 | 是否将矿石位置传递给 Baritone                         | false |
| AutoMine                      | 布尔 | 是否在扫描完毕时自动使用 Baritone 挖矿                    | false |                      |
| Iron / Gold / Diamond / Lapis | 布尔 | 是否渲染并追踪该矿石                                  | true  |

1. 将此MOD和MeteorClient一起添加到你的模组列表中，记得保证你添加了Baritone
2. 设置所需 **Range**、**Step** 和矿石类型。
3. 如果使用 Baritone，启用 `Baritone` 选项。
4. 模块会自动扫描玩家周围方块，并在 3D 中标记，同时可更新 Baritone 的矿石位置。
5. 扫描进度与完成提示会显示在聊天框。
6. 扫描时玩家走动不会改变扫描中心的位置，扫描完毕后如果启用了自动挖矿，将自动使用 Baritone 挖矿，请保证 Baritone 选项也打开。
7. 模块关闭后会取消一切扫描结果和 Baritone 行为，再次打开会重新开始扫描  
## AutoRefreshTrade
 * 使用这个模块必须搭配packetmine来挖方块，否则挖不掉讲台
 * 模块的放置逻辑可以绕过Grim的检测，在严反也可以使用
 * 使用时面对讲台，会渲染面前的位置作为讲台的位置，不能保证捡到讲台，建议多带一点
 * 附魔的格式用中文或者英文都可以，但是中文MeteorClient的列表可能渲染不了，建议用英文
 * 如果想用中文，请参考注意事项
 * 附魔支持列表多个匹配，当附魔为所需附魔并且等级大于等于Level所设置的值时模块视为找到目标
 * 注意如果你在刷精准采集这类1级附魔，不需要主动降低level的值，模块有自动检测max的逻辑，当附魔已经为可能的最大附魔且在需求列表中，也会视为找到目标
 * 比如：你选择了锋利和经验修补以及耐久，level设置为5，寻找锋利时只有5级才符合要求，但当村民出现经验修补时，因为1级就是精修的最大等级，模块会自动视为找到目标，如果刷出耐久三同理
<img src="screen2.png">  
<img src="screen3.png">



