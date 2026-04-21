export type CreateProfileRequest = {
  userId: number
  countryCode: string
  languageCode: string
  inviteCode?: string
}

export type ProfileResponse = {
  userId: number
  inviteCode: string
  countryCode: string
  languageCode: string
  accessToken: string
}

export type AdminSessionResponse = {
  sessionToken: string
  expiresAt: string
}

export type DistributionHomeResponse = {
  userId: number
  inviteCode: string
  inviterUserId: number | null
  invitedUsers: number
  effectiveUsers: number
  totalReward: number
  frozenReward: number
  availableReward: number
  riskHoldReward: number
}

export type TeamMemberItem = {
  userId: number
  inviteCode: string
  countryCode: string
  effectiveUser: boolean
  confirmedIncomeTotal: number
  lockStatus: string
  bindTime: string
}

export type TeamListResponse = {
  items: TeamMemberItem[]
  total: number
}

export type RewardListItem = {
  beneficiaryUserId: number
  sourceUserId: number
  rewardLevel: number
  rewardAmount: number
  rewardStatus: string
  calculatedAt: string
}

export type RewardListResponse = {
  items: RewardListItem[]
  total: number
  page: number
  size: number
}

export type RiskEventListItem = {
  id: number
  userId: number
  riskType: string
  riskLevel: number
  riskStatus: string
  detailJson: string
  detectedAt: string
  handledBy: number | null
  handledAt: string | null
  resultNote: string | null
}

export type RiskEventListResponse = {
  items: RiskEventListItem[]
  total: number
  page: number
  size: number
}

export type AuditLogListItem = {
  id: number
  moduleName: string
  targetType: string
  targetId: number
  actionName: string
  operatorRole: string
  operatorId: number
  requestIp: string | null
  remark: string | null
  operatedAt: string
}

export type AuditLogListResponse = {
  items: AuditLogListItem[]
  total: number
  page: number
  size: number
}

export type OverviewReportResponse = {
  invitedUsers: number
  effectiveUsers: number
  rewardTotal: number
  frozenRewardTotal: number
  availableRewardTotal: number
  riskEventCount: number
}

export type RelationDetailResponse = {
  userId: number
  level1InviterId: number | null
  level2InviterId: number | null
  level3InviterId: number | null
  bindSource: string
  lockStatus: string
  bindTime: string
  lockTime: string | null
  countryCode: string
  crossCountry: boolean
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {}),
    },
    ...init,
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `request failed: ${response.status}`)
  }

  return response.json() as Promise<T>
}

export function createProfile(profileCreateToken: string, payload: CreateProfileRequest) {
  return request<ProfileResponse>('/api/distribution/profiles', {
    method: 'POST',
    headers: {
      'X-Profile-Create-Token': profileCreateToken,
    },
    body: JSON.stringify(payload),
  })
}

export function createAdminSession(password: string) {
  return request<AdminSessionResponse>('/admin/auth/session', {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}

export function getDistributionHome(userId: number, accessToken: string) {
  return request<DistributionHomeResponse>(`/api/distribution/home/${userId}`, {
    headers: {
      'X-Distribution-Token': accessToken,
    },
  })
}

export function getDistributionTeam(userId: number, accessToken: string) {
  return request<TeamListResponse>(`/api/distribution/team/${userId}`, {
    headers: {
      'X-Distribution-Token': accessToken,
    },
  })
}

export function getDistributionRewards(userId: number, accessToken: string) {
  return request<RewardListResponse>(`/api/distribution/rewards/${userId}`, {
    headers: {
      'X-Distribution-Token': accessToken,
    },
  })
}

export function getAdminOverview(adminSessionToken: string) {
  return request<OverviewReportResponse>('/admin/distribution/reports/overview', {
    headers: {
      'X-Admin-Session': adminSessionToken,
    },
  })
}

export function getAdminRewards(adminSessionToken: string, filters?: {
  beneficiaryUserId?: number
  status?: string
  startAt?: string
  endAt?: string
  page?: number
  size?: number
}) {
  const params = new URLSearchParams()
  if (filters?.beneficiaryUserId) params.set('beneficiaryUserId', String(filters.beneficiaryUserId))
  if (filters?.status) params.set('status', filters.status)
  if (filters?.startAt) params.set('startAt', filters.startAt)
  if (filters?.endAt) params.set('endAt', filters.endAt)
  if (filters?.page !== undefined) params.set('page', String(filters.page))
  if (filters?.size !== undefined) params.set('size', String(filters.size))
  const query = params.toString()
  return request<RewardListResponse>(`/admin/distribution/rewards${query ? `?${query}` : ''}`, {
    headers: {
      'X-Admin-Session': adminSessionToken,
    },
  })
}

export function getAdminRiskEvents(adminSessionToken: string, filters?: {
  userId?: number
  riskStatus?: string
  startAt?: string
  endAt?: string
  page?: number
  size?: number
}) {
  const params = new URLSearchParams()
  if (filters?.userId) params.set('userId', String(filters.userId))
  if (filters?.riskStatus) params.set('riskStatus', filters.riskStatus)
  if (filters?.startAt) params.set('startAt', filters.startAt)
  if (filters?.endAt) params.set('endAt', filters.endAt)
  if (filters?.page !== undefined) params.set('page', String(filters.page))
  if (filters?.size !== undefined) params.set('size', String(filters.size))
  const query = params.toString()
  return request<RiskEventListResponse>(`/admin/distribution/risk-events${query ? `?${query}` : ''}`, {
    headers: {
      'X-Admin-Session': adminSessionToken,
    },
  })
}

export function getAdminRelation(adminSessionToken: string, userId: number) {
  return request<RelationDetailResponse>(`/admin/distribution/relation/${userId}`, {
    headers: {
      'X-Admin-Session': adminSessionToken,
    },
  })
}

export function applyAdminRiskEventAction(adminSessionToken: string, riskEventId: number, payload: {
  action: 'HANDLE' | 'IGNORE' | 'FREEZE_USER' | 'UNFREEZE_USER'
  note?: string
}) {
  return request<RiskEventListItem>(`/admin/distribution/risk-events/${riskEventId}/actions`, {
    method: 'POST',
    headers: {
      'X-Admin-Session': adminSessionToken,
    },
    body: JSON.stringify(payload),
  })
}

export function getAdminAuditLogs(adminSessionToken: string, filters?: {
  moduleName?: string
  page?: number
  size?: number
}) {
  const params = new URLSearchParams()
  if (filters?.moduleName) params.set('moduleName', filters.moduleName)
  if (filters?.page !== undefined) params.set('page', String(filters.page))
  if (filters?.size !== undefined) params.set('size', String(filters.size))
  const query = params.toString()
  return request<AuditLogListResponse>(`/admin/distribution/audit-logs${query ? `?${query}` : ''}`, {
    headers: {
      'X-Admin-Session': adminSessionToken,
    },
  })
}
