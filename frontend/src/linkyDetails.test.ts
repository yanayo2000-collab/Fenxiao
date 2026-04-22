import { describe, expect, it } from 'vitest'
import type { LinkyReplayRecordListItem, LinkyWebhookLogListItem } from './api'
import {
  buildLinkyRelatedContext,
  buildLinkyReplayDetailSections,
  buildLinkyWebhookDetailSections,
  buildLinkyWebhookHeadline,
} from './linkyDetails'

const formattedPaidAt = `${new Date('2026-04-23T08:00:00Z').toLocaleDateString()} ${new Date('2026-04-23T08:00:00Z').toLocaleTimeString()}`
const formattedReceivedAt = `${new Date('2026-04-23T08:01:00Z').toLocaleDateString()} ${new Date('2026-04-23T08:01:00Z').toLocaleTimeString()}`
const formattedSecondReceivedAt = `${new Date('2026-04-23T08:02:00Z').toLocaleDateString()} ${new Date('2026-04-23T08:02:00Z').toLocaleTimeString()}`
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

const secondWebhookItem: LinkyWebhookLogListItem = {
  ...webhookItem,
  id: 12,
  requestStatus: 'DUPLICATE',
  requestReceivedAt: '2026-04-23T08:02:00Z',
}

const secondReplayItem: LinkyReplayRecordListItem = {
  ...replayItem,
  id: 23,
  requestFingerprint: 'otherfingerprint',
  linkyOrderId: 'order-1002',
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

describe('buildLinkyRelatedContext', () => {
  it('collects same-order webhook and replay records around a webhook detail item', () => {
    expect(buildLinkyRelatedContext({
      selected: { kind: 'webhook', item: webhookItem },
      webhookItems: [webhookItem, secondWebhookItem],
      replayItems: [replayItem, secondReplayItem],
    })).toEqual({
      relatedWebhooks: [
        `REJECTED · ${formattedReceivedAt}`,
        `DUPLICATE · ${formattedSecondReceivedAt}`,
      ],
      relatedReplays: [
        'abc123fingerprint · 3 次 · DUPLICATE',
      ],
      fingerprintHint: '当前 webhook 列表接口还没返回 requestFingerprint，如需逐条同指纹串联，下一步需要把该字段补到 admin API。',
    })
  })

  it('collects same-order webhooks and same-fingerprint replay summary around a replay detail item', () => {
    expect(buildLinkyRelatedContext({
      selected: { kind: 'replay', item: replayItem },
      webhookItems: [webhookItem, secondWebhookItem],
      replayItems: [replayItem, secondReplayItem],
    })).toEqual({
      relatedWebhooks: [
        `REJECTED · ${formattedReceivedAt}`,
        `DUPLICATE · ${formattedSecondReceivedAt}`,
      ],
      relatedReplays: [
        'abc123fingerprint · 3 次 · DUPLICATE',
      ],
      fingerprintHint: '这条 replay 记录本身就代表同一指纹的累计命中；命中次数和最新状态已经在上面展开。',
    })
  })
})
