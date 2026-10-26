import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize } from 'rxjs/operators';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { Icon } from '../../../shared/ui/icon/icon';
import type { EmrTemplateRow } from '../../workstation/data/workstation.models';
import { EmrTemplateApi, type EmrTemplatePayload } from './emr-template.api';
import { EmrTemplateDialog } from './emr-template-dialog';

@Component({
  selector: 'app-emr-templates-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatCardModule, MatDialogModule, MatProgressBarModule, Icon],
  templateUrl: './emr-templates-page.html',
  styleUrl: './emr-templates-page.scss',
})
export class EmrTemplatesPage {
  private readonly api = inject(EmrTemplateApi);
  private readonly dialog = inject(MatDialog);
  protected readonly templates = signal<readonly EmrTemplateRow[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<MedflowError | null>(null);
  protected readonly message = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (templates) => this.templates.set(templates),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected openCreate(): void {
    const ref = this.dialog.open<EmrTemplateDialog, { mode: 'create' }, EmrTemplatePayload>(
      EmrTemplateDialog,
      { width: '720px', data: { mode: 'create' } },
    );
    ref.afterClosed().subscribe((payload) => {
      if (payload) {
        this.api.create(payload).subscribe({
          next: () => {
            this.message.set('模板已创建并发布 v1');
            this.load();
          },
          error: (error: unknown) => this.error.set(toMedflowError(error)),
        });
      }
    });
  }

  protected openPublish(template: EmrTemplateRow): void {
    const ref = this.dialog.open<EmrTemplateDialog, { mode: 'publish'; template: EmrTemplateRow }, EmrTemplatePayload>(
      EmrTemplateDialog,
      { width: '720px', data: { mode: 'publish', template } },
    );
    ref.afterClosed().subscribe((payload) => {
      if (payload) {
        this.api.publish(template.id, payload).subscribe({
          next: (updated) => {
            this.message.set(`已发布 v${updated.versionNo}`);
            this.load();
          },
          error: (error: unknown) => this.error.set(toMedflowError(error)),
        });
      }
    });
  }

  protected archive(template: EmrTemplateRow): void {
    this.api.archive(template.id).subscribe({
      next: () => {
        this.message.set('模板已归档，历史病历仍引用旧版本');
        this.load();
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  protected clear(): void {
    this.error.set(null);
    this.message.set(null);
  }
}
