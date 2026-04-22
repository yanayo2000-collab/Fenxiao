import type { LinkyReplayRecordListItem, LinkyWebhookLogListItem } from './api'

export type LinkyDetailSection = {
  title: string
  rows: Array<[string, string]>
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

function formatTimestamp(value: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`
}
