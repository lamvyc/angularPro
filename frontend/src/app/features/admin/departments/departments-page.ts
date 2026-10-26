import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize } from 'rxjs/operators';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { HasPermissionDirective } from '../../../core/permissions/has-permission.directive';
import { Icon } from '../../../shared/ui/icon/icon';
import { IamApi } from '../data/iam.api';
import type { Department, DepartmentPayload } from '../data/iam.models';
import { DepartmentEditorDialog } from '../shared/admin-dialogs';

@Component({
  selector: 'app-departments-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    HasPermissionDirective,
    Icon,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatProgressBarModule,
  ],
  templateUrl: './departments-page.html',
  styleUrl: './departments-page.scss',
})
export class DepartmentsPage {
  private readonly api = inject(IamApi);
  private readonly dialog = inject(MatDialog);

  protected readonly departments = signal<readonly Department[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<MedflowError | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .listDepartments()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (departments) => {
          this.departments.set(departments);
          this.error.set(null);
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected openCreate(): void {
    const ref = this.dialog.open<DepartmentEditorDialog, unknown, DepartmentPayload>(
      DepartmentEditorDialog,
      { width: '520px', data: {} },
    );
    ref.afterClosed().subscribe((payload) => {
      if (!payload) {
        return;
      }
      this.api.createDepartment(payload).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
    });
  }

  protected openEdit(department: Department): void {
    const ref = this.dialog.open<DepartmentEditorDialog, unknown, DepartmentPayload>(
      DepartmentEditorDialog,
      { width: '520px', data: { department } },
    );
    ref.afterClosed().subscribe((payload) => {
      if (!payload) {
        return;
      }
      this.api
        .updateDepartment(department.id, {
          name: payload.name,
          parentId: payload.parentId,
          type: payload.type,
          sortNo: payload.sortNo,
        })
        .subscribe({
          next: () => this.load(),
          error: (error: unknown) => this.error.set(toMedflowError(error)),
        });
    });
  }

  protected typeLabel(type: Department['type']): string {
    return type === 'CLINICAL' ? '临床科室' : type === 'PHARMACY' ? '药房' : '行政';
  }
}
