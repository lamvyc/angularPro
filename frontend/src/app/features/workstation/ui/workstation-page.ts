import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { HasPermissionDirective } from '../../../core/permissions/has-permission.directive';
import { Icon } from '../../../shared/ui/icon/icon';
import { CancelDialog } from '../../appointments/ui/appointment-dialogs';
import type { EmrCondition, EmrFieldDef } from '../data/workstation.models';
import { WorkstationStore } from '../state/workstation.store';
import { AmendReasonDialog, EmrHistoryDialog } from './workstation-dialogs';

interface FieldOption {
  readonly value: string;
  readonly label: string;
}

@Component({
  selector: 'app-workstation-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    HasPermissionDirective,
    Icon,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatProgressBarModule,
  ],
  templateUrl: './workstation-page.html',
  styleUrl: './workstation-page.scss',
})
export class WorkstationPage {
  protected readonly store = inject(WorkstationStore);
  private readonly dialog = inject(MatDialog);

  protected readonly sections = computed(() => this.store.detail()?.emr?.schema?.sections ?? []);

  constructor() {
    this.store.loadQueue();
  }

  protected setDate(event: Event): void {
    this.store.queueDate.set((event.target as HTMLInputElement).value);
    this.store.loadQueue();
  }

  protected visible(field: EmrFieldDef): boolean {
    return this.condition(field.visibleWhen);
  }

  protected required(field: EmrFieldDef): boolean {
    return field.required === true || (field.requiredWhen != null && this.condition(field.requiredWhen));
  }

  protected textValue(field: EmrFieldDef): string {
    const value = this.store.content()[field.key];
    return value == null ? '' : String(value);
  }

  protected boolValue(field: EmrFieldDef): boolean {
    return this.store.content()[field.key] === true;
  }

  protected options(field: EmrFieldDef): readonly FieldOption[] {
    return (field.options ?? []).map((option) =>
      typeof option === 'string' ? { value: option, label: option } : option,
    );
  }

  protected multiChecked(field: EmrFieldDef, option: string): boolean {
    const value = this.store.content()[field.key];
    return Array.isArray(value) && value.map(String).includes(option);
  }

  protected onText(field: EmrFieldDef, event: Event): void {
    const raw = (event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value;
    if (field.type === 'number') {
      this.store.updateField(field.key, raw === '' ? null : Number(raw));
    } else {
      this.store.updateField(field.key, raw);
    }
  }

  protected onBoolean(field: EmrFieldDef, event: Event): void {
    this.store.updateField(field.key, (event.target as HTMLInputElement).checked);
  }

  protected onMulti(field: EmrFieldDef, option: string, event: Event): void {
    const current = Array.isArray(this.store.content()[field.key])
      ? [...(this.store.content()[field.key] as unknown[]).map(String)]
      : [];
    const checked = (event.target as HTMLInputElement).checked;
    const next = checked ? [...current, option] : current.filter((item) => item !== option);
    this.store.updateField(field.key, next);
  }

  protected fieldError(field: EmrFieldDef): string | null {
    return this.store.fieldError(field.key);
  }

  protected openCancel(): void {
    const ref = this.dialog.open<CancelDialog, unknown, string>(CancelDialog, { width: '440px' });
    ref.afterClosed().subscribe((reason) => {
      if (reason) {
        this.store.cancel(reason);
      }
    });
  }

  protected openAmend(): void {
    const ref = this.dialog.open<AmendReasonDialog, unknown, string>(AmendReasonDialog, {
      width: '500px',
    });
    ref.afterClosed().subscribe((reason) => {
      if (reason) {
        this.store.beginAmend(reason);
      }
    });
  }

  protected openHistory(): void {
    const id = this.store.selectedId();
    if (id !== null) {
      this.dialog.open(EmrHistoryDialog, { width: '760px', data: { encounterId: id } });
    }
  }

  protected print(): void {
    window.print();
  }

  protected statusLabel(status: string): string {
    switch (status) {
      case 'WAITING':
        return '候诊中';
      case 'IN_PROGRESS':
        return '接诊中';
      case 'COMPLETED':
        return '已完成';
      default:
        return '已取消';
    }
  }

  protected statusClass(status: string): string {
    switch (status) {
      case 'WAITING':
        return 'tag tag-warning';
      case 'IN_PROGRESS':
        return 'tag tag-success';
      case 'COMPLETED':
        return 'tag';
      default:
        return 'tag tag-danger';
    }
  }

  private condition(condition: EmrCondition | undefined): boolean {
    if (!condition) {
      return true;
    }
    const actual = this.store.content()[condition.field];
    switch (condition.op) {
      case 'eq':
        return String(actual) === String(condition.value);
      case 'ne':
        return String(actual) !== String(condition.value);
      case 'gte':
        return Number(actual) >= Number(condition.value);
      case 'lte':
        return Number(actual) <= Number(condition.value);
      case 'in':
        return Array.isArray(condition.value)
          ? condition.value.map(String).includes(String(actual))
          : false;
      default:
        return true;
    }
  }
}
