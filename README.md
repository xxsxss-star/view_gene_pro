# View Gene Pro

图片素材制作网站，当前完成到多阶段实施方案的阶段 1（工程骨架与基础设施）。本阶段只有最小首页、健康检查和工程配置，不包含登录、模板、上传或任务业务。

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

访问 `http://localhost:8080/actuator/health` 检查健康状态。应用启动时会连接 MySQL 并运行 Flyway；阶段 2 才会加入业务表迁移。

## 配置约定

完整键清单见 [`.env.example`](./.env.example)。Spring Boot 支持环境变量覆盖数据库、JWT、上传目录及模拟任务配置。仓库只包含开发默认值和占位密钥，不包含真实凭据。

## 当前边界

- 已完成：前后端工程入口、基础目录、构建/测试脚本、统一 API 地址、Actuator 健康端点、数据库/Flyway 配置。
- 未完成：数据库表、认证、模板、素材上传、任务及异步执行，按阶段 2 以后实施。
- Docker Compose、前后端 Dockerfile 和 CI 暂缓；用户当前明确不准备 Docker，最终交付在阶段 10 补齐。
