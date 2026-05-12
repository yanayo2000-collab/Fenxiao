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

export type AdminWorkspaceShortcut = {
  title: string
  value: string
  description: string
  tone: 'neutral' | 'primary' | 'success' | 'warning'
  href: string
  cta: string
}

export type AdminSectionLink = {
  label: string
  description: string
  href: string
}

export function buildAdminTaskCards(input: {
  adminLoggedIn: boolean
  overviewLoaded: boolean
  pendingRiskCount: number
  failedLinkyRequests: number
  replayedLinkyRequests: number
}): AdminTaskCard[] {
  return [
    {
      title: '分销后台会话',
      value: input.adminLoggedIn ? '已登录' : '待登录',
      tone: input.adminLoggedIn ? 'success' : 'warning',
      hint: input.adminLoggedIn
        ? '后台 session 已建立，可以继续处理接入、收益和绑定关系。'
        : '先建立后台 session，概览、邀请码、绑定关系和收益记录才能继续联动。',
    },
    {
      title: '分销概览',
      value: input.overviewLoaded ? '已同步' : '待同步',
      tone: input.overviewLoaded ? 'success' : 'primary',
      hint: input.overviewLoaded
        ? '总览已到位，可以继续判断邀请码、绑定和收益进度。'
        : '先同步一次总览，确认当前邀请码、绑定和收益总况。',
    },
    {
      title: '邀请码入口',
      value: '三页已就位',
      tone: 'primary',
      hint: 'invite / bind / earnings 三个对外入口已经集中到后台，适合直接发给用户。',
    },
    {
      title: '绑定与收益',
      value: input.pendingRiskCount > 0 ? '先看主链，不进高级排查' : '保持主链清晰',
      tone: 'neutral',
      hint: '这一版后台先聚焦接入、绑定和收益，不再把治理型和调试型模块放在首页主入口。',
    },
  ]
}

export function buildAdminWorkspaceShortcuts(input: {
  adminLoggedIn: boolean
  overviewLoaded: boolean
  pendingRiskCount: number
}): AdminWorkspaceShortcut[] {
  return [
    {
      title: '先登录后台',
      value: input.adminLoggedIn ? '已完成' : '现在去做',
      description: input.adminLoggedIn ? '后台会话已建立，可以直接继续查概览和业务主链。' : '第一步先建立后台会话，不然核心模块都只是占位。',
      tone: input.adminLoggedIn ? 'success' : 'primary',
      href: '#admin-login',
      cta: input.adminLoggedIn ? '查看登录状态' : '去登录',
    },
    {
      title: '同步分销概览',
      value: input.overviewLoaded ? '已同步' : input.adminLoggedIn ? '建议现在同步' : '登录后再做',
      description: input.overviewLoaded ? '已经有全局盘面，可以继续判断邀请码、绑定和收益优先级。' : '先拉一次总览，后面再查单点问题会更有方向。',
      tone: input.overviewLoaded ? 'success' : input.adminLoggedIn ? 'warning' : 'neutral',
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
      title: '处理绑定与收益',
      value: '主链优先',
      description: '查收益去收益记录，查关系去绑定关系，这一版先不把治理和调试模块放到首页主线。',
      tone: 'neutral',
      href: '#admin-modules',
      cta: '查看主链模块',
    },
  ]
}

export function buildAdminSectionLinks(): AdminSectionLink[] {
  return [
    {
      label: '分销接入',
      description: '创建分销档案时邀请码必填，首批运营也从初始邀请码进入。',
      href: '#admin-onboarding',
    },
    {
      label: '分销概览',
      description: '先看邀请码、绑定和收益总况。',
      href: '#admin-overview',
    },
    {
      label: '邀请码与对外入口',
      description: '统一管理 invite / bind / earnings 三个对外页面，适合发给渠道、客服或用户。',
      href: '#admin-invite-ops',
    },
    {
      label: '收益记录',
      description: '奖励没起来、金额不对时，先从这里查。',
      href: '#admin-rewards',
    },
    {
      label: '绑定关系',
      description: '用户关系错了、需要人工修正时，从这里进。',
      href: '#admin-bindings',
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
      title: 'Linky 回传链路待校验',
      summary: '先按订单号查一笔 Linky webhook，确认收益事件是否已经稳定进入 Fenxiao。',
    }
  }
  if (blockedCount > 0 || input.replayedCount > 0) {
    return {
      tone: 'danger',
      title: 'Linky 回传链路存在阻塞',
      summary: `最近查询里有 ${blockedCount} 条失败/拒绝请求、${input.replayedCount} 条重复命中，请先确认回传是否影响归因和奖励结算。`,
    }
  }
  return {
    tone: 'success',
    title: 'Linky 回传链路较稳定',
    summary: input.processedCount > 0
      ? '最近查询以 PROCESSED 为主，可继续按订单追奖励结算与裂变归因。'
      : '最近没有命中异常结果，可以换订单号或用户继续校验增长链路。',
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
