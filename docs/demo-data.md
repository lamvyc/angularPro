# 演示数据指南

## 1. 加载方式

演示数据由 Flyway Repeatable 脚本 `backend/src/main/resources/db/demo/R__demo_seed.sql` 提供。

- 仅在 **local / docker** profile 生效：`spring.flyway.locations = classpath:db/migration,classpath:db/demo`。
- `test` profile 与默认生产配置不加载该目录；自动化测试不会被演示数据污染。
- 首次启动自动写入；演示日期以首次执行当天为基准（`CURDATE()` / `CURDATE()+1`）。
- 脚本使用高段 ID（9001+）与 `INSERT IGNORE` / `ON DUPLICATE KEY`，可在已有数据库上重复执行。

## 2. 内置数据

| 数据 | 数量 | 覆盖场景 |
| --- | --- | --- |
| 患者 | 6 | 合成姓名、手机号、脱敏证件号、过敏史 |
| 排班 | 3 | 全科今日上午、明日上午；皮肤科今日下午 |
| 号源 | 8 | 今日候诊/接诊中占号，明日可预约 |
| 预约 | 6 | COMPLETED / CHECKED_IN / IN_CONSULTATION / BOOKED / CANCELLED |
| 就诊 | 4 | 已完成 1、候诊 2、接诊中 1 |
| 病历 | 2 | 已签署 + 接诊中草稿 |
| 诊断 | 2 | 主诊断 |
| 处方 | 5 | SUBMITTED / APPROVED / DISPENSED / REJECTED / DRAFT |
| 库存批次 | 7 | 3 个药品，含临期批次与预占/发药流水 |
| 药品 | 3 | 阿莫西林、布洛芬、沙丁胺醇 |

典型演示对象：

- 医生工作站：李华（候诊）、张伟（接诊中，草稿病历）、王明（已完成，已签署病历）。
- 药房：`RXDEMO0001` 待审方；`RXDEMO0002` 已通过（已预占）；`RXDEMO0003` 已发药（可退药）；`RXDEMO0004` 已驳回。
- 预约：王明已完成、李华签到、张伟接诊中、刘洋已取消；明日赵敏已预约。

## 3. 推荐演示顺序

1. 收费员登录 → 预约挂号列表查看今日状态；新建预约到明日全科。
2. 医生登录 → 医生工作站查看候诊队列；接诊张伟 → 编辑结构化病历 → 签署 → 结束就诊。
3. 药师登录 → 药房审方：`RXDEMO0001` 开始审方并通过（FEFO 预占）；对 `RXDEMO0002` 发药；对 `RXDEMO0003` 退药。
4. 药师 → 药房库存：查看批次、库存汇总、流水；入库一个有效批次；尝试过期批次应返回 422。

## 4. 重置演示数据

演示日期在首次加载时固定。若需全新数据（推荐每次演示前）：

```bash
# 会删除本地 Compose 的 MySQL/Redis 数据卷，仅用于演示环境
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d mysql redis
cd backend && ./mvnw spring-boot:run
```

或手动重建数据库（确保没有真实数据）：

```bash
mysql -h127.0.0.1 -P3307 -uroot -p \
  -e "DROP DATABASE medflow; CREATE DATABASE medflow CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

重启后端后 Flyway 会依次执行 V1–V9 和演示脚本。

## 5. 合规

- 所有姓名、手机号、证件号、地址均为合成数据；证件号仅存掩码。
- 药品、处方、诊断仅用于流程演示，不构成临床建议。
- 登录页、工作台标注“演示环境 · 合成数据”。
