import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, RouterLink],
  template: `
    <div class="mf-page forbidden">
      <div class="panel">
        <span class="badge">403 · FORBIDDEN</span>
        <h1>没有访问权限</h1>
        <p>当前角色无权访问此页面。若你需要该权限，请联系系统管理员在角色授权中开启。</p>
        <div class="actions">
          <a mat-flat-button routerLink="/dashboard">返回工作台</a>
        </div>
      </div>
    </div>
  `,
  styles: `
    .forbidden {
      display: grid;
      place-items: center;
      min-height: 58vh;
    }

    .panel {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      max-width: 520px;
      padding: 46px;
      border: 1px solid var(--mf-border);
      border-radius: var(--mf-radius-lg);
      background: var(--mf-surface);
      box-shadow: var(--mf-shadow-soft);
      text-align: center;
    }

    .badge {
      color: var(--mf-warning);
      font-family: var(--mf-mono);
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.14em;
    }

    h1 {
      font-size: 24px;
      letter-spacing: -0.02em;
    }

    p {
      color: var(--mf-muted);
      font-size: 13px;
      line-height: 1.7;
    }

    .actions {
      display: flex;
      gap: 10px;
      margin-top: 8px;
    }
  `,
})
export class ForbiddenPage {}
