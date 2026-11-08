# View Gene Pro

图片素材制作网站，当前完成到多阶段实施方案的阶段 3（登录认证闭环）。当前已具备数据库预置用户登录、BCrypt 密码校验、JWT Bearer 认证、当前用户恢复、退出登录和前端路由保护；模板、上传和任务业务仍按后续阶段实施。

## 固定技术基线

| 项目 | 版本/约定 |
|---|---|
| Node.js | 24.x |
| npm | 11.x，使用 `package-lock.json` |
| Next.js | 16.4.5（App Router） |
| React | 19.2.0 |
| Java | 17 |
| Spring Boot | 3.5.7 |
| MyBatis Spring Boot Starter | 3.0.5 |
| MySQL | 8.x，本地端口 `3306` |
| 前端端口 | `3000` |
| 后端端口 | `8080` |

前端样式使用 CSS Modules/全局 CSS，不在阶段 1 引入组件库。Windows 本地开发使用 PowerShell；当前阶段不要求 Docker。

## 目录

```text
frontend/             Next.js 前端
backend/              Spring Boot 后端
docs/ai-generated/    技术方案和实施计划
.env.example          环境变量清单
pom.xml               根 Maven 聚合入口（包含 backend）
```

## Windows 本地准备

需要 Node.js 24、npm 11、JDK 17、MySQL 8。先在 MySQL 中创建空库和开发账号（不要把真实密码提交到仓库）：

```sql
CREATE DATABASE view_gene CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'view_gene'@'localhost' IDENTIFIED BY 'your-local-password';
GRANT ALL PRIVILEGES ON view_gene.* TO 'view_gene'@'localhost';
```

在 PowerShell 中设置本次终端使用的配置：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/view_gene?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USERNAME='view_gene'
$env:DB_PASSWORD='your-local-password'
$env:JWT_SECRET='replace-with-at-least-32-random-characters'
$env:JWT_EXPIRATION_SECONDS='28800'
```

## 前端

```powershell
cd frontend
npm install
npm run lint
npm run type-check
npm test
npm run build
npm run dev
```

浏览 `http://localhost:3000`。API 基础地址默认是 `http://localhost:8080/api/v1`，可用 `NEXT_PUBLIC_API_BASE_URL` 覆盖。

## 后端

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

访问 `http://localhost:8080/actuator/health` 检查健康状态。应用启动时会连接 MySQL，并由 Flyway 自动执行尚未应用的迁移。

首次连接空的 `view_gene` 数据库时，Flyway 会创建六张核心表并写入两个演示用户、两个分类和三个模板。演示用户为 `demo_user`、`demo_user_b`，开发演示密码均为 `demo123`。后端启动后可在 `http://localhost:3000/login` 登录。

认证接口：

- `POST /api/v1/auth/login`：用户名和密码登录。
- `GET /api/v1/auth/me`：携带 `Authorization: Bearer <token>` 恢复当前用户。
- `POST /api/v1/auth/logout`：返回 204，由前端清除令牌和用户级查询缓存。

JWT 当前只提供 8 小时访问令牌，不提供刷新令牌或服务端撤销列表。因此退出登录只会清除本地令牌，已签发令牌在过期前理论上仍可使用；生产环境应评估 HttpOnly Secure Cookie/BFF、令牌撤销和更严格的密钥管理。

如需显式运行本机 MySQL 集成测试，请先在当前 PowerShell 设置数据库环境变量，再执行：

```powershell
$env:RUN_MYSQL_INTEGRATION_TESTS='true'
.\mvnw.cmd '-Dtest=DatabaseFoundationIntegrationTest' test
```

## 配置约定

完整键清单见 [`.env.example`](./.env.example)。Spring Boot 支持环境变量覆盖数据库、JWT、上传目录及模拟任务配置。仓库只包含开发默认值和占位密钥，不包含真实凭据。

## 当前边界

- 已完成：阶段 1～3，包括工程入口、数据库公共基础、预置用户、BCrypt、JWT、认证 API、登录页、身份恢复、退出和受保护路由。
- 未完成：模板业务 Controller、素材上传、任务及异步执行，按阶段 4 以后实施。
- Docker Compose、前后端 Dockerfile 和 CI 暂缓；用户当前明确不准备 Docker，最终交付在阶段 10 补齐。
