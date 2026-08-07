# 计算机技术组外包需求管理系统

面向校内需求方和计算机技术组成员的需求管理平台。P0 范围包括账号登录、需求提交、技术评估、任务分配、进度更新、交付验收、后台管理和基础统计。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router。
- 后端：JDK 21、Spring Boot 3.5、Spring Security、Spring Session JDBC。
- 数据访问：MyBatis-Plus、MySQL 8、Flyway。
- 部署：Docker Compose、Nginx。

## 目录

- `frontend`：Vue 前端。
- `backend`：Spring Boot API。
- `docs`：需求、流程和开发规范。
- `deploy`：Docker 与 Nginx 配置。

## 环境要求

- Node.js 22 LTS
- pnpm 10
- JDK 21
- Docker Desktop
- 后端统一使用仓库内 Maven Wrapper

当前电脑检测到的 Node.js 和 Java 版本不代表项目基线。请按上述要求配置，尤其需要把 `JAVA_HOME` 指向有效的 JDK 21 目录。

## 本地启动

1. 启动 MySQL：

   ```powershell
   docker compose up -d mysql
   ```

2. 创建后端本地配置：

   ```powershell
   Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
   ```

3. 启动后端：

   ```powershell
   Set-Location backend
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. 启动前端：

   ```powershell
   Set-Location frontend
   Copy-Item .env.example .env.local
   pnpm install --frozen-lockfile
   pnpm dev
   ```

首次尚未生成锁文件时使用 `pnpm install`，随后必须提交并保留 `pnpm-lock.yaml`。

## 默认地址

- 前端：http://localhost:5173
- 后端：http://localhost:8080
- 健康检查：http://localhost:8080/actuator/health
- OpenAPI：http://localhost:8080/v3/api-docs
- Swagger UI：http://localhost:8080/swagger-ui.html

## 开发顺序

先完成数据库迁移、认证权限和统一响应，再依次实现需求提交、评估、分配、进度、交付验收、管理和统计。详细规则见 `docs`。
