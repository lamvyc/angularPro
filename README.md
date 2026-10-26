# MedFlow · 门诊诊疗管理平台

基于 **Angular 22 + Spring Boot 4.1** 的企业级门诊业务演示系统。覆盖预约挂号、医生工作站、电子病历、处方与药房库存，重点实现真实业务中的**状态机、并发控制、幂等、审计与数据一致性**，而不是一组孤立的 CRUD 页面。

> 全部数据为合成测试数据，不包含任何真实患者信息；不实现真实临床决策或处方推荐。

## 当前进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 需求 / 架构 / 数据库 / 流程 / 计划 + 前后端基础工程 + Compose + CI | ✅ 已完成 |
| M1 | 登录、角色权限、菜单/按钮/接口权限、科室数据隔离、审计 | ✅ 已完成 |
| M2 | 排班号源、并发预约、签到、退号退费 | ✅ 已完成 |
| M3 | 候诊队列、接诊状态机、草稿自动保存、乐观锁冲突 | ✅ 已完成 |
| M4 | 结构化病历模板、动态表单、签署、修订与历史版本 | 计划中 |
| M5 | 开方、审方、驳回重提、幂等提交、状态机 | 计划中 |
| M6 | 批次库存、FEFO 预占、发药退药、补偿与对账 | 计划中 |

设计文档： [需求](docs/requirements.md) · [架构](docs/architecture.md) · [数据库](docs/database-design.md) · [业务流程](docs/business-flows.md) · [开发计划](docs/development-plan.md) · [M1 实现说明](docs/modules/m1-iam.md) · [M2 实现说明](docs/modules/m2-appointment.md) · [M3 实现说明](docs/modules/m3-workstation.md)

## 技术栈（已锁定版本）

| 层 | 技术 |
| --- | --- |
| 前端 | Angular 22.2.3（standalone / signals / zoneless）、Angular Material 22.2.3、RxJS 7.8.2、TypeScript 6.0.2、Vitest 5.0.5 |
| 后端 | Java 21、Spring Boot 4.1.2、Spring Security 7.1.2、MyBatis-Plus 3.5.18、Flyway 12.4、springdoc 3.1.0 |
| 数据 | MySQL 8.4 LTS、Redis 8 |
| 工程化 | pnpm 12.11.2、Docker Compose、ESLint 10 / Prettier 3、GitHub Actions |

版本组合与兼容性验证记录见 [架构文档 3.2 节](docs/architecture.md#32-已执行的兼容性验证2026-10-26)。

## 快速开始

### 前置条件

- JDK **21**（`java -version` 确认）
- Node.js **≥ 24.15**（Angular 22 要求）
- pnpm **12.11.2**（`corepack enable` 后由 `packageManager` 自动获取）
- Docker + Compose（本地依赖）

### 1. 启动本地依赖

```bash
# 现代 Docker（Compose 插件）
docker compose -f infra/docker-compose.yml up -d

# 若本机只有独立二进制
docker-compose -f infra/docker-compose.yml up -d
```

MySQL 映射到宿主机 **3307**（避免和本机 3306 冲突），Redis 为 6379。

```bash
docker compose -f infra/docker-compose.yml ps
# medflow-mysql / medflow-redis 应为 healthy
```

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

- API： <http://localhost:8080>
- 健康检查： <http://localhost:8080/actuator/health>（本地 profile 展示 db/redis 详情）
- OpenAPI / Swagger： <http://localhost:8080/swagger-ui.html>
- Flyway 会自动执行 `V1__infrastructure.sql`，创建审计、幂等、序列与发件箱表。

> 除公开接口外，未携带令牌访问 `/api/v1/**` 返回 401 Problem Details；登录使用 `/api/v1/auth/login`。

### 3. 启动前端

```bash
# 仓库根目录
pnpm install
pnpm web:start
```

访问 <http://localhost:4200>。开发服务器把 `/api` 与 `/actuator` 代理到 `8080`。
登录页可一键填入演示账号，统一密码 `Medflow@2026`：

| 账号 | 角色 | 科室 | 数据范围 |
| --- | --- | --- | --- |
| `admin` | 系统管理员 | 行政管理 | ALL |
| `doctor1` | 医生 | 皮肤科 | DEPT |
| `nurse1` | 护士 | 皮肤科 | DEPT |
| `pharmacist1` | 药师 | 药房 | ALL |
| `cashier1` | 收费员 | 全科医学科 | DEPT |
| `nurse3` | 护士 | 全科医学科 | DEPT |

不同角色登录后菜单不同：医生/护士有医生工作站；医生/护士/收费员有预约挂号与排班管理；医生可接诊、开病历、下诊断，收费员可预约收费，护士可签到；管理员拥有用户、角色、科室与审计后台。

### 4. 测试与构建

```bash
# 前端：lint + Vitest + 生产构建
pnpm web:lint
pnpm web:test
pnpm web:build

# 后端：单元 + ArchUnit + Testcontainers 集成测试
cd backend && ./mvnw verify
```

集成测试会启动真实的 MySQL 8.4 与 Redis 容器，验证 Flyway、Actuator、安全基线与 Problem Details。

## 目录结构

```text
.
├── docs/                       # 5 份设计文档（需求/架构/数据库/流程/计划）
├── frontend/                   # Angular 22 应用
│   └── src/app/
│       ├── core/               # 布局、HTTP、权限、配置
│       ├── shared/             # 无业务依赖的 UI（图标等）
│       └── features/           # auth/dashboard/staff/admin（按业务域）
├── backend/                    # Spring Boot 4.1 应用
│   └── src/main/java/com/medflow/
│       ├── common/             # 配置、错误、JWT、数据范围、请求 ID
│       ├── auth/               # 登录、刷新令牌、当前用户、菜单
│       ├── iam/                # 用户、角色、权限、科室、审计
│       └── system/             # 系统探针
├── infra/                      # Docker Compose、MySQL 初始化
└── .github/workflows/ci.yml    # 前端 / 后端 / Compose 三组校验
```

后续模块按 `features/<domain>/{api,application,domain,infrastructure}`（后端）与 `features/<domain>/{data,state,ui}`（前端）扩展，规则见架构文档。

## 设计要点

1. **状态只能通过动作改变**：后端领域层维护显式状态迁移表，非法迁移返回 409；`status` 不出现在通用更新 DTO 中。
2. **防超卖不靠"先查后改"**：号源使用 `UPDATE ... WHERE booked_count < capacity` 原子条件更新，配唯一索引与幂等表三层防线。
3. **幂等**：写接口使用 `Idempotency-Key`，业务唯一键优先；重复请求回放首次结果，处理中返回 409。
4. **乐观锁**：病历/处方携带 `version`，冲突返回 409 与服务器最新数据，由用户合并。
5. **库存**：`stock_qty - reserved_qty` 条件更新 + FEFO 排序 + 整单回滚 + 流水唯一键，取消/过期通过幂等补偿释放预占。
6. **权限**：RBAC（菜单/按钮/接口）+ 科室数据范围；JWT 访问令牌 15 分钟、刷新令牌 7 天且一次性轮换；前端隐藏按钮仅为体验，后端每个写接口都重新校验。
7. **会话安全**：刷新令牌只存 SHA-256；重放旧令牌会吊销当前会话；登出使用 `jti` 拒绝名单；密码重置/停用用户吊销全部会话。
8. **表单**：登录与静态表单使用 Signal Forms，运行时生成的病历 Schema 使用 Reactive Forms + 自定义引擎。
9. **可观测**：每个请求带 `X-Request-Id`，贯穿日志、Problem Details 与审计；越权尝试也会记录 `ACCESS_DENIED`。

更多取舍见 [架构文档 ADR](docs/architecture.md#7-架构决策记录adr)。

## 常用脚本

| 命令 | 说明 |
| --- | --- |
| `pnpm web:start` | 启动 Angular 开发服务器（代理 API） |
| `pnpm web:build` | 生产构建 |
| `pnpm web:test` | Vitest（无 watch） |
| `pnpm web:lint` | ESLint |
| `pnpm api:run` | 启动 Spring Boot |
| `pnpm api:test` | 后端测试（使用 Maven Wrapper） |
| `pnpm infra:up` / `infra:down` | 启动/停止 MySQL、Redis |

## 演示数据与合规

- 种子数据将使用合成姓名、手机号与证件号；证件号仅存哈希与掩码。
- 代码与文档中不包含真实患者信息、真实药品处方建议或临床决策逻辑。
- 登录页与工作台均标注"演示环境 · 合成数据"。

## 已知环境说明

- MySQL 使用宿主机 **3307**；如需修改，复制 `infra/.env.example` 为 `infra/.env` 并调整 `MYSQL_PORT`，同时设置后端 `DB_PORT`。
- 若本机已有 Redis 占用 **6379**（如 Homebrew Redis），请停止本机 Redis，或改用 `REDIS_PORT=6380` 同时调整 Compose 与后端；否则应用会连到本机 Redis 而不是 Compose 容器。
- 若使用独立 `docker-compose` 二进制，将脚本中的 `docker compose` 替换为 `docker-compose`。
- Angular 22 的 `@angular/compiler-cli` 最高为 22.2.3，因此所有 Angular 包统一锁定 22.2.3，避免 patch 版本错配。

## 本阶段验证结果

| 项目 | 命令 / 方式 | 结果 |
| --- | --- | --- |
| 前端静态检查 | `pnpm --filter medflow-web lint` | 0 error |
| 前端单元测试 | `pnpm --filter medflow-web test` | 19 个文件 / 37 个用例通过 |
| 前端生产构建 | `pnpm --filter medflow-web build` | 成功，初始包 ≈ 315 kB（未压缩） |
| 后端单元 + 架构 + 集成测试 | `cd backend && ./mvnw verify` | 46 个用例通过（Testcontainers 真实 MySQL/Redis） |
| 本地依赖健康 | `docker compose ps` | MySQL / Redis 均 healthy |
| 后端运行 | `GET /actuator/health` | `UP`，db/redis 均 `UP` |
| 未登录访问 | `GET /api/v1/system/info` | `401` + `application/problem+json`（`MED-AUTH-001`） |
| 角色菜单 | 医生 `/api/v1/auth/me` | 工作台 / 科室人员 / 科室管理 / 预约挂号 / 排班管理 |
| 并发预约 | 50 线程抢 1 个号源 | 恰好 1 个 201，`booked_count=1`，无超卖 |
| 退号退款 | 收费后取消预约 | 号源释放，退款记录 SUCCESS，账单 REFUNDED |
| 签到 | 当日预约签到 | `CHECKED_IN`，创建 `encounter(WAITING)` |
| 接诊并发 | 两窗口同版本保存病历 | 一个 200、一个 409，版本只 +1 |
| 结束就诊 | 主诉 + 主诊断 | encounter/appointment 均 COMPLETED |
| 数据范围 | 医生 `/api/v1/staff/doctors` | 仅返回皮肤科张明；跨科室参数 403 |
| 越权审计 | 医生访问 `/api/v1/users` 后查审计 | `403`，审计出现 `ACCESS_DENIED` |
| API 文档 | `GET /v3/api-docs` | `200` |
| 前端代理 | `GET http://localhost:4200/actuator/health` | `200` |
