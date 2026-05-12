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

  it('renders a merged admin console without a separate user-workbench mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('进入运营后台')
    expect(markup).toContain('分销接入')
    expect(markup).toContain('邀请码与对外入口')
    expect(markup).not.toContain('>用户工作台<')
    expect(markup).not.toContain('分销用户工作台')
  })

  it('keeps only the core distribution modules in admin mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('分销接入')
    expect(markup).toContain('分销概览')
    expect(markup).toContain('邀请码与对外入口')
    expect(markup).toContain('收益记录管理')
    expect(markup).toContain('绑定关系管理')
    expect(markup).not.toContain('产品归属管理')
    expect(markup).not.toContain('异常处理')
    expect(markup).not.toContain('高级排查')
    expect(markup).not.toContain('>当前环境入口<')
    expect(markup).not.toContain('下一批后台能力')
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

  it('renders user-facing earnings guidance and next-step actions instead of console language', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('我的收益概览')
    expect(markup).toContain('继续去生成邀请码')
    expect(markup).toContain('继续去绑定关系')
    expect(markup).not.toContain('控制台')
    expect(markup).not.toContain('工作台')
  })

  it('renders a clear linky eligibility verification workspace in admin mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('Linky 资格核验')
    expect(markup).toContain('Linky 账号')
    expect(markup).toContain('刷新资格结果')
    expect(markup).toContain('批量刷新全部 Linky 资格')
    expect(markup).toContain('批量刷新会逐个重查已登记 Linky ID 的公会归属，并返回成功/失败计数，失败账号保留在后台日志继续排查。')
    expect(markup).toContain('成功数量')
    expect(markup).toContain('失败数量')
    expect(markup).toContain('公会归属会直接决定这个账号能不能注册分销；如果当前公会后台查不到，只能判定未在我方公会命中，外部归属仍待确认。')
  })

  it('renders a guild weekly report workspace in admin mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('公会周报')
    expect(markup).toContain('公会 ID')
    expect(markup).toContain('查询公会周报')
    expect(markup).toContain('周报会按公会归属聚合注册用户、本周收入和由这些用户贡献出的分佣。')
  })

  it('renders a guild config management workspace in admin mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('公会配置管理')
    expect(markup).toContain('查询公会配置')
    expect(markup).toContain('保存公会配置')
    expect(markup).toContain('上级用户 ID（为空则为默认公会）')
    expect(markup).toContain('公会邀请码')
    expect(markup).toContain('启用状态')
    expect(markup).toContain('运营可以在这里维护上级分销人对应的 Linky 公会 ID 和邀请码；没有上级配置时会回落到默认公会。')
  })

  it('renders invite code as a required field for profile onboarding in admin mode', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

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

  it('renders payout guidance and a user-facing empty reward state before records arrive', () => {
    const markup = renderToStaticMarkup(<App />)

    expect(markup).toContain('奖励到账说明')
    expect(markup).toContain('冻结奖励')
    expect(markup).toContain('风险冻结')
    expect(markup).toContain('发起提现申请')
    expect(markup).toContain('提现只会按可用奖励里的钻石数量生成申请单，后续由运营人工发放。')
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
