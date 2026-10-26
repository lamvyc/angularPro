import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { toMedflowError } from '../../../core/http/problem-detail';
import { PrescriptionApi } from '../../prescriptions/data/prescription.api';
import type { PrescriptionReservation } from '../../prescriptions/data/prescription-reservation.model';
import type { PrescriptionView } from '../../prescriptions/data/prescription.models';

@Component({
  selector: 'app-return-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>退药</h2>
    <mat-dialog-content>
      @if (error(); as error) {
        <p class="error">{{ error }}</p>
      }
      @if (loading()) {
        <p class="muted">加载发药批次…</p>
      } @else {
        <table class="return-table">
          <thead>
            <tr>
              <th>药品</th>
              <th>批次 / 有效期</th>
              <th>已发 / 已退</th>
              <th>本次退药</th>
            </tr>
          </thead>
          <tbody>
            @for (reservation of reservations(); track reservation.id) {
              <tr>
                <td>{{ reservation.drugName }}</td>
                <td>{{ reservation.batchNo }}<br /><small>{{ reservation.expiryDate }}</small></td>
                <td>{{ reservation.quantity }} / {{ reservation.returnedQty }}</td>
                <td>
                  <input
                    type="number"
                    min="0"
                    [max]="reservation.quantity - reservation.returnedQty"
                    [value]="quantities()[reservation.id] ?? 0"
                    (input)="setQuantity(reservation.id, $event, reservation)"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
        <mat-form-field appearance="outline" class="full">
          <mat-label>退药原因</mat-label>
          <input matInput [value]="reason()" (input)="reason.set($any($event.target).value)" />
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()" [disabled]="saving()">确认退药</button>
    </mat-dialog-actions>
  `,
  styles: `
    .return-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 10px;
      font-size: 11.5px;
    }

    th,
    td {
      padding: 6px 8px;
      border-bottom: 1px solid var(--mf-border);
      text-align: left;
    }

    input {
      width: 80px;
      height: 30px;
      border: 1px solid var(--mf-border-strong);
      border-radius: 6px;
      padding: 0 6px;
    }

    .full {
      width: 100%;
      min-width: 460px;
    }

    .error {
      margin: 0 0 8px;
      color: var(--mf-danger);
      font-size: 11px;
    }

    .muted {
      color: var(--mf-muted);
      font-size: 12px;
    }
  `,
})
export class ReturnDialog {
  private readonly api = inject(PrescriptionApi);
  private readonly ref = inject(MatDialogRef<ReturnDialog, PrescriptionView>);
  protected readonly data = inject<{ prescriptionId: number }>(MAT_DIALOG_DATA);
  protected readonly reservations = signal<readonly PrescriptionReservation[]>([]);
  protected readonly quantities = signal<Record<number, number>>({});
  protected readonly reason = signal('患者退药');
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.reservations(this.data.prescriptionId).subscribe({
      next: (reservations) => {
        this.reservations.set(reservations.filter((item) => item.status === 'CONSUMED'));
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(toMedflowError(error).message);
      },
    });
  }

  protected setQuantity(id: number, event: Event, reservation: PrescriptionReservation): void {
    const remaining = reservation.quantity - reservation.returnedQty;
    const value = Math.max(0, Math.min(remaining, Number((event.target as HTMLInputElement).value)));
    this.quantities.update((quantities) => ({ ...quantities, [id]: value }));
  }

  protected submit(): void {
    const lines = this.reservations()
      .map((reservation) => ({
        reservationId: reservation.id,
        quantity: this.quantities()[reservation.id] ?? 0,
      }))
      .filter((line) => line.quantity > 0);
    if (lines.length === 0) {
      this.error.set('请填写至少一条退药数量');
      return;
    }
    const key = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `return-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    this.saving.set(true);
    this.api.returnStock(this.data.prescriptionId, key, this.reason().trim() || '退药', lines).subscribe({
      next: (view) => this.ref.close(view),
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(toMedflowError(error).message);
      },
    });
  }
}
