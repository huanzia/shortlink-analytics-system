# shortlink-system Project Overview

## 项目简介

`shortlink-system` 是一个基于 Spring Boot 的短链系统后端项目，覆盖了从短链创建、跳转解析，到异步访问埋点、PV/UV 统计和基础分析查询的一条完整主链路。

项目目标不是做“全功能后台”，而是用较完整的工程链路展示常见高频访问场景下的后端设计思路，包括：

- Redis 缓存回源
- 空值缓存防穿透
- 布隆过滤器前置拦截
- RabbitMQ 异步埋点
- PV / UV 统计
- 热门排行、趋势统计、维度聚合

## 核心技术栈

- Spring Boot 3
- Spring MVC
- MyBatis-Plus
- MySQL 8
- Redis 7
- RabbitMQ
- Redisson BloomFilter
- Lombok
- Knife4j / OpenAPI 3
- Docker Compose

## 系统架构说明

项目当前是典型的单体后端架构，分层大致如下：

- `controller`
  负责接收 HTTP 请求并返回统一结果
- `service`
  负责核心业务逻辑
- `manager`
  封装 Redis、布隆过滤器、统计去重等基础能力
- `mapper`
  负责数据库访问
- `mq`
  负责访问埋点消息的生产与消费
- `common`
  负责统一返回、异常处理、工具类

主链路上已经形成了“同步主流程 + 异步统计”的结构：

- 跳转本身尽量快
- 统计与日志尽量异步落到 MQ 后消费处理

## 当前已实现功能列表

- 用户登录
- 短链创建
- 我的短链列表
- 短链详情查询
- 短链跳转
- Redis 缓存回源
- 空值缓存防穿透
- 布隆过滤器
- 过期 / 禁用短链校验
- RabbitMQ 异步访问埋点
- access log 落库
- PV 更新
- Redis Set UV 去重
- UV 更新
- 短链基础统计查询
- 最近访问记录查询
- 热门短链排行
- 最近 7 天趋势统计
- browser / os / referer 基础维度统计

## 核心主链路说明

### 1. 短链创建链路

入口：

- `POST /link/create`

流程：

1. 校验入参
2. 生成唯一 `shortCode`
3. 组装 `shortUrl`
4. 写入 `tb_short_link`
5. 将 `shortCode` 增量加入布隆过滤器
6. 返回创建结果

### 2. 短链跳转链路

入口：

- `GET /s/{shortCode}`

流程：

1. 先走布隆过滤器，快速拦截明显不存在的短码
2. 再查 Redis 缓存
3. Redis 未命中则回源 MySQL
4. 对不存在短码写入空值缓存
5. 对禁用 / 过期短链直接返回失败，不重定向
6. 对可用短链返回 302 跳转
7. 成功跳转后异步发送访问埋点消息

### 3. 异步埋点链路

流程：

1. 跳转成功后发送访问消息到 RabbitMQ
2. Consumer 消费消息
3. 写入 `tb_link_access_log`
4. `pv_count + 1`
5. 基于 Redis Set 做 UV 去重
6. 首次 UV 访问时 `uv_count + 1`

### 4. 统计查询链路

已提供：

- 短链基础统计
- 最近访问记录
- 热门排行
- 最近 7 天趋势统计
- browser / os / referer 基础维度聚合


