import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { HasPermissionDirective } from '../../../core/permissions/has-permission.directive';
import { Icon } from '../../../shared/ui/icon/icon';
import { CancelDialog } from '../../appointments/ui/appointment-dialogs';
import { WorkstationStore, type EmrField } from '../state/workstation.store';

interface FieldConfig {
  readonly key: EmrField;
  readonly label: string;
  readonly placeholder: string;
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

  protected readonly fields: readonly FieldConfig[] = [
    { key: 'chiefComplaint', label: '主诉', placeholder: '患者主要症状与持续时间' },
    { key: 'presentIllness', label: '现病史', placeholder: '起病、发展、诊疗经过' },
    { key: 'pastHistory', label: '既往史', placeholder: '既往疾病、手术、过敏史' },
    { key: 'physicalExam', label: '体格检查', placeholder: '生命体征与专科检查' },
    { key: 'auxiliaryExam', label: '辅助检查', placeholder: '检验、影像等结果' },
    { key: 'treatmentPlan', label: '处理意见', placeholder: '治疗、用药与随访计划' },
  ];

  constructor() {
    this.store.loadQueue();
  }

  protected setDate(event: Event): void {
    this.store.queueDate.set((event.target as HTMLInputElement).value);
    this.store.loadQueue();
  }

  protected openCancel(): void {
    const ref = this.dialog.open<CancelDialog, unknown, string>(CancelDialog, { width: '440px' });
    ref.afterClosed().subscribe((reason) => {
      if (reason) {
        this.store.cancel(reason);
      }
    });
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
}
