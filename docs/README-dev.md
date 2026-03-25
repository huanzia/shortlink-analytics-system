# shortlink-system Dev Guide

## 环境准备

### 基础环境

- JDK 17
- Maven 3.9+
- Docker Desktop
- Windows PowerShell

### 基础服务

项目依赖以下本地服务：

- MySQL: `localhost:3308`
- Redis: `localhost:6380`
- RabbitMQ AMQP: `localhost:5673`
- RabbitMQ 管理台: `http://localhost:15673`

### Docker 启动

在项目根目录执行：

```powershell
docker compose up -d
```

查看容器状态：

```powershell
docker compose ps
```

查看日志：

```powershell
docker compose logs -f
```

停止环境：

```powershell
docker compose down
```

### Spring Boot 本地端口

- 应用端口: `http://localhost:20080`
- Knife4j: `http://localhost:20080/doc.html`
- Swagger UI: `http://localhost:20080/swagger-ui/index.html`
- OpenAPI Docs: `http://localhost:20080/v3/api-docs`

### 压测开关

当前支持以下跳转链路压测开关，均在 `application.yml` 中配置：

- `short-link.features.redis-cache-enabled`
- `short-link.features.bloom-filter-enabled`
- `short-link.features.mq-access-log-enabled`

默认值都是 `true`，也就是当前完整优化版链路。

### 测试数据说明

- 历史初始化数据仅用于参考，不建议作为主测试数据。
- 部分旧初始化数据可能带有历史端口或历史 shortUrl。
- 推荐先通过 `/link/create` 新建一条短链，再使用新生成的 `id` 和 `shortCode` 做所有测试。
- 下文中的 `c9f9df06`、`id=7` 仅为示例占位，建议替换为你自己新创建的数据。

## 启动方式

### 启动项目

```powershell
mvn spring-boot:run
```

如果你修改了压测开关配置，重启应用后再验证：

```powershell
mvn clean
mvn spring-boot:run
```

可通过访问一个存在短链和一个不存在短链来快速验证当前开关组合是否生效。

### DevTools 端口占用排查

如果日志里出现以下信息，说明当前是带 DevTools 的开发模式启动：

- `restartedMain`
- `Devtools property defaults active`
- `Unable to start LiveReload server`

项目已关闭：

- `spring.devtools.restart.enabled=false`
- `spring.devtools.livereload.enabled=false`

如果仍然提示 `Port 20080 was already in use`，先检查是否已有旧的 Java 进程：

```powershell
Get-NetTCPConnection -LocalPort 20080
Get-CimInstance Win32_Process | Where-Object { $_.ProcessId -eq <PID> } | Select-Object ProcessId,Name,CommandLine
```

必要时可先清理构建产物：

```powershell
mvn clean
```

## 核心接口

### 短链创建与查询

- `POST /link/create`
- `GET /link/my`
- `GET /link/{id}`
- `PUT /link/{id}`
- `PUT /link/{id}/status`
- `DELETE /link/{id}`

### 跳转主链路

- `GET /s/{shortCode}`

### 统计查询

- `GET /stats/link/{id}`
- `GET /stats/link/{id}/access-records`
- `GET /stats/link/{id}/trend`
- `GET /stats/link/{id}/dimensions`
- `GET /stats/hot-links`

### 用户接口

- `POST /user/login`

## 推荐测试顺序

### 1. 创建短链

```powershell
curl.exe -X POST http://localhost:20080/link/create `
  -H "Content-Type: application/json" `
  -d "{\"originUrl\":\"https://openai.com\",\"title\":\"OpenAI\",\"description\":\"demo link\",\"expireTime\":\"2026-12-31 23:59:59\"}"
```

### 2. 查询我的短链，拿到 `id` 和 `shortCode`

```powershell
curl.exe "http://localhost:20080/link/my?pageNum=1&pageSize=5"
```

### 2.1 编辑短链

```powershell
curl.exe -X PUT http://localhost:20080/link/请替换为你的id `
  -H "Content-Type: application/json" `
  -d "{\"title\":\"Updated Title\",\"description\":\"updated description\",\"expireTime\":\"2026-12-31 23:59:59\"}"
```

### 2.2 启用 / 禁用短链

禁用：

```powershell
curl.exe -X PUT http://localhost:20080/link/请替换为你的id/status `
  -H "Content-Type: application/json" `
  -d "{\"status\":0}"
```

启用：

```powershell
curl.exe -X PUT http://localhost:20080/link/请替换为你的id/status `
  -H "Content-Type: application/json" `
  -d "{\"status\":1}"
```

### 2.3 删除短链

```powershell
curl.exe -X DELETE http://localhost:20080/link/请替换为你的id
```

删除后再访问对应 `/s/{shortCode}`，应返回失败而不是继续跳转。

### 3. 跳转验证

```powershell
curl.exe -i http://localhost:20080/s/请替换为你的shortCode
```

如果刚刚禁用了短链，再访问应返回失败而不是继续跳转。

### 4. Redis 缓存验证

```powershell
docker exec shortlink-redis redis-cli DEL shortlink:cache:请替换为你的shortCode
curl.exe -i http://localhost:20080/s/请替换为你的shortCode
docker exec shortlink-redis redis-cli GET shortlink:cache:请替换为你的shortCode
```

如果刚执行过编辑、状态更新或删除，再查一次 Redis key，预期该缓存已经被删除；下次跳转会按最新数据库状态回源。

### 5. 空值缓存验证

```powershell
docker exec shortlink-redis redis-cli DEL shortlink:cache:notfound001
curl.exe -i http://localhost:20080/s/notfound001
docker exec shortlink-redis redis-cli GET shortlink:cache:notfound001
curl.exe -i http://localhost:20080/s/notfound001
```

### 6. 过期 / 禁用验证

过期短链：

请使用一个明确早于今天的日期，例如 `2024-01-01 00:00:00`：

```powershell
docker exec shortlink-mysql mysql -uroot -proot -D shortlink_db -e "INSERT INTO tb_short_link (user_id, short_code, short_url, origin_url, title, description, expire_time, status, pv_count, uv_count) VALUES (1, 'expired01', 'http://localhost:20080/s/expired01', 'https://example.com/expired', 'expired', 'expired test', '2024-01-01 00:00:00', 1, 0, 0) ON DUPLICATE KEY UPDATE expire_time='2024-01-01 00:00:00', status=1;"
curl.exe -i http://localhost:20080/s/expired01
```

禁用短链：

```powershell
docker exec shortlink-mysql mysql -uroot -proot -D shortlink_db -e "INSERT INTO tb_short_link (user_id, short_code, short_url, origin_url, title, description, expire_time, status, pv_count, uv_count) VALUES (1, 'disabled01', 'http://localhost:20080/s/disabled01', 'https://example.com/disabled', 'disabled', 'disabled test', NULL, 0, 0, 0) ON DUPLICATE KEY UPDATE status=0;"
curl.exe -i http://localhost:20080/s/disabled01
```

也可以直接对你自己创建的短链先禁用，再验证它不再跳转：

```powershell
curl.exe -X PUT http://localhost:20080/link/请替换为你的id/status `
  -H "Content-Type: application/json" `
  -d "{\"status\":0}"
curl.exe -i http://localhost:20080/s/请替换为你的shortCode
curl.exe -X PUT http://localhost:20080/link/请替换为你的id/status `
  -H "Content-Type: application/json" `
  -d "{\"status\":1}"
curl.exe -i http://localhost:20080/s/请替换为你的shortCode
```

### 7. access log / PV / UV 验证

```powershell
curl.exe -i -c cookies.txt http://localhost:20080/s/请替换为你的shortCode
curl.exe -i -b cookies.txt -c cookies.txt http://localhost:20080/s/请替换为你的shortCode
docker exec shortlink-mysql mysql -uroot -proot -D shortlink_db -e "SELECT id, short_code, pv_count, uv_count FROM tb_short_link WHERE short_code = '请替换为你的shortCode';"
docker exec shortlink-mysql mysql -uroot -proot -D shortlink_db -e "SELECT id, short_code, visitor_id, user_ip, access_time FROM tb_link_access_log WHERE short_code = '请替换为你的shortCode' ORDER BY id DESC LIMIT 5;"
```

### 8. 热门排行验证

```powershell
curl.exe http://localhost:20080/stats/hot-links
```

### 9. 趋势统计验证

```powershell
curl.exe http://localhost:20080/stats/link/请替换为你的id/trend
```

### 10. 维度统计验证

```powershell
curl.exe http://localhost:20080/stats/link/请替换为你的id/dimensions
```

### 11. 布隆过滤器验证

```powershell
curl.exe -i http://localhost:20080/s/notfound001
```

然后新建一条短链并立即访问它，验证新 shortCode 已增量加入布隆过滤器：

```powershell
curl.exe -X POST http://localhost:20080/link/create `
  -H "Content-Type: application/json" `
  -d "{\"originUrl\":\"https://www.mi.com/\",\"title\":\"XiaoMi\",\"description\":\"bloom filter test\",\"expireTime\":\"2026-12-31 23:59:59\"}"
```

### 12. 删除后跳转与分页验证

分页查询：

```powershell
curl.exe "http://localhost:20080/link/my?pageNum=1&pageSize=2"
curl.exe "http://localhost:20080/link/my?pageNum=2&pageSize=2"
```

删除后跳转失败：

```powershell
curl.exe -X DELETE http://localhost:20080/link/请替换为你的id
curl.exe -i http://localhost:20080/s/请替换为你的shortCode
docker exec shortlink-redis redis-cli GET shortlink:cache:请替换为你的shortCode
```

## 当前限制

- 历史初始化数据里仍可能存在旧 shortUrl 端口，不建议作为主验证数据。
- 布隆过滤器当前只支持启动时加载和新增短链追加，不处理删除后的重建或移除。删除后的短码在布隆过滤器层面仍可能被判定为“可能存在”，但后续缓存和状态校验会拦住。
- 短链编辑当前只支持修改 `title`、`description`、`expireTime`，不开放修改 `originUrl`。
- 启用 / 禁用后采用“删除对应 shortCode 缓存”的简单策略，不做更复杂的缓存重建。
- 删除当前采用软删除方案，本轮不做物理删除，也不做删除后的布隆过滤器精细维护。
- 删除态对外统一按 `not found` 处理，跳转链路返回 `404`。
- 统计接口当前按“固定 `userId=1` 且排除删除态”做可见性控制；禁用态和过期态仍可统计，便于管理侧查看历史数据。
- `GET /link/my` 当前只支持最基础分页，不支持多条件筛选和排序配置。
- UV 去重当前基于 Redis Set，不基于 `tb_link_uv_record` 做正式持久化去重。
- access log 中 `browser`、`os`、`referer` 的提取仍是基础版，维度统计可能大量落到 `UNKNOWN` / `DIRECT`。
- 统计接口均为基础版，不支持复杂筛选、分页、多时间范围或图表展示。
- 压测开关主要用于对比不同优化阶段的跳转链路；关闭 MQ 埋点后，不会更新 access log、PV、UV。
