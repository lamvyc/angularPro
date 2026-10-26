import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { toMedflowError } from '../../../core/http/problem-detail';
import { PrescriptionApi } from '../data/prescription.api';
import type { Drug, PrescriptionPayload, PrescriptionView } from '../data/prescription.models';

interface EditableItem {
  readonly key: number;
  drugId: number;
  dosage: string;
  frequency: string;
  route: string;
  days: number;
  quantity: number;
  note: string;
}

@Component({
  selector: 'app-prescription-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data.prescription ? '编辑处方' : '新建处方' }}</h2>
    <mat-dialog-content>
      @if (error(); as error) {
        <p class="error">{{ error }}</p>
      }
      <div class="two-col">
        <mat-form-field appearance="outline">
          <mat-label>诊断摘要</mat-label>
          <input matInput [value]="diagnosis()" (input)="diagnosis.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>医嘱</mat-label>
          <input matInput [value]="advice()" (input)="advice.set($any($event.target).value)" />
        </mat-form-field>
      </div>

      <div class="items-head">
        <strong>药品明细</strong>
        <button mat-button type="button" (click)="addItem()">添加药品</button>
      </div>

      @for (item of items(); track item.key; let i = $index) {
        <div class="rx-item">
          <select [value]="item.drugId" (change)="update(i, 'drugId', $event)">
            <option [value]="0">选择药品</option>
            @for (drug of drugs(); track drug.id) {
              <option [value]="drug.id">{{ drug.name }} · {{ drug.spec }}</option>
            }
          </select>
          <input placeholder="剂量，如 1粒" [value]="item.dosage" (input)="update(i, 'dosage', $event)" />
          <input placeholder="频次，如 每日3次" [value]="item.frequency" (input)="update(i, 'frequency', $event)" />
          <input placeholder="途径，如 口服" [value]="item.route" (input)="update(i, 'route', $event)" />
          <input type="number" min="1" placeholder="天数" [value]="item.days" (input)="update(i, 'days', $event)" />
          <input type="number" min="1" placeholder="数量" [value]="item.quantity" (input)="update(i, 'quantity', $event)" />
          <button mat-button type="button" (click)="removeItem(i)">删除</button>
        </div>
      }

      <div class="total">合计：¥{{ total().toFixed(2) }}</div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="save()" [disabled]="saving()">保存草稿</button>
    </mat-dialog-actions>
  `,
  styles: `
    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-bottom: 8px;
    }

    .items-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin: 4px 0 6px;
      font-size: 12px;
    }

    .rx-item {
      display: grid;
      grid-template-columns: 1.5fr 1fr 1fr 0.8fr 70px 70px auto;
      gap: 5px;
      margin-bottom: 6px;

      input,
      select {
        height: 32px;
        padding: 0 6px;
        border: 1px solid var(--mf-border-strong);
        border-radius: 7px;
        font: inherit;
        font-size: 11px;
      }
    }

    .total {
      margin-top: 8px;
      text-align: right;
      font-size: 12px;
      font-weight: 700;
    }

    .error {
      margin: 0 0 8px;
      color: var(--mf-danger);
      font-size: 11px;
    }

    mat-dialog-content {
      min-width: 780px;
    }
  `,
})
export class PrescriptionDialog {
  private readonly api = inject(PrescriptionApi);
  private readonly ref = inject(MatDialogRef<PrescriptionDialog, PrescriptionView>);
  protected readonly data = inject<{
    encounterId: number;
    prescription?: PrescriptionView;
  }>(MAT_DIALOG_DATA);

  protected readonly drugs = signal<readonly Drug[]>([]);
  protected readonly items = signal<readonly EditableItem[]>(
    this.data.prescription
      ? this.data.prescription.items.map((item) => ({
          key: item.id,
          drugId: item.drugId,
          dosage: item.dosage,
          frequency: item.frequency,
          route: item.route,
          days: item.days,
          quantity: item.quantity,
          note: item.note ?? '',
        }))
      : [this.emptyItem()],
  );
  protected readonly diagnosis = signal(this.data.prescription?.header.diagnosisSummary ?? '');
  protected readonly advice = signal(this.data.prescription?.header.medicalAdvice ?? '');
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  private itemKey = 1000;

  constructor() {
    this.api.drugs().subscribe({
      next: (drugs) => this.drugs.set(drugs),
      error: () => this.error.set('药品目录加载失败'),
    });
  }

  protected readonly total = computed(() =>
    this.items().reduce((sum, item) => {
      const drug = this.drugs().find((candidate) => candidate.id === item.drugId);
      return sum + (drug?.unitPrice ?? 0) * item.quantity;
    }, 0),
  );

  protected addItem(): void {
    this.items.update((items) => [...items, this.emptyItem()]);
  }

  protected removeItem(index: number): void {
    this.items.update((items) => items.filter((_, i) => i !== index));
  }

  protected update(index: number, field: keyof EditableItem, event: Event): void {
    const raw = (event.target as HTMLInputElement | HTMLSelectElement).value;
    const value = field === 'drugId' || field === 'days' || field === 'quantity' ? Number(raw) : raw;
    this.items.update((items) =>
      items.map((item, i) => (i === index ? { ...item, [field]: value } : item)),
    );
  }

  protected save(): void {
    if (this.items().length === 0) {
      this.error.set('至少添加一条药品明细');
      return;
    }
    for (const item of this.items()) {
      if (
        !item.drugId ||
        !item.dosage.trim() ||
        !item.frequency.trim() ||
        !item.route.trim() ||
        item.days < 1 ||
        item.quantity < 1
      ) {
        this.error.set('请完整填写药品、剂量、频次、途径、天数与数量');
        return;
      }
    }
    const payload: PrescriptionPayload = {
      type: 'WESTERN',
      diagnosisSummary: this.diagnosis().trim() || null,
      medicalAdvice: this.advice().trim() || null,
      items: this.items().map((item) => ({
        drugId: item.drugId,
        dosage: item.dosage.trim(),
        frequency: item.frequency.trim(),
        route: item.route.trim(),
        days: item.days,
        quantity: item.quantity,
        note: item.note.trim() || null,
      })),
    };
    this.saving.set(true);
    const request$ = this.data.prescription
      ? this.api.update(this.data.prescription.header.id, payload)
      : this.api.create(this.data.encounterId, payload);
    request$.subscribe({
      next: (view) => this.ref.close(view),
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(toMedflowError(error).message);
      },
    });
  }

  private emptyItem(): EditableItem {
    return {
      key: ++this.itemKey,
      drugId: 0,
      dosage: '',
      frequency: '',
      route: '口服',
      days: 3,
      quantity: 1,
      note: '',
    };
  }
}
