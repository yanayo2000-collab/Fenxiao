# Fenxiao 分销后台收口清单

## 保留的核心主链
1. 运营登录
2. 分销概览
3. 分销接入（邀请码必填）
4. 邀请码与对外入口（invite / bind / earnings）
5. 收益记录管理
6. 绑定关系管理

## 从主后台隐藏的冗余模块
1. 产品归属管理（ownership）
2. 异常处理（risk-events）
3. 高级排查（Linky webhook / replay）
4. 审计日志（audit logs）
5. 环境入口 / 下一批后台能力说明区

## 当前不删底层代码、但不再作为主后台默认可见入口
- ownership 相关 API 与 service
- risk-events 相关 API 与 service
- audit logs 相关 API 与 service
- linky webhook / replay 相关 API 与 service

## 当前主链已落地能力
1. Linky 注册资格已接入 Spring 主链：本地无资格记录时会自动调用 probe 结果刷新资格
2. 后台已提供 Linky 资格刷新入口，运营可以手动触发核验并查看公会/资格结果
3. 收益页已支持用户发起提现申请：只按 `AVAILABLE + UNCLAIMED` 奖励生成申请单
4. 后台已提供提现申请列表，方便运营按申请单人工发放

## 当前仍待继续补强的点
1. 真实 guild 后台账号/权限和稳定探测环境仍需持续校验，避免 probe 受外部登录态影响
2. 提现后台目前以“查看申请单”为主，尚未补发放完成/驳回等后续处理动作

## 本轮目标
- 主后台只保留核心分销闭环
- 运营进入页面后，不再先看到治理型/排查型/调试型模块
- 在主链内补齐 Linky 资格核验与提现申请最小闭环
