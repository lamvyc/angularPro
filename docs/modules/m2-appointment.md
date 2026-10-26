# M2 预约挂号 · 实现说明

| 项 | 值 |
| --- | --- |
| 状态 | ✅ 已完成（2026-10-26） |
| 对应需求 | FR-M2-01 ~ FR-M2-10 |
| 后端 | V4 迁移、原子号源扣减、生成列唯一索引、幂等表、退款记录、签到建 encounter |
| 前端 | 预约/排班页面、患者快速建档、签到/收费/退号操作 |
| 测试 | 后端 42 个用例（6 个 M2 集成用例，含 50 并发抢号）、前端 35 个用例 |

## 1. 交付范围

- 患者建档与检索（证件号只存 SHA-256 哈希 + 掩码）。
- 医生排班创建、号源生成、排班查询/关闭。
- 预约：幂等提交、原子扣号、重复预约拦截。
- 签到：`BOOKED → CHECKED_IN`，创建 `encounter(WAITING)`。
- 退号：释放号源；已支付挂号费生成退款并置 `REFUNDED`；未支付账单取消。
- 日终过号任务：`BOOKED/CHECKED_IN` 且日期已过 → `NO_SHOW`。
- 前端：按日期选排班 → 选号源 → 搜索/新建患者 → 预约；列表内签到/收费/退号。
- 菜单：`/appointments`、`/schedules`，由 V4 权限数据驱动，按角色可见。

## 2. API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/patients` | `patient:read` | 分页检索 |
| POST | `/api/v1/patients` | `patient:create` | 建档，证件号查重 |
| POST | `/api/v1/schedules` | `schedule:manage` | 创建排班并生成号源（仅医生本人） |
| GET | `/api/v1/schedules?date=` | `schedule:read` | 排班与余号，按科室范围 |
| GET | `/api/v1/schedules/{id}/slots` | `schedule:read` | 号源列表 |
| POST | `/api/v1/schedules/{id}/close` | `schedule:manage` | 关闭本人排班 |
| POST | `/api/v1/appointments` | `appointment:create` | 预约，`Idempotency-Key` 必填 |
| GET | `/api/v1/appointments?date=` | `appointment:read` | 预约列表，按科室范围 |
| POST | `/api/v1/appointments/{id}/cancel` | `appointment:cancel` | 退号 + 自动退款 |
| POST | `/api/v1/appointments/{id}/check-in` | `appointment:checkin` | 签到并创建 encounter |
| POST | `/api/v1/charges/{id}/pay` | `charge:collect` | 挂号收费，重复收费幂等 |

## 3. 并发与一致性

### 3.1 防超卖

```sql
UPDATE schedule_slot
SET booked_count = booked_count + 1, version = version + 1
WHERE id = ? AND status = 'OPEN' AND booked_count < capacity;
```

- `affected = 0` → `409 MED-SLOT-001`，不依赖“先查再改”。
- `CHECK (booked_count >= 0 AND booked_count <= capacity)` 兜底。
- 取消时 `UPDATE ... SET booked_count = booked_count - 1 WHERE booked_count > 0`。
- **测试证据**：50 个线程抢 1 个号源，最终恰好 1 条 `201`、`booked_count=1`、`appointment` 表 1 行。

### 3.2 重复预约

- `active_patient_id` 生成列 + `UNIQUE(slot_id, active_patient_id)`：同一患者同一号源只允许一条活动预约，终态自动变 `NULL`，取消后可重新预约。
- `active_doctor_day_key` 生成列 + 唯一索引：同一患者同医生同半天不可重复。
- 服务层先查活动预约返回 `MED-SLOT-002`（即使号源已被自己占满）；并发下由唯一索引兜底，捕获 `DuplicateKeyException` 转为同一错误码。

### 3.3 幂等提交

- 客户端生成 `Idempotency-Key`；`sys_idempotency_record` 唯一 `(user_id, idem_key)`。
- `IN_PROGRESS` 记录使用独立事务提交，业务事务失败写 `FAILED`，成功后写 `COMPLETED + response_body`。
- 同键同体重放 → 返回首次响应（HTTP 200）；同键异体 → 409；处理中 → `MED-IDEM-001`。
- 前端每次点击生成 UUID，网络重试复用同一个 key（后续可持久化到重试队列）。

### 3.4 退号退款

- `BOOKED → CANCELLED` 条件更新 + 释放号源。
- 已支付：插入 `refund_record(SUCCESS)`（模拟支付网关同步成功），`charge_order → REFUNDED`；重复取消直接返回已取消预约，不重复退款。
- 未支付：`charge_order → CANCELLED`。
- `refund_record.idem_key = refund:REGISTRATION:{appointmentId}` 唯一索引防重复。

### 3.5 签到

- 只有就诊当日、`BOOKED` 状态可签到；护士/医生数据范围校验。
- 创建 `encounter(WAITING)`，回填 `appointment.encounter_id`；重复签到 409。
- M3 将基于该 encounter 实现接诊/病历/诊断。

### 3.6 过号

- `@Scheduled(cron = "0 5 0 * * *", zone="Asia/Shanghai")` 将日期早于今天且仍为 `BOOKED/CHECKED_IN` 的预约置为 `NO_SHOW`。
- 不释放号源（历史号源保留占用量）；审计记录 `APPOINTMENT_NO_SHOW_JOB`。

## 4. 数据与权限

V4 新增表：`patient`、`doctor_schedule`、`schedule_slot`、`appointment`、`charge_order`、`refund_record`、`encounter`（最小版本）。

- 菜单：`menu:appointment`、`menu:schedule`，授予 DOCTOR/NURSE/CASHIER。
- 患者主档为全院共享；预约/排班查询按 `DataScopeResolver` 强制科室过滤。
- 医生只能创建/关闭本人排班；药师/管理员无预约权限（403）。
- 前端按钮权限与后端注解双重校验；请求仍由后端重新鉴权。

## 5. 前端

```text
features/appointments/
  data/      models + AppointmentApi
  state/     AppointmentStore（signals + RxJS）
  ui/        appointments-page + 患者建档/取消对话框
features/schedules/
  ui/        schedules-page（医生发布/关闭排班）
```

- 预约页：选日期 → 排班卡片 → 号源 chip → 搜索/快速建档患者 → 确认预约；列表内签到/收费/退号。
- 收费员只看到本科室排班；医生进入预约页可查看本科室预约，但没有 `appointment:create` 按钮。
- 前端不缓存号源余量用于判断成功；以后端结果为准，失败刷新列表。

## 6. 验证证据

| 验证 | 结果 |
| --- | --- |
| `mvn clean verify` | 42 个后端测试通过 |
| `AppointmentFlowTest` | 6/6：排班号源、幂等/重复、50 并发、取消退款、签到、角色/科室 |
| `pnpm --filter medflow-web test` | 35 个前端测试通过 |
| `pnpm --filter medflow-web lint` | 0 error |
| `pnpm --filter medflow-web build` | 成功 |
| 手工链路 | 医生建排班 → 收费员建档/预约 → 收费 → 退号退款 → 号源释放；护士签到生成 encounter |

## 7. 已知边界

- 退款为模拟同步成功；真实支付网关需要异步重试、对账与回调验签。
- 预约列表暂未分页（按日期过滤，演示数据量小）。
- 同一患者同医生同半天的限制按需求实现；跨天复诊不受影响。
- 患者检索暂未做科室级数据隔离（患者主档为全院共享），预约/排班和就诊数据已隔离。
- 过号任务未在测试中触发定时器，仅通过服务方法可测；后续加调度集成测试。

## 8. 下一步（M3）

复用 `encounter(WAITING)`、预约状态与科室范围，实现候诊队列、接诊状态机、病历草稿自动保存与乐观锁冲突。
