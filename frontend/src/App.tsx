import { useMemo, useState } from 'react'
import './App.css'
import {
  createAdminSession,
  createProfile,
  getAdminOverview,
  getAdminRelation,
  getAdminRewards,
  getDistributionHome,
  getDistributionRewards,
  getDistributionTeam,
  type DistributionHomeResponse,
  type OverviewReportResponse,
  type ProfileResponse,
  type RelationDetailResponse,
  type RewardListResponse,
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

const STORAGE_KEY = 'fenxiao-web-session'
const PROFILE_CREATE_TOKEN_KEY = 'fenxiao-profile-create-token'

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
  const [adminPassword, setAdminPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [home, setHome] = useState<DistributionHomeResponse | null>(null)
  const [team, setTeam] = useState<TeamListResponse | null>(null)
  const [rewards, setRewards] = useState<RewardListResponse | null>(null)
  const [adminOverview, setAdminOverview] = useState<OverviewReportResponse | null>(null)
  const [adminRewards, setAdminRewards] = useState<RewardListResponse | null>(null)
  const [adminRelation, setAdminRelation] = useState<RelationDetailResponse | null>(null)
  const [form, setForm] = useState({
    userId: session?.userId?.toString() ?? '',
    countryCode: session?.countryCode ?? 'ID',
    languageCode: session?.languageCode ?? 'id',
    inviteCode: '',
  })
  const [adminRewardQuery, setAdminRewardQuery] = useState({ beneficiaryUserId: '', status: '' })
  const [relationQueryUserId, setRelationQueryUserId] = useState('')
  const [profileCreateToken, setProfileCreateToken] = useState(() => localStorage.getItem(PROFILE_CREATE_TOKEN_KEY) || '')

  const canLoadData = useMemo(() => Boolean(session?.userId && session?.accessToken), [session])
  const canLoadAdmin = useMemo(() => Boolean(adminSession?.sessionToken), [adminSession])
  const canCreateProfile = useMemo(() => Boolean(profileCreateToken.trim()), [profileCreateToken])

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
    } catch (err) {
      setError(err instanceof Error ? err.message : '后台登录失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleAdminLogout() {
    setAdminSession(null)
    setAdminOverview(null)
    setAdminRewards(null)
    setAdminRelation(null)
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
      setError(err instanceof Error ? err.message : '加载数据失败')
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
      setError(err instanceof Error ? err.message : '加载后台概览失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleLoadAdminRewards() {
    if (!adminSession) return
    setLoading(true)
    setError('')
    try {
      const result = await getAdminRewards(adminSession.sessionToken, {
        beneficiaryUserId: adminRewardQuery.beneficiaryUserId ? Number(adminRewardQuery.beneficiaryUserId) : undefined,
        status: adminRewardQuery.status || undefined,
      })
      setAdminRewards(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载后台奖励失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleLoadRelation() {
    if (!adminSession || !relationQueryUserId) return
    setLoading(true)
    setError('')
    try {
      const relation = await getAdminRelation(adminSession.sessionToken, Number(relationQueryUserId))
      setAdminRelation(relation)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载关系链失败')
    } finally {
      setLoading(false)
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

  return (
    <div className="page-shell">
      <header className="hero-block">
        <div>
          <p className="eyebrow">Fenxiao Web MVP</p>
          <h1>三级分销网页端演示台</h1>
          <p className="subtext">这版已经包含前台分销页面 + 后台基础运营页面，并升级为后台登录态，不需要再手填 Admin Token。</p>
        </div>
        {session ? (
          <button className="ghost-btn" onClick={handleLogout}>退出当前前台会话</button>
        ) : null}
      </header>

      <section className="card">
        <div className="section-head">
          <h2>1. 前台接入令牌</h2>
          <button className="primary-btn" onClick={handleProfileCreateTokenSave}>保存接入令牌</button>
        </div>
        <div className="grid-form compact-form single-line wide-line">
          <label>
            Profile Create Token
            <input value={profileCreateToken} onChange={(e) => setProfileCreateToken(e.target.value)} placeholder="请输入创建分销档案的接入令牌" />
          </label>
        </div>
      </section>

      <section className="card">
        <h2>2. 接入 / 创建分销档案</h2>
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
          <button className="primary-btn" type="submit" disabled={loading || !canCreateProfile}>创建 / 进入</button>
        </form>

        {session ? (
          <div className="session-box">
            <div><strong>当前用户：</strong>{session.userId}</div>
            <div><strong>邀请码：</strong>{session.inviteCode}</div>
            <div><strong>Access Token：</strong><code>{session.accessToken}</code></div>
          </div>
        ) : null}
      </section>

      <section className="card">
        <div className="section-head">
          <h2>3. 前台分销数据</h2>
          <button className="primary-btn" onClick={handleLoadDashboard} disabled={!canLoadData || loading}>加载前台数据</button>
        </div>
        {error ? <pre className="error-box">{error}</pre> : null}

        <div className="stats-grid">
          <Metric label="邀请人数" value={home?.invitedUsers} />
          <Metric label="有效用户" value={home?.effectiveUsers} />
          <Metric label="总奖励" value={home?.totalReward} />
          <Metric label="可用奖励" value={home?.availableReward} />
          <Metric label="冻结奖励" value={home?.frozenReward} />
          <Metric label="风险冻结" value={home?.riskHoldReward} />
        </div>
      </section>

      <section className="card">
        <h2>4. 直属团队</h2>
        <DataTable
          headers={['用户ID', '邀请码', '国家', '有效用户', '确认收益', '锁定状态', '绑定时间']}
          emptyText="暂无团队数据"
          rows={team?.items?.map((item) => [
            item.userId,
            item.inviteCode,
            item.countryCode,
            item.effectiveUser ? '是' : '否',
            item.confirmedIncomeTotal,
            item.lockStatus,
            item.bindTime,
          ])}
        />
      </section>

      <section className="card">
        <h2>5. 奖励明细</h2>
        <DataTable
          headers={['来源用户', '层级', '奖励金额', '状态', '计算时间']}
          emptyText="暂无奖励数据"
          rows={rewards?.items?.map((item) => [
            item.sourceUserId,
            item.rewardLevel,
            item.rewardAmount,
            item.rewardStatus,
            item.calculatedAt,
          ])}
        />
      </section>

      <section className="card admin-section">
        <div className="section-head">
          <h2>6. 后台登录</h2>
          {adminSession ? (
            <button className="ghost-btn" onClick={handleAdminLogout} disabled={loading}>退出后台</button>
          ) : null}
        </div>

        {adminSession ? (
          <div className="session-box">
            <div><strong>后台状态：</strong>已登录</div>
            <div><strong>会话到期：</strong>{adminSession.expiresAt}</div>
            <div><strong>安全说明：</strong>后台会话仅保存在当前页面内存，刷新页面后需重新登录。</div>
          </div>
        ) : (
          <form className="grid-form compact-form single-line wide-line" onSubmit={handleAdminLogin}>
            <label>
              后台登录口令
              <input
                type="password"
                value={adminPassword}
                onChange={(e) => setAdminPassword(e.target.value)}
                placeholder="请输入后台登录口令"
              />
            </label>
            <button className="primary-btn" type="submit" disabled={loading || !adminPassword.trim()}>登录后台</button>
          </form>
        )}
      </section>

      <section className="card admin-section">
        <div className="section-head">
          <h2>7. 后台概览报表</h2>
          <button className="primary-btn" onClick={handleLoadAdminOverview} disabled={loading || !canLoadAdmin}>加载后台概览</button>
        </div>
        <div className="stats-grid">
          <Metric label="累计邀请用户" value={adminOverview?.invitedUsers} />
          <Metric label="有效用户数" value={adminOverview?.effectiveUsers} />
          <Metric label="奖励总额" value={adminOverview?.rewardTotal} />
          <Metric label="冻结奖励总额" value={adminOverview?.frozenRewardTotal} />
          <Metric label="可用奖励总额" value={adminOverview?.availableRewardTotal} />
          <Metric label="风险事件数" value={adminOverview?.riskEventCount} />
        </div>
      </section>

      <section className="card admin-section">
        <div className="section-head">
          <h2>8. 后台奖励筛选</h2>
          <button className="primary-btn" onClick={handleLoadAdminRewards} disabled={loading || !canLoadAdmin}>查询后台奖励</button>
        </div>
        <div className="grid-form compact-form">
          <label>
            受益用户ID
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
        <DataTable
          headers={['受益用户', '来源用户', '层级', '奖励金额', '状态', '计算时间']}
          emptyText="暂无后台奖励数据"
          rows={adminRewards?.items?.map((item) => [
            item.beneficiaryUserId,
            item.sourceUserId,
            item.rewardLevel,
            item.rewardAmount,
            item.rewardStatus,
            item.calculatedAt,
          ])}
        />
      </section>

      <section className="card admin-section">
        <div className="section-head">
          <h2>9. 关系链查询</h2>
          <button className="primary-btn" onClick={handleLoadRelation} disabled={loading || !relationQueryUserId || !canLoadAdmin}>查询关系链</button>
        </div>
        <div className="grid-form compact-form single-line">
          <label>
            用户ID
            <input value={relationQueryUserId} onChange={(e) => setRelationQueryUserId(e.target.value)} placeholder="例如 10003" />
          </label>
        </div>
        {adminRelation ? (
          <div className="relation-grid">
            <RelationItem label="用户ID" value={adminRelation.userId} />
            <RelationItem label="一级上级" value={adminRelation.level1InviterId} />
            <RelationItem label="二级上级" value={adminRelation.level2InviterId} />
            <RelationItem label="三级上级" value={adminRelation.level3InviterId} />
            <RelationItem label="绑定来源" value={adminRelation.bindSource} />
            <RelationItem label="锁定状态" value={adminRelation.lockStatus} />
            <RelationItem label="绑定时间" value={adminRelation.bindTime} />
            <RelationItem label="锁定时间" value={adminRelation.lockTime || '-'} />
            <RelationItem label="国家" value={adminRelation.countryCode} />
            <RelationItem label="跨国家" value={adminRelation.crossCountry ? '是' : '否'} />
          </div>
        ) : (
          <div className="empty-card">暂无关系链数据</div>
        )}
      </section>
    </div>
  )
}

function Metric({ label, value }: { label: string; value?: number }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong>{value ?? '-'}</strong>
    </div>
  )
}

function RelationItem({ label, value }: { label: string; value: string | number | null }) {
  return (
    <div className="relation-item">
      <span>{label}</span>
      <strong>{value ?? '-'}</strong>
    </div>
  )
}

function DataTable({ headers, rows, emptyText }: { headers: string[]; rows?: Array<Array<string | number>>; emptyText: string }) {
  return (
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
  )
}

export default App
