export type PublicEntryLink = {
  key: 'invite' | 'bind' | 'earnings'
  label: string
  path: '/invite' | '/bind' | '/earnings'
  url: string
}

export type ChannelEntryInput = {
  product?: string
  country?: string
  language?: string
  channel?: string
  inviteCode?: string
}

const DEFAULT_ORIGIN = 'http://127.0.0.1:4173'

function normalizeOrigin(origin?: string): string {
  return origin && origin.trim() ? origin.trim().replace(/\/$/, '') : DEFAULT_ORIGIN
}

function buildTrackedUrl(origin: string, path: PublicEntryLink['path'], input: ChannelEntryInput): string {
  const params = new URLSearchParams()
  const product = input.product?.trim()
  const country = input.country?.trim()
  const language = input.language?.trim()
  const channel = input.channel?.trim()
  const inviteCode = input.inviteCode?.trim()

  if (product && product !== 'ALL') params.set('product', product)
  if (country) params.set('country', country.toUpperCase())
  if (language) params.set('lang', language.toLowerCase())
  if (channel) params.set('channel', channel)
  if (inviteCode) params.set('inviteCode', inviteCode)

  const query = params.toString()
  return `${origin}${path}${query ? `?${query}` : ''}`
}

export function buildPublicEntryLinks(origin?: string): PublicEntryLink[] {
  const safeOrigin = normalizeOrigin(origin)
  return [
    { key: 'invite', label: '生成邀请码', path: '/invite', url: `${safeOrigin}/invite` },
    { key: 'bind', label: '绑定关系', path: '/bind', url: `${safeOrigin}/bind` },
    { key: 'earnings', label: '收益查看', path: '/earnings', url: `${safeOrigin}/earnings` },
  ]
}

export function buildChannelEntryLinks(origin: string | undefined, input: ChannelEntryInput): PublicEntryLink[] {
  const safeOrigin = normalizeOrigin(origin)
  return [
    { key: 'invite', label: '邀请注册入口', path: '/invite', url: buildTrackedUrl(safeOrigin, '/invite', input) },
    { key: 'bind', label: 'Linky 绑定入口', path: '/bind', url: buildTrackedUrl(safeOrigin, '/bind', input) },
    { key: 'earnings', label: '收益查看入口', path: '/earnings', url: buildTrackedUrl(safeOrigin, '/earnings', input) },
  ]
}
