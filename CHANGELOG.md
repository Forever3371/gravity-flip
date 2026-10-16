# 更新日志

## [1.2.0] - 2025

### 变更

- **默认不再启用**：模组加载后不会碰任何人的重力，必须由 OP 在游戏内执行
  `/gravityflip on` 才会开始工作。
- **新增 /gravityflip 命令**（权限等级 2）：
  - `on` / `off` —— 启用 / 禁用（只对本次运行有效，不写回配置）
  - `trigger` —— 手动翻一次（不用等成就就能看效果）
  - `status` —— 查看当前状态
- 删掉了配置项 `same_dimension_only`：之前配置里有它但代码没实现，留着是坑。

### 注意

- 启用状态**不写回配置文件**，每次重启都要重新执行一次 `on`。
  想让它重启后自动启用，把配置里的 `enabled` 改成 `true`。
- 从 1.1.x 升级时**旧配置文件的值会保留**。如果你的 `config/gravity_flip.properties`
  里还是 `enabled=true`，需要手动改成 `false` 或删掉文件让它重新生成。

### 测试

- gametest 5 项，新增 `disabledByDefaultBlocksTrigger`：
  验证默认（未启用）状态下完成成就真的不会触发、重力方向不变。
- 每个测试自己设置开关，不再依赖测试之间的执行顺序（之前是靠顺序侥幸通过）。
- 生产验证：真实服务器上用数据包执行 `/gravityflip on` / `off`，断言命令返回成功。

## [1.1.1] - 2025

### 修复

- **崩服 bug**：\AdvancementProgress.getProgressText()\ 在 requirements 还没初始化时会返回 null，
  而我在 \ward\ 返回后立刻就去取它，导致 \NullPointerException\ 直接把服务端 tick 崩掉
  （错误报告：Ticking entity）。现在进度文本改成自己数 completed/total，
  不再依赖那个会返回 null 的方法。
- **给整个触发逻辑加异常保护**：成就触发只是附加玩法，任何意外都只记日志、不再影响服务器运行。

### 测试

- gametest 改成**端到端**：真的调用 \ward\ 去完成真实成就（\husbandry/balanced_diet\、
  \story/root\）的 criterion，验证 Mixin 注入生效、重力真的被翻、第二个 criterion 再翻一次、
  重复 award 不重复触发。

## [1.1.0] - 2025

### 变更

- **改成每个 criterion 完成就触发一次**，不再等整个成就做完：
  - 吃一次苹果（均衡饮食的 40 种食物之一）就翻一次
  - 走进一个新群系（探索的时光的一部分）就翻一次
  - 整个成就做完了同样会翻（那本身也是完成了一个 criterion）
- **去重键从"成就 id"改成"成就 id + criterion 名"**：
  否则均衡饮食那 40 种食物只会翻第一次。
- 播报文案区分两种情况：整个成就完成时说"完成了成就 XX"，
  只完成一部分时说"完成了 XX 的一部分（3/40）"，进度用原版自带的进度文本。

### 说明

- 触发频率明显变高了（这是需求本身决定的）：任何一次进度推进都会翻一次重力。
  如果觉得太频繁，可以考虑加冷却，但目前没加。

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
