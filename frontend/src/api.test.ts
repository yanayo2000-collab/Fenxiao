import { afterEach, describe, expect, it, vi } from 'vitest'
import { correctAdminOwnership, getAdminOwnership } from './api'

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
})
