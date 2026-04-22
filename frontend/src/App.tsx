import { useEffect, useMemo, useState } from 'react'
import './App.css'
import {
  adjustAdminRelation,
  applyAdminRiskEventAction,
  createAdminSession,
  createProfile,
  getAdminAuditLogs,
  getAdminOverview,
  getAdminRelation,
  getAdminRewards,
  getAdminRiskEvents,
  getDistributionHome,
  getDistributionRewards,
  getDistributionTeam,
  type AuditLogListResponse,
  type DistributionHomeResponse,
  type OverviewReportResponse,
  type ProfileResponse,
  type RelationDetailResponse,
  type RewardListResponse,
  type RiskEventListResponse,
  type TeamListResponse,
} from './api'

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

type ViewMode = 'user' | 'admin'
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

const STORAGE_KEY = 'fenxiao-web-session'
const PROFILE_CREATE_TOKEN_KEY = 'fenxiao-profile-create-token'
const ADMIN_REWARD_QUERY_KEY = 'fenxiao-admin-reward-query'
const RISK_QUERY_KEY = 'fenxiao-admin-risk-query'

function loadJsonState<T>(key: string): T | null {
  const raw = localStorage.getItem(key)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
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

function App() {
  const [session, setSession] = useState<SessionState | null>(() => loadJsonState<SessionState>(STORAGE_KEY))
  const [adminSession, setAdminSession] = useState<AdminAuthState | null>(null)
  const [viewMode, setViewMode] = useState<ViewMode>('user')
  const [adminPassword, setAdminPassword] = useState('')
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
  const [adminRelation, setAdminRelation] = useState<RelationDetailResponse | null>(null)
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
  const [auditQuery, setAuditQuery] = useState({
    moduleName: 'risk_event',
    page: '0',
    size: '5',
  })
  const [riskActionDrafts, setRiskActionDrafts] = useState<Record<number, string>>({})
  const [pendingRiskAction, setPendingRiskAction] = useState<PendingRiskAction | null>(null)
  const [riskActionLoadingId, setRiskActionLoadingId] = useState<number | null>(null)
  const [relationQueryUserId, setRelationQueryUserId] = useState('')
  const [relationAdjustInviterId, setRelationAdjustInviterId] = useState('')
  const [relationAdjustNote, setRelationAdjustNote] = useState('')
  const [relationBeforeAdjust, setRelationBeforeAdjust] = useState<RelationDetailResponse | null>(null)
  const [pendingRelationChange, setPendingRelationChange] = useState<PendingRelationChange | null>(null)
  const [relationAdjustLoading, setRelationAdjustLoading] = useState(false)
  const [profileCreateToken, setProfileCreateToken] = useState(() => localStorage.getItem(PROFILE_CREATE_TOKEN_KEY) || '')

  const canLoadData = useMemo(() => Boolean(session?.userId && session?.accessToken), [session])
  const canLoadAdmin = useMemo(() => Boolean(adminSession?.sessionToken), [adminSession])
  const canCreateProfile = useMemo(() => Boolean(profileCreateToken.trim() && form.userId.trim()), [profileCreateToken, form.userId])

  useEffect(() => {
    localStorage.setItem(ADMIN_REWARD_QUERY_KEY, JSON.stringify(adminRewardQuery))
  }, [adminRewardQuery])

  useEffect(() => {
    localStorage.setItem(RISK_QUERY_KEY, JSON.stringify(riskQuery))
  }, [riskQuery])

  async function loadAdminRewards(query = adminRewardQuery) {
    if (!adminSession) return
    setLoading(true)
    setError('')
    try {
      const result = await getAdminRewards(adminSession.sessionToken, {
        beneficiaryUserId: query.beneficiaryUserId ? Number(query.beneficiaryUserId) : undefined,
        status: query.status || undefined,
        startAt: query.startAt || undefined,
        endAt: query.endAt || undefined,
        page: Number(query.page || 0),
        size: Number(query.size || 10),
      })
      setAdminRewards(result)
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
        startAt: query.startAt || undefined,
        endAt: query.endAt || undefined,
        page: Number(query.page || 0),
        size: Number(query.size || 10),
      })
      setRiskEvents(result)
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
      setViewMode('user')
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
      setViewMode('admin')
      const [overviewResult, auditResult] = await Promise.all([
        getAdminOverview(nextSession.sessionToken),
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
    setPendingRiskAction(null)
    setPendingRelationChange(null)
    setRiskActionDrafts({})
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
      const overview = await getAdminOverview(adminSession.sessionToken)
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
      const relation = await getAdminRelation(adminSession.sessionToken, Number(relationQueryUserId))
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

  async function handleAdjustRelation() {
    if (!adminSession || !adminRelation) return
    setRelationAdjustLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const updated = await adjustAdminRelation(adminSession.sessionToken, adminRelation.userId, {
        level1InviterId: relationAdjustInviterId.trim() ? Number(relationAdjustInviterId) : undefined,
        note: relationAdjustNote.trim() || undefined,
      })
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

  const userSummaryItems = [
    {
      label: '当前身份',
      value: session ? `用户 #${session.userId}` : '未接入',
      hint: session ? `邀请码 ${session.inviteCode}` : '先创建或接入分销档案',
    },
    {
      label: '国家 / 语言',
      value: session ? `${session.countryCode} / ${session.languageCode}` : '-',
      hint: '用于规则匹配与前台展示',
    },
    {
      label: '数据状态',
      value: home ? '已同步' : '待加载',
      hint: home ? '前台工作台已拿到最新数据' : '点击“刷新用户工作台”拉取最新数据',
    },
  ]

  const adminSummaryItems = [
    {
      label: '运营登录状态',
      value: adminSession ? '已登录' : '未登录',
      hint: adminSession ? `到期 ${formatDateTime(adminSession.expiresAt)}` : '使用后台登录口令进入运营台',
    },
    {
      label: '概览数据',
      value: adminOverview ? '已加载' : '未加载',
      hint: adminOverview ? '报表已获取，可继续查奖励、风险和关系' : '先拉取运营概览',
    },
    {
      label: '安全模式',
      value: 'Session Only',
      hint: '后台接口不再接受直接 Admin Token 访问',
    },
  ]

  const rewardPageLabel = adminRewards
    ? `本次命中 ${adminRewards.total} 条，当前第 ${adminRewards.page + 1} 页，每页 ${adminRewards.size} 条。`
    : '先执行一次奖励查询'
  const riskPageLabel = riskEvents
    ? `本次命中 ${riskEvents.total} 条，当前第 ${riskEvents.page + 1} 页，每页 ${riskEvents.size} 条。`
    : '先执行一次风险事件查询'
  const hasRewardPrevPage = Number(adminRewardQuery.page) > 0
  const hasRewardNextPage = adminRewards ? (adminRewards.page + 1) * adminRewards.size < adminRewards.total : false
  const hasRiskPrevPage = Number(riskQuery.page) > 0
  const hasRiskNextPage = riskEvents ? (riskEvents.page + 1) * riskEvents.size < riskEvents.total : false
  const relationPreview = adminRelation
    ? buildRelationPreview(adminRelation, relationAdjustInviterId)
    : null

  return (
    <div className="page-shell">
      <header className="hero-card">
        <div className="hero-copy">
          <div className="hero-badges">
            <Badge label="Fenxiao" tone="primary" />
            <Badge label="Web Console" tone="neutral" />
            <Badge label="MVP+" tone="success" />
          </div>
          <p className="eyebrow">Distribution Console</p>
          <h1>三级分销产品控制台</h1>
          <p className="subtext">
            这版不再只是接口演示页，而是把「用户工作台」和「运营工作台」拆清楚，方便你继续往产品化、可运营、可上线方向推进。
          </p>
        </div>
        <div className="hero-actions">
          <button className={`switch-chip ${viewMode === 'user' ? 'active' : ''}`} onClick={() => setViewMode('user')}>用户工作台</button>
          <button className={`switch-chip ${viewMode === 'admin' ? 'active' : ''}`} onClick={() => setViewMode('admin')}>运营工作台</button>
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
          <span>{viewMode === 'user' ? '先完成用户接入，再刷新用户工作台。' : '先登录后台，再拉取运营概览和查询数据。'}</span>
        </section>
      )}

      <section className="overview-grid">
        {viewMode === 'user'
          ? userSummaryItems.map((item) => <SummaryCard key={item.label} {...item} />)
          : adminSummaryItems.map((item) => <SummaryCard key={item.label} {...item} />)}
      </section>

      <div className="console-layout">
        <main className="console-main">
          {viewMode === 'user' ? (
            <>
              <PanelSection
                eyebrow="Onboarding"
                title="分销接入"
                description="先保存 profile 创建令牌，再创建或接入用户分销档案。"
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
                    <InfoCard title="已接入用户会话" tone="success">
                      <InfoRow label="用户 ID" value={session.userId} />
                      <InfoRow label="邀请码" value={session.inviteCode} />
                      <InfoRow label="国家 / 语言" value={`${session.countryCode} / ${session.languageCode}`} />
                      <InfoRow label="Access Token" value={session.accessToken} code />
                    </InfoCard>
                  ) : (
                    <EmptyState
                      title="还没有用户会话"
                      description="完成一次接入后，这里会显示当前用户身份、邀请码和令牌信息。"
                    />
                  )}
                </div>
              </PanelSection>

              <PanelSection
                eyebrow="Workspace"
                title="用户工作台"
                description="集中看邀请规模、有效用户和奖励状态。"
                action={<button className="primary-btn" onClick={handleLoadDashboard} disabled={!canLoadData || loading}>刷新用户工作台</button>}
              >
                <div className="stats-grid">
                  <Metric label="邀请人数" value={home?.invitedUsers} hint="直属与裂变邀请总量" tone="neutral" />
                  <Metric label="有效用户" value={home?.effectiveUsers} hint="已满足锁定条件的用户" tone="success" />
                  <Metric label="总奖励" value={home?.totalReward} hint="累计奖励规模" tone="primary" />
                  <Metric label="可用奖励" value={home?.availableReward} hint="可进入后续提现流程" tone="success" />
                  <Metric label="冻结奖励" value={home?.frozenReward} hint="仍在冻结周期内" tone="warning" />
                  <Metric label="风险冻结" value={home?.riskHoldReward} hint="因风控暂时冻结" tone="danger" />
                </div>
              </PanelSection>

              <div className="content-grid two-columns">
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
                    <EmptyState title="暂无直属团队数据" description="创建用户后并产生下级绑定，这里会出现一度团队成员。" />
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
                    <EmptyState title="暂无奖励记录" description="当用户链路产生收益事件后，这里会显示对应奖励明细。" />
                  )}
                </PanelSection>
              </div>
            </>
          ) : (
            <>
              <PanelSection
                eyebrow="Security"
                title="运营登录"
                description="后台已经改成 session-only，登录后才能访问报表、奖励和关系链。"
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
                eyebrow="Operations"
                title="运营概览"
                description="给运营或风控一个全局入口，先看规模，再决定看奖励、风险或查关系。"
                action={<button className="primary-btn" onClick={handleLoadAdminOverview} disabled={loading || !canLoadAdmin}>拉取运营概览</button>}
              >
                <div className="stats-grid">
                  <Metric label="累计邀请用户" value={adminOverview?.invitedUsers} hint="系统范围总邀请量" tone="neutral" />
                  <Metric label="有效用户数" value={adminOverview?.effectiveUsers} hint="已锁定有效关系用户" tone="success" />
                  <Metric label="奖励总额" value={adminOverview?.rewardTotal} hint="累计奖励总额" tone="primary" />
                  <Metric label="冻结奖励" value={adminOverview?.frozenRewardTotal} hint="待解冻奖励池" tone="warning" />
                  <Metric label="可用奖励" value={adminOverview?.availableRewardTotal} hint="当前已可用奖励" tone="success" />
                  <Metric label="风险事件数" value={adminOverview?.riskEventCount} hint="已被风控标记的事件量" tone="danger" />
                </div>
              </PanelSection>

              <div className="content-grid two-columns">
                <PanelSection
                  eyebrow="Query"
                  title="奖励筛选"
                  description="按受益用户、状态、时间区间分页查奖励，适合运营排查。"
                  action={<button className="primary-btn" onClick={handleLoadAdminRewards} disabled={loading || !canLoadAdmin}>查询奖励</button>}
                >
                  <div className="grid-form compact-form">
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
                    <label>
                      开始时间
                      <input type="datetime-local" value={adminRewardQuery.startAt} onChange={(e) => setAdminRewardQuery({ ...adminRewardQuery, startAt: e.target.value })} />
                    </label>
                    <label>
                      结束时间
                      <input type="datetime-local" value={adminRewardQuery.endAt} onChange={(e) => setAdminRewardQuery({ ...adminRewardQuery, endAt: e.target.value })} />
                    </label>
                    <label>
                      页码
                      <input type="number" min="0" value={adminRewardQuery.page} onChange={(e) => setAdminRewardQuery({ ...adminRewardQuery, page: e.target.value })} placeholder="0" />
                    </label>
                    <label>
                      每页条数
                      <input type="number" min="1" max="100" value={adminRewardQuery.size} onChange={(e) => setAdminRewardQuery({ ...adminRewardQuery, size: e.target.value })} placeholder="10" />
                    </label>
                  </div>
                  <InlineHint text={rewardPageLabel} />
                  <div className="table-toolbar">
                    <button className="ghost-btn small-btn" onClick={() => handleAdminRewardPageChange(Number(adminRewardQuery.page) - 1)} disabled={loading || !hasRewardPrevPage}>上一页</button>
                    <button className="ghost-btn small-btn" onClick={() => handleAdminRewardPageChange(Number(adminRewardQuery.page) + 1)} disabled={loading || !hasRewardNextPage}>下一页</button>
                  </div>
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
                    <EmptyState title="暂无奖励查询结果" description="先登录后台并执行一次筛选查询，结果会显示在这里。" />
                  )}
                </PanelSection>

                <PanelSection
                  eyebrow="Relation"
                  title="关系链查询"
                  description="适合运营、客服或风控定位单个用户的上下游关系。"
                  action={<button className="primary-btn" onClick={handleLoadRelation} disabled={loading || !relationQueryUserId || !canLoadAdmin}>查询关系链</button>}
                >
                  <div className="grid-form compact-form single-line">
                    <label>
                      用户 ID
                      <input value={relationQueryUserId} onChange={(e) => setRelationQueryUserId(e.target.value)} placeholder="例如 10003" />
                    </label>
                  </div>
                  {adminRelation ? (
                    <>
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
                      <div className="content-grid two-columns nested-grid">
                        <InfoCard title="当前关系快照" tone="neutral">
                          <InfoRow label="当前一级上级" value={adminRelation.level1InviterId ?? '-'} />
                          <InfoRow label="当前二级上级" value={adminRelation.level2InviterId ?? '-'} />
                          <InfoRow label="当前三级上级" value={adminRelation.level3InviterId ?? '-'} />
                          <InfoRow label="当前来源" value={adminRelation.bindSource} />
                        </InfoCard>
                        <InfoCard title="修正后预览" tone="success">
                          <InfoRow label="修正后一级上级" value={relationPreview?.nextLevel1InviterId ?? '-'} />
                          <InfoRow label="修正后来源" value={relationPreview?.nextBindSource ?? 'MANUAL'} />
                          <InfoRow label="变更说明" value={relationPreview?.summary ?? '保持当前关系'} />
                          <InfoRow label="操作建议" value={adminRelation.lockStatus === 'LOCKED' ? '当前关系已锁定，不能手工修改。' : '先确认预览，再提交人工修正。'} />
                        </InfoCard>
                      </div>
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
                      <InlineHint text="支持人工修正一级上级。提交前先看预览；留空后保存会把该用户改成根关系；如果关系已经锁定，系统会禁止手工改动。" />
                      <div className="table-toolbar">
                        <button className="primary-btn small-btn" onClick={openRelationAdjustConfirm} disabled={relationAdjustLoading || !canLoadAdmin || adminRelation.lockStatus === 'LOCKED'}>预览后确认修正</button>
                        <button className="ghost-btn small-btn" onClick={() => setRelationAdjustInviterId('')} disabled={relationAdjustLoading}>设为根关系</button>
                      </div>
                    </>
                  ) : (
                    <EmptyState title="暂无关系链结果" description="输入用户 ID 后查询，这里会显示完整三级关系、锁定状态和人工修正预览。" />
                  )}
                </PanelSection>
              </div>

              <PanelSection
                eyebrow="Risk"
                title="风险事件运营页"
                description="按用户、状态、时间区间分页查看风险事件，快速定位风险冻结来源。"
                action={<button className="primary-btn" onClick={handleLoadRiskEvents} disabled={loading || !canLoadAdmin}>查询风险事件</button>}
              >
                <div className="grid-form compact-form">
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
                  <label>
                    开始时间
                    <input type="datetime-local" value={riskQuery.startAt} onChange={(e) => setRiskQuery({ ...riskQuery, startAt: e.target.value })} />
                  </label>
                  <label>
                    结束时间
                    <input type="datetime-local" value={riskQuery.endAt} onChange={(e) => setRiskQuery({ ...riskQuery, endAt: e.target.value })} />
                  </label>
                  <label>
                    页码
                    <input type="number" min="0" value={riskQuery.page} onChange={(e) => setRiskQuery({ ...riskQuery, page: e.target.value })} placeholder="0" />
                  </label>
                  <label>
                    每页条数
                    <input type="number" min="1" max="100" value={riskQuery.size} onChange={(e) => setRiskQuery({ ...riskQuery, size: e.target.value })} placeholder="10" />
                  </label>
                </div>
                <InlineHint text="风险动作已升级成逐条备注 + 二次确认。先看事件，再对目标用户执行处理。" />
                <InlineHint text={riskPageLabel} />
                <div className="table-toolbar">
                  <button className="ghost-btn small-btn" onClick={() => handleRiskPageChange(Number(riskQuery.page) - 1)} disabled={loading || !hasRiskPrevPage}>上一页</button>
                  <button className="ghost-btn small-btn" onClick={() => handleRiskPageChange(Number(riskQuery.page) + 1)} disabled={loading || !hasRiskNextPage}>下一页</button>
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
                              <p>用户 #{item.userId} · {item.riskType} · {formatDateTime(item.detectedAt)}</p>
                            </div>
                            {renderStatusBadge(item.riskStatus)}
                          </div>
                          <div className="risk-event-meta">
                            <span>等级 {item.riskLevel}</span>
                            <span>处理信息：{item.handledAt ? `${formatDateTime(item.handledAt)} / #${item.handledBy ?? 0}` : '未处理'}</span>
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
                  <EmptyState title="暂无风险事件结果" description="这里会展示风控冻结、风险用户等运营需要跟进的事件。" />
                )}
              </PanelSection>

              <PanelSection
                eyebrow="Audit"
                title="最近处理记录"
                description="方便运营复盘最近做过哪些动作，确认谁在什么时间处理了什么问题。"
                action={<button className="primary-btn" onClick={() => loadAuditLogs(auditQuery)} disabled={!canLoadAdmin}>刷新处理记录</button>}
              >
                <div className="grid-form compact-form">
                  <label>
                    模块
                    <select value={auditQuery.moduleName} onChange={(e) => setAuditQuery({ ...auditQuery, moduleName: e.target.value })}>
                      <option value="risk_event">risk_event</option>
                      <option value="relation">relation</option>
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
                  <EmptyState title="暂无处理记录" description="执行一次风险处理动作后，这里会沉淀最近的操作审计。" />
                )}
              </PanelSection>
            </>
          )}
        </main>

        <aside className="console-side">
          <PanelSection eyebrow="Guide" title="本阶段产品优先级" description="继续把运营可用 MVP 打磨成更顺手的后台。">
            <Checklist
              items={[
                '风险动作改成逐条备注 + 二次确认，降低误操作',
                '关系链人工修正先预览 before / after，再落审计',
                '地址、健康检查、部署入口不再靠口头记忆',
                '文档、控制台和后端能力保持同一口径',
              ]}
            />
          </PanelSection>

          <PanelSection eyebrow="Access" title="当前环境入口" description="把常用地址和部署入口收在这里，减少来回翻 README。">
            <InfoCard title="本地 / 部署入口" tone="neutral">
              <InfoRow label="本地后端" value="http://localhost:8080" code />
              <InfoRow label="前端开发" value="http://localhost:5173" code />
              <InfoRow label="Docker 前端" value="http://localhost:8088" code />
              <InfoRow label="健康检查" value="http://localhost:8080/actuator/health" code />
              <InfoRow label="部署文件" value="deploy/docker-compose.yml" code />
            </InfoCard>
          </PanelSection>

          <PanelSection eyebrow="Next" title="下一批最值得补" description="这版收口后，继续往真实业务化推进。">
            <RoadmapList
              items={[
                { title: 'Linky webhook 日志落库', desc: '把签名、时间窗、原始 payload 和处理结果都留痕。' },
                { title: '关系修正审计增强', desc: '补更多 before / after 信息和复核说明。' },
                { title: '上线前 checklist', desc: '把部署、验证、交接收成真正可执行的清单。' },
                { title: '更细粒度权限', desc: '为后续审批流和多角色后台做准备。' },
              ]}
            />
          </PanelSection>
        </aside>
      </div>

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

function PanelSection({ eyebrow, title, description, action, children }: { eyebrow: string; title: string; description: string; action?: React.ReactNode; children: React.ReactNode }) {
  return (
    <section className="panel-card">
      <div className="panel-head">
        <div>
          <p className="panel-eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
          <p className="panel-desc">{description}</p>
        </div>
        {action ? <div className="panel-action">{action}</div> : null}
      </div>
      {children}
    </section>
  )
}

function SummaryCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{hint}</p>
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

function InfoRow({ label, value, code = false }: { label: string; value: string | number; code?: boolean }) {
  return (
    <div className="info-row">
      <span>{label}</span>
      {code ? <code>{value}</code> : <strong>{value}</strong>}
    </div>
  )
}

function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="empty-card">
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  )
}

function Checklist({ items }: { items: string[] }) {
  return (
    <ul className="checklist">
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
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

function InlineHint({ text }: { text: string }) {
  return <p className="inline-hint">{text}</p>
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
  const tone = status === 'HANDLED' || status === 'AVAILABLE'
    ? 'success'
    : status === 'IGNORED'
      ? 'neutral'
      : status === 'RISK_HOLD' || status === 'LOCKED'
        ? 'warning'
        : 'primary'
  return <span className={`badge badge-${tone}`}>{status}</span>
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

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`
}

export default App
