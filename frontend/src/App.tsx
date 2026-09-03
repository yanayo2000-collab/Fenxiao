import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import {
  ArrowRight,
  Bell,
  CaretRight,
  CheckCircle,
  Copy,
  Diamond,
  Eye,
  EyeSlash,
  IdentificationCard,
  LinkSimple,
  GearSix,
  House,
  Megaphone,
  Phone,
  ShareNetwork,
  ShieldCheck,
  SignIn,
  LockSimple,
  Medal,
  Sparkle,
  Target,
  User,
  UserCircle,
  UserPlus,
  UsersThree,
  Wallet,
} from '@phosphor-icons/react'
import './App.css'
import {
  adjustAdminRelation,
  applyAdminRiskEventAction,
  applyAdminRiskEventBatchAction,
  applyAdminWithdrawBatchAction,
  approveWithdrawForPayment,
  changeExperimentStatus,
  changeAdminPassword,
  correctAdminOwnership,
  createExperiment,
  createAdminSession,
  createAdminAccount,
  createProfile,
  createWithdrawRequest,
  getAdminAuditLogs,
  getAdminAccounts,
  getAdminDeviceSessions,
  getCurrentAdminSession,
  getMyAdminSecurityEvents,
  getAdminGuildConfigs,
  getAdminGuildWeeklyReport,
  getAdminLinkyReplayRecords,
  getAdminLinkyWebhookLogs,
  getAdminOverview,
  getAdminOwnership,
  getAdminRelation,
  getAdminRewards,
  getAdminRiskEvents,
  getAdminWithdrawRequests,
  getExperimentDashboard,
  getDistributionHome,
  getDistributionRewards,
  getDistributionRewardSummary,
  getDistributionTeam,
  getDistributionTeamWeeklyIncome,
  getWithdrawHistory,
  issuePhoneCode,
  logoutAdminSession,
  logoutAllAdminSessions,
  phoneLogin,
  refreshAdminLinkyEligibility,
  refreshAdminLinkyEligibilityBatch,
  registerInviteBinding,
  recordWithdrawPayment,
  reverseWithdrawPayment,
  resetAdminPassword,
  unlockAdminAccount,
  revokeAdminDeviceSession,
  rejectAdminWithdrawRequest,
  saveAdminGuildConfig,
  updateAdminAccount,
  enrollExperimentParticipant,
  type AdminWithdrawRequestListResponse,
  type BatchOperationResultResponse,
  type AdminAccountResponse,
  type AdminDeviceSessionResponse,
  type AdminSecurityEventResponse,
  type AuditLogListResponse,
  type DistributionHomeResponse,
  type ExperimentDashboardResponse,
  type GuildConfigRequest,
  type GuildConfigResponse,
  type GuildWeeklyReportResponse,
  type InviteBindingResponse,
  type LinkyEligibilityCheckResponse,
  type LinkyBatchRefreshResponse,
  type LinkyReplayRecordListResponse,
  type LinkyWebhookLogListResponse,
  type OverviewReportResponse,
  type OwnershipDetailResponse,
  type ProfileResponse,
  type RelationDetailResponse,
  type RewardListResponse,
  type RewardSummaryResponse,
  type RiskEventListResponse,
  type TeamListResponse,
  type TeamWeeklyIncomeResponse,
  type WithdrawHistoryListResponse,
  type WithdrawRequestResponse,
} from './api'
import {
  buildLinkyReplaySummary,
  buildLinkyWebhookSummary,
  buildPagedResultLabel,
} from './linkyConsole'
import {
  buildLinkyRelatedContext,
  buildLinkyReplayDetailSections,
  buildLinkyWebhookDetailSections,
  buildLinkyWebhookHeadline,
} from './linkyDetails'
void buildLinkyReplaySummary
void buildLinkyWebhookSummary
import {
  buildAdminSectionLinks,
  buildEmptyStatePreset,
  buildLinkyDiagnosticSnapshot,
  deleteNamedFilterView,
  saveNamedFilterView,
  type NamedFilterView,
} from './opsConsole'
import { buildChannelEntryLinks } from './publicEntries'

type SessionState = {
  userId: number
  inviteCode: string
  countryCode: string
  languageCode: string
  accessToken: string
}

type AdminAuthState = {
  sessionToken: string
  expiresAt: string
  username: string
  displayName: string
  role: string
  mustChangePassword?: boolean
  rememberMe?: boolean
  passwordExpiresAt?: string | null
  platformScope?: string
  guildScope?: string
  regionScope?: string
}

type AdminProductKey = 'ALL' | 'LINKY'
type AdminSectionKey = 'overview' | 'channel' | 'bindings' | 'rewards' | 'accounts' | 'settings'
type RiskActionName = 'HANDLE' | 'IGNORE' | 'FREEZE_USER' | 'UNFREEZE_USER'
type WithdrawActionName = 'approve' | 'reject' | 'paid' | 'failed' | 'reverse'
type WithdrawQuery = { userId: string; status: string; page: string; size: string }
type RiskQuery = { userId: string; riskStatus: string; startAt: string; endAt: string; page: string; size: string }
type PendingBatchAction =
  | { kind: 'withdraw'; action: 'APPROVE' | 'REJECT'; targetIds: string[] }
  | { kind: 'risk'; action: 'HANDLE' | 'IGNORE'; targetIds: number[] }

const ADMIN_SECTION_HASHES: Record<AdminSectionKey, string> = {
  overview: '#admin-overview',
  channel: '#admin-channel-entries',
  bindings: '#admin-bindings',
  rewards: '#admin-rewards',
  accounts: '#admin-accounts',
  settings: '#admin-settings',
}

function resolveAdminSectionFromHash(hash?: string): AdminSectionKey {
  const normalized = hash || '#admin-overview'
  const match = (Object.entries(ADMIN_SECTION_HASHES) as Array<[AdminSectionKey, string]>).find(([, value]) => value === normalized)
  if (match) return match[0]
  if (normalized === '#admin-invite-ops') return 'channel'
  if (normalized === '#admin-withdraw-requests') return 'rewards'
  if (normalized === '#admin-onboarding' || normalized === '#admin-user-facts') return 'settings'
  return 'overview'
}

type PendingRiskAction = {
  riskEventId: number
  userId: number
  riskStatus: string
  action: RiskActionName
  note: string
}

type PendingWithdrawAction = {
  requestNo: string
  userId: number
  requestedDiamondAmount: number
  requestStatus: string
  action: WithdrawActionName
}

type PendingAdminAccountAction = {
  account: AdminAccountResponse
  action: 'save' | 'toggle' | 'reset' | 'unlock'
}

type PendingRelationChange = {
  userId: number
  previousInviterId: number | null
  nextInviterId: number | null
  previousLevel2InviterId: number | null
  previousLevel3InviterId: number | null
  note: string
}

type ConsoleAppProps = {
  initialViewMode?: 'user' | 'admin'
  initialAdminSession?: AdminAuthState | null
}

type SelectedLinkyDrawer =
  | { kind: 'webhook'; item: LinkyWebhookLogListResponse['items'][number] }
  | { kind: 'replay'; item: LinkyReplayRecordListResponse['items'][number] }

const STORAGE_KEY = 'fenxiao-web-session'
const PROFILE_CREATE_TOKEN_KEY = 'fenxiao-profile-create-token'
const EXTERNAL_LOCALE_KEY = 'fenxiao-external-locale'
const ADMIN_REWARD_QUERY_KEY = 'fenxiao-admin-reward-query'
const ADMIN_WITHDRAW_QUERY_KEY = 'fenxiao-admin-withdraw-query'
const RISK_QUERY_KEY = 'fenxiao-admin-risk-query'
const ADMIN_WITHDRAW_VIEWS_KEY = 'fenxiao-admin-withdraw-views'
const RISK_VIEWS_KEY = 'fenxiao-admin-risk-views'
const LINKY_WEBHOOK_QUERY_KEY = 'fenxiao-linky-webhook-query'
const LINKY_REPLAY_QUERY_KEY = 'fenxiao-linky-replay-query'
const ADMIN_PRODUCT_OPTIONS: Array<{ value: AdminProductKey; label: string }> = [
  { value: 'ALL', label: '全部产品' },
  { value: 'LINKY', label: 'Linky' },
]
const ADMIN_ROLE_OPTIONS = [
  { value: 'super_admin', label: '最高管理员' }, { value: 'admin', label: '管理员' },
  { value: 'operations', label: '运营' }, { value: 'operator', label: '操作员' },
  { value: 'finance', label: '财务' }, { value: 'customer_support', label: '客服' },
  { value: 'mentor', label: '导师' }, { value: 'team_leader', label: '团队负责人' },
]

function loadExternalLocale(): 'zh' | 'en' | 'es' | 'id' | 'pt' {
  if (typeof window === 'undefined') return 'zh'
  const value = window.localStorage.getItem(EXTERNAL_LOCALE_KEY)
  if (value === 'zh' || value === 'en' || value === 'es' || value === 'id' || value === 'pt') return value
  return 'zh'
}

function loadJsonState<T>(key: string): T | null {
  const storage = typeof window !== 'undefined'
    ? window.localStorage
    : typeof globalThis !== 'undefined' && 'localStorage' in globalThis
      ? globalThis.localStorage
      : null
  const raw = storage?.getItem(key)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

function loadPlainState(key: string): string {
  const storage = typeof window !== 'undefined'
    ? window.localStorage
    : typeof globalThis !== 'undefined' && 'localStorage' in globalThis
      ? globalThis.localStorage
      : null
  return storage?.getItem(key) || ''
}

function saveUserSession(profile: ProfileResponse) {
  const session: SessionState = {
    userId: profile.userId,
    inviteCode: profile.inviteCode,
    countryCode: profile.countryCode,
    languageCode: profile.languageCode,
    accessToken: profile.accessToken,
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  return session
}

function ConsoleApp({ initialViewMode = 'user', initialAdminSession = null }: ConsoleAppProps) {
  void initialViewMode
  const shouldRestoreAdminSession = !initialAdminSession && typeof window !== 'undefined'
    && (window.location.pathname === '/' || window.location.pathname.startsWith('/admin'))
  const [session, setSession] = useState<SessionState | null>(() => loadJsonState<SessionState>(STORAGE_KEY))
  const [adminSession, setAdminSession] = useState<AdminAuthState | null>(initialAdminSession)
  const [adminSessionRestoring, setAdminSessionRestoring] = useState(shouldRestoreAdminSession)
  const [adminUsername, setAdminUsername] = useState('')
  const [adminPassword, setAdminPassword] = useState('')
  const [adminRememberMe, setAdminRememberMe] = useState(true)
  const [adminAccounts, setAdminAccounts] = useState<AdminAccountResponse[]>([])
  const [adminDevices, setAdminDevices] = useState<AdminDeviceSessionResponse[]>([])
  const [adminSecurityEvents, setAdminSecurityEvents] = useState<AdminSecurityEventResponse[]>([])
  const [adminTemporaryPassword, setAdminTemporaryPassword] = useState('')
  const [pendingAdminAccountAction, setPendingAdminAccountAction] = useState<PendingAdminAccountAction | null>(null)
  const [adminAccountForm, setAdminAccountForm] = useState({ username: '', displayName: '', role: 'operator', platformScope: '*', guildScope: '*', regionScope: '*' })
  const [adminPasswordForm, setAdminPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [adminAccountView, setAdminAccountView] = useState<'security' | 'staff' | 'audit'>('security')
  const [adminProduct, setAdminProduct] = useState<AdminProductKey>('ALL')
  const [activeAdminSection, setActiveAdminSection] = useState<AdminSectionKey>(() => resolveAdminSectionFromHash(typeof window !== 'undefined' ? window.location.hash : undefined))
  const [showAdvancedOps, setShowAdvancedOps] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [adminOverview, setAdminOverview] = useState<OverviewReportResponse | null>(null)
  const [adminRewards, setAdminRewards] = useState<RewardListResponse | null>(null)
  const [adminWithdrawRequests, setAdminWithdrawRequests] = useState<AdminWithdrawRequestListResponse | null>(null)
  const [riskEvents, setRiskEvents] = useState<RiskEventListResponse | null>(null)
  const [auditLogs, setAuditLogs] = useState<AuditLogListResponse | null>(null)
  const [adminOwnership, setAdminOwnership] = useState<OwnershipDetailResponse | null>(null)
  const [adminRelation, setAdminRelation] = useState<RelationDetailResponse | null>(null)
  const [linkyWebhookLogs, setLinkyWebhookLogs] = useState<LinkyWebhookLogListResponse | null>(null)
  const [linkyReplayRecords, setLinkyReplayRecords] = useState<LinkyReplayRecordListResponse | null>(null)
  const [hasQueriedAdminRewards, setHasQueriedAdminRewards] = useState(false)
  const [hasQueriedRiskEvents, setHasQueriedRiskEvents] = useState(false)
  const [hasQueriedLinkyWebhookLogs, setHasQueriedLinkyWebhookLogs] = useState(false)
  const [hasQueriedLinkyReplayRecords, setHasQueriedLinkyReplayRecords] = useState(false)
  const [linkyWebhookLoading, setLinkyWebhookLoading] = useState(false)
  const [linkyReplayLoading, setLinkyReplayLoading] = useState(false)
  const [form, setForm] = useState({
    userId: session?.userId?.toString() ?? '',
    countryCode: session?.countryCode ?? 'ID',
    languageCode: session?.languageCode ?? 'id',
    inviteCode: '',
  })
  const [adminRewardQuery, setAdminRewardQuery] = useState(() => loadJsonState<{ beneficiaryUserId: string; status: string; startAt: string; endAt: string; page: string; size: string }>(ADMIN_REWARD_QUERY_KEY) || {
    beneficiaryUserId: '',
    status: '',
    startAt: '',
    endAt: '',
    page: '0',
    size: '10',
  })
  const [adminWithdrawQuery, setAdminWithdrawQuery] = useState(() => loadJsonState<WithdrawQuery>(ADMIN_WITHDRAW_QUERY_KEY) || {
    userId: '',
    status: 'PENDING_REVIEW',
    page: '0',
    size: '10',
  })
  const [adminWithdrawAction, setAdminWithdrawAction] = useState({
    remark: '',
    paymentChannel: 'MANUAL',
    paymentReference: '',
    evidenceUri: '',
    evidenceHash: '',
    failureReason: '',
    reversalReason: '',
    reversalCurrency: 'DIAMOND',
  })
  const [adminWithdrawActionLoadingNo, setAdminWithdrawActionLoadingNo] = useState<string | null>(null)
  const [adminWithdrawActionMessage, setAdminWithdrawActionMessage] = useState('')
  const [pendingWithdrawAction, setPendingWithdrawAction] = useState<PendingWithdrawAction | null>(null)
  const [adminFinanceView, setAdminFinanceView] = useState<'withdrawals' | 'rewards'>('withdrawals')
  const [selectedWithdrawRequestNo, setSelectedWithdrawRequestNo] = useState<string | null>(null)
  const [selectedWithdrawRequestNos, setSelectedWithdrawRequestNos] = useState<string[]>([])
  const [withdrawViews, setWithdrawViews] = useState(() => loadJsonState<NamedFilterView<WithdrawQuery>[]>(ADMIN_WITHDRAW_VIEWS_KEY) || [])
  const [withdrawViewName, setWithdrawViewName] = useState('')
  const [selectedWithdrawViewId, setSelectedWithdrawViewId] = useState('')
  const [adminBindingView, setAdminBindingView] = useState<'users' | 'risks'>('users')
  const [adminSettingsView, setAdminSettingsView] = useState<'experiment' | 'guilds' | 'advanced'>('experiment')
  const [experimentCode, setExperimentCode] = useState('BANDEIRA_V1_100')
  const [experimentDashboard, setExperimentDashboard] = useState<ExperimentDashboardResponse | null>(null)
  const [experimentForm, setExperimentForm] = useState({ name: 'BANDEIRA V1 100人实验', primaryMetricCode: 'FIRST_INCOME', enrollmentStartsAt: '', enrollmentEndsAt: '', observationEndsAt: '' })
  const [experimentParticipant, setExperimentParticipant] = useState({ userId: '', cohortCode: 'BR_LINKY', eligibilitySnapshot: '' })
  const [riskQuery, setRiskQuery] = useState(() => loadJsonState<RiskQuery>(RISK_QUERY_KEY) || {
    userId: '',
    riskStatus: 'PENDING',
    startAt: '',
    endAt: '',
    page: '0',
    size: '10',
  })
  const [linkyWebhookQuery, setLinkyWebhookQuery] = useState(() => loadJsonState<{ linkyOrderId: string; userId: string; requestStatus: string; page: string; size: string }>(LINKY_WEBHOOK_QUERY_KEY) || {
    linkyOrderId: '',
    userId: '',
    requestStatus: '',
    page: '0',
    size: '10',
  })
  const [linkyReplayQuery, setLinkyReplayQuery] = useState(() => loadJsonState<{ linkyOrderId: string; userId: string; page: string; size: string }>(LINKY_REPLAY_QUERY_KEY) || {
    linkyOrderId: '',
    userId: '',
    page: '0',
    size: '10',
  })
  const [auditQuery, setAuditQuery] = useState({
    moduleName: 'risk_event',
    page: '0',
    size: '5',
  })
  const [riskActionDrafts, setRiskActionDrafts] = useState<Record<number, string>>({})
  const [selectedRiskEventIds, setSelectedRiskEventIds] = useState<number[]>([])
  const [riskViews, setRiskViews] = useState(() => loadJsonState<NamedFilterView<RiskQuery>[]>(RISK_VIEWS_KEY) || [])
  const [riskViewName, setRiskViewName] = useState('')
  const [selectedRiskViewId, setSelectedRiskViewId] = useState('')
  const [pendingRiskAction, setPendingRiskAction] = useState<PendingRiskAction | null>(null)
  const [pendingBatchAction, setPendingBatchAction] = useState<PendingBatchAction | null>(null)
  const [batchActionNote, setBatchActionNote] = useState('')
  const [batchActionLoading, setBatchActionLoading] = useState(false)
  const [batchActionResult, setBatchActionResult] = useState<BatchOperationResultResponse | null>(null)
  const [riskActionLoadingId, setRiskActionLoadingId] = useState<number | null>(null)
  const [selectedLinkyDrawer, setSelectedLinkyDrawer] = useState<SelectedLinkyDrawer | null>(null)
  const [ownershipQueryUserId, setOwnershipQueryUserId] = useState('')
  const [ownershipCorrectionProductCode, setOwnershipCorrectionProductCode] = useState('LINKY')
  const [ownershipCorrectionNote, setOwnershipCorrectionNote] = useState('')
  const [ownershipCorrectionLoading, setOwnershipCorrectionLoading] = useState(false)
  const [relationQueryUserId, setRelationQueryUserId] = useState('')
  const [linkyEligibilityAccount, setLinkyEligibilityAccount] = useState('')
  const [linkyEligibilityResult, setLinkyEligibilityResult] = useState<LinkyEligibilityCheckResponse | null>(null)
  const [linkyEligibilityLoading, setLinkyEligibilityLoading] = useState(false)
  const [linkyBatchRefreshResult, setLinkyBatchRefreshResult] = useState<LinkyBatchRefreshResponse | null>(null)
  const [linkyBatchRefreshLoading, setLinkyBatchRefreshLoading] = useState(false)
  const [guildWeeklyQuery, setGuildWeeklyQuery] = useState({ guildId: '', week: 'CURRENT' })
  const [guildWeeklyReport, setGuildWeeklyReport] = useState<GuildWeeklyReportResponse | null>(null)
  const [guildWeeklyLoading, setGuildWeeklyLoading] = useState(false)
  const [guildConfigs, setGuildConfigs] = useState<GuildConfigResponse[] | null>(null)
  const [guildConfigLoading, setGuildConfigLoading] = useState(false)
  const [guildConfigForm, setGuildConfigForm] = useState({
    productCode: 'LINKY',
    inviterUserId: '',
    guildId: '',
    guildName: '',
    guildInviteCode: '',
    enabled: true,
  })
  const [relationAdjustInviterId, setRelationAdjustInviterId] = useState('')
  const [relationAdjustNote, setRelationAdjustNote] = useState('')
  const [relationBeforeAdjust, setRelationBeforeAdjust] = useState<RelationDetailResponse | null>(null)
  const [pendingRelationChange, setPendingRelationChange] = useState<PendingRelationChange | null>(null)
  const [relationAdjustLoading, setRelationAdjustLoading] = useState(false)
  const [profileCreateToken, setProfileCreateToken] = useState(() => loadPlainState(PROFILE_CREATE_TOKEN_KEY))
  const [channelEntryForm, setChannelEntryForm] = useState({
    origin: typeof window !== 'undefined' ? window.location.origin : '',
    country: form.countryCode || 'ID',
    language: form.languageCode || 'id',
    channel: 'whatsapp-main',
    inviteCode: form.inviteCode || session?.inviteCode || '',
  })

  const canLoadAdmin = useMemo(() => Boolean(adminSession), [adminSession])
  const canCreateProfile = useMemo(
    () => Boolean(profileCreateToken.trim() && form.userId.trim() && form.inviteCode.trim()),
    [profileCreateToken, form.userId, form.inviteCode],
  )
  const currentAdminProductLabel = ADMIN_PRODUCT_OPTIONS.find((item) => item.value === adminProduct)?.label ?? '全部产品'
  const activeAdminProductCode = adminProduct === 'ALL' ? undefined : adminProduct
  const adminSectionLinks = useMemo(() => buildAdminSectionLinks(adminSession?.role), [adminSession?.role])
  const canViewAdminSection = (section: AdminSectionKey) => adminSectionLinks.some((item) => item.href === ADMIN_SECTION_HASHES[section])
  const showingProductSpecificDiagnostics = adminProduct === 'LINKY'
  const channelEntryLinks = useMemo(
    () => buildChannelEntryLinks(channelEntryForm.origin, {
      product: adminProduct,
      country: channelEntryForm.country,
      language: channelEntryForm.language,
      channel: channelEntryForm.channel,
      inviteCode: channelEntryForm.inviteCode,
    }),
    [adminProduct, channelEntryForm],
  )

  useEffect(() => {
    if (typeof window === 'undefined') return undefined
    const syncSection = () => setActiveAdminSection(resolveAdminSectionFromHash(window.location.hash))
    syncSection()
    window.addEventListener('hashchange', syncSection)
    return () => window.removeEventListener('hashchange', syncSection)
  }, [])

  useEffect(() => {
    if (!adminSession || adminSectionLinks.some((item) => item.href === ADMIN_SECTION_HASHES[activeAdminSection])) return
    window.location.hash = ADMIN_SECTION_HASHES.overview
  }, [activeAdminSection, adminSectionLinks, adminSession])

  useEffect(() => {
    if (!shouldRestoreAdminSession) return
    let cancelled = false
    void getCurrentAdminSession()
      .then((restored) => { if (!cancelled) setAdminSession(restored) })
      .catch(() => undefined)
      .finally(() => { if (!cancelled) setAdminSessionRestoring(false) })
    return () => { cancelled = true }
  }, [shouldRestoreAdminSession])

  useEffect(() => {
    localStorage.setItem(ADMIN_REWARD_QUERY_KEY, JSON.stringify(adminRewardQuery))
  }, [adminRewardQuery])

  useEffect(() => {
    localStorage.setItem(ADMIN_WITHDRAW_QUERY_KEY, JSON.stringify(adminWithdrawQuery))
  }, [adminWithdrawQuery])

  useEffect(() => {
    localStorage.setItem(RISK_QUERY_KEY, JSON.stringify(riskQuery))
  }, [riskQuery])

  useEffect(() => {
    localStorage.setItem(ADMIN_WITHDRAW_VIEWS_KEY, JSON.stringify(withdrawViews))
  }, [withdrawViews])

  useEffect(() => {
    localStorage.setItem(RISK_VIEWS_KEY, JSON.stringify(riskViews))
  }, [riskViews])

  useEffect(() => {
    if (!import.meta.env.DEV || typeof window === 'undefined'
      || new URLSearchParams(window.location.search).get('adminData') !== 'stress') return
    let cancelled = false
    void import('./adminStressFixtures').then(({ buildAdminStressRiskEvents, buildAdminStressWithdrawRequests }) => {
      if (cancelled) return
      setAdminWithdrawRequests(buildAdminStressWithdrawRequests())
      setRiskEvents(buildAdminStressRiskEvents())
      setHasQueriedRiskEvents(true)
      setSelectedWithdrawRequestNo((current) => current || buildAdminStressWithdrawRequests().items[0]?.requestNo || null)
    })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    localStorage.setItem(LINKY_WEBHOOK_QUERY_KEY, JSON.stringify(linkyWebhookQuery))
  }, [linkyWebhookQuery])

  useEffect(() => {
    localStorage.setItem(LINKY_REPLAY_QUERY_KEY, JSON.stringify(linkyReplayQuery))
  }, [linkyReplayQuery])

  useEffect(() => {
    // Product switching intentionally clears product-scoped admin caches in one render cycle.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setAdminOverview(null)
    setAdminRewards(null)
    setAdminWithdrawRequests(null)
    setRiskEvents(null)
    setAdminOwnership(null)
    setAdminRelation(null)
    setLinkyEligibilityResult(null)
    setLinkyBatchRefreshResult(null)
    setLinkyWebhookLogs(null)
    setLinkyReplayRecords(null)
    setHasQueriedAdminRewards(false)
    setHasQueriedRiskEvents(false)
    setHasQueriedLinkyWebhookLogs(false)
    setHasQueriedLinkyReplayRecords(false)
  }, [adminProduct])

  async function loadAdminRewards(query = adminRewardQuery) {
    if (!adminSession) return
    setLoading(true)
    setError('')
    try {
      const result = await getAdminRewards(adminSession.sessionToken, {
        beneficiaryUserId: query.beneficiaryUserId ? Number(query.beneficiaryUserId) : undefined,
        status: query.status || undefined,
        product: activeAdminProductCode,
        startAt: query.startAt || undefined,
        endAt: query.endAt || undefined,
        page: Number(query.page || 0),
        size: Number(query.size || 10),
      })
      setAdminRewards(result)
      setHasQueriedAdminRewards(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载奖励列表失败')
    } finally {
      setLoading(false)
    }
  }

  async function loadAdminWithdrawRequests(query = adminWithdrawQuery) {
    if (!adminSession) return
    setLoading(true)
    setError('')
    try {
      const result = await getAdminWithdrawRequests(adminSession.sessionToken, {
        userId: query.userId ? Number(query.userId) : undefined,
        status: query.status || undefined,
        page: Number(query.page || 0),
        size: Number(query.size || 10),
      })
      setAdminWithdrawRequests(result)
      setSelectedWithdrawRequestNos([])
      setSelectedWithdrawRequestNo((current) => result.items.some((item) => item.requestNo === current) ? current : result.items[0]?.requestNo ?? null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载提现申请失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleAdminWithdrawAction(requestNo: string, action: WithdrawActionName) {
    if (!adminSession) return
    setAdminWithdrawActionLoadingNo(requestNo)
    setError('')
    setAdminWithdrawActionMessage('')
    try {
      if (action === 'approve') {
        await approveWithdrawForPayment(adminSession.sessionToken, requestNo, adminWithdrawAction.remark.trim() || '财务审核通过，等待打款')
      } else if (action === 'reject') {
        await rejectAdminWithdrawRequest(adminSession.sessionToken, requestNo, { remark: adminWithdrawAction.remark.trim() || '财务拒绝提现申请' })
      } else if (action === 'paid') {
        await recordWithdrawPayment(adminSession.sessionToken, requestNo, {
          paymentChannel: adminWithdrawAction.paymentChannel.trim(), paymentReference: adminWithdrawAction.paymentReference.trim(),
          evidenceUri: adminWithdrawAction.evidenceUri.trim(), evidenceHash: adminWithdrawAction.evidenceHash.trim(),
        }, true)
      } else if (action === 'failed') {
        await recordWithdrawPayment(adminSession.sessionToken, requestNo, {
          paymentChannel: adminWithdrawAction.paymentChannel.trim(), paymentReference: adminWithdrawAction.paymentReference.trim(),
          evidenceUri: adminWithdrawAction.evidenceUri.trim(), evidenceHash: adminWithdrawAction.evidenceHash.trim(),
          failureReason: adminWithdrawAction.failureReason.trim() || '打款失败，等待重试',
        }, false)
      } else {
        await reverseWithdrawPayment(adminSession.sessionToken, requestNo, {
          reason: adminWithdrawAction.reversalReason.trim(),
          currencyCode: adminWithdrawAction.reversalCurrency.trim().toUpperCase(),
        })
      }
      const labels = { approve: '已进入待打款', reject: '已拒绝', paid: '已确认打款', failed: '已记录打款失败', reverse: '已创建冲正账目' }
      setAdminWithdrawActionMessage(`${labels[action]} ${requestNo}`)
      setPendingWithdrawAction(null)
      setAdminWithdrawAction({ remark: '', paymentChannel: 'MANUAL', paymentReference: '', evidenceUri: '', evidenceHash: '', failureReason: '', reversalReason: '', reversalCurrency: 'DIAMOND' })
      await loadAdminWithdrawRequests()
    } catch (err) {
      setError(err instanceof Error ? err.message : '处理提现申请失败')
    } finally {
      setAdminWithdrawActionLoadingNo(null)
    }
  }

  function openWithdrawActionConfirm(item: AdminWithdrawRequestListResponse['items'][number], action: WithdrawActionName) {
    setPendingWithdrawAction({
      requestNo: item.requestNo,
      userId: item.userId,
      requestedDiamondAmount: item.requestedDiamondAmount,
      requestStatus: item.requestStatus,
      action,
    })
  }

  function selectWithdrawRequest(requestNo: string) {
    if (requestNo === selectedWithdrawRequestNo) return
    setSelectedWithdrawRequestNo(requestNo)
    setAdminWithdrawActionMessage('')
    setAdminWithdrawAction({ remark: '', paymentChannel: 'MANUAL', paymentReference: '', evidenceUri: '', evidenceHash: '', failureReason: '', reversalReason: '', reversalCurrency: 'DIAMOND' })
  }

  async function handleWithdrawPageChange(nextPage: number) {
    if (nextPage < 0) return
    const nextQuery = { ...adminWithdrawQuery, page: String(nextPage) }
    setAdminWithdrawQuery(nextQuery)
    await loadAdminWithdrawRequests(nextQuery)
  }

  function resetWithdrawFilters() {
    setAdminWithdrawQuery({ userId: '', status: 'PENDING_REVIEW', page: '0', size: '10' })
    setAdminWithdrawRequests(null)
    setSelectedWithdrawRequestNo(null)
    setSelectedWithdrawRequestNos([])
    setAdminWithdrawActionMessage('')
  }

  function renderAdminWithdrawActions(item: AdminWithdrawRequestListResponse['items'][number]) {
    const busy = !canLoadAdmin || adminWithdrawActionLoadingNo === item.requestNo
    if (item.requestStatus === 'PENDING_REVIEW') return (
      <div className="table-toolbar compact-toolbar">
        <button className="primary-btn small-btn" onClick={() => openWithdrawActionConfirm(item, 'approve')} disabled={busy}>{busy ? '处理中…' : '通过审核'}</button>
        <button className="ghost-btn small-btn" onClick={() => openWithdrawActionConfirm(item, 'reject')} disabled={busy || !adminWithdrawAction.remark.trim()}>拒绝</button>
      </div>
    )
    if (item.requestStatus === 'PAYMENT_PENDING' || item.requestStatus === 'PAYMENT_FAILED') return (
      <div className="table-toolbar compact-toolbar">
        <button className="primary-btn small-btn" onClick={() => openWithdrawActionConfirm(item, 'paid')} disabled={busy || !adminWithdrawAction.paymentChannel.trim() || !adminWithdrawAction.paymentReference.trim()}>{busy ? '处理中…' : '确认已打款'}</button>
        {item.requestStatus === 'PAYMENT_PENDING' ? <button className="ghost-btn small-btn" onClick={() => openWithdrawActionConfirm(item, 'failed')} disabled={busy || !adminWithdrawAction.paymentChannel.trim() || !adminWithdrawAction.failureReason.trim()}>标记失败</button> : null}
      </div>
    )
    if (item.requestStatus === 'PAID_OUT') return (
      <button className="ghost-btn small-btn" onClick={() => openWithdrawActionConfirm(item, 'reverse')} disabled={busy || !adminWithdrawAction.reversalReason.trim() || !adminWithdrawAction.reversalCurrency.trim()}>发起冲正</button>
    )
    return <span className="subtext">已终结</span>
  }

  function saveWithdrawView() {
    try {
      const next = saveNamedFilterView(withdrawViews, withdrawViewName, { ...adminWithdrawQuery, page: '0' })
      setWithdrawViews(next)
      setSelectedWithdrawViewId(next[0].id)
      setWithdrawViewName('')
      setSuccessMessage(`已保存个人筛选视图“${next[0].name}”`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存筛选视图失败')
    }
  }

  function applyWithdrawView(viewId: string) {
    setSelectedWithdrawViewId(viewId)
    const view = withdrawViews.find((item) => item.id === viewId)
    if (!view) return
    setAdminWithdrawQuery({ ...view.query, page: '0' })
    setAdminWithdrawRequests(null)
    setSelectedWithdrawRequestNo(null)
    setSelectedWithdrawRequestNos([])
  }

  function removeWithdrawView() {
    if (!selectedWithdrawViewId) return
    setWithdrawViews(deleteNamedFilterView(withdrawViews, selectedWithdrawViewId))
    setSelectedWithdrawViewId('')
  }

  function saveRiskView() {
    try {
      const next = saveNamedFilterView(riskViews, riskViewName, { ...riskQuery, page: '0' })
      setRiskViews(next)
      setSelectedRiskViewId(next[0].id)
      setRiskViewName('')
      setSuccessMessage(`已保存个人筛选视图“${next[0].name}”`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存筛选视图失败')
    }
  }

  function applyRiskView(viewId: string) {
    setSelectedRiskViewId(viewId)
    const view = riskViews.find((item) => item.id === viewId)
    if (!view) return
    setRiskQuery({ ...view.query, page: '0' })
    setRiskEvents(null)
    setHasQueriedRiskEvents(false)
    setSelectedRiskEventIds([])
  }

  function removeRiskView() {
    if (!selectedRiskViewId) return
    setRiskViews(deleteNamedFilterView(riskViews, selectedRiskViewId))
    setSelectedRiskViewId('')
  }

  async function handleBatchAction() {
    if (!adminSession || !pendingBatchAction) return
    if ((pendingBatchAction.action === 'REJECT' || pendingBatchAction.action === 'IGNORE') && !batchActionNote.trim()) {
      setError('批量拒绝或忽略必须填写统一原因。')
      return
    }
    setBatchActionLoading(true)
    setError('')
    try {
      const result = pendingBatchAction.kind === 'withdraw'
        ? await applyAdminWithdrawBatchAction(adminSession.sessionToken, {
            requestNos: pendingBatchAction.targetIds,
            action: pendingBatchAction.action,
            remark: batchActionNote.trim() || undefined,
          })
        : await applyAdminRiskEventBatchAction(adminSession.sessionToken, {
            riskEventIds: pendingBatchAction.targetIds,
            action: pendingBatchAction.action,
            note: batchActionNote.trim() || undefined,
          })
      setBatchActionResult(result)
      setSuccessMessage(`批量操作完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条。`)
      if (pendingBatchAction.kind === 'withdraw') {
        setSelectedWithdrawRequestNos([])
        await loadAdminWithdrawRequests()
      } else {
        setSelectedRiskEventIds([])
        await loadRiskEvents(riskQuery)
      }
      setPendingBatchAction(null)
      setBatchActionNote('')
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量操作失败')
    } finally {
      setBatchActionLoading(false)
    }
  }

  async function handleLoadExperiment() {
    if (!adminSession || !experimentCode.trim()) return
    setLoading(true); setError('')
    try { setExperimentDashboard(await getExperimentDashboard(adminSession.sessionToken, experimentCode.trim())) }
    catch (err) { setError(err instanceof Error ? err.message : '加载实验看板失败') }
    finally { setLoading(false) }
  }

  async function handleCreateExperiment() {
    if (!adminSession) return
    setLoading(true); setError('')
    try {
      await createExperiment(adminSession.sessionToken, {
        experimentCode: experimentCode.trim(), experimentName: experimentForm.name.trim(), plannedSampleSize: 100,
        primaryMetricCode: experimentForm.primaryMetricCode.trim(), enrollmentStartsAt: experimentForm.enrollmentStartsAt,
        enrollmentEndsAt: experimentForm.enrollmentEndsAt, observationEndsAt: experimentForm.observationEndsAt,
      })
      setSuccessMessage('100 人实验已创建为草稿，确认后再开启招募。')
      await handleLoadExperiment()
    } catch (err) { setError(err instanceof Error ? err.message : '创建实验失败') }
    finally { setLoading(false) }
  }

  async function handleExperimentStatus(status: string) {
    if (!adminSession) return
    setLoading(true); setError('')
    try { await changeExperimentStatus(adminSession.sessionToken, experimentCode.trim(), status, '后台人工确认'); await handleLoadExperiment() }
    catch (err) { setError(err instanceof Error ? err.message : '更新实验状态失败') }
    finally { setLoading(false) }
  }

  async function handleEnrollParticipant() {
    if (!adminSession || !experimentParticipant.userId) return
    setLoading(true); setError('')
    try {
      await enrollExperimentParticipant(adminSession.sessionToken, experimentCode.trim(), {
        userId: Number(experimentParticipant.userId), cohortCode: experimentParticipant.cohortCode.trim(), eligibilitySnapshot: experimentParticipant.eligibilitySnapshot.trim(),
      })
      setExperimentParticipant({ ...experimentParticipant, userId: '', eligibilitySnapshot: '' }); await handleLoadExperiment()
    } catch (err) { setError(err instanceof Error ? err.message : '加入实验队列失败') }
    finally { setLoading(false) }
  }

  async function loadRiskEvents(query = riskQuery) {
    if (!adminSession) return
    setLoading(true)
    setError('')
    try {
      const result = await getAdminRiskEvents(adminSession.sessionToken, {
        userId: query.userId ? Number(query.userId) : undefined,
        riskStatus: query.riskStatus || undefined,
        product: activeAdminProductCode,
        startAt: query.startAt || undefined,
        endAt: query.endAt || undefined,
        page: Number(query.page || 0),
        size: Number(query.size || 10),
      })
      setRiskEvents(result)
      setSelectedRiskEventIds([])
      setHasQueriedRiskEvents(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载风险事件失败')
    } finally {
      setLoading(false)
    }
  }

  async function loadAuditLogs(query = auditQuery) {
    if (!adminSession) return
    try {
      const result = await getAdminAuditLogs(adminSession.sessionToken, {
        moduleName: query.moduleName || undefined,
        page: Number(query.page || 0),
        size: Number(query.size || 5),
      })
      setAuditLogs(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载处理记录失败')
    }
  }

  async function loadLinkyWebhookLogs(query = linkyWebhookQuery) {
    if (!adminSession) return
    setLinkyWebhookLoading(true)
    setError('')
    try {
      const result = await getAdminLinkyWebhookLogs(adminSession.sessionToken, {
        linkyOrderId: query.linkyOrderId.trim() || undefined,
        userId: query.userId ? Number(query.userId) : undefined,
        requestStatus: query.requestStatus || undefined,
        product: activeAdminProductCode,
        page: Number(query.page || 0),
        size: Number(query.size || 10),
      })
      setLinkyWebhookLogs(result)
      setHasQueriedLinkyWebhookLogs(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载 Linky webhook 日志失败')
    } finally {
      setLinkyWebhookLoading(false)
    }
  }

  async function loadLinkyReplayRecords(query = linkyReplayQuery) {
    if (!adminSession) return
    setLinkyReplayLoading(true)
    setError('')
    try {
      const result = await getAdminLinkyReplayRecords(adminSession.sessionToken, {
        linkyOrderId: query.linkyOrderId.trim() || undefined,
        userId: query.userId ? Number(query.userId) : undefined,
        product: activeAdminProductCode,
        page: Number(query.page || 0),
        size: Number(query.size || 10),
      })
      setLinkyReplayRecords(result)
      setHasQueriedLinkyReplayRecords(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载 Linky replay 记录失败')
    } finally {
      setLinkyReplayLoading(false)
    }
  }

  function handleLoadLinkyWebhookLogs() {
    const nextQuery = { ...linkyWebhookQuery, page: '0' }
    setLinkyWebhookQuery(nextQuery)
    void loadLinkyWebhookLogs(nextQuery)
  }

  function handleLoadLinkyReplayRecords() {
    const nextQuery = { ...linkyReplayQuery, page: '0' }
    setLinkyReplayQuery(nextQuery)
    void loadLinkyReplayRecords(nextQuery)
  }

  function handleLinkyWebhookPageChange(nextPage: number) {
    const safePage = Math.max(0, nextPage)
    const nextQuery = { ...linkyWebhookQuery, page: String(safePage) }
    setLinkyWebhookQuery(nextQuery)
    void loadLinkyWebhookLogs(nextQuery)
  }

  function handleLinkyReplayPageChange(nextPage: number) {
    const safePage = Math.max(0, nextPage)
    const nextQuery = { ...linkyReplayQuery, page: String(safePage) }
    setLinkyReplayQuery(nextQuery)
    void loadLinkyReplayRecords(nextQuery)
  }

  async function handleCreateProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const profile = await createProfile(profileCreateToken, {
        userId: Number(form.userId),
        countryCode: form.countryCode.trim().toUpperCase(),
        languageCode: form.languageCode.trim(),
        inviteCode: form.inviteCode.trim(),
      })
      const nextSession = saveUserSession(profile)
      setSession(nextSession)
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建分销档案失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleAdminLogin(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const nextSession = await createAdminSession({ username: adminUsername.trim(), password: adminPassword, rememberMe: adminRememberMe })
      setAdminSession(nextSession)
      if (typeof window !== 'undefined' && window.location.pathname === '/' && window.history?.replaceState) {
        window.history.replaceState(null, '', `/admin${window.location.hash || ''}`)
      }
      setAdminPassword('')
      if (nextSession.mustChangePassword) return
      const [overviewResult, auditResult] = await Promise.all([
        getAdminOverview(nextSession.sessionToken, activeAdminProductCode),
        getAdminAuditLogs(nextSession.sessionToken, {
          moduleName: auditQuery.moduleName,
          page: Number(auditQuery.page || 0),
          size: Number(auditQuery.size || 5),
        }),
      ])
      setAdminOverview(overviewResult)
      setAuditLogs(auditResult)
    } catch (err) {
      setError(err instanceof Error ? err.message : '后台登录失败')
    } finally {
      setLoading(false)
    }
  }

  function clearAdminState() {
    setAdminSession(null)
    setAdminOverview(null)
    setAdminRewards(null)
    setAdminWithdrawRequests(null)
    setRiskEvents(null)
    setAuditLogs(null)
    setAdminRelation(null)
    setLinkyEligibilityResult(null)
    setLinkyWebhookLogs(null)
    setLinkyReplayRecords(null)
    setHasQueriedAdminRewards(false)
    setHasQueriedRiskEvents(false)
    setHasQueriedLinkyWebhookLogs(false)
    setHasQueriedLinkyReplayRecords(false)
    setPendingRiskAction(null)
    setPendingWithdrawAction(null)
    setPendingAdminAccountAction(null)
    setSelectedLinkyDrawer(null)
    setPendingRelationChange(null)
    setRiskActionDrafts({})
    setSuccessMessage('')
  }

  async function handleAdminLogout() {
    try { await logoutAdminSession() } finally { clearAdminState() }
  }

  async function handleChangeAdminPassword(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError('')
    if (adminPasswordForm.newPassword !== adminPasswordForm.confirmPassword) { setError('两次输入的新密码不一致'); return }
    try {
      await changeAdminPassword({ currentPassword: adminPasswordForm.currentPassword, newPassword: adminPasswordForm.newPassword })
      clearAdminState(); setAdminPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err) { setError(err instanceof Error ? err.message : '修改密码失败') }
  }

  async function handleLoadAdminIdentityCenter() {
    setLoading(true); setError('')
    try {
      const [devices, events] = await Promise.all([getAdminDeviceSessions(), getMyAdminSecurityEvents()])
      setAdminDevices(devices); setAdminSecurityEvents(events)
      if (adminSession?.role.toLowerCase() === 'super_admin') setAdminAccounts(await getAdminAccounts())
    } catch (err) { setError(err instanceof Error ? err.message : '加载账号中心失败') }
    finally { setLoading(false) }
  }

  async function handleCreateAdminAccount(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setLoading(true); setError(''); setAdminTemporaryPassword('')
    try {
      const created = await createAdminAccount(adminAccountForm); setAdminTemporaryPassword(created.temporaryPassword)
      setAdminAccountForm({ username: '', displayName: '', role: 'operator', platformScope: '*', guildScope: '*', regionScope: '*' })
      setAdminAccounts(await getAdminAccounts())
    } catch (err) { setError(err instanceof Error ? err.message : '创建员工账号失败') }
    finally { setLoading(false) }
  }

  async function handleToggleAdminAccount(account: AdminAccountResponse) {
    setLoading(true); setError('')
    try { await updateAdminAccount(account.id, { displayName: account.displayName, role: account.role, enabled: !account.enabled, platformScope: account.platformScope, guildScope: account.guildScope, regionScope: account.regionScope }); setAdminAccounts(await getAdminAccounts()); setPendingAdminAccountAction(null); setSuccessMessage(`员工账号已${account.enabled ? '停用' : '恢复'}`) }
    catch (err) { setError(err instanceof Error ? err.message : '更新员工账号失败') } finally { setLoading(false) }
  }

  async function handleSaveAdminAccount(account: AdminAccountResponse) {
    setLoading(true); setError('')
    try { await updateAdminAccount(account.id, { displayName: account.displayName, role: account.role, enabled: account.enabled, platformScope: account.platformScope, guildScope: account.guildScope, regionScope: account.regionScope }); setAdminAccounts(await getAdminAccounts()); setPendingAdminAccountAction(null); setSuccessMessage('员工账号已更新') }
    catch (err) { setError(err instanceof Error ? err.message : '更新员工账号失败') } finally { setLoading(false) }
  }

  async function handleResetAdminPassword(id: number) {
    setLoading(true); setError(''); setAdminTemporaryPassword('')
    try { const result = await resetAdminPassword(id); setAdminTemporaryPassword(result.temporaryPassword); setAdminAccounts(await getAdminAccounts()); setPendingAdminAccountAction(null); setSuccessMessage('员工密码已重置，旧会话已失效') }
    catch (err) { setError(err instanceof Error ? err.message : '重置密码失败') } finally { setLoading(false) }
  }

  async function handleUnlockAdminAccount(id: number) {
    setLoading(true); setError('')
    try { await unlockAdminAccount(id); setAdminAccounts(await getAdminAccounts()); setPendingAdminAccountAction(null); setSuccessMessage('员工账号已解锁') }
    catch (err) { setError(err instanceof Error ? err.message : '解锁账号失败') } finally { setLoading(false) }
  }

  async function handleLogoutAllAdminDevices() { try { await logoutAllAdminSessions() } finally { clearAdminState() } }
  async function handleRevokeAdminDevice(id: number) { await revokeAdminDeviceSession(id); if (adminDevices.find((item) => item.id === id)?.current) clearAdminState(); else setAdminDevices(await getAdminDeviceSessions()) }
  async function handleCopyFingerprint(fingerprint: string) {
    try {
      await navigator.clipboard.writeText(fingerprint)
      setSuccessMessage('Linky replay 指纹已复制到剪贴板。')
    } catch {
      setError('复制 Linky replay 指纹失败，请手动复制。')
    }
  }

  async function handleCopyInviteCode(inviteCode: string) {
    try {
      await navigator.clipboard.writeText(inviteCode)
      setSuccessMessage('邀请码已复制到剪贴板，可直接发给 Linky 用户去绑定页登记。')
    } catch {
      setError('复制邀请码失败，请手动复制。')
    }
  }

  function openExternalLandingPage(url: string) {
    if (typeof window !== 'undefined') {
      window.open(url, '_blank', 'noopener,noreferrer')
    }
  }

  async function copyPublicEntryLink(url: string) {
    try {
      await navigator.clipboard.writeText(url)
      setSuccessMessage(`已复制链接：${url}`)
      setError('')
    } catch {
      setError('复制链接失败，请手动复制。')
    }
  }

  async function handleLoadAdminOverview() {
    if (!adminSession) return
    setLoading(true)
    setError('')
    try {
      const [overview, pendingWithdrawals, pendingRisks] = await Promise.all([
        getAdminOverview(adminSession.sessionToken, activeAdminProductCode),
        canViewAdminSection('rewards')
          ? getAdminWithdrawRequests(adminSession.sessionToken, { status: 'PENDING_REVIEW', page: 0, size: 1 })
          : Promise.resolve(null),
        canViewAdminSection('bindings')
          ? getAdminRiskEvents(adminSession.sessionToken, { riskStatus: 'PENDING', product: activeAdminProductCode, page: 0, size: 1 })
          : Promise.resolve(null),
      ])
      setAdminOverview(overview)
      if (pendingWithdrawals) setAdminWithdrawRequests(pendingWithdrawals)
      if (pendingRisks) setRiskEvents(pendingRisks)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载运营概览失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleLoadAdminRewards() {
    await loadAdminRewards(adminRewardQuery)
  }

  async function handleLoadRiskEvents() {
    await loadRiskEvents(riskQuery)
  }

  async function handleAdminRewardPageChange(nextPage: number) {
    if (nextPage < 0) return
    const nextQuery = { ...adminRewardQuery, page: String(nextPage) }
    setAdminRewardQuery(nextQuery)
    await loadAdminRewards(nextQuery)
  }

  async function handleRiskPageChange(nextPage: number) {
    if (nextPage < 0) return
    const nextQuery = { ...riskQuery, page: String(nextPage) }
    setRiskQuery(nextQuery)
    await loadRiskEvents(nextQuery)
  }

  async function handleRiskAction(actionRequest: PendingRiskAction) {
    if (!adminSession) return
    const { riskEventId, action, note } = actionRequest
    setRiskActionLoadingId(riskEventId)
    setError('')
    setSuccessMessage('')
    try {
      const updatedItem = await applyAdminRiskEventAction(adminSession.sessionToken, riskEventId, {
        action,
        note: note.trim() || undefined,
      })
      setRiskEvents((current) => current ? {
        ...current,
        items: current.items.map((item) => item.id === updatedItem.id ? updatedItem : item),
      } : current)
      if (adminRewards) {
        await loadAdminRewards(adminRewardQuery)
      }
      if (adminRelation && relationQueryUserId && Number(relationQueryUserId) === updatedItem.userId) {
        await handleLoadRelation()
      }
      await loadAuditLogs(auditQuery)
      setRiskActionDrafts((current) => {
        const next = { ...current }
        delete next[riskEventId]
        return next
      })
      setPendingRiskAction(null)
      setSuccessMessage(`风险事件 #${riskEventId} 已执行 ${riskActionLabel(action)}，审计和相关数据已同步刷新。`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '处理风险事件失败')
    } finally {
      setRiskActionLoadingId(null)
    }
  }

  async function handleLoadRelation() {
    if (!adminSession || !relationQueryUserId) return
    setLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const relation = await getAdminRelation(adminSession.sessionToken, Number(relationQueryUserId), activeAdminProductCode)
      setAdminRelation(relation)
      setRelationBeforeAdjust(relation)
      setRelationAdjustInviterId(relation.level1InviterId ? String(relation.level1InviterId) : '')
      setRelationAdjustNote('')
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载关系链失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleRefreshLinkyEligibility() {
    if (!adminSession || !linkyEligibilityAccount.trim()) return
    setLinkyEligibilityLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const result = await refreshAdminLinkyEligibility(adminSession.sessionToken, linkyEligibilityAccount.trim())
      setLinkyEligibilityResult(result)
      setSuccessMessage(`Linky 账号 ${result.linkyAccount} 的资格结果已刷新：${formatEligibilitySummary(result)}`)
    } catch (err) {
      setLinkyEligibilityResult(null)
      setError(err instanceof Error ? err.message : '刷新 Linky 资格失败')
    } finally {
      setLinkyEligibilityLoading(false)
    }
  }

  async function handleRefreshAllLinkyEligibility() {
    if (!adminSession) return
    setLinkyBatchRefreshLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const result = await refreshAdminLinkyEligibilityBatch(adminSession.sessionToken)
      setLinkyBatchRefreshResult(result)
      setSuccessMessage(`已完成全部 Linky 资格批量刷新：成功 ${result.successCount} 条，失败 ${result.failureCount} 条。`)
    } catch (err) {
      setLinkyBatchRefreshResult(null)
      setError(err instanceof Error ? err.message : '批量刷新 Linky 资格失败')
    } finally {
      setLinkyBatchRefreshLoading(false)
    }
  }

  async function handleLoadGuildWeeklyReport() {
    if (!adminSession || !guildWeeklyQuery.guildId.trim()) return
    setGuildWeeklyLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const result = await getAdminGuildWeeklyReport(adminSession.sessionToken, guildWeeklyQuery.guildId.trim(), {
        product: activeAdminProductCode || 'LINKY',
        week: guildWeeklyQuery.week || 'CURRENT',
      })
      setGuildWeeklyReport(result)
      setSuccessMessage(`公会 ${result.guildId} 周报已更新：注册 ${result.registeredUsers} 人，收入 ${result.incomeAmount}，分佣 ${result.rewardAmount}。`)
    } catch (err) {
      setGuildWeeklyReport(null)
      setError(err instanceof Error ? err.message : '加载公会周报失败')
    } finally {
      setGuildWeeklyLoading(false)
    }
  }

  async function handleLoadGuildConfigs() {
    if (!adminSession) return
    setGuildConfigLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const result = await getAdminGuildConfigs(adminSession.sessionToken)
      setGuildConfigs(result)
      setSuccessMessage(`已加载 ${result.length} 条公会配置。`)
    } catch (err) {
      setGuildConfigs(null)
      setError(err instanceof Error ? err.message : '加载公会配置失败')
    } finally {
      setGuildConfigLoading(false)
    }
  }

  async function handleSaveGuildConfig() {
    if (!adminSession || !guildConfigForm.productCode.trim() || !guildConfigForm.guildId.trim() || !guildConfigForm.guildInviteCode.trim()) return
    setGuildConfigLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const payload: GuildConfigRequest = {
        productCode: guildConfigForm.productCode.trim(),
        inviterUserId: guildConfigForm.inviterUserId.trim() ? Number(guildConfigForm.inviterUserId) : null,
        guildId: guildConfigForm.guildId.trim(),
        guildName: guildConfigForm.guildName.trim(),
        guildInviteCode: guildConfigForm.guildInviteCode.trim(),
        enabled: guildConfigForm.enabled,
      }
      const saved = await saveAdminGuildConfig(adminSession.sessionToken, payload)
      setGuildConfigs((current) => {
        const list = current || []
        const withoutSameScope = list.filter((item) => !(item.productCode === saved.productCode && item.inviterUserId === saved.inviterUserId))
        return [saved, ...withoutSameScope]
      })
      setSuccessMessage(`公会配置已保存：${saved.productCode} / ${saved.inviterUserId ?? '默认公会'} → ${saved.guildInviteCode}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存公会配置失败')
    } finally {
      setGuildConfigLoading(false)
    }
  }

  async function handleLoadOwnership() {
    if (!adminSession || !ownershipQueryUserId) return
    setLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const queriedUserId = Number(ownershipQueryUserId)
      const ownership = await getAdminOwnership(adminSession.sessionToken, queriedUserId)
      setAdminOwnership(ownership)
      setRelationQueryUserId(String(queriedUserId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载产品归属失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleLoadJointWorkbench() {
    if (!adminSession || !ownershipQueryUserId.trim()) return
    const queriedUserId = Number(ownershipQueryUserId)
    setLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const jointAuditQuery = { ...auditQuery, moduleName: '', page: '0' }
      const [ownership, relation, audit] = await Promise.all([
        getAdminOwnership(adminSession.sessionToken, queriedUserId),
        getAdminRelation(adminSession.sessionToken, queriedUserId, activeAdminProductCode),
        getAdminAuditLogs(adminSession.sessionToken, {
          moduleName: jointAuditQuery.moduleName || undefined,
          page: Number(jointAuditQuery.page || 0),
          size: Number(jointAuditQuery.size || 5),
        }),
      ])
      setAdminOwnership(ownership)
      setAdminRelation(relation)
      setRelationBeforeAdjust(relation)
      setRelationAdjustInviterId(relation.level1InviterId ? String(relation.level1InviterId) : '')
      setRelationAdjustNote('')
      setRelationQueryUserId(String(queriedUserId))
      setAuditQuery(jointAuditQuery)
      setAuditLogs(audit)
      setShowAdvancedOps(true)
      setSuccessMessage('ownership、绑定关系和联合审计已经一次性同步完成。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '一键联合查询失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleSyncOwnershipRelation() {
    const targetUserId = adminOwnership?.userId ?? (ownershipQueryUserId.trim() ? Number(ownershipQueryUserId) : null)
    if (!adminSession || !targetUserId) return
    setRelationQueryUserId(String(targetUserId))
    setLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const relation = await getAdminRelation(adminSession.sessionToken, targetUserId, activeAdminProductCode)
      setAdminRelation(relation)
      setRelationBeforeAdjust(relation)
      setRelationAdjustInviterId(relation.level1InviterId ? String(relation.level1InviterId) : '')
      setRelationAdjustNote('')
      setSuccessMessage('当前用户的产品归属和绑定关系已经同步到联合处置视图。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '同步绑定关系失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleCorrectOwnership() {
    if (!adminSession || !adminOwnership || !ownershipCorrectionProductCode.trim()) return
    setOwnershipCorrectionLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const updated = await correctAdminOwnership(adminSession.sessionToken, adminOwnership.userId, {
        productCode: ownershipCorrectionProductCode.trim().toUpperCase(),
        note: ownershipCorrectionNote.trim() || undefined,
      })
      setAdminOwnership(updated)
      const ownershipAuditQuery = { ...auditQuery, moduleName: 'ownership', page: '0' }
      setAuditQuery(ownershipAuditQuery)
      setShowAdvancedOps(true)
      await loadAuditLogs(ownershipAuditQuery)
      setSuccessMessage('产品归属已完成人工修正，当前归属和 ownership 审计都已同步刷新。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '人工修正产品归属失败')
    } finally {
      setOwnershipCorrectionLoading(false)
    }
  }

  function handleLoadOwnershipAudit() {
    if (!adminSession) return
    const ownershipAuditQuery = { ...auditQuery, moduleName: 'ownership', page: '0' }
    setAuditQuery(ownershipAuditQuery)
    setShowAdvancedOps(true)
    void loadAuditLogs(ownershipAuditQuery)
  }

  function handleLoadJointAudit() {
    if (!adminSession) return
    const jointAuditQuery = { ...auditQuery, moduleName: '', page: '0' }
    setAuditQuery(jointAuditQuery)
    setShowAdvancedOps(true)
    void loadAuditLogs(jointAuditQuery)
  }

  async function handleAdjustRelation() {
    if (!adminSession || !adminRelation) return
    setRelationAdjustLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const updated = await adjustAdminRelation(adminSession.sessionToken, adminRelation.userId, {
        level1InviterId: relationAdjustInviterId.trim() ? Number(relationAdjustInviterId) : undefined,
        note: relationAdjustNote.trim() || undefined,
      }, activeAdminProductCode)
      setRelationBeforeAdjust(adminRelation)
      setAdminRelation(updated)
      setRelationAdjustInviterId(updated.level1InviterId ? String(updated.level1InviterId) : '')
      const relationAuditQuery = { ...auditQuery, moduleName: 'relation', page: '0' }
      setAuditQuery(relationAuditQuery)
      await loadAuditLogs(relationAuditQuery)
      setPendingRelationChange(null)
      setSuccessMessage('关系链已完成人工修正，before / after 已更新，relation 审计也已同步刷新。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '人工修正关系链失败')
    } finally {
      setRelationAdjustLoading(false)
    }
  }

  function handleProfileCreateTokenSave() {
    localStorage.setItem(PROFILE_CREATE_TOKEN_KEY, profileCreateToken)
  }

  function updateRiskActionDraft(riskEventId: number, note: string) {
    setRiskActionDrafts((current) => ({
      ...current,
      [riskEventId]: note,
    }))
  }

  function openRiskActionConfirm(item: RiskEventListResponse['items'][number], action: RiskActionName) {
    setPendingRiskAction({
      riskEventId: item.id,
      userId: item.userId,
      riskStatus: item.riskStatus,
      action,
      note: riskActionDrafts[item.id] || '',
    })
  }

  function openRelationAdjustConfirm() {
    if (!adminRelation) return
    setPendingRelationChange({
      userId: adminRelation.userId,
      previousInviterId: adminRelation.level1InviterId,
      nextInviterId: relationAdjustInviterId.trim() ? Number(relationAdjustInviterId) : null,
      previousLevel2InviterId: adminRelation.level2InviterId,
      previousLevel3InviterId: adminRelation.level3InviterId,
      note: relationAdjustNote.trim(),
    })
  }

  const processedLinkyRequestCount = linkyWebhookLogs?.items?.filter((item) => item.requestStatus === 'PROCESSED').length ?? 0
  const replayedLinkyRequestCount = linkyReplayRecords?.items?.filter((item) => item.hitCount > 1).length
    ?? linkyWebhookLogs?.items?.filter((item) => item.replayRecordStatus === 'REPLAYED').length
    ?? 0

  const rewardPageLabel = adminRewards
    ? `本次命中 ${adminRewards.total} 条，当前第 ${adminRewards.page + 1} 页，每页 ${adminRewards.size} 条。`
    : '先执行一次奖励查询'
  const riskPageLabel = riskEvents
    ? `本次命中 ${riskEvents.total} 条，当前第 ${riskEvents.page + 1} 页，每页 ${riskEvents.size} 条。`
    : '先执行一次风险事件查询'
  const withdrawPageLabel = adminWithdrawRequests
    ? `共 ${adminWithdrawRequests.total} 笔 · 第 ${adminWithdrawRequests.page + 1} 页 · 每页 ${adminWithdrawRequests.size} 笔`
    : '按条件查询提现申请'
  const rewardEmptyState = buildEmptyStatePreset('reward', hasQueriedAdminRewards)
  const riskEmptyState = buildEmptyStatePreset('risk', hasQueriedRiskEvents)
  const linkyWebhookEmptyState = buildEmptyStatePreset('linky-webhook', hasQueriedLinkyWebhookLogs)
  const linkyReplayEmptyState = buildEmptyStatePreset('linky-replay', hasQueriedLinkyReplayRecords)
  const linkyDiagnosticSnapshot = buildLinkyDiagnosticSnapshot({
    hasQueried: hasQueriedLinkyWebhookLogs || hasQueriedLinkyReplayRecords,
    processedCount: processedLinkyRequestCount,
    failedCount: linkyWebhookLogs?.items?.filter((item) => item.requestStatus === 'FAILED').length ?? 0,
    rejectedCount: linkyWebhookLogs?.items?.filter((item) => item.requestStatus === 'REJECTED').length ?? 0,
    replayedCount: replayedLinkyRequestCount,
  })
  const linkyWebhookPageLabel = buildPagedResultLabel(linkyWebhookLogs ? {
    page: linkyWebhookLogs.page,
    size: linkyWebhookLogs.size,
    total: linkyWebhookLogs.total,
    subject: 'Webhook 日志',
  } : null)
  const linkyReplayPageLabel = buildPagedResultLabel(linkyReplayRecords ? {
    page: linkyReplayRecords.page,
    size: linkyReplayRecords.size,
    total: linkyReplayRecords.total,
    subject: 'Replay 记录',
  } : null)
  const hasRewardPrevPage = Number(adminRewardQuery.page) > 0
  const hasRewardNextPage = adminRewards ? (adminRewards.page + 1) * adminRewards.size < adminRewards.total : false
  const hasRiskPrevPage = Number(riskQuery.page) > 0
  const hasRiskNextPage = riskEvents ? (riskEvents.page + 1) * riskEvents.size < riskEvents.total : false
  const hasWithdrawPrevPage = Number(adminWithdrawQuery.page) > 0
  const hasWithdrawNextPage = adminWithdrawRequests ? (adminWithdrawRequests.page + 1) * adminWithdrawRequests.size < adminWithdrawRequests.total : false
  const hasLinkyWebhookPrevPage = Number(linkyWebhookQuery.page) > 0
  const hasLinkyWebhookNextPage = linkyWebhookLogs ? (linkyWebhookLogs.page + 1) * linkyWebhookLogs.size < linkyWebhookLogs.total : false
  const hasLinkyReplayPrevPage = Number(linkyReplayQuery.page) > 0
  const hasLinkyReplayNextPage = linkyReplayRecords ? (linkyReplayRecords.page + 1) * linkyReplayRecords.size < linkyReplayRecords.total : false
  const selectedWithdrawRequest = adminWithdrawRequests?.items.find((item) => item.requestNo === selectedWithdrawRequestNo) ?? null
  const selectedWithdrawIsReview = selectedWithdrawRequest?.requestStatus === 'PENDING_REVIEW'
  const selectedWithdrawIsPayment = selectedWithdrawRequest?.requestStatus === 'PAYMENT_PENDING' || selectedWithdrawRequest?.requestStatus === 'PAYMENT_FAILED'
  const selectedWithdrawIsPaid = selectedWithdrawRequest?.requestStatus === 'PAID_OUT'
  const relationPreview = adminRelation
    ? buildRelationPreview(adminRelation, relationAdjustInviterId)
    : null
  const activeOwnershipItem = adminOwnership?.items.find((item) => item.ownershipStatus === 'ACTIVE')
  const isJointWorkbenchReady = Boolean(adminOwnership || adminRelation)
  const jointAuditFocus = auditQuery.moduleName || '全部'
  const selectedLinkyTitle = selectedLinkyDrawer?.kind === 'webhook'
    ? buildLinkyWebhookHeadline(selectedLinkyDrawer.item)
    : selectedLinkyDrawer?.item.linkyOrderId
      || `Replay #${selectedLinkyDrawer?.item.id ?? ''}`
  const selectedLinkySections = selectedLinkyDrawer?.kind === 'webhook'
    ? buildLinkyWebhookDetailSections(selectedLinkyDrawer.item)
    : selectedLinkyDrawer?.kind === 'replay'
      ? buildLinkyReplayDetailSections(selectedLinkyDrawer.item)
      : []
  const selectedLinkyRelated = selectedLinkyDrawer
    ? buildLinkyRelatedContext({
        selected: selectedLinkyDrawer,
        webhookItems: linkyWebhookLogs?.items ?? [],
        replayItems: linkyReplayRecords?.items ?? [],
      })
    : null

  void [
    showAdvancedOps,
    auditLogs,
    linkyWebhookLoading,
    linkyReplayLoading,
    setOwnershipQueryUserId,
    setOwnershipCorrectionProductCode,
    setOwnershipCorrectionNote,
    ownershipCorrectionLoading,
    showingProductSpecificDiagnostics,
    handleLoadLinkyWebhookLogs,
    handleLoadLinkyReplayRecords,
    handleLinkyWebhookPageChange,
    handleLinkyReplayPageChange,
    handleCopyFingerprint,
    handleLoadRiskEvents,
    handleRiskPageChange,
    handleLoadOwnership,
    handleLoadJointWorkbench,
    handleSyncOwnershipRelation,
    handleCorrectOwnership,
    handleLoadOwnershipAudit,
    handleLoadJointAudit,
    updateRiskActionDraft,
    openRiskActionConfirm,
    riskPageLabel,
    riskEmptyState,
    linkyWebhookEmptyState,
    linkyReplayEmptyState,
    linkyDiagnosticSnapshot,
    linkyWebhookPageLabel,
    linkyReplayPageLabel,
    hasRiskPrevPage,
    hasRiskNextPage,
    hasLinkyWebhookPrevPage,
    hasLinkyWebhookNextPage,
    hasLinkyReplayPrevPage,
    hasLinkyReplayNextPage,
    activeOwnershipItem,
    isJointWorkbenchReady,
    jointAuditFocus,
    canHandleRisk,
    canIgnoreRisk,
    canFreezeRisk,
    canUnfreezeRisk,
  ]

  if (adminSessionRestoring) {
    return (
      <div className="admin-login-page">
        <section className="admin-login-shell">
          <div className="admin-login-form-panel admin-session-restoring" role="status" aria-live="polite">
            <div className="admin-login-form-head"><h1>正在恢复登录状态</h1></div>
            <p className="inline-hint">正在确认本机登录信息，请稍候。</p>
          </div>
        </section>
      </div>
    )
  }

  if (!adminSession) {
    return (
      <div className="admin-login-page">
        <section className="admin-login-shell">
          <div className="admin-login-form-panel">
            <div className="admin-login-form-head">
              <h1>分销运营后台</h1>
            </div>

            {error ? (
              <section className="alert-banner error admin-login-alert">
                <strong>登录失败</strong>
                <span>{error}</span>
              </section>
            ) : null}

            <form className="admin-login-form" onSubmit={handleAdminLogin}>
              <label>
                后台账号
                <input value={adminUsername} onChange={(e) => setAdminUsername(e.target.value)} placeholder="请输入后台账号" autoComplete="username" autoFocus />
              </label>
              <label>
                登录密码
                <input className="admin-password-input" type="password" value={adminPassword} onChange={(e) => setAdminPassword(e.target.value)} placeholder="请输入登录密码" autoComplete="current-password" />
              </label>
              <label className="admin-remember-row">
                <input type="checkbox" checked={adminRememberMe} onChange={(e) => setAdminRememberMe(e.target.checked)} />
                <span>在本机保持登录；连续 7 天未使用后自动退出</span>
              </label>
              <button className="primary-btn admin-login-submit" type="submit" disabled={loading || !adminUsername.trim() || !adminPassword.trim()}>进入后台</button>
            </form>
          </div>
        </section>
      </div>
    )
  }

  if (adminSession.mustChangePassword) {
    return (
      <div className="admin-login-page">
        <section className="admin-login-shell"><div className="admin-login-form-panel">
          <div className="admin-login-form-head"><h1>首次登录，请修改密码</h1></div>
          {error ? <section className="alert-banner error admin-login-alert"><strong>修改失败</strong><span>{error}</span></section> : null}
          <form className="admin-login-form" onSubmit={handleChangeAdminPassword}>
            <label>临时密码<input type="password" autoComplete="current-password" value={adminPasswordForm.currentPassword} onChange={(e) => setAdminPasswordForm({ ...adminPasswordForm, currentPassword: e.target.value })} /></label>
            <label>新密码<input type="password" autoComplete="new-password" value={adminPasswordForm.newPassword} onChange={(e) => setAdminPasswordForm({ ...adminPasswordForm, newPassword: e.target.value })} /></label>
            <label>确认新密码<input type="password" autoComplete="new-password" value={adminPasswordForm.confirmPassword} onChange={(e) => setAdminPasswordForm({ ...adminPasswordForm, confirmPassword: e.target.value })} /></label>
            <p className="inline-hint">至少 12 位，并包含大小写字母、数字、符号中的至少三类。</p>
            <button className="primary-btn admin-login-submit" type="submit">修改密码并重新登录</button>
          </form>
        </div></section>
      </div>
    )
  }

  return (
    <div className="page-shell admin-console-page admin-console-v3">
      <header className="admin-topbar">
        <div className="admin-page-heading">
          <p className="eyebrow">运营后台</p>
          <h1>{adminSectionLinks.find((item) => item.href === ADMIN_SECTION_HASHES[activeAdminSection])?.label}</h1>
        </div>
        <div className="hero-actions">
          <label className="hero-select-field">
            当前产品
            <select value={adminProduct} onChange={(e) => setAdminProduct(e.target.value as AdminProductKey)}>
              {ADMIN_PRODUCT_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
          </label>
          <div className="admin-account-chip">
            <UserCircle size={28} weight="duotone" />
            <span><strong>{adminSession.displayName}</strong><small>{formatAdminRole(adminSession.role)}</small></span>
          </div>
          <button className="ghost-btn" onClick={handleAdminLogout}>退出</button>
        </div>
      </header>

      {error ? (
        <section className="alert-banner error" role="alert">
          <strong>操作失败</strong>
          <span>{error}</span>
        </section>
      ) : successMessage ? (
        <section className="alert-banner info" role="status" aria-live="polite">
          <strong>已更新</strong>
          <span>{successMessage}</span>
        </section>
      ) : null}

      <aside className="admin-sidebar">
        <div className="admin-nav-strip" id="admin-modules" aria-label="后台模块导航">
          {adminSectionLinks.map((item) => (
            <a key={item.label} className={`admin-nav-chip ${item.href === ADMIN_SECTION_HASHES[activeAdminSection] ? 'is-active' : ''}`} href={item.href} aria-current={item.href === ADMIN_SECTION_HASHES[activeAdminSection] ? 'page' : undefined}>
              <AdminNavIcon label={item.label} />
              <span>{item.label}</span>
            </a>
          ))}
        </div>
        <div className="admin-environment"><span />{window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' ? '本地环境' : '生产环境'}</div>
      </aside>

      <div className="console-layout admin-layout admin-workspace-shell">
        <main className="console-main">
          {activeAdminSection === 'accounts' ? (
            <div className="stack-gap" id="admin-accounts">
              <PanelSection eyebrow="Identity" title="账号与安全中心" description="" action={<button className="primary-btn" onClick={() => void handleLoadAdminIdentityCenter()} disabled={loading}>刷新账号中心</button>}>
                <div className="admin-view-tabs" role="tablist" aria-label="账号中心分类">
                  <button className={adminAccountView === 'security' ? 'is-active' : ''} onClick={() => setAdminAccountView('security')} role="tab" aria-selected={adminAccountView === 'security'}>我的安全</button>
                  {adminSession.role.toLowerCase() === 'super_admin' ? <button className={adminAccountView === 'staff' ? 'is-active' : ''} onClick={() => setAdminAccountView('staff')} role="tab" aria-selected={adminAccountView === 'staff'}>员工与权限</button> : null}
                  <button className={adminAccountView === 'audit' ? 'is-active' : ''} onClick={() => setAdminAccountView('audit')} role="tab" aria-selected={adminAccountView === 'audit'}>安全记录</button>
                </div>
                <div className="admin-account-section" hidden={adminAccountView !== 'security'}>
                <div className="content-grid two-columns entity-grid">
                  <InfoCard title="修改我的密码" tone="neutral">
                    <InfoRow label="密码到期时间" value={adminSession.passwordExpiresAt ? formatDateTime(adminSession.passwordExpiresAt) : '未设置'} />
                    <form className="grid-form compact-form" onSubmit={handleChangeAdminPassword}>
                      <label>当前密码<input type="password" autoComplete="current-password" value={adminPasswordForm.currentPassword} onChange={(e) => setAdminPasswordForm({ ...adminPasswordForm, currentPassword: e.target.value })} /></label>
                      <label>新密码<input type="password" autoComplete="new-password" value={adminPasswordForm.newPassword} onChange={(e) => setAdminPasswordForm({ ...adminPasswordForm, newPassword: e.target.value })} /></label>
                      <label>确认新密码<input type="password" autoComplete="new-password" value={adminPasswordForm.confirmPassword} onChange={(e) => setAdminPasswordForm({ ...adminPasswordForm, confirmPassword: e.target.value })} /></label>
                      <button className="primary-btn small-btn" type="submit">修改并退出全部设备</button>
                    </form>
                  </InfoCard>
                </div>
                <InfoCard title="本机与其他登录设备" tone="neutral">
                  <div className="table-toolbar"><button className="ghost-btn small-btn" onClick={() => void handleLogoutAllAdminDevices()}>退出全部设备</button></div>
                  <DataTable headers={['设备', '最近使用', '到期时间', '网络地址', '状态', '操作']} rows={adminDevices.map((item) => [item.userAgent || '未知设备', formatDateTime(item.lastSeenAt), formatDateTime(item.expiresAt), item.ipAddress || '-', item.current ? '本机' : item.rememberMe ? '保持登录' : '普通会话', <button className="ghost-btn small-btn" onClick={() => void handleRevokeAdminDevice(item.id)}>退出</button>])} emptyText="刷新后查看当前登录设备" />
                </InfoCard>
                </div>
                {adminSession.role.toLowerCase() === 'super_admin' ? (
                  <div className="admin-account-section" hidden={adminAccountView !== 'staff'}>
                    <InfoCard title="新增员工账号" tone="success">
                      <form className="grid-form compact-form exception-filter-grid" onSubmit={handleCreateAdminAccount}>
                        <label>登录账号<input required value={adminAccountForm.username} onChange={(e) => setAdminAccountForm({ ...adminAccountForm, username: e.target.value })} /></label>
                        <label>员工姓名<input required value={adminAccountForm.displayName} onChange={(e) => setAdminAccountForm({ ...adminAccountForm, displayName: e.target.value })} /></label>
                        <label>角色<select value={adminAccountForm.role} onChange={(e) => setAdminAccountForm({ ...adminAccountForm, role: e.target.value })}>{ADMIN_ROLE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
                        <label>平台范围<input value={adminAccountForm.platformScope} onChange={(e) => setAdminAccountForm({ ...adminAccountForm, platformScope: e.target.value })} placeholder="* / TIMO,LINKY" /></label>
                        <label>公会范围<input value={adminAccountForm.guildScope} onChange={(e) => setAdminAccountForm({ ...adminAccountForm, guildScope: e.target.value })} placeholder="* / guild ids" /></label>
                        <label>地区范围<input value={adminAccountForm.regionScope} onChange={(e) => setAdminAccountForm({ ...adminAccountForm, regionScope: e.target.value })} placeholder="* / BR,MX,ID" /></label>
                        <button className="primary-btn small-btn" type="submit">创建员工账号</button>
                      </form>
                      {adminTemporaryPassword ? <div className="alert-banner info top-gap"><strong>一次性临时密码</strong><code>{adminTemporaryPassword}</code><button className="ghost-btn small-btn" onClick={() => navigator.clipboard.writeText(adminTemporaryPassword)}>复制</button></div> : null}
                    </InfoCard>
                    <InfoCard title="员工账号" tone="neutral">
                      <DataTable headers={['账号/姓名', '角色', '平台/公会/地区', '安全状态', '最近登录', '操作']} rows={adminAccounts.map((account, index) => [
                        <div className="stack-gap small"><strong>{account.username}</strong><input value={account.displayName} onChange={(e) => setAdminAccounts((items) => items.map((item, i) => i === index ? { ...item, displayName: e.target.value } : item))} /></div>,
                        <select value={account.role} onChange={(e) => setAdminAccounts((items) => items.map((item, i) => i === index ? { ...item, role: e.target.value } : item))}>{ADMIN_ROLE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select>,
                        <div className="stack-gap small"><input value={account.platformScope} onChange={(e) => setAdminAccounts((items) => items.map((item, i) => i === index ? { ...item, platformScope: e.target.value } : item))} /><input value={account.guildScope} onChange={(e) => setAdminAccounts((items) => items.map((item, i) => i === index ? { ...item, guildScope: e.target.value } : item))} /><input value={account.regionScope} onChange={(e) => setAdminAccounts((items) => items.map((item, i) => i === index ? { ...item, regionScope: e.target.value } : item))} /></div>,
                        <span>{account.enabled ? '已启用' : '已停用'} · {account.lockedUntil ? `锁定至 ${formatDateTime(account.lockedUntil)}` : '未锁定'} · {account.activeSessions} 台设备</span>,
                        formatDateTime(account.lastLoginAt || undefined),
                        <div className="action-row"><button className="ghost-btn small-btn" onClick={() => setPendingAdminAccountAction({ account, action: 'save' })}>保存</button><button className="ghost-btn small-btn" onClick={() => setPendingAdminAccountAction({ account, action: 'toggle' })}>{account.enabled ? '停用' : '恢复'}</button>{account.lockedUntil ? <button className="ghost-btn small-btn" onClick={() => setPendingAdminAccountAction({ account, action: 'unlock' })}>解锁</button> : null}<button className="ghost-btn small-btn" onClick={() => setPendingAdminAccountAction({ account, action: 'reset' })}>重置密码</button></div>,
                      ])} emptyText="点击刷新账号中心加载员工账号" />
                    </InfoCard>
                  </div>
                ) : null}
                <div className="admin-account-section" hidden={adminAccountView !== 'audit'}><InfoCard title="最近安全事件" tone="neutral"><DataTable headers={['时间', '事件', '结果', '网络地址', '说明']} rows={adminSecurityEvents.map((item) => [formatDateTime(item.occurredAt), item.eventType, item.success ? '成功' : '失败', item.ipAddress || '-', item.detail || '-'])} emptyText="刷新后查看最近安全事件" /></InfoCard></div>
              </PanelSection>
            </div>
          ) : null}
          {activeAdminSection === 'settings' ? (
            <div className="admin-view-tabs" role="tablist" aria-label="配置分类">
              <button className={adminSettingsView === 'experiment' ? 'is-active' : ''} onClick={() => setAdminSettingsView('experiment')} role="tab" aria-selected={adminSettingsView === 'experiment'}>100 人实验</button>
              <button className={adminSettingsView === 'guilds' ? 'is-active' : ''} onClick={() => setAdminSettingsView('guilds')} role="tab" aria-selected={adminSettingsView === 'guilds'}>公会配置</button>
              <button className={adminSettingsView === 'advanced' ? 'is-active' : ''} onClick={() => setAdminSettingsView('advanced')} role="tab" aria-selected={adminSettingsView === 'advanced'}>高级接入</button>
            </div>
          ) : null}

          {activeAdminSection === 'settings' ? (
            <div hidden={adminSettingsView !== 'advanced'}>
              <PanelSection
                sectionId="admin-onboarding"
                eyebrow="Onboarding"
                title="分销接入"
                description=""
                action={<button className="primary-btn" onClick={handleProfileCreateTokenSave}>保存接入令牌</button>}
              >
                <div className="stack-gap">
                  <div className="grid-form compact-form single-line wide-line">
                    <label>
                      Profile Create Token
                      <input value={profileCreateToken} onChange={(e) => setProfileCreateToken(e.target.value)} placeholder="请输入创建分销档案的接入令牌" />
                    </label>
                  </div>
                  <form className="grid-form" onSubmit={handleCreateProfile}>
                    <label>
                      用户 ID
                      <input value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })} placeholder="例如 5001" />
                    </label>
                    <label>
                      国家码
                      <input value={form.countryCode} onChange={(e) => setForm({ ...form, countryCode: e.target.value })} placeholder="ID" />
                    </label>
                    <label>
                      语言码
                      <input value={form.languageCode} onChange={(e) => setForm({ ...form, languageCode: e.target.value })} placeholder="id" />
                    </label>
                    <label>
                      邀请码（必填，首批运营请填写初始邀请码）
                      <input required value={form.inviteCode} onChange={(e) => setForm({ ...form, inviteCode: e.target.value })} placeholder="ABCD1234" />
                    </label>
                    <button className="primary-btn" type="submit" disabled={loading || !canCreateProfile}>创建 / 接入</button>
                  </form>
                  {session ? (
                    <InfoCard title="当前用户会话" tone="success">
                      <InfoRow label="用户 ID" value={session.userId} />
                      <InfoRow label="邀请码" value={session.inviteCode} />
                      <InfoRow label="国家 / 语言" value={`${session.countryCode} / ${session.languageCode}`} />
                      <div className="action-row top-gap">
                        <button className="ghost-btn small-btn" type="button" onClick={() => handleCopyInviteCode(session.inviteCode)}>复制邀请码</button>
                      </div>
                    </InfoCard>
                  ) : (
                    <EmptyState title="当前用户会话待接入" description="完成一次接入后，这里会显示当前用户身份、邀请码和令牌信息。" />
                  )}
                </div>
              </PanelSection>
            </div>
          ) : null}

          {activeAdminSection === 'overview' ? (
              <PanelSection
                sectionId="admin-overview"
                eyebrow="Overview"
                title="今日工作台"
                description={`${formatAdminRole(adminSession.role)}视角 · 先处理阻塞，再查看业务趋势`}
                action={<button className="primary-btn" onClick={handleLoadAdminOverview} disabled={loading || !canLoadAdmin}>{loading ? '刷新中…' : '刷新工作台'}</button>}
              >
                <div className="admin-overview-grid">
                  <section className="admin-overview-priority" aria-labelledby="admin-priority-title">
                    <div className="admin-subsection-head"><div><h3 id="admin-priority-title">需要你处理</h3><p>按业务阻塞程度排序</p></div><span>今日</span></div>
                    <div className="admin-task-board" aria-label="运营待办">
                      {canViewAdminSection('rewards') ? <a href="#admin-rewards"><span>待审核提现<small>进入财务队列</small></span><strong>{adminWithdrawRequests?.total ?? '—'}</strong><CaretRight size={16} /></a> : null}
                      {canViewAdminSection('bindings') ? <a href="#admin-bindings" onClick={() => { setAdminBindingView('risks'); if (!riskEvents) void handleLoadRiskEvents() }}><span>待处理异常<small>核验绑定与风险</small></span><strong>{riskEvents?.total ?? adminOverview?.riskEventCount ?? '—'}</strong><CaretRight size={16} /></a> : null}
                      {canViewAdminSection('channel') ? <a href="#admin-channel-entries"><span>渠道入口<small>创建可追踪链接</small></span><strong>生成</strong><CaretRight size={16} /></a> : null}
                      <a href="#admin-accounts"><span>工作台状态<small>{currentAdminProductLabel}</small></span><strong>{adminOverview ? '已更新' : '待刷新'}</strong><CaretRight size={16} /></a>
                    </div>
                  </section>
                  <section className="admin-overview-pulse" aria-labelledby="admin-pulse-title">
                    <div className="admin-subsection-head"><div><h3 id="admin-pulse-title">关键指标</h3><p>当前产品累计数据</p></div></div>
                    <div className="stats-grid">
                      <Metric label="邀请人数" value={adminOverview?.invitedUsers} hint="累计邀请" tone="neutral" />
                      <Metric label="有效人数" value={adminOverview?.effectiveUsers} hint="有效归因" tone="success" />
                      <Metric label="累计奖励" value={adminOverview?.rewardTotal} hint="奖励总额" tone="primary" />
                      <Metric label="冻结奖励" value={adminOverview?.frozenRewardTotal} hint="待复核" tone="warning" />
                      <Metric label="可用奖励" value={adminOverview?.availableRewardTotal} hint="可结算" tone="success" />
                      <Metric label="待处理异常" value={adminOverview?.riskEventCount} hint="需人工处理" tone="danger" />
                    </div>
                  </section>
                </div>
              </PanelSection>
          ) : null}

          {activeAdminSection === 'channel' ? (
              <PanelSection
                sectionId="admin-invite-ops"
                eyebrow="Channel Entry"
                title="渠道入口管理"
                description=""
              >
                <InfoCard title="入口生成条件" tone="neutral">
                  <div className="grid-form compact-form exception-filter-grid">
                    <label>
                      入口域名
                      <input value={channelEntryForm.origin} onChange={(e) => setChannelEntryForm({ ...channelEntryForm, origin: e.target.value })} placeholder="https://your-domain.com" />
                    </label>
                    <label>
                      国家
                      <input value={channelEntryForm.country} onChange={(e) => setChannelEntryForm({ ...channelEntryForm, country: e.target.value })} placeholder="ID / MX / BR" />
                    </label>
                    <label>
                      语言
                      <input value={channelEntryForm.language} onChange={(e) => setChannelEntryForm({ ...channelEntryForm, language: e.target.value })} placeholder="id / es / pt" />
                    </label>
                    <label>
                      渠道标识
                      <input value={channelEntryForm.channel} onChange={(e) => setChannelEntryForm({ ...channelEntryForm, channel: e.target.value })} placeholder="whatsapp-main / meta-id-01" />
                    </label>
                    <label>
                      邀请码
                      <input value={channelEntryForm.inviteCode} onChange={(e) => setChannelEntryForm({ ...channelEntryForm, inviteCode: e.target.value })} placeholder="ABCD1234" />
                    </label>
                  </div>
                  <InlineHint text="自动生成三条渠道链接。" />
                </InfoCard>
                <InfoCard title="追踪参数" tone="success">
                  <div className="relation-grid top-gap">
                    <div className="relation-item"><span>产品</span><strong>{currentAdminProductLabel}</strong></div>
                    <div className="relation-item"><span>国家 / 语言</span><strong>{channelEntryForm.country || '-'} / {channelEntryForm.language || '-'}</strong></div>
                    <div className="relation-item"><span>渠道</span><strong>{channelEntryForm.channel || '-'}</strong></div>
                    <div className="relation-item"><span>邀请码</span><strong>{channelEntryForm.inviteCode || '-'}</strong></div>
                  </div>
                  {channelEntryLinks.map((item) => (
                    <div key={item.key} className="public-entry-item">
                      <InfoRow label={item.label} value={item.url} code />
                      <div className="action-row top-gap public-entry-actions">
                        <button className="ghost-btn small-btn" type="button" onClick={() => openExternalLandingPage(item.url)}>打开页面</button>
                        <button className="ghost-btn small-btn" type="button" onClick={() => copyPublicEntryLink(item.url)}>复制渠道链接</button>
                      </div>
                    </div>
                  ))}
                </InfoCard>
              </PanelSection>
          ) : null}

          {activeAdminSection === 'rewards' ? (
              <div className="admin-finance-workbench">
                <div className="admin-view-tabs" role="tablist" aria-label="收益与提现分类">
                  <button className={adminFinanceView === 'withdrawals' ? 'is-active' : ''} onClick={() => setAdminFinanceView('withdrawals')} role="tab" aria-selected={adminFinanceView === 'withdrawals'}>提现审核</button>
                  <button className={adminFinanceView === 'rewards' ? 'is-active' : ''} onClick={() => setAdminFinanceView('rewards')} role="tab" aria-selected={adminFinanceView === 'rewards'}>奖励流水</button>
                </div>
                {adminFinanceView === 'rewards' ? (
                <PanelSection
                  sectionId="admin-rewards"
                  eyebrow="Rewards"
                  title="收益记录管理"
                  description=""
                  action={<button className="primary-btn" onClick={handleLoadAdminRewards} disabled={loading || !canLoadAdmin}>查询收益记录</button>}
                >
                  <InfoCard title="筛选条件" tone="neutral">
                    <div className="query-shell soft-query-shell compact-query-shell">
                      <div className="grid-form compact-form exception-filter-grid">
                        <label>
                          受益用户 ID
                          <input value={adminRewardQuery.beneficiaryUserId} onChange={(e) => setAdminRewardQuery({ ...adminRewardQuery, beneficiaryUserId: e.target.value })} placeholder="例如 11001" />
                        </label>
                        <label>
                          状态
                          <select value={adminRewardQuery.status} onChange={(e) => setAdminRewardQuery({ ...adminRewardQuery, status: e.target.value })}>
                            <option value="">全部</option>
                            <option value="FROZEN">冻结中</option>
                            <option value="AVAILABLE">可用</option>
                            <option value="RISK_HOLD">风险冻结</option>
                          </select>
                        </label>
                      </div>
                      <InlineHint text={rewardPageLabel} />
                      <div className="table-toolbar compact-toolbar">
                        <button className="ghost-btn small-btn" onClick={() => handleAdminRewardPageChange(Number(adminRewardQuery.page) - 1)} disabled={loading || !hasRewardPrevPage}>上一页</button>
                        <button className="ghost-btn small-btn" onClick={() => handleAdminRewardPageChange(Number(adminRewardQuery.page) + 1)} disabled={loading || !hasRewardNextPage}>下一页</button>
                      </div>
                    </div>
                  </InfoCard>

                  {adminRewards?.items?.length ? (
                    <DataTable
                      headers={['受益用户', '来源用户', '层级', '奖励金额', '状态', '计算时间']}
                      rows={adminRewards.items.map((item) => [
                        item.beneficiaryUserId,
                        item.sourceUserId,
                        item.rewardLevel,
                        item.rewardAmount,
                        renderStatusBadge(item.rewardStatus),
                        formatDateTime(item.calculatedAt),
                      ])}
                      emptyText="暂无后台奖励数据"
                    />
                  ) : (
                    <EmptyState title="暂无后台奖励数据" description="先按受益用户或状态查一页。" actionLabel={rewardEmptyState.actionLabel} />
                  )}
                </PanelSection>
                ) : null}
                {adminFinanceView === 'withdrawals' ? (
                <PanelSection
                  sectionId="admin-withdraw-requests"
                  eyebrow="Withdraw"
                  title="提现申请管理"
                  description="先筛选队列，再在右侧核对详情并完成留痕操作。"
                >
                  <form className="admin-filter-bar" onSubmit={(event) => { event.preventDefault(); void loadAdminWithdrawRequests() }} aria-label="提现队列筛选">
                    <label>用户 ID<input value={adminWithdrawQuery.userId} onChange={(e) => setAdminWithdrawQuery({ ...adminWithdrawQuery, userId: e.target.value, page: '0' })} placeholder="输入用户 ID…" inputMode="numeric" /></label>
                    <label>状态<select value={adminWithdrawQuery.status} onChange={(e) => setAdminWithdrawQuery({ ...adminWithdrawQuery, status: e.target.value, page: '0' })}>
                      <option value="">全部</option><option value="PENDING_REVIEW">待审核</option><option value="PAYMENT_PENDING">待打款</option><option value="PAYMENT_FAILED">打款失败</option><option value="PAID_OUT">已打款</option><option value="REJECTED">已拒绝</option><option value="REVERSED">已冲正</option>
                    </select></label>
                    <div className="admin-filter-actions"><button className="primary-btn small-btn" type="submit" disabled={loading || !canLoadAdmin}>{loading ? '查询中…' : '查询'}</button><button className="ghost-btn small-btn" type="button" onClick={resetWithdrawFilters} disabled={loading}>重置</button></div>
                  </form>
                  <div className="admin-saved-views" aria-label="提现个人筛选视图">
                    <select value={selectedWithdrawViewId} onChange={(event) => applyWithdrawView(event.target.value)} aria-label="选择提现筛选视图"><option value="">个人筛选视图</option>{withdrawViews.map((view) => <option key={view.id} value={view.id}>{view.name}</option>)}</select>
                    <input value={withdrawViewName} onChange={(event) => setWithdrawViewName(event.target.value)} placeholder="给当前筛选命名…" aria-label="提现筛选视图名称" />
                    <button className="ghost-btn small-btn" type="button" onClick={saveWithdrawView} disabled={!withdrawViewName.trim()}>保存视图</button>
                    <button className="ghost-btn small-btn" type="button" onClick={removeWithdrawView} disabled={!selectedWithdrawViewId}>删除</button>
                  </div>
                  <div className="admin-split-workbench" aria-busy={loading}>
                    <div className="admin-queue-pane">
                      {selectedWithdrawRequestNos.length ? <div className="admin-batch-bar" role="region" aria-label="提现批量操作"><strong>已选 {selectedWithdrawRequestNos.length} 笔待审核</strong><div><button className="primary-btn small-btn" type="button" onClick={() => { setBatchActionResult(null); setPendingBatchAction({ kind: 'withdraw', action: 'APPROVE', targetIds: selectedWithdrawRequestNos }) }}>批量通过</button><button className="ghost-btn small-btn" type="button" onClick={() => { setBatchActionResult(null); setPendingBatchAction({ kind: 'withdraw', action: 'REJECT', targetIds: selectedWithdrawRequestNos }) }}>批量拒绝</button><button className="ghost-btn small-btn" type="button" onClick={() => setSelectedWithdrawRequestNos([])}>清空</button></div></div> : null}
                      {batchActionResult ? <BatchResultSummary result={batchActionResult} /> : null}
                      {adminWithdrawRequests?.items?.length ? <DataTable
                        headers={[<input type="checkbox" aria-label="选择本页全部待审核申请" checked={adminWithdrawRequests.items.some((item) => item.requestStatus === 'PENDING_REVIEW') && adminWithdrawRequests.items.filter((item) => item.requestStatus === 'PENDING_REVIEW').every((item) => selectedWithdrawRequestNos.includes(item.requestNo))} onChange={(event) => setSelectedWithdrawRequestNos(event.target.checked ? adminWithdrawRequests.items.filter((item) => item.requestStatus === 'PENDING_REVIEW').map((item) => item.requestNo) : [])} />, '申请单号', '用户 ID', '申请钻石', '状态', '申请时间']}
                        rows={adminWithdrawRequests.items.map((item) => [
                          <input type="checkbox" aria-label={`选择提现申请 ${item.requestNo}`} disabled={item.requestStatus !== 'PENDING_REVIEW'} checked={selectedWithdrawRequestNos.includes(item.requestNo)} onChange={(event) => setSelectedWithdrawRequestNos((current) => event.target.checked ? [...current, item.requestNo] : current.filter((requestNo) => requestNo !== item.requestNo))} />,
                          <button className={`admin-table-link ${selectedWithdrawRequestNo === item.requestNo ? 'is-active' : ''}`} onClick={() => selectWithdrawRequest(item.requestNo)} aria-pressed={selectedWithdrawRequestNo === item.requestNo}>{item.requestNo}</button>,
                          item.userId, item.requestedDiamondAmount, renderStatusBadge(item.requestStatus), formatDateTime(item.requestedAt),
                        ])}
                        rowClassNames={adminWithdrawRequests.items.map((item) => selectedWithdrawRequestNo === item.requestNo ? 'is-selected' : '')}
                        emptyText="暂无提现申请"
                      /> : <EmptyState title="暂无提现申请" description="按用户或状态查询后，申请会显示在处理队列。" actionLabel="建议先查看待审核" />}
                      <div className="admin-pagination"><span className="admin-page-note" role="status">{withdrawPageLabel}</span><div><button className="ghost-btn small-btn" type="button" onClick={() => void handleWithdrawPageChange(Number(adminWithdrawQuery.page) - 1)} disabled={loading || !hasWithdrawPrevPage}>上一页</button><button className="ghost-btn small-btn" type="button" onClick={() => void handleWithdrawPageChange(Number(adminWithdrawQuery.page) + 1)} disabled={loading || !hasWithdrawNextPage}>下一页</button></div></div>
                    </div>
                    <aside className={`admin-detail-pane ${selectedWithdrawRequest ? 'is-open' : ''}`} aria-label="提现申请详情">
                      {selectedWithdrawRequest ? <>
                        <button className="admin-mobile-detail-close" type="button" onClick={() => setSelectedWithdrawRequestNo(null)} aria-label="关闭申请详情">关闭</button>
                        <div className="admin-detail-heading"><div><span>申请单</span><h3>{selectedWithdrawRequest.requestNo}</h3></div>{renderStatusBadge(selectedWithdrawRequest.requestStatus)}</div>
                        <div className="admin-detail-facts"><InfoRow label="用户 ID" value={selectedWithdrawRequest.userId} /><InfoRow label="申请钻石" value={selectedWithdrawRequest.requestedDiamondAmount} /><InfoRow label="申请周" value={selectedWithdrawRequest.requestWeek} /><InfoRow label="申请时间" value={formatDateTime(selectedWithdrawRequest.requestedAt)} /></div>
                        {selectedWithdrawIsReview ? <div className="admin-action-form"><label>审批备注<input value={adminWithdrawAction.remark} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, remark: e.target.value })} placeholder="通过说明；拒绝时必须填写原因…" /></label></div> : null}
                        {selectedWithdrawIsPayment ? <div className="admin-action-form">
                          <label>打款渠道<input value={adminWithdrawAction.paymentChannel} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, paymentChannel: e.target.value })} placeholder="MANUAL / PIX" /></label>
                          <label>支付凭证号<input value={adminWithdrawAction.paymentReference} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, paymentReference: e.target.value })} placeholder="确认打款时必须填写流水号…" /></label>
                          <label>打款失败原因<input value={adminWithdrawAction.failureReason} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, failureReason: e.target.value })} placeholder="标记失败时必须填写原因…" /></label>
                          <details><summary>补充审计凭证</summary><div className="stack-gap small top-gap"><label>凭证地址<input value={adminWithdrawAction.evidenceUri} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, evidenceUri: e.target.value })} placeholder="受控存储中的凭证地址…" /></label><label>凭证摘要<input value={adminWithdrawAction.evidenceHash} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, evidenceHash: e.target.value })} placeholder="SHA-256" spellCheck={false} /></label></div></details>
                        </div> : null}
                        {selectedWithdrawIsPaid ? <div className="admin-action-form"><label>冲正原因<input value={adminWithdrawAction.reversalReason} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, reversalReason: e.target.value })} placeholder="必须说明冲正依据…" /></label><label>账本币种<input value={adminWithdrawAction.reversalCurrency} onChange={(e) => setAdminWithdrawAction({ ...adminWithdrawAction, reversalCurrency: e.target.value.toUpperCase() })} placeholder="DIAMOND" /></label></div> : null}
                        <InlineHint text={selectedWithdrawIsReview ? '拒绝必须填写原因；提交前会再次确认。' : selectedWithdrawIsPayment ? '确认打款必须填写渠道与流水号；失败必须填写原因。' : selectedWithdrawIsPaid ? '已打款记录不可删除；纠错必须创建冲正账目并保留完整历史。' : '该申请已进入终态，仅保留审计查看。'} />
                        {adminWithdrawActionMessage ? <InlineHint text={adminWithdrawActionMessage} /> : null}
                        <div className="admin-detail-actions">{renderAdminWithdrawActions(selectedWithdrawRequest)}</div>
                      </> : <EmptyState title="选择一笔申请" description="从左侧队列选择申请后，在这里完成审核和打款留痕。" />}
                    </aside>
                  </div>
                </PanelSection>
                ) : null}
              </div>
          ) : null}

          {activeAdminSection === 'bindings' || activeAdminSection === 'settings' ? (
              <div className="admin-workbench-container" hidden={activeAdminSection === 'settings' && adminSettingsView === 'advanced'}>
                <PanelSection
                  sectionId="admin-bindings"
                  eyebrow="Bindings"
                  title={activeAdminSection === 'settings' ? '配置' : '绑定关系管理'}
                  description=""
                  action={activeAdminSection === 'bindings' && adminBindingView === 'users' ? <button className="primary-btn" onClick={handleLoadRelation} disabled={loading || !canLoadAdmin || !relationQueryUserId}>查询用户关系</button> : undefined}
                >
                  {activeAdminSection === 'bindings' ? (
                    <>
                  <div className="admin-view-tabs" role="tablist" aria-label="绑定与风险分类">
                    <button className={adminBindingView === 'users' ? 'is-active' : ''} onClick={() => setAdminBindingView('users')} role="tab" aria-selected={adminBindingView === 'users'}>用户与绑定</button>
                    <button className={adminBindingView === 'risks' ? 'is-active' : ''} onClick={() => { setAdminBindingView('risks'); if (!riskEvents) void handleLoadRiskEvents() }} role="tab" aria-selected={adminBindingView === 'risks'}>风险队列{riskEvents?.total ? ` · ${riskEvents.total}` : ''}</button>
                  </div>
                  <div className="admin-binding-user-workbench" hidden={adminBindingView !== 'users'}>
                  <InfoCard title="查询入口" tone="neutral">
                    <div className="grid-form compact-form single-line">
                      <label>
                        用户 ID
                        <input value={relationQueryUserId} onChange={(e) => setRelationQueryUserId(e.target.value)} placeholder="例如 10003" />
                      </label>
                    </div>
                    <InlineHint text="查询当前关系。" />
                  </InfoCard>
                  <InfoCard title="Linky 资格核验" tone="neutral">
                    <div className="grid-form compact-form single-line">
                      <label>
                        Linky 账号
                        <input value={linkyEligibilityAccount} onChange={(e) => setLinkyEligibilityAccount(e.target.value)} placeholder="例如 12345678" />
                      </label>
                    </div>
                    <InlineHint text="校验公会归属。" />
                    <div className="table-toolbar top-gap">
                      <button className="primary-btn small-btn" onClick={handleRefreshLinkyEligibility} disabled={linkyEligibilityLoading || !canLoadAdmin || !linkyEligibilityAccount.trim()}>
                        {linkyEligibilityLoading ? '刷新中…' : '刷新资格结果'}
                      </button>
                      <button className="ghost-btn small-btn" onClick={handleRefreshAllLinkyEligibility} disabled={linkyBatchRefreshLoading || !canLoadAdmin}>
                        {linkyBatchRefreshLoading ? '批量刷新中…' : '批量刷新全部 Linky 资格'}
                      </button>
                    </div>
                    <InlineHint text="批量刷新资格。" />
                    {linkyBatchRefreshResult ? (
                      <div className="relation-grid top-gap">
                        <RelationItem label="成功数量" value={linkyBatchRefreshResult.successCount} />
                        <RelationItem label="失败数量" value={linkyBatchRefreshResult.failureCount} />
                      </div>
                    ) : (
                      <div className="relation-grid top-gap">
                        <RelationItem label="成功数量" value="-" />
                        <RelationItem label="失败数量" value="-" />
                      </div>
                    )}
                    {linkyEligibilityResult ? (
                      <div className="relation-grid top-gap">
                        <RelationItem label="Linky 账号" value={linkyEligibilityResult.linkyAccount} />
                        <RelationItem label="公会判定" value={renderEligibilityStatusBadge(linkyEligibilityResult.guildCheckStatus)} />
                        <RelationItem label="注册资格" value={renderEligibilityStatusBadge(linkyEligibilityResult.registrationEligibility)} />
                        <RelationItem label="公会 ID" value={linkyEligibilityResult.guildId ?? '-'} />
                        <RelationItem label="公会名称" value={linkyEligibilityResult.guildName ?? '-'} />
                        <RelationItem label="检查时间" value={linkyEligibilityResult.checkedAt ? formatDateTime(linkyEligibilityResult.checkedAt) : '-'} />
                        <RelationItem label="结果说明" value={linkyEligibilityResult.remark ?? '-'} />
                      </div>
                    ) : (
                      <EmptyState title="还没有资格结果" description="输入 Linky 账号后刷新，这里会显示当前公会归属和注册资格。" actionLabel="推荐先刷一条真实账号" />
                    )}
                  </InfoCard>
                  </div>
                  <div className="admin-risk-workbench" hidden={adminBindingView !== 'risks'}>
                    <form className="admin-filter-bar" onSubmit={(event) => { event.preventDefault(); void handleLoadRiskEvents() }} aria-label="风险队列筛选">
                      <label>用户 ID<input value={riskQuery.userId} onChange={(e) => setRiskQuery({ ...riskQuery, userId: e.target.value, page: '0' })} placeholder="输入用户 ID…" inputMode="numeric" /></label>
                      <label>状态<select value={riskQuery.riskStatus} onChange={(e) => setRiskQuery({ ...riskQuery, riskStatus: e.target.value, page: '0' })}><option value="PENDING">待处理</option><option value="HANDLED">已处理</option><option value="IGNORED">已忽略</option><option value="">全部</option></select></label>
                      <div className="admin-filter-actions"><button className="primary-btn small-btn" type="submit" disabled={loading || !canLoadAdmin}>{loading ? '查询中…' : '查询'}</button><button className="ghost-btn small-btn" type="button" onClick={() => { setRiskQuery({ userId: '', riskStatus: 'PENDING', startAt: '', endAt: '', page: '0', size: '10' }); setRiskEvents(null); setHasQueriedRiskEvents(false) }} disabled={loading}>重置</button></div>
                    </form>
                    <div className="admin-saved-views" aria-label="风险个人筛选视图"><select value={selectedRiskViewId} onChange={(event) => applyRiskView(event.target.value)} aria-label="选择风险筛选视图"><option value="">个人筛选视图</option>{riskViews.map((view) => <option key={view.id} value={view.id}>{view.name}</option>)}</select><input value={riskViewName} onChange={(event) => setRiskViewName(event.target.value)} placeholder="给当前筛选命名…" aria-label="风险筛选视图名称" /><button className="ghost-btn small-btn" type="button" onClick={saveRiskView} disabled={!riskViewName.trim()}>保存视图</button><button className="ghost-btn small-btn" type="button" onClick={removeRiskView} disabled={!selectedRiskViewId}>删除</button></div>
                    {selectedRiskEventIds.length ? <div className="admin-batch-bar" role="region" aria-label="风险批量操作"><strong>已选 {selectedRiskEventIds.length} 条待处理风险</strong><div><button className="primary-btn small-btn" type="button" onClick={() => { setBatchActionResult(null); setPendingBatchAction({ kind: 'risk', action: 'HANDLE', targetIds: selectedRiskEventIds }) }}>批量处理</button><button className="ghost-btn small-btn" type="button" onClick={() => { setBatchActionResult(null); setPendingBatchAction({ kind: 'risk', action: 'IGNORE', targetIds: selectedRiskEventIds }) }}>批量忽略</button><button className="ghost-btn small-btn" type="button" onClick={() => setSelectedRiskEventIds([])}>清空</button></div></div> : null}
                    {batchActionResult ? <BatchResultSummary result={batchActionResult} /> : null}
                    {riskEvents?.items?.length ? <DataTable headers={[<input type="checkbox" aria-label="选择本页全部待处理风险" checked={riskEvents.items.some((item) => item.riskStatus === 'PENDING') && riskEvents.items.filter((item) => item.riskStatus === 'PENDING').every((item) => selectedRiskEventIds.includes(item.id))} onChange={(event) => setSelectedRiskEventIds(event.target.checked ? riskEvents.items.filter((item) => item.riskStatus === 'PENDING').map((item) => item.id) : [])} />, '事件', '用户', '风险', '状态', '发现时间', '处理']} rows={riskEvents.items.map((item) => [
                      <input type="checkbox" aria-label={`选择风险事件 ${item.id}`} disabled={item.riskStatus !== 'PENDING'} checked={selectedRiskEventIds.includes(item.id)} onChange={(event) => setSelectedRiskEventIds((current) => event.target.checked ? [...current, item.id] : current.filter((riskEventId) => riskEventId !== item.id))} />,
                      `#${item.id}`, `#${item.userId}`, <div><strong>{item.riskType}</strong><small className="admin-cell-note">等级 {item.riskLevel}</small></div>, renderStatusBadge(item.riskStatus), formatDateTime(item.detectedAt),
                      <div className="admin-row-actions"><input value={riskActionDrafts[item.id] || ''} onChange={(e) => updateRiskActionDraft(item.id, e.target.value)} placeholder="处理备注…" aria-label={`风险事件 ${item.id} 处理备注`} />{item.riskStatus === 'PENDING' ? <><button className="primary-btn small-btn" onClick={() => openRiskActionConfirm(item, 'HANDLE')} disabled={riskActionLoadingId === item.id}>处理</button><button className="ghost-btn small-btn" onClick={() => openRiskActionConfirm(item, 'IGNORE')} disabled={riskActionLoadingId === item.id || !(riskActionDrafts[item.id] || '').trim()}>忽略</button><button className="ghost-btn small-btn" onClick={() => openRiskActionConfirm(item, 'FREEZE_USER')} disabled={riskActionLoadingId === item.id || !(riskActionDrafts[item.id] || '').trim()}>冻结用户</button></> : item.riskStatus === 'HANDLED' ? <button className="ghost-btn small-btn" onClick={() => openRiskActionConfirm(item, 'UNFREEZE_USER')} disabled={riskActionLoadingId === item.id}>解冻用户</button> : null}</div>,
                    ])} emptyText="当前筛选下没有风险事件" /> : <EmptyState title="暂无待处理风险" description="当前筛选下没有需要人工处置的事件。" actionLabel="可切换状态查看历史" />}
                    <InlineHint text="忽略或冻结用户属于高影响操作，必须先填写处理备注并二次确认。" />
                    <div className="table-toolbar"><button className="ghost-btn small-btn" onClick={() => handleRiskPageChange(Number(riskQuery.page) - 1)} disabled={!hasRiskPrevPage}>上一页</button><span className="admin-page-note">{riskPageLabel}</span><button className="ghost-btn small-btn" onClick={() => handleRiskPageChange(Number(riskQuery.page) + 1)} disabled={!hasRiskNextPage}>下一页</button></div>
                  </div>
                    </>
                  ) : null}

                  {activeAdminSection === 'settings' ? (
                    <>
                  <div hidden={adminSettingsView !== 'experiment'}>
                  <InfoCard title="100 人验证实验" tone="neutral">
                    <div className="grid-form compact-form exception-filter-grid">
                      <label>实验代码<input value={experimentCode} onChange={(e) => setExperimentCode(e.target.value)} placeholder="BANDEIRA_V1_100" /></label>
                      <label>实验名称<input value={experimentForm.name} onChange={(e) => setExperimentForm({ ...experimentForm, name: e.target.value })} /></label>
                      <label>主指标<input value={experimentForm.primaryMetricCode} onChange={(e) => setExperimentForm({ ...experimentForm, primaryMetricCode: e.target.value })} placeholder="FIRST_INCOME" /></label>
                      <label>招募开始<input type="datetime-local" value={experimentForm.enrollmentStartsAt} onChange={(e) => setExperimentForm({ ...experimentForm, enrollmentStartsAt: e.target.value })} /></label>
                      <label>招募结束<input type="datetime-local" value={experimentForm.enrollmentEndsAt} onChange={(e) => setExperimentForm({ ...experimentForm, enrollmentEndsAt: e.target.value })} /></label>
                      <label>观察结束<input type="datetime-local" value={experimentForm.observationEndsAt} onChange={(e) => setExperimentForm({ ...experimentForm, observationEndsAt: e.target.value })} /></label>
                    </div>
                    <InlineHint text="样本上限固定为 100；退出用户仍计入固定分母，生产不自动生成测试用户或指标。" />
                    <div className="table-toolbar top-gap">
                      <button className="ghost-btn small-btn" onClick={() => void handleLoadExperiment()} disabled={loading || !experimentCode.trim()}>加载看板</button>
                      <button className="primary-btn small-btn" onClick={() => void handleCreateExperiment()} disabled={loading || !experimentForm.enrollmentStartsAt || !experimentForm.enrollmentEndsAt || !experimentForm.observationEndsAt}>创建草稿</button>
                      <button className="ghost-btn small-btn" onClick={() => void handleExperimentStatus('ENROLLING')} disabled={loading || experimentDashboard?.status !== 'DRAFT'}>开启招募</button>
                      <button className="ghost-btn small-btn" onClick={() => void handleExperimentStatus('RUNNING')} disabled={loading || experimentDashboard?.status !== 'ENROLLING'}>开始观察</button>
                      <button className="ghost-btn small-btn" onClick={() => void handleExperimentStatus('COMPLETED')} disabled={loading || experimentDashboard?.status !== 'RUNNING'}>完成实验</button>
                    </div>
                    {experimentDashboard ? <div className="relation-grid top-gap">
                      <RelationItem label="状态" value={renderStatusBadge(experimentDashboard.status)} />
                      <RelationItem label="固定分母" value={`${experimentDashboard.fixedDenominator} / ${experimentDashboard.plannedSampleSize}`} />
                      <RelationItem label="观察中" value={experimentDashboard.active} />
                      <RelationItem label="已完成" value={experimentDashboard.completed} />
                      <RelationItem label="退出（仍计分母）" value={experimentDashboard.withdrawn} />
                      <RelationItem label={experimentDashboard.primaryMetricCode} value={`${experimentDashboard.convertedCount} 人 / ${experimentDashboard.metricTotal}`} />
                    </div> : null}
                    <div className="grid-form compact-form exception-filter-grid top-gap">
                      <label>用户 ID<input value={experimentParticipant.userId} onChange={(e) => setExperimentParticipant({ ...experimentParticipant, userId: e.target.value })} /></label>
                      <label>队列分组<input value={experimentParticipant.cohortCode} onChange={(e) => setExperimentParticipant({ ...experimentParticipant, cohortCode: e.target.value })} /></label>
                      <label>资格快照<input value={experimentParticipant.eligibilitySnapshot} onChange={(e) => setExperimentParticipant({ ...experimentParticipant, eligibilitySnapshot: e.target.value })} placeholder='{"phoneVerified":true}' /></label>
                    </div>
                    <button className="primary-btn small-btn top-gap" onClick={() => void handleEnrollParticipant()} disabled={loading || experimentDashboard?.status !== 'ENROLLING' || !experimentParticipant.userId || !experimentParticipant.eligibilitySnapshot.trim()}>加入实验队列</button>
                  </InfoCard>
                  </div>
                  <div hidden={adminSettingsView !== 'guilds'}>
                  <InfoCard title="公会周报" tone="success">
                    <div className="grid-form compact-form exception-filter-grid">
                      <label>
                        公会 ID
                        <input value={guildWeeklyQuery.guildId} onChange={(e) => setGuildWeeklyQuery({ ...guildWeeklyQuery, guildId: e.target.value })} placeholder="例如 GUILD-A" />
                      </label>
                      <label>
                        周期
                        <select value={guildWeeklyQuery.week} onChange={(e) => setGuildWeeklyQuery({ ...guildWeeklyQuery, week: e.target.value })}>
                          <option value="CURRENT">本周</option>
                          <option value="PREVIOUS">上周</option>
                        </select>
                      </label>
                    </div>
                    <InlineHint text="按公会聚合。" />
                    <div className="table-toolbar top-gap">
                      <button className="primary-btn small-btn" onClick={handleLoadGuildWeeklyReport} disabled={guildWeeklyLoading || !canLoadAdmin || !guildWeeklyQuery.guildId.trim()}>
                        {guildWeeklyLoading ? '查询中…' : '查询公会周报'}
                      </button>
                    </div>
                    {guildWeeklyReport ? (
                      <div className="relation-grid top-gap">
                        <RelationItem label="产品" value={guildWeeklyReport.productCode} />
                        <RelationItem label="公会 ID" value={guildWeeklyReport.guildId} />
                        <RelationItem label="周期" value={guildWeeklyReport.week} />
                        <RelationItem label="注册用户" value={guildWeeklyReport.registeredUsers} />
                        <RelationItem label="收入金额" value={guildWeeklyReport.incomeAmount} />
                        <RelationItem label="贡献分佣" value={guildWeeklyReport.rewardAmount} />
                      </div>
                    ) : (
                      <EmptyState title="还没有公会周报" description="输入公会 ID 后查询，这里会显示真实聚合后的注册、收入和分佣数据。" />
                    )}
                  </InfoCard>
                  <InfoCard title="公会配置管理" tone="neutral">
                    <div className="grid-form compact-form exception-filter-grid">
                      <label>
                        产品
                        <input value={guildConfigForm.productCode} onChange={(e) => setGuildConfigForm({ ...guildConfigForm, productCode: e.target.value })} placeholder="LINKY" />
                      </label>
                      <label>
                        上级用户 ID（为空则为默认公会）
                        <input value={guildConfigForm.inviterUserId} onChange={(e) => setGuildConfigForm({ ...guildConfigForm, inviterUserId: e.target.value })} placeholder="例如 1001" />
                      </label>
                      <label>
                        Linky 公会 ID
                        <input value={guildConfigForm.guildId} onChange={(e) => setGuildConfigForm({ ...guildConfigForm, guildId: e.target.value })} placeholder="例如 LINKY_GUILD_A" />
                      </label>
                      <label>
                        公会名称
                        <input value={guildConfigForm.guildName} onChange={(e) => setGuildConfigForm({ ...guildConfigForm, guildName: e.target.value })} placeholder="例如 Linky A Guild" />
                      </label>
                      <label>
                        公会邀请码
                        <input value={guildConfigForm.guildInviteCode} onChange={(e) => setGuildConfigForm({ ...guildConfigForm, guildInviteCode: e.target.value })} placeholder="例如 JOIN-A" />
                      </label>
                      <label>
                        启用状态
                        <select value={guildConfigForm.enabled ? 'ENABLED' : 'DISABLED'} onChange={(e) => setGuildConfigForm({ ...guildConfigForm, enabled: e.target.value === 'ENABLED' })}>
                          <option value="ENABLED">启用</option>
                          <option value="DISABLED">停用</option>
                        </select>
                      </label>
                    </div>
                    <InlineHint text="维护公会映射。" />
                    <div className="table-toolbar top-gap">
                      <button className="ghost-btn small-btn" onClick={handleLoadGuildConfigs} disabled={guildConfigLoading || !canLoadAdmin}>
                        {guildConfigLoading ? '查询中…' : '查询公会配置'}
                      </button>
                      <button className="primary-btn small-btn" onClick={handleSaveGuildConfig} disabled={guildConfigLoading || !canLoadAdmin || !guildConfigForm.productCode.trim() || !guildConfigForm.guildId.trim() || !guildConfigForm.guildInviteCode.trim()}>
                        {guildConfigLoading ? '保存中…' : '保存公会配置'}
                      </button>
                    </div>
                    {guildConfigs?.length ? (
                      <DataTable
                        headers={['产品', '上级用户', 'Linky 公会 ID', '公会名称', '公会邀请码', '启用状态']}
                        rows={guildConfigs.map((item) => [
                          item.productCode,
                          item.inviterUserId ?? '默认公会',
                          item.guildId,
                          item.guildName,
                          item.guildInviteCode,
                          item.enabled ? '启用' : '停用',
                        ])}
                        emptyText="暂无公会配置"
                      />
                    ) : (
                      <EmptyState title="暂无公会配置" description="点击查询公会配置加载现有映射；保存后会显示上级分销人对应的 Linky 公会邀请码。" />
                    )}
                  </InfoCard>
                  </div>
                    </>
                  ) : null}

                  {activeAdminSection === 'bindings' && adminBindingView === 'users' ? (
                  adminRelation ? (
                    <div className="stack-gap relation-workbench">
                      <div className="relation-grid">
                        <RelationItem label="用户ID" value={adminRelation.userId} />
                        <RelationItem label="一级上级" value={adminRelation.level1InviterId} />
                        <RelationItem label="二级上级" value={adminRelation.level2InviterId} />
                        <RelationItem label="三级上级" value={adminRelation.level3InviterId} />
                        <RelationItem label="绑定来源" value={adminRelation.bindSource} />
                        <RelationItem label="锁定状态" value={renderStatusBadge(adminRelation.lockStatus)} />
                        <RelationItem label="绑定时间" value={formatDateTime(adminRelation.bindTime)} />
                        <RelationItem label="锁定时间" value={adminRelation.lockTime ? formatDateTime(adminRelation.lockTime) : '-'} />
                        <RelationItem label="国家" value={adminRelation.countryCode} />
                        <RelationItem label="跨国家" value={adminRelation.crossCountry ? '是' : '否'} />
                      </div>

                      <div className="content-grid two-columns nested-grid admin-workspace-grid workspace-grid">
                        <InfoCard title="当前关系" tone="neutral">
                          <InfoRow label="当前一级上级" value={adminRelation.level1InviterId ?? '-'} />
                          <InfoRow label="当前二级上级" value={adminRelation.level2InviterId ?? '-'} />
                          <InfoRow label="当前三级上级" value={adminRelation.level3InviterId ?? '-'} />
                          <InfoRow label="当前来源" value={adminRelation.bindSource} />
                        </InfoCard>
                        <InfoCard title="修正预览" tone="success">
                          <InfoRow label="修正后一级上级" value={relationPreview?.nextLevel1InviterId ?? '-'} />
                          <InfoRow label="修正后来源" value={relationPreview?.nextBindSource ?? 'MANUAL'} />
                          <InfoRow label="变更说明" value={relationPreview?.summary ?? '保持当前关系'} />
                        </InfoCard>
                      </div>

                      <InfoCard title="人工修正" tone="neutral">
                        <div className="grid-form compact-form">
                          <label>
                            人工修正后的一级上级用户 ID
                            <input value={relationAdjustInviterId} onChange={(e) => setRelationAdjustInviterId(e.target.value)} placeholder="留空后保存 = 设为根关系" />
                          </label>
                          <label>
                            修正备注
                            <input value={relationAdjustNote} onChange={(e) => setRelationAdjustNote(e.target.value)} placeholder="例如：人工修正绑定关系" />
                          </label>
                        </div>
                        <InlineHint text={adminRelation.lockStatus === 'LOCKED' ? '当前关系已锁定，请先解锁后再人工修正。' : '确认预览无误后再提交人工修正。'} />
                        <div className="table-toolbar">
                          <button className="primary-btn small-btn" onClick={openRelationAdjustConfirm} disabled={relationAdjustLoading || !canLoadAdmin || adminRelation.lockStatus === 'LOCKED'}>预览后确认修正</button>
                          <button className="ghost-btn small-btn" onClick={() => setRelationAdjustInviterId('')} disabled={relationAdjustLoading}>设为根关系</button>
                        </div>
                      </InfoCard>
                    </div>
                  ) : (
                    <EmptyState title="暂无关系链结果" description="输入用户 ID 后查询，这里会显示当前关系和修正预览。" />
                  )
                  ) : null}
                </PanelSection>
              </div>
          ) : null}
        </main>

      </div>

      {selectedLinkyDrawer ? (

        <DrawerDialog
          title={selectedLinkyTitle}
          subtitle={selectedLinkyDrawer.kind === 'webhook' ? 'Linky webhook 详情' : 'Linky replay 详情'}
          onClose={() => setSelectedLinkyDrawer(null)}
        >
          {selectedLinkySections.map((section) => (
            <DetailSection key={section.title} title={section.title} rows={section.rows} />
          ))}
          {selectedLinkyRelated ? (
            <RelatedLinkySection
              relatedWebhooks={selectedLinkyRelated.relatedWebhooks}
              relatedReplays={selectedLinkyRelated.relatedReplays}
              fingerprintHint={selectedLinkyRelated.fingerprintHint}
            />
          ) : null}
        </DrawerDialog>
      ) : null}

      {pendingAdminAccountAction ? (
        <ConfirmDialog
          title={`确认${adminAccountActionLabel(pendingAdminAccountAction)}?`}
          tone={pendingAdminAccountAction.action === 'save' ? 'primary' : pendingAdminAccountAction.action === 'unlock' || (!pendingAdminAccountAction.account.enabled && pendingAdminAccountAction.action === 'toggle') ? 'success' : 'warning'}
          confirmText={`确认${adminAccountActionLabel(pendingAdminAccountAction)}`}
          onCancel={() => setPendingAdminAccountAction(null)}
          onConfirm={() => {
            const { account, action } = pendingAdminAccountAction
            if (action === 'save') void handleSaveAdminAccount(account)
            else if (action === 'toggle') void handleToggleAdminAccount(account)
            else if (action === 'reset') void handleResetAdminPassword(account.id)
            else void handleUnlockAdminAccount(account.id)
          }}
          loading={loading}
        >
          <InfoRow label="员工账号" value={`${pendingAdminAccountAction.account.username} / ${pendingAdminAccountAction.account.displayName}`} />
          <InfoRow label="角色" value={formatAdminRole(pendingAdminAccountAction.account.role)} />
          <InfoRow label="数据范围" value={`${pendingAdminAccountAction.account.platformScope} / ${pendingAdminAccountAction.account.guildScope} / ${pendingAdminAccountAction.account.regionScope}`} />
          {pendingAdminAccountAction.action === 'reset' ? <InlineHint text="重置后旧会话立即失效，临时密码只显示一次。" /> : null}
          {pendingAdminAccountAction.action === 'toggle' && pendingAdminAccountAction.account.enabled ? <InlineHint text="停用后该员工不能继续登录，现有会话也会失效。" /> : null}
        </ConfirmDialog>
      ) : null}

      {pendingWithdrawAction ? (
        <ConfirmDialog
          title={`确认${withdrawActionLabel(pendingWithdrawAction.action)}?`}
          tone={pendingWithdrawAction.action === 'reject' || pendingWithdrawAction.action === 'failed' || pendingWithdrawAction.action === 'reverse' ? 'warning' : 'primary'}
          confirmText={`确认${withdrawActionLabel(pendingWithdrawAction.action)}`}
          onCancel={() => setPendingWithdrawAction(null)}
          onConfirm={() => void handleAdminWithdrawAction(pendingWithdrawAction.requestNo, pendingWithdrawAction.action)}
          loading={adminWithdrawActionLoadingNo === pendingWithdrawAction.requestNo}
        >
          <InfoRow label="申请单" value={pendingWithdrawAction.requestNo} />
          <InfoRow label="用户" value={`#${pendingWithdrawAction.userId}`} />
          <InfoRow label="申请钻石" value={pendingWithdrawAction.requestedDiamondAmount} />
          <InfoRow label="当前状态" value={renderStatusBadge(pendingWithdrawAction.requestStatus)} />
          {pendingWithdrawAction.action === 'reject' ? <InfoRow label="拒绝原因" value={adminWithdrawAction.remark} /> : null}
          {pendingWithdrawAction.action === 'paid' ? <><InfoRow label="打款渠道" value={adminWithdrawAction.paymentChannel} /><InfoRow label="支付流水号" value={adminWithdrawAction.paymentReference} /></> : null}
          {pendingWithdrawAction.action === 'failed' ? <InfoRow label="失败原因" value={adminWithdrawAction.failureReason} /> : null}
          {pendingWithdrawAction.action === 'reverse' ? <><InfoRow label="冲正原因" value={adminWithdrawAction.reversalReason} /><InfoRow label="账本币种" value={adminWithdrawAction.reversalCurrency} /><InlineHint text="确认后将新增不可变冲正账目；原支付与审批历史不会被删除。" /></> : null}
        </ConfirmDialog>
      ) : null}

      {pendingBatchAction ? (
        <ConfirmDialog
          title={`确认批量${pendingBatchAction.kind === 'withdraw' ? pendingBatchAction.action === 'APPROVE' ? '通过提现' : '拒绝提现' : pendingBatchAction.action === 'HANDLE' ? '处理风险' : '忽略风险'}?`}
          tone={pendingBatchAction.action === 'REJECT' || pendingBatchAction.action === 'IGNORE' ? 'warning' : 'primary'}
          confirmText={`确认处理 ${pendingBatchAction.targetIds.length} 条`}
          onCancel={() => { setPendingBatchAction(null); setBatchActionNote('') }}
          onConfirm={() => void handleBatchAction()}
          loading={batchActionLoading}
        >
          <InfoRow label="选中数量" value={pendingBatchAction.targetIds.length} />
          <InfoRow label="处理范围" value={pendingBatchAction.targetIds.slice(0, 5).join('、') + (pendingBatchAction.targetIds.length > 5 ? ` 等 ${pendingBatchAction.targetIds.length} 条` : '')} />
          <label className="dialog-field">统一备注<input value={batchActionNote} onChange={(event) => setBatchActionNote(event.target.value)} placeholder={pendingBatchAction.action === 'REJECT' || pendingBatchAction.action === 'IGNORE' ? '此操作必须填写原因…' : '可填写本批次处理依据…'} /></label>
          <InlineHint text="系统会逐项执行并返回成功/失败明细；单条失败不会掩盖其他条目的真实结果。" />
        </ConfirmDialog>
      ) : null}

      {pendingRiskAction ? (
        <ConfirmDialog
          title={`确认${riskActionLabel(pendingRiskAction.action)}?`}
          tone={pendingRiskAction.action === 'FREEZE_USER' ? 'warning' : pendingRiskAction.action === 'UNFREEZE_USER' ? 'success' : 'neutral'}
          confirmText={`确认${riskActionLabel(pendingRiskAction.action)}`}
          onCancel={() => setPendingRiskAction(null)}
          onConfirm={() => handleRiskAction(pendingRiskAction)}
          loading={riskActionLoadingId === pendingRiskAction.riskEventId}
        >
          <InfoRow label="风险事件" value={`#${pendingRiskAction.riskEventId}`} />
          <InfoRow label="目标用户" value={`#${pendingRiskAction.userId}`} />
          <InfoRow label="当前状态" value={pendingRiskAction.riskStatus} />
          <InfoRow label="本次备注" value={pendingRiskAction.note || '未填写，将按系统默认备注处理'} />
        </ConfirmDialog>
      ) : null}

      {pendingRelationChange ? (
        <ConfirmDialog
          title="确认提交关系人工修正?"
          tone="primary"
          confirmText="确认提交"
          onCancel={() => setPendingRelationChange(null)}
          onConfirm={handleAdjustRelation}
          loading={relationAdjustLoading}
        >
          <InfoRow label="目标用户" value={`#${pendingRelationChange.userId}`} />
          <InfoRow label="当前一级上级" value={relationBeforeAdjust?.level1InviterId ?? pendingRelationChange.previousInviterId ?? '-'} />
          <InfoRow label="修正后一级上级" value={pendingRelationChange.nextInviterId ?? '-'} />
          <InfoRow label="原二级 / 三级" value={`${relationBeforeAdjust?.level2InviterId ?? pendingRelationChange.previousLevel2InviterId ?? '-'} / ${relationBeforeAdjust?.level3InviterId ?? pendingRelationChange.previousLevel3InviterId ?? '-'}`} />
          <InfoRow label="备注" value={pendingRelationChange.note || '未填写备注'} />
        </ConfirmDialog>
      ) : null}
    </div>
  )
}

function PanelSection({ eyebrow, title, description, action, children, sectionId }: { eyebrow: string; title: string; description?: string; action?: React.ReactNode; children: React.ReactNode; sectionId?: string }) {
  return (
    <section className="panel-card" id={sectionId}>
      <div className="panel-head">
        <div>
          <p className="panel-eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
          {description ? <p className="panel-desc">{description}</p> : null}
        </div>
        {action ? <div className="panel-action">{action}</div> : null}
      </div>
      {children}
    </section>
  )
}

function AdminNavIcon({ label }: { label: string }) {
  const props = { size: 18, weight: 'duotone' as const }
  if (label === '分销概览') return <House {...props} />
  if (label === '渠道入口') return <Megaphone {...props} />
  if (label === '绑定关系') return <LinkSimple {...props} />
  if (label === '收益提现') return <Wallet {...props} />
  if (label === '账号中心') return <UsersThree {...props} />
  return <GearSix {...props} />
}

function DiagnosticBanner({ eyebrow, title, description, tone }: { eyebrow: string; title: string; description: string; tone: 'success' | 'warning' | 'danger' }) {
  return (
    <div className={`diagnostic-banner tone-${tone}`}>
      <p className="panel-eyebrow">{eyebrow}</p>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  )
}

function Metric({ label, value, hint, tone }: { label: string; value?: number; hint: string; tone: 'neutral' | 'primary' | 'success' | 'warning' | 'danger' }) {
  return (
    <div className={`metric-card tone-${tone}`}>
      <span>{label}</span>
      <strong>{value ?? '-'}</strong>
      <p>{hint}</p>
    </div>
  )
}

function RelationItem({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="relation-item">
      <span>{label}</span>
      <strong>{value ?? '-'}</strong>
    </div>
  )
}

function InfoCard({ title, tone, children }: { title: string; tone: 'success' | 'neutral'; children: React.ReactNode }) {
  return (
    <div className={`info-card ${tone}`}>
      <h3>{title}</h3>
      <div className="stack-gap small">{children}</div>
    </div>
  )
}

function InfoRow({ label, value, code = false }: { label: string; value: React.ReactNode; code?: boolean }) {
  return (
    <div className="info-row">
      <span>{label}</span>
      {code ? <code>{value}</code> : <strong>{value}</strong>}
    </div>
  )
}

function EmptyState({ title, description, actionLabel }: { title: string; description: string; actionLabel?: string }) {
  const stateLabel = title.includes('登录')
    ? '待登录'
    : title.includes('接入')
      ? '待接入'
      : title.includes('设置')
        ? '未设置'
        : '待同步'

  return (
    <div className="empty-card">
      <span className="empty-state-label">{stateLabel}</span>
      <strong>{title}</strong>
      <p>{description}</p>
      {actionLabel ? <span className="empty-action">{actionLabel}</span> : null}
    </div>
  )
}

function RoadmapList({ items }: { items: Array<{ title: string; desc: string }> }) {
  return (
    <div className="roadmap-list">
      {items.map((item) => (
        <div className="roadmap-item" key={item.title}>
          <strong>{item.title}</strong>
          <p>{item.desc}</p>
        </div>
      ))}
    </div>
  )
}
void DiagnosticBanner
void RoadmapList

function ToastStack({ items, tone = 'neutral' }: { items: string[]; tone?: 'neutral' | 'success' | 'warning' }) {
  return (
    <div className={`toast-stack tone-${tone}`} role="status" aria-live="polite">
      {items.map((item) => (
        <div className="toast-note" key={item}>{item}</div>
      ))}
    </div>
  )
}

function InlineHint({ text }: { text: string }) {
  return <ToastStack items={[text]} />
}

function ConfirmDialog({
  title,
  tone,
  confirmText,
  loading,
  children,
  onCancel,
  onConfirm,
}: {
  title: string
  tone: 'primary' | 'warning' | 'success' | 'neutral'
  confirmText: string
  loading?: boolean
  children: React.ReactNode
  onCancel: () => void
  onConfirm: () => void
}) {
  const dialogRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    dialogRef.current?.querySelector<HTMLButtonElement>('button')?.focus()
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !loading) onCancel()
      if (event.key !== 'Tab' || !dialogRef.current) return
      const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), select:not(:disabled), [tabindex]:not([tabindex="-1"])'))
      if (!focusable.length) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
      previousFocus?.focus()
    }
  }, [loading, onCancel])

  return (
    <div className="dialog-backdrop" role="presentation">
      <div ref={dialogRef} className={`dialog-card tone-${tone}`} role="dialog" aria-modal="true" aria-label={title}>
        <div className="dialog-head">
          <div>
            <p className="panel-eyebrow">确认操作</p>
            <h3>{title}</h3>
          </div>
          <button className="ghost-btn small-btn" onClick={onCancel} disabled={loading}>关闭</button>
        </div>
        <div className="stack-gap small">{children}</div>
        <div className="dialog-actions">
          <button className="ghost-btn" onClick={onCancel} disabled={loading}>取消</button>
          <button className="primary-btn" onClick={onConfirm} disabled={loading}>{loading ? '处理中...' : confirmText}</button>
        </div>
      </div>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const badgeMap: Record<string, { label: string; tone: 'success' | 'warning' | 'danger' | 'primary' | 'neutral' }> = {
    AVAILABLE: { label: '已可用', tone: 'success' },
    PENDING_REVIEW: { label: '待审核', tone: 'primary' },
    PAYMENT_PENDING: { label: '待打款', tone: 'warning' },
    PAYMENT_FAILED: { label: '打款失败', tone: 'danger' },
    PAID_OUT: { label: '已打款', tone: 'success' },
    REVERSED: { label: '已冲正', tone: 'neutral' },
    HANDLED: { label: '已处理', tone: 'success' },
    PROCESSED: { label: '已处理', tone: 'success' },
    IGNORED: { label: '已忽略', tone: 'neutral' },
    PENDING: { label: '待处理', tone: 'primary' },
    LOCKED: { label: '已锁定', tone: 'warning' },
    RISK_HOLD: { label: '风控冻结', tone: 'warning' },
    FROZEN: { label: '已冻结', tone: 'warning' },
    FAILED: { label: '异常', tone: 'danger' },
    REJECTED: { label: '已拒绝', tone: 'danger' },
    UNLOCKED: { label: '未锁定', tone: 'success' },
  }
  const normalized = badgeMap[status] || { label: status, tone: 'primary' as const }
  return <span className={`badge badge-${normalized.tone}`}>{normalized.label}</span>
}

function renderEligibilityStatusBadge(status: string) {
  const badgeMap: Record<string, { label: string; tone: 'primary' | 'neutral' | 'success' }> = {
    MATCHED_OURS: { label: '我方公会', tone: 'success' },
    JOINED_OTHER_GUILD: { label: '已加入别家公会', tone: 'primary' },
    NOT_JOINED: { label: '未在我方公会命中', tone: 'neutral' },
    ELIGIBLE: { label: '允许注册', tone: 'success' },
    NOT_ELIGIBLE: { label: '不可注册', tone: 'primary' },
  }
  const normalized = badgeMap[status] || { label: status, tone: 'primary' as const }
  return <span className={`badge badge-${normalized.tone}`}>{normalized.label}</span>
}

function formatEligibilitySummary(result: LinkyEligibilityCheckResponse) {
  const guildSummary = result.guildName ? `，公会：${result.guildName}` : ''
  switch (result.guildCheckStatus) {
    case 'MATCHED_OURS':
      return `命中我方公会，可注册${guildSummary}`
    case 'JOINED_OTHER_GUILD':
      return `已加入别家公会，不可注册${guildSummary}`
    case 'NOT_JOINED':
      return `当前公会后台未命中该账号，外部归属仍待确认，先不允许注册`
    default:
      return `${result.guildCheckStatus}${guildSummary}`
  }
}

function renderStatusBadge(status: string) {
  return <StatusBadge status={status} />
}

function riskActionLabel(action: RiskActionName) {
  switch (action) {
    case 'HANDLE':
      return '处理'
    case 'IGNORE':
      return '忽略'
    case 'FREEZE_USER':
      return '冻结用户'
    case 'UNFREEZE_USER':
      return '解冻用户'
  }
}

function withdrawActionLabel(action: WithdrawActionName) {
  if (action === 'approve') return '通过审核'
  if (action === 'reject') return '拒绝申请'
  if (action === 'paid') return '记录已打款'
  if (action === 'reverse') return '发起账本冲正'
  return '记录打款失败'
}

function adminAccountActionLabel(input: PendingAdminAccountAction) {
  if (input.action === 'save') return '保存权限变更'
  if (input.action === 'reset') return '重置员工密码'
  if (input.action === 'unlock') return '解锁员工账号'
  return input.account.enabled ? '停用员工账号' : '恢复员工账号'
}

function buildRelationPreview(relation: RelationDetailResponse, nextInviterIdRaw: string) {
  const nextLevel1InviterId = nextInviterIdRaw.trim() ? Number(nextInviterIdRaw) : null
  const changed = nextLevel1InviterId !== relation.level1InviterId
  return {
    nextLevel1InviterId,
    nextBindSource: changed ? 'MANUAL' : relation.bindSource,
    summary: changed
      ? nextLevel1InviterId === null
        ? '将把当前用户改成根关系，并清空上级链路。'
        : `将把一级上级从 #${relation.level1InviterId ?? '-'} 调整为 #${nextLevel1InviterId}。`
      : '一级上级未变化，可继续补备注后提交。',
  }
}

function canHandleRisk(status: string) {
  return status === 'PENDING'
}

function canIgnoreRisk(status: string) {
  return status === 'PENDING'
}

function canFreezeRisk(status: string) {
  return status === 'PENDING'
}

function canUnfreezeRisk(status: string) {
  return status === 'HANDLED'
}

function DataTable({ headers, rows, emptyText, rowClassNames }: { headers: React.ReactNode[]; rows?: Array<Array<React.ReactNode>>; emptyText: string; rowClassNames?: string[] }) {
  return (
    <div className="table-shell">
      <table>
        <thead>
          <tr>
            {headers.map((header, index) => <th key={index}>{header}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows?.length ? rows.map((row, index) => (
            <tr key={`${row[0]}-${index}`} className={rowClassNames?.[index] || undefined}>
              {row.map((cell, cellIndex) => <td key={`${index}-${cellIndex}`}>{cell}</td>)}
            </tr>
          )) : (
            <tr><td colSpan={headers.length}>{emptyText}</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

function BatchResultSummary({ result }: { result: BatchOperationResultResponse }) {
  const failures = result.items.filter((item) => !item.success)
  return (
    <div className={`admin-batch-result ${failures.length ? 'has-failures' : ''}`} role="status">
      <strong>批量回执：成功 {result.successCount} 条，失败 {result.failureCount} 条</strong>
      {failures.length ? <details><summary>查看失败明细</summary><ul>{failures.map((item) => <li key={item.targetId}><code>{item.targetId}</code><span>{item.message || '操作失败'}</span></li>)}</ul></details> : <span>全部条目已完成。</span>}
    </div>
  )
}

function FingerprintCell({ fingerprint, onCopy }: { fingerprint: string; onCopy: (fingerprint: string) => void | Promise<void> }) {
  return (
    <div className="fingerprint-cell">
      <code className="truncated-code" title={fingerprint}>{fingerprint}</code>
      <button className="ghost-btn small-btn fingerprint-copy-btn" onClick={() => void onCopy(fingerprint)} type="button">复制</button>
    </div>
  )
}
void FingerprintCell

function DrawerDialog({ title, subtitle, children, onClose }: { title: string; subtitle: string; children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <aside className="drawer-panel" role="dialog" aria-modal="true" aria-label={title} onClick={(event) => event.stopPropagation()}>
        <div className="drawer-head">
          <div>
            <p className="panel-eyebrow">{subtitle}</p>
            <h3>{title}</h3>
          </div>
          <button className="ghost-btn small-btn" type="button" onClick={onClose}>关闭</button>
        </div>
        <div className="drawer-body">{children}</div>
      </aside>
    </div>
  )
}

function DetailSection({ title, rows }: { title: string; rows: Array<[string, string]> }) {
  return (
    <section className="detail-section">
      <h4>{title}</h4>
      <div className="detail-grid">
        {rows.map(([label, value]) => (
          <div className="detail-item" key={`${title}-${label}`}>
            <span>{label}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
    </section>
  )
}

function RelatedLinkySection({ relatedWebhooks, relatedReplays, fingerprintHint }: { relatedWebhooks: string[]; relatedReplays: string[]; fingerprintHint: string }) {
  return (
    <section className="detail-section">
      <h4>关联请求视图</h4>
      <div className="stack-gap small">
        <div>
          <p className="detail-subtitle">同订单 webhook</p>
          {relatedWebhooks.length ? (
            <ul className="detail-list">
              {relatedWebhooks.map((item) => <li key={item}>{item}</li>)}
            </ul>
          ) : (
            <p className="inline-hint">当前列表范围内没有更多同订单 webhook。</p>
          )}
        </div>
        <div>
          <p className="detail-subtitle">关联 replay 记录</p>
          {relatedReplays.length ? (
            <ul className="detail-list">
              {relatedReplays.map((item) => <li key={item}>{item}</li>)}
            </ul>
          ) : (
            <p className="inline-hint">当前列表范围内没有更多 replay 记录。</p>
          )}
          <p className="inline-hint">{fingerprintHint}</p>
        </div>
      </div>
    </section>
  )
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`
}

function formatAdminRole(role: string) {
  const labels: Record<string, string> = {
    super_admin: '最高管理员', admin: '管理员', operator: '操作员', operations: '运营',
    finance: '财务', customer_support: '客服', mentor: '导师', team_leader: '团队负责人', viewer: '只读',
  }
  return labels[role.toLowerCase()] ?? role
}

// eslint-disable-next-line react-refresh/only-export-components
export function buildBindGuildInviteGuidance(message: string) {
  const inviteCodeMatch = message.match(/invite code\s+([A-Za-z0-9_-]+)/i)
  if (!inviteCodeMatch) return null
  const inviteCode = inviteCodeMatch[1].replace(/[.。]$/, '')
  return {
    title: '请先加入指定 Linky 公会',
    inviteCode,
    description: `这个 Linky ID 还没有命中上级对应公会。请先用公会邀请码 ${inviteCode} 加入指定公会，再回来提交绑定。`,
  }
}

function BindLandingPage() {
  const initialInviteCode = typeof window !== 'undefined'
    ? new URLSearchParams(window.location.search).get('inviteCode') || ''
    : ''

  const copyByLocale = {
    zh: {
      languageLabel: '语言',
      productLabel: '产品',
      productHelper: '暂时仅开放 Linky',
      kicker: 'FLEXIBLE REMOTE REWARD PROGRAM',
      heroTitle: '先绑定邀请码，锁定后续奖励归属',
      heroSubtitle: '填写邀请码、WhatsApp 和 8 位账号，先把关系登记进去。',
      chips: ['居家灵活用工', '金币奖励链路', '手机即可开始'],
      floating: ['🏠 居家', '📱 手机', '🪙 金币'],
      stats: ['先绑定', '再推广', '后归因'],
      formTitle: '现在提交，锁定你的奖励线',
      formSubtitle: '只做一个动作：先把关系登记进去。',
      inviteCode: '邀请码',
      inviteCodePlaceholder: '例如 ABCD1234',
      whatsappNumber: 'WhatsApp 号码',
      whatsappPlaceholder: '例如 +6281234567890',
      linkyAccount: 'App 账户（8位数字）',
      linkyPlaceholder: '例如 12345678',
      submit: '立即开始锁定奖励关系',
      submitting: '提交中...',
      failure: '登记失败',
      success: '登记成功',
      successText: '关系已写入系统，后续归因按当前邀请码记录。',
      factsTitle: '基本原理',
      fact1: '一个 Linky 账号只能归属一个邀请码。',
      fact2: 'WhatsApp 号码唯一，重复登记会被拒绝。',
      fact3: '填错后不能自己改绑，只能后台修正。',
      stepsTitle: '怎么做',
      step1: '拿到邀请码',
      step2: '填 WhatsApp',
      step3: '填 8 位账号并提交',
      foot1: '邀请码固定',
      foot2: '立即生效',
      foot3: '后续按此归因',
      resultTitle: '当前结果',
      resultWritten: '这次绑定已经写入系统',
      inviterUserId: '邀请人用户 ID',
      status: '状态',
      navBind: '绑定页',
      navInvite: '邀请好友',
      navEarnings: '我的收益',
    },
    en: {
      languageLabel: 'Language',
      productLabel: 'Product',
      productHelper: 'Linky only for now',
      kicker: 'FLEXIBLE REMOTE REWARD PROGRAM',
      heroTitle: 'Use your phone from home. Bind the invite code first and lock your reward line.',
      heroSubtitle: 'Simple rule: register invite code + WhatsApp + 8-digit account first, then future attribution and rewards follow this line.',
      chips: ['Remote flexible work', 'Coin reward flow', 'Phone-first start'],
      floating: ['🏠 Home', '📱 Phone', '🪙 Coins'],
      stats: ['Bind first', 'Promote next', 'Reward later'],
      formTitle: 'Submit now and lock your reward line',
      formSubtitle: 'One action only: register the relationship first.',
      inviteCode: 'Invite code',
      inviteCodePlaceholder: 'e.g. ABCD1234',
      whatsappNumber: 'WhatsApp number',
      whatsappPlaceholder: 'e.g. +6281234567890',
      linkyAccount: 'App account (8 digits)',
      linkyPlaceholder: 'e.g. 12345678',
      submit: 'Lock my reward relationship now',
      submitting: 'Submitting...',
      failure: 'Failed',
      success: 'Success',
      successText: 'The relationship is saved. Future attribution follows this invite code.',
      factsTitle: 'How it works',
      fact1: 'One Linky account can belong to one invite code only.',
      fact2: 'WhatsApp number must be unique.',
      fact3: 'Wrong submissions can only be fixed by support.',
      stepsTitle: 'Steps',
      step1: 'Get the invite code',
      step2: 'Enter WhatsApp',
      step3: 'Enter 8-digit account and submit',
      foot1: 'Fixed invite code',
      foot2: 'Live immediately',
      foot3: 'Future rewards follow this record',
      resultTitle: 'Current result',
      resultWritten: 'This binding has been saved',
      inviterUserId: 'Inviter user ID',
      status: 'Status',
      navBind: 'Binding page',
      navInvite: 'Generate my invite code',
      navEarnings: 'View my team earnings',
    },
    es: {
      languageLabel: 'Idioma',
      productLabel: 'Producto',
      productHelper: 'Solo Linky por ahora',
      kicker: 'FLEXIBLE REMOTE REWARD PROGRAM',
      heroTitle: 'Trabaja desde casa con tu móvil. Vincula primero el código y bloquea tu línea de recompensa.',
      heroSubtitle: 'Regla simple: primero registra código + WhatsApp + cuenta de 8 dígitos, luego la atribución y las recompensas seguirán esta línea.',
      chips: ['Trabajo remoto flexible', 'Flujo de monedas', 'Empieza con tu móvil'],
      floating: ['🏠 Casa', '📱 Móvil', '🪙 Monedas'],
      stats: ['Vincula primero', 'Promociona después', 'Recompensa luego'],
      formTitle: 'Envía ahora y bloquea tu línea de recompensa',
      formSubtitle: 'Solo una acción: registra primero la relación.',
      inviteCode: 'Código de invitación',
      inviteCodePlaceholder: 'ej. ABCD1234',
      whatsappNumber: 'Número de WhatsApp',
      whatsappPlaceholder: 'ej. +6281234567890',
      linkyAccount: 'Cuenta de la app (8 dígitos)',
      linkyPlaceholder: 'ej. 12345678',
      submit: 'Bloquear mi recompensa ahora',
      submitting: 'Enviando...',
      failure: 'Error',
      success: 'Éxito',
      successText: 'La relación fue guardada. La atribución futura seguirá este código.',
      factsTitle: 'Cómo funciona',
      fact1: 'Una cuenta Linky solo puede pertenecer a un código.',
      fact2: 'El número de WhatsApp debe ser único.',
      fact3: 'Los errores solo pueden corregirse manualmente.',
      stepsTitle: 'Pasos',
      step1: 'Consigue el código',
      step2: 'Ingresa WhatsApp',
      step3: 'Ingresa la cuenta de 8 dígitos y envía',
      foot1: 'Código fijo',
      foot2: 'Activo al instante',
      foot3: 'Recompensas futuras siguen este registro',
      resultTitle: 'Resultado actual',
      resultWritten: 'Este vínculo ya fue guardado',
      inviterUserId: 'ID del invitador',
      status: 'Estado',
      navBind: 'Página de vínculo',
      navInvite: 'Generar mi código',
      navEarnings: 'Ver ganancias de mi equipo',
    },
    id: {
      languageLabel: 'Bahasa',
      productLabel: 'Produk',
      productHelper: 'Untuk sementara hanya Linky',
      kicker: 'FLEXIBLE REMOTE REWARD PROGRAM',
      heroTitle: 'Kerja fleksibel dari rumah pakai HP. Ikat kode undangan dulu, lalu kunci jalur reward kamu.',
      heroSubtitle: 'Aturannya sederhana: daftarkan kode undangan + WhatsApp + akun 8 digit dulu, lalu atribusi dan reward berikutnya akan mengikuti jalur ini.',
      chips: ['Kerja fleksibel dari rumah', 'Alur reward koin', 'Mulai lewat HP'],
      floating: ['🏠 Rumah', '📱 HP', '🪙 Koin'],
      stats: ['Bind dulu', 'Promosi berikutnya', 'Reward belakangan'],
      formTitle: 'Kirim sekarang dan kunci jalur reward kamu',
      formSubtitle: 'Cuma satu langkah: daftar relasinya dulu.',
      inviteCode: 'Kode undangan',
      inviteCodePlaceholder: 'contoh ABCD1234',
      whatsappNumber: 'Nomor WhatsApp',
      whatsappPlaceholder: 'contoh +6281234567890',
      linkyAccount: 'Akun app (8 digit)',
      linkyPlaceholder: 'contoh 12345678',
      submit: 'Kunci relasi reward saya sekarang',
      submitting: 'Mengirim...',
      failure: 'Gagal',
      success: 'Berhasil',
      successText: 'Relasi sudah disimpan. Atribusi berikutnya mengikuti kode ini.',
      factsTitle: 'Cara kerja',
      fact1: 'Satu akun Linky hanya bisa dimiliki satu kode undangan.',
      fact2: 'Nomor WhatsApp harus unik.',
      fact3: 'Jika salah isi, hanya bisa diperbaiki manual.',
      stepsTitle: 'Langkah',
      step1: 'Ambil kode undangan',
      step2: 'Isi WhatsApp',
      step3: 'Isi akun 8 digit lalu kirim',
      foot1: 'Kode tetap',
      foot2: 'Langsung aktif',
      foot3: 'Reward berikutnya ikut catatan ini',
      resultTitle: 'Hasil saat ini',
      resultWritten: 'Binding ini sudah tersimpan',
      inviterUserId: 'ID pengundang',
      status: 'Status',
      navBind: 'Halaman bind',
      navInvite: 'Buat kode undangan saya',
      navEarnings: 'Lihat penghasilan tim saya',
    },
    pt: {
      languageLabel: 'Idioma',
      productLabel: 'Produto',
      productHelper: 'Apenas Linky por enquanto',
      kicker: 'FLEXIBLE REMOTE REWARD PROGRAM',
      heroTitle: 'Trabalhe de casa com o celular. Vincule o código primeiro e bloqueie sua linha de recompensa.',
      heroSubtitle: 'Regra simples: registre primeiro código + WhatsApp + conta de 8 dígitos, depois a atribuição e as recompensas seguirão esta linha.',
      chips: ['Trabalho remoto flexível', 'Fluxo de moedas', 'Comece pelo celular'],
      floating: ['🏠 Casa', '📱 Celular', '🪙 Moedas'],
      stats: ['Vincule primeiro', 'Promova depois', 'Reward depois'],
      formTitle: 'Envie agora e bloqueie sua linha de recompensa',
      formSubtitle: 'Uma ação só: registre a relação primeiro.',
      inviteCode: 'Código de convite',
      inviteCodePlaceholder: 'ex. ABCD1234',
      whatsappNumber: 'Número do WhatsApp',
      whatsappPlaceholder: 'ex. +6281234567890',
      linkyAccount: 'Conta do app (8 dígitos)',
      linkyPlaceholder: 'ex. 12345678',
      submit: 'Bloquear minha recompensa agora',
      submitting: 'Enviando...',
      failure: 'Falha',
      success: 'Sucesso',
      successText: 'A relação foi salva. A atribuição futura seguirá este código.',
      factsTitle: 'Como funciona',
      fact1: 'Uma conta Linky só pode pertencer a um código.',
      fact2: 'O número de WhatsApp deve ser único.',
      fact3: 'Erros só podem ser corrigidos manualmente.',
      stepsTitle: 'Passos',
      step1: 'Pegue o código',
      step2: 'Digite o WhatsApp',
      step3: 'Digite a conta de 8 dígitos e envie',
      foot1: 'Código fixo',
      foot2: 'Ativo na hora',
      foot3: 'Próximas recompensas seguem este registro',
      resultTitle: 'Resultado atual',
      resultWritten: 'Este vínculo já foi salvo',
      inviterUserId: 'ID do convidador',
      status: 'Status',
      navBind: 'Página de vínculo',
      navInvite: 'Gerar meu código',
      navEarnings: 'Ver ganhos da minha equipe',
    },
  } as const

  const [locale, setLocale] = useState<keyof typeof copyByLocale>(() => loadExternalLocale())
  const product = 'linky'
  const [form, setForm] = useState({
    inviteCode: initialInviteCode,
    whatsappNumber: '',
    linkyAccount: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<InviteBindingResponse | null>(null)

  const copy = copyByLocale[locale]
  const guildInviteGuidance = error ? buildBindGuildInviteGuidance(error) : null
  const canSubmit = Boolean(form.inviteCode.trim() && form.whatsappNumber.trim() && form.linkyAccount.length === 8)

  useEffect(() => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(EXTERNAL_LOCALE_KEY, locale)
    }
  }, [locale])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const response = await registerInviteBinding({
        productCode: product,
        ...form,
      })
      setResult(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : copy.failure)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="consumer-app-page">
      <main className="consumer-shell consumer-form-shell">
        <header className="consumer-topbar">
          <a className="consumer-brand" href="/earnings"><img className="consumer-brand-logo" src="/bandeira-logo-v1.png" alt="" />BANDEIRA</a>
          <div className="consumer-topbar-actions">
            <select className="consumer-language" aria-label={copy.languageLabel} value={locale} onChange={(event) => setLocale(event.target.value as keyof typeof copyByLocale)}>
              <option value="zh">中文</option>
              <option value="en">EN</option>
              <option value="es">ES</option>
              <option value="id">ID</option>
              <option value="pt">PT</option>
            </select>
            <a className="consumer-account-link" href="/earnings">
              <Wallet weight="regular" aria-hidden="true" />
              <span>{copy.navEarnings}</span>
              <CaretRight weight="bold" aria-hidden="true" />
            </a>
          </div>
        </header>

        <section className="consumer-commercial-hero consumer-bind-hero">
          <span className="consumer-visually-hidden">{copy.navBind}</span>
          <div className="consumer-commercial-kicker"><Diamond weight="fill" aria-hidden="true" /> BANDEIRA REWARDS</div>
          <h1>{copy.heroTitle}</h1>
          <p>{copy.productLabel} · Linky · {copy.heroSubtitle}</p>
          <div className="consumer-commercial-proof">
            <span><ShieldCheck weight="fill" aria-hidden="true" />归属锁定</span>
            <span><LinkSimple weight="bold" aria-hidden="true" />记录可追踪</span>
          </div>
        </section>

        {error ? (
          <section className="consumer-banner is-error" role="alert">
            <strong>{guildInviteGuidance?.title ?? copy.failure}</strong>
            <span>{guildInviteGuidance?.description ?? error}</span>
            {guildInviteGuidance ? <span>公会邀请码：<strong>{guildInviteGuidance.inviteCode}</strong></span> : null}
          </section>
        ) : result ? (
          <section className="consumer-banner is-success" role="status"><strong>{copy.success}</strong><span>{copy.successText}</span></section>
        ) : null}

        <form className="consumer-form-card" onSubmit={handleSubmit}>
          <label className="consumer-field">
            <span>{copy.inviteCode}</span>
            <input value={form.inviteCode} onChange={(event) => setForm({ ...form, inviteCode: event.target.value.toUpperCase() })} placeholder={copy.inviteCodePlaceholder} autoFocus />
          </label>
          <label className="consumer-field">
            <span>{copy.whatsappNumber}</span>
            <input value={form.whatsappNumber} onChange={(event) => setForm({ ...form, whatsappNumber: event.target.value })} placeholder={copy.whatsappPlaceholder} inputMode="tel" />
          </label>
          <label className="consumer-field">
            <span>{copy.linkyAccount}</span>
            <input value={form.linkyAccount} onChange={(event) => setForm({ ...form, linkyAccount: event.target.value.replace(/\D/g, '').slice(0, 8) })} placeholder={copy.linkyPlaceholder} inputMode="numeric" />
          </label>
          <input type="hidden" value={product} readOnly />
          <button className="consumer-form-submit" type="submit" disabled={loading || !canSubmit}>
            {loading ? copy.submitting : copy.submit}<ArrowRight weight="bold" aria-hidden="true" />
          </button>
          <p className="consumer-form-note"><ShieldCheck weight="fill" aria-hidden="true" />{copy.fact1}</p>
        </form>

        {result ? (
          <section className="consumer-result-card">
            <div><CheckCircle weight="fill" aria-hidden="true" /><strong>{copy.resultWritten}</strong></div>
            <dl>
              <div><dt>{copy.inviteCode}</dt><dd>{result.inviteCode}</dd></div>
              <div><dt>{copy.linkyAccount}</dt><dd>{result.linkyAccount}</dd></div>
              <div><dt>{copy.status}</dt><dd>{result.bindStatus === 'BOUND' ? copy.success : result.bindStatus}</dd></div>
            </dl>
            <a href="/earnings">{copy.navEarnings}<ArrowRight weight="bold" aria-hidden="true" /></a>
          </section>
        ) : null}

        <nav className="consumer-bottom-nav" aria-label="用户导航">
          <a href="/earnings"><Wallet weight="regular" aria-hidden="true" /><span>{locale === 'zh' ? '收益' : copy.navEarnings}</span></a>
          <a href="/invite"><UserPlus weight="regular" aria-hidden="true" /><span>{locale === 'zh' ? '邀请' : copy.navInvite}</span></a>
          <a className="is-active" href="/bind"><LinkSimple weight="fill" aria-hidden="true" /><span>{locale === 'zh' ? '绑定' : copy.navBind}</span></a>
        </nav>
      </main>
    </div>
  )
}

function formatMoney(value?: number | null) {
  if (value === undefined || value === null) return '--'
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

// eslint-disable-next-line react-refresh/only-export-components
export function formatBusinessRewardLevel(rewardLevel?: number | null, locale: string = 'zh') {
  if (locale === 'zh') {
    const labels: Record<number, string> = {
      1: '直接邀请奖励',
      2: '历史二级佣金（只读）',
      3: '历史三级佣金（只读）',
    }
    return labels[rewardLevel ?? 0] ?? `历史层级 ${rewardLevel ?? '-'} 佣金（只读）`
  }
  const labels: Record<number, string> = {
    1: 'Direct invite reward',
    2: 'Legacy level 2 commission (read-only)',
    3: 'Legacy level 3 commission (read-only)',
  }
  return labels[rewardLevel ?? 0] ?? `Legacy level ${rewardLevel ?? '-'} commission (read-only)`
}

const externalPageCopyByLocale = {
  zh: {
    navBind: '绑定页',
    navInvite: '邀请好友',
    navEarnings: '我的收益',
    languageLabel: '语言',
    inviteKicker: 'INVITE CODE ENTRY',
    inviteTitle: '邀请好友',
    inviteSubtitle: '登录后复制邀请码或分享绑定链接。',
    productLabel: '产品',
    whatsappLabel: 'WhatsApp 号码',
    appAccountLabel: 'app 账户（8位数字）',
    generateButton: '立即生成邀请码',
    generating: '生成中...',
    myInviteCode: '我的邀请码',
    copyInviteCode: '一键复制邀请码',
    issueSuccess: '邀请码已生成。',
    issueFailure: '生成邀请码失败',
    copySuccess: '邀请码已复制。',
    copyFailure: '复制邀请码失败，请手动复制。',
    earningsKicker: 'EARNINGS ENTRY',
    earningsTitle: '我的收益',
    earningsSubtitle: '查看可用、冻结和累计奖励。',
    boardTitle: '你的收益会在这里持续更新',
    boardSubtitle: '从邀请码、绑定到奖励到账，这一页会持续帮你看清进度。',
    boardBadgeCode: '邀请码固定不变',
    boardBadgeBind: '绑定后自动累计',
    boardBadgeStatus: '到账状态一目了然',
    noSession: '还没有用户会话，请先去“生成我的邀请码”页面生成邀请码。',
    noSessionTitle: '登录后查看你的邀请码',
    noSessionHint: '使用手机号登录后即可邀请好友和查看收益。',
    noSessionPrimary: '手机号登录',
    noSessionSecondary: '去绑定关系',
    inviteeIncome: '被邀请人的收益',
    inviteeIncomeBadge: '已确认',
    myCommission: '你的提成',
    myCommissionBadge: '累计提成',
    availableReward: '可用奖励',
    availableRewardBadge: '可立即查看',
    frozenReward: '冻结奖励',
    riskHoldReward: '风险冻结',
    inviteeIncomeHint: '来自你的下线成员累计确认收益。',
    myCommissionHint: '按奖励记录汇总出来的你的分销提成。',
    availableRewardHint: '当前已经进入可结算状态的奖励。',
    earningsOverview: '我的收益概览',
    overviewCardTitle: '当前邀请码与收益总览',
    overviewBadge: '邀请码 / 团队 / 奖励',
    progressTitle: '当前邀请进度',
    progressCardTitle: '绑定完成后人数和收益会持续更新',
    progressBadge: '进度追踪',
    progressHint: '邀请码固定不变；完成绑定后，邀请人数、有效人数和收益会逐步更新。',
    settlementTitle: '奖励到账说明',
    settlementCardTitle: '冻结中 → 可结算 → 风险冻结',
    settlementBadge: '到账路径',
    settlementHint: '奖励会先进入冻结，满足结算条件后转为可用；如触发风控，会暂时进入风险冻结。',
    nextStepsTitle: '接下来你可以继续做',
    nextInvite: '继续去生成邀请码',
    nextBind: '继续去绑定关系',
    inviteCode: '邀请码',
    invitedUsers: '邀请人数',
    effectiveUsers: '有效人数',
    totalReward: '累计奖励',
    rewardRecords: '收益记录',
    rewardActivityTitle: '最近奖励动态',
    rewardActivityHint: '每一笔奖励都会显示状态和时间，方便你确认什么时候到账。',
    rewardStatusGuideTitle: '状态说明',
    rewardStatusGuideFrozen: '冻结中：奖励正在等待结算',
    rewardStatusGuideAvailable: '可结算：奖励已经可以使用',
    rewardStatusGuideRiskHold: '风险冻结：奖励暂时进入风控复核',
    loading: '加载中...',
    noRewards: '暂时还没有收益记录。',
    emptyRewardsTitle: '还没有收益记录',
    emptyRewardsHint: '先去生成邀请码并完成绑定，后续有收益会自动显示在这里。',
    emptyRewardsAction: '去生成我的邀请码',
    rewardLine: '被邀请人收益层级',
    commissionTail: '你的提成',
    rewardStatusFrozen: '冻结中',
    rewardStatusAvailable: '可结算',
    rewardStatusRiskHold: '风险冻结',
    rewardStatusDefault: '处理中',
  },
  en: {
    navBind: 'Binding page',
    navInvite: 'Generate my invite code',
    navEarnings: 'View my team earnings',
    languageLabel: 'Language',
    inviteKicker: 'INVITE CODE ENTRY',
    inviteTitle: 'Generate my invite code',
    inviteSubtitle: 'Enter product, WhatsApp, and app account to generate an invite code in one click.',
    productLabel: 'Product',
    whatsappLabel: 'WhatsApp number',
    appAccountLabel: 'App account (8 digits)',
    generateButton: 'Generate invite code',
    generating: 'Generating...',
    myInviteCode: 'My invite code',
    copyInviteCode: 'Copy invite code',
    issueSuccess: 'Invite code generated.',
    issueFailure: 'Failed to generate invite code',
    copySuccess: 'Invite code copied.',
    copyFailure: 'Failed to copy invite code.',
    earningsKicker: 'EARNINGS ENTRY',
    earningsTitle: 'View my team earnings',
    earningsSubtitle: 'See two parts: invitee earnings + your commission.',
    boardTitle: 'Your earnings will keep updating here',
    boardSubtitle: 'From invite code to binding to reward settlement, this page helps you track the whole progress clearly.',
    boardBadgeCode: 'Invite code stays the same',
    boardBadgeBind: 'Accumulates after binding',
    boardBadgeStatus: 'Settlement status at a glance',
    noSession: 'No user session yet. Generate your invite code first.',
    noSessionTitle: 'Generate your invite code first',
    noSessionHint: 'No invites yet, and that is okay. Generate your invite code first, then complete the binding step. Earnings will start to accumulate here automatically.',
    noSessionPrimary: 'Generate my invite code',
    noSessionSecondary: 'Go to binding page',
    inviteeIncome: 'Invitee earnings',
    inviteeIncomeBadge: 'Confirmed',
    myCommission: 'Your commission',
    myCommissionBadge: 'Total commission',
    availableReward: 'Available reward',
    availableRewardBadge: 'Ready to view',
    frozenReward: 'Frozen reward',
    riskHoldReward: 'Risk hold',
    inviteeIncomeHint: 'Confirmed earnings from your downstream members.',
    myCommissionHint: 'Your commission aggregated from reward records.',
    availableRewardHint: 'Rewards already available for settlement.',
    earningsOverview: 'My earnings overview',
    overviewCardTitle: 'Current invite code and reward snapshot',
    overviewBadge: 'Invite code / Team / Rewards',
    progressTitle: 'Current invite progress',
    progressCardTitle: 'After binding, users and rewards will keep updating here',
    progressBadge: 'Progress tracking',
    progressHint: 'Your invite code stays the same. After binding is completed, invited users, effective users, and rewards will update here step by step.',
    settlementTitle: 'How reward settlement works',
    settlementCardTitle: 'Frozen → Available → Risk hold',
    settlementBadge: 'Settlement path',
    settlementHint: 'Rewards usually enter frozen status first, become available after settlement conditions are met, and may move into risk hold if a risk review is triggered.',
    nextStepsTitle: 'What you can do next',
    nextInvite: 'Generate another invite code',
    nextBind: 'Go to binding page',
    inviteCode: 'Invite code',
    invitedUsers: 'Invited users',
    effectiveUsers: 'Effective users',
    totalReward: 'Total reward',
    rewardRecords: 'Reward records',
    rewardActivityTitle: 'Recent reward activity',
    rewardActivityHint: 'Each reward shows its status and time so you can see when it becomes available.',
    rewardStatusGuideTitle: 'Status guide',
    rewardStatusGuideFrozen: 'Frozen: reward is waiting for settlement',
    rewardStatusGuideAvailable: 'Available: reward is ready to use',
    rewardStatusGuideRiskHold: 'Risk hold: reward is under risk review for now',
    loading: 'Loading...',
    noRewards: 'No reward records yet.',
    emptyRewardsTitle: 'No reward records yet',
    emptyRewardsHint: 'Generate an invite code and complete the binding flow first. Once earnings are created, they will show up here automatically.',
    emptyRewardsAction: 'Generate my invite code',
    rewardLine: 'Invitee reward level',
    commissionTail: 'your commission',
    rewardStatusFrozen: 'Frozen',
    rewardStatusAvailable: 'Available',
    rewardStatusRiskHold: 'Risk hold',
    rewardStatusDefault: 'Processing',
  },
  es: {
    navBind: 'Página de vínculo',
    navInvite: 'Generar mi código',
    navEarnings: 'Ver ganancias de mi equipo',
    languageLabel: 'Idioma',
    inviteKicker: 'INVITE CODE ENTRY',
    inviteTitle: 'Generar mi código',
    inviteSubtitle: 'Completa producto, WhatsApp y cuenta app para generar un código con un clic.',
    productLabel: 'Producto',
    whatsappLabel: 'Número de WhatsApp',
    appAccountLabel: 'Cuenta app (8 dígitos)',
    generateButton: 'Generar código',
    generating: 'Generando...',
    myInviteCode: 'Mi código',
    copyInviteCode: 'Copiar código',
    issueSuccess: 'Código generado.',
    issueFailure: 'Error al generar el código',
    copySuccess: 'Código copiado.',
    copyFailure: 'Error al copiar el código.',
    earningsKicker: 'EARNINGS ENTRY',
    earningsTitle: 'Ver ganancias de mi equipo',
    earningsSubtitle: 'Mira dos partes: ganancias del invitado + tu comisión.',
    boardTitle: 'Tus ganancias se actualizarán aquí continuamente',
    boardSubtitle: 'Desde el código de invitación hasta el vínculo y la liquidación, esta página te ayuda a seguir todo el progreso con claridad.',
    boardBadgeCode: 'El código no cambia',
    boardBadgeBind: 'Se acumula después del vínculo',
    boardBadgeStatus: 'Estado visible de un vistazo',
    noSession: 'Todavía no hay sesión. Genera tu código primero.',
    noSessionTitle: 'Primero genera tu código',
    noSessionHint: 'Si todavía no empezaste a invitar, no pasa nada. Genera tu código primero y luego completa el vínculo. Las ganancias se acumularán aquí automáticamente.',
    noSessionPrimary: 'Generar mi código',
    noSessionSecondary: 'Ir a la página de vínculo',
    inviteeIncome: 'Ganancias del invitado',
    inviteeIncomeBadge: 'Confirmadas',
    myCommission: 'Tu comisión',
    myCommissionBadge: 'Comisión acumulada',
    availableReward: 'Recompensa disponible',
    availableRewardBadge: 'Lista para ver',
    frozenReward: 'Recompensa congelada',
    riskHoldReward: 'Retención por riesgo',
    inviteeIncomeHint: 'Ganancias confirmadas de tus miembros referidos.',
    myCommissionHint: 'Tu comisión agregada desde los registros.',
    availableRewardHint: 'Recompensas ya disponibles para liquidación.',
    earningsOverview: 'Resumen de mis ganancias',
    overviewCardTitle: 'Código actual y resumen de ganancias',
    overviewBadge: 'Código / Equipo / Recompensas',
    progressTitle: 'Progreso actual de invitación',
    progressCardTitle: 'Después del vínculo, usuarios y recompensas seguirán actualizándose aquí',
    progressBadge: 'Seguimiento',
    progressHint: 'Tu código no cambia. Después del vínculo, invitados, usuarios efectivos y recompensas se actualizarán aquí paso a paso.',
    settlementTitle: 'Cómo se acredita la recompensa',
    settlementCardTitle: 'Congelada → Disponible → Retención por riesgo',
    settlementBadge: 'Ruta de acreditación',
    settlementHint: 'La recompensa primero pasa por congelación, luego se vuelve disponible al cumplir las condiciones y puede entrar en retención por riesgo si se activa una revisión.',
    nextStepsTitle: 'Qué puedes hacer ahora',
    nextInvite: 'Seguir para generar mi código',
    nextBind: 'Seguir para vincular relación',
    inviteCode: 'Código',
    invitedUsers: 'Invitados',
    effectiveUsers: 'Usuarios efectivos',
    totalReward: 'Recompensa total',
    rewardRecords: 'Registros de recompensa',
    rewardActivityTitle: 'Actividad reciente de recompensas',
    rewardActivityHint: 'Cada recompensa muestra su estado y hora para que puedas confirmar cuándo se acredita.',
    rewardStatusGuideTitle: 'Guía de estados',
    rewardStatusGuideFrozen: 'Congelada: la recompensa está esperando liquidación',
    rewardStatusGuideAvailable: 'Disponible: la recompensa ya se puede usar',
    rewardStatusGuideRiskHold: 'Retención por riesgo: la recompensa está en revisión temporal',
    loading: 'Cargando...',
    noRewards: 'Todavía no hay registros.',
    emptyRewardsTitle: 'Todavía no hay registros',
    emptyRewardsHint: 'Primero genera tu código y completa el vínculo. Cuando aparezcan ganancias, se verán aquí automáticamente.',
    emptyRewardsAction: 'Generar mi código',
    rewardLine: 'Nivel de recompensa del invitado',
    commissionTail: 'tu comisión',
    rewardStatusFrozen: 'Congelada',
    rewardStatusAvailable: 'Disponible',
    rewardStatusRiskHold: 'Retención por riesgo',
    rewardStatusDefault: 'En proceso',
  },
  id: {
    navBind: 'Halaman bind',
    navInvite: 'Buat kode undangan saya',
    navEarnings: 'Lihat penghasilan tim saya',
    languageLabel: 'Bahasa',
    inviteKicker: 'INVITE CODE ENTRY',
    inviteTitle: 'Buat kode undangan saya',
    inviteSubtitle: 'Isi produk, WhatsApp, dan akun app untuk membuat kode sekali klik.',
    productLabel: 'Produk',
    whatsappLabel: 'Nomor WhatsApp',
    appAccountLabel: 'Akun app (8 digit)',
    generateButton: 'Buat kode undangan',
    generating: 'Membuat...',
    myInviteCode: 'Kode undangan saya',
    copyInviteCode: 'Salin kode undangan',
    issueSuccess: 'Kode undangan berhasil dibuat.',
    issueFailure: 'Gagal membuat kode undangan',
    copySuccess: 'Kode undangan disalin.',
    copyFailure: 'Gagal menyalin kode undangan.',
    earningsKicker: 'EARNINGS ENTRY',
    earningsTitle: 'Lihat penghasilan tim saya',
    earningsSubtitle: 'Lihat dua bagian: penghasilan bawahan + komisi kamu.',
    boardTitle: 'Penghasilan kamu akan terus diperbarui di sini',
    boardSubtitle: 'Dari kode undangan, bind, sampai reward masuk, halaman ini membantu kamu melihat progresnya dengan jelas.',
    boardBadgeCode: 'Kode undangan tetap sama',
    boardBadgeBind: 'Otomatis akumulasi setelah bind',
    boardBadgeStatus: 'Status reward langsung kelihatan',
    noSession: 'Belum ada sesi pengguna. Buat kode undangan dulu.',
    noSessionTitle: 'Buat kode undangan dulu',
    noSessionHint: 'Kalau belum mulai mengundang juga tidak masalah. Buat kode undangan dulu, lalu selesaikan bind. Penghasilan akan otomatis terkumpul di sini.',
    noSessionPrimary: 'Buat kode undangan saya',
    noSessionSecondary: 'Ke halaman bind',
    inviteeIncome: 'Penghasilan bawahan',
    inviteeIncomeBadge: 'Terkonfirmasi',
    myCommission: 'Komisi kamu',
    myCommissionBadge: 'Komisi terkumpul',
    availableReward: 'Reward tersedia',
    availableRewardBadge: 'Siap dilihat',
    frozenReward: 'Reward dibekukan',
    riskHoldReward: 'Tertahan risiko',
    inviteeIncomeHint: 'Akumulasi penghasilan terkonfirmasi dari tim kamu.',
    myCommissionHint: 'Komisi kamu yang dihitung dari catatan reward.',
    availableRewardHint: 'Reward yang sudah bisa diproses.',
    earningsOverview: 'Ringkasan penghasilan saya',
    overviewCardTitle: 'Kode undangan saat ini dan ringkasan reward',
    overviewBadge: 'Kode / Tim / Reward',
    progressTitle: 'Progres undangan saat ini',
    progressCardTitle: 'Setelah bind selesai, pengguna dan reward akan terus diperbarui di sini',
    progressBadge: 'Lacak progres',
    progressHint: 'Kode undangan kamu tetap sama. Setelah bind selesai, jumlah undangan, pengguna efektif, dan reward akan diperbarui bertahap di sini.',
    settlementTitle: 'Cara reward masuk',
    settlementCardTitle: 'Dibekukan → Tersedia → Tertahan risiko',
    settlementBadge: 'Alur reward',
    settlementHint: 'Reward biasanya masuk ke status beku dulu, lalu menjadi tersedia setelah syarat terpenuhi, dan bisa masuk ke penahanan risiko bila ada review risiko.',
    nextStepsTitle: 'Langkah berikutnya',
    nextInvite: 'Lanjut buat kode undangan',
    nextBind: 'Lanjut ke halaman bind',
    inviteCode: 'Kode undangan',
    invitedUsers: 'Jumlah undangan',
    effectiveUsers: 'Pengguna efektif',
    totalReward: 'Total reward',
    rewardRecords: 'Catatan reward',
    rewardActivityTitle: 'Aktivitas reward terbaru',
    rewardActivityHint: 'Setiap reward menampilkan status dan waktunya supaya kamu tahu kapan reward masuk.',
    rewardStatusGuideTitle: 'Penjelasan status',
    rewardStatusGuideFrozen: 'Dibekukan: reward masih menunggu settlement',
    rewardStatusGuideAvailable: 'Tersedia: reward sudah bisa dipakai',
    rewardStatusGuideRiskHold: 'Tertahan risiko: reward sedang masuk review risiko',
    loading: 'Memuat...',
    noRewards: 'Belum ada catatan reward.',
    emptyRewardsTitle: 'Belum ada catatan reward',
    emptyRewardsHint: 'Buat kode undangan dan selesaikan bind dulu. Setelah ada penghasilan, catatannya akan otomatis muncul di sini.',
    emptyRewardsAction: 'Buat kode undangan saya',
    rewardLine: 'Level reward bawahan',
    commissionTail: 'komisi kamu',
    rewardStatusFrozen: 'Dibekukan',
    rewardStatusAvailable: 'Tersedia',
    rewardStatusRiskHold: 'Tertahan risiko',
    rewardStatusDefault: 'Diproses',
  },
  pt: {
    navBind: 'Página de vínculo',
    navInvite: 'Gerar meu código',
    navEarnings: 'Ver ganhos da minha equipe',
    languageLabel: 'Idioma',
    inviteKicker: 'INVITE CODE ENTRY',
    inviteTitle: 'Gerar meu código',
    inviteSubtitle: 'Preencha produto, WhatsApp e conta do app para gerar um código com um clique.',
    productLabel: 'Produto',
    whatsappLabel: 'Número do WhatsApp',
    appAccountLabel: 'Conta do app (8 dígitos)',
    generateButton: 'Gerar código de convite',
    generating: 'Gerando...',
    myInviteCode: 'Meu código',
    copyInviteCode: 'Copiar código',
    issueSuccess: 'Código gerado.',
    issueFailure: 'Falha ao gerar o código',
    copySuccess: 'Código copiado.',
    copyFailure: 'Falha ao copiar o código.',
    earningsKicker: 'EARNINGS ENTRY',
    earningsTitle: 'Ver ganhos da minha equipe',
    earningsSubtitle: 'Veja duas partes: ganhos do convidado + sua comissão.',
    boardTitle: 'Seus ganhos vão continuar sendo atualizados aqui',
    boardSubtitle: 'Do código de convite ao vínculo e à liquidação, esta página ajuda você a acompanhar todo o progresso com clareza.',
    boardBadgeCode: 'O código não muda',
    boardBadgeBind: 'Acumula depois do vínculo',
    boardBadgeStatus: 'Status visível de imediato',
    noSession: 'Ainda não há sessão. Gere seu código primeiro.',
    noSessionTitle: 'Gere seu código primeiro',
    noSessionHint: 'Se você ainda não começou a convidar, tudo bem. Gere seu código primeiro e depois conclua o vínculo. Os ganhos vão começar a aparecer aqui automaticamente.',
    noSessionPrimary: 'Gerar meu código',
    noSessionSecondary: 'Ir para a página de vínculo',
    inviteeIncome: 'Ganhos dos convidados',
    inviteeIncomeBadge: 'Confirmados',
    myCommission: 'Sua comissão',
    myCommissionBadge: 'Comissão acumulada',
    availableReward: 'Recompensa disponível',
    availableRewardBadge: 'Pronta para ver',
    frozenReward: 'Recompensa congelada',
    riskHoldReward: 'Bloqueio de risco',
    inviteeIncomeHint: 'Ganhos confirmados dos membros da sua equipe.',
    myCommissionHint: 'Sua comissão somada a partir dos registros.',
    availableRewardHint: 'Recompensas já disponíveis para liquidação.',
    earningsOverview: 'Resumo dos meus ganhos',
    overviewCardTitle: 'Código atual e visão geral das recompensas',
    overviewBadge: 'Código / Equipe / Recompensas',
    progressTitle: 'Progresso atual dos convites',
    progressCardTitle: 'Depois do vínculo, usuários e recompensas continuarão sendo atualizados aqui',
    progressBadge: 'Acompanhar progresso',
    progressHint: 'Seu código de convite não muda. Depois do vínculo, convidados, usuários efetivos e recompensas serão atualizados aqui aos poucos.',
    settlementTitle: 'Como a recompensa entra',
    settlementCardTitle: 'Congelada → Disponível → Bloqueio de risco',
    settlementBadge: 'Caminho da recompensa',
    settlementHint: 'A recompensa normalmente entra primeiro como congelada, vira disponível após cumprir as condições e pode ir para bloqueio de risco se houver revisão.',
    nextStepsTitle: 'Próximos passos',
    nextInvite: 'Continuar para gerar meu código',
    nextBind: 'Continuar para vincular relação',
    inviteCode: 'Código de convite',
    invitedUsers: 'Convidados',
    effectiveUsers: 'Usuários efetivos',
    totalReward: 'Recompensa total',
    rewardRecords: 'Registros de recompensa',
    rewardActivityTitle: 'Atividade recente de recompensas',
    rewardActivityHint: 'Cada recompensa mostra o status e o horário para você acompanhar quando ela entra.',
    rewardStatusGuideTitle: 'Guia de status',
    rewardStatusGuideFrozen: 'Congelada: a recompensa está aguardando liquidação',
    rewardStatusGuideAvailable: 'Disponível: a recompensa já pode ser usada',
    rewardStatusGuideRiskHold: 'Bloqueio de risco: a recompensa está em revisão temporária',
    loading: 'Carregando...',
    noRewards: 'Ainda não há registros.',
    emptyRewardsTitle: 'Ainda não há registros',
    emptyRewardsHint: 'Gere seu código e conclua o vínculo primeiro. Quando houver ganhos, eles aparecerão aqui automaticamente.',
    emptyRewardsAction: 'Gerar meu código',
    rewardLine: 'Nível de recompensa do convidado',
    commissionTail: 'sua comissão',
    rewardStatusFrozen: 'Congelada',
    rewardStatusAvailable: 'Disponível',
    rewardStatusRiskHold: 'Bloqueio de risco',
    rewardStatusDefault: 'Em processamento',
  },
} as const

function InviteCodePage() {
  const [session, setSession] = useState<SessionState | null>(() => loadJsonState<SessionState>(STORAGE_KEY))
  const [locale, setLocale] = useState<keyof typeof externalPageCopyByLocale>(() => loadExternalLocale())
  const incomingInviteCode = typeof window !== 'undefined' ? new URLSearchParams(window.location.search).get('inviteCode')?.trim().toUpperCase() ?? '' : ''
  const [phoneForm, setPhoneForm] = useState({
    phoneNumber: '',
    verificationCode: '',
    inviteCode: incomingInviteCode || session?.inviteCode || '',
    countryCode: session?.countryCode ?? 'BR',
    languageCode: session?.languageCode ?? 'pt-br',
  })
  const [phoneCodeHint, setPhoneCodeHint] = useState('')
  const [phoneAuthLoading, setPhoneAuthLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const copy = externalPageCopyByLocale[locale]

  useEffect(() => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(EXTERNAL_LOCALE_KEY, locale)
    }
  }, [locale])

  async function handleCopyInviteCode() {
    const inviteCode = session?.inviteCode
    if (!inviteCode) return
    try {
      await navigator.clipboard.writeText(inviteCode)
      setSuccess(copy.copySuccess)
      setError('')
    } catch {
      setError(copy.copyFailure)
    }
  }

  async function handleShareInviteCode() {
    if (!session?.inviteCode) return
    const shareUrl = `${window.location.origin}/bind?inviteCode=${encodeURIComponent(session.inviteCode)}`
    try {
      if (navigator.share) {
        await navigator.share({ title: 'BANDEIRA 邀请', text: `使用邀请码 ${session.inviteCode} 完成绑定`, url: shareUrl })
      } else {
        await navigator.clipboard.writeText(shareUrl)
        setSuccess('邀请链接已复制。')
      }
      setError('')
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') return
      setError('分享失败，请稍后重试。')
    }
  }

  async function handleIssuePhoneCode() {
    setPhoneAuthLoading(true)
    setError('')
    setSuccess('')
    try {
      const response = await issuePhoneCode(phoneForm.phoneNumber)
      setPhoneCodeHint(response.verificationCode ? `测试验证码 ${response.verificationCode}，${response.ttlMinutes} 分钟内有效。` : `验证码已发送，${response.ttlMinutes} 分钟内有效。`)
      setSuccess('验证码已发送。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '获取验证码失败')
    } finally {
      setPhoneAuthLoading(false)
    }
  }

  async function handlePhoneLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPhoneAuthLoading(true)
    setError('')
    setSuccess('')
    try {
      const profile = await phoneLogin({
        phoneNumber: phoneForm.phoneNumber,
        verificationCode: phoneForm.verificationCode,
        inviteCode: phoneForm.inviteCode || undefined,
        countryCode: phoneForm.countryCode || undefined,
        languageCode: phoneForm.languageCode || undefined,
      })
      const nextSession = saveUserSession(profile)
      setSession(nextSession)
      setPhoneForm({ ...phoneForm, inviteCode: profile.inviteCode, countryCode: profile.countryCode, languageCode: profile.languageCode })
      setSuccess('登录成功，你的邀请码已准备好。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '手机号登录失败')
    } finally {
      setPhoneAuthLoading(false)
    }
  }

  return (
    <div className="consumer-app-page">
      <main className="consumer-shell consumer-form-shell">
        <header className="consumer-topbar">
          <a className="consumer-brand" href="/earnings"><img className="consumer-brand-logo" src="/bandeira-logo-v1.png" alt="" />BANDEIRA</a>
          <div className="consumer-topbar-actions">
            <label className="consumer-language-select">
              <select aria-label={copy.languageLabel} value={locale} onChange={(event) => setLocale(event.target.value as keyof typeof externalPageCopyByLocale)}>
                <option value="zh">中文</option><option value="en">English</option><option value="es">Español</option><option value="id">Bahasa Indonesia</option><option value="pt">Português</option>
              </select>
            </label>
            <a className="consumer-account-link" href="/earnings"><UserCircle size={26} weight="duotone" /><span>{copy.navEarnings}</span><CaretRight size={18} /></a>
          </div>
        </header>

        <section className="consumer-commercial-heading">
          <p><Diamond weight="fill" aria-hidden="true" /> BANDEIRA REWARDS</p>
          <h1>{copy.inviteTitle}</h1>
          <span>{copy.inviteSubtitle}</span>
        </section>

        {error ? <div className="consumer-banner is-error"><strong>操作失败</strong><span>{error}</span></div> : null}
        {success ? <div className="consumer-banner is-success"><CheckCircle size={20} weight="fill" /><span>{success}</span></div> : null}

        {session ? (
          <section className="consumer-invite-card">
            <div className="consumer-invite-card-top"><span>专属邀请权益</span><Diamond weight="fill" aria-hidden="true" /></div>
            <p>我的邀请码</p>
            <strong>{session.inviteCode}</strong>
            <span className="consumer-invite-caption">好友完成绑定后，邀请进度会自动更新。</span>
            <div className="consumer-invite-actions">
              <button type="button" onClick={handleCopyInviteCode}><Copy size={21} />复制邀请码</button>
              <button type="button" onClick={handleShareInviteCode}><ShareNetwork size={21} />分享邀请链接</button>
            </div>
          </section>
        ) : (
          <form id="phone-login" className="consumer-form-card" onSubmit={handlePhoneLogin}>
            <div className="consumer-form-card-heading"><div><h2>登录后开始邀请</h2><p>验证码登录，无需设置密码。</p></div><ShieldCheck size={28} weight="duotone" /></div>
            <label className="consumer-field"><span>手机号 / WhatsApp</span><div className="consumer-input-with-icon"><Phone size={20} /><input value={phoneForm.phoneNumber} onChange={(e) => setPhoneForm({ ...phoneForm, phoneNumber: e.target.value })} placeholder="例如 +5511999999999" /></div></label>
            <label className="consumer-field"><span>验证码</span><div className="consumer-code-row"><input value={phoneForm.verificationCode} onChange={(e) => setPhoneForm({ ...phoneForm, verificationCode: e.target.value.replace(/\D/g, '').slice(0, 6) })} placeholder="6 位验证码" /><button type="button" onClick={handleIssuePhoneCode} disabled={phoneAuthLoading || !phoneForm.phoneNumber.trim()}>获取验证码</button></div></label>
            <div className="consumer-field-grid">
              <label className="consumer-field"><span>邀请码（首次注册必填）</span><input value={phoneForm.inviteCode} onChange={(e) => setPhoneForm({ ...phoneForm, inviteCode: e.target.value.trim().toUpperCase() })} placeholder="新用户请输入有效邀请码" /></label>
              <label className="consumer-field"><span>国家</span><input value={phoneForm.countryCode} onChange={(e) => setPhoneForm({ ...phoneForm, countryCode: e.target.value.trim().toUpperCase() })} placeholder="BR" /></label>
            </div>
            {phoneCodeHint ? <p className="consumer-form-note">{phoneCodeHint}</p> : null}
            <button className="consumer-form-submit" type="submit" disabled={phoneAuthLoading || !phoneForm.phoneNumber.trim() || phoneForm.verificationCode.length < 6}><SignIn size={21} />手机号登录</button>
          </form>
        )}

        {session ? (
          <section className="consumer-account-card">
            <IdentificationCard size={30} weight="duotone" />
            <div><span>当前账户</span><strong>用户 {session.userId} · {session.countryCode}</strong></div>
            <a href={`/bind?inviteCode=${encodeURIComponent(session.inviteCode)}`}>去绑定<CaretRight size={18} /></a>
          </section>
        ) : null}

        <nav className="consumer-bottom-nav" aria-label="主要导航">
          <a href="/earnings"><span><Diamond size={24} weight="duotone" /></span><small>{copy.navEarnings}</small></a>
          <a className="is-active" href="/invite"><span><UserPlus size={24} weight="duotone" /></span><small>{copy.navInvite}</small></a>
          <a href="/bind"><span><UserCircle size={24} weight="duotone" /></span><small>{copy.navBind}</small></a>
        </nav>
      </main>
    </div>
  )
}

function EarningsPage() {
  const [session, setSession] = useState<SessionState | null>(() => loadJsonState<SessionState>(STORAGE_KEY))
  const [locale, setLocale] = useState<keyof typeof externalPageCopyByLocale>(() => loadExternalLocale())
  const [home, setHome] = useState<DistributionHomeResponse | null>(null)
  const [team, setTeam] = useState<TeamListResponse | null>(null)
  const [teamWeeklyIncome, setTeamWeeklyIncome] = useState<TeamWeeklyIncomeResponse | null>(null)
  const [rewards, setRewards] = useState<RewardListResponse | null>(null)
  const [rewardSummary, setRewardSummary] = useState<RewardSummaryResponse | null>(null)
  const [withdrawRequest, setWithdrawRequest] = useState<WithdrawRequestResponse | null>(null)
  const [withdrawHistory, setWithdrawHistory] = useState<WithdrawHistoryListResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [showBalance, setShowBalance] = useState(true)
  const [teamDetailsOpen, setTeamDetailsOpen] = useState(false)
  const [rewardDetailsOpen, setRewardDetailsOpen] = useState(false)
  const copy = externalPageCopyByLocale[locale]

  useEffect(() => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(EXTERNAL_LOCALE_KEY, locale)
    }
  }, [locale])

  useEffect(() => {
    async function loadData() {
      if (!session) return
      setLoading(true)
      setError('')
      try {
        const [homeData, teamData, teamWeeklyIncomeData, rewardData, rewardSummaryData, withdrawHistoryData] = await Promise.all([
          getDistributionHome(session.userId, session.accessToken),
          getDistributionTeam(session.userId, session.accessToken),
          getDistributionTeamWeeklyIncome(session.userId, session.accessToken),
          getDistributionRewards(session.userId, session.accessToken),
          getDistributionRewardSummary(session.userId, session.accessToken),
          getWithdrawHistory(session.userId, session.accessToken, { page: 0, size: 10 }),
        ])
        setHome(homeData)
        setTeam(teamData)
        setTeamWeeklyIncome(teamWeeklyIncomeData)
        setRewards(rewardData)
        setRewardSummary(rewardSummaryData)
        setWithdrawHistory(withdrawHistoryData)
      } catch (err) {
        const message = err instanceof Error ? err.message : '加载收益失败'
        if (/access denied|unauthorized|session/i.test(message)) {
          window.localStorage.removeItem(STORAGE_KEY)
          setSession(null)
          setError('登录状态已过期，请重新登录。')
        } else {
          setError(message)
        }
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [session])

  async function handleCreateWithdrawRequest() {
    if (!session) return
    setLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const request = await createWithdrawRequest(session.userId, session.accessToken)
      setWithdrawRequest(request)
      setSuccessMessage(`提现申请已提交，申请单号 ${request.requestNo}，本次申请钻石 ${request.requestedDiamondAmount}。`)
      const [homeData, rewardData, withdrawHistoryData] = await Promise.all([
        getDistributionHome(session.userId, session.accessToken),
        getDistributionRewards(session.userId, session.accessToken),
        getWithdrawHistory(session.userId, session.accessToken, { page: 0, size: 10 }),
      ])
      setHome(homeData)
      setRewards(rewardData)
      setWithdrawHistory(withdrawHistoryData)
    } catch (err) {
      setError(err instanceof Error ? err.message : '发起提现申请失败')
    } finally {
      setLoading(false)
    }
  }

  function getRewardStatusLabel(status?: string) {
    if (status === 'FROZEN') return copy.rewardStatusFrozen
    if (status === 'AVAILABLE') return copy.rewardStatusAvailable
    if (status === 'RISK_HOLD') return copy.rewardStatusRiskHold
    return copy.rewardStatusDefault
  }

  const inviteeIncome = team?.items.reduce((sum, item) => sum + item.confirmedIncomeTotal, 0) ?? 0
  const myCommission = rewards?.items.reduce((sum, item) => sum + item.rewardAmount, 0) ?? 0
  const rewardItems = rewards?.items ?? []
  const rewardTierSummary = rewardSummary?.tiers ?? []
  const tierSummaryByLevel = new Map(rewardTierSummary.map((tier) => [tier.rewardLevel, tier]))

  function getRewardActivityTitle(level?: number | null) {
    if (locale !== 'zh') return formatBusinessRewardLevel(level, locale)
    if (level === 1) return '直接邀请奖励'
    if (level === 2) return '历史二级佣金（只读）'
    if (level === 3) return '历史三级佣金（只读）'
    return '历史层级佣金（只读）'
  }

  function formatRewardDate(value?: string) {
    if (!value) return '--'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return new Intl.DateTimeFormat(locale === 'zh' ? 'zh-CN' : locale, {
      month: 'short',
      day: 'numeric',
    }).format(date)
  }

  const availableReward = home?.availableReward ?? 0
  const totalReward = home?.totalReward ?? myCommission
  const frozenReward = home?.frozenReward ?? 0
  const effectiveUsersThisView = home?.effectiveUsers ?? 0
  const growthTarget = 10
  const growthProgress = Math.min(100, Math.round((effectiveUsersThisView / growthTarget) * 100))
  const growthRemaining = Math.max(0, growthTarget - effectiveUsersThisView)

  return (
    <div className="consumer-app-page">
      <main className="consumer-shell">
        <header className="consumer-topbar">
          <a className="consumer-brand" href="/earnings"><img className="consumer-brand-logo" src="/bandeira-logo-v1.png" alt="" />BANDEIRA</a>
          <div className="consumer-topbar-actions">
            {!session ? (
              <select className="consumer-language" aria-label={copy.languageLabel} value={locale} onChange={(event) => setLocale(event.target.value as keyof typeof externalPageCopyByLocale)}>
                <option value="zh">中文</option>
                <option value="en">EN</option>
                <option value="es">ES</option>
                <option value="id">ID</option>
                <option value="pt">PT</option>
              </select>
            ) : null}
            <a className="consumer-account-link" href="/invite#phone-login">
              <UserCircle weight="regular" aria-hidden="true" />
              <span>{session ? '我的账户' : '登录'}</span>
              <CaretRight weight="bold" aria-hidden="true" />
            </a>
          </div>
        </header>

        {error ? <div className="consumer-banner is-error" role="alert">{error}</div> : null}
        {!error && successMessage ? <div className="consumer-banner is-success" role="status">{successMessage}</div> : null}

        {!session ? (
          <section className="consumer-auth-gate">
            <div className="consumer-auth-icon"><Wallet weight="duotone" aria-hidden="true" /></div>
            <p className="consumer-eyebrow">{copy.earningsOverview}</p>
            <h1>{copy.noSessionTitle}</h1>
            <p>{copy.noSessionHint}</p>
            <a className="consumer-primary-link" href="/invite#phone-login">
              {copy.noSessionPrimary}<ArrowRight weight="bold" aria-hidden="true" />
            </a>
            <a className="consumer-secondary-link" href="/bind">{copy.noSessionSecondary}</a>
          </section>
        ) : (
          <>
            <div className="consumer-home-greeting">
              <h1 className="consumer-visually-hidden">{copy.earningsTitle}</h1>
              <span className="consumer-home-avatar"><User weight="fill" aria-hidden="true" /></span>
              <div>
                <strong>{locale === 'zh' ? '早上好，伙伴！' : copy.earningsTitle}</strong>
                <span>{locale === 'zh' ? '每一次有效邀请，都在积累你的奖励。' : copy.earningsSubtitle}</span>
              </div>
              <a className="consumer-notification-link" href="#all-rewards" aria-label="查看奖励记录"><Bell weight="regular" aria-hidden="true" /><i /></a>
            </div>

            <section className="consumer-balance-card" aria-label={copy.availableReward}>
              <div className="consumer-balance-top">
                <div className="consumer-balance-label">
                  <span>{copy.availableReward}</span>
                  <button type="button" className="consumer-icon-button" onClick={() => setShowBalance((value) => !value)} aria-label={showBalance ? '隐藏余额' : '显示余额'}>
                    {showBalance ? <Eye weight="regular" aria-hidden="true" /> : <EyeSlash weight="regular" aria-hidden="true" />}
                  </button>
                </div>
                <button className="consumer-hero-withdraw" type="button" onClick={handleCreateWithdrawRequest} disabled={loading || availableReward <= 0}>
                  {loading ? '处理中…' : '申请提现'}<CaretRight weight="bold" aria-hidden="true" />
                </button>
              </div>
              <div className="consumer-balance-value">
                <Diamond weight="fill" aria-hidden="true" />
                <strong>{showBalance ? formatMoney(availableReward) : '••••••'}</strong>
              </div>
              <div className="consumer-balance-metrics">
                <div>
                  <span>{copy.frozenReward}</span>
                  <strong>{showBalance ? formatMoney(frozenReward) : '••••'}</strong>
                </div>
                <div>
                  <span>{copy.totalReward}</span>
                  <strong>{showBalance ? formatMoney(totalReward) : '••••'}</strong>
                </div>
                <div>
                  <span>{copy.inviteeIncome}</span>
                  <strong>{showBalance ? formatMoney(inviteeIncome) : '••••'}</strong>
                </div>
                <div>
                  <span>{copy.effectiveUsers}</span>
                  <strong>{effectiveUsersThisView}</strong>
                </div>
              </div>
            </section>

            <button className="consumer-growth-card" type="button" onClick={() => setTeamDetailsOpen(true)}>
              <span className="consumer-growth-medal"><Medal weight="duotone" aria-hidden="true" /></span>
              <span className="consumer-growth-copy">
                <span><strong>新星邀请人</strong><b>{effectiveUsersThisView}<small>/{growthTarget}</small></b></span>
                <i><em style={{ width: `${growthProgress}%` }} /></i>
                <small>{growthRemaining > 0 ? <>再邀请 <strong>{growthRemaining}</strong> 位有效用户，即可完成本阶段目标</> : '本阶段目标已完成，继续保持增长'}</small>
              </span>
              <CaretRight weight="bold" aria-hidden="true" />
            </button>

            <button className="consumer-team-summary" type="button" onClick={() => setTeamDetailsOpen(true)}>
              <span className="consumer-card-title"><UsersThree weight="fill" aria-hidden="true" />邀请概览（本周）</span>
              <CaretRight weight="bold" aria-hidden="true" />
              <span className="consumer-team-summary-grid">
                <span><small>已邀请用户</small><strong>{home?.directInvitedUsers ?? 0}</strong></span>
                <span><small>下线确认收益</small><strong>{formatMoney(inviteeIncome)}</strong></span>
                <span><small>我的累计奖励</small><strong>{formatMoney(totalReward)}</strong></span>
              </span>
            </button>

            <section className="consumer-task-section">
              <div className="consumer-section-head consumer-task-head">
                <h2><Target weight="fill" aria-hidden="true" />今天怎么推进收益</h2>
                <span>4 个关键动作</span>
              </div>

              <div className="consumer-task-list">
                <a href="/invite"><span className="is-orange"><UserPlus weight="fill" /></span><div><strong>邀请新用户</strong><small>当前已邀请 {home?.directInvitedUsers ?? 0} 人</small></div><b>去邀请</b></a>
                <a href="/bind"><span className="is-pink"><LinkSimple weight="bold" /></span><div><strong>完成平台绑定</strong><small>登记并验证 Timo / Linky ID</small></div><b>去绑定</b></a>
                <button type="button" onClick={() => setTeamDetailsOpen(true)}><span className="is-green"><UsersThree weight="fill" /></span><div><strong>跟进有效用户</strong><small>本期有效用户 {effectiveUsersThisView} 人</small></div><b>查看团队</b></button>
                <button type="button" onClick={() => setRewardDetailsOpen(true)}><span className="is-purple"><Sparkle weight="fill" /></span><div><strong>查看奖励记录</strong><small>当前共 {rewardItems.length} 笔奖励</small></div><b>查看记录</b></button>
              </div>
            </section>

            <section className="consumer-activity-section">
              <div className="consumer-section-head">
                <h2>{copy.rewardActivityTitle}</h2>
                <button type="button" onClick={() => setRewardDetailsOpen(true)}>{locale === 'zh' ? '全部记录' : copy.rewardRecords}<CaretRight weight="bold" aria-hidden="true" /></button>
              </div>

              {loading ? (
                <div className="consumer-loading-list" aria-label={copy.loading}>
                  <span /><span /><span />
                </div>
              ) : rewardItems.length ? (
                <div className="consumer-activity-list">
                  {rewardItems.slice(0, 3).map((item, index) => {
                    const isAvailable = item.rewardStatus === 'AVAILABLE'
                    return (
                      <article className="consumer-activity-row" key={`${item.sourceUserId}-${item.calculatedAt}-${index}`}>
                        <span className={`consumer-activity-icon ${isAvailable ? 'is-available' : 'is-frozen'}`}>
                          {isAvailable ? <CheckCircle weight="fill" aria-hidden="true" /> : <LockSimple weight="fill" aria-hidden="true" />}
                        </span>
                        <div className="consumer-activity-copy">
                          <strong>{getRewardActivityTitle(item.rewardLevel)}</strong>
                          <span>{locale === 'zh' ? '来自用户' : copy.inviteeIncome} #{item.sourceUserId}</span>
                        </div>
                        <div className={`consumer-activity-amount ${isAvailable ? 'is-available' : 'is-frozen'}`}>
                          <strong>+ {formatMoney(item.rewardAmount)}</strong>
                          <span>{getRewardStatusLabel(item.rewardStatus)} · {formatRewardDate(item.calculatedAt)}</span>
                        </div>
                      </article>
                    )
                  })}
                </div>
              ) : (
                <div className="consumer-empty-state">
                  <UserPlus weight="duotone" aria-hidden="true" />
                  <div><strong>{copy.emptyRewardsTitle}</strong><p>{copy.emptyRewardsHint}</p></div>
                  <a href="/invite">{copy.emptyRewardsAction}</a>
                </div>
              )}
            </section>

            <section className="consumer-details" id="team-details">
              <details open={teamDetailsOpen} onToggle={(event) => setTeamDetailsOpen(event.currentTarget.open)}>
                <summary><span>团队概览</span><strong>{home?.totalTeamUsers ?? 0} 人</strong></summary>
                <div className="consumer-detail-grid">
                  <div><span>一级用户</span><strong>{home?.directInvitedUsers ?? 0}</strong></div>
                  <div><span>二级用户</span><strong>{home?.secondLevelInvitedUsers ?? 0}</strong></div>
                  <div><span>三级用户</span><strong>{home?.thirdLevelInvitedUsers ?? 0}</strong></div>
                  <div><span>团队本周收入</span><strong>{formatMoney(teamWeeklyIncome?.currentWeekTeamIncome)}</strong></div>
                </div>
              </details>
              <details id="all-rewards" open={rewardDetailsOpen} onToggle={(event) => setRewardDetailsOpen(event.currentTarget.open)}>
                <summary><span>奖励明细</span><strong>{rewardItems.length} 笔</strong></summary>
                <div className="consumer-detail-grid">
                  {[1, 2, 3].map((level) => {
                    const tier = tierSummaryByLevel.get(level)
                    return <div key={level}><span>{tier?.businessLevelLabel || formatBusinessRewardLevel(level, locale)}</span><strong>{formatMoney(tier?.rewardAmount)}</strong></div>
                  })}
                  <div><span>{copy.inviteeIncome}</span><strong>{formatMoney(inviteeIncome)}</strong></div>
                </div>
              </details>
              <details>
                <summary><span>提现记录</span><strong>{withdrawHistory?.total ?? 0} 条</strong></summary>
                {withdrawRequest ? <p className="consumer-detail-note">最近申请 {withdrawRequest.requestNo} · {withdrawRequest.requestedDiamondAmount} 钻石</p> : null}
                {withdrawHistory?.items?.length ? (
                  <div className="consumer-withdraw-list">
                    {withdrawHistory.items.slice(0, 5).map((item) => (
                      <div key={item.requestNo}><span>{item.requestNo}<small>{formatRewardDate(item.requestedAt)}</small></span><strong>{item.requestedDiamondAmount} · {item.requestStatus}</strong></div>
                    ))}
                  </div>
                ) : <p className="consumer-detail-note">还没有提现记录。</p>}
              </details>
            </section>
          </>
        )}

        <nav className="consumer-bottom-nav" aria-label="用户导航">
          <a className="is-active" href="/earnings"><Wallet weight="fill" aria-hidden="true" /><span>{locale === 'zh' ? '收益' : copy.navEarnings}</span></a>
          <a href="/invite"><UserPlus weight="regular" aria-hidden="true" /><span>{locale === 'zh' ? '邀请' : copy.navInvite}</span></a>
          <a href="/invite#phone-login"><User weight="regular" aria-hidden="true" /><span>{locale === 'zh' ? '我的' : '账户'}</span></a>
        </nav>
      </main>
    </div>
  )
}

function App() {
  const pathname = typeof window !== 'undefined' ? window.location.pathname : '/'
  if (pathname.startsWith('/bind')) return <BindLandingPage />
  if (pathname.startsWith('/invite')) return <InviteCodePage />
  if (pathname.startsWith('/earnings')) return <EarningsPage />
  const designPreview = import.meta.env.DEV && typeof window !== 'undefined' && new URLSearchParams(window.location.search).get('adminPreview') === '1'
  return <ConsoleApp initialAdminSession={designPreview ? {
    sessionToken: 'local-design-preview',
    expiresAt: '2099-12-31T23:59:59Z',
    username: 'design-preview',
    displayName: 'BANDEIRA Admin',
    role: 'super_admin',
    platformScope: '*',
    guildScope: '*',
    regionScope: 'BR',
  } : null} />
}

export { ConsoleApp }
export default App
