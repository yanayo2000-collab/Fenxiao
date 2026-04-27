import { beforeEach, describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import App, { ConsoleApp } from './App'

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

describe('ConsoleApp admin ownership workspace', () => {
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

  it('renders an ownership management workspace in admin mode with query, correction, and joint ownership-relation triage', () => {
    const markup = renderToStaticMarkup(<ConsoleApp initialViewMode="admin" />)

    expect(markup).toContain('产品归属管理')
    expect(markup).toContain('查询产品归属')
    expect(markup).toContain('目标产品编码')
    expect(markup).toContain('查看 ownership 审计')
    expect(markup).toContain('联合处置视图')
    expect(markup).toContain('一键联合查询')
    expect(markup).toContain('同步绑定关系')
    expect(markup).toContain('查看联合审计')
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
})
