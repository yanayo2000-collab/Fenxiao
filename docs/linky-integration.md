# Linky 收益同步接入协议

## 1. 目标
这份文档用于说明 Fenxiao 当前如何接入 Linky 收益事件，以及上游在调用时需要满足哪些字段、请求头和签名规则。

当前这版已经支持：
- Linky 专用入口
- 字段别名兼容
- 基础签名校验
- 时间窗防重放
- 收益事件幂等处理
- webhook 请求日志落库
- 显式 replay record 指纹去重追踪
- 后台按订单 / 用户 / 处理状态回看 webhook 日志
- 后台按订单 / 用户回看 replay record

当前这版还未完成：
- 上游最终字段协议定稿
- 多版本签名协议兼容
- 前端直接回看 Linky 日志 / replay 记录

---

## 2. 接口地址

### Linky 专用入口
`POST /internal/distribution/linky/income-events`

### 鉴权头
必须带：
- `X-Internal-Token`
- `X-Linky-Timestamp`
- `X-Linky-Signature`

---

## 3. 请求体字段

### 当前主字段
```json
{
  "linkyOrderId": "linky-order-1001",
  "userId": 24102,
  "incomeAmount": 120.50,
  "currencyCode": "USD",
  "paidAt": "2026-04-21T13:30:00"
}
```

### 当前兼容别名
Fenxiao 当前已兼容以下别名：

- `orderId` / `externalOrderId` -> `linkyOrderId`
- `memberId` / `beneficiaryUserId` -> `userId`
- `commissionAmount` / `amount` -> `incomeAmount`
- `currency` -> `currencyCode`
- `settledAt` / `paidTime` -> `paidAt`

也就是说，下面这种上游请求当前也能接受：

```json
{
  "orderId": "linky-order-1001",
  "memberId": 24102,
  "commissionAmount": 120.50,
  "currency": "USD",
  "settledAt": "2026-04-21T13:30:00"
}
```

---

## 4. 签名规则

### 4.1 签名串拼接顺序
当前服务端签名串为：

```text
timestamp.linkyOrderId.userId.incomeAmount.currencyCode.paidAt
```

其中：
- `timestamp` 来自请求头 `X-Linky-Timestamp`
- `linkyOrderId` 去掉前后空格
- `userId` 为整型数字
- `incomeAmount` 使用十进制字符串，例如 `120.50`
- `currencyCode` 转大写
- `paidAt` 使用 `ISO_LOCAL_DATE_TIME`，例如 `2026-04-21T13:30:00`

### 4.2 签名算法
- 算法：`HmacSHA256`
- 输出：`Base64 URL-safe without padding`

### 4.3 示例
待签名 payload：

```text
2026-04-22T04:00:00Z.linky-order-1001.24102.120.50.USD.2026-04-21T13:30:00
```

请求头示例：

```text
X-Linky-Timestamp: 2026-04-22T04:00:00Z
X-Linky-Signature: <base64url-hmac>
```

---

## 5. 时间窗防重放
当前默认启用时间窗防重放：

- 配置项：`app.distribution.linky-replay-window-seconds`
- 默认值：`900` 秒（15 分钟）

服务端会比较：
- `X-Linky-Timestamp`
- 当前服务器 UTC 时间

如果超出允许时间窗，会返回：
- HTTP `403`
- message: `linky request expired`

这层解决的是：
- 请求是否已经过旧 / 过新

它和下面的 replay record 是两层能力，不是同一件事。

---

## 6. 幂等规则
Linky 进入 Fenxiao 后，会被映射成内部 `sourceEventId`：

```text
LINKY:{linkyOrderId}
```

例如：
```text
LINKY:linky-order-1001
```

因此同一个 `linkyOrderId` 重复提交时：
- 第一次会返回 `PROCESSED`
- 后续重复请求会返回 `DUPLICATE`

这层幂等依赖现有收益主链路，不需要 Linky 侧自己额外实现重复保护才能成立。

---

## 7. webhook 日志与排查
当前每次命中 Linky 入口时，Fenxiao 都会落一条 `linky_webhook_log`：
- 订单号 / 用户 ID / 金额 / 币种 / paidAt
- `X-Linky-Timestamp`
- `X-Linky-Signature`
- internal token 校验结果
- signature 校验结果
- replay / 时间窗校验结果
- replay record 结果：`FIRST_SEEN` / `REPLAYED` / `NOT_RECORDED`
- replay hit count
- 最终处理结果：`PROCESSED` / `DUPLICATE` / `REJECTED` / `FAILED`
- 失败原因
- 规范化后的 payload JSON

当前后台最小回看入口：
- `GET /admin/distribution/linky-webhook-logs`
- `GET /admin/distribution/linky-replay-records`

支持的最小筛选项：
- webhook logs：`linkyOrderId` / `userId` / `requestStatus` / `page` / `size`
- replay records：`linkyOrderId` / `userId` / `page` / `size`

这意味着后续对接上游时，如果出现：
- 签名不一致
- 时间戳过期
- 同一签名请求被重复推送
- 同单二次推送但业务已幂等
- Fenxiao 侧用户不存在

都可以先查 webhook 日志和 replay record，而不是只看接口返回。

---

## 8. 响应结构
成功响应示例：

```json
{
  "sourceEventId": "LINKY:linky-order-1001",
  "status": "PROCESSED"
}
```

重复响应示例：

```json
{
  "sourceEventId": "LINKY:linky-order-1001",
  "status": "DUPLICATE"
}
```

---

## 9. 常见错误

### 8.1 Internal token 错误
- HTTP: `403`
- code: `FORBIDDEN`
- message: `internal token invalid`

### 8.2 Linky 签名错误
- HTTP: `403`
- code: `FORBIDDEN`
- message: `linky signature invalid`

### 8.3 Linky 请求过期
- HTTP: `403`
- code: `FORBIDDEN`
- message: `linky request expired`

### 8.4 请求体字段校验失败
- HTTP: `400`
- code: `VALIDATION_ERROR`

### 8.5 用户在 Fenxiao 中不存在
- HTTP: `400`
- code: `BAD_REQUEST`
- message 可能是：`source user not found`

---

## 10. curl 示例
```bash
curl -X POST http://localhost:8080/internal/distribution/linky/income-events \
  -H 'Content-Type: application/json' \
  -H 'X-Internal-Token: your-internal-token' \
  -H 'X-Linky-Timestamp: 2026-04-22T04:00:00Z' \
  -H 'X-Linky-Signature: your-signature' \
  -d '{
    "orderId": "linky-order-1001",
    "memberId": 24102,
    "commissionAmount": 120.50,
    "currency": "USD",
    "settledAt": "2026-04-21T13:30:00"
  }'
```

---

## 11. 后续建议
下一步建议按这个顺序继续：
1. 明确 Linky 上游最终字段协议，只保留一套主字段并把别名兼容降级为过渡层
2. 补更复杂的 replay 策略（例如 nonce / 主动拒绝重复签名请求），而不只做追踪
3. 把 webhook 日志和 replay 记录查询补进运营后台前端，而不只停留在 admin API
4. 把签名算法、版本号、密钥轮换策略写成正式对接文档
