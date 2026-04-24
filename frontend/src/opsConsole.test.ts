import { describe, expect, it } from 'vitest'
import {
  buildAdminTaskCards,
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
