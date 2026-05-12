import { afterEach, describe, expect, it, vi } from 'vitest'
import { correctAdminOwnership, createAdminSession, createWithdrawRequest, getAdminOwnership, getAdminWithdrawRequests, refreshAdminLinkyEligibility } from './api'

describe('ownership admin api', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requests ownership detail with admin session header', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ userId: 1001, items: [] }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await getAdminOwnership('session-token', 1001)

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/ownership/1001', expect.objectContaining({
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-Admin-Session': 'session-token',
      }),
    }))
  })

  it('posts ownership correction payload with product code and note', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ userId: 1001, items: [] }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await correctAdminOwnership('session-token', 1001, {
      productCode: 'LINKY',
      note: '人工修正产品归属',
    })

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/ownership/1001/corrections', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-Admin-Session': 'session-token',
      }),
      body: JSON.stringify({
        productCode: 'LINKY',
        note: '人工修正产品归属',
      }),
    }))
  })

  it('posts refresh request for linky eligibility with admin session header', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        linkyAccount: '12345678',
        guildCheckStatus: 'MATCHED_OURS',
        registrationEligibility: 'ELIGIBLE',
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await refreshAdminLinkyEligibility('session-token', '12345678')

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/linky-eligibility-checks/12345678/refresh', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-Admin-Session': 'session-token',
      }),
    }))
  })

  it('creates withdraw request with user session token header', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ requestNo: 'WR-001', requestStatus: 'PENDING' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await createWithdrawRequest(1001, 'user-token')

    expect(fetchMock).toHaveBeenCalledWith('/api/distribution/withdraw-requests/1001', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-Distribution-Token': 'user-token',
      }),
    }))
  })

  it('requests admin withdraw list with session header and filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ items: [], total: 0, page: 0, size: 20 }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await getAdminWithdrawRequests('session-token', { userId: 1001, status: 'PENDING', page: 0, size: 20 })

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/withdraw-requests?userId=1001&status=PENDING&page=0&size=20', expect.objectContaining({
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-Admin-Session': 'session-token',
      }),
    }))
  })

  it('surfaces backend message field when admin login fails', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      text: async () => JSON.stringify({
        code: 'FORBIDDEN',
        message: 'admin login invalid',
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(createAdminSession('wrong-token')).rejects.toThrow('admin login invalid')
  })
})
