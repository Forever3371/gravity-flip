# Gravity Flip（成就翻转重力）

Minecraft **1.21.1** / **Fabric** 模组：**任何一个玩家完成一个成就，全服所有人的重力方向随机翻转**，翻完一直保持到下一次成就。

## 效果

- 完成成就 → 全员的重力方向随机变成 **6 个方向之一**（上下左右前后）
- **翻完一直保持**，不会自动恢复，直到下一个人完成成就再重新抽
- 聊天框会播报"谁完成了什么成就、重力变成了哪个方向"
- 输入映射、落地判定、视角都会跟着重力走（这是借用实现本身就带的）

## 重力实现来自哪里

重力本体（真重力向量、6 方向、输入映射、碰撞与落地判定、客户端同步）**借用自
[GravityChanger-1.21.1](https://github.com/FugLord77/GravityChanger-1.21.1)**，
它又是 [qouteall/GravityChanger](https://github.com/qouteall/GravityChanger) 的分支。

- **许可证：MIT**（原版权归 Gaider10 等所有，见 `LICENSE-GravityChanger.txt`）
- 原作者：Gaider10、Fusion Flux、qouteall、CyborgCabbage、Mortimer Kerman 及贡献者
- 本项目在其基础上**新增**了"成就触发 + 随机方向 + 全服同步"这一层，并去掉了物品、重力板方块、药水效果等不需要的内容

> 这也意味着：**它的成熟度与坑都由上游决定**。上游最后更新于 2024-10，对应 MC 1.21。

## 配置

`config/gravity_flip.properties`：

```properties
# 总开关
enabled=true

# 是否在聊天框播报是谁完成了什么成就、重力变成了哪个方向
show_message=true

# 是否只影响和完成成就者同一维度的玩家（false = 全服所有人，含其它维度）
same_dimension_only=false
```

## 依赖

**需要额外安装两个模组**（上游就依赖它们，我暂时没能去掉）：

- [Cardinal Components API](https://modrinth.com/mod/cardinal-components-api)（存重力方向）
- [Cloth Config API](https://modrinth.com/mod/cloth-config)（上游的配置界面用了它）

客户端和服务端**都要装**这三个。

## 测试

```bash
./gradlew runGameTest    # 5 项 gametest
./gradlew build          # 构建
```

gametest 覆盖：

- **设置重力方向后读回来一致**（证明借用的重力实现真的生效，不是摆设）
- 随机方向一定是六个方向之一（抽 100 次）
- **完成成就会触发翻转**
- **同一个成就只触发一次**（一个成就有多个 criterion，必须去重）
- 关掉开关后不再触发

### 一个查过字节码确认的设计

玩家上线时，进度是通过私有的 `loadProgressMap` **直接塞进去的**，
**不经过** `award`（yarn 叫 `grantCriterion`）——所以**不需要持久化去重**，
不会出现"每次登录都误触发一次翻转"。

## 已知问题

- **依赖两个外部模组**：`include`（把依赖打进 jar）在这个项目上没生效——
  Loom 把 maven 坐标从 `dev.onyxstudios` 重定向到 `org.ladysnake`，打包环节没跟上。
  后续可以改成 Fabric 的 `AttachmentRegistry`（自带同步）来彻底去掉 CCA。
- **旁观者不参与**这条没有自动化测试覆盖（gametest 的 mock 玩家切不了旁观者模式）。

## 许可

MIT。重力实现部分版权归 Gravity Changer 原作者，详见 `LICENSE-GravityChanger.txt`。
