# Fenxiao

多国家三级分销系统网页端 MVP。

## 当前形态

本仓库现在包含两部分：

- `src/main/java`：Spring Boot 后端服务
- `frontend/`：Vite + React 网页前端

已经具备：
- 分销 profile 创建
- 三级关系绑定
- 收益事件入库
- 奖励计算 / 冻结 / 解冻
- 前台首页 / 团队 / 奖励明细接口
- 网页端 MVP 页面骨架
- Docker / docker-compose / Nginx 基础部署资产

---

## 技术栈

### 后端
- Java 21
- Spring Boot 3
- Spring Web / Spring Data JPA
- MySQL
- Flyway

### 前端
- React
- TypeScript
- Vite

---

## 本地开发

### 1. 启动后端（MySQL）
```bash
mvn spring-boot:run
```

### 2. 启动后端（本地演示 / H2）
```bash
ADMIN_TOKEN=change-this-admin-token \
INTERNAL_DISTRIBUTION_TOKEN=change-this-internal-token \
PROFILE_CREATE_TOKEN=change-this-profile-create-token \
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

默认后端地址：
- `http://localhost:8080`

### 3. 启动前端
```bash
cd frontend
npm install
cp .env.example .env.local
npm run dev
```

如果不使用 `.env.local`，也可以直接临时传入：
```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

默认前端开发地址：
- `http://localhost:5173`

---

## 前端页面

当前网页端 MVP 包含：

1. 分销档案创建 / 接入页
2. 分销首页
3. 直属团队列表
4. 奖励明细列表
5. 运营登录 / 概览
6. 奖励查询（带分页）
7. 风险事件查询与处理（处理 / 忽略 / 冻结 / 解冻）
8. 关系链查询
9. 最近处理记录 / 审计列表

---

## 生产配置

后端生产配置文件：
- `src/main/resources/application-prod.yml`

关键环境变量：

```bash
DB_URL=jdbc:mysql://mysql:3306/fenxiao?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
DB_USERNAME=fenxiao
DB_PASSWORD=change-this-db-password
INTERNAL_DISTRIBUTION_TOKEN=change-this-internal-token
ADMIN_TOKEN=change-this-admin-token
PROFILE_CREATE_TOKEN=change-this-profile-create-token
WEB_ALLOWED_ORIGINS=http://localhost:8088,http://localhost:5173,http://127.0.0.1:5173
SERVER_PORT=8080
```

说明：
- `prod` 环境不再内置数据库默认值，部署时必须显式注入。
- 后台 / 内部接入 / profile 创建三个 token 也都必须显式配置。
- 网页端后台登录使用 `ADMIN_TOKEN` 作为登录口令，但后台运营接口本身已改为 session-only，会先换取短期后台会话再访问。

---

## Docker 部署

部署文件目录：
- `deploy/Dockerfile.backend`
- `deploy/docker-compose.yml`
- `deploy/.env.example`
- `deploy/nginx.conf`

### 1. 构建前端
```bash
cd frontend
npm install
npm run build
```

### 2. 准备部署环境变量
```bash
cd /tmp/Fenxiao
cp deploy/.env.example deploy/.env
```

按实际环境修改 `deploy/.env`，至少要改：
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `ADMIN_TOKEN`
- `INTERNAL_DISTRIBUTION_TOKEN`
- `PROFILE_CREATE_TOKEN`

### 3. 启动整套服务
```bash
cd deploy
docker compose up --build
```

如果你的环境只有旧命令，也可以用：
```bash
docker-compose up --build
```

启动后默认地址：
- 前端：`http://localhost:8088`
- 后端：`http://localhost:8080`
- MySQL：`localhost:3306`

---

## 健康检查

```bash
curl http://localhost:8080/actuator/health
```

---

## 当前运营可用能力

当前这版已经不是只读控制台，而是可给运营日常使用的 MVP：

- 后台使用 session 登录，登录后自动拉取首屏概览与最近审计
- 奖励列表支持状态/时间/分页查询
- 风险事件支持状态/时间/分页查询
- 风险动作支持处理、忽略、冻结用户、解冻用户
- 风险动作已升级为逐条备注 + 二次确认，减少误操作
- 风险动作会联动刷新奖励、关系链与最近处理记录
- 审计日志可直接按 `risk_event` / `relation` / 全部筛选查看
- 关系链查询支持人工修正一级上级，并在提交前看到 before / after 预览
- 非法动作会被后端拦截，前端也会按状态禁用不该点击的按钮

详细操作说明见：`docs/operations-handbook.md`

Linky 对接协议见：`docs/linky-integration.md`

当前 Linky webhook 现状：
- 已记录每次请求的 token / signature / 时间窗校验结果
- 已落库 webhook 日志，支持按订单 / 用户 / 处理状态回看
- 已有显式 replay record，能区分 `FIRST_SEEN` / `REPLAYED`
- 后台最小排查接口：`GET /admin/distribution/linky-webhook-logs`
- 后台 replay 回看接口：`GET /admin/distribution/linky-replay-records`

## 一期后续增强方向

1. Linky 正式收益同步接入（已补适配入口、字段别名兼容、基础签名校验与时间窗防重放，待对接真实上游字段与回放明细策略）
2. token 轮换/吊销
3. 关系人工修正的 before/after 审计增强与复核说明
4. 更细的后台权限模型
5. 网页端体验优化与登录体系接入
6. 提交整理与上线前 checklist 固化
