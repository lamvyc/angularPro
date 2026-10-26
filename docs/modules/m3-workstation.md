# M3 医生工作站 · 实现说明

| 项 | 值 |
| --- | --- |
| 状态 | ✅ 已完成（2026-10-26） |
| 对应需求 | FR-M3-01 ~ FR-M3-10 |
| 后端 | V5 诊断/事件/病历草稿、状态机、乐观锁、Redis 草稿副本 |
| 前端 | 候诊队列、接诊、SOAP 草稿自动保存、诊断编辑、结束就诊 |
| 测试 | 后端 46 个用例（4 个 M3 集成用例）、前端 37 个用例；M4 后全量 48/38 |

## 1. 交付范围

- 医生工作站 `/workstation`：按日期查看本人候诊队列，护士可查看本科室队列。
- 接诊：`WAITING → IN_PROGRESS`，预约 `CHECKED_IN → IN_CONSULTATION`，自动创建空病历草稿。
- 病历草稿：主诉/现病史/既往史/体格检查/辅助检查/处理意见；输入停止 2 秒自动保存。
- 乐观锁：`version` 条件更新，冲突返回 `409 MED-STATE-002`，前端提示重新加载。
- Redis 草稿副本：`emr:draft:{recordId}`，TTL 24h；后端较新版本时提示恢复。
- 诊断：覆盖式保存，必须且只能一条 `PRIMARY`；数据库生成列唯一索引兜底。
- 结束就诊：M3 校验主诉/主诊断；M4 起追加“病历已签署”，再完成 `IN_PROGRESS → COMPLETED`。
- 取消就诊：`WAITING/IN_PROGRESS → CANCELLED`，预约同步取消。
- 就诊事件追加：START / DIAGNOSES_SAVED / FINISH / CANCEL；全部关键动作写审计。

## 2. API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/encounters/queue?date=` | `encounter:read` | 医生本人 / 护士本科室候诊队列 |
| GET | `/api/v1/encounters/{id}` | `encounter:read` | 就诊详情 + 病历 + 诊断 + Redis 草稿 |
| POST | `/api/v1/encounters/{id}/start` | `encounter:start` | 开始接诊，创建空病历 |
| PUT | `/api/v1/encounters/{id}/emr` | `emr:edit` | 保存病历，带 `version` |
| PUT | `/api/v1/encounters/{id}/diagnoses` | `emr:edit` | 覆盖式保存诊断 |
| POST | `/api/v1/encounters/{id}/finish` | `encounter:finish` | 结束就诊，校验主诉/主诊断 |
| POST | `/api/v1/encounters/{id}/cancel` | `encounter:finish` | 取消就诊 |

保存病历请求：

```json
{
  "content": {
    "chiefComplaint": "发热三天",
    "presentIllness": "受凉后发热",
    "pastHistory": "无",
    "physicalExam": "体温 38.5℃",
    "auxiliaryExam": "血常规待查",
    "treatmentPlan": "对症治疗"
  },
  "version": 3
}
```

## 3. 并发与状态机

```text
WAITING ──start──▶ IN_PROGRESS ──finish──▶ COMPLETED
   │                    │
   └────cancel──────────┴────cancel──────▶ CANCELLED
```

- 每次状态迁移都是 `UPDATE ... WHERE id=? AND status=?`；影响行数为 0 → 409，避免双开两个窗口重复接诊。
- 病历保存 `UPDATE emr_record SET content_json=?, version=version+1 WHERE id=? AND version=?`。
- 两个窗口同版本保存：一个成功，一个 `MED-STATE-002`；后保存者必须重新加载，不静默覆盖。
- 诊断覆盖在事务内先删后插；`primary_encounter_id` 生成列唯一索引保证一个就诊只有一条主诊断。
- Redis 只做草稿副本，不参与最终一致性；数据库是唯一事实源。

## 4. 结束就诊校验

M3 的校验（M4 签署后会更严格）：

- 就诊必须 `IN_PROGRESS`。
- 病历记录存在，`chiefComplaint` 非空。
- 至少一条诊断，且必须有 `PRIMARY`。
- M5 处方模块接入后追加“无未提交处方”校验；M4 接入签署后追加“病历已签署”。

任何条件不满足返回 `409 MED-STATE-002`，`detail` 明确指出缺少主诉或主诊断。

## 5. 数据模型（V5）

- `encounter_diagnosis`：诊断，`primary_encounter_id` 生成列唯一。
- `encounter_event`：追加式就诊事件，`payload JSON`。
- `emr_record`：M3 草稿，`content_json JSON`、`version`；M4 扩展模板/版本/签署。
- `doctor_schedule.next_seq`：排班级原子序号，修复 M2 多容量号源并发时 `seqNo` 可能重复的问题。

## 6. 前端

```text
features/workstation/
  data/   models + WorkstationApi
  state/  WorkstationStore（队列、自动保存、冲突、草稿恢复）
  ui/     workstation-page（队列 + 编辑器 + 诊断）
```

- 队列选中后加载详情；接诊按钮仅 `encounter:start` 可见。
- 文本域 `(input)` 更新 signal，2 秒防抖保存；保存中显示状态，成功后显示时间。
- 409 冲突显示红色提示与「重新加载」，不覆盖服务器内容。
- 诊断行内编辑，主/次诊断下拉；结束就诊按钮在未满足条件时禁用并提示。
- 草稿恢复仅在 Redis 内容与数据库不同时显示。

## 7. 验证证据

| 验证 | 结果 |
| --- | --- |
| `mvn clean verify` | 46 个后端测试通过 |
| `WorkstationFlowTest` | 4/4：完整接诊、缺主诉拦截、并发乐观锁、医生/科室范围 |
| `pnpm --filter medflow-web test` | 37 个前端测试通过 |
| `lint` / `build` | 0 error / 成功 |
| 手工链路 | 候诊 → 接诊 → 保存 v1 → 旧版本 409 → 诊断 → 结束就诊；预约变 COMPLETED |
| 菜单 | 医生/护士出现「医生工作站」；药师/收费员/管理员不出现 |

## 8. 已知边界

- M3 病历是扁平 SOAP 文本，不含模板、结构化字段、签名与版本对比；M4 交付。
- 结束就诊暂不校验处方（M5 才有处方表）。
- Redis 草稿是尽力而为；Redis 不可用时自动降级为数据库版本，不影响保存。
- 自动保存是前端 2 秒防抖，不是协同编辑；冲突采用“重新加载”而非自动合并。
- 队列每 10 秒自动刷新尚未启用，当前由手动刷新/操作后刷新触发。

## 9. 下一步（M4）

在 `emr_record` 上增加模板、字段值、版本历史与签署；实现结构化病历编辑器、签名留痕、打印视图与历史版本对比。
