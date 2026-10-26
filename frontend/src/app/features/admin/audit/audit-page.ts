import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize } from 'rxjs/operators';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { Icon } from '../../../shared/ui/icon/icon';
import { IamApi } from '../data/iam.api';
import type { AuditRow } from '../data/iam.models';

const ACTION_LABELS: Readonly<Record<string, string>> = {
  AUTH_LOGIN_SUCCESS: '登录成功',
  AUTH_LOGIN_FAILED: '登录失败',
  AUTH_LOGOUT: '退出登录',
  USER_CREATED: '新建用户',
  USER_UPDATED: '编辑用户',
  USER_STATUS_CHANGED: '变更用户状态',
  USER_PASSWORD_RESET: '重置密码',
  ROLE_CREATED: '新建角色',
  ROLE_UPDATED: '编辑角色',
  ROLE_PERMISSION_CHANGED: '角色授权',
  DEPARTMENT_CREATED: '新建科室',
  DEPARTMENT_UPDATED: '编辑科室',
  ACCESS_DENIED: '越权访问',
};

@Component({
  selector: 'app-audit-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, Icon, MatButtonModule, MatCardModule, MatProgressBarModule],
  templateUrl: './audit-page.html',
  styleUrl: './audit-page.scss',
})
export class AuditPage {
  private readonly api = inject(IamApi);

  protected readonly rows = signal<readonly AuditRow[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<MedflowError | null>(null);
  protected readonly page = signal(1);
  protected readonly size = 20;
  protected readonly action = signal('');
  protected readonly actorId = signal<number | null>(null);
  protected readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size)));

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .listAudit({
        page: this.page(),
        size: this.size,
        action: this.action(),
        actorId: this.actorId(),
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.rows.set(result.items);
          this.total.set(result.total);
          this.error.set(null);
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected search(): void {
    this.page.set(1);
    this.load();
  }

  protected onAction(event: Event): void {
    this.action.set((event.target as HTMLInputElement).value.trim());
  }

  protected onActor(event: Event): void {
    const value = (event.target as HTMLInputElement).value.trim();
    this.actorId.set(value ? Number(value) : null);
  }

  protected actionLabel(action: string): string {
    return ACTION_LABELS[action] ?? action;
  }

  protected prevPage(): void {
    if (this.page() > 1) {
      this.page.update((value) => value - 1);
      this.load();
    }
  }

  protected nextPage(): void {
    if (this.page() < this.totalPages()) {
      this.page.update((value) => value + 1);
      this.load();
    }
  }
}
