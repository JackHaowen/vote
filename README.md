# 匿名投票系统

前端位于 `frontend`，使用 Vue 3；后端位于 `backend`，使用 Java 17 和 Spring Boot；数据库结构由 Flyway 管理。

## 本地启动

1. 启动 PostgreSQL，创建数据库 `vote`、账号 `vote` 和密码 `vote`，或在 `deploy` 目录运行 `docker compose up --build`。
2. 在 `backend` 运行 `mvn spring-boot:run`。
3. 在 `frontend` 运行 `npm install` 后执行 `npm run dev`，访问终端输出的网址。

生产环境必须通过密钥管理系统设置 `VISITOR_HMAC_SECRET` 和数据库连接信息，不能使用仓库中的开发默认值。
