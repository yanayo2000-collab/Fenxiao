import { describe, expect, it } from 'vitest'
import { buildChannelEntryLinks, buildPublicEntryLinks } from './publicEntries'

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

describe('buildChannelEntryLinks', () => {
  it('builds channel-tracked public links for real operations', () => {
    expect(buildChannelEntryLinks('https://dist.example.com/', {
      product: 'LINKY',
      country: 'ID',
      language: 'id',
      channel: 'whatsapp-group-a',
      inviteCode: 'ABCD1234',
    })).toEqual([
      {
        key: 'invite',
        label: '邀请注册入口',
        path: '/invite',
        url: 'https://dist.example.com/invite?product=LINKY&country=ID&lang=id&channel=whatsapp-group-a&inviteCode=ABCD1234',
      },
      {
        key: 'bind',
        label: 'Linky 绑定入口',
        path: '/bind',
        url: 'https://dist.example.com/bind?product=LINKY&country=ID&lang=id&channel=whatsapp-group-a&inviteCode=ABCD1234',
      },
      {
        key: 'earnings',
        label: '收益查看入口',
        path: '/earnings',
        url: 'https://dist.example.com/earnings?product=LINKY&country=ID&lang=id&channel=whatsapp-group-a&inviteCode=ABCD1234',
      },
    ])
  })

  it('omits empty tracking parameters instead of producing noisy urls', () => {
    expect(buildChannelEntryLinks('https://dist.example.com', {
      product: 'ALL',
      country: '',
      language: '',
      channel: '',
      inviteCode: '',
    })[0].url).toBe('https://dist.example.com/invite')
  })
})
