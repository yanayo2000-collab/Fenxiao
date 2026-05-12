# 全项目差距与执行计划（对照 2026-05-12 分销功能脑图）

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** 对照用户提供的“分销功能”脑图，明确 Fenxiao 当前实现距离全项目完成还差什么，并给出后续开发顺序。

**Architecture:** 当前项目已经具备 Spring Boot 后端 + React/Vite 前端 + H2/MySQL 迁移结构；本计划不重写系统，而是在现有邀请码、Linky 绑定、奖励计算、提现、运营后台基础上补齐业务脑图缺口。

**Tech Stack:** Spring Boot 3、JPA、Flyway、React、Vite、Vitest、Maven。

---

## 1. 当前实现对照结论

### 1.1 已基本实现

| 模块 | 当前状态 | 证据 / 说明 |
|---|---|---|
| 分销系统网页入口 | 已有 | 前端已有 `/invite`、`/bind`、`/earnings` 与 console |
| 注册时输入分销邀请码 | 已有 | `CreateProfileRequest.inviteCode` 已强制必填；前端也已改必填 |
| 生成自己的分销邀请码 | 已有 | `issueInviteCode` / 邀请码与对外入口已存在 |
| Linky ID 唯一绑定 | 已有 | `invite_binding_registration` / `linky_account` 唯一逻辑已存在 |
| Linky ID 资格校验 | 部分到位 | 已有 `linky_account_binding`、probe client、后台刷新资格入口 |
| 收益记录 | 已有 | `RewardCalculationService`、`RewardRecord`、前后台奖励列表 |
| 提现：每周 1 次 | 已有 | `WithdrawRequestService.existsByUserIdAndRequestWeek` |
| 提现：1000 钻石起 | 已有 | `MIN_WITHDRAW_DIAMOND_AMOUNT = 1000` |
| 提现申请占用奖励 | 已有 | `withdraw_status = CLAIMED_IN_REQUEST` |
| 提现后台处理 | 已补 | approve -> `PAID_OUT`，reject -> `UNCLAIMED` |
| 运营后台基础 | 已有 | 分销概览、奖励、绑定关系、提现申请、Linky 资格核验 |
| Linky webhook / replay 留痕 | 已有 | webhook logs / replay records / 详情抽屉能力已存在 |
| 后端/前端基础回归 | 已通过 | 最近验证：`mvn -q test`、`npm test -- --run`、`npm run build` 通过 |

---

### 1.2 部分实现，但和脑图口径不完全一致

| 模块 | 当前状态 | 差距 |
|---|---|---|
| 手机号注册 | 未按脑图完整实现 | 当前更多是 userId / WhatsApp / Linky 账号接入，不是手机号注册登录体系 |
| Linky 公会校验 | 有最小 probe | 脑图要求“是否为上级对应公会”，当前更多是“是否命中我方公会”，还缺“上级 -> 对应公会邀请码/公会 ID”的精确映射 |
| 查询不到时弹窗提醒公会邀请码 | 后端可判定，前端提示不足 | 当前有资格核验/失败提示，但还没有面向用户的“对应 Linky 公会邀请码”弹窗闭环 |
| 所有 ID 最终根据爬取数据确认 | 有脚本/日志基础 | 还缺稳定调度、批量刷新、失败重试、结果报表 |
| 佣金层级 | 技术三层存在 | 脑图业务口径是二级 10%、三级 2%、四级 0.5%；当前本地 seed 是 level1=15%、level2=5%、level3=2%，比例不一致，且没有 0.5% |
| 收益时间 | 规则表有 effective 时间 | 还缺后台可配置永久/活动期比例提升的完整 UI 和操作流程 |
| 裂变人数 | 直接下级较完整 | 脑图要求团队总人数、下级、下下级、下下下级；当前用户端主要是直接团队，深层统计不足 |
| 裂变收益 | 奖励记录有层级 | 用户端还没有按业务二级/三级/四级做成清晰汇总与展开 |
| 下级本周/上周钻石 | 用户 profile 有累计收入 | 缺上周/本周时间窗统计和明细表 |
| 提现记录 | 申请与后台处理已有 | 用户端还缺提现历史列表；后台还缺更完整处理备注/审计/导出 |
| 公会端 | 后台基础存在 | 还不是“公会端”：缺公会账号、公会规则、公会报表、公会邀请码配置 |

---

### 1.3 尚未实现 / 明显缺口

1. 手机号注册登录体系。
2. 上级用户对应 Linky 公会的配置模型。
3. 用户绑定 Linky ID 时，基于“上级对应公会”的精确校验。
4. 查询不到 Linky ID 时，向用户弹窗展示对应 Linky 公会邀请码。
5. 批量爬取 / 定时刷新所有 ID 的公会归属。
6. 按脑图重设默认分佣比例：10% / 2% / 0.5%。
7. 公会端规则配置 UI：提成比例、收益时间、活动期比例提升。
8. 用户端深层裂变人数统计：下级、下下级、下下下级。
9. 用户端深层收益汇总：二级、三级、四级收益。
10. 下级本周/上周钻石收入统计。
11. 二级收益按具体 ID 展开，并展示总提成、上周提成、本周提成。
12. 用户端提现历史。
13. 每周裂变提现报表。
14. 每个公会裂变信息报表。
15. 报表导出。
16. 公会端账号/权限/公会隔离。
17. 提现处理审计与操作人追踪。
18. 全量验收环境和上线清单。

---

## 2. 推荐剩余研发顺序

### Phase A：先把业务规则口径对齐

#### Task A1：调整默认分佣规则为脑图口径

**Objective:** 把默认规则从当前本地口径对齐到业务脑图：10% / 2% / 0.5%。

**Files:**
- Modify: `src/main/java/com/fenxiao/rule/service/LocalRewardRuleSeeder.java`
- Modify: seed / migration 或测试数据中涉及默认比例的地方
- Test: `src/test/java/com/fenxiao/reward/service/RewardCalculationServiceTest.java`
- Test: `src/test/java/com/fenxiao/reward/service/RewardMvpFlowTest.java`

**Acceptance:**
- level1 技术层 = 业务二级 = 10%。
- level2 技术层 = 业务三级 = 2%。
- level3 技术层 = 业务四级 = 0.5%。
- 收益页展示文案按业务二级/三级/四级，而不是技术 level1/2/3。

#### Task A2：明确并落库“上级对应公会”映射

**Objective:** 给用户/邀请码/产品增加对应 Linky 公会信息，用于绑定时校验。

**Files:**
- Create/Modify migration: add guild fields to suitable table, e.g. invite code issue / ownership / dedicated guild config
- Modify: `InviteCodeIssueService` 或新增 `GuildConfigService`
- Test: service + controller tests

**Acceptance:**
- 可以知道某个邀请码 / 上级用户对应哪个 Linky 公会 ID / 公会邀请码。
- 后续 Linky ID 校验不再只判断“我方公会”，而能判断“是否为上级对应公会”。

---

### Phase B：补齐 Linky 绑定与公会校验闭环

#### Task B1：用户绑定 Linky ID 时校验上级对应公会

**Objective:** `/bind` 或注册绑定流程中，按上级对应公会校验 Linky ID。

**Files:**
- Modify: `LinkyRegistrationEligibilityService`
- Modify: `InviteBindingRegistrationService`
- Modify: `DistributionController`
- Test: `InviteBindingRegistrationServiceTest`、`DistributionControllerTest`

**Acceptance:**
- Linky ID 属于上级对应公会：绑定成功。
- Linky ID 属于其他公会：拒绝并说明。
- Linky ID 查询不到：返回用户可读提示，包含对应 Linky 公会邀请码。

#### Task B2：用户端弹窗提醒对应 Linky 公会邀请码

**Objective:** 当 Linky ID 查询不到 / 未绑定公会时，用户端明确提示应绑定的公会邀请码。

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/api.ts`
- Test: `frontend/src/App.test.tsx`

**Acceptance:**
- 用户提交 Linky ID 后，如果后端返回未命中，页面显示清晰弹窗/提示。
- 提示包括对应 Linky 公会邀请码或下一步操作。

#### Task B3：批量刷新所有 Linky ID 公会归属

**Objective:** 公会归属最终以爬取数据确认，系统需要批量刷新能力。

**Files:**
- Create: batch service / command / scheduled job
- Modify: `LinkyAccountBindingRepository`
- Test: service tests

**Acceptance:**
- 可批量扫描已绑定 Linky ID。
- 每个 ID 刷新公会归属、状态、更新时间、失败原因。
- 失败可回看。

---

### Phase C：补用户端裂变数据看板

#### Task C1：深层团队人数统计

**Objective:** 用户端展示团队总人数、下级、下下级、下下下级人数。

**Files:**
- Modify: `DistributionFrontendService`
- Modify DTO: `DistributionHomeResponse` / new response
- Modify: `frontend/src/App.tsx`
- Test: backend + frontend tests

**Acceptance:**
- 能看到 3 层下线人数。
- 能看到团队总人数。

#### Task C2：下级明细增加收入时间窗

**Objective:** 下级明细展示具体 ID、总钻石、上周钻石、本周钻石。

**Files:**
- Create query for income events / profile income aggregates
- Modify: `TeamMemberItem`
- Modify: `DistributionFrontendService`
- Test: `DistributionFrontendControllerTest`

**Acceptance:**
- 每个直接下级展示：ID、总钻石、上周钻石、本周钻石。

#### Task C3：收益按业务二级/三级/四级汇总与明细

**Objective:** 用户端收益页展示所有总收益、二级收益、三级收益、四级收益；二级收益可展开 ID 明细。

**Files:**
- Modify: `RewardRecordRepository`
- Modify: `DistributionFrontendService`
- Modify: `frontend/src/App.tsx`
- Test: backend + frontend tests

**Acceptance:**
- 用户能清楚看到二级/三级/四级收益。
- 二级收益可看到具体 ID、总提成、上周提成、本周提成。

---

### Phase D：补提现完整运营闭环

#### Task D1：用户端提现历史

**Objective:** 用户可查看历史提现申请。

**Files:**
- Add: `GET /api/distribution/withdraw-requests/{userId}`
- Modify: `WithdrawRequestService`
- Modify: `frontend/src/App.tsx`
- Test: backend + frontend tests

**Acceptance:**
- 用户看到申请单号、金额、状态、申请时间、处理备注。

#### Task D2：后台提现处理 UI

**Objective:** 后台提现申请列表支持通过/驳回按钮。

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/App.tsx`
- Test: `frontend/src/App.test.tsx` / `api.test.ts`

**Acceptance:**
- 运营可直接在后台处理提现申请。
- 操作后列表状态刷新。

#### Task D3：提现处理审计

**Objective:** 记录提现是谁处理、何时处理、备注是什么。

**Files:**
- Modify: `WithdrawRequestService`
- Modify: audit repository/service
- Test: service/controller tests

**Acceptance:**
- approve/reject 都写 audit log。
- 后台可按 withdraw 模块查看审计。

---

### Phase E：公会端与报表

#### Task E1：公会配置模型

**Objective:** 建立公会端核心模型：公会 ID、名称、公会邀请码、产品、负责人等。

**Acceptance:**
- 系统可配置多个公会。
- 上级/邀请码可映射到公会。

#### Task E2：公会端规则配置

**Objective:** 公会端可配置提成比例和收益时间。

**Acceptance:**
- 可配置默认永久规则。
- 可配置活动期临时规则。
- 规则可按公会/产品维度生效。

#### Task E3：每周裂变提现报表

**Objective:** 公会端查看每周提现报表。

**Acceptance:**
- 按周筛选。
- 包含用户 ID、Linky ID、公会、申请钻石、状态、处理备注。
- 支持导出。

#### Task E4：每个公会裂变信息报表

**Objective:** 公会端查看各公会裂变信息。

**Acceptance:**
- 按公会统计用户数、各层级人数、总钻石、总提成、提现金额。
- 支持导出。

---

## 3. 当前距离“全项目完成”的判断

按脑图定义的全项目范围看，当前不是空壳，已经完成了核心底座和一部分主链闭环：

- 邀请码 / 绑定 / 收益 / 提现 / 后台基础：约 60%-70%。
- 用户端深层裂变数据和收益明细：约 40%。
- Linky 公会精确校验：约 50%，缺“上级对应公会”映射与用户提示闭环。
- 公会端规则、爬取、报表：约 20%-30%。
- 运营可用后台：约 65%-75%，但提现 UI、审计、报表还要补。

综合判断：

> 当前距离“脑图全项目完成”大约还差 35%-45%。  
> 如果目标是先上线 MVP，可优先补 Phase A-D；如果目标是完整公会端产品，还必须补 Phase E。

---

## 4. 推荐下一步最小批次

下一批建议直接做 4 件事：

1. 对齐佣金比例：10% / 2% / 0.5%。
2. 建立“上级/邀请码 -> Linky 公会邀请码/公会 ID”的映射。
3. 用户绑定 Linky ID 时，失败弹窗提示对应公会邀请码。
4. 后台提现申请 UI 支持通过/驳回。

这四件事做完后，用户端主链会非常接近脑图要求；再往后就是深层报表和公会端。