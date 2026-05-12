import { afterEach, describe, expect, it, vi } from 'vitest'
import { correctAdminOwnership, createAdminSession, createWithdrawRequest, getAdminGuildConfigs, getAdminGuildWeeklyReport, getAdminOwnership, getAdminWithdrawRequests, refreshAdminLinkyEligibility, refreshAdminLinkyEligibilityBatch, saveAdminGuildConfig } from './api'

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

  it('posts batch refresh request for all linky eligibility checks with admin session header', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ successCount: 8, failureCount: 2 }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await refreshAdminLinkyEligibilityBatch('session-token')

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/linky-eligibility-checks/batch-refresh', expect.objectContaining({
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

  it('requests guild config list with admin session header', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ([{
        id: 1,
        productCode: 'LINKY',
        inviterUserId: null,
        guildId: 'GUILD-A',
        guildName: 'Linky A',
        guildInviteCode: 'JOIN-A',
        enabled: true,
      }]),
    })
    vi.stubGlobal('fetch', fetchMock)

    await getAdminGuildConfigs('session-token')

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/guild-configs', expect.objectContaining({
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-Admin-Session': 'session-token',
      }),
    }))
  })

  it('saves guild config payload with admin session header', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        id: 2,
        productCode: 'LINKY',
        inviterUserId: 1001,
        guildId: 'GUILD-B',
        guildName: 'Linky B',
        guildInviteCode: 'JOIN-B',
        enabled: false,
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await saveAdminGuildConfig('session-token', {
      productCode: 'LINKY',
      inviterUserId: 1001,
      guildId: 'GUILD-B',
      guildName: 'Linky B',
      guildInviteCode: 'JOIN-B',
      enabled: false,
    })

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/guild-configs', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'X-Admin-Session': 'session-token',
      }),
      body: JSON.stringify({
        productCode: 'LINKY',
        inviterUserId: 1001,
        guildId: 'GUILD-B',
        guildName: 'Linky B',
        guildInviteCode: 'JOIN-B',
        enabled: false,
      }),
    }))
  })

  it('requests guild weekly report with product and week filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ productCode: 'LINKY', guildId: 'GUILD-A', week: 'CURRENT', registeredUsers: 1, incomeAmount: 100, rewardAmount: 10 }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await getAdminGuildWeeklyReport('session-token', 'GUILD-A', { product: 'LINKY', week: 'CURRENT' })

    expect(fetchMock).toHaveBeenCalledWith('/admin/distribution/guild-configs/GUILD-A/weekly-report?product=LINKY&week=CURRENT', expect.objectContaining({
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
