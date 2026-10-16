# 更新日志

## [1.0.1] - 2025

### 修复

- **播报里的成就名显示成资源路径**：原来用的是 `advancementId.getPath()`，
  玩家看到的是 `[重力翻转] xxx 完成了成就 story/root` 这种内部 id。
  现在改为显示成就的标题（比如"获得木头"），取不到标题时才退回资源 id。
- 成就名用金色高亮，和前后文区分开。

## [1.0.0] - 2025

### 新增

- **完成成就 → 全服重力随机翻转**：任何一个玩家完成一个成就，所有人的重力方向随机变成 6 个方向之一。
- **翻完保持**：不设自动恢复，一直保持到下一个人完成成就。
- 聊天框播报是谁完成了什么成就、重力变成了哪个方向。
- 配置项 `enabled` / `show_message` / `same_dimension_only`。
- gametest 5 项：重力方向可设置且读回一致、随机方向合法、完成成就触发翻转、
  同一成就只触发一次、关掉开关不触发。

### 重力实现的来源

重力本体借用 [GravityChanger-1.21.1](https://github.com/FugLord77/GravityChanger-1.21.1)
（MIT，原作者 Gaider10、Fusion Flux、qouteall、CyborgCabbage、Mortimer Kerman 等），
它支持真重力向量、6 个方向、输入映射跟随重力、落地判定与客户端同步。
本项目在此之上新增成就触发层，并移除了重力改变物品、重力板方块、重力药水效果等不需要的内容。

### 已知问题

- 仍需额外安装 Cardinal Components API 与 Cloth Config API（`include` 打进 jar 没生效）。
- 旁观者不参与这一条没有自动化测试覆盖。
