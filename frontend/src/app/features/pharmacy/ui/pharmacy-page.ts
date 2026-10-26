import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin, finalize } from 'rxjs';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { Icon } from '../../../shared/ui/icon/icon';
import { PrescriptionApi } from '../../prescriptions/data/prescription.api';
import type { PrescriptionHeader } from '../../prescriptions/data/prescription.models';
import { ReasonDialog } from '../../prescriptions/ui/reason-dialog';
import { ReturnDialog } from './return-dialog';

@Component({
  selector: 'app-pharmacy-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, MatButtonModule, MatCardModule, MatDialogModule, MatProgressBarModule],
  templateUrl: './pharmacy-page.html',
  styleUrl: './pharmacy-page.scss',
})
export class PharmacyPage {
  private readonly api = inject(PrescriptionApi);
  private readonly dialog = inject(MatDialog);
  protected readonly prescriptions = signal<readonly PrescriptionHeader[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<MedflowError | null>(null);
  protected readonly message = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    forkJoin({
      submitted: this.api.list(undefined, 'SUBMITTED'),
      reviewing: this.api.list(undefined, 'UNDER_REVIEW'),
      approved: this.api.list(undefined, 'APPROVED'),
      dispensed: this.api.list(undefined, 'DISPENSED'),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ submitted, reviewing, approved, dispensed }) =>
          this.prescriptions.set([...reviewing, ...submitted, ...approved, ...dispensed]),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected startReview(prescription: PrescriptionHeader): void {
    this.api.startReview(prescription.id, prescription.version).subscribe({
      next: () => {
        this.message.set('已开始审方');
        this.load();
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  protected approve(prescription: PrescriptionHeader): void {
    this.api.approve(prescription.id, prescription.version).subscribe({
      next: () => {
        this.message.set('审方通过，库存已按 FEFO 预占');
        this.load();
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  protected dispense(prescription: PrescriptionHeader): void {
    this.api.dispense(prescription.id).subscribe({
      next: () => {
        this.message.set('发药成功，实库存与预占已同步扣减');
        this.load();
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  protected openReturn(prescription: PrescriptionHeader): void {
    const ref = this.dialog.open(ReturnDialog, {
      width: '640px',
      data: { prescriptionId: prescription.id },
    });
    ref.afterClosed().subscribe((view) => {
      if (view) {
        this.message.set('退药完成，库存已回补');
        this.load();
      }
    });
  }

  protected reject(prescription: PrescriptionHeader): void {
    const ref = this.dialog.open<ReasonDialog, { title: string; label: string; confirm: string }, string>(
      ReasonDialog,
      {
        width: '440px',
        data: { title: '驳回处方', label: '驳回原因', confirm: '确认驳回' },
      },
    );
    ref.afterClosed().subscribe((reason) => {
      if (reason) {
        this.api.reject(prescription.id, prescription.version, reason).subscribe({
          next: () => {
            this.message.set('处方已驳回');
            this.load();
          },
          error: (error: unknown) => this.error.set(toMedflowError(error)),
        });
      }
    });
  }

  protected statusLabel(status: string): string {
    switch (status) {
      case 'SUBMITTED':
        return '待审方';
      case 'UNDER_REVIEW':
        return '审核中';
      case 'APPROVED':
        return '已通过';
      case 'DISPENSED':
        return '已发药';
      default:
        return status;
    }
  }

  protected statusClass(status: string): string {
    switch (status) {
      case 'SUBMITTED':
        return 'tag tag-warning';
      case 'UNDER_REVIEW':
        return 'tag tag-warning';
      case 'APPROVED':
        return 'tag tag-success';
      default:
        return 'tag';
    }
  }

  protected clear(): void {
    this.error.set(null);
    this.message.set(null);
  }
}
