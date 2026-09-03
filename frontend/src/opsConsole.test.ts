import { describe, expect, it } from 'vitest'
import {
  buildAdminSectionLinks,
  buildAdminTaskCards,
  buildAdminWorkspaceShortcuts,
  buildEmptyStatePreset,
  buildLinkyDiagnosticSnapshot,
} from './opsConsole'

describe('buildAdminTaskCards', () => {
  it('highlights login, overview, invite entry, and main distribution flow before advanced operations are available', () => {
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
        hint: '先同步一次总览，确认当前邀请码、绑定和收益总况。',
      },
      {
        title: '渠道入口',
        value: '可生成追踪链接',
        tone: 'primary',
        hint: '按产品、国家、语言、渠道和邀请码生成对外入口，方便运营区分来源和归因。',
      },
      {
        title: '绑定与收益',
        value: '保持主链清晰',
        tone: 'neutral',
        hint: '这一版后台先聚焦接入、绑定和收益，不再把治理型和调试型模块放在首页主入口。',
      },
    ])
  })

  it('keeps the fourth card focused on the core distribution flow even when non-core anomalies exist', () => {
    expect(buildAdminTaskCards({
      adminLoggedIn: true,
      overviewLoaded: true,
      pendingRiskCount: 3,
      failedLinkyRequests: 2,
      replayedLinkyRequests: 4,
    })[3]).toEqual({
      title: '绑定与收益',
      value: '先看主链，不进高级排查',
      tone: 'neutral',
      hint: '这一版后台先聚焦接入、绑定和收益，不再把治理型和调试型模块放在首页主入口。',
    })
  })
})

describe('buildAdminWorkspaceShortcuts', () => {
  it('guides operators through login, overview, invite entry and main distribution routing before admin data exists', () => {
    expect(buildAdminWorkspaceShortcuts({
      adminLoggedIn: false,
      overviewLoaded: false,
      pendingRiskCount: 0,
    })).toEqual([
      {
        title: '先登录后台',
        value: '现在去做',
        description: '第一步先建立后台会话，不然核心模块都只是占位。',
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
        title: '管理渠道入口',
        value: '可生成追踪链接',
        description: '按产品、国家、语言、渠道和邀请码生成可复制的正式入口。',
        tone: 'primary',
        href: '#admin-invite-ops',
        cta: '去生成链接',
      },
      {
        title: '处理绑定与收益',
        value: '主链优先',
        description: '查收益去收益记录，查关系去绑定关系，这一版先不把治理和调试模块放到首页主线。',
        tone: 'neutral',
        href: '#admin-modules',
        cta: '查看主链模块',
      },
    ])
  })

  it('keeps shortcut emphasis on the core chain even when non-core anomalies exist', () => {
    expect(buildAdminWorkspaceShortcuts({
      adminLoggedIn: true,
      overviewLoaded: false,
      pendingRiskCount: 2,
    })[3]).toEqual({
      title: '处理绑定与收益',
      value: '主链优先',
      description: '查收益去收益记录，查关系去绑定关系，这一版先不把治理和调试模块放到首页主线。',
      tone: 'neutral',
      href: '#admin-modules',
      cta: '查看主链模块',
    })
  })
})

describe('buildAdminSectionLinks', () => {
  it('keeps the operator navigation focused on the core distribution flow', () => {
    expect(buildAdminSectionLinks()).toEqual([
      {
        label: '分销概览',
        description: '总览。',
        href: '#admin-overview',
      },
      {
        label: '渠道入口',
        description: '生成渠道链接。',
        href: '#admin-channel-entries',
      },
      {
        label: '绑定关系',
        description: '查询和修正绑定。',
        href: '#admin-bindings',
      },
      {
        label: '收益提现',
        description: '收益记录和提现审批。',
        href: '#admin-rewards',
      },
      {
        label: '账号中心',
        description: '员工、密码和设备安全。',
        href: '#admin-accounts',
      },
      {
        label: '配置',
        description: '接入、公会和产品配置。',
        href: '#admin-settings',
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
      summary: '先按订单号查一笔 Linky webhook，确认收益事件是否已经稳定进入 BANDEIRA。',
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
      description: '建议先输入订单号；如果没有结果，再确认请求是否真的到达 BANDEIRA。',
      actionLabel: '推荐先按订单号查',
    })
  })

  it('returns post-query copy when a query has already been executed', () => {
    expect(buildEmptyStatePreset('linky-webhook', true)).toEqual({
      title: '这次没查到 Linky webhook',
      description: '换一个订单号、用户或请求状态再试；如果仍然为空，优先确认上游请求是否真的打到 BANDEIRA。',
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
