import { describe, expect, it } from 'vitest'
import {
  buildAdminTaskCards,
  buildEmptyStatePreset,
  buildLinkyDiagnosticSnapshot,
} from './opsConsole'

describe('buildAdminTaskCards', () => {
  it('highlights login and overview before other ops actions are available', () => {
    expect(buildAdminTaskCards({
      adminLoggedIn: false,
      overviewLoaded: false,
      pendingRiskCount: 0,
      failedLinkyRequests: 0,
      replayedLinkyRequests: 0,
    })).toEqual([
      {
        title: '先登录后台',
        value: '未登录',
        tone: 'warning',
        hint: '先建立后台 session，奖励、风险、Linky 排查区才可用。',
      },
      {
        title: '拉取运营概览',
        value: '待处理',
        tone: 'primary',
        hint: '先看规模和风险量，再决定往奖励、风险还是 Linky 入口深挖。',
      },
      {
        title: '处理风险事件',
        value: '0 条待跟进',
        tone: 'neutral',
        hint: '风险列表会优先暴露冻结、人工处置和待复核事件。',
      },
      {
        title: '排查 Linky 异常',
        value: '0 条异常',
        tone: 'neutral',
        hint: '建议先按订单号查 webhook，再结合 replay 记录判断是否重复推送。',
      },
    ])
  })

  it('elevates risk and Linky anomalies when operational data exists', () => {
    expect(buildAdminTaskCards({
      adminLoggedIn: true,
      overviewLoaded: true,
      pendingRiskCount: 3,
      failedLinkyRequests: 2,
      replayedLinkyRequests: 4,
    })[2]).toEqual({
      title: '处理风险事件',
      value: '3 条待跟进',
      tone: 'danger',
      hint: '优先处理待复核风险，避免奖励长期冻结。',
    })
  })
})

describe('buildLinkyDiagnosticSnapshot', () => {
  it('returns a neutral summary before any Linky query has been executed', () => {
    expect(buildLinkyDiagnosticSnapshot({
      hasQueried: false,
      processedCount: 0,
      failedCount: 0,
      rejectedCount: 0,
      replayedCount: 0,
    })).toEqual({
      tone: 'warning',
      title: 'Linky 还没开始排查',
      summary: '先按订单号查一笔 webhook，建立基线后再判断是否存在失败、拒绝或重复命中。',
    })
  })

  it('summarizes failed and replayed requests for first-screen diagnosis', () => {
    expect(buildLinkyDiagnosticSnapshot({
      hasQueried: true,
      processedCount: 5,
      failedCount: 2,
      rejectedCount: 1,
      replayedCount: 4,
    })).toEqual({
      tone: 'danger',
      title: 'Linky 需要优先排查',
      summary: '最近查询里有 3 条失败/拒绝请求、4 条重复命中，请先看 webhook 状态再切 replay 记录。',
    })
  })

  it('returns a healthy summary when only processed requests are present', () => {
    expect(buildLinkyDiagnosticSnapshot({
      hasQueried: true,
      processedCount: 6,
      failedCount: 0,
      rejectedCount: 0,
      replayedCount: 0,
    })).toEqual({
      tone: 'success',
      title: 'Linky 当前较平稳',
      summary: '最近查询以 PROCESSED 为主，若还要追单，优先按订单号查 webhook 明细。',
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
