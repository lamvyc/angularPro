import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { WorkstationApi } from '../data/workstation.api';
import type { EmrVersionView } from '../data/workstation.models';

@Component({
  selector: 'app-amend-reason-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>修订已签署病历</h2>
    <mat-dialog-content>
      <p class="hint">修订将生成新版本，旧版本置为 SUPERSEDED，并记录修订原因与内容哈希。</p>
      <mat-form-field appearance="outline" class="full">
        <mat-label>修订原因</mat-label>
        <textarea matInput rows="3" [formField]="editor.reason"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>返回</button>
      <button mat-flat-button type="button" (click)="submit()">进入修订</button>
    </mat-dialog-actions>
  `,
  styles: `
    .full {
      width: 100%;
      min-width: 380px;
    }

    .hint {
      margin-bottom: 12px;
      color: var(--mf-muted);
      font-size: 12px;
      line-height: 1.6;
    }
  `,
})
export class AmendReasonDialog {
  private readonly ref = inject(MatDialogRef<AmendReasonDialog, string>);
  protected readonly model = signal({ reason: '' });
  protected readonly editor = form(this.model, (path) => {
    required(path.reason, { message: '修订原因必填' });
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (this.editor().valid()) {
      this.ref.close(this.model().reason.trim());
    }
  }
}

@Component({
  selector: 'app-emr-history-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>病历版本历史</h2>
    <mat-dialog-content>
      @if (loading()) {
        <p class="muted">加载版本…</p>
      } @else {
        <table class="version-table">
          <thead>
            <tr>
              <th>版本</th>
              <th>状态</th>
              <th>签署人</th>
              <th>时间</th>
              <th>原因</th>
              <th>对比</th>
            </tr>
          </thead>
          <tbody>
            @for (version of versions(); track version.versionNo) {
              <tr>
                <td class="mf-mono">v{{ version.versionNo }}</td>
                <td>{{ version.status }}</td>
                <td>{{ version.signedByName ?? '—' }}</td>
                <td>{{ version.signedAt ?? '—' }}</td>
                <td>{{ version.changeReason ?? '首次签署' }}</td>
                <td>
                  <button mat-button type="button" (click)="setFrom(version.versionNo)">A</button>
                  <button mat-button type="button" (click)="setTo(version.versionNo)">B</button>
                </td>
              </tr>
            }
          </tbody>
        </table>

        @if (fromVersion() !== null && toVersion() !== null) {
          <div class="compare">
            <h3>对比 v{{ fromVersion() }} → v{{ toVersion() }}</h3>
            @for (change of changes(); track change.key) {
              <div class="change">
                <strong>{{ change.key }}</strong>
                <span class="before">{{ change.before }}</span>
                <span class="arrow">→</span>
                <span class="after">{{ change.after }}</span>
              </div>
            } @empty {
              <p class="muted">两个版本内容一致</p>
            }
          </div>
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>关闭</button>
    </mat-dialog-actions>
  `,
  styles: `
    .version-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 11.5px;
    }

    th,
    td {
      padding: 6px 8px;
      border-bottom: 1px solid var(--mf-border);
      text-align: left;
    }

    .compare {
      margin-top: 16px;
    }

    .change {
      display: flex;
      gap: 8px;
      padding: 6px 0;
      border-bottom: 1px dashed var(--mf-border);
      font-size: 11.5px;
    }

    .before {
      color: var(--mf-danger);
      text-decoration: line-through;
    }

    .after {
      color: var(--mf-success);
    }

    .arrow {
      color: var(--mf-muted);
    }

    .muted {
      color: var(--mf-muted);
      font-size: 12px;
    }
  `,
})
export class EmrHistoryDialog {
  private readonly api = inject(WorkstationApi);
  private readonly data = inject<{ encounterId: number }>(MAT_DIALOG_DATA);
  protected readonly versions = signal<readonly EmrVersionView[]>([]);
  protected readonly loading = signal(true);
  protected readonly fromVersion = signal<number | null>(null);
  protected readonly toVersion = signal<number | null>(null);

  constructor() {
    this.api.versions(this.data.encounterId).subscribe({
      next: (versions) => {
        this.versions.set(versions);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected setFrom(versionNo: number): void {
    this.fromVersion.set(versionNo);
  }

  protected setTo(versionNo: number): void {
    this.toVersion.set(versionNo);
  }

  protected readonly changes = computed(() => {
    const from = this.versions().find((v) => v.versionNo === this.fromVersion());
    const to = this.versions().find((v) => v.versionNo === this.toVersion());
    if (!from || !to) {
      return [];
    }
    const keys = new Set([...Object.keys(from.content), ...Object.keys(to.content)]);
    return [...keys]
      .map((key) => ({
        key,
        before: this.render(from.content[key]),
        after: this.render(to.content[key]),
      }))
      .filter((change) => change.before !== change.after);
  });

  private render(value: unknown): string {
    if (value === undefined || value === null || value === '') {
      return '（空）';
    }
    return Array.isArray(value) ? value.join('、') : String(value);
  }
}
