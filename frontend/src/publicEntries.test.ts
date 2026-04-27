import { describe, expect, it } from 'vitest'
import { buildPublicEntryLinks } from './publicEntries'

describe('buildPublicEntryLinks', () => {
  it('builds the three public landing pages from the current origin', () => {
    expect(buildPublicEntryLinks('http://127.0.0.1:4173')).toEqual([
      { key: 'invite', label: '生成邀请码', path: '/invite', url: 'http://127.0.0.1:4173/invite' },
      { key: 'bind', label: '绑定关系', path: '/bind', url: 'http://127.0.0.1:4173/bind' },
      { key: 'earnings', label: '收益查看', path: '/earnings', url: 'http://127.0.0.1:4173/earnings' },
    ])
  })

  it('falls back to local dev origin when origin is blank', () => {
    expect(buildPublicEntryLinks('')).toEqual([
      { key: 'invite', label: '生成邀请码', path: '/invite', url: 'http://127.0.0.1:4173/invite' },
      { key: 'bind', label: '绑定关系', path: '/bind', url: 'http://127.0.0.1:4173/bind' },
      { key: 'earnings', label: '收益查看', path: '/earnings', url: 'http://127.0.0.1:4173/earnings' },
    ])
  })
})
