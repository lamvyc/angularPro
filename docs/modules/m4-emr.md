# M4 电子病历 · 实现说明

| 项 | 值 |
| --- | --- |
| 状态 | ✅ 已完成（2026-10-26） |
| 对应需求 | FR-M4-01 ~ FR-M4-10 |
| 后端 | V6 模板/版本/签署/修订快照；V7 管理员模板读权限 |
| 前端 | 按 Schema 动态渲染、条件显示、签署/修订、版本历史对比、打印、模板管理 |
| 测试 | 后端 48 个用例（2 个 M4 集成用例）；前端 38 个用例 |

## 1. 交付范围

- 结构化模板：分区 + 字段，字段含 `type`、`required`、`maxLength`、`min/max`、`options`、`unit`、`visibleWhen`、`requiredWhen`。
- 模板版本：发布后不可原地修改；修改生成新版本，新病历使用最新已发布版本，历史病历固定旧版本。
- 动态渲染：文本、长文本、数值、日期、布尔、单选、多选；条件字段隐藏后不参与校验。
- 前后端校验：前端按 Schema 渲染并展示字段错误；后端用同一份模板版本重新校验，返回 422 + `fieldErrors`。
- 签署：作者本人、模板校验通过后签署，保存签署人/时间/规范化内容 SHA-256；签署后草稿接口拒绝修改。
- 修订：已签署病历可填原因修订，旧版本置 `SUPERSEDED`，新版本签署，记录状态 `AMENDED`。
- 历史：版本号、状态、签署人、时间、原因、内容快照；前端可选择两个版本对比。
- 模板管理：管理员创建/发布新版本/归档；已归档模板不能发布新版本，被历史病历引用的版本不删除。
- 打印：工作站浏览器打印当前病历。

## 2. API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/emr-templates` | `emr:template:read` | 模板及当前版本 |
| GET | `/api/v1/emr-templates/{id}/versions` | `emr:template:read` | 模板版本历史 |
| POST | `/api/v1/emr-templates` | `emr:template:manage` | 创建模板并发布 v1 |
| PUT | `/api/v1/emr-templates/{id}` | `emr:template:manage` | 发布新版本 |
| POST | `/api/v1/emr-templates/{id}/archive` | `emr:template:manage` | 归档模板 |
| POST | `/api/v1/encounters/{id}/emr/sign` | `emr:sign` | 校验并签署 |
| POST | `/api/v1/encounters/{id}/emr/amend` | `emr:revise` | 修订并签署新版本 |
| GET | `/api/v1/encounters/{id}/emr/versions` | `emr:read` | 版本列表 |
| GET | `/api/v1/encounters/{id}/emr/versions/{no}` | `emr:read` | 指定版本内容 |

签署请求：

```json
{
  "content": {
    "chiefComplaint": "发热两天",
    "fever": true,
    "temperature": 38.6
  },
  "version": 0
}
```

## 3. 数据模型

- `emr_template`：编码唯一、范围（HOSPITAL/DEPT）、当前版本。
- `emr_template_version`：`schema_json`、版本号、变更说明、发布人/时间；发布后应用层禁止更新 schema。
- `emr_record`：固定 `template_id` + `template_version_id`；状态 `DRAFT/SIGNED/AMENDED`；签署人/时间/哈希。
- `emr_record_version`：每次签署生成不可变快照，`(record_id, version_no)` 唯一；修订时旧版本置 `SUPERSEDED`。
- V6 为存量 M3 病历回填内置模板 `GENERAL_FIRST`。

## 4. 校验规则

执行顺序：

1. 根据 `visibleWhen` 剔除隐藏字段。
2. 类型校验（number/date/boolean/select/radio/checkbox）。
3. 范围、长度、选项。
4. `required` 或 `requiredWhen` 条件必填。
5. 服务端使用病历固定的 `template_version_id`，不接受客户端“已校验”标记。

错误以 `422 MED-VALID-001` 返回，`fieldErrors[].field` 形如 `content.temperature`。

## 5. 签署与不可变

- 签名载荷：`{templateVersionId, content, signerId, signedAt}`，顶层键排序后 SHA-256。
- 所有草稿更新 SQL 均带 `status='DRAFT'`；已签署内容只能通过修订生成新版本。
- 签署/修订审计记录 `beforeVersion`、`afterVersion`、`hash`，不记录病历正文。
- 签署后清除 Redis 草稿；结束就诊强制要求 `SIGNED/AMENDED`。

## 6. 前端

```text
features/workstation/
  ui/workstation-page            动态 Schema 渲染、签署/修订/打印
  ui/workstation-dialogs         修订原因、版本历史与对比
features/admin/emr-templates/
  emr-templates-page             模板列表/归档
  emr-template-dialog            创建/发布（JSON Schema 编辑）
```

- 字段按分区渲染；`visibleWhen` 隐藏时前端不显示。
- 签署后字段只读；「修订病历」填写原因后进入修订模式，提交生成新版本。
- 版本历史弹窗可选择 A/B 两个版本进行字段级差异对比。
- 模板管理使用 JSON Schema 编辑，适合管理员维护；后续可替换为可视化字段设计器。

## 7. 验证证据

| 验证 | 结果 |
| --- | --- |
| `mvn clean verify` | 48 个后端测试通过 |
| `EmrFlowTest` | 2/2：条件校验/签署/不可改/修订/历史；模板创建/发版/归档/越权 |
| 条件必填 | `fever=true` 缺 `temperature` → 422，fieldErrors 含 temperature |
| 签署不可改 | 签署后 `PUT /emr` → 409 |
| 修订 | 旧版本 `SUPERSEDED`，新版本 `SIGNED`，记录 `AMENDED` |
| 前端 | 38 个测试通过；lint/build 通过 |
| 手工链路 | 接诊 → 动态模板 → 签署 → 修订 → 版本历史 → 结束就诊 |

## 8. 已知边界

- 修订采用“一次提交即签署”的简化流程；未实现独立的修订草稿/放弃修订状态（`DISCARDED`）。
- 模板管理为 JSON 编辑，不是拖拽式设计器；条件规则支持 `visibleWhen/requiredWhen`，跨字段 `rules` 暂未执行。
- 生命体征组以多个 number 字段表达，未做专门组合控件。
- 版本对比按字段 key 展示，未把 key 映射为模板 label。
- 打印使用浏览器打印样式，未生成 PDF 归档。

## 9. 下一步（M5）

处方与医嘱：草稿/提交/审方/驳回重提/取消/过期，库存预占在审核通过时按 FEFO 执行，提交快照与幂等键。
