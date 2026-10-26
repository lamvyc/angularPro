import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin, timer, type Subscription } from 'rxjs';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { SystemApi } from '../data/system.api';
import type { HealthResponse, PingResponse, ServiceState } from '../data/system.models';

/**
 * 工作台系统状态 Store。
 * - 同步派生状态用 computed；异步请求用 RxJS；组件只读信号。
 * - 轮询由组件显式启动，避免在构造函数里隐式发请求导致测试不可控。
 */
@Injectable({ providedIn: 'root' })
export class SystemStatusStore {
  private readonly api = inject(SystemApi);
  private readonly destroyRef = inject(DestroyRef);

  private readonly _health = signal<HealthResponse | null>(null);
  private readonly _ping = signal<PingResponse | null>(null);
  private readonly _loading = signal(false);
  private readonly _error = signal<MedflowError | null>(null);
  private readonly _lastCheckedAt = signal<Date | null>(null);
  private pollSubscription: Subscription | null = null;

  readonly health = this._health.asReadonly();
  readonly ping = this._ping.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly lastCheckedAt = this._lastCheckedAt.asReadonly();

  readonly apiState = computed<ServiceState>(() => {
    if (this._error() !== null) {
      return 'down';
    }
    const health = this._health();
    if (health === null) {
      return 'unknown';
    }
    return health.status === 'UP' ? 'up' : 'down';
  });

  readonly dbState = computed<ServiceState>(() => this.componentState('db'));
  readonly redisState = computed<ServiceState>(() => this.componentState('redis'));
  readonly serverTime = computed<string | null>(() => this._ping()?.serverTime ?? null);

  refresh(): void {
    if (this._loading()) {
      return;
    }
    this._loading.set(true);
    forkJoin({ health: this.api.health(), ping: this.api.ping() })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this._loading.set(false)),
      )
      .subscribe({
        next: ({ health, ping }) => {
          this._health.set(health);
          this._ping.set(ping);
          this._error.set(null);
          this._lastCheckedAt.set(new Date());
        },
        error: (error: unknown) => {
          this._health.set(null);
          this._ping.set(null);
          this._error.set(toMedflowError(error));
          this._lastCheckedAt.set(new Date());
        },
      });
  }

  startAutoRefresh(intervalMs = 30_000): void {
    this.refresh();
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = timer(intervalMs, intervalMs)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh());
  }

  private componentState(name: string): ServiceState {
    const health = this._health();
    if (health === null || health.components === undefined) {
      return 'unknown';
    }
    const component = health.components[name];
    if (component === undefined) {
      return 'unknown';
    }
    return component.status === 'UP' ? 'up' : 'down';
  }
}
