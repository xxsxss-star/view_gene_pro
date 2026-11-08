# View Gene Pro

图片素材制作网站，当前完成到多阶段实施方案的阶段 2（数据库与后端公共基础）。当前已具备工程骨架、核心表结构、种子数据、MyBatis 基础查询和统一响应/异常能力，但尚未开放登录、模板、上传或任务业务接口。

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

首次连接空的 `view_gene` 数据库时，Flyway 会创建六张核心表并写入两个演示用户、两个分类和三个模板。演示用户为 `demo_user`、`demo_user_b`，开发演示密码均为 `demo123`；阶段 3 才会实现登录接口。

如需显式运行本机 MySQL 集成测试，请先在当前 PowerShell 设置数据库环境变量，再执行：

```powershell
$env:RUN_MYSQL_INTEGRATION_TESTS='true'
.\mvnw.cmd '-Dtest=DatabaseFoundationIntegrationTest' test
```

## 配置约定

完整键清单见 [`.env.example`](./.env.example)。Spring Boot 支持环境变量覆盖数据库、JWT、上传目录及模拟任务配置。仓库只包含开发默认值和占位密钥，不包含真实凭据。

## 当前边界

- 已完成：前后端工程入口、六张核心表、种子数据、原创演示素材、领域模型、MyBatis 基础查询、统一响应/异常、requestId 和 UTC 时间序列化。
- 未完成：认证、业务 Controller、素材上传、任务及异步执行，按阶段 3 以后实施。
- Docker Compose、前后端 Dockerfile 和 CI 暂缓；用户当前明确不准备 Docker，最终交付在阶段 10 补齐。
