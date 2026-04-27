import { useEffect, useMemo, useState, type FormEvent } from 'react'
import './App.css'
import {
  adjustAdminRelation,
  applyAdminRiskEventAction,
  correctAdminOwnership,
  createAdminSession,
  createProfile,
  getAdminAuditLogs,
  getAdminLinkyReplayRecords,
  getAdminLinkyWebhookLogs,
  getAdminOverview,
  getAdminOwnership,
  getAdminRelation,
  getAdminRewards,
  getAdminRiskEvents,
  getDistributionHome,
  getDistributionRewards,
  getDistributionTeam,
  issueInviteCode,
  registerInviteBinding,
  type AuditLogListResponse,
  type DistributionHomeResponse,
  type InviteBindingResponse,
  type IssueInviteCodeResponse,
  type LinkyReplayRecordListResponse,
  type LinkyWebhookLogListResponse,
  type OverviewReportResponse,
  type OwnershipDetailResponse,
  type ProfileResponse,
  type RelationDetailResponse,
  type RewardListResponse,
  type RiskEventListResponse,
  type TeamListResponse,
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
import {
  buildAdminSectionLinks,
  buildAdminWorkspaceShortcuts,
  buildEmptyStatePreset,
  buildLinkyDiagnosticSnapshot,
  type AdminWorkspaceShortcut,
} from './opsConsole'
import { buildPublicEntryLinks } from './publicEntries'

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
}

type AdminProductKey = 'ALL' | 'LINKY'
type RiskActionName = 'HANDLE' | 'IGNORE' | 'FREEZE_USER' | 'UNFREEZE_USER'

type PendingRiskAction = {
  riskEventId: number
  userId: number
  riskStatus: string
  action: RiskActionName
  note: string
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
}

type SelectedLinkyDrawer =
  | { kind: 'webhook'; item: LinkyWebhookLogListResponse['items'][number] }
  | { kind: 'replay'; item: LinkyReplayRecordListResponse['items'][number] }

const STORAGE_KEY = 'fenxiao-web-session'
const PROFILE_CREATE_TOKEN_KEY = 'fenxiao-profile-create-token'
const EXTERNAL_LOCALE_KEY = 'fenxiao-external-locale'
const ADMIN_REWARD_QUERY_KEY = 'fenxiao-admin-reward-query'
const RISK_QUERY_KEY = 'fenxiao-admin-risk-query'
const LINKY_WEBHOOK_QUERY_KEY = 'fenxiao-linky-webhook-query'
const LINKY_REPLAY_QUERY_KEY = 'fenxiao-linky-replay-query'
const ADMIN_PRODUCT_OPTIONS: Array<{ value: AdminProductKey; label: string }> = [
  { value: 'ALL', label: '全部产品' },
  { value: 'LINKY', label: 'Linky' },
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

function ConsoleApp({ initialViewMode = 'user' }: ConsoleAppProps) {
  void initialViewMode
  const [session, setSession] = useState<SessionState | null>(() => loadJsonState<SessionState>(STORAGE_KEY))
  const [adminSession, setAdminSession] = useState<AdminAuthState | null>(null)
  const [adminPassword, setAdminPassword] = useState('')
  const [adminProduct, setAdminProduct] = useState<AdminProductKey>('ALL')
  const [showAdvancedOps, setShowAdvancedOps] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [home, setHome] = useState<DistributionHomeResponse | null>(null)
  const [team, setTeam] = useState<TeamListResponse | null>(null)
  const [rewards, setRewards] = useState<RewardListResponse | null>(null)
  const [adminOverview, setAdminOverview] = useState<OverviewReportResponse | null>(null)
  const [adminRewards, setAdminRewards] = useState<RewardListResponse | null>(null)
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
  const [riskQuery, setRiskQuery] = useState(() => loadJsonState<{ userId: string; riskStatus: string; startAt: string; endAt: string; page: string; size: string }>(RISK_QUERY_KEY) || {
    userId: '',
    riskStatus: '',
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
  const [pendingRiskAction, setPendingRiskAction] = useState<PendingRiskAction | null>(null)
  const [riskActionLoadingId, setRiskActionLoadingId] = useState<number | null>(null)
  const [selectedLinkyDrawer, setSelectedLinkyDrawer] = useState<SelectedLinkyDrawer | null>(null)
  const [ownershipQueryUserId, setOwnershipQueryUserId] = useState('')
  const [ownershipCorrectionProductCode, setOwnershipCorrectionProductCode] = useState('LINKY')
  const [ownershipCorrectionNote, setOwnershipCorrectionNote] = useState('')
  const [ownershipCorrectionLoading, setOwnershipCorrectionLoading] = useState(false)
  const [relationQueryUserId, setRelationQueryUserId] = useState('')
  const [relationAdjustInviterId, setRelationAdjustInviterId] = useState('')
  const [relationAdjustNote, setRelationAdjustNote] = useState('')
  const [relationBeforeAdjust, setRelationBeforeAdjust] = useState<RelationDetailResponse | null>(null)
  const [pendingRelationChange, setPendingRelationChange] = useState<PendingRelationChange | null>(null)
  const [relationAdjustLoading, setRelationAdjustLoading] = useState(false)
  const [profileCreateToken, setProfileCreateToken] = useState(() => loadPlainState(PROFILE_CREATE_TOKEN_KEY))

  const canLoadData = useMemo(() => Boolean(session?.userId && session?.accessToken), [session])
  const canLoadAdmin = useMemo(() => Boolean(adminSession?.sessionToken), [adminSession])
  const canCreateProfile = useMemo(() => Boolean(profileCreateToken.trim() && form.userId.trim()), [profileCreateToken, form.userId])
  const currentAdminProductLabel = ADMIN_PRODUCT_OPTIONS.find((item) => item.value === adminProduct)?.label ?? '全部产品'
  const activeAdminProductCode = adminProduct === 'ALL' ? undefined : adminProduct
  const showingProductSpecificDiagnostics = adminProduct === 'LINKY'
  const publicEntryLinks = useMemo(
    () => buildPublicEntryLinks(typeof window !== 'undefined' ? window.location.origin : ''),
    [],
  )

  useEffect(() => {
    localStorage.setItem(ADMIN_REWARD_QUERY_KEY, JSON.stringify(adminRewardQuery))
  }, [adminRewardQuery])

  useEffect(() => {
    localStorage.setItem(RISK_QUERY_KEY, JSON.stringify(riskQuery))
  }, [riskQuery])

  useEffect(() => {
    localStorage.setItem(LINKY_WEBHOOK_QUERY_KEY, JSON.stringify(linkyWebhookQuery))
  }, [linkyWebhookQuery])

  useEffect(() => {
    localStorage.setItem(LINKY_REPLAY_QUERY_KEY, JSON.stringify(linkyReplayQuery))
  }, [linkyReplayQuery])

  useEffect(() => {
    setAdminOverview(null)
    setAdminRewards(null)
    setRiskEvents(null)
    setAdminOwnership(null)
    setAdminRelation(null)
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
        inviteCode: form.inviteCode.trim() || undefined,
      })
      const nextSession = saveUserSession(profile)
      setSession(nextSession)
      setHome(null)
      setTeam(null)
      setRewards(null)
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
      const nextSession = await createAdminSession(adminPassword)
      setAdminSession(nextSession)
      setAdminPassword('')
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

  function handleAdminLogout() {
    setAdminSession(null)
    setAdminOverview(null)
    setAdminRewards(null)
    setRiskEvents(null)
    setAuditLogs(null)
    setAdminRelation(null)
    setLinkyWebhookLogs(null)
    setLinkyReplayRecords(null)
    setHasQueriedAdminRewards(false)
    setHasQueriedRiskEvents(false)
    setHasQueriedLinkyWebhookLogs(false)
    setHasQueriedLinkyReplayRecords(false)
    setPendingRiskAction(null)
    setSelectedLinkyDrawer(null)
    setPendingRelationChange(null)
    setRiskActionDrafts({})
    setSuccessMessage('')
  }

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

  function openExternalLandingPage(path: '/invite' | '/bind' | '/earnings') {
    const target = typeof window !== 'undefined'
      ? `${window.location.origin}${path}`
      : path
    if (typeof window !== 'undefined') {
      window.open(target, '_blank', 'noopener,noreferrer')
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

  async function handleLoadDashboard() {
    if (!session) return
    setLoading(true)
    setError('')
    try {
      const [homeData, teamData, rewardData] = await Promise.all([
        getDistributionHome(session.userId, session.accessToken),
        getDistributionTeam(session.userId, session.accessToken),
        getDistributionRewards(session.userId, session.accessToken),
      ])
      setHome(homeData)
      setTeam(teamData)
      setRewards(rewardData)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载用户工作台失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleLoadAdminOverview() {
    if (!adminSession) return
    setLoading(true)
    setError('')
    try {
      const overview = await getAdminOverview(adminSession.sessionToken, activeAdminProductCode)
      setAdminOverview(overview)
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

  function handleLogout() {
    localStorage.removeItem(STORAGE_KEY)
    setSession(null)
    setHome(null)
    setTeam(null)
    setRewards(null)
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

  const pendingRiskCount = riskEvents?.items?.filter((item) => item.riskStatus === 'PENDING').length ?? 0
  const processedLinkyRequestCount = linkyWebhookLogs?.items?.filter((item) => item.requestStatus === 'PROCESSED').length ?? 0
  const replayedLinkyRequestCount = linkyReplayRecords?.items?.filter((item) => item.hitCount > 1).length
    ?? linkyWebhookLogs?.items?.filter((item) => item.replayRecordStatus === 'REPLAYED').length
    ?? 0

  const adminSummaryItems = [
    {
      label: '后台状态',
      value: adminSession ? '已进入后台' : '待进入',
      hint: adminSession ? `到期 ${formatDateTime(adminSession.expiresAt)}` : '拿到口令后先进入后台，后面的概览、关系和收益才会联动。',
    },
    {
      label: '当前产品视角',
      value: currentAdminProductLabel,
      hint: adminProduct === 'ALL' ? '当前按多产品总盘子查看，Linky 只是其中一个产品选项。' : '当前已切到 Linky 视角，便于继续看该产品的绑定、收益和高级排查。',
    },
    {
      label: '当前焦点',
      value: !adminSession ? '先登录' : !adminOverview ? '先同步总览' : pendingRiskCount > 0 ? '先处理异常' : '按问题进入模块',
      hint: !adminSession
        ? '首页第一步先建立后台会话。'
        : !adminOverview
          ? '先拉一次总览，再决定后面先查收益、绑定还是异常。'
          : pendingRiskCount > 0
            ? '当前有待处理异常，建议优先看异常处理。'
            : '可以直接按问题进入收益记录、绑定关系或高级排查。',
    },
  ]

  const rewardPageLabel = adminRewards
    ? `本次命中 ${adminRewards.total} 条，当前第 ${adminRewards.page + 1} 页，每页 ${adminRewards.size} 条。`
    : '先执行一次奖励查询'
  const riskPageLabel = riskEvents
    ? `本次命中 ${riskEvents.total} 条，当前第 ${riskEvents.page + 1} 页，每页 ${riskEvents.size} 条。`
    : '先执行一次风险事件查询'
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
  const hasLinkyWebhookPrevPage = Number(linkyWebhookQuery.page) > 0
  const hasLinkyWebhookNextPage = linkyWebhookLogs ? (linkyWebhookLogs.page + 1) * linkyWebhookLogs.size < linkyWebhookLogs.total : false
  const hasLinkyReplayPrevPage = Number(linkyReplayQuery.page) > 0
  const hasLinkyReplayNextPage = linkyReplayRecords ? (linkyReplayRecords.page + 1) * linkyReplayRecords.size < linkyReplayRecords.total : false
  const relationPreview = adminRelation
    ? buildRelationPreview(adminRelation, relationAdjustInviterId)
    : null
  const activeOwnershipItem = adminOwnership?.items.find((item) => item.ownershipStatus === 'ACTIVE')
  const isJointWorkbenchReady = Boolean(adminOwnership || adminRelation)
  const jointAuditFocus = auditQuery.moduleName || '全部'
  const adminShortcutCards = buildAdminWorkspaceShortcuts({
    adminLoggedIn: Boolean(adminSession),
    overviewLoaded: Boolean(adminOverview),
    pendingRiskCount,
  })
  const adminSectionLinks = buildAdminSectionLinks()
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

  return (
    <div className="page-shell">
      <header className="hero-card">
        <div className="hero-copy">
          <div className="hero-badges">
            <Badge label="Fenxiao" tone="primary" />
            <Badge label="Distribution Console" tone="neutral" />
            <Badge label={currentAdminProductLabel} tone="success" />
          </div>
          <p className="eyebrow">Distribution Console</p>
          <h1>多产品分销运营后台</h1>
          <p className="subtext">
            后台按分销动作组织：概览、邀请码、绑定关系、收益记录、异常处理。产品只是选择维度，Linky 只是当前其中一个产品。
          </p>
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
          {session ? <button className="ghost-btn" onClick={handleLogout}>退出用户会话</button> : null}
        </div>
      </header>

      {error ? (
        <section className="alert-banner error">
          <strong>操作失败</strong>
          <span>{error}</span>
        </section>
      ) : successMessage ? (
        <section className="alert-banner info">
          <strong>最新进展</strong>
          <span>{successMessage}</span>
        </section>
      ) : (
        <section className="alert-banner info">
          <strong>当前建议</strong>
          <span>先看总览，再查邀请码、绑定关系、收益记录；产品事件排查放到高级入口里做。</span>
        </section>
      )}

      <section className="overview-grid">
        {adminSummaryItems.map((item) => <SummaryCard key={item.label} {...item} />)}
      </section>

      <section className="ops-priority-board">
          <div className="admin-nav-strip" id="admin-modules" aria-label="后台模块导航">
            {adminSectionLinks.map((item) => (
              <a key={item.label} className="admin-nav-chip" href={item.href}>{item.label}</a>
            ))}
          </div>
          <PanelSection
            sectionId="admin-start"
            eyebrow="Start Here"
            title="先做这 4 件事"
            description="首页只保留最关键的动作：先登录、再看总览、再管邀请码与对外入口，最后按问题进入具体模块。"
          >
            <div className="admin-shortcut-grid">
              {adminShortcutCards.map((item) => (
                <ShortcutCard key={item.title} {...item} />
              ))}
            </div>
          </PanelSection>
        </section>

      <div className="console-layout admin-layout">
        <main className="console-main">
              <PanelSection
                sectionId="admin-onboarding"
                eyebrow="Onboarding"
                title="分销接入"
                description="把内部接入、邀请码生成和基础数据同步统一收进运营后台。"
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
                      邀请码（可选）
                      <input value={form.inviteCode} onChange={(e) => setForm({ ...form, inviteCode: e.target.value })} placeholder="ABCD1234" />
                    </label>
                    <button className="primary-btn" type="submit" disabled={loading || !canCreateProfile}>创建 / 接入</button>
                  </form>
                  {session ? (
                    <InfoCard title="当前用户会话" tone="success">
                      <InfoRow label="用户 ID" value={session.userId} />
                      <InfoRow label="邀请码" value={session.inviteCode} />
                      <InfoRow label="国家 / 语言" value={`${session.countryCode} / ${session.languageCode}`} />
                      <InfoRow label="Access Token" value={session.accessToken} code />
                      <div className="action-row top-gap">
                        <button className="ghost-btn small-btn" type="button" onClick={() => handleCopyInviteCode(session.inviteCode)}>复制邀请码</button>
                        <button className="primary-btn small-btn" type="button" onClick={handleLoadDashboard} disabled={!canLoadData || loading}>同步当前用户收益</button>
                      </div>
                    </InfoCard>
                  ) : (
                    <EmptyState title="当前用户会话待接入" description="完成一次接入后，这里会显示当前用户身份、邀请码和令牌信息。" />
                  )}
                </div>
              </PanelSection>

              <PanelSection
                sectionId="admin-user-facts"
                eyebrow="User Snapshot"
                title="当前用户收益快照"
                description="把原来独立用户后台里的收益、团队和奖励快照收进运营后台。"
                action={<button className="primary-btn" onClick={handleLoadDashboard} disabled={!canLoadData || loading}>刷新当前用户收益</button>}
              >
                <div className="stats-grid">
                  <Metric label="邀请人数" value={home?.invitedUsers} hint="直属与裂变邀请总量" tone="neutral" />
                  <Metric label="有效用户" value={home?.effectiveUsers} hint="已满足锁定条件的用户" tone="success" />
                  <Metric label="总奖励" value={home?.totalReward} hint="累计奖励规模" tone="primary" />
                  <Metric label="可用奖励" value={home?.availableReward} hint="可进入后续提现流程" tone="success" />
                  <Metric label="冻结奖励" value={home?.frozenReward} hint="仍在冻结周期内" tone="warning" />
                  <Metric label="风险冻结" value={home?.riskHoldReward} hint="因风控暂时冻结" tone="danger" />
                </div>
                <div className="content-grid two-columns entity-grid top-gap-xl">
                  <PanelSection eyebrow="Team" title="直属团队" description="看当前用户的一度团队关系和锁定状态。">
                    {team?.items?.length ? (
                      <DataTable
                        headers={['用户ID', '邀请码', '国家', '有效用户', '确认收益', '锁定状态', '绑定时间']}
                        rows={team.items.map((item) => [
                          item.userId,
                          item.inviteCode,
                          item.countryCode,
                          item.effectiveUser ? '是' : '否',
                          item.confirmedIncomeTotal,
                          item.lockStatus,
                          item.bindTime,
                        ])}
                        emptyText="暂无团队数据"
                      />
                    ) : (
                      <EmptyState title="直属团队待同步" description="创建用户后并产生下级绑定，这里会显示一度团队成员。" />
                    )}
                  </PanelSection>

                  <PanelSection eyebrow="Rewards" title="奖励明细" description="快速看来源用户、奖励层级和当前奖励状态。">
                    {rewards?.items?.length ? (
                      <DataTable
                        headers={['来源用户', '层级', '奖励金额', '状态', '计算时间']}
                        rows={rewards.items.map((item) => [
                          item.sourceUserId,
                          item.rewardLevel,
                          item.rewardAmount,
                          item.rewardStatus,
                          item.calculatedAt,
                        ])}
                        emptyText="暂无奖励数据"
                      />
                    ) : (
                      <EmptyState title="奖励记录待同步" description="当用户链路产生收益事件后，这里会显示对应奖励明细。" />
                    )}
                  </PanelSection>
                </div>
              </PanelSection>

              <PanelSection
                sectionId="admin-login"
                eyebrow="Access"
                title="进入运营后台"
                description="先拿口令建立后台会话；进来之后再按概览、邀请码、收益、绑定和异常去处理。"
                action={adminSession ? <button className="ghost-btn" onClick={handleAdminLogout}>退出后台</button> : undefined}
              >
                {adminSession ? (
                  <InfoCard title="后台会话已建立" tone="success">
                    <InfoRow label="登录状态" value="已登录" />
                    <InfoRow label="会话到期" value={formatDateTime(adminSession.expiresAt)} />
                    <InfoRow label="安全说明" value="后台会话仅保存在当前页面内存，刷新页面后需重新登录。" />
                  </InfoCard>
                ) : (
                  <form className="grid-form compact-form single-line wide-line" onSubmit={handleAdminLogin}>
                    <label>
                      后台登录口令
                      <input type="password" value={adminPassword} onChange={(e) => setAdminPassword(e.target.value)} placeholder="请输入后台登录口令" />
                    </label>
                    <button className="primary-btn" type="submit" disabled={loading || !adminPassword.trim()}>登录后台</button>
                  </form>
                )}
              </PanelSection>

              <PanelSection
                sectionId="admin-overview"
                eyebrow="Overview"
                title="分销概览"
                description="按当前产品视角看邀请码、绑定关系、收益和异常总况。"
                action={<button className="primary-btn" onClick={handleLoadAdminOverview} disabled={loading || !canLoadAdmin}>同步分销概览</button>}
              >
                <div className="stats-grid">
                  <Metric label="邀请人数" value={adminOverview?.invitedUsers} hint="当前视角下累计邀请规模" tone="neutral" />
                  <Metric label="有效人数" value={adminOverview?.effectiveUsers} hint="已满足有效归因条件的用户" tone="success" />
                  <Metric label="累计奖励" value={adminOverview?.rewardTotal} hint="当前视角下累计奖励规模" tone="primary" />
                  <Metric label="冻结奖励" value={adminOverview?.frozenRewardTotal} hint="仍在冻结或待复核中的奖励" tone="warning" />
                  <Metric label="可用奖励" value={adminOverview?.availableRewardTotal} hint="当前已可结算的奖励" tone="success" />
                  <Metric label="待处理异常" value={adminOverview?.riskEventCount} hint="需要人工继续处理的异常或风险事件" tone="danger" />
                </div>
              </PanelSection>

              <PanelSection
                sectionId="admin-invite-ops"
                eyebrow="Invite & Entry"
                title="邀请码与对外入口"
                description="这里统一打开和复制对外三页。"
              >
                <InfoCard title="对外网页入口" tone="success">
                  <InlineHint text="常用顺序：先生成邀请码，再绑定关系，最后看收益。" />
                  {publicEntryLinks.map((item) => (
                    <div key={item.key} className="public-entry-item">
                      <InfoRow label={item.label} value={item.url} code />
                      <div className="action-row top-gap public-entry-actions">
                        <button className="ghost-btn small-btn" type="button" onClick={() => openExternalLandingPage(item.path)}>打开页面</button>
                        <button className="ghost-btn small-btn" type="button" onClick={() => copyPublicEntryLink(item.url)}>复制链接</button>
                      </div>
                    </div>
                  ))}
                </InfoCard>
              </PanelSection>

              <div className="content-grid two-columns entity-grid">
                <PanelSection
                  sectionId="admin-rewards"
                  eyebrow="Rewards"
                  title="收益记录管理"
                  description="按受益用户、状态和时间范围查奖励记录。"
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
                            <option value="FROZEN">FROZEN</option>
                            <option value="AVAILABLE">AVAILABLE</option>
                            <option value="RISK_HOLD">RISK_HOLD</option>
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

                <PanelSection
                  sectionId="admin-ownership"
                  eyebrow="Ownership"
                  title="产品归属管理"
                  description="按用户 ID 查看当前产品归属，并支持人工修正。"
                  action={<button className="primary-btn" onClick={handleLoadOwnership} disabled={loading || !ownershipQueryUserId || !canLoadAdmin}>查询产品归属</button>}
                >
                  <InfoCard title="查询入口" tone="neutral">
                    <div className="grid-form compact-form single-line">
                      <label>
                        用户 ID
                        <input value={ownershipQueryUserId} onChange={(e) => setOwnershipQueryUserId(e.target.value)} placeholder="例如 10003" />
                      </label>
                    </div>
                    <InlineHint text="先查当前用户已落在哪些产品，再决定是否需要人工切换归属。" />
                  </InfoCard>
                  {adminOwnership ? (
                    <div className="stack-gap relation-workbench">
                      <InfoCard title="当前产品归属" tone="success">
                        <InfoRow label="用户 ID" value={adminOwnership.userId} />
                        <InfoRow label="当前 ACTIVE 归属" value={adminOwnership.items.find((item) => item.ownershipStatus === 'ACTIVE')?.productCode || '-'} />
                        <InfoRow label="归属记录数" value={adminOwnership.items.length} />
                      </InfoCard>

                      <DataTable
                        headers={['产品', '状态', '来源', '来源记录', '生效时间']}
                        rows={adminOwnership.items.map((item) => [
                          item.productCode,
                          renderStatusBadge(item.ownershipStatus),
                          item.ownershipSource,
                          item.sourceRecordId ? `${item.sourceRecordType} #${item.sourceRecordId}` : item.sourceRecordType,
                          formatDateTime(item.effectiveAt),
                        ])}
                        emptyText="暂无产品归属记录"
                      />
                    </div>
                  ) : (
                    <EmptyState title="暂无产品归属结果" description="输入用户 ID 后查询，这里会显示产品归属、来源记录和人工修正入口。" />
                  )}

                  <InfoCard title="人工修正" tone="neutral">
                    <div className="grid-form compact-form">
                      <label>
                        目标产品编码
                        <input value={ownershipCorrectionProductCode} onChange={(e) => setOwnershipCorrectionProductCode(e.target.value)} placeholder="例如 LINKY" />
                      </label>
                      <label>
                        修正备注
                        <input value={ownershipCorrectionNote} onChange={(e) => setOwnershipCorrectionNote(e.target.value)} placeholder="例如：人工修正产品归属" />
                      </label>
                    </div>
                    <InlineHint text={adminOwnership ? '提交后，原 ACTIVE 归属会转成 CORRECTED，目标产品归属会切成 ACTIVE。' : '先查一位用户，再提交产品归属修正。'} />
                    <div className="table-toolbar">
                      <button className="primary-btn small-btn" onClick={handleCorrectOwnership} disabled={ownershipCorrectionLoading || !canLoadAdmin || !ownershipCorrectionProductCode.trim() || !adminOwnership}>提交产品归属修正</button>
                      <button className="ghost-btn small-btn" onClick={handleLoadOwnershipAudit} disabled={!canLoadAdmin}>查看 ownership 审计</button>
                    </div>
                  </InfoCard>

                  <InfoCard title="联合处置视图" tone="success">
                    <InfoRow label="当前用户" value={(adminOwnership?.userId ?? adminRelation?.userId ?? ownershipQueryUserId) || '-'} />
                    <InfoRow label="当前 ACTIVE 归属" value={activeOwnershipItem?.productCode || '待同步'} />
                    <InfoRow label="当前一级上级" value={adminRelation?.level1InviterId ?? '待同步'} />
                    <InfoRow label="当前审计焦点" value={jointAuditFocus} />
                    <InlineHint text={isJointWorkbenchReady ? '现在可以在同一块里看产品归属、绑定关系和审计入口。' : '先查产品归属，再把绑定关系和审计同步进来。'} />
                    <div className="table-toolbar">
                      <button className="primary-btn small-btn" onClick={handleLoadJointWorkbench} disabled={!canLoadAdmin || !ownershipQueryUserId.trim() || loading}>一键联合查询</button>
                      <button className="ghost-btn small-btn" onClick={handleSyncOwnershipRelation} disabled={!canLoadAdmin || (!adminOwnership && !ownershipQueryUserId.trim())}>同步绑定关系</button>
                      <button className="ghost-btn small-btn" onClick={handleLoadJointAudit} disabled={!canLoadAdmin}>查看联合审计</button>
                    </div>
                  </InfoCard>
                </PanelSection>

                <PanelSection
                  sectionId="admin-bindings"
                  eyebrow="Bindings"
                  title="绑定关系管理"
                  description="按用户 ID 查看当前邀请关系，并支持人工修正。"
                  action={<button className="primary-btn" onClick={handleLoadRelation} disabled={loading || !relationQueryUserId || !canLoadAdmin}>查询绑定关系</button>}
                >
                  <InfoCard title="查询入口" tone="neutral">
                    <div className="grid-form compact-form single-line">
                      <label>
                        用户 ID
                        <input value={relationQueryUserId} onChange={(e) => setRelationQueryUserId(e.target.value)} placeholder="例如 10003" />
                      </label>
                    </div>
                    <InlineHint text="适合运营、客服或风控定位单个用户的上下游关系。" />
                  </InfoCard>
                  {adminRelation ? (
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
                  )}
                </PanelSection>
              </div>

              <PanelSection
                sectionId="admin-exceptions"
                eyebrow="Exceptions"
                title="异常处理"
                description="默认先看待处理异常。"
                action={<button className="primary-btn" onClick={handleLoadRiskEvents} disabled={loading || !canLoadAdmin}>查询异常</button>}
              >
                <div className="query-shell compact-query-shell">
                  <div className="grid-form compact-form exception-filter-grid">
                    <label>
                      用户 ID
                      <input value={riskQuery.userId} onChange={(e) => setRiskQuery({ ...riskQuery, userId: e.target.value })} placeholder="例如 13002" />
                    </label>
                    <label>
                      风险状态
                      <select value={riskQuery.riskStatus} onChange={(e) => setRiskQuery({ ...riskQuery, riskStatus: e.target.value })}>
                        <option value="">全部</option>
                        <option value="PENDING">PENDING</option>
                        <option value="HANDLED">HANDLED</option>
                        <option value="IGNORED">IGNORED</option>
                      </select>
                    </label>
                  </div>
                  <InlineHint text={riskQuery.riskStatus ? riskPageLabel : '默认先查 PENDING；没有结果再切全部。'} />
                  <div className="table-toolbar">
                    <button className="ghost-btn small-btn" onClick={() => handleRiskPageChange(Number(riskQuery.page) - 1)} disabled={loading || !hasRiskPrevPage}>上一页</button>
                    <button className="ghost-btn small-btn" onClick={() => handleRiskPageChange(Number(riskQuery.page) + 1)} disabled={loading || !hasRiskNextPage}>下一页</button>
                  </div>
                </div>
                {riskEvents?.items?.length ? (
                  <div className="risk-event-list">
                    {riskEvents.items.map((item) => {
                      const draftNote = riskActionDrafts[item.id] || ''
                      return (
                        <div className="risk-event-card" key={item.id}>
                          <div className="risk-event-head">
                            <div>
                              <strong>风险事件 #{item.id}</strong>
                              <p>用户 #{item.userId} · {item.riskType}</p>
                            </div>
                            {renderStatusBadge(item.riskStatus)}
                          </div>
                          <div className="risk-event-meta">
                            <span>{item.handledAt ? `${formatDateTime(item.handledAt)} / #${item.handledBy ?? 0}` : '未处理'}</span>
                            <span>备注：{item.resultNote || '暂无'}</span>
                          </div>
                          <label className="note-field">
                            本次处理备注
                            <input value={draftNote} onChange={(e) => updateRiskActionDraft(item.id, e.target.value)} placeholder="例如：人工复核通过 / 确认异常冻结" />
                          </label>
                          <div className="action-row">
                            <button className="ghost-btn small-btn" onClick={() => openRiskActionConfirm(item, 'HANDLE')} disabled={riskActionLoadingId === item.id || !canHandleRisk(item.riskStatus)}>处理</button>
                            <button className="ghost-btn small-btn" onClick={() => openRiskActionConfirm(item, 'IGNORE')} disabled={riskActionLoadingId === item.id || !canIgnoreRisk(item.riskStatus)}>忽略</button>
                            <button className="ghost-btn small-btn warning-btn" onClick={() => openRiskActionConfirm(item, 'FREEZE_USER')} disabled={riskActionLoadingId === item.id || !canFreezeRisk(item.riskStatus)}>冻结用户</button>
                            <button className="ghost-btn small-btn success-btn" onClick={() => openRiskActionConfirm(item, 'UNFREEZE_USER')} disabled={riskActionLoadingId === item.id || !canUnfreezeRisk(item.riskStatus)}>解冻用户</button>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                ) : (
                  <EmptyState title="暂无异常结果" description="先查 PENDING，没有结果再放宽条件。" actionLabel={riskEmptyState.actionLabel} />
                )}
              </PanelSection>

              <PanelSection
                sectionId="admin-advanced"
                eyebrow="Advanced"
                title="高级排查"
                description="默认把产品事件日志、重复回放和审计日志收起来，只在需要时展开。"
                action={
                  <button
                    className="ghost-btn"
                    onClick={() => setShowAdvancedOps((current) => !current)}
                    disabled={!canLoadAdmin}
                  >
                    {showAdvancedOps ? '收起高级排查' : '展开高级排查'}
                  </button>
                }
              >
                <InfoCard title="当前排查范围" tone="neutral">
                  <InfoRow label="产品" value={currentAdminProductLabel} />
                  <InfoRow label="排查状态" value={showAdvancedOps ? '已展开' : '默认收起'} />
                  <InfoRow label="说明" value={showingProductSpecificDiagnostics ? '当前可查看 Linky 的产品事件日志、重复回放和审计记录。' : '先在上方完成概览、绑定关系、收益记录和异常处理；切到具体产品后再展开高级排查。'} />
                </InfoCard>
              </PanelSection>

              {showAdvancedOps && showingProductSpecificDiagnostics ? (
                <>
                  <PanelSection
                    eyebrow="Product Events"
                    title={`${currentAdminProductLabel} 产品事件日志`}
                    description="当前先开放 Linky 的事件排查；后续接入其他产品时沿用同一块高级排查位。"
                  >
                    <DiagnosticBanner
                      eyebrow="Triage"
                      tone={linkyDiagnosticSnapshot.tone}
                      title={linkyDiagnosticSnapshot.title}
                      description={linkyDiagnosticSnapshot.summary}
                    />
                    <div className="content-grid two-columns entity-grid">
                      <div className="stack-gap">
                        <InfoCard title="事件日志查询" tone="neutral">
                          <div className="query-shell soft-query-shell">
                            <div className="grid-form compact-form">
                              <label>
                                Linky 订单号
                                <input value={linkyWebhookQuery.linkyOrderId} onChange={(e) => setLinkyWebhookQuery({ ...linkyWebhookQuery, linkyOrderId: e.target.value })} placeholder="例如 order-20260421-001" />
                              </label>
                              <label>
                                用户 ID
                                <input value={linkyWebhookQuery.userId} onChange={(e) => setLinkyWebhookQuery({ ...linkyWebhookQuery, userId: e.target.value })} placeholder="例如 13002" />
                              </label>
                              <label>
                                请求状态
                                <select value={linkyWebhookQuery.requestStatus} onChange={(e) => setLinkyWebhookQuery({ ...linkyWebhookQuery, requestStatus: e.target.value })}>
                                  <option value="">全部</option>
                                  <option value="PROCESSED">PROCESSED</option>
                                  <option value="DUPLICATE">DUPLICATE</option>
                                  <option value="REJECTED">REJECTED</option>
                                  <option value="FAILED">FAILED</option>
                                </select>
                              </label>
                              <label>
                                页码
                                <input type="number" min="0" value={linkyWebhookQuery.page} onChange={(e) => setLinkyWebhookQuery({ ...linkyWebhookQuery, page: e.target.value })} />
                              </label>
                              <label>
                                每页条数
                                <input type="number" min="1" max="100" value={linkyWebhookQuery.size} onChange={(e) => setLinkyWebhookQuery({ ...linkyWebhookQuery, size: e.target.value })} />
                              </label>
                            </div>
                            <div className="table-toolbar wrap-toolbar">
                              <button className="primary-btn small-btn" onClick={handleLoadLinkyWebhookLogs} disabled={linkyWebhookLoading || !canLoadAdmin}>查询事件日志</button>
                              <button className="ghost-btn small-btn" onClick={() => handleLinkyWebhookPageChange(Number(linkyWebhookQuery.page) - 1)} disabled={linkyWebhookLoading || !hasLinkyWebhookPrevPage}>上一页</button>
                              <button className="ghost-btn small-btn" onClick={() => handleLinkyWebhookPageChange(Number(linkyWebhookQuery.page) + 1)} disabled={linkyWebhookLoading || !hasLinkyWebhookNextPage}>下一页</button>
                            </div>
                            <InlineHint text={linkyWebhookPageLabel} />
                          </div>
                        </InfoCard>

                        {linkyWebhookLogs?.items?.length ? (
                          <div className="linky-card-list">
                            {linkyWebhookLogs.items.map((item) => (
                              <div className="linky-log-card" key={item.id}>
                                <div className="risk-event-head">
                                  <div>
                                    <strong>{item.linkyOrderId || `日志 #${item.id}`}</strong>
                                    <p>用户 #{item.userId ?? '-'} · event {item.sourceEventId || '-'} · {formatDateTime(item.requestReceivedAt || undefined)}</p>
                                  </div>
                                  {renderStatusBadge(item.requestStatus)}
                                </div>
                                <div className="link-grid">
                                  <InfoRow label="签名 / token" value={`${item.signatureStatus} / ${item.internalTokenStatus}`} />
                                  <InfoRow label="时间窗 / 指纹" value={`${item.replayStatus} / ${item.replayRecordStatus}`} />
                                  <InfoRow label="金额" value={item.incomeAmount !== null ? `${item.incomeAmount} ${item.currencyCode || ''}`.trim() : '-'} />
                                  <InfoRow label="支付时间" value={item.paidAt ? formatDateTime(item.paidAt) : '-'} />
                                </div>
                                <p className="inline-hint strong-hint">{buildLinkyWebhookSummary(item)}</p>
                                <div className="action-row top-gap">
                                  <button className="ghost-btn small-btn" type="button" onClick={() => setSelectedLinkyDrawer({ kind: 'webhook', item })}>查看详情</button>
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <EmptyState title={linkyWebhookEmptyState.title} description={linkyWebhookEmptyState.description} actionLabel={linkyWebhookEmptyState.actionLabel} />
                        )}
                      </div>

                      <div className="stack-gap">
                        <InfoCard title="重复回放记录查询" tone="success">
                          <div className="query-shell soft-query-shell">
                            <div className="grid-form compact-form">
                              <label>
                                Linky 订单号
                                <input value={linkyReplayQuery.linkyOrderId} onChange={(e) => setLinkyReplayQuery({ ...linkyReplayQuery, linkyOrderId: e.target.value })} placeholder="例如 order-20260421-001" />
                              </label>
                              <label>
                                用户 ID
                                <input value={linkyReplayQuery.userId} onChange={(e) => setLinkyReplayQuery({ ...linkyReplayQuery, userId: e.target.value })} placeholder="例如 13002" />
                              </label>
                              <label>
                                页码
                                <input type="number" min="0" value={linkyReplayQuery.page} onChange={(e) => setLinkyReplayQuery({ ...linkyReplayQuery, page: e.target.value })} />
                              </label>
                              <label>
                                每页条数
                                <input type="number" min="1" max="100" value={linkyReplayQuery.size} onChange={(e) => setLinkyReplayQuery({ ...linkyReplayQuery, size: e.target.value })} />
                              </label>
                            </div>
                            <div className="table-toolbar wrap-toolbar">
                              <button className="primary-btn small-btn" onClick={handleLoadLinkyReplayRecords} disabled={linkyReplayLoading || !canLoadAdmin}>查询重复回放</button>
                              <button className="ghost-btn small-btn" onClick={() => handleLinkyReplayPageChange(Number(linkyReplayQuery.page) - 1)} disabled={linkyReplayLoading || !hasLinkyReplayPrevPage}>上一页</button>
                              <button className="ghost-btn small-btn" onClick={() => handleLinkyReplayPageChange(Number(linkyReplayQuery.page) + 1)} disabled={linkyReplayLoading || !hasLinkyReplayNextPage}>下一页</button>
                            </div>
                            <InlineHint text={linkyReplayPageLabel} />
                          </div>
                        </InfoCard>

                        {linkyReplayRecords?.items?.length ? (
                          <DataTable
                            headers={['订单号', '用户', '首次命中', '最近命中', '摘要', '指纹', '操作']}
                            rows={linkyReplayRecords.items.map((item) => [
                              item.linkyOrderId || '-',
                              item.userId ? `#${item.userId}` : '-',
                              item.firstSeenAt ? formatDateTime(item.firstSeenAt) : '-',
                              item.lastSeenAt ? formatDateTime(item.lastSeenAt) : '-',
                              buildLinkyReplaySummary(item),
                              <FingerprintCell fingerprint={item.requestFingerprint} onCopy={handleCopyFingerprint} />,
                              <button className="ghost-btn small-btn" type="button" onClick={() => setSelectedLinkyDrawer({ kind: 'replay', item })}>查看详情</button>,
                            ])}
                            emptyText="暂无 replay 记录"
                          />
                        ) : (
                          <EmptyState title={linkyReplayEmptyState.title} description={linkyReplayEmptyState.description} actionLabel={linkyReplayEmptyState.actionLabel} />
                        )}
                      </div>
                    </div>
                  </PanelSection>

                  <PanelSection
                    eyebrow="Audit"
                    title="处理审计日志"
                    description="记录异常处理与绑定关系修正，后续其他产品也走同一块审计区。"
                    action={<button className="primary-btn" onClick={() => loadAuditLogs(auditQuery)} disabled={!canLoadAdmin}>刷新审计日志</button>}
                  >
                  <div className="grid-form compact-form">
                    <label>
                      模块
                      <select value={auditQuery.moduleName} onChange={(e) => setAuditQuery({ ...auditQuery, moduleName: e.target.value })}>
                        <option value="risk_event">risk_event</option>
                        <option value="relation">relation</option>
                        <option value="ownership">ownership</option>
                        <option value="">全部</option>
                      </select>
                    </label>
                    <label>
                      页码
                      <input type="number" min="0" value={auditQuery.page} onChange={(e) => setAuditQuery({ ...auditQuery, page: e.target.value })} />
                    </label>
                    <label>
                      每页条数
                      <input type="number" min="1" max="100" value={auditQuery.size} onChange={(e) => setAuditQuery({ ...auditQuery, size: e.target.value })} />
                    </label>
                  </div>
                  {auditLogs?.items?.length ? (
                    <DataTable
                      headers={['时间', '模块', '动作', '目标ID', '操作人', '备注']}
                      rows={auditLogs.items.map((item) => [
                        formatDateTime(item.operatedAt),
                        item.moduleName,
                        renderStatusBadge(item.actionName),
                        item.targetId,
                        `${item.operatorRole} / #${item.operatorId}`,
                        item.remark || '-',
                      ])}
                      emptyText="暂无处理记录"
                    />
                  ) : (
                    <EmptyState title="暂无处理记录" description="执行一次异常处理或关系修正后，这里会沉淀最近的操作审计。" />
                  )}
                </PanelSection>
              </>
            ) : null}
        </main>

        <aside className="console-side">
              <PanelSection eyebrow="Guide" title="多产品分销后台优先级">
                <ToastStack
                  tone="neutral"
                  items={[
                    '后台先按动作理解：登录 → 看总览 → 管邀请码与对外入口 → 再按收益 / 绑定 / 异常分模块处理。',
                    adminProduct === 'ALL' ? '当前是全部产品视角，适合先看总盘子。' : `当前聚焦 ${currentAdminProductLabel}，适合继续看该产品的绑定、收益和事件链路。`,
                  ]}
                />
              </PanelSection>

              <PanelSection eyebrow="Access" title="当前环境入口">
                <InfoCard title="本地 / 部署入口" tone="neutral">
                  <InfoRow label="本地后端" value="http://localhost:8080" code />
                  <InfoRow label="前端开发" value="http://localhost:5173" code />
                  <InfoRow label="Docker 前端" value="http://localhost:8088" code />
                  <InfoRow label="健康检查" value="http://localhost:8080/actuator/health" code />
                  <InfoRow label="部署文件" value="deploy/docker-compose.yml" code />
                </InfoCard>
              </PanelSection>

              <PanelSection eyebrow="Next" title="下一批后台能力">
                <RoadmapList
                  items={[
                    { title: '补产品级筛选能力', desc: '后续在概览、收益、异常查询接口里真正接上 product 参数。' },
                    { title: '统一产品事件排查框架', desc: '把 Linky 事件日志这套排查能力抽象成通用产品事件模块。' },
                    { title: '绑定关系修正审计增强', desc: '补足 before / after 和复核说明，保证人工修正可追溯。' },
                    { title: '更细粒度后台权限', desc: '为收益处理、关系修正、产品事件排查拆开权限边界。' },
                  ]}
                />
              </PanelSection>
        </aside>
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

function SummaryCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  const isPending = value.includes('待') || value === '未设置'
  return (
    <div className={`summary-card ${isPending ? 'is-pending' : 'is-ready'}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{hint}</p>
    </div>
  )
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

function ShortcutCard({ title, value, description, tone, href, cta }: AdminWorkspaceShortcut) {
  return (
    <a className={`shortcut-card tone-${tone}`} href={href}>
      <span>{title}</span>
      <strong>{value}</strong>
      <p>{description}</p>
      <em>{cta}</em>
    </a>
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

function Badge({ label, tone }: { label: string; tone: 'primary' | 'neutral' | 'success' }) {
  return <span className={`badge badge-${tone}`}>{label}</span>
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

function ToastStack({ items, tone = 'neutral' }: { items: string[]; tone?: 'neutral' | 'success' | 'warning' }) {
  return (
    <div className={`toast-stack tone-${tone}`}>
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
  return (
    <div className="dialog-backdrop">
      <div className={`dialog-card tone-${tone}`}>
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

function DataTable({ headers, rows, emptyText }: { headers: string[]; rows?: Array<Array<React.ReactNode>>; emptyText: string }) {
  return (
    <div className="table-shell">
      <table>
        <thead>
          <tr>
            {headers.map((header) => <th key={header}>{header}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows?.length ? rows.map((row, index) => (
            <tr key={`${row[0]}-${index}`}>
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

function FingerprintCell({ fingerprint, onCopy }: { fingerprint: string; onCopy: (fingerprint: string) => void | Promise<void> }) {
  return (
    <div className="fingerprint-cell">
      <code className="truncated-code" title={fingerprint}>{fingerprint}</code>
      <button className="ghost-btn small-btn fingerprint-copy-btn" onClick={() => void onCopy(fingerprint)} type="button">复制</button>
    </div>
  )
}

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
      navInvite: '生成我的邀请码',
      navEarnings: '查看我的人收益',
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
  const [product, setProduct] = useState('linky')
  const [form, setForm] = useState({
    inviteCode: initialInviteCode,
    whatsappNumber: '',
    linkyAccount: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<InviteBindingResponse | null>(null)

  const copy = copyByLocale[locale]
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
    <div className="page-shell bind-page-shell">
      <section className="bind-canvas">
        <div className="bind-backdrop bind-backdrop-left" />
        <div className="bind-backdrop bind-backdrop-right" />

        <header className="bind-topbar">
          <div>
            <p className="bind-kicker">{copy.kicker}</p>
            <strong className="bind-brand">Referral Hub</strong>
          </div>
          <div className="bind-topbar-controls">
            <label className="bind-select-group bind-select-inline topbar-pill-control">
              <select aria-label={copy.languageLabel} value={locale} onChange={(event) => setLocale(event.target.value as keyof typeof copyByLocale)}>
                <option value="zh">中文</option>
                <option value="en">English</option>
                <option value="es">Español</option>
                <option value="id">Bahasa Indonesia</option>
                <option value="pt">Português</option>
              </select>
            </label>
          </div>
        </header>

        <nav className="entry-link-bar">
          <a className="entry-link active topbar-pill-control" href="/bind">{copy.navBind}</a>
          <a className="entry-link topbar-pill-control" href="/invite">{copy.navInvite}</a>
          <a className="entry-link topbar-pill-control" href="/earnings">{copy.navEarnings}</a>
        </nav>

        <div className="bind-stage">
          <div className="bind-copy-zone">
            <h1>{copy.heroTitle}</h1>
            <p className="bind-hero-text">{copy.heroSubtitle}</p>
          </div>

          <div className="bind-form-zone">
            <div className="bind-form-head">
              <h2>{copy.formTitle}</h2>
            </div>

            {error ? (
              <section className="bind-inline-banner bind-inline-banner-error">
                <strong>{copy.failure}</strong>
                <span>{error}</span>
              </section>
            ) : result ? (
              <section className="bind-inline-banner bind-inline-banner-success">
                <strong>{copy.success}</strong>
                <span>{copy.successText}</span>
              </section>
            ) : null}

            <form className="bind-form" onSubmit={handleSubmit}>
              <div className="bind-field-grid bind-field-grid-top">
                <label className="bind-input-group">
                  <span>{copy.productLabel}</span>
                  <select value={product} onChange={(event) => setProduct(event.target.value)}>
                    <option value="linky">Linky</option>
                  </select>
                </label>
                <label className="bind-input-group">
                  <span>{copy.whatsappNumber}</span>
                  <input
                    value={form.whatsappNumber}
                    onChange={(e) => setForm({ ...form, whatsappNumber: e.target.value })}
                    placeholder={copy.whatsappPlaceholder}
                  />
                </label>
              </div>

              <div className="bind-field-grid">
                <label className="bind-input-group">
                  <span>{copy.linkyAccount}</span>
                  <input
                    value={form.linkyAccount}
                    onChange={(e) => setForm({ ...form, linkyAccount: e.target.value.replace(/\D/g, '').slice(0, 8) })}
                    placeholder={copy.linkyPlaceholder}
                    inputMode="numeric"
                  />
                </label>
                <label className="bind-input-group">
                  <span>{copy.inviteCode}</span>
                  <input
                    value={form.inviteCode}
                    onChange={(e) => setForm({ ...form, inviteCode: e.target.value.toUpperCase() })}
                    placeholder={copy.inviteCodePlaceholder}
                  />
                </label>
              </div>

              <button className="bind-submit-btn" type="submit" disabled={loading || !canSubmit}>
                {loading ? copy.submitting : copy.submit}
              </button>
            </form>

          </div>
        </div>

        {result ? (
          <section className="bind-result-strip">
            <div className="bind-result-head">
              <p className="bind-kicker">{copy.resultTitle}</p>
              <h3>{copy.resultWritten}</h3>
            </div>
            <div className="bind-result-grid">
              <div className="bind-result-item"><span>{copy.inviteCode}</span><strong>{result.inviteCode}</strong></div>
              <div className="bind-result-item"><span>{copy.inviterUserId}</span><strong>{result.inviterUserId}</strong></div>
              <div className="bind-result-item"><span>{copy.whatsappNumber}</span><strong>{result.whatsappNumber}</strong></div>
              <div className="bind-result-item"><span>{copy.linkyAccount}</span><strong>{result.linkyAccount}</strong></div>
              <div className="bind-result-item"><span>{copy.status}</span><strong>{result.bindStatus}</strong></div>
            </div>
          </section>
        ) : null}
      </section>
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

const externalPageCopyByLocale = {
  zh: {
    navBind: '绑定页',
    navInvite: '生成我的邀请码',
    navEarnings: '查看我的人收益',
    languageLabel: '语言',
    inviteKicker: 'INVITE CODE ENTRY',
    inviteTitle: '生成我的邀请码',
    inviteSubtitle: '填完信息，马上生成并复制你的邀请码。',
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
    earningsTitle: '查看我的人收益',
    earningsSubtitle: '看两部分：被邀请人的收益 + 你的提成。',
    boardTitle: '你的收益会在这里持续更新',
    boardSubtitle: '从邀请码、绑定到奖励到账，这一页会持续帮你看清进度。',
    boardBadgeCode: '邀请码固定不变',
    boardBadgeBind: '绑定后自动累计',
    boardBadgeStatus: '到账状态一目了然',
    noSession: '还没有用户会话，请先去“生成我的邀请码”页面生成邀请码。',
    noSessionTitle: '先生成你的邀请码',
    noSessionHint: '还没开始邀请也没关系。先生成邀请码，再去完成绑定，收益会自动累计到这里。',
    noSessionPrimary: '去生成我的邀请码',
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
    totalReward: '总奖励',
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
  const [form, setForm] = useState({
    productCode: 'linky',
    whatsappNumber: '',
    appAccount: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [issued, setIssued] = useState<IssueInviteCodeResponse | null>(null)
  const copy = externalPageCopyByLocale[locale]

  useEffect(() => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(EXTERNAL_LOCALE_KEY, locale)
    }
  }, [locale])

  async function handleGenerateInviteCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setSuccess('')
    try {
      const response = await issueInviteCode(form)
      const nextSession: SessionState = {
        userId: response.userId,
        inviteCode: response.inviteCode,
        countryCode: response.countryCode,
        languageCode: response.languageCode,
        accessToken: response.accessToken,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession))
      setSession(nextSession)
      setIssued(response)
      setSuccess(copy.issueSuccess)
    } catch (err) {
      setError(err instanceof Error ? err.message : copy.issueFailure)
    } finally {
      setLoading(false)
    }
  }

  async function handleCopyInviteCode() {
    const inviteCode = issued?.inviteCode || session?.inviteCode
    if (!inviteCode) return
    try {
      await navigator.clipboard.writeText(inviteCode)
      setSuccess(copy.copySuccess)
      setError('')
    } catch {
      setError(copy.copyFailure)
    }
  }

  return (
    <div className="page-shell route-page-shell">
      <section className="route-surface">
        <header className="route-header">
          <div>
            <p className="route-kicker">{copy.inviteKicker}</p>
            <h1>{copy.inviteTitle}</h1>
            <p>{copy.inviteSubtitle}</p>
          </div>
          <div className="route-nav-links">
            <label className="bind-select-group route-select-group route-select-inline topbar-pill-control">
              <select aria-label={copy.languageLabel} value={locale} onChange={(event) => setLocale(event.target.value as keyof typeof externalPageCopyByLocale)}>
                <option value="zh">中文</option>
                <option value="en">English</option>
                <option value="es">Español</option>
                <option value="id">Bahasa Indonesia</option>
                <option value="pt">Português</option>
              </select>
            </label>
            <a className="topbar-pill-control" href="/bind">{copy.navBind}</a>
            <a className="topbar-pill-control active" href="/invite">{copy.navInvite}</a>
            <a className="topbar-pill-control" href="/earnings">{copy.navEarnings}</a>
          </div>
        </header>

        {error ? <div className="route-banner route-banner-error">{error}</div> : null}
        {success ? <div className="route-banner route-banner-success">{success}</div> : null}

        <div className="route-grid two-columns-route">
          <form className="route-panel route-form" onSubmit={handleGenerateInviteCode}>
            <div className="route-field-grid">
              <label>
                {copy.productLabel}
                <select value={form.productCode} onChange={(e) => setForm({ ...form, productCode: e.target.value })}>
                  <option value="linky">Linky</option>
                </select>
              </label>
              <label>
                {copy.whatsappLabel}
                <input value={form.whatsappNumber} onChange={(e) => setForm({ ...form, whatsappNumber: e.target.value })} placeholder="ex. +6281234567890" />
              </label>
            </div>
            <div className="route-field-grid single-field-route">
              <label>
                {copy.appAccountLabel}
                <input value={form.appAccount} onChange={(e) => setForm({ ...form, appAccount: e.target.value.replace(/\D/g, '').slice(0, 8) })} placeholder="ex. 12345678" />
              </label>
            </div>
            <button className="primary-btn route-primary-btn" type="submit" disabled={loading || !form.whatsappNumber.trim() || form.appAccount.length !== 8}>
              {loading ? copy.generating : copy.generateButton}
            </button>
          </form>

          <div className="route-panel">
            <p className="route-label">{copy.myInviteCode}</p>
            <h2>{issued?.inviteCode || session?.inviteCode || '--'}</h2>
            <div className="route-detail-list compact">
              <div><span>{copy.productLabel}</span><strong>{issued?.productCode || form.productCode.toUpperCase()}</strong></div>
              <div><span>{copy.whatsappLabel}</span><strong>{issued?.whatsappNumber || '--'}</strong></div>
              <div><span>{copy.appAccountLabel}</span><strong>{issued?.appAccount || '--'}</strong></div>
            </div>
            <div className="route-action-row">
              <button className="primary-btn" type="button" onClick={handleCopyInviteCode} disabled={!(issued?.inviteCode || session?.inviteCode)}>{copy.copyInviteCode}</button>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}

function EarningsPage() {
  const [session] = useState<SessionState | null>(() => loadJsonState<SessionState>(STORAGE_KEY))
  const [locale, setLocale] = useState<keyof typeof externalPageCopyByLocale>(() => loadExternalLocale())
  const [home, setHome] = useState<DistributionHomeResponse | null>(null)
  const [team, setTeam] = useState<TeamListResponse | null>(null)
  const [rewards, setRewards] = useState<RewardListResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
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
        const [homeData, teamData, rewardData] = await Promise.all([
          getDistributionHome(session.userId, session.accessToken),
          getDistributionTeam(session.userId, session.accessToken),
          getDistributionRewards(session.userId, session.accessToken),
        ])
        setHome(homeData)
        setTeam(teamData)
        setRewards(rewardData)
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载收益失败')
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [session])

  function getRewardStatusLabel(status?: string) {
    if (status === 'FROZEN') return copy.rewardStatusFrozen
    if (status === 'AVAILABLE') return copy.rewardStatusAvailable
    if (status === 'RISK_HOLD') return copy.rewardStatusRiskHold
    return copy.rewardStatusDefault
  }

  const inviteeIncome = team?.items.reduce((sum, item) => sum + item.confirmedIncomeTotal, 0) ?? 0
  const myCommission = rewards?.items.reduce((sum, item) => sum + item.rewardAmount, 0) ?? 0
  const rewardItems = rewards?.items ?? []

  return (
    <div className="page-shell route-page-shell">
      <section className="route-surface">
        <header className="route-header">
          <div>
            <p className="route-kicker">{copy.earningsKicker}</p>
            <h1>{copy.earningsTitle}</h1>
            <p>{copy.earningsSubtitle}</p>
          </div>
          <div className="route-nav-links">
            <label className="bind-select-group route-select-group route-select-inline topbar-pill-control">
              <select aria-label={copy.languageLabel} value={locale} onChange={(event) => setLocale(event.target.value as keyof typeof externalPageCopyByLocale)}>
                <option value="zh">中文</option>
                <option value="en">English</option>
                <option value="es">Español</option>
                <option value="id">Bahasa Indonesia</option>
                <option value="pt">Português</option>
              </select>
            </label>
            <a className="topbar-pill-control" href="/bind">{copy.navBind}</a>
            <a className="topbar-pill-control" href="/invite">{copy.navInvite}</a>
            <a className="topbar-pill-control active" href="/earnings">{copy.navEarnings}</a>
          </div>
        </header>

        {error ? <div className="route-banner route-banner-error">{error}</div> : null}

        <div className="route-panel route-board-panel top-gap-xl">
          <p className="route-label">{copy.earningsOverview}</p>
          <h2>{copy.boardTitle}</h2>
          <p className="route-caption route-board-copy">{copy.boardSubtitle}</p>
          <div className="route-badge-row">
            <span className="route-board-badge">{copy.boardBadgeCode}</span>
            <span className="route-board-badge">{copy.boardBadgeBind}</span>
            <span className="route-board-badge">{copy.boardBadgeStatus}</span>
          </div>
        </div>

        {!session ? (
          <div className="route-panel route-onboarding-panel top-gap-xl">
            <p className="route-label">{copy.noSessionTitle}</p>
            <h2>{copy.noSessionTitle}</h2>
            <p className="route-caption route-onboarding-copy">{copy.noSessionHint}</p>
            <div className="route-action-row">
              <a className="primary-btn" href="/invite">{copy.noSessionPrimary}</a>
              <a className="ghost-btn" href="/bind">{copy.noSessionSecondary}</a>
            </div>
          </div>
        ) : null}

        <div className="route-grid three-columns-route top-gap-xl">
          <div className="route-panel route-metric-panel">
            <div className="route-metric-head">
              <p className="route-label">{copy.inviteeIncome}</p>
              <span className="route-metric-pill">{copy.inviteeIncomeBadge}</span>
            </div>
            <h2>{formatMoney(inviteeIncome)}</h2>
            <p className="route-caption">{copy.inviteeIncomeHint}</p>
          </div>
          <div className="route-panel route-metric-panel">
            <div className="route-metric-head">
              <p className="route-label">{copy.myCommission}</p>
              <span className="route-metric-pill">{copy.myCommissionBadge}</span>
            </div>
            <h2>{formatMoney(myCommission)}</h2>
            <p className="route-caption">{copy.myCommissionHint}</p>
          </div>
          <div className="route-panel route-metric-panel">
            <div className="route-metric-head">
              <p className="route-label">{copy.availableReward}</p>
              <span className="route-metric-pill">{copy.availableRewardBadge}</span>
            </div>
            <h2>{formatMoney(home?.availableReward)}</h2>
            <p className="route-caption">{copy.availableRewardHint}</p>
          </div>
        </div>

        <div className="route-grid two-columns-route top-gap-xl">
          <div className="route-panel route-detail-card">
            <div className="route-detail-card-head">
              <div>
                <p className="route-label">{copy.earningsOverview}</p>
                <h3 className="route-section-title">{copy.overviewCardTitle}</h3>
              </div>
              <span className="route-detail-badge">{copy.overviewBadge}</span>
            </div>
            <div className="route-detail-list compact">
              <div><span>{copy.inviteCode}</span><strong>{home?.inviteCode || session?.inviteCode || '--'}</strong></div>
              <div><span>{copy.invitedUsers}</span><strong>{home?.invitedUsers ?? '--'}</strong></div>
              <div><span>{copy.effectiveUsers}</span><strong>{home?.effectiveUsers ?? '--'}</strong></div>
              <div><span>{copy.totalReward}</span><strong>{formatMoney(home?.totalReward)}</strong></div>
            </div>
          </div>

          <div className="route-panel route-detail-card">
            <div className="route-detail-card-head">
              <div>
                <p className="route-label">{copy.progressTitle}</p>
                <h3 className="route-section-title">{copy.progressCardTitle}</h3>
              </div>
              <span className="route-detail-badge">{copy.progressBadge}</span>
            </div>
            <div className="route-detail-list compact">
              <div><span>{copy.frozenReward}</span><strong>{formatMoney(home?.frozenReward)}</strong></div>
              <div><span>{copy.availableReward}</span><strong>{formatMoney(home?.availableReward)}</strong></div>
              <div><span>{copy.riskHoldReward}</span><strong>{formatMoney(home?.riskHoldReward)}</strong></div>
            </div>
            <p className="route-caption route-panel-copy">{copy.progressHint}</p>
          </div>
        </div>

        <div className="route-grid two-columns-route top-gap-xl">
          <div className="route-panel">
            <p className="route-label">{copy.rewardRecords}</p>
            <h3 className="route-section-title">{copy.rewardActivityTitle}</h3>
            <p className="route-caption route-panel-copy">{copy.rewardActivityHint}</p>
            <div className="route-status-guide">
              <strong>{copy.rewardStatusGuideTitle}</strong>
              <ul>
                <li>{copy.rewardStatusGuideFrozen}</li>
                <li>{copy.rewardStatusGuideAvailable}</li>
                <li>{copy.rewardStatusGuideRiskHold}</li>
              </ul>
            </div>
            {loading ? <p className="route-caption">{copy.loading}</p> : rewardItems.length ? (
              <div className="route-record-list">
                {rewardItems.slice(0, 6).map((item, index) => (
                  <div className="route-record-item" key={`${item.sourceUserId}-${item.calculatedAt}-${index}`}>
                    <div className="route-record-topline">
                      <strong>用户 #{item.sourceUserId}</strong>
                      <span className={`route-status-pill status-${item.rewardStatus.toLowerCase()}`}>{getRewardStatusLabel(item.rewardStatus)}</span>
                    </div>
                    <span>{copy.rewardLine} {item.rewardLevel} · {copy.commissionTail} {formatMoney(item.rewardAmount)}</span>
                    <em>{formatDateTime(item.calculatedAt)}</em>
                  </div>
                ))}
              </div>
            ) : (
              <div className="route-empty-state">
                <strong>{copy.emptyRewardsTitle}</strong>
                <p>{copy.emptyRewardsHint}</p>
                <a className="ghost-btn" href="/invite">{copy.emptyRewardsAction}</a>
              </div>
            )}
          </div>

          <div className="route-panel route-detail-card">
            <div className="route-detail-card-head">
              <div>
                <p className="route-label">{copy.settlementTitle}</p>
                <h3 className="route-section-title">{copy.settlementCardTitle}</h3>
              </div>
              <span className="route-detail-badge">{copy.settlementBadge}</span>
            </div>
            <div className="route-detail-list compact">
              <div><span>{copy.totalReward}</span><strong>{formatMoney(home?.totalReward)}</strong></div>
              <div><span>{copy.frozenReward}</span><strong>{formatMoney(home?.frozenReward)}</strong></div>
              <div><span>{copy.availableReward}</span><strong>{formatMoney(home?.availableReward)}</strong></div>
              <div><span>{copy.riskHoldReward}</span><strong>{formatMoney(home?.riskHoldReward)}</strong></div>
            </div>
            <p className="route-caption route-panel-copy">{copy.settlementHint}</p>
          </div>
        </div>

        <div className="route-panel top-gap-xl">
          <p className="route-label">{copy.nextStepsTitle}</p>
          <div className="route-action-row">
            <a className="primary-btn" href="/invite">{copy.nextInvite}</a>
            <a className="ghost-btn" href="/bind">{copy.nextBind}</a>
          </div>
        </div>
      </section>
    </div>
  )
}

function App() {
  const pathname = typeof window !== 'undefined' ? window.location.pathname : '/'
  if (pathname.startsWith('/bind')) return <BindLandingPage />
  if (pathname.startsWith('/invite')) return <InviteCodePage />
  if (pathname.startsWith('/earnings')) return <EarningsPage />
  return <ConsoleApp />
}

export { ConsoleApp }
export default App
