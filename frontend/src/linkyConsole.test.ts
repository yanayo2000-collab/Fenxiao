import { describe, expect, it } from 'vitest'
import {
  buildLinkyReplaySummary,
  buildLinkyWebhookSummary,
  buildPagedResultLabel,
} from './linkyConsole'

describe('buildPagedResultLabel', () => {
  it('formats current page and total count', () => {
    expect(buildPagedResultLabel({ page: 1, size: 20, total: 45, subject: 'Webhook 日志' })).toBe('Webhook 日志：第 2 页 / 共 45 条')
  })

  it('returns waiting hint when no query has been executed', () => {
    expect(buildPagedResultLabel(null)).toBe('先执行一次查询')
  })
})

describe('buildLinkyWebhookSummary', () => {
  it('includes replay and request status for operators', () => {
    expect(buildLinkyWebhookSummary({
      requestStatus: 'DUPLICATE',
      replayStatus: 'VALID',
      replayRecordStatus: 'REPLAYED',
      replayHitCount: 3,
      failureReason: null,
    })).toBe('DUPLICATE · 时间窗 VALID · 指纹 REPLAYED · 第 3 次命中')
  })

  it('appends failure reason when request was rejected', () => {
    expect(buildLinkyWebhookSummary({
      requestStatus: 'REJECTED',
      replayStatus: 'EXPIRED',
      replayRecordStatus: 'NOT_RECORDED',
      replayHitCount: null,
      failureReason: 'linky request expired',
    })).toBe('REJECTED · 时间窗 EXPIRED · 指纹 NOT_RECORDED · 原因：linky request expired')
  })
})

describe('buildLinkyReplaySummary', () => {
  it('shows hit count and latest request status', () => {
    expect(buildLinkyReplaySummary({
      hitCount: 4,
      latestRequestStatus: 'DUPLICATE',
      latestFailureReason: null,
    })).toBe('命中 4 次 · 最新状态 DUPLICATE')
  })

  it('shows latest failure reason when present', () => {
    expect(buildLinkyReplaySummary({
      hitCount: 1,
      latestRequestStatus: 'FAILED',
      latestFailureReason: 'rule not found',
    })).toBe('命中 1 次 · 最新状态 FAILED · 原因：rule not found')
  })
})
