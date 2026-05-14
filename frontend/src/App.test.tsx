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
    expect(markup).toContain('生成我的邀请码')
    expect(markup).toContain('查看我的人收益')
  })

  it('uses the same topbar pill control class for the language selector and the bind page entry links', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('class="bind-select-group bind-select-inline topbar-pill-control"')
    expect(markup).toContain('class="entry-link active topbar-pill-control"')
    expect(markup).toContain('class="entry-link topbar-pill-control"')
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
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('admin-login-page')
    expect(markup).toContain('admin-login-shell')
    expect(markup).toContain('分销运营后台')
    expect(markup).toContain('后台账号')
    expect(markup).toContain('登录密码')
    expect(markup).toContain('type="password"')
    expect(markup).not.toContain('显示密码')
    expect(markup).toContain('进入后台')
    expect(markup).not.toContain('admin-login-brand-panel')
    expect(markup).not.toContain('Fx')
    expect(markup).not.toContain('渠道入口')
    expect(markup).not.toContain('登录后统一处理')
    expect(markup).not.toContain('admin-sidebar')
    expect(markup).not.toContain('分销概览')
  })

  it('renders a module-based admin console without a separate user-workbench mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" initialAdminSession={adminTestSession} />)

    expect(markup).toContain('admin-console-page')
    expect(markup).toContain('admin-sidebar')
    expect(markup).toContain('admin-workspace-shell')
    expect(markup).toContain('后台会话已建立')
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
    expect(markup).toContain('用手机号登录并继续查看收益')
    expect(markup).toContain('如果你已经有邀请码，也可以在登录时带上邀请码完成资料初始化。')
  })

  it('renders user-facing earnings guidance and next-step actions instead of console language', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('我的收益概览')
    expect(markup).toContain('继续去生成邀请码')
    expect(markup).toContain('继续去绑定关系')
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
    expect(markup).toContain('审批备注')
    expect(markup).toContain('当前登录账号用于审批留痕。')
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

    expect(markup).toContain('先生成你的邀请码')
    expect(markup).toContain('还没开始邀请也没关系。先生成邀请码，再去完成绑定，收益会自动累计到这里。')
    expect(markup).toContain('去生成我的邀请码')
    expect(markup).toContain('去绑定关系')
  })

  it('renders team weekly income summary and direct-member detail placeholders', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('团队周收入')
    expect(markup).toContain('直属下级本周 / 上周钻石收入')
    expect(markup).toContain('团队本周钻石收入')
    expect(markup).toContain('团队上周钻石收入')
    expect(markup).toContain('直属下级收入明细')
    expect(markup).toContain('每个直属下级的本周和上周钻石收入会显示在这里。')
  })

  it('renders deep team size and reward tier summary placeholders on earnings page', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('三层裂变人数')
    expect(markup).toContain('一级下级')
    expect(markup).toContain('二级下级')
    expect(markup).toContain('三级下级')
    expect(markup).toContain('业务二级收益汇总')
    expect(markup).toContain('业务三级收益汇总')
    expect(markup).toContain('业务四级收益汇总')
    expect(markup).toContain('按业务层级汇总你的分销提成和明细数量。')
  })

  it('renders payout guidance and a user-facing empty reward state before records arrive', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('奖励到账说明')
    expect(markup).toContain('冻结奖励')
    expect(markup).toContain('风险冻结')
    expect(markup).toContain('发起提现申请')
    expect(markup).toContain('提现只会按可用奖励里的钻石数量生成申请单，后续由运营人工发放。')
    expect(markup).toContain('提现历史')
    expect(markup).toContain('完整提现记录会按申请时间持续显示在这里。')
    expect(markup).toContain('还没有提现申请')
    expect(markup).toContain('还没有收益记录')
    expect(markup).toContain('先去生成邀请码并完成绑定，后续有收益会自动显示在这里。')
  })

  it('renders a user-facing earnings board that explains how this page helps them track progress', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('你的收益会在这里持续更新')
    expect(markup).toContain('邀请码固定不变')
    expect(markup).toContain('绑定后自动累计')
    expect(markup).toContain('到账状态一目了然')
  })

  it('renders a formal reward activity module with status guidance', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('最近奖励动态')
    expect(markup).toContain('每一笔奖励都会显示状态和时间，方便你确认什么时候到账。')
    expect(markup).toContain('状态说明')
    expect(markup).toContain('冻结中：奖励正在等待结算')
    expect(markup).toContain('可结算：奖励已经可以使用')
    expect(markup).toContain('风险冻结：奖励暂时进入风控复核')
  })

  it('renders polished summary cards with user-facing subtitles and status pills', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('来自你的下线成员累计确认收益。')
    expect(markup).toContain('按奖励记录汇总出来的你的分销提成。')
    expect(markup).toContain('当前已经进入可结算状态的奖励。')
    expect(markup).toContain('已确认')
    expect(markup).toContain('累计提成')
    expect(markup).toContain('可立即查看')
  })

  it('renders unified product-grade detail cards for overview, progress, and settlement', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('当前邀请码与收益总览')
    expect(markup).toContain('绑定完成后人数和收益会持续更新')
    expect(markup).toContain('冻结中 → 可结算 → 风险冻结')
    expect(markup).toContain('邀请码 / 团队 / 奖励')
    expect(markup).toContain('进度追踪')
    expect(markup).toContain('到账路径')
  })

  it('maps technical reward levels to business second, third, and fourth level labels', () => {
    expect(formatBusinessRewardLevel(1, 'zh')).toBe('业务二级收益')
    expect(formatBusinessRewardLevel(2, 'zh')).toBe('业务三级收益')
    expect(formatBusinessRewardLevel(3, 'zh')).toBe('业务四级收益')
    expect(formatBusinessRewardLevel(4, 'zh')).toBe('业务层级 4 收益')
  })
})
