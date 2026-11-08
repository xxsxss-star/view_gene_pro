# View Gene Pro

图片素材制作网站，当前完成到多阶段实施方案的阶段 7（三阶段异步执行）。当前已具备登录认证、模板浏览、图片上传、任务创建与查询，以及可配置的后台模拟制作流程。

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

模板接口：

- `GET /api/v1/templates`：读取启用分类和模板，可用 `categoryCode` 筛选。
- `GET /api/v1/templates/{templateId}`：读取模板图片数量、格式和大小要求。

模板读取接口及 `/images/**` 演示资源公开访问；模板页面仍要求用户先登录。模板数据来自 MySQL，修改数据库中的启用模板后无需改动前端代码。

素材接口：

- `POST /api/v1/assets`：使用 `multipart/form-data` 上传 `templateId` 和 `file`。
- `GET /api/v1/assets/{assetId}/content`：仅允许素材所属用户读取图片内容。

上传文件默认保存在后端运行目录的 `uploads` 文件夹，可通过 `UPLOAD_DIR` 指定绝对目录。服务端根据文件头识别 JPG/PNG，不信任扩展名或浏览器声明的类型；存储名使用 UUID，数据库写入失败时会删除已经落盘的文件。

任务接口：

- `POST /api/v1/tasks`：按前端槽位顺序提交 `templateId` 和 `assetIds`，事务写入任务及素材关联。
- `GET /api/v1/tasks`：查询当前登录用户的任务，可使用 `status` 参数筛选。
- `GET /api/v1/tasks/{taskId}`：查询当前登录用户的单个任务。

创建任务时会再次校验模板要求、素材数量和重复项，并确认素材属于当前用户、状态和文件均有效。任务事务提交后，后台有界线程池依次执行 `MATERIAL_CHECK → GENERATING → FINALIZING`；每次状态变化使用独立短事务，创建接口不会等待模拟制作完成，也暂不承诺严格幂等。线程池拒绝任务时会把已创建记录补偿为 `FAILED / TASK_QUEUE_FULL`，接口返回 503 和 `taskId`。

### 模拟成功、失败和耗时

默认执行时间分别为 1 秒、3 秒和 1 秒。可在启动后端前通过 PowerShell 调整，单阶段范围为 0～60000 毫秒：

```powershell
$env:SIMULATION_MATERIAL_CHECK_DELAY_MS='1000'
$env:SIMULATION_GENERATING_DELAY_MS='3000'
$env:SIMULATION_FINALIZING_DELAY_MS='1000'
$env:SIMULATION_FAILURE_STAGE='NONE'
```

`SIMULATION_FAILURE_STAGE` 支持 `NONE`、`MATERIAL_CHECK`、`GENERATING`、`FINALIZING`。例如设置为 `GENERATING` 后，新建任务会稳定地在模拟生成阶段失败；改回 `NONE` 后恢复成功路径。配置在后端启动时读取，修改环境变量后需要重启后端。

JWT 当前只提供 8 小时访问令牌，不提供刷新令牌或服务端撤销列表。因此退出登录只会清除本地令牌，已签发令牌在过期前理论上仍可使用；生产环境应评估 HttpOnly Secure Cookie/BFF、令牌撤销和更严格的密钥管理。

如需显式运行本机 MySQL 集成测试，请先在当前 PowerShell 设置数据库环境变量，再执行：

```powershell
$env:RUN_MYSQL_INTEGRATION_TESTS='true'
.\mvnw.cmd '-Dtest=DatabaseFoundationIntegrationTest' test
```

## 配置约定

完整键清单见 [`.env.example`](./.env.example)。Spring Boot 支持环境变量覆盖数据库、JWT、上传目录及模拟任务配置。仓库只包含开发默认值和占位密钥，不包含真实凭据。

## 当前边界

- 已完成：阶段 1～7，包括工程入口、数据库公共基础、登录认证、模板浏览、图片上传、任务受理与查询、有界线程池和三阶段异步模拟执行。
- 未完成：前端多任务轮询、刷新恢复与后续交付能力，按阶段 8 以后实施。
- Docker Compose、前后端 Dockerfile 和 CI 暂缓；用户当前明确不准备 Docker，最终交付在阶段 10 补齐。
