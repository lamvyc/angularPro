import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthStore } from '../../../core/auth/auth.store';
import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { HasPermissionDirective } from '../../../core/permissions/has-permission.directive';
import { Icon } from '../../../shared/ui/icon/icon';
import { IamApi } from '../data/iam.api';
import type { Department, RoleSummary, UserSummary } from '../data/iam.models';
import {
  ResetPasswordDialog,
  UserEditorDialog,
  type UserEditorResult,
} from '../shared/admin-dialogs';

@Component({
  selector: 'app-users-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    HasPermissionDirective,
    Icon,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatProgressBarModule,
  ],
  templateUrl: './users-page.html',
  styleUrl: './users-page.scss',
})
export class UsersPage {
  private readonly api = inject(IamApi);
  private readonly dialog = inject(MatDialog);
  protected readonly auth = inject(AuthStore);

  protected readonly users = signal<readonly UserSummary[]>([]);
  protected readonly departments = signal<readonly Department[]>([]);
  protected readonly roles = signal<readonly RoleSummary[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<MedflowError | null>(null);
  protected readonly page = signal(1);
  protected readonly size = 20;
  protected readonly keyword = signal('');
  protected readonly deptId = signal<number | null>(null);
  protected readonly status = signal('');
  protected readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size)));

  constructor() {
    forkJoin([this.api.listDepartments(), this.api.listRoles()]).subscribe({
      next: ([departments, roles]) => {
        this.departments.set(departments);
        this.roles.set(roles);
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .listUsers({
        page: this.page(),
        size: this.size,
        keyword: this.keyword(),
        deptId: this.deptId(),
        status: this.status(),
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.users.set(result.items);
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

  protected onKeyword(event: Event): void {
    this.keyword.set((event.target as HTMLInputElement).value.trim());
  }

  protected onDept(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.deptId.set(value ? Number(value) : null);
    this.search();
  }

  protected onStatus(event: Event): void {
    this.status.set((event.target as HTMLSelectElement).value);
    this.search();
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

  protected openCreate(): void {
    const dialogRef = this.dialog.open<UserEditorDialog, unknown, UserEditorResult>(
      UserEditorDialog,
      {
        width: '680px',
        data: { departments: this.departments(), roles: this.roles() },
      },
    );
    dialogRef.afterClosed().subscribe((result) => {
      if (!result || result.mode !== 'create') {
        return;
      }
      this.api.createUser(result.payload).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
    });
  }

  protected openEdit(user: UserSummary): void {
    const dialogRef = this.dialog.open<UserEditorDialog, unknown, UserEditorResult>(
      UserEditorDialog,
      {
        width: '680px',
        data: { user, departments: this.departments(), roles: this.roles() },
      },
    );
    dialogRef.afterClosed().subscribe((result) => {
      if (!result || result.mode !== 'edit') {
        return;
      }
      this.api.updateUser(user.id, result.payload).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
    });
  }

  protected toggleStatus(user: UserSummary): void {
    const next = user.status === 'ENABLED' || user.status === 'LOCKED' ? 'DISABLED' : 'ENABLED';
    this.api.changeUserStatus(user.id, next).subscribe({
      next: () => this.load(),
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  protected openResetPassword(user: UserSummary): void {
    const dialogRef = this.dialog.open<ResetPasswordDialog, unknown, string | undefined>(
      ResetPasswordDialog,
      { width: '420px' },
    );
    dialogRef.afterClosed().subscribe((password) => {
      if (!password) {
        return;
      }
      this.api.resetPassword(user.id, password).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
    });
  }

  protected statusLabel(user: UserSummary): string {
    if (user.status === 'LOCKED') {
      return '已锁定';
    }
    return user.status === 'ENABLED' ? '启用' : '停用';
  }

  protected statusClass(user: UserSummary): string {
    if (user.status === 'LOCKED') {
      return 'tag tag-warning';
    }
    return user.status === 'ENABLED' ? 'tag tag-success' : 'tag tag-danger';
  }
}
