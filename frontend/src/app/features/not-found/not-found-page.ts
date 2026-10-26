import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { Icon } from '../../shared/ui/icon/icon';

@Component({
  selector: 'app-not-found-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, RouterLink, Icon],
  template: `
    <div class="mf-page not-found">
      <div class="panel">
        <span class="code">404</span>
        <h1>页面不存在</h1>
        <p>你访问的页面可能已移动，或对应的模块尚未在 P0 阶段开放。</p>
        <a mat-flat-button routerLink="/dashboard">
          <app-icon name="arrowRight" [size]="16" />
          返回工作台
        </a>
      </div>
    </div>
  `,
  styles: `
    .not-found {
      display: grid;
      place-items: center;
      min-height: 60vh;
    }

    .panel {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 48px;
      border: 1px solid var(--mf-border);
      border-radius: var(--mf-radius-lg);
      background: var(--mf-surface);
      box-shadow: var(--mf-shadow-soft);
      text-align: center;
    }

    .code {
      color: var(--mf-primary);
      font-family: var(--mf-mono);
      font-size: 13px;
      font-weight: 700;
      letter-spacing: 0.2em;
    }

    h1 {
      font-size: 24px;
      letter-spacing: -0.02em;
    }

    p {
      max-width: 380px;
      color: var(--mf-muted);
      font-size: 13px;
    }

    a {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      margin-top: 8px;
      border-radius: 999px;
    }
  `,
})
export class NotFoundPage {}
