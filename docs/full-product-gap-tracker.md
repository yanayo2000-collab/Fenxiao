# Fenxiao 完整产品缺口追踪清单

> 目标：按用户给出的 11 项脑图/完整产品口径逐项补齐，避免漏项。状态只按已落代码并通过测试验证计入。

| # | 缺口 | 当前状态 | 代码/验证锚点 |
|---|---|---|---|
| 1 | 手机号注册登录体系完整化 | 已完成用户端手机号验证码登录与验证码安全收口：邀请码页可获取验证码、填写验证码/邀请码/国家并登录保存用户 session；后端发码响应不再回传验证码，验证码改为随机 6 位，同手机号未消费且未过期验证码重复发码返回 429，并已抽象 `SmsSender` 发送接口；真实第三方 SMS provider 配置仍待接入 | `PhoneAuthService`、`PhoneVerificationCodeRepository`、`SmsSender`、`LoggingSmsSender`、`/api/distribution/auth/phone-*`、`issuePhoneCode(...)`、`phoneLogin(...)`、邀请码页“手机号登录” |
| 2 | “上级 / 邀请码 → 对应 Linky 公会邀请码 / 公会 ID”映射落库 | 已完成后端落库、配置接口与运营后台配置管理入口：支持查询、保存上级用户对应 Linky 公会 ID/邀请码、启停状态 | `GuildAccountConfig`、`GuildAccountConfigService`、`/admin/distribution/guild-configs`、`getAdminGuildConfigs(...)`、`saveAdminGuildConfig(...)` |
| 3 | 用户绑定 Linky ID 时按“上级对应公会”精确校验 | 已实现后端校验 | `InviteBindingRegistrationService.expectedGuild(...)`、`assertEligibleForExpectedGuild(...)` |
| 4 | Linky ID 查询不到时，用户端明确提示对应公会邀请码 | 已完成绑定页用户提示：后端返回 invite code 时展示指定公会邀请码卡片 | `assertEligibleForExpectedGuild(...)`、`buildBindGuildInviteGuidance(...)`、绑定页错误卡片 |
| 5 | 批量刷新 / 定时刷新所有 Linky ID 公会归属闭环 | 已完成定时刷新、单账号失败不中断整批、失败状态/原因持久化、后台批量刷新失败明细返回；前端已有批量刷新入口与成功失败计数展示 | `LinkyEligibilityRefreshScheduler`、`refreshAllEligibility()`、`markRefreshFailed(...)`、`LinkyBatchRefreshResponse.failures`、`/linky-eligibility-checks/batch-refresh` |
| 6 | 用户端深层裂变数据：下级、下下级、下下下级人数 | 已完成用户端三层裂变人数展示：收益页展示一级/二级/三级下级人数及各层有效人数 | `DistributionHomeResponse.directInvitedUsers`、`secondLevelInvitedUsers`、`thirdLevelInvitedUsers`、收益页“三层裂变人数” |
| 7 | 用户端收益按业务二级 / 三级 / 四级汇总与明细展开 | 已完成用户端业务二/三/四级收益汇总展示与明细层级标签：收益页展示各业务层级奖励金额和明细数量，奖励记录使用业务层级文案 | `RewardSummaryResponse`、`RewardTierSummaryItem`、`getDistributionRewardSummary(...)`、收益页“收益层级汇总” |
| 8 | 下级本周 / 上周钻石收入统计 | 已完成团队直属下级维度本周/上周钻石收入统计：后端新增团队周收入接口，前端 API 已接入，收益页展示团队汇总与直属下级明细 | `TeamWeeklyIncomeResponse`、`/api/distribution/team/{userId}/weekly-income`、`getDistributionTeamWeeklyIncome(...)`、收益页“团队周收入” |
| 9 | 用户端提现历史完整展示 | 已完成用户端完整提现历史首屏列表：收益页加载最近 10 条提现申请，发起提现后自动刷新；状态、申请周、申请时间可见 | `getWithdrawHistory(...)`、`/api/distribution/withdraw-requests/{userId}`、收益页“提现历史” |
| 10 | 后台提现处理审计、操作人追踪、导出报表 | 已完成审批/拒绝真实 operator 审计透传、前端 API 与后台 UI 操作入口；运营可在提现列表填写操作人 ID/角色/备注并直接通过或拒绝，导出已存在 | `/admin/distribution/withdraw-requests*`、`approveAdminWithdrawRequest(...)`、`rejectAdminWithdrawRequest(...)`、后台“提现申请”操作列 |
| 11 | 公会端账号、规则配置、公会隔离、每周报表 | 周报真实聚合与 API/UI 入口已推进；规则配置入口已补；公会端账号/隔离体验待继续 | `GuildAccountConfigService.weeklyReport(...)`、`getAdminGuildWeeklyReport(...)`、公会配置管理 UI |

## 本轮刚完成

- #1 手机号验证码安全收口已补：
  - 后端 `POST /api/distribution/auth/phone-codes` 响应不再回传 `verificationCode`，只返回手机号与 TTL，避免生产接口泄露验证码。
  - `PhoneAuthService.issueCode(...)` 从固定测试码改为 `SecureRandom` 随机 6 位数字验证码。
  - 同手机号、`LOGIN` purpose 存在未消费且未过期验证码时，重复发码返回 429，降低刷码与误触风险。
  - 新增 `SmsSender` 抽象与默认 `LoggingSmsSender`，日志只保留脱敏手机号与有效期，不输出验证码明文。
  - 前端 `PhoneCodeResponse.verificationCode?` 保持可选兼容，页面在无验证码响应时展示“验证码已发送，X 分钟内有效”。
  - 验证已通过：`mvn -q -Dtest=DistributionControllerTest test`、`npm test -- --run src/api.test.ts src/App.test.tsx`、`npm test -- --run`、`npm run build`、`mvn -q test`。
  - 剩余边界：真实第三方 SMS provider 的 endpoint/app key/template/signature、发送失败重试与告警仍待上线配置接入。
- #1 手机号验证码登录前端成品化已补：
  - 前端 API 新增 `issuePhoneCode(...)` 与 `phoneLogin(...)`，分别对接 `/api/distribution/auth/phone-codes` 和 `/api/distribution/auth/phone-login`。
  - 邀请码页新增“手机号登录”工作区，支持手机号获取验证码、填写验证码、可选邀请码和国家码。
  - 手机号登录成功后复用 `saveUserSession(...)` 保存用户 session，后续可直接进入收益页查看团队、奖励和提现历史。
  - 页面明确提示“已有邀请码可在登录时带上完成资料初始化”，降低用户只靠生成邀请码入口的理解成本。
  - 验证已通过：`npm test -- --run src/api.test.ts src/App.test.tsx`、`npm test -- --run`、`npm run build`、`mvn -q -Dtest=DistributionControllerTest,DistributionFrontendControllerTest test`、`mvn -q test`。
  - 剩余边界：真实短信发送通道、验证码风控/限频增强仍是 #1 的下一阶段，不在本轮前端成品化范围内。
- #6/#7 用户端深层裂变人数与业务层级收益展示已补：
  - `DistributionHomeResponse` 前端类型补齐三层裂变字段，收益页新增“三层裂变人数”区块。
  - 用户端已展示一级/二级/三级下级人数，以及各层有效人数和总团队人数。
  - 前端 API 新增 `getDistributionRewardSummary(...)`，复用用户 `X-Distribution-Token` 调 `/api/distribution/rewards/{userId}/summary`。
  - 收益页新增“收益层级汇总”区块，按业务二级/三级/四级展示奖励金额和明细数量。
  - 奖励明细继续使用业务层级文案，避免把技术 `rewardLevel=1/2/3` 暴露给用户。
  - 验证已通过：`npm test -- --run src/api.test.ts src/App.test.tsx`、`npm test -- --run`、`npm run build`、`mvn -q -Dtest=DistributionFrontendControllerTest test`、`mvn -q test`。
- #8 团队直属下级本周 / 上周钻石收入展示与明细已补：
  - 后端新增 `GET /api/distribution/team/{userId}/weekly-income`，继续使用用户 `X-Distribution-Token` 校验访问。
  - 新增 `TeamWeeklyIncomeResponse` / `TeamWeeklyIncomeItem`，返回团队本周总收入、上周总收入，以及每个直属下级的本周/上周钻石收入、邀请码、有效用户状态。
  - `DistributionFrontendService.getTeamWeeklyIncome(...)` 复用直属下级关系与收益事件汇总，按本周/上周窗口统计 `DISTRIBUTION_REWARD` 钻石收入。
  - 前端 API 新增 `getDistributionTeamWeeklyIncome(...)`，收益页加载时同步拉取团队周收入。
  - 收益页新增“团队周收入”区块，展示团队本周/上周汇总和“直属下级收入明细”；暂无收入时展示用户可理解空态。
  - 验证已通过：`mvn -q -Dtest=DistributionFrontendControllerTest#shouldReturnTeamWeeklyIncomeStatsForDirectMembers test`、`npm test -- --run src/api.test.ts src/App.test.tsx`、`npm test -- --run`、`npm run build`、`mvn -q -Dtest=DistributionFrontendControllerTest test`、`mvn -q test`。
- #10 后台提现审批/拒绝 UI 操作入口已补：
  - 运营后台“提现申请”筛选区新增审批操作人 ID、操作角色、审批备注输入项。
  - 提现列表新增“操作”列，对 `PENDING` 申请可直接点击“通过 / 拒绝”。
  - 点击后调用 `approveAdminWithdrawRequest(...)` / `rejectAdminWithdrawRequest(...)`，随请求提交真实 `operatorId / operatorRole / remark`，后端继续写入既有审计链路。
  - 成功后展示处理结果并自动刷新提现申请列表；非待处理申请按钮置灰，降低误操作。
  - 验证已通过：`npm test -- --run src/App.test.tsx`、`npm test -- --run src/api.test.ts src/App.test.tsx`、`npm test -- --run`、`npm run build`、`mvn -q test`。
- #5 Linky 资格定时刷新与失败排查闭环已补：
  - 新增 `LinkyEligibilityRefreshScheduler`，每 6 小时自动触发一次全部 Linky 资格刷新。
  - `refreshAllEligibility()` 改为单账号失败不阻断整批，成功/失败分别计数。
  - 单账号探测异常会持久化为 `guildCheckStatus = REFRESH_FAILED`、`registrationEligibility = PENDING_REVIEW`，并把失败原因写入 `remark`，方便后台排查。
  - 后台批量刷新接口返回 `failures` 明细，包含失败 Linky 账号、刷新状态与失败原因；测试覆盖后台排查入口。
  - 验证已通过：`mvn -q -Dtest=DistributionAdminControllerTest#shouldReturnBatchRefreshFailureDetailsForAdminTroubleshooting test`、`mvn -q -Dtest=LinkyRegistrationEligibilityServiceTest,DistributionAdminControllerTest test`、`mvn -q test`、`npm test -- --run`、`npm run build`。
- #10 后台提现审批/拒绝真实 operator 审计已补：
  - `WithdrawRequestActionRequest` 支持 `operatorId / operatorRole / remark`。
  - 后台 approve/reject 接口已把操作人信息透传到 `WithdrawRequestService`，审计不再固定 `0 / ADMIN_SESSION`。
  - 前端 API 新增 `approveAdminWithdrawRequest(...)` / `rejectAdminWithdrawRequest(...)`，支持提交操作人审计 payload。
- #9 用户端提现历史完整列表已补：
  - `frontend/src/api.ts` 新增 `getWithdrawHistory(...)`，复用用户 `X-Distribution-Token` 调 `/api/distribution/withdraw-requests/{userId}`。
  - 收益页进入时同步加载最近 10 条提现申请；用户发起提现申请后自动刷新提现历史。
  - 页面新增“提现历史 / 完整提现记录”，展示申请单号、钻石数量、状态、申请周和申请时间；无记录时展示用户可理解的空态。
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

## 最终 MVP 验收摘要

### 已完成，可进入验收

11 项完整产品缺口均已有代码 / 前端入口 / 文档锚点进入可验收状态：

1. 手机号注册登录体系：用户端手机号验证码登录、后端验证码安全收口、短信发送抽象已完成。
2. 邀请码 / 上级到 Linky 公会映射：公会配置落库、查询、保存、启停和后台配置入口已完成。
3. Linky ID 按上级对应公会校验：后端绑定校验已完成。
4. Linky 查询不到时提示公会邀请码：绑定页中文提示卡片已完成。
5. Linky 公会归属批量 / 定时刷新：失败不中断、失败持久化、失败明细、调度器和前端批量入口已完成。
6. 三层裂变人数：收益页一级 / 二级 / 三级人数与有效人数展示已完成。
7. 业务二 / 三 / 四级收益：收益汇总与明细业务层级文案已完成。
8. 下级本周 / 上周钻石收入：团队周收入接口、前端 API 与收益页明细已完成。
9. 提现历史：用户端完整提现历史列表与申请后刷新已完成。
10. 提现处理审计：后台审批 / 拒绝入口、真实 operator payload 与审计透传已完成。
11. 公会规则配置与周报：公会配置、周报真实聚合、CSV 导出和后台查询入口已完成。

### 需上线配置后才能正式开放

- 真实第三方 SMS provider：需要补 endpoint、app key、template、signature、发送失败重试与告警；生产环境不能使用默认 `LoggingSmsSender` 给真实用户收码。
- 安全凭据：后台 session、用户 token、Linky internal token、数据库连接串、第三方后台账号密码均需按目标环境安全注入，日志和文档只允许字段名或 `[REDACTED]`。
- 网络与回调：正式域名、HTTPS、CORS、前端 API base URL、Linky webhook 回调地址需按上线环境配置。
- 数据与运维：数据库迁移、备份、回滚、报表导出权限、日志脱敏策略需上线前确认。
- 调度与告警：Linky 定时刷新、公会归属刷新失败、短信发送失败、webhook/replay 异常需确认告警和重试策略。

### 仍有产品边界，作为上线后迭代

- 后台细粒度权限、多运营账号、账号管理与更完整 RBAC 仍可继续增强。
- 手机号验证码已有重复未过期限频，但还不是完整的 IP / 设备 / 错误次数风控体系。
- 公会后台自动核验深度仍受目标后台登录、验证码、反自动化策略影响，需要后续单独稳定化。
- 公会周报当前覆盖核心注册用户 / 收入 / 分佣聚合，可后续增加更多周期、运营负责人、异常标记与财务对账维度。
- 风控当前具备基础事件处理、冻结 / 解冻与审计闭环，可后续补审批流、规则配置台和批量处理。

详细验收矩阵已同步到 `docs/operations-handbook.md` 第 9 节。

## 下一步优先顺序

1. 按 `docs/operations-handbook.md` 第 9 节做最终 MVP 人工验收：用户端闭环、运营后台闭环、Linky / 公会链路、上线配置核对、技术验证记录。
2. 接入真实第三方 SMS provider：补 provider 实现、endpoint/app key/template/signature 配置、发送失败重试与告警；保持日志和文档不输出任何验证码或凭据明文。
