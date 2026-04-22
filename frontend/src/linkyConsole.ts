import type { LinkyReplayRecordListItem, LinkyWebhookLogListItem } from './api'

export function buildPagedResultLabel(input: { page: number; size: number; total: number; subject: string } | null) {
  if (!input) return '先执行一次查询'
  return `${input.subject}：第 ${input.page + 1} 页 / 共 ${input.total} 条`
}

export function buildLinkyWebhookSummary(item: Pick<LinkyWebhookLogListItem, 'requestStatus' | 'replayStatus' | 'replayRecordStatus' | 'replayHitCount' | 'failureReason'>) {
  const parts = [
    item.requestStatus,
    `时间窗 ${item.replayStatus}`,
    `指纹 ${item.replayRecordStatus}`,
  ]
  if (item.replayHitCount) {
    parts.push(`第 ${item.replayHitCount} 次命中`)
  }
  if (item.failureReason) {
    parts.push(`原因：${item.failureReason}`)
  }
  return parts.join(' · ')
}

export function buildLinkyReplaySummary(item: Pick<LinkyReplayRecordListItem, 'hitCount' | 'latestRequestStatus' | 'latestFailureReason'>) {
  const parts = [`命中 ${item.hitCount} 次`, `最新状态 ${item.latestRequestStatus}`]
  if (item.latestFailureReason) {
    parts.push(`原因：${item.latestFailureReason}`)
  }
  return parts.join(' · ')
}
