export type PublicEntryLink = {
  key: 'invite' | 'bind' | 'earnings'
  label: string
  path: '/invite' | '/bind' | '/earnings'
  url: string
}

const DEFAULT_ORIGIN = 'http://127.0.0.1:4173'

export function buildPublicEntryLinks(origin?: string): PublicEntryLink[] {
  const safeOrigin = origin && origin.trim() ? origin.replace(/\/$/, '') : DEFAULT_ORIGIN
  return [
    { key: 'invite', label: '生成邀请码', path: '/invite', url: `${safeOrigin}/invite` },
    { key: 'bind', label: '绑定关系', path: '/bind', url: `${safeOrigin}/bind` },
    { key: 'earnings', label: '收益查看', path: '/earnings', url: `${safeOrigin}/earnings` },
  ]
}
