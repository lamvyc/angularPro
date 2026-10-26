# M5 处方与审方 · 实现说明

| 项 | 值 |
| --- | --- |
| 状态 | ✅ 已完成（2026-10-26） |
| 对应需求 | FR-M5-01 ~ FR-M5-09 |
| 后端 | V8 处方/明细/修订/审核/事件 + 最小药品批次库存/FEFO 预占 |
| 前端 | 工作站开方/提交/取消；药房待审队列、开始审方、通过预占、驳回 |
| 测试 | 后端 53 个用例（5 个 M5 集成用例）；前端 39 个用例 |

## 1. 交付范围

- 医生在 `IN_PROGRESS` 就诊中创建处方，多药品明细；剂量、频次、途径、天数、数量必填。
- 草稿可编辑；提交生成不可变修订快照与 SHA-256，修订号 +1。
- 提交使用 `Idempotency-Key`，重复提交返回同一处方与修订号。
- 药师开始审核 → 通过/驳回；驳回必须填原因。
- 驳回后医生修改并重提，修订号 +1，历史审核记录保留。
- 通过时按 FEFO（先过期先出）跨批次预占；库存不足整单失败，处方保持 `UNDER_REVIEW`，库存不变。
- 取消：未发药可取消；`APPROVED` 取消释放预占。
- 通过后 72 小时未发药，定时任务置 `EXPIRED` 并释放预占。
- 全部动作写审计与处方事件；审核记录绑定修订号。

## 2. API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/drugs` | `drug:read` | 启用药品目录 |
| POST | `/api/v1/prescriptions/encounters/{id}` | `prescription:create` | 创建草稿 |
| PUT | `/api/v1/prescriptions/{id}` | `prescription:create` | 修改草稿/驳回处方 |
| POST | `/api/v1/prescriptions/{id}/submit` | `prescription:submit` | 提交，幂等键必填 |
| GET | `/api/v1/prescriptions` | `prescription:read` | 按就诊/状态查询 |
| GET | `/api/v1/prescriptions/{id}` | `prescription:read` | 处方详情 |
| POST | `/api/v1/prescriptions/{id}/review/start` | `pharmacy:review` | 开始审方 |
| POST | `/api/v1/prescriptions/{id}/review/approve` | `pharmacy:review` | 通过并预占 |
| POST | `/api/v1/prescriptions/{id}/review/reject` | `pharmacy:review` | 驳回 |
| POST | `/api/v1/prescriptions/{id}/cancel` | `prescription:cancel` / `pharmacy:review` | 取消 |

## 3. 状态机

```text
DRAFT ──submit──▶ SUBMITTED ──startReview──▶ UNDER_REVIEW
  │                   │                         │
  │                   │                         ├──approve──▶ APPROVED ──expire──▶ EXPIRED
  │                   │                         └──reject───▶ REJECTED ──submit──▶ SUBMITTED
  └───────────────────┴────────cancel──────────▶ CANCELLED ◀──cancel── APPROVED（释放预占）
```

所有状态迁移使用 `UPDATE ... WHERE id=? AND version=? AND status=?`；影响行数为 0 返回 409。

## 4. 库存预占（FEFO）

- 查询药品 `ACTIVE`、未过期、`stock_qty > reserved_qty` 的批次，按 `expiry_date, id` 升序。
- 逐批 `UPDATE drug_batch SET reserved_qty=reserved_qty+? WHERE stock_qty-reserved_qty>=?`；条件不满足则跳过该批次。
- 每个批次生成 `stock_reservation(HELD)` 与 `stock_movement(RESERVE)`。
- 任一明细无法满足 → 抛 `MED-STOCK-001`，整个事务回滚：处方仍 `UNDER_REVIEW`，已预占批次与预占行全部撤销。
- 取消/过期：逐条 `HELD → RELEASED`，减少 `reserved_qty`，写 `RELEASE` 流水；释放幂等。
- M5 只提供最小药品/批次库存用于预占；M6 完善目录、入库、发药、退药、盘点和流水页面。

## 5. 前端

- 工作站新增处方区：新建/编辑草稿、提交、取消；显示状态、修订号、驳回原因。
- 药房 `/pharmacy`：待审方/审核中/已通过三类队列；开始审方、通过并预占、驳回。
- 医生只看到自己的处方；药师可查看所有处方；收费员/管理员无处方权限。
- 药房菜单由 V8 权限数据驱动。

## 6. 验证证据

| 验证 | 结果 |
| --- | --- |
| `mvn clean verify` | 53 个后端测试通过 |
| `PrescriptionFlowTest` | 5/5：提交幂等/审核预占/取消释放；驳回重提；库存不足回滚；过期释放；角色范围 |
| 库存不足 | 处方保持 `UNDER_REVIEW`，无预占行，批次 `reserved_qty` 不变 |
| 过期任务 | 手动将 `expires_at` 置为过去，`expireDue()` 后 `EXPIRED` 且预占释放 |
| 前端 | 39 个测试通过；lint/build 通过 |
| 手工链路 | 开方 → 提交 → 药师审核通过 → 批次预占；药房菜单可见 |

## 7. 已知边界

- 发药/退药与库存扣减在 M6 实现；M5 只做预占与释放。
- 药品目录/批次入库在 M6 提供管理界面；V8 仅内置 3 种药品用于演示。
- 库存流水 `before_qty/after_qty` 记录可用量变化，M6 对账页面统一展示。
- 处方类型前端固定 `WESTERN`；后端支持 `HERBAL/TREATMENT` 枚举但未提供不同表单。
- 审核通过后无自动发药；72 小时过期任务每 5 分钟扫描一次。

## 8. 下一步（M6）

药品目录、批次入库、FEFO 发药、退药、库存对账与流水；发药幂等与条件扣减，库存不足不出现负库存。
