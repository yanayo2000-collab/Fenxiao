import { describe, expect, it } from 'vitest'
import {
  buildAdminSectionLinks,
  buildAdminTaskCards,
  buildAdminWorkspaceShortcuts,
  buildEmptyStatePreset,
  buildLinkyDiagnosticSnapshot,
} from './opsConsole'

describe('buildAdminTaskCards', () => {
  it('highlights login and overview before advanced operations are available', () => {
    expect(buildAdminTaskCards({
      adminLoggedIn: false,
      overviewLoaded: false,
      pendingRiskCount: 0,
      failedLinkyRequests: 0,
      replayedLinkyRequests: 0,
    })).toEqual([
      {
        title: '分销后台会话',
        value: '待登录',
        tone: 'warning',
        hint: '先建立后台 session，概览、邀请码、绑定关系和收益记录才能继续联动。',
      },
      {
        title: '分销概览',
        value: '待同步',
        tone: 'primary',
        hint: '先同步一次总览，确认当前多产品分销盘子的邀请、收益和异常概况。',
      },
      {
        title: '异常处理',
        value: '已清空',
        tone: 'success',
        hint: '当前没有待处理异常，可以继续查看绑定关系和收益走势。',
      },
      {
        title: '产品事件排查',
        value: '已稳定',
        tone: 'success',
        hint: '高级排查里再按具体产品查看事件日志和重复回放，Linky 只是其中一个产品。',
      },
    ])
  })

  it('elevates exceptions and product event anomalies when operational data exists', () => {
    expect(buildAdminTaskCards({
      adminLoggedIn: true,
      overviewLoaded: true,
      pendingRiskCount: 3,
      failedLinkyRequests: 2,
      replayedLinkyRequests: 4,
    })[2]).toEqual({
      title: '异常处理',
      value: '3 条待处理',
      tone: 'danger',
      hint: '优先处理待确认异常，避免绑定关系、收益状态或事件回传长期卡住。',
    })
  })
})

describe('buildAdminWorkspaceShortcuts', () => {
  it('guides operators through login, overview, invite entry and module routing before admin data exists', () => {
    expect(buildAdminWorkspaceShortcuts({
      adminLoggedIn: false,
      overviewLoaded: false,
      pendingRiskCount: 0,
    })).toEqual([
      {
        title: '先登录后台',
        value: '现在去做',
        description: '第一步先建立后台会话，不然大部分模块都只是占位。',
        tone: 'primary',
        href: '#admin-login',
        cta: '去登录',
      },
      {
        title: '同步分销概览',
        value: '登录后再做',
        description: '先拉一次总览，后面再查单点问题会更有方向。',
        tone: 'neutral',
        href: '#admin-overview',
        cta: '去看概览',
      },
      {
        title: '管理邀请码与对外入口',
        value: '三页入口已就位',
        description: '发码、绑定、收益查看都统一放在这一块，运营不用再去侧边找链接。',
        tone: 'primary',
        href: '#admin-invite-ops',
        cta: '去看入口',
      },
      {
        title: '按问题进入模块',
        value: '按场景操作',
        description: '查收益去收益记录，查归属去绑定关系，查回传去高级排查。',
        tone: 'neutral',
        href: '#admin-modules',
        cta: '查看模块',
      },
    ])
  })

  it('surfaces pending exceptions as the primary next-step warning', () => {
    expect(buildAdminWorkspaceShortcuts({
      adminLoggedIn: true,
      overviewLoaded: false,
      pendingRiskCount: 2,
    })[3]).toEqual({
      title: '按问题进入模块',
      value: '2 个异常待处理',
      description: '先处理异常，再回头看绑定和收益，避免误判。',
      tone: 'warning',
      href: '#admin-modules',
      cta: '查看模块',
    })
  })
})

describe('buildAdminSectionLinks', () => {
  it('keeps the operator navigation focused on core modules before advanced diagnostics', () => {
    expect(buildAdminSectionLinks()).toEqual([
      {
        label: '邀请码与对外入口',
        description: '统一管理 invite / bind / earnings 三个对外页面，适合发给渠道、客服或用户。',
        href: '#admin-invite-ops',
      },
      {
        label: '收益记录',
        description: '奖励没起来、金额不对、状态异常时，先从这里查。',
        href: '#admin-rewards',
      },
      {
        label: '产品归属',
        description: '查单个用户当前归属到哪个产品，必要时做人工修正。',
        href: '#admin-ownership',
      },
      {
        label: '绑定关系',
        description: '用户归属错了、需要人工修正关系时，从这里进。',
        href: '#admin-bindings',
      },
      {
        label: '异常处理',
        description: '有待处理风险、冻结、忽略或人工复核需求时，从这里进。',
        href: '#admin-exceptions',
      },
      {
        label: '高级排查',
        description: '只有产品事件链路出问题时再展开，避免主后台一上来太重。',
        href: '#admin-advanced',
      },
    ])
  })
})

describe('buildLinkyDiagnosticSnapshot', () => {
  it('returns a growth-chain warning before any Linky query has been executed', () => {
    expect(buildLinkyDiagnosticSnapshot({
      hasQueried: false,
      processedCount: 0,
      failedCount: 0,
      rejectedCount: 0,
      replayedCount: 0,
    })).toEqual({
      tone: 'warning',
      title: 'Linky 回传链路待校验',
      summary: '先按订单号查一笔 Linky webhook，确认收益事件是否已经稳定进入 Fenxiao。',
    })
  })

  it('summarizes blocked and replayed Linky callbacks for growth diagnosis', () => {
    expect(buildLinkyDiagnosticSnapshot({
      hasQueried: true,
      processedCount: 5,
      failedCount: 2,
      rejectedCount: 1,
      replayedCount: 4,
    })).toEqual({
      tone: 'danger',
      title: 'Linky 回传链路存在阻塞',
      summary: '最近查询里有 3 条失败/拒绝请求、4 条重复命中，请先确认回传是否影响归因和奖励结算。',
    })
  })

  it('returns a healthy summary when Linky callback processing is stable', () => {
    expect(buildLinkyDiagnosticSnapshot({
      hasQueried: true,
      processedCount: 6,
      failedCount: 0,
      rejectedCount: 0,
      replayedCount: 0,
    })).toEqual({
      tone: 'success',
      title: 'Linky 回传链路较稳定',
      summary: '最近查询以 PROCESSED 为主，可继续按订单追奖励结算与裂变归因。',
    })
  })
})

describe('buildEmptyStatePreset', () => {
  it('returns task-oriented copy for Linky webhook empty states', () => {
    expect(buildEmptyStatePreset('linky-webhook')).toEqual({
      title: '先查一笔 Linky webhook',
      description: '建议先输入订单号；如果没有结果，再确认请求是否真的到达 Fenxiao。',
      actionLabel: '推荐先按订单号查',
    })
  })

  it('returns post-query copy when a query has already been executed', () => {
    expect(buildEmptyStatePreset('linky-webhook', true)).toEqual({
      title: '这次没查到 Linky webhook',
      description: '换一个订单号、用户或请求状态再试；如果仍然为空，优先确认上游请求是否真的打到 Fenxiao。',
      actionLabel: '建议切换订单号或状态',
    })
  })

  it('returns task-oriented copy for risk empty states', () => {
    expect(buildEmptyStatePreset('risk')).toEqual({
      title: '还没有风险结果',
      description: '先按用户或状态查一次，待处理风险会直接显示在这里。',
      actionLabel: '推荐先看 PENDING',
    })
  })
})
