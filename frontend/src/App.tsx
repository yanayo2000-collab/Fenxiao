import { useMemo, useState } from 'react'
import './App.css'
import {
  createProfile,
  getDistributionHome,
  getDistributionRewards,
  getDistributionTeam,
  type DistributionHomeResponse,
  type ProfileResponse,
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

const STORAGE_KEY = 'fenxiao-web-session'

function loadSession(): SessionState | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SessionState
  } catch {
    return null
  }
}

function saveSession(profile: ProfileResponse) {
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
  const [session, setSession] = useState<SessionState | null>(() => loadSession())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [home, setHome] = useState<DistributionHomeResponse | null>(null)
  const [team, setTeam] = useState<TeamListResponse | null>(null)
  const [rewards, setRewards] = useState<RewardListResponse | null>(null)
  const [form, setForm] = useState({
    userId: session?.userId?.toString() ?? '',
    countryCode: session?.countryCode ?? 'ID',
    languageCode: session?.languageCode ?? 'id',
    inviteCode: '',
  })

  const canLoadData = useMemo(() => Boolean(session?.userId && session?.accessToken), [session])

  async function handleCreateProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const profile = await createProfile({
        userId: Number(form.userId),
        countryCode: form.countryCode.trim().toUpperCase(),
        languageCode: form.languageCode.trim(),
        inviteCode: form.inviteCode.trim() || undefined,
      })
      const nextSession = saveSession(profile)
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

  function handleLogout() {
    localStorage.removeItem(STORAGE_KEY)
    setSession(null)
    setHome(null)
    setTeam(null)
    setRewards(null)
  }

  return (
    <div className="page-shell">
      <header className="hero-block">
        <div>
          <p className="eyebrow">Fenxiao Web MVP</p>
          <h1>三级分销网页端演示台</h1>
          <p className="subtext">当前这版直接对接现有 Spring Boot API，先把网页端最小闭环跑起来。</p>
        </div>
        {session ? (
          <button className="ghost-btn" onClick={handleLogout}>退出当前会话</button>
        ) : null}
      </header>

      <section className="card">
        <h2>1. 接入 / 创建分销档案</h2>
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
          <button className="primary-btn" type="submit" disabled={loading}>创建 / 进入</button>
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
          <h2>2. 分销数据面板</h2>
          <button className="primary-btn" onClick={handleLoadDashboard} disabled={!canLoadData || loading}>加载面板数据</button>
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
        <h2>3. 直属团队</h2>
        <table>
          <thead>
            <tr>
              <th>用户ID</th>
              <th>邀请码</th>
              <th>国家</th>
              <th>有效用户</th>
              <th>确认收益</th>
              <th>锁定状态</th>
              <th>绑定时间</th>
            </tr>
          </thead>
          <tbody>
            {team?.items?.length ? team.items.map((item) => (
              <tr key={item.userId}>
                <td>{item.userId}</td>
                <td>{item.inviteCode}</td>
                <td>{item.countryCode}</td>
                <td>{item.effectiveUser ? '是' : '否'}</td>
                <td>{item.confirmedIncomeTotal}</td>
                <td>{item.lockStatus}</td>
                <td>{item.bindTime}</td>
              </tr>
            )) : (
              <tr><td colSpan={7}>暂无数据</td></tr>
            )}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2>4. 奖励明细</h2>
        <table>
          <thead>
            <tr>
              <th>来源用户</th>
              <th>层级</th>
              <th>奖励金额</th>
              <th>状态</th>
              <th>计算时间</th>
            </tr>
          </thead>
          <tbody>
            {rewards?.items?.length ? rewards.items.map((item, index) => (
              <tr key={`${item.sourceUserId}-${index}`}>
                <td>{item.sourceUserId}</td>
                <td>{item.rewardLevel}</td>
                <td>{item.rewardAmount}</td>
                <td>{item.rewardStatus}</td>
                <td>{item.calculatedAt}</td>
              </tr>
            )) : (
              <tr><td colSpan={5}>暂无数据</td></tr>
            )}
          </tbody>
        </table>
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

export default App
