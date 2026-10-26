# M6 药房库存 · 实现说明

| 项 | 值 |
| --- | --- |
| 状态 | ✅ 已完成（2026-10-26） |
| 对应需求 | FR-M6-01 ~ FR-M6-09 |
| 后端 | V9 药品目录扩展、批次入库、发药扣减、退药回补/隔离、流水与对账 |
| 前端 | 药房发药/退药；库存页：目录、批次、汇总、流水、入库 |
| 测试 | 后端 57 个用例（4 个 M6 集成用例）；前端 40 个用例 |

## 1. 交付范围

- 药品目录：编码唯一、商品名/通用名/规格/单位/剂型/厂家/类别/价格/处方类型；停用不影响历史处方。
- 批次入库：批号 + 生产/有效期 + 数量；过期批次拒绝；同药品批号唯一；生成 `IN` 流水。
- 发药：仅 `APPROVED` 处方；按预占批次扣减实库存与预占量，生成 `DISPENSE` 流水；重复发药幂等。
- 退药：按原批次退，数量不得超过已发；回补库存；过期批次自动转 `QUARANTINE`；生成 `RETURN` 流水。
- 库存汇总：实存、预占、可用量、批次数；流水按时间倒序。
- 前端：药房页发药/退药；库存页药品目录、批次、汇总、流水与入库。

## 2. API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/drugs/admin` | `drug:manage` | 全部药品 |
| POST | `/api/v1/drugs` | `drug:manage` | 新建药品 |
| PUT | `/api/v1/drugs/{id}` | `drug:manage` | 编辑药品 |
| POST | `/api/v1/drugs/{id}/status` | `drug:manage` | 启用/停用 |
| GET | `/api/v1/stock/batches` | `stock:read` | 批次列表 |
| POST | `/api/v1/stock/batches` | `stock:manage` | 批次入库 |
| GET | `/api/v1/stock/summary` | `stock:read` | 库存汇总 |
| GET | `/api/v1/stock/movements` | `stock:read` | 库存流水 |
| GET | `/api/v1/prescriptions/{id}/reservations` | `pharmacy:dispense` | 发药/退药批次明细 |
| POST | `/api/v1/prescriptions/{id}/dispense` | `pharmacy:dispense` | 发药 |
| POST | `/api/v1/prescriptions/{id}/returns` | `pharmacy:return` | 退药，幂等键必填 |

## 3. 发药与库存一致性

```sql
UPDATE drug_batch
SET stock_qty = stock_qty - ?,
    reserved_qty = reserved_qty - ?,
    status = CASE WHEN stock_qty - ? = 0 THEN 'DEPLETED' ELSE status END
WHERE id = ? AND status = 'ACTIVE'
  AND stock_qty >= ? AND reserved_qty >= ?;
```

- 条件更新失败 → 抛库存不足，整个发药事务回滚，处方保持 `APPROVED`。
- 处方状态 `APPROVED → DISPENSED` 条件更新，重复发药直接返回已发药状态，不重复扣减。
- 每个预占明细生成 `DISPENSE` 流水；`before/after` 为可用量变化。
- `CHECK (stock_qty >= 0 AND reserved_qty >= 0 AND reserved_qty <= stock_qty)` 数据库兜底。

## 4. 退药

- 仅已发药处方；每行 `reservationId + quantity`。
- `returned_qty + quantity <= quantity` 条件更新；超退返回 422。
- 批次回补实库存；若 `expiry_date < CURRENT_DATE`，状态置 `QUARANTINE`。
- `Idempotency-Key` 保证重复退药请求只回补一次；流水 `idem_key` 唯一兜底。
- 退药不改变处方 `DISPENSED` 状态，审计与流水保留。

## 5. 对账

- 任意时刻 `available = stock_qty - reserved_qty`，数据库 CHECK 保证非负。
- 汇总 `stock/summary` 按药品汇总；流水 `stock/movements` 追加式。
- 入库、预占、释放、发药、退药均写流水；M5 预占/释放已在 V8 建立。
- 过期/取消释放预占沿用 M5 事务。

## 6. 前端

```text
features/pharmacy/ui/
  pharmacy-page        审方 + 已通过发药 + 已发药退药
  return-dialog        按发药批次退药
  inventory-page       目录、批次、汇总、流水
  inventory-dialogs    药品维护、批次入库
```

- 药房菜单 `/pharmacy` 与库存子页 `/pharmacy/inventory`。
- 医生/收费员/管理员不可访问库存与发药。
- 停用药品从开方目录消失，但历史处方明细快照不受影响。

## 7. 验证证据

| 验证 | 结果 |
| --- | --- |
| `mvn clean verify` | 57 个后端测试通过 |
| `PharmacyInventoryTest` | 4/4：发药/重复发药/退药幂等/超退；入库/过期拒绝；过期退药隔离；目录与权限 |
| 重复发药 | 第二次返回 200，库存不再扣减 |
| 退药幂等 | 同键重复只回补一次；超退 422 |
| 过期退药 | 批次状态变 `QUARANTINE` |
| 前端 | 40 个测试通过；lint/build 通过 |
| 手工链路 | 发药扣减 → 退药回补 → 入库 → 过期入库 422；库存页可见 |

## 8. 已知边界

- 未实现盘点/手工调整流水（`ADJUST`）；可在 M6 后续补充。
- 批次隔离后提供解封接口未实现，当前只能通过数据库运维处理。
- 发药一次完成，无“部分发药”状态；缺货必须整单取消/重新审方。
- 流水 `before/after` 为可用量，不是批次实存绝对量；对账以汇总与批次表为准。
- 退药不自动退款；退费属于收费模块范围，未在 M6 合并。

## 9. 完成状态

M1–M6 主链路已闭环：登录/权限 → 建档/预约 → 接诊/病历签署 → 处方/审方 → 预占/发药/退药。
后续工作为演示数据、可观测性、性能基准与部署加固。
