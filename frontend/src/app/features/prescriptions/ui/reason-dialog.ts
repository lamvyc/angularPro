import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-reason-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ data.label }}</mat-label>
        <textarea matInput rows="3" [formField]="editor.reason"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>返回</button>
      <button mat-flat-button type="button" (click)="submit()">{{ data.confirm }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    .full {
      width: 100%;
      min-width: 380px;
    }
  `,
})
export class ReasonDialog {
  private readonly ref = inject(MatDialogRef<ReasonDialog, string>);
  protected readonly data = inject<{ title: string; label: string; confirm: string }>(MAT_DIALOG_DATA);
  protected readonly model = signal({ reason: '' });
  protected readonly editor = form(this.model, (path) => {
    required(path.reason, { message: '原因必填' });
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (this.editor().valid()) {
      this.ref.close(this.model().reason.trim());
    }
  }
}
