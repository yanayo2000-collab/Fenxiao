import { describe, expect, it } from 'vitest'
import type { LinkyReplayRecordListItem, LinkyWebhookLogListItem } from './api'
import {
  buildLinkyReplayDetailSections,
  buildLinkyWebhookDetailSections,
  buildLinkyWebhookHeadline,
} from './linkyDetails'

const formattedPaidAt = `${new Date('2026-04-23T08:00:00Z').toLocaleDateString()} ${new Date('2026-04-23T08:00:00Z').toLocaleTimeString()}`
const formattedReceivedAt = `${new Date('2026-04-23T08:01:00Z').toLocaleDateString()} ${new Date('2026-04-23T08:01:00Z').toLocaleTimeString()}`
const formattedFirstSeenAt = `${new Date('2026-04-23T08:00:00Z').toLocaleDateString()} ${new Date('2026-04-23T08:00:00Z').toLocaleTimeString()}`
const formattedLastSeenAt = `${new Date('2026-04-23T08:05:00Z').toLocaleDateString()} ${new Date('2026-04-23T08:05:00Z').toLocaleTimeString()}`

const webhookItem: LinkyWebhookLogListItem = {
  id: 11,
  linkyOrderId: 'order-1001',
  sourceEventId: 'LINKY:order-1001',
  userId: 13002,
  incomeAmount: 12.5,
  currencyCode: 'USD',
  paidAt: '2026-04-23T08:00:00Z',
  requestReceivedAt: '2026-04-23T08:01:00Z',
  internalTokenStatus: 'VALID',
  signatureStatus: 'INVALID',
  replayStatus: 'VALID',
  replayRecordStatus: 'REPLAYED',
  replayHitCount: 3,
  requestStatus: 'REJECTED',
  failureReason: 'signature mismatch',
}

const replayItem: LinkyReplayRecordListItem = {
  id: 22,
  requestFingerprint: 'abc123fingerprint',
  linkyOrderId: 'order-1001',
  sourceEventId: 'LINKY:order-1001',
  userId: 13002,
  firstSeenAt: '2026-04-23T08:00:00Z',
  lastSeenAt: '2026-04-23T08:05:00Z',
  hitCount: 3,
  latestRequestStatus: 'DUPLICATE',
  latestFailureReason: null,
}

describe('buildLinkyWebhookHeadline', () => {
  it('surfaces the order and final status for drawer titles', () => {
    expect(buildLinkyWebhookHeadline(webhookItem)).toBe('order-1001 · REJECTED')
  })

  it('falls back to log id when order id is absent', () => {
    expect(buildLinkyWebhookHeadline({ ...webhookItem, linkyOrderId: null })).toBe('日志 #11 · REJECTED')
  })
})

describe('buildLinkyWebhookDetailSections', () => {
  it('groups webhook details into context, verification and result sections', () => {
    expect(buildLinkyWebhookDetailSections(webhookItem)).toEqual([
      {
        title: '请求上下文',
        rows: [
          ['订单号', 'order-1001'],
          ['内部事件', 'LINKY:order-1001'],
          ['用户', '#13002'],
          ['金额', '12.5 USD'],
          ['支付时间', formattedPaidAt],
          ['接收时间', formattedReceivedAt],
        ],
      },
      {
        title: '校验结果',
        rows: [
          ['Token', 'VALID'],
          ['Signature', 'INVALID'],
          ['时间窗', 'VALID'],
          ['指纹去重', 'REPLAYED'],
          ['Replay 命中', '第 3 次'],
        ],
      },
      {
        title: '处理结果',
        rows: [
          ['最终状态', 'REJECTED'],
          ['失败原因', 'signature mismatch'],
        ],
      },
    ])
  })
})

describe('buildLinkyReplayDetailSections', () => {
  it('groups replay details into identity and timeline sections', () => {
    expect(buildLinkyReplayDetailSections(replayItem)).toEqual([
      {
        title: '指纹标识',
        rows: [
          ['订单号', 'order-1001'],
          ['内部事件', 'LINKY:order-1001'],
          ['用户', '#13002'],
          ['请求指纹', 'abc123fingerprint'],
        ],
      },
      {
        title: '命中轨迹',
        rows: [
          ['首次命中', formattedFirstSeenAt],
          ['最近命中', formattedLastSeenAt],
          ['累计次数', '3 次'],
          ['最新状态', 'DUPLICATE'],
          ['失败原因', '-'],
        ],
      },
    ])
  })
})
