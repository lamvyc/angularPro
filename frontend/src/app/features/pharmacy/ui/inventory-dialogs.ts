import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { toMedflowError } from '../../../core/http/problem-detail';
import { PharmacyApi } from '../data/pharmacy.api';
import type { BatchView, DrugAdmin } from '../data/pharmacy.models';

@Component({
  selector: 'app-drug-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data.drug ? '编辑药品' : '新建药品' }}</h2>
    <mat-dialog-content>
      @if (error(); as error) {
        <p class="error">{{ error }}</p>
      }
      <div class="grid">
        @if (!data.drug) {
          <mat-form-field appearance="outline">
            <mat-label>药品编码</mat-label>
            <input matInput [value]="model().code" (input)="set('code', $event)" />
          </mat-form-field>
        }
        <mat-form-field appearance="outline">
          <mat-label>商品名</mat-label>
          <input matInput [value]="model().name" (input)="set('name', $event)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>通用名</mat-label>
          <input matInput [value]="model().genericName" (input)="set('genericName', $event)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>规格</mat-label>
          <input matInput [value]="model().spec" (input)="set('spec', $event)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>单位</mat-label>
          <input matInput [value]="model().unit" (input)="set('unit', $event)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>剂型</mat-label>
          <input matInput [value]="model().dosageForm" (input)="set('dosageForm', $event)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>生产厂家</mat-label>
          <input matInput [value]="model().manufacturer" (input)="set('manufacturer', $event)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>价格</mat-label>
          <input matInput type="number" [value]="model().unitPrice" (input)="set('unitPrice', $event)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>类别</mat-label>
          <select matNativeControl [value]="model().category" (change)="set('category', $event)">
            <option value="WESTERN">西药</option>
            <option value="HERBAL">中药</option>
          </select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>处方类型</mat-label>
          <select matNativeControl [value]="model().prescriptionType" (change)="set('prescriptionType', $event)">
            <option value="RX">处方药</option>
            <option value="OTC">非处方药</option>
          </select>
        </mat-form-field>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="save()" [disabled]="saving()">保存</button>
    </mat-dialog-actions>
  `,
  styles: `
    .grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 4px 12px;
      min-width: 620px;
    }

    .error {
      margin: 0 0 8px;
      color: var(--mf-danger);
      font-size: 11px;
    }
  `,
})
export class DrugDialog {
  private readonly api = inject(PharmacyApi);
  private readonly ref = inject(MatDialogRef<DrugDialog, DrugAdmin>);
  protected readonly data = inject<{ drug?: DrugAdmin }>(MAT_DIALOG_DATA);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly model = signal({
    code: this.data.drug?.code ?? '',
    name: this.data.drug?.name ?? '',
    genericName: this.data.drug?.genericName ?? '',
    spec: this.data.drug?.spec ?? '',
    unit: this.data.drug?.unit ?? '盒',
    dosageForm: this.data.drug?.dosageForm ?? '',
    manufacturer: this.data.drug?.manufacturer ?? '',
    category: this.data.drug?.category ?? 'WESTERN',
    unitPrice: this.data.drug?.unitPrice ?? 0,
    prescriptionType: this.data.drug?.prescriptionType ?? 'RX',
  });

  protected set(field: string, event: Event): void {
    const raw = (event.target as HTMLInputElement | HTMLSelectElement).value;
    this.model.update((model) => ({
      ...model,
      [field]: field === 'unitPrice' ? Number(raw) : raw,
    }));
  }

  protected save(): void {
    const value = this.model();
    if (!value.name.trim() || !value.spec.trim() || !value.unit.trim() || value.unitPrice < 0) {
      this.error.set('请填写名称、规格、单位与非负价格');
      return;
    }
    if (!this.data.drug && !value.code.trim()) {
      this.error.set('请填写药品编码');
      return;
    }
    this.saving.set(true);
    const payload = {
      code: value.code.trim(),
      name: value.name.trim(),
      genericName: value.genericName.trim() || null,
      spec: value.spec.trim(),
      unit: value.unit.trim(),
      dosageForm: value.dosageForm.trim() || null,
      manufacturer: value.manufacturer.trim() || null,
      category: value.category,
      unitPrice: value.unitPrice,
      prescriptionType: value.prescriptionType,
    };
    const request$ = this.data.drug
      ? this.api.updateDrug(this.data.drug.id, payload)
      : this.api.createDrug(payload);
    request$.subscribe({
      next: (drug) => this.ref.close(drug),
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(toMedflowError(error).message);
      },
    });
  }
}

@Component({
  selector: 'app-batch-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>批次入库</h2>
    <mat-dialog-content>
      @if (error(); as error) {
        <p class="error">{{ error }}</p>
      }
      <div class="grid">
        <mat-form-field appearance="outline">
          <mat-label>药品</mat-label>
          <select matNativeControl [value]="drugId()" (change)="drugId.set(+$any($event.target).value)">
            <option [value]="0">选择药品</option>
            @for (drug of data.drugs; track drug.id) {
              <option [value]="drug.id">{{ drug.name }} · {{ drug.spec }}</option>
            }
          </select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>批号</mat-label>
          <input matInput [value]="batchNo()" (input)="batchNo.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>生产日期</mat-label>
          <input matInput type="date" [value]="productionDate()" (input)="productionDate.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>有效期</mat-label>
          <input matInput type="date" [value]="expiryDate()" (input)="expiryDate.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>入库数量</mat-label>
          <input matInput type="number" [value]="quantity()" (input)="quantity.set(+$any($event.target).value)" />
        </mat-form-field>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="save()" [disabled]="saving()">入库</button>
    </mat-dialog-actions>
  `,
  styles: `
    .grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 4px 12px;
      min-width: 560px;
    }

    .error {
      margin: 0 0 8px;
      color: var(--mf-danger);
      font-size: 11px;
    }
  `,
})
export class BatchDialog {
  private readonly api = inject(PharmacyApi);
  private readonly ref = inject(MatDialogRef<BatchDialog, BatchView>);
  protected readonly data = inject<{ drugs: readonly DrugAdmin[] }>(MAT_DIALOG_DATA);
  protected readonly drugId = signal(0);
  protected readonly batchNo = signal('');
  protected readonly productionDate = signal('');
  protected readonly expiryDate = signal('');
  protected readonly quantity = signal(1);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected save(): void {
    if (!this.drugId() || !this.batchNo().trim() || !this.expiryDate() || this.quantity() < 1) {
      this.error.set('请完整填写药品、批号、有效期与数量');
      return;
    }
    this.saving.set(true);
    this.api
      .createBatch({
        drugId: this.drugId(),
        batchNo: this.batchNo().trim(),
        productionDate: this.productionDate() || null,
        expiryDate: this.expiryDate(),
        quantity: this.quantity(),
      })
      .subscribe({
        next: (batch) => this.ref.close(batch),
        error: (error: unknown) => {
          this.saving.set(false);
          this.error.set(toMedflowError(error).message);
        },
      });
  }
}
