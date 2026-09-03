import { describe, expect, it } from 'vitest'
import { buildAdminStressRiskEvents, buildAdminStressWithdrawRequests } from './adminStressFixtures'

describe('admin stress fixtures', () => {
  it('covers dense withdrawal pages, long identifiers and all terminal states', () => {
    const page = buildAdminStressWithdrawRequests()
    expect(page.items).toHaveLength(100)
    expect(page.total).toBe(1287)
    expect(page.items.some((item) => item.requestNo.length > 45)).toBe(true)
    expect(new Set(page.items.map((item) => item.requestStatus))).toEqual(new Set([
      'PENDING_REVIEW', 'PAYMENT_PENDING', 'PAYMENT_FAILED', 'PAID_OUT', 'REJECTED', 'REVERSED',
    ]))
  })

  it('covers high-density risk statuses and long operational text', () => {
    const page = buildAdminStressRiskEvents()
    expect(page.items).toHaveLength(100)
    expect(page.total).toBe(2041)
    expect(new Set(page.items.map((item) => item.riskStatus))).toEqual(new Set(['PENDING', 'HANDLED', 'IGNORED']))
    expect(page.items.some((item) => item.riskType.length > 35)).toBe(true)
  })
})
