import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { HasPermissionDirective } from '../../../core/permissions/has-permission.directive';
import { Icon } from '../../../shared/ui/icon/icon';
import { IamApi } from '../data/iam.api';
import type { PermissionNode, RoleSummary } from '../data/iam.models';
import { PermissionDialog, RoleEditorDialog, type RoleEditorResult } from '../shared/admin-dialogs';

@Component({
  selector: 'app-roles-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    HasPermissionDirective,
    Icon,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatProgressBarModule,
  ],
  templateUrl: './roles-page.html',
  styleUrl: './roles-page.scss',
})
export class RolesPage {
  private readonly api = inject(IamApi);
  private readonly dialog = inject(MatDialog);

  protected readonly roles = signal<readonly RoleSummary[]>([]);
  protected readonly permissions = signal<readonly PermissionNode[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<MedflowError | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    forkJoin([this.api.listRoles(), this.api.listPermissions()])
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ([roles, permissions]) => {
          this.roles.set(roles);
          this.permissions.set(permissions);
          this.error.set(null);
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected openCreate(): void {
    const ref = this.dialog.open<RoleEditorDialog, unknown, RoleEditorResult>(RoleEditorDialog, {
      width: '480px',
      data: {},
    });
    ref.afterClosed().subscribe((result) => {
      if (!result || result.mode !== 'create') {
        return;
      }
      this.api.createRole(result.payload).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
    });
  }

  protected openEdit(role: RoleSummary): void {
    const ref = this.dialog.open<RoleEditorDialog, unknown, RoleEditorResult>(RoleEditorDialog, {
      width: '480px',
      data: { role },
    });
    ref.afterClosed().subscribe((result) => {
      if (!result || result.mode !== 'edit') {
        return;
      }
      this.api.updateRole(role.id, result.payload).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
    });
  }

  protected openPermissions(role: RoleSummary): void {
    const ref = this.dialog.open<PermissionDialog, unknown, number[]>(PermissionDialog, {
      width: '680px',
      data: { role, tree: this.permissions() },
    });
    ref.afterClosed().subscribe((permissionIds) => {
      if (!permissionIds) {
        return;
      }
      this.api.assignPermissions(role.id, permissionIds).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
    });
  }

  protected scopeLabel(role: RoleSummary): string {
    switch (role.dataScope) {
      case 'ALL':
        return '全院';
      case 'DEPT':
        return '本科室';
      default:
        return '仅本人';
    }
  }
}
