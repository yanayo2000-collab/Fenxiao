import { beforeEach, describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import App, { ConsoleApp, buildBindGuildInviteGuidance, formatBusinessRewardLevel } from './App'

type FakeStorage = {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
  removeItem: (key: string) => void
  clear: () => void
}

function createStorage(): FakeStorage {
  const store = new Map<string, string>()
  return {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => {
      store.set(key, value)
    },
    removeItem: (key) => {
      store.delete(key)
    },
    clear: () => {
      store.clear()
    },
  }
}

const adminTestSession = {
  sessionToken: 'admin-token',
  expiresAt: '2099-01-01T00:00:00Z',
  username: 'operator',
  displayName: '运营账号',
  role: 'ADMIN',
}

describe('App external landing pages', () => {
  beforeEach(() => {
    const localStorage = createStorage()
    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: {
        location: {
          pathname: '/bind',
          search: '',
          origin: 'http://127.0.0.1:4173',
        },
        localStorage,
      },
    })
  })

  it('keeps the language selector accessible without rendering a visible language label in the bind topbar', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).not.toContain('>语言<')
    expect(markup).toContain('aria-label="语言"')
    expect(markup).toContain('绑定页')
    expect(markup).toContain('>邀请<')
    expect(markup).toContain('我的收益')
  })

  it('uses the shared consumer navigation and form system on the bind page', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('class="consumer-topbar"')
    expect(markup).toContain('class="consumer-form-card"')
    expect(markup).toContain('class="consumer-bottom-nav"')
  })
})

describe('bind guild invite guidance', () => {
  it('extracts the expected Linky guild invite code from backend errors', () => {
    expect(buildBindGuildInviteGuidance('Please join expected Linky guild with invite code GUILD-88 before binding.')).toEqual({
      title: '请先加入指定 Linky 公会',
      inviteCode: 'GUILD-88',
      description: '这个 Linky ID 还没有命中上级对应公会。请先用公会邀请码 GUILD-88 加入指定公会，再回来提交绑定。',
    })
  })

  it('returns null for ordinary bind errors', () => {
    expect(buildBindGuildInviteGuidance('WhatsApp number already exists')).toBeNull()
  })
})

describe('ConsoleApp admin core distribution workspace', () => {
  beforeEach(() => {
    const localStorage = createStorage()
    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: {
        location: {
          pathname: '/',
          search: '',
          origin: 'http://127.0.0.1:4173',
        },
        localStorage,
      },
    })
  })

  it('renders login as the first admin page before any backend workspace is visible', () => {
    window.location.pathname = '/manual-login'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('admin-login-page')
    expect(markup).toContain('admin-login-shell')
    expect(markup).toContain('分销运营后台')
    expect(markup).toContain('后台账号')
    expect(markup).toContain('登录密码')
    expect(markup).toContain('type="password"')
    expect(markup).not.toContain('显示密码')
    expect(markup).toContain('进入后台')
    expect(markup).not.toContain('双重验证码')
    expect(markup).not.toContain('admin-login-brand-panel')
    expect(markup).not.toContain('Fx')
    expect(markup).not.toContain('渠道入口')
    expect(markup).not.toContain('登录后统一处理')
    expect(markup).not.toContain('admin-sidebar')
    expect(markup).not.toContain('分销概览')
  })

  it('shows a neutral restore state instead of the login form while a remembered session is checked', () => {
    window.location.pathname = '/'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('正在恢复登录状态')
    expect(markup).not.toContain('登录密码')
    expect(markup).not.toContain('双重验证码')
  })

  it('renders a module-based admin console without a separate user-workbench mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('admin-console-page')
    expect(markup).toContain('admin-sidebar')
    expect(markup).toContain('admin-workspace-shell')
    expect(markup).toContain('admin-account-chip')
    expect(markup).toContain('运营账号')
    expect(markup).not.toContain('后台账号')
    expect(markup).not.toContain('登录密码')
    expect(markup).not.toContain('后台登录口令')
    expect(markup).toContain('分销概览')
    expect(markup).toContain('渠道入口')
    expect(markup).toContain('绑定关系')
    expect(markup).toContain('收益提现')
    expect(markup).toContain('配置')
    expect(markup).not.toContain('>用户工作台<')
    expect(markup).not.toContain('分销用户工作台')
  })

  it('renders only the active admin module instead of stacking every function panel', () => {
    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: {
        location: {
          pathname: '/',
          search: '',
          hash: '#admin-channel-entries',
          origin: 'http://127.0.0.1:4173',
        },
        localStorage: createStorage(),
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
      },
    })
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('渠道入口管理')
    expect(markup).toContain('入口域名')
    expect(markup).not.toContain('收益记录管理')
    expect(markup).not.toContain('提现申请管理')
    expect(markup).not.toContain('绑定关系管理')
    expect(markup).not.toContain('公会配置管理')
    expect(markup).not.toContain('先做这 4 件事')
    expect(markup).not.toContain('当前主链顺序')
  })
})

describe('Earnings landing page', () => {
  function mountEarningsPage(withSession = true) {
    const localStorage = createStorage()
    if (withSession) {
      localStorage.setItem('fenxiao-web-session', JSON.stringify({
        userId: 10001,
        inviteCode: 'ABCD1234',
        countryCode: 'ID',
        languageCode: 'id',
        accessToken: 'token-1',
      }))
    }
    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: {
        location: {
          pathname: '/earnings',
          search: '',
          origin: 'http://127.0.0.1:4173',
        },
        localStorage,
      },
    })
  }

  beforeEach(() => {
    mountEarningsPage(true)
  })

  it('renders phone login and verification code workflow on invite page', () => {
    const localStorage = createStorage()
    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: {
        location: {
          pathname: '/invite',
          search: '',
          origin: 'http://127.0.0.1:4173',
        },
        localStorage,
      },
    })
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('手机号登录')
    expect(markup).toContain('获取验证码')
    expect(markup).toContain('验证码')
    expect(markup).toContain('登录后开始邀请')
    expect(markup).toContain('邀请码（首次注册必填）')
    expect(markup).not.toContain('立即生成邀请码')
  })

  it('renders a task-first earnings home instead of console language', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('我的收益')
    expect(markup).toContain('申请提现')
    expect(markup).toContain('全部记录')
    expect(markup).toContain('用户导航')
    expect(markup).not.toContain('控制台')
    expect(markup).not.toContain('工作台')
  })

  it('renders a clear linky eligibility verification workspace in admin mode', () => {
    window.location.hash = '#admin-bindings'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('Linky 资格核验')
    expect(markup).toContain('Linky 账号')
    expect(markup).toContain('刷新资格结果')
    expect(markup).toContain('批量刷新全部 Linky 资格')
    expect(markup).toContain('批量刷新资格。')
    expect(markup).toContain('成功数量')
    expect(markup).toContain('失败数量')
    expect(markup).toContain('校验公会归属。')
  })

  it('renders a guild weekly report workspace in admin mode', () => {
    window.location.hash = '#admin-settings'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('公会周报')
    expect(markup).toContain('公会 ID')
    expect(markup).toContain('查询公会周报')
    expect(markup).toContain('按公会聚合。')
  })

  it('renders a guild config management workspace in admin mode', () => {
    window.location.hash = '#admin-settings'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('公会配置管理')
    expect(markup).toContain('查询公会配置')
    expect(markup).toContain('保存公会配置')
    expect(markup).toContain('上级用户 ID（为空则为默认公会）')
    expect(markup).toContain('公会邀请码')
    expect(markup).toContain('启用状态')
    expect(markup).toContain('维护公会映射。')
  })

  it('renders a finished withdraw approval workspace with operator audit controls in admin mode', () => {
    window.location.hash = '#admin-rewards'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('提现申请管理')
    expect(markup).not.toContain('审批操作人 ID')
    expect(markup).not.toContain('操作角色')
    expect(markup).toContain('提现申请详情')
    expect(markup).toContain('选择一笔申请')
    expect(markup).toContain('从左侧队列选择申请后，在这里完成审核和打款留痕。')
    expect(markup).toContain('提现队列筛选')
    expect(markup).toContain('按条件查询提现申请')
    expect(markup).toContain('>重置<')
    expect(markup).not.toContain('审批备注')
  })

  it('restores the operator withdrawal queue filters between visits', () => {
    window.location.hash = '#admin-rewards'
    window.localStorage.setItem('fenxiao-admin-withdraw-query', JSON.stringify({ userId: '54001', status: 'PAYMENT_PENDING', page: '2', size: '10' }))

    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('value="54001"')
    expect(markup).toContain('<option value="PAYMENT_PENDING" selected="">待打款</option>')
  })

  it('renders channel entry management instead of a static localhost link list', () => {
    window.location.hash = '#admin-channel-entries'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('渠道入口管理')
    expect(markup).toContain('渠道标识')
    expect(markup).toContain('入口域名')
    expect(markup).toContain('追踪参数')
    expect(markup).toContain('邀请注册入口')
    expect(markup).toContain('Linky 绑定入口')
    expect(markup).toContain('收益查看入口')
    expect(markup).toContain('复制渠道链接')
    expect(markup).not.toContain('这里统一打开和复制对外三页。')
    expect(markup).not.toContain('常用顺序：先生成邀请码，再绑定关系，最后看收益。')
  })

  it('renders invite code as a required field for profile onboarding in admin mode', () => {
    window.location.hash = '#admin-settings'
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('邀请码（必填，首批运营请填写初始邀请码）')
    expect(markup).toContain('required=""')
  })

  it('renders a clear no-session onboarding state for first-time users', () => {
    mountEarningsPage(false)
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('登录后查看你的邀请码')
    expect(markup).toContain('使用手机号登录后即可邀请好友和查看收益。')
    expect(markup).toContain('手机号登录')
    expect(markup).toContain('去绑定关系')
  })

  it('keeps team income available in a compact disclosure', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('团队概览')
    expect(markup).toContain('团队本周收入')
    expect(markup).not.toContain('每个直属下级的本周和上周钻石收入会显示在这里。')
  })

  it('keeps deep team size and reward tiers without exposing them as top-level cards', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('一级用户')
    expect(markup).toContain('二级用户')
    expect(markup).toContain('三级用户')
    expect(markup).toContain('直接邀请奖励')
    expect(markup).toContain('历史二级佣金（只读）')
    expect(markup).toContain('历史三级佣金（只读）')
    expect(markup).not.toContain('三层裂变人数')
  })

  it('renders the payout action and concise empty states', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('可用奖励')
    expect(markup).toContain('冻结奖励')
    expect(markup).toContain('申请提现')
    expect(markup).toContain('提现记录')
    expect(markup).toContain('还没有提现记录')
    expect(markup).toContain('还没有收益记录')
    expect(markup).toContain('先去生成邀请码并完成绑定，后续有收益会自动显示在这里。')
    expect(markup).not.toContain('提现只会按可用奖励里的钻石数量生成申请单')
  })

  it('removes the educational earnings board from the primary journey', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('可用奖励')
    expect(markup).toContain('我的收益')
    expect(markup).not.toContain('你的收益会在这里持续更新')
    expect(markup).not.toContain('邀请码固定不变')
  })

  it('renders reward activity without a permanent status tutorial', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('最近奖励动态')
    expect(markup).toContain('全部记录')
    expect(markup).not.toContain('状态说明')
    expect(markup).not.toContain('冻结中：奖励正在等待结算')
  })

  it('prioritizes balances over explanatory summary cards', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('可用奖励')
    expect(markup).toContain('冻结奖励')
    expect(markup).toContain('累计奖励')
    expect(markup).not.toContain('来自你的下线成员累计确认收益。')
  })

  it('moves secondary detail into progressive disclosure', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('<details>')
    expect(markup).toContain('团队概览')
    expect(markup).toContain('奖励明细')
    expect(markup).toContain('提现记录')
    expect(markup).not.toContain('冻结中 → 可结算 → 风险冻结')
  })

  it('maps direct rewards and legacy commission levels to safe labels', () => {
    expect(formatBusinessRewardLevel(1, 'zh')).toBe('直接邀请奖励')
    expect(formatBusinessRewardLevel(2, 'zh')).toBe('历史二级佣金（只读）')
    expect(formatBusinessRewardLevel(3, 'zh')).toBe('历史三级佣金（只读）')
    expect(formatBusinessRewardLevel(4, 'zh')).toBe('历史层级 4 佣金（只读）')
  })
})
