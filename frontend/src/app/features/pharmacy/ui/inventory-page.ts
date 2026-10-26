import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin, finalize } from 'rxjs';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { Icon } from '../../../shared/ui/icon/icon';
import { PharmacyApi } from '../data/pharmacy.api';
import type {
  BatchView,
  DrugAdmin,
  MovementView,
  StockSummary,
} from '../data/pharmacy.models';
import { BatchDialog, DrugDialog } from './inventory-dialogs';

@Component({
  selector: 'app-inventory-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, MatButtonModule, MatCardModule, MatDialogModule, MatProgressBarModule],
  templateUrl: './inventory-page.html',
  styleUrl: './inventory-page.scss',
})
export class InventoryPage {
  private readonly api = inject(PharmacyApi);
  private readonly dialog = inject(MatDialog);
  protected readonly drugs = signal<readonly DrugAdmin[]>([]);
  protected readonly batches = signal<readonly BatchView[]>([]);
  protected readonly summary = signal<readonly StockSummary[]>([]);
  protected readonly movements = signal<readonly MovementView[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<MedflowError | null>(null);
  protected readonly message = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    forkJoin({
      drugs: this.api.drugs(),
      batches: this.api.batches(),
      summary: this.api.summary(),
      movements: this.api.movements(100),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ drugs, batches, summary, movements }) => {
          this.drugs.set(drugs);
          this.batches.set(batches);
          this.summary.set(summary);
          this.movements.set(movements);
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected openDrug(drug?: DrugAdmin): void {
    const ref = this.dialog.open<DrugDialog, { drug?: DrugAdmin }, DrugAdmin>(DrugDialog, {
      width: '720px',
      data: { drug },
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.message.set(drug ? '药品已更新' : '药品已创建');
        this.load();
      }
    });
  }

  protected openBatch(): void {
    const ref = this.dialog.open<BatchDialog, { drugs: readonly DrugAdmin[] }, BatchView>(BatchDialog, {
      width: '640px',
      data: { drugs: this.drugs().filter((drug) => drug.status === 'ENABLED') },
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.message.set(`批次 ${saved.batchNo} 已入库 ${saved.stockQty}`);
        this.load();
      }
    });
  }

  protected toggleDrug(drug: DrugAdmin): void {
    const next = drug.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    this.api.setDrugStatus(drug.id, next).subscribe({
      next: () => {
        this.message.set(next === 'ENABLED' ? '药品已启用' : '药品已停用，历史处方不受影响');
        this.load();
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  protected movementLabel(type: string): string {
    switch (type) {
      case 'IN':
        return '入库';
      case 'RESERVE':
        return '预占';
      case 'RELEASE':
        return '释放';
      case 'DISPENSE':
        return '发药';
      case 'RETURN':
        return '退药';
      default:
        return type;
    }
  }

  protected clear(): void {
    this.error.set(null);
    this.message.set(null);
  }
}
