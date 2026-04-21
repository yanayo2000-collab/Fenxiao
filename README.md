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
ADMIN_TOKEN=your-admin-token INTERNAL_DISTRIBUTION_TOKEN=your-internal-token PROFILE_CREATE_TOKEN=your-profile-create-token mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

默认后端地址：
- `http://localhost:8080`

### 2. 启动前端
```bash
cd frontend
npm install
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

---

## 生产配置

后端生产配置文件：
- `src/main/resources/application-prod.yml`

关键环境变量：

```bash
DB_URL=jdbc:mysql://mysql:3306/fenxiao?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
DB_USERNAME=fenxiao
DB_PASSWORD=fenxiao123
INTERNAL_DISTRIBUTION_TOKEN=change-me
ADMIN_TOKEN=change-me-admin
PROFILE_CREATE_TOKEN=change-me-profile-create
WEB_ALLOWED_ORIGINS=http://localhost:8088
SERVER_PORT=8080
```

---

## Docker 部署

部署文件目录：
- `deploy/Dockerfile.backend`
- `deploy/docker-compose.yml`
- `deploy/nginx.conf`

### 1. 构建前端
```bash
cd frontend
npm install
npm run build
```

### 2. 启动整套服务
```bash
cd deploy
export ADMIN_TOKEN=your-admin-token
export INTERNAL_DISTRIBUTION_TOKEN=your-internal-token
export PROFILE_CREATE_TOKEN=your-profile-create-token
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

## 一期后续增强方向

1. 后台管理页面 UI
2. Linky 正式收益同步接入
3. token 轮换/吊销
4. 风险事件后台处理流转
5. 关系人工修正与审计增强
6. 网页端体验优化与登录体系接入
