# Linky 收益同步接入协议

## 1. 目标
这份文档用于说明 Fenxiao 当前如何接入 Linky 收益事件，以及上游在调用时需要满足哪些字段、请求头和签名规则。

当前这版已经支持：
- Linky 专用入口
- 字段别名兼容
- 基础签名校验
- 时间窗防重放
- 收益事件幂等处理

当前这版还未完成：
- 上游最终字段协议定稿
- 原始 webhook 明细落库
- 更细的 replay record / nonce 追踪
- 多版本签名协议兼容

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

这版防的是基础重放，不是完整 replay record 持久化方案。

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

## 7. 响应结构
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

## 8. 常见错误

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

## 9. curl 示例
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

## 10. 后续建议
下一步建议按这个顺序继续：
1. 明确 Linky 上游最终字段协议，只保留一套主字段并把别名兼容降级为过渡层
2. 增加 webhook 原始事件日志表，记录 timestamp / signature / payload 摘要 / 校验结果
3. 做显式 replay record，而不只靠时间窗
4. 把签名算法、版本号、密钥轮换策略写成正式对接文档
