export type AdminTaskCard = {
  title: string
  value: string
  tone: 'neutral' | 'primary' | 'success' | 'warning' | 'danger'
  hint: string
}

export type LinkyDiagnosticSnapshot = {
  tone: 'success' | 'warning' | 'danger'
  title: string
  summary: string
}

export type EmptyStatePreset = {
  title: string
  description: string
  actionLabel: string
}

export function buildAdminTaskCards(input: {
  adminLoggedIn: boolean
  overviewLoaded: boolean
  pendingRiskCount: number
  failedLinkyRequests: number
  replayedLinkyRequests: number
}): AdminTaskCard[] {
  const linkyIssueCount = input.failedLinkyRequests + input.replayedLinkyRequests
  return [
    {
      title: '先登录后台',
      value: input.adminLoggedIn ? '已登录' : '未登录',
      tone: input.adminLoggedIn ? 'success' : 'warning',
      hint: input.adminLoggedIn
        ? '后台 session 已建立，可以直接继续排查与运营处理。'
        : '先建立后台 session，奖励、风险、Linky 排查区才可用。',
    },
    {
      title: '拉取运营概览',
      value: input.overviewLoaded ? '已就绪' : '待处理',
      tone: input.overviewLoaded ? 'success' : 'primary',
      hint: input.overviewLoaded
        ? '概览已到位，可以根据规模和风险量决定下一步动作。'
        : '先看规模和风险量，再决定往奖励、风险还是 Linky 入口深挖。',
    },
    {
      title: '处理风险事件',
      value: `${input.pendingRiskCount} 条待跟进`,
      tone: input.pendingRiskCount > 0 ? 'danger' : 'neutral',
      hint: input.pendingRiskCount > 0
        ? '优先处理待复核风险，避免奖励长期冻结。'
        : '风险列表会优先暴露冻结、人工处置和待复核事件。',
    },
    {
      title: '排查 Linky 异常',
      value: `${linkyIssueCount} 条异常`,
      tone: linkyIssueCount > 0 ? 'warning' : 'neutral',
      hint: '建议先按订单号查 webhook，再结合 replay 记录判断是否重复推送。',
    },
  ]
}

export function buildLinkyDiagnosticSnapshot(input: {
  hasQueried: boolean
  processedCount: number
  failedCount: number
  rejectedCount: number
  replayedCount: number
}): LinkyDiagnosticSnapshot {
  const blockedCount = input.failedCount + input.rejectedCount
  if (!input.hasQueried) {
    return {
      tone: 'warning',
      title: 'Linky 还没开始排查',
      summary: '先按订单号查一笔 webhook，建立基线后再判断是否存在失败、拒绝或重复命中。',
    }
  }
  if (blockedCount > 0 || input.replayedCount > 0) {
    return {
      tone: 'danger',
      title: 'Linky 需要优先排查',
      summary: `最近查询里有 ${blockedCount} 条失败/拒绝请求、${input.replayedCount} 条重复命中，请先看 webhook 状态再切 replay 记录。`,
    }
  }
  return {
    tone: 'success',
    title: 'Linky 当前较平稳',
    summary: input.processedCount > 0
      ? '最近查询以 PROCESSED 为主，若还要追单，优先按订单号查 webhook 明细。'
      : '最近没有命中异常结果，可以换订单号或用户继续追单。',
  }
}

export function buildEmptyStatePreset(kind: 'linky-webhook' | 'linky-replay' | 'risk' | 'reward', hasQueried = false): EmptyStatePreset {
  switch (kind) {
    case 'linky-webhook':
      return hasQueried
        ? {
            title: '这次没查到 Linky webhook',
            description: '换一个订单号、用户或请求状态再试；如果仍然为空，优先确认上游请求是否真的打到 Fenxiao。',
            actionLabel: '建议切换订单号或状态',
          }
        : {
            title: '先查一笔 Linky webhook',
            description: '建议先输入订单号；如果没有结果，再确认请求是否真的到达 Fenxiao。',
            actionLabel: '推荐先按订单号查',
          }
    case 'linky-replay':
      return hasQueried
        ? {
            title: '这次没命中 replay 记录',
            description: '如果 webhook 已出现重复推送，再换订单号或用户确认是否存在同指纹重复命中。',
            actionLabel: '建议回看 webhook 状态',
          }
        : {
            title: '先看重复命中记录',
            description: '如果 webhook 里已经出现 REPLAYED，再切到这里确认同一指纹累计打了几次。',
            actionLabel: '推荐先看 REPLAYED 单',
          }
    case 'risk':
      return hasQueried
        ? {
            title: '这次没查到风险结果',
            description: '换一个用户、状态或时间范围再试；如果仍为空，说明当前筛选下暂无待处理风险。',
            actionLabel: '建议缩小时间范围',
          }
        : {
            title: '还没有风险结果',
            description: '先按用户或状态查一次，待处理风险会直接显示在这里。',
            actionLabel: '推荐先看 PENDING',
          }
    case 'reward':
      return hasQueried
        ? {
            title: '这次没查到奖励记录',
            description: '换一个受益用户、状态或时间区间再试，确认是不是筛选条件过严。',
            actionLabel: '建议先放宽筛选条件',
          }
        : {
            title: '先筛一轮奖励记录',
            description: '建议先按受益用户或状态查一页，确认奖励是否被冻结、可用或风险挂起。',
            actionLabel: '推荐先查 AVAILABLE / RISK_HOLD',
          }
  }
}
