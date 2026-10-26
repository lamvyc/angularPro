# M2 预约挂号 Implementation Plan

**Goal:** 完成患者建档、医生排班、号源生成、并发预约、签到、取消/退号退费与会话闭环。

**Architecture:** 单库本地事务；号源 `UPDATE ... WHERE booked_count < capacity` 原子扣减；`appointment` 生成列唯一索引兜底重复预约；`Idempotency-Key` + `sys_idempotency_record` 保证重复提交回放；签到创建 `encounter(WAITING)` 供 M3 接诊；审计与业务同事务。

**Tech Stack:** Spring Boot 4.1 / MyBatis / MySQL 8.4 / Angular 22 / Vitest。

**Spec:** `docs/requirements.md` M2、`docs/business-flows.md` 2、`docs/database-design.md` 4.4。

## 验收

- 50 并发抢 1 个号源，恰好 1 个成功，`booked_count=1`，无超卖。
- 同一 `Idempotency-Key` 重放返回同一预约；同患者同号源/同医生同日重复预约 409。
- 取消释放号源；已支付挂号费生成退款并最终 `REFUNDED`；未支付账单取消。
- 护士签到创建 `encounter(WAITING)`；非当日/非法状态 409。
- 医生只见本科室排班/预约；药师无预约权限；管理员无临床权限。
- 前端可完成 建档→选号→预约→签到→收费→退号 的主链路。
- 后端全量测试、前端 lint/test/build 通过。

## Tasks

1. V4 迁移：患者、排班、号源、预约、收费、退款、encounter + 菜单权限。
2. 患者 API + 搜索/建档。
3. 排班 API：创建并生成号源、查询、关闭。
4. 预约 API：幂等预约、原子扣号、列表、取消/退款、签到、过号任务。
5. 集成测试：并发防超卖、幂等、重复、退号、签到、数据范围。
6. 前端：预约/排班页面、患者快速建档、签到/收费/退号动作。
7. 文档/验证/评审。

## 执行记录

- 完成于 2026-10-26，主分支；后端 42 个测试、前端 35 个测试通过。
- 实际追加：V4 同时创建最小 encounter 表并提供护士 nurse3（全科），保证签到链路可演示；M3 只追加诊断/事件表。
- 退款为本地模拟同步成功，退款单状态 SUCCESS，账单 REFUNDED；真实网关异步重试留到后续。
