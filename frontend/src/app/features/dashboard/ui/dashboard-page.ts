import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import type { ServiceState } from '../data/system.models';
import { SystemStatusStore } from '../state/system-status.store';
import { Icon, type IconName } from '../../../shared/ui/icon/icon';

interface ServiceRow {
  readonly label: string;
  readonly hint: string;
  readonly icon: IconName;
  readonly state: ServiceState;
}

interface RoadmapItem {
  readonly phase: string;
  readonly title: string;
  readonly description: string;
  readonly status: 'done' | 'next' | 'planned';
}

@Component({
  selector: 'app-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatCardModule, MatProgressBarModule, Icon],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  protected readonly store = inject(SystemStatusStore);

  protected readonly services = computed<readonly ServiceRow[]>(() => [
    {
      label: 'API 服务',
      hint: '/api/v1/system/ping',
      icon: 'server',
      state: this.store.apiState(),
    },
    {
      label: 'MySQL 数据库',
      hint: 'Flyway V1 · 基础设施表',
      icon: 'database',
      state: this.store.dbState(),
    },
    {
      label: 'Redis',
      hint: '会话 / 缓存 / 草稿',
      icon: 'database',
      state: this.store.redisState(),
    },
  ]);

  protected readonly lastCheckedLabel = computed(() => {
    const checkedAt = this.store.lastCheckedAt();
    if (checkedAt === null) {
      return '尚未检查';
    }
    return checkedAt.toLocaleTimeString('zh-CN', { hour12: false });
  });

  protected readonly serverTimeLabel = computed(() => {
    const value = this.store.serverTime();
    if (value === null) {
      return null;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString('zh-CN', { hour12: false, dateStyle: 'short', timeStyle: 'medium' });
  });

  protected readonly completed: readonly string[] = [
    '5 份设计文档：需求 / 架构 / 数据库 / 流程 / 计划',
    'Docker Compose：MySQL 8.4 + Redis 8',
    'Spring Boot 4.1.2 分层骨架与 Flyway 迁移',
    'Angular 22 工程、Material 主题与 Vitest',
    '统一 Problem Details、请求 ID 与安全基线',
    'Testcontainers 集成测试与 CI 质量门禁',
  ];

  protected readonly roadmap: readonly RoadmapItem[] = [
    {
      phase: 'M1',
      title: '权限体系',
      description: '登录、JWT、菜单/按钮/接口权限、科室数据隔离、审计',
      status: 'next',
    },
    {
      phase: 'M2',
      title: '预约挂号',
      description: '排班号源、并发防超卖、重复预约、退号退费',
      status: 'planned',
    },
    {
      phase: 'M3',
      title: '医生工作站',
      description: '候诊队列、接诊状态机、草稿恢复、乐观锁冲突',
      status: 'planned',
    },
    {
      phase: 'M4',
      title: '电子病历',
      description: '动态模板、条件联动、签署不可变、修订与版本',
      status: 'planned',
    },
    {
      phase: 'M5',
      title: '处方管理',
      description: '开方、幂等提交、审方驳回、重提与审计',
      status: 'planned',
    },
    {
      phase: 'M6',
      title: '药房库存',
      description: 'FEFO 预占、发药退药、库存不足补偿与对账',
      status: 'planned',
    },
  ];

  protected readonly stack: readonly string[] = [
    'Angular 22.2',
    'Signals / Zoneless',
    'Angular Material 22',
    'Vitest 5',
    'Spring Boot 4.1',
    'Spring Security 7',
    'MyBatis-Plus 3.5',
    'MySQL 8.4',
    'Redis 8',
    'Flyway 12',
    'Testcontainers 2',
  ];

  protected readonly documents: readonly { file: string; title: string }[] = [
    { file: 'requirements.md', title: '需求与角色权限矩阵' },
    { file: 'architecture.md', title: '架构、选型与 ADR' },
    { file: 'database-design.md', title: 'ER 模型与表设计' },
    { file: 'business-flows.md', title: '流程与状态机' },
    { file: 'development-plan.md', title: '里程碑与验收标准' },
  ];

  protected refresh(): void {
    this.store.refresh();
  }

  protected stateLabel(state: ServiceState): string {
    switch (state) {
      case 'up':
        return '正常';
      case 'down':
        return '异常';
      default:
        return '待检测';
    }
  }
}
