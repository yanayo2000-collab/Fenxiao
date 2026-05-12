# Fenxiao 完整产品缺口追踪清单

> 目标：按用户给出的 11 项脑图/完整产品口径逐项补齐，避免漏项。状态只按已落代码并通过测试验证计入。

| # | 缺口 | 当前状态 | 代码/验证锚点 |
|---|---|---|---|
| 1 | 手机号注册登录体系完整化 | 已有后端基础，待继续做前端成品化与真实短信/风控增强 | `PhoneAuthService`、`PhoneVerificationCode`、`/api/distribution/auth/phone-*` |
| 2 | “上级 / 邀请码 → 对应 Linky 公会邀请码 / 公会 ID”映射落库 | 已完成后端落库、配置接口与运营后台配置管理入口：支持查询、保存上级用户对应 Linky 公会 ID/邀请码、启停状态 | `GuildAccountConfig`、`GuildAccountConfigService`、`/admin/distribution/guild-configs`、`getAdminGuildConfigs(...)`、`saveAdminGuildConfig(...)` |
| 3 | 用户绑定 Linky ID 时按“上级对应公会”精确校验 | 已实现后端校验 | `InviteBindingRegistrationService.expectedGuild(...)`、`assertEligibleForExpectedGuild(...)` |
| 4 | Linky ID 查询不到时，用户端明确提示对应公会邀请码 | 已完成绑定页用户提示：后端返回 invite code 时展示指定公会邀请码卡片 | `assertEligibleForExpectedGuild(...)`、`buildBindGuildInviteGuidance(...)`、绑定页错误卡片 |
| 5 | 批量刷新 / 定时刷新所有 Linky ID 公会归属闭环 | 已完成后台批量刷新接口与前端批量刷新入口/成功失败计数展示；定时刷新仍待补 | `refreshAllEligibility()`、`/linky-eligibility-checks/batch-refresh`、`refreshAdminLinkyEligibilityBatch(...)` |
| 6 | 用户端深层裂变数据：下级、下下级、下下下级人数 | 已有后端 home 汇总字段，待确认前端展示完整性 | `DistributionHomeResponse`、`DistributionFrontendService.getHome(...)` |
| 7 | 用户端收益按业务二级 / 三级 / 四级汇总与明细展开 | 已有后端汇总与明细，待确认前端展示完整性 | `RewardSummaryResponse`、`RewardTierSummaryItem` |
| 8 | 下级本周 / 上周钻石收入统计 | 已有用户维度周统计接口，待补团队下级维度展示/明细 | `WeeklyIncomeStatsResponse`、`/income-stats/weekly/{userId}` |
| 9 | 用户端提现历史完整展示 | 已有后端历史接口雏形，前端仍偏最近申请展示，待补完整列表 | `withdrawHistory`、`/api/distribution/withdraw-requests/{userId}` |
| 10 | 后台提现处理审计、操作人追踪、导出报表 | 已有列表/审批/拒绝/导出，待补真实 operator 审计字段 | `/admin/distribution/withdraw-requests*` |
| 11 | 公会端账号、规则配置、公会隔离、每周报表 | 周报真实聚合与 API/UI 入口已推进；规则配置入口已补；公会端账号/隔离体验待继续 | `GuildAccountConfigService.weeklyReport(...)`、`getAdminGuildWeeklyReport(...)`、公会配置管理 UI |

## 本轮刚完成

- #2 公会配置管理 UI 已补：
  - 运营后台新增“公会配置管理”工作区。
  - 支持查询现有公会配置、保存产品 + 上级用户 ID + Linky 公会 ID + 公会名称 + 公会邀请码 + 启用状态。
  - 上级用户 ID 可为空，作为默认公会配置；无上级配置时仍按后端规则回落默认公会。
  - 已接入 `getAdminGuildConfigs(...)` / `saveAdminGuildConfig(...)`，并解决前端 build 中未使用 import 的红灯。
- #4 绑定页公会邀请码提示已补：
  - 新增 `buildBindGuildInviteGuidance(...)`，从后端错误里提取 `invite code`。
  - 用户绑定 Linky ID 未命中上级对应公会时，页面不再只显示英文错误，而是展示“请先加入指定 Linky 公会”和“对应 Linky 公会邀请码”。
  - 已补前端单测覆盖普通错误与公会邀请码错误。
- #5 批量刷新前端入口已补：
  - `frontend/src/api.ts` 新增 `refreshAdminLinkyEligibilityBatch(...)`。
  - 运营后台 “Linky 资格核验” 区新增“批量刷新全部 Linky 资格”按钮。
  - UI 展示批量刷新成功数量、失败数量，并保留失败需看后台日志排查的提示。
- 公会周报接口从 `0,0,0` 占位改为真实聚合：
  - 注册用户：按 `linky_account_binding.guild_id` 且 `user_id is not null` 去重统计
  - 收入：按公会用户作为 `income_event.user_id` 聚合
  - 分佣：按公会用户作为 `reward_record.source_user_id` 聚合
- CSV 导出复用同一份 service 结果。
- 前端 API 增加 `getAdminGuildWeeklyReport(...)`。
- 运营后台增加“公会周报”查询工作区。

## 下一步优先顺序

1. 继续补 #5：挂定时刷新、补更细失败明细持久化/后台排查入口。
2. 补 #9：用户端提现历史列表，而不是只显示最近申请单。
3. 补 #10：提现审批/拒绝绑定真实 operator 与审计日志。
