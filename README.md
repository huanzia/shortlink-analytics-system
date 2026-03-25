# shortlink-system

一个基于 Spring Boot 3 的短链系统后端项目，覆盖短链创建、管理、跳转、异步访问埋点和基础统计分析等核心能力。项目重点展示短链跳转高频场景下的常见优化方案，包括 Redis 缓存回源、空值缓存防穿透、Bloom Filter 前置判断、RabbitMQ 异步埋点、PV/UV 统计等。

## 项目简介

当前版本已经实现一条相对完整的后端闭环：

- 短链创建、详情查询、编辑、启用/禁用、软删除
- 我的短链分页查询
- 短链跳转主链路
- Redis 缓存回源
- 空值缓存防穿透
- Bloom Filter 基础版
- RabbitMQ 异步访问埋点
- Access Log 落库
- PV / UV 统计
- 热门排行、趋势统计、浏览器 / OS / referer 维度统计

这个项目更适合作为后端链路设计与工程实践示例，而不是完整商用后台。

## 技术栈

- Java 17
- Spring Boot 3
- Spring MVC
- MyBatis-Plus
- MySQL 8
- Redis 7
- RabbitMQ
- Redisson BloomFilter
- Knife4j / OpenAPI 3
- Docker Compose

## 已实现功能

- 用户登录基础版
- 短链创建
- 我的短链分页查询
- 短链详情查询
- 短链编辑
- 短链启用 / 禁用
- 短链软删除
- 短链跳转
- 缓存回源与空值缓存
- Bloom Filter 前置拦截
- RabbitMQ 异步访问埋点
- Access Log 落库
- PV / UV 统计
- 热门短链排行
- 最近 7 天趋势统计
- browser / os / referer 基础维度统计

## 项目结构

```text
shortlink-system
├─ src/main/java/com/huanzi/shortlinksystem
│  ├─ common        # 统一返回、异常、工具类
│  ├─ config        # Spring / MyBatis / Redis / RabbitMQ / Redisson 配置
│  ├─ constant      # 常量定义
│  ├─ controller    # 接口入口
│  ├─ dto           # 请求对象
│  ├─ entity        # 数据库实体
│  ├─ enums         # 状态枚举
│  ├─ manager       # Redis、BloomFilter、统计辅助能力
│  ├─ mapper        # MyBatis-Plus Mapper
│  ├─ mq            # MQ 生产与消费
│  ├─ service       # 业务服务
│  └─ vo            # 返回对象
├─ src/main/resources
│  ├─ application.yml
│  └─ sql/init.sql
├─ docker-compose.yml
└─ docs             # 开发说明、项目概览、压测说明
```

## 环境准备

需要准备以下环境：

- JDK 17
- Maven 3.9+
- Docker Desktop

本地依赖服务默认使用以下端口：

- MySQL: `localhost:3308`
- Redis: `localhost:6380`
- RabbitMQ AMQP: `localhost:5673`
- RabbitMQ 管理台: `http://localhost:15673`

## 快速启动

1. 启动依赖服务

```bash
docker compose up -d
```

2. 初始化数据库

```bash
mysql -uroot -proot -P3308 -h127.0.0.1 shortlink_db < src/main/resources/sql/init.sql
```

3. 启动应用

```bash
mvn spring-boot:run
```

4. 打开接口文档

- `http://localhost:20080/doc.html`
- `http://localhost:20080/swagger-ui/index.html`

## Docker 启动方式

在项目根目录执行：

```bash
docker compose up -d
```

查看容器：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f
```

## SQL 初始化方式

项目初始化 SQL 位于：

- `src/main/resources/sql/init.sql`

你可以使用以下方式导入：

```bash
mysql -uroot -proot -P3308 -h127.0.0.1 shortlink_db < src/main/resources/sql/init.sql
```

如果你使用 Navicat、DataGrip 等工具，也可以直接执行该 SQL 文件。

## 配置说明

默认本地配置文件为：

- `src/main/resources/application.yml`

示例配置文件为：

- `src/main/resources/application.example.yml`

如果你需要修改数据库、Redis、RabbitMQ、服务端口或短链基础地址，建议先复制示例配置并按本地环境调整。

## 接口文档入口

应用启动后默认访问：

- Knife4j: `http://localhost:20080/doc.html`
- Swagger UI: `http://localhost:20080/swagger-ui/index.html`
- OpenAPI Docs: `http://localhost:20080/v3/api-docs`

## 当前项目边界

- 当前登录态仍是基础版，管理接口按固定 `userId=1` 演示
- 删除后的 Bloom Filter 不做精细移除或重建
- UV 去重当前基于 Redis Set，不基于 `tb_link_uv_record` 做正式持久化去重
- MQ 链路未实现复杂幂等、死信队列和重试队列
- 统计接口是基础版，不支持复杂筛选、图表页和后台页面
- `browser` / `os` 字段解析仍是基础版

## 更多文档

- [开发与本地调试说明](docs/README-dev.md)
- [项目概览](docs/PROJECT_OVERVIEW.md)
- [压测准备说明](docs/PERFORMANCE_TEST.md)
