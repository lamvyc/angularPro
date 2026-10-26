# M3 医生工作站 Implementation Plan

**Goal:** 候诊队列、接诊状态机、病历草稿自动保存、诊断录入、结束就诊与并发冲突处理。

**Architecture:** encounter 状态机条件更新；`emr_record` 乐观锁 `version`；Redis 草稿副本仅提示；诊断主诊断由生成列唯一索引兜底；关键动作追加 `encounter_event` 与审计。

**Tech Stack:** Spring Boot 4.1 / MyBatis / MySQL / Redis / Angular 22 Signals + RxJS。

**Spec:** `docs/requirements.md` M3、`docs/business-flows.md` 3、`docs/database-design.md` 5。

## 验收

- 医生只见本人候诊，护士只见本科室；跨科室 403。
- 接诊：WAITING → IN_PROGRESS，预约 IN_CONSULTATION，自动建空病历。
- 两窗口同版本保存：一个 200、一个 409，版本只 +1。
- 结束就诊必须主诉非空 + 一条主诊断；成功后 encounter/appointment 均 COMPLETED。
- 前端可完成 候诊 → 接诊 → 编辑（自动保存）→ 诊断 → 结束 主链路。
- 后端全量测试、前端 lint/test/build 通过。

## Tasks

1. V5：诊断、事件、emr_record、排班原子序号、工作站菜单。
2. 后端：队列、详情、接诊、病历保存、诊断、结束/取消、事件与审计。
3. 集成测试：完整链路、缺主诉、并发乐观锁、医生/科室范围。
4. 前端：workstation store/page、自动保存、冲突提示、诊断编辑、结束按钮。
5. 文档/验证/评审。

## 执行记录

- 完成于 2026-10-26；后端 46 个测试、前端 37 个测试通过。
- V5 同时修复 M2 多容量号源并发 seqNo 重复：`doctor_schedule.next_seq` 原子分配。
- 病历为 M3 扁平 SOAP 草稿；M4 再扩展模板、版本、签署。
