import type { LinkyReplayRecordListItem, LinkyWebhookLogListItem } from './api'

export type LinkyDetailSection = {
  title: string
  rows: Array<[string, string]>
}

export type LinkyRelatedContext = {
  relatedWebhooks: string[]
  relatedReplays: string[]
  fingerprintHint: string
}

export function buildLinkyWebhookHeadline(item: LinkyWebhookLogListItem) {
  return `${item.linkyOrderId || `日志 #${item.id}`} · ${item.requestStatus}`
}

export function buildLinkyWebhookDetailSections(item: LinkyWebhookLogListItem): LinkyDetailSection[] {
  return [
    {
      title: '请求上下文',
      rows: [
        ['订单号', item.linkyOrderId || '-'],
        ['内部事件', item.sourceEventId || '-'],
        ['用户', item.userId ? `#${item.userId}` : '-'],
        ['金额', item.incomeAmount !== null ? `${item.incomeAmount} ${item.currencyCode || ''}`.trim() : '-'],
        ['支付时间', formatTimestamp(item.paidAt)],
        ['接收时间', formatTimestamp(item.requestReceivedAt)],
      ],
    },
    {
      title: '校验结果',
      rows: [
        ['Token', item.internalTokenStatus],
        ['Signature', item.signatureStatus],
        ['时间窗', item.replayStatus],
        ['指纹去重', item.replayRecordStatus],
        ['Replay 命中', item.replayHitCount ? `第 ${item.replayHitCount} 次` : '-'],
      ],
    },
    {
      title: '处理结果',
      rows: [
        ['最终状态', item.requestStatus],
        ['失败原因', item.failureReason || '-'],
      ],
    },
  ]
}

export function buildLinkyReplayDetailSections(item: LinkyReplayRecordListItem): LinkyDetailSection[] {
  return [
    {
      title: '指纹标识',
      rows: [
        ['订单号', item.linkyOrderId || '-'],
        ['内部事件', item.sourceEventId || '-'],
        ['用户', item.userId ? `#${item.userId}` : '-'],
        ['请求指纹', item.requestFingerprint],
      ],
    },
    {
      title: '命中轨迹',
      rows: [
        ['首次命中', formatTimestamp(item.firstSeenAt)],
        ['最近命中', formatTimestamp(item.lastSeenAt)],
        ['累计次数', `${item.hitCount} 次`],
        ['最新状态', item.latestRequestStatus],
        ['失败原因', item.latestFailureReason || '-'],
      ],
    },
  ]
}

export function buildLinkyRelatedContext(input: {
  selected: { kind: 'webhook'; item: LinkyWebhookLogListItem } | { kind: 'replay'; item: LinkyReplayRecordListItem }
  webhookItems: LinkyWebhookLogListItem[]
  replayItems: LinkyReplayRecordListItem[]
}): LinkyRelatedContext {
  const linkyOrderId = input.selected.item.linkyOrderId
  const relatedWebhooks = input.webhookItems
    .filter((item) => linkyOrderId && item.linkyOrderId === linkyOrderId)
    .map((item) => `${item.requestStatus} · ${formatTimestamp(item.requestReceivedAt)}`)

  const relatedReplays = input.replayItems
    .filter((item) => {
      if (input.selected.kind === 'replay') {
        return item.requestFingerprint === input.selected.item.requestFingerprint
      }
      return linkyOrderId && item.linkyOrderId === linkyOrderId
    })
    .map((item) => `${item.requestFingerprint} · ${item.hitCount} 次 · ${item.latestRequestStatus}`)

  return {
    relatedWebhooks,
    relatedReplays,
    fingerprintHint: input.selected.kind === 'replay'
      ? '这条 replay 记录本身就代表同一指纹的累计命中；命中次数和最新状态已经在上面展开。'
      : '当前 webhook 列表接口还没返回 requestFingerprint，如需逐条同指纹串联，下一步需要把该字段补到 admin API。',
  }
}

function formatTimestamp(value: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`
}
