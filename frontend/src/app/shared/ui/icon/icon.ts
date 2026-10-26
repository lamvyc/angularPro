import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * 内联 SVG 图标，避免依赖外部图标字体（离线可用、无 FOUT）。
 * 路径统一 24×24 视图框、线性风格。
 */
const ICON_PATHS = {
  dashboard: ['M4 4h6v6H4z', 'M14 4h6v4h-6z', 'M14 10h6v10h-6z', 'M4 12h6v8H4z'],
  calendar: ['M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14H4z', 'M8 2v5', 'M16 2v5', 'M4 10h16'],
  clipboard: ['M9 4h6v3H9z', 'M8 5H6v16h12V5h-2', 'M9 12h6', 'M9 16h4'],
  prescription: ['M14 3H6v18h12V7z', 'M14 3v4h4', 'M9 12h6', 'M9 16h6'],
  pill: ['M10.5 20.5a7 7 0 0 1-9.9-9.9l6.1-6.1a7 7 0 0 1 9.9 9.9z', 'M8.5 8.5l7 7'],
  package: ['M3 7l9-4 9 4-9 4z', 'M3 7v10l9 4 9-4V7', 'M12 11v10'],
  lock: ['M6 11h12v9H6z', 'M9 11V8a3 3 0 0 1 6 0v3'],
  users: [
    'M16 20v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1',
    'M9.5 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7',
    'M17 4a3 3 0 0 1 0 6',
    'M21 20v-1a3 3 0 0 0-2-2.8',
  ],
  refresh: ['M21 12a9 9 0 1 1-2.64-6.36', 'M21 3v6h-6'],
  check: ['M20 6 9 17l-5-5'],
  alert: ['M12 3 2 21h20z', 'M12 9v5', 'M12 18h.01'],
  server: ['M4 4h16v6H4z', 'M4 14h16v6H4z', 'M8 7h.01', 'M8 17h.01'],
  database: [
    'M12 3c4.4 0 8 1.3 8 3s-3.6 3-8 3-8-1.3-8-3 3.6-3 8-3z',
    'M4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6',
    'M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
  ],
  pulse: [
    'M20.8 5.6a5.5 5.5 0 0 0-7.8 0L12 6.6l-1-1a5.5 5.5 0 0 0-7.8 7.8L12 22l8.8-8.6a5.5 5.5 0 0 0 0-7.8z',
    'M3.5 12h4.5l2-3 3 6 2-3h5.5',
  ],
  menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
  arrowRight: ['M5 12h14', 'M13 6l6 6-6 6'],
  shield: ['M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z', 'M9 12l2 2 4-4'],
} as const;

export type IconName = keyof typeof ICON_PATHS;

@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      [attr.stroke-width]="strokeWidth()"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      @for (path of paths(); track path) {
        <path [attr.d]="path" />
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      line-height: 0;
      flex: none;
    }
  `,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly size = input(20);
  readonly strokeWidth = input(1.8);
  protected readonly paths = computed<readonly string[]>(() => ICON_PATHS[this.name()]);
}
