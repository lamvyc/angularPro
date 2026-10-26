import { BreakpointObserver } from '@angular/cdk/layout';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatSidenavModule } from '@angular/material/sidenav';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { NAV_SECTIONS } from '../config/navigation';
import { Icon } from '../../shared/ui/icon/icon';
import { SystemStatusStore } from '../../features/dashboard/state/system-status.store';

@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatButtonModule,
    MatDividerModule,
    Icon,
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  private readonly breakpoint = inject(BreakpointObserver);
  protected readonly store = inject(SystemStatusStore);

  protected readonly navSections = NAV_SECTIONS;
  protected readonly opened = signal(true);
  protected readonly isHandset = toSignal(
    this.breakpoint.observe('(max-width: 900px)').pipe(map((result) => result.matches)),
    { initialValue: false },
  );
  protected readonly sidenavMode = computed<'over' | 'side'>(() =>
    this.isHandset() ? 'over' : 'side',
  );
  protected readonly statusLabel = computed(() => {
    switch (this.store.apiState()) {
      case 'up':
        return 'API 正常';
      case 'down':
        return 'API 异常';
      default:
        return '检查中';
    }
  });

  constructor() {
    this.store.startAutoRefresh();
    effect(() => {
      this.opened.set(!this.isHandset());
    });
  }

  protected toggleSidenav(): void {
    this.opened.update((value) => !value);
  }

  protected closeOnNavigate(): void {
    if (this.isHandset()) {
      this.opened.set(false);
    }
  }
}
