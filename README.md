# 计算机技术组外包需求管理系统

面向校内师生、社团和授权用户的需求协作中心：提交问题、补充资料、查看进度并验收成果；技术组负责评估、分配、处理和管理。

当前代码包含完整需求闭环、需求方注册/账号开通、邮箱找回密码、站内通知、统计 CSV、成员负载与推荐，以及 GHCR 拉取式部署程序。生产注册与找回默认关闭，服务器自动部署须另行安装和启用。完整能力与限制见[需求文档](docs/需求文档.md)，全部文档见[文档导航](docs/README.md)。

## 阅读入口

| 要做的事                  | 文档                                                                       |
| ------------------------- | -------------------------------------------------------------------------- |
| 提交、补充和验收需求      | [需求方使用指南](docs/需求方使用指南.md)                                   |
| 开发、审查和本地检查      | [项目开发规范](docs/项目开发规范.md)、[前端实现说明](docs/前端实现说明.md) |
| 安排迭代、验收和发布      | [项目流程书](docs/项目流程书.md)                                           |
| 维护既有 Linux 生产服务器 | [服务器准备指南](docs/持续部署服务器准备指南.md)                           |
| 新建源码构建生产环境      | [源码构建部署与运维](docs/源码构建部署与运维.md)                           |
| 查看功能变化              | [CHANGELOG](CHANGELOG.md)                                                  |

## 技术与目录

前端使用 Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios 和 Lucide；后端使用 Spring Boot 3.5.9、MyBatis-Plus、Spring Security 与 Spring Session JDBC；数据库为 MySQL 8.4，迁移由 Flyway 管理。

```text
backend/                   Spring Boot API、测试与数据库迁移
frontend/                  Vue 页面、组件与测试
deploy/ci/                 GHCR 发布记录生成与校验
deploy/server/             拉取部署、告警、保留程序与 systemd 模板
deploy/nginx/              容器内 Nginx 配置
docs/                      用户、工程、部署与验收文档
design-system/             设计决策
.github/workflows/         CI 与镜像发布
docker-compose.yml         本地开发 MySQL（tech-request-dev）
docker-compose.prod.yml    源码构建生产栈（命名卷）
```

## 本地启动

需要 Git、JDK 21、Node.js 22.12.0 或更高兼容版本、pnpm 10、Docker 与 Compose v2。CI 当前使用 Node.js 22.12.0。后端使用仓库内 Maven Wrapper，无需全局 Maven。

以下 PowerShell 命令从仓库根目录开始；配置文件已存在时直接核对，勿覆盖本机设置。

### 1. MySQL 与后端

```powershell
docker compose up -d mysql
docker compose ps

Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
$env:SPRING_PROFILES_ACTIVE = "local"
$bootstrapAdminSecret = Read-Host "初始管理员密码（至少 12 位，含字母和数字）" -AsSecureString
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $bootstrapAdminSecret).Password
Remove-Variable bootstrapAdminSecret

Set-Location backend
.\mvnw.cmd spring-boot:run
```

默认数据库为 localhost:3306、库/用户 tech_request、密码 change-me-local，仅用于开发。端口冲突时设置 MYSQL_PORT 并同步本地 JDBC 地址。初始化账号默认 admin；已有管理员不会因修改环境变量而重置密码。停止后清理当前终端中的初始化密码：

```powershell
Remove-Item Env:APP_BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

### 2. 前端

另开终端，从仓库根目录执行：

```powershell
Set-Location frontend
Copy-Item .env.example .env.local
corepack enable
pnpm install --frozen-lockfile
pnpm dev
```

默认前端为 [localhost:5173](http://localhost:5173)，后端为 [localhost:8080](http://localhost:8080)。保持浏览器源与后端允许源一致；默认允许 localhost，不要随意换成 127.0.0.1 后混用会话。

- [健康检查](http://localhost:8080/actuator/health)
- [OpenAPI](http://localhost:8080/v3/api-docs)
- [Swagger UI](http://localhost:8080/swagger-ui.html)

本地配置和 env 文件不得提交。VITE_ 变量会进入浏览器，不可放秘密。生产默认关闭 OpenAPI/Swagger。

## 检查与发布

完整命令集中在[项目开发规范](docs/项目开发规范.md)。[CI](.github/workflows/ci.yml)检查后端 clean verify、前端格式/类型/lint/测试/构建，以及 Compose、部署脚本、发布记录和 systemd 模板。

[镜像发布工作流](.github/workflows/publish-images.yml)在 main push 的 CI 成功后发布前后端镜像，再发布固定 digest 的 production 记录；手动触发只发布应用镜像。服务器轮询与实际部署说明见[GHCR 发布端](docs/CD-pull-ghcr.md)和[服务器执行端](docs/CD-pull-agent.md)。

源码构建预检脚本为 [Test-ReleaseReadiness.ps1](deploy/Test-ReleaseReadiness.ps1)，默认要求干净工作区和真实生产环境配置。它不启动容器，也不能替代测试 MySQL 迁移、邮件送达、浏览器和备份恢复验收。

部署故障先从对应部署文档排查，保留现有数据库、附件和运行状态；不要通过删除数据卷或覆盖环境文件修复配置问题。
