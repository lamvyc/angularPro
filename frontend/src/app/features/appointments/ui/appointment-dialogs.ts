import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import type { CreatePatientPayload } from '../data/appointment.models';

@Component({
  selector: 'app-patient-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>患者建档</h2>
    <mat-dialog-content>
      <form class="form">
        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>姓名</mat-label>
            <input id="patient-name" matInput [formField]="editor.name" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>性别</mat-label>
            <select id="patient-gender" matNativeControl [formField]="editor.gender">
              <option value="FEMALE">女</option>
              <option value="MALE">男</option>
              <option value="UNKNOWN">未知</option>
            </select>
          </mat-form-field>
        </div>
        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>出生日期</mat-label>
            <input id="patient-birth" matInput type="date" [formField]="editor.birthDate" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>手机号</mat-label>
            <input id="patient-phone" matInput [formField]="editor.phone" />
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>证件号（仅存哈希与掩码）</mat-label>
          <input id="patient-id-card" matInput [formField]="editor.idCardNo" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>过敏史</mat-label>
          <textarea matInput rows="2" [formField]="editor.allergyHistory"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()">建档</button>
    </mat-dialog-actions>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 480px;
      padding-top: 8px;
    }

    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
  `,
})
export class PatientDialog {
  private readonly dialogRef = inject(MatDialogRef<PatientDialog, CreatePatientPayload>);
  protected readonly model = signal({
    name: '',
    gender: 'FEMALE',
    birthDate: '1990-01-01',
    idCardNo: '',
    phone: '',
    allergyHistory: '',
  });
  protected readonly editor = form(this.model, (path) => {
    required(path.name, { message: '请输入姓名' });
    required(path.birthDate, { message: '请选择出生日期' });
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (!this.editor().valid()) {
      return;
    }
    const value = this.model();
    this.dialogRef.close({
      name: value.name.trim(),
      gender: value.gender,
      birthDate: value.birthDate,
      idCardNo: value.idCardNo.trim() || null,
      phone: value.phone.trim() || null,
      address: null,
      allergyHistory: value.allergyHistory.trim() || null,
    });
  }
}

@Component({
  selector: 'app-cancel-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>取消预约</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full">
        <mat-label>取消原因</mat-label>
        <textarea matInput rows="2" [formField]="editor.reason"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>返回</button>
      <button mat-flat-button type="button" (click)="submit()">确认取消</button>
    </mat-dialog-actions>
  `,
  styles: `
    .full {
      width: 100%;
      min-width: 360px;
    }
  `,
})
export class CancelDialog {
  private readonly dialogRef = inject(MatDialogRef<CancelDialog, string>);
  protected readonly model = signal({ reason: '患者临时有事' });
  protected readonly editor = form(this.model, (path) => {
    required(path.reason, { message: '请输入取消原因' });
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (this.editor().valid()) {
      this.dialogRef.close(this.model().reason.trim());
    }
  }
}
