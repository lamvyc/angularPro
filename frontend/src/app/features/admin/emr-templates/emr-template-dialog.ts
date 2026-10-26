import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import type { EmrTemplateRow } from '../../workstation/data/workstation.models';
import type { EmrTemplatePayload } from './emr-template.api';

const DEFAULT_SCHEMA = {
  sections: [
    {
      key: 'chief',
      title: '主诉与现病史',
      fields: [
        {
          key: 'chiefComplaint',
          type: 'textarea',
          label: '主诉',
          required: true,
          maxLength: 200,
        },
        { key: 'presentIllness', type: 'textarea', label: '现病史', maxLength: 1000 },
      ],
    },
  ],
};

@Component({
  selector: 'app-emr-template-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? '新建病历模板' : '发布新版本' }}</h2>
    <mat-dialog-content>
      <form class="form">
        @if (data.mode === 'create') {
          <div class="two-col">
            <mat-form-field appearance="outline">
              <mat-label>模板编码</mat-label>
              <input matInput [formField]="editor.code" placeholder="GENERAL_FIRST" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>模板名称</mat-label>
              <input matInput [formField]="editor.name" />
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline">
            <mat-label>适用范围</mat-label>
            <select matNativeControl [formField]="editor.scopeType">
              <option value="HOSPITAL">全院</option>
              <option value="DEPT">科室</option>
            </select>
          </mat-form-field>
        }
        <mat-form-field appearance="outline">
          <mat-label>Schema JSON</mat-label>
          <textarea matInput rows="16" class="schema" [formField]="editor.schema"></textarea>
        </mat-form-field>
        @if (schemaError()) {
          <p class="error">{{ schemaError() }}</p>
        }
        <mat-form-field appearance="outline">
          <mat-label>变更说明</mat-label>
          <input matInput [formField]="editor.changelog" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()">
        {{ data.mode === 'create' ? '创建并发布' : '发布新版本' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 620px;
    }

    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }

    .schema {
      font-family: var(--mf-mono);
      font-size: 11px;
    }

    .error {
      margin: 0 0 8px;
      color: var(--mf-danger);
      font-size: 11px;
    }
  `,
})
export class EmrTemplateDialog {
  private readonly ref = inject(MatDialogRef<EmrTemplateDialog, EmrTemplatePayload>);
  protected readonly data = inject<{ mode: 'create' | 'publish'; template?: EmrTemplateRow }>(
    MAT_DIALOG_DATA,
  );
  protected readonly schemaError = signal<string | null>(null);
  protected readonly model = signal({
    code: '',
    name: '',
    scopeType: 'HOSPITAL',
    schema: JSON.stringify(this.data.template?.schemaJson
      ? JSON.parse(this.data.template.schemaJson)
      : DEFAULT_SCHEMA, null, 2),
    changelog: '',
  });
  protected readonly editor = form(this.model, (path) => {
    if (this.data.mode === 'create') {
      required(path.code, { message: '编码必填' });
      required(path.name, { message: '名称必填' });
    }
    required(path.schema, { message: 'Schema 必填' });
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (!this.editor().valid()) {
      return;
    }
    let schema: Record<string, unknown>;
    try {
      schema = JSON.parse(this.model().schema) as Record<string, unknown>;
    } catch {
      this.schemaError.set('Schema 不是合法 JSON');
      return;
    }
    if (!Array.isArray(schema['sections']) || schema['sections'].length === 0) {
      this.schemaError.set('Schema 必须包含 sections');
      return;
    }
    this.schemaError.set(null);
    this.ref.close({
      code: this.model().code.trim(),
      name: this.model().name.trim(),
      scopeType: this.model().scopeType as 'HOSPITAL' | 'DEPT',
      deptId: null,
      schema,
      changelog: this.model().changelog.trim() || null,
    });
  }
}
