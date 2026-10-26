import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AuthStore } from '../../../core/auth/auth.store';
import { Icon } from '../../../shared/ui/icon/icon';
import { StaffStore } from '../state/staff.store';

@Component({
  selector: 'app-staff-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatCardModule, MatProgressBarModule, Icon],
  template: `
    <div class="mf-page">
      <header class="page-head">
        <div>
          <span class="mf-eyebrow">数据范围 · {{ dataScopeLabel() }}</span>
          <h1>科室人员</h1>
          <p>列表由后端按当前用户的科室数据范围过滤，前端无法通过参数绕过。</p>
        </div>
        <button
          mat-stroked-button
          type="button"
          (click)="store.load()"
          [disabled]="store.loading()"
        >
          <app-icon name="refresh" [size]="16" />
          刷新
        </button>
      </header>

      <mat-card appearance="outlined" class="table-card">
        @if (store.loading()) {
          <mat-progress-bar mode="indeterminate" />
        }
        @if (store.error(); as error) {
          <div class="error" role="alert">
            <app-icon name="alert" [size]="16" />
            {{ error.message }} <code>{{ error.code }}</code>
          </div>
        } @else {
          <table>
            <thead>
              <tr>
                <th>医生</th>
                <th>科室</th>
                <th>职称</th>
                <th>数据范围</th>
              </tr>
            </thead>
            <tbody>
              @for (doctor of store.doctors(); track doctor.id) {
                <tr>
                  <td>
                    <span class="avatar">{{ doctor.realName.slice(0, 1) }}</span>
                    <strong>{{ doctor.realName }}</strong>
                  </td>
                  <td>{{ doctor.deptName }}</td>
                  <td>{{ doctor.title ?? '—' }}</td>
                  <td><span class="scope">仅本科室</span></td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="4" class="empty">当前数据范围内暂无医生</td>
                </tr>
              }
            </tbody>
          </table>
        }
        <footer class="table-foot">共 {{ store.total() }} 位医生</footer>
      </mat-card>
    </div>
  `,
  styles: `
    .page-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 20px;

      h1 {
        margin-top: 8px;
        font-size: 26px;
        letter-spacing: -0.025em;
      }

      p {
        margin-top: 6px;
        color: var(--mf-muted);
        font-size: 13px;
      }
    }

    .table-card {
      overflow: hidden;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th,
    td {
      padding: 14px 20px;
      border-bottom: 1px solid var(--mf-border);
      text-align: left;
      font-size: 13px;
    }

    th {
      background: #f8fafd;
      color: var(--mf-muted);
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }

    td {
      color: var(--mf-ink-soft);
    }

    td:first-child {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .avatar {
      display: grid;
      place-items: center;
      width: 30px;
      height: 30px;
      border-radius: 50%;
      background: var(--mf-primary-soft);
      color: var(--mf-primary);
      font-size: 12px;
      font-weight: 700;
    }

    .scope {
      padding: 3px 9px;
      border-radius: 999px;
      background: var(--mf-success-soft);
      color: var(--mf-success);
      font-size: 11px;
      font-weight: 650;
    }

    .empty {
      padding: 40px;
      text-align: center;
      color: var(--mf-muted);
    }

    .error {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 18px;
      padding: 12px;
      border-radius: var(--mf-radius-sm);
      background: var(--mf-danger-soft);
      color: var(--mf-danger);
      font-size: 12px;
    }

    .table-foot {
      padding: 12px 20px;
      color: var(--mf-muted);
      font-size: 11px;
    }
  `,
})
export class StaffPage {
  protected readonly store = inject(StaffStore);
  private readonly auth = inject(AuthStore);

  protected readonly dataScopeLabel = computed(() =>
    this.auth.user()?.dataScope === 'ALL' ? '全院' : '本科室',
  );

  constructor() {
    this.store.load();
  }
}
