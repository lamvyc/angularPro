# M5 处方与审方 Implementation Plan

**Goal:** 处方草稿/提交/审方/驳回重提/取消/过期，提交快照与幂等，审核通过按 FEFO 预占库存。

**Architecture:** 处方条件更新 + 修订快照；审核记录绑定修订号；预占条件更新批次 `reserved_qty`，整单事务失败回滚；取消/过期释放预占。

**Tech Stack:** Spring Boot 4.1 / MyBatis / MySQL / Angular 22 Signals。

**Spec:** `docs/requirements.md` M5、`docs/business-flows.md` 5、`docs/database-design.md` 4.7。

## 验收

- `DRAFT → SUBMITTED` 保存快照、修订号 +1；同幂等键返回同一结果。
- 药师开始审核、通过/驳回；驳回原因必填。
- 通过时 FEFO 预占；库存不足整单回滚，处方保持 `UNDER_REVIEW`。
- 取消 `APPROVED` 释放预占；72h 后自动 `EXPIRED` 并释放。
- 前端工作站可开方/提交/取消；药房可审方。
- 后端全量、前端 lint/test/build 通过。

## Tasks

1. V8 处方/明细/修订/审核/事件 + 最小药品批次/预占/流水；药房菜单。
2. 后端开方、提交幂等、审核、FEFO 预占/释放、过期任务、审计。
3. 集成测试：提交幂等/预占/取消；驳回重提；库存不足；过期；权限范围。
4. 前端工作站处方区、处方弹窗；药房审方页。
5. 文档/验证/评审。

## 执行记录

- 完成于 2026-10-26；后端 53 个测试、前端 39 个测试通过。
- V8 同时建立最小药品/批次库存供 M5 预占；M6 完善目录、入库、发药、退药与对账。
- 发药/退药未在 M5 实现，`APPROVED` 之后由 M6 推进。
