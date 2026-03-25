# shortlink-system Performance Test Guide

## 压测目标

- 目标接口：`GET /s/{shortCode}`
- 目标：对比短链跳转链路在不同优化阶段下的吞吐和响应时间差异

建议按以下三个版本做对比：

1. 朴素版
2. Redis 缓存版
3. 最终优化版（BloomFilter + Redis + MQ）

## 推荐工具

- JMeter

如果你熟悉其他工具，也可以使用 `wrk`、`ab` 或 `k6`，但为了整理演示结果，当前文档默认使用 JMeter。

## 压测前准备

### 1. 启动基础依赖

```powershell
docker compose up -d
```

### 2. 启动应用

```powershell
mvn spring-boot:run
```

### 3. 准备测试数据

建议准备两类数据：

- 存在短链
  通过 `POST /link/create` 新建一条短链，拿到 `shortCode`
- 不存在短链
  使用一个明确不存在的短码，例如 `notfound001`

推荐先用新创建的短链做“存在短链”压测，不要优先使用历史初始化数据。

## 压测开关

在 [application.yml](C:/Users/huanzi/Desktop/work/JavaProject/shortlink-system/src/main/resources/application.yml) 中可配置以下开关：

- `short-link.features.redis-cache-enabled`
- `short-link.features.bloom-filter-enabled`
- `short-link.features.mq-access-log-enabled`

默认值：

- `redis-cache-enabled: true`
- `bloom-filter-enabled: true`
- `mq-access-log-enabled: true`

修改配置后需要重启 Spring Boot 应用。

## 三种压测模式

### 1. 朴素版

目标：

- 不走 Redis
- 不走 BloomFilter
- 不发 MQ 埋点

建议配置：

```yaml
short-link:
  features:
    redis-cache-enabled: false
    bloom-filter-enabled: false
    mq-access-log-enabled: false
```

行为说明：

- 每次跳转直接查 MySQL
- 不做空值缓存
- 不做布隆过滤器前置拦截
- 不发送访问埋点

适合观察：

- 最原始数据库直查模式下的响应时间和吞吐上限

### 2. Redis 缓存版

目标：

- 开启 Redis 缓存
- 关闭 BloomFilter
- 关闭 MQ 埋点

建议配置：

```yaml
short-link:
  features:
    redis-cache-enabled: true
    bloom-filter-enabled: false
    mq-access-log-enabled: false
```

行为说明：

- 存在短链会优先命中 Redis
- 不存在短链会写空值缓存
- 不走布隆过滤器
- 不发送访问埋点

适合观察：

- 加缓存后，存在短链和不存在短链的性能变化
- 空值缓存对无效短码压测的影响

### 3. 最终优化版

目标：

- 开启 Redis 缓存
- 开启 BloomFilter
- 开启 MQ 异步埋点

建议配置：

```yaml
short-link:
  features:
    redis-cache-enabled: true
    bloom-filter-enabled: true
    mq-access-log-enabled: true
```

行为说明：

- 短码先经过布隆过滤器
- 命中后优先查 Redis
- 成功跳转后异步发送埋点消息

适合观察：

- 最终版本在真实链路下的综合表现
- MQ 异步化是否能把统计成本从主链路中剥离出去

## JMeter 压测建议

### 存在短链压测

请求地址：

```text
http://localhost:20080/s/请替换为你的shortCode
```

建议：

- 用固定存在短链做压测
- 先手动访问一次，确保 Redis 缓存已建立，再测缓存版和最终版

### 不存在短链压测

请求地址：

```text
http://localhost:20080/s/notfound001
```

建议：

- 朴素版：观察数据库直查下的性能
- Redis 缓存版：第一次回源、后续命中空值缓存
- 最终优化版：布隆过滤器直接前置拦截

## 每轮建议记录的指标

- QPS / 吞吐量
- 平均响应时间
- P95
- P99
- 错误率

如果条件允许，可以额外记录：

- MySQL CPU / 连接数
- Redis 命中率
- RabbitMQ 队列堆积情况

## 建议的对比方式

### 对比一：存在短链

- 朴素版 vs Redis 缓存版 vs 最终优化版

重点看：

- 平均响应时间是否下降
- P95 / P99 是否更平稳
- QPS 是否提升

### 对比二：不存在短链

- 朴素版 vs Redis 缓存版 vs 最终优化版

重点看：

- 空值缓存是否减少后续数据库压力
- BloomFilter 是否进一步拦截明显不存在短码

## 结论

建议从以下角度总结：

1. 朴素版的问题
   数据库直查，吞吐受限，面对无效短码时容易被打穿

2. Redis 缓存版的改进
   热点短链访问被拦在缓存层，不存在短码可通过空值缓存减轻回源压力

3. 最终优化版的进一步提升
   布隆过滤器把明显无效请求拦在最前面，MQ 异步埋点降低主链路同步处理成本

4. 最终结论表达模板
   在短链跳转高频场景下，通过“BloomFilter + Redis + MQ 异步埋点”组合方案，把无效请求、热点请求和统计写入成本分别前移、缓存和异步化，主链路性能和稳定性都更适合高并发访问场景。

## 当前压测边界

- 当前没有内置 JMeter 脚本，需要手工创建压测计划
- 当前没有接入监控面板，指标需要通过 JMeter 结果和基础服务日志辅助观察
- 关闭 MQ 埋点后，不会更新访问日志、PV、UV，这属于压测对比模式，不代表正式业务配置
