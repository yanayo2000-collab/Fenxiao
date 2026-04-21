# Fenxiao

多国家三级分销系统一期骨架工程。

## 一期范围
- 分销用户扩展信息
- 三级关系链
- 收益事件入库
- 奖励规则配置
- 奖励明细记录
- 风险事件与审计日志

## 技术栈
- Java 21
- Spring Boot 3
- Spring Web / Spring Data JPA
- MySQL
- Flyway

## 本地启动
```bash
mvn spring-boot:run
```

## 后续研发重点
1. 注册绑定接口
2. Linky 收益同步适配器
3. 奖励计算服务
4. 后台配置 / 关系查询 / 奖励查询接口
