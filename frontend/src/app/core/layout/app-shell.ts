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

import { AuthStore } from '../auth/auth.store';
import type { MenuNode } from '../auth/auth.models';
import { SystemStatusStore } from '../../features/dashboard/state/system-status.store';
import { Icon, type IconName } from '../../shared/ui/icon/icon';

interface NavEntry {
  readonly label: string;
  readonly route: string;
  readonly icon: IconName;
}

interface NavGroup {
  readonly title: string;
  readonly items: readonly NavEntry[];
}

const ICONS: Readonly<Record<string, IconName>> = {
  dashboard: 'dashboard',
  users: 'users',
  lock: 'lock',
  clipboard: 'clipboard',
  shield: 'shield',
  calendar: 'calendar',
  package: 'package',
  pulse: 'pulse',
  prescription: 'prescription',
  database: 'database',
  server: 'server',
};

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
  protected readonly auth = inject(AuthStore);

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
  protected readonly roleLabel = computed(() =>
    this.auth
      .roles()
      .map((role) => role.name)
      .join(' / '),
  );

  protected readonly navGroups = computed<readonly NavGroup[]>(() => {
    const menus = this.auth.menus();
    const groups: NavGroup[] = [];

    const leaves = menus
      .filter((menu) => menu.children.length === 0)
      .map((menu) => this.toEntry(menu))
      .filter((entry): entry is NavEntry => entry !== null);
    if (leaves.length > 0) {
      groups.push({ title: '导航', items: leaves });
    }

    for (const menu of menus.filter((item) => item.children.length > 0)) {
      const items = menu.children
        .map((child) => this.toEntry(child))
        .filter((entry): entry is NavEntry => entry !== null);
      if (items.length > 0) {
        groups.push({ title: menu.name, items });
      }
    }
    return groups;
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

  protected logout(): void {
    this.auth.logout();
  }

  private toEntry(menu: MenuNode): NavEntry | null {
    if (!menu.path) {
      return null;
    }
    return {
      label: menu.name,
      route: menu.path,
      icon: ICONS[menu.icon ?? ''] ?? 'arrowRight',
    };
  }
}
