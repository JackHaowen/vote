# 匿名投票系统部署指南

本文面向需要在新机器或服务器上部署本项目的维护人员。项目通过 Docker Compose 启动三个容器：

- **frontend**：Nginx 托管 Vue 前端，并将 `/api` 请求转发给后端。
- **backend**：Java 17 + Spring Boot 投票接口与实时通知服务。
- **database**：PostgreSQL 17 数据库，保存投票、选项和匿名访客的一票记录。

Docker Compose 是用于用一份配置文件同时管理多个容器的工具。

## 1. 部署前准备

目标机器需要安装：

- Git
- Docker Engine 24+ 和 Docker Compose v2

确认 Docker 可用：

```bash
docker --version
docker compose version
```

服务器需要允许访问部署端口。当前配置将前端服务发布在 `8088` 端口；若使用云服务器，还需要在安全组或防火墙中允许 TCP `8088` 入站访问。

## 2. 获取代码

```bash
git clone https://github.com/JackHaowen/vote.git
cd vote
```

以下命令均以仓库根目录下的 `deploy` 目录为工作目录：

```bash
cd deploy
```

## 3. 配置生产环境密钥和数据库密码

打开 `deploy/docker-compose.yml`，修改以下值：

```yaml
database:
  environment:
    POSTGRES_PASSWORD: 替换为高强度数据库密码

backend:
  environment:
    DATABASE_PASSWORD: 使用与 POSTGRES_PASSWORD 相同的值
    VISITOR_HMAC_SECRET: 替换为至少32字符的随机密钥
```

`VISITOR_HMAC_SECRET` 是 HMAC 密钥。HMAC（基于哈希的消息认证码）用于将浏览器生成的匿名访客标识转换为不可逆的服务端标识，从而在不保存原始浏览器标识的前提下实现“每位访客每个投票最多一票”。

可在 Linux 或 macOS 上使用以下命令生成密钥：

```bash
openssl rand -hex 32
```

请勿将生产密码或密钥提交到 Git 仓库。正式环境推荐将部署配置放在受控的密钥管理系统中，并限制只有部署账户可以读取。

## 4. 构建并启动

首次部署或代码更新后运行：

```bash
docker compose up --build -d
```

参数说明：

- `--build`：重新构建前端和后端镜像，确保使用当前代码。
- `-d`：后台运行容器，关闭终端后服务继续运行。

检查服务状态：

```bash
docker compose ps
```

预期可看到 `database`、`backend`、`frontend` 三个服务运行，其中数据库应显示为 `healthy`。Flyway 会在后端启动时自动执行数据库结构迁移；Flyway 是用于按版本管理数据库表结构变更的工具。

## 5. 访问与功能验证

访问地址：

```text
http://服务器地址:8088
```

按以下步骤验收：

1. 创建一个包含标题和三个选项的新投票。
2. 在普通浏览器窗口投票。
3. 用无痕窗口或另一浏览器打开同一个投票链接并投票。
4. 检查第一个窗口是否自动更新总票数和进度条。
5. 在同一浏览器改选其他选项，确认总票数不增加，仅选项票数变化。

无痕窗口使用独立的浏览器本地存储，可模拟另一位匿名访客。

## 6. 日常运维

查看所有服务日志：

```bash
docker compose logs -f
```

仅查看后端日志：

```bash
docker compose logs -f backend
```

停止服务但保留数据库数据：

```bash
docker compose down
```

更新代码并重新发布：

```bash
git pull --ff-only origin main
docker compose up --build -d
```

数据库数据保存在 Docker 卷 `postgres-data` 中。Docker 卷是由 Docker 管理的持久化存储，普通的容器重建或 `docker compose down` 不会删除其中的数据。

## 7. 备份与恢复

部署前先确认数据库容器名称：

```bash
docker compose ps
```

导出数据库备份：

```bash
docker compose exec -T database pg_dump -U vote vote > vote-backup.sql
```

恢复前应先停止后端，避免恢复过程被并发写入影响：

```bash
docker compose stop backend
docker compose exec -T database psql -U vote -d vote < vote-backup.sql
docker compose start backend
```

请定期将备份文件复制到独立于部署服务器的位置，并测试恢复流程。

## 8. 常见问题

### 8088 端口被占用

修改 `deploy/docker-compose.yml` 中前端端口映射，例如改为：

```yaml
ports: ["80:80"]
```

然后重新运行 `docker compose up -d`。若改为 80，访问地址不再需要写 `:8088`。

### 前端可访问但投票提交失败

先查看后端日志：

```bash
docker compose logs --tail=200 backend
```

确认 `database` 服务为 `healthy`，并检查 `DATABASE_PASSWORD` 与 `POSTGRES_PASSWORD` 是否一致。

### 两个浏览器的票数没有自动更新

确认两边打开的是同一投票链接，并检查浏览器开发者工具中的网络连接没有被代理、防火墙或负载均衡器中断。系统使用 SSE（Server-Sent Events）向浏览器推送结果变化；反向代理必须允许长连接且不能缓冲该响应。

### 不要执行的清理操作

不要在生产环境执行 `docker compose down -v`，其中 `-v` 会删除数据库卷，导致投票数据丢失。

## 9. 公网部署建议

- 在 Nginx、云负载均衡器或其他入口层配置 HTTPS。
- 将数据库端口限制在 Docker 内部网络，不要向公网发布 `5432`。
- 使用强随机密码和独立的 `VISITOR_HMAC_SECRET`。
- 定期更新基础镜像与宿主机安全补丁。
- 为日志、备份文件和 Docker 卷设置访问控制。
