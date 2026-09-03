import type { AdminWithdrawRequestListResponse, RiskEventListResponse } from './api'

const WITHDRAW_STATUSES = ['PENDING_REVIEW', 'PAYMENT_PENDING', 'PAYMENT_FAILED', 'PAID_OUT', 'REJECTED', 'REVERSED']
const RISK_STATUSES = ['PENDING', 'HANDLED', 'IGNORED']

export function buildAdminStressWithdrawRequests(page = 0, size = 100, total = 1_287): AdminWithdrawRequestListResponse {
  const offset = page * size
  return {
    total,
    page,
    size,
    items: Array.from({ length: Math.min(size, Math.max(total - offset, 0)) }, (_, index) => {
      const sequence = offset + index + 1
      return {
        requestNo: sequence % 9 === 0
          ? `BR-PIX-20260825-${String(sequence).padStart(7, '0')}-RECONCILIATION-LONG-REFERENCE`
          : `BR-PIX-20260825-${String(sequence).padStart(7, '0')}`,
        userId: 70_000 + sequence,
        requestedDiamondAmount: 1_000 + ((sequence * 7919) % 900_000),
        requestStatus: WITHDRAW_STATUSES[sequence % WITHDRAW_STATUSES.length],
        requestWeek: `2026-W${String(30 + (sequence % 8)).padStart(2, '0')}`,
        requestedAt: new Date(Date.UTC(2026, 7, 25, 12, 0) - sequence * 71_000).toISOString(),
      }
    }),
  }
}

export function buildAdminStressRiskEvents(page = 0, size = 100, total = 2_041): RiskEventListResponse {
  const offset = page * size
  return {
    total,
    page,
    size,
    items: Array.from({ length: Math.min(size, Math.max(total - offset, 0)) }, (_, index) => {
      const sequence = offset + index + 1
      const status = RISK_STATUSES[sequence % RISK_STATUSES.length]
      return {
        id: 90_000 + sequence,
        userId: 70_000 + sequence,
        riskType: sequence % 4 === 0 ? 'CROSS_CRM_DUPLICATE_PLATFORM_ID_WITH_LONG_CONTEXT' : ['DUPLICATE_BINDING', 'GUILD_MISMATCH', 'INCOME_REVERSAL'][sequence % 3],
        riskLevel: 1 + (sequence % 5),
        riskStatus: status,
        detailJson: JSON.stringify({ source: 'SANITIZED_STRESS_FIXTURE', sequence, platform: sequence % 2 ? 'LINKY' : 'TIMO' }),
        detectedAt: new Date(Date.UTC(2026, 7, 25, 10, 0) - sequence * 93_000).toISOString(),
        handledBy: status === 'PENDING' ? null : 1,
        handledAt: status === 'PENDING' ? null : new Date(Date.UTC(2026, 7, 25, 11, 0) - sequence * 87_000).toISOString(),
        resultNote: status === 'PENDING' ? null : '脱敏压力数据：用于验证终态、长文本和高密度表格。',
      }
    }),
  }
}
