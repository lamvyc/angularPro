import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  catchError,
  finalize,
  firstValueFrom,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
  type Observable,
} from 'rxjs';

import { MedflowError, toMedflowError } from '../http/problem-detail';
import { AuthApi } from './auth.api';
import type { LoginCredentials, TokenResponse, UserProfile } from './auth.models';
import { TokenStorage } from './token-storage';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApi);
  private readonly tokens = inject(TokenStorage);
  private readonly router = inject(Router);

  private readonly _user = signal<UserProfile | null>(null);
  private readonly _loading = signal(false);
  private readonly _restoring = signal(false);
  private readonly _restored = signal(false);
  private readonly _error = signal<MedflowError | null>(null);
  private refreshInFlight: Observable<TokenResponse> | null = null;

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly restoring = this._restoring.asReadonly();
  readonly restored = this._restored.asReadonly();
  readonly error = this._error.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly menus = computed(() => this._user()?.menus ?? []);
  readonly permissions = computed(() => new Set(this._user()?.permissions ?? []));
  readonly roles = computed(() => this._user()?.roles ?? []);
  readonly displayName = computed(() => this._user()?.realName ?? '未登录');
  readonly departmentName = computed(() => this._user()?.deptName ?? '');
  readonly initials = computed(() => this.displayName().slice(0, 1).toUpperCase());

  login(credentials: LoginCredentials): Observable<UserProfile> {
    this._loading.set(true);
    this._error.set(null);
    return this.api.login(credentials).pipe(
      tap((response) => {
        this.tokens.setTokens(response.accessToken, response.refreshToken);
        this._user.set(response.user);
      }),
      map((response) => response.user),
      catchError((error: unknown) => {
        const normalized = toMedflowError(error);
        this._error.set(normalized);
        return throwError(() => normalized);
      }),
      finalize(() => this._loading.set(false)),
    );
  }

  restore(): Promise<void> {
    const refreshToken = this.tokens.refreshToken();
    if (!refreshToken) {
      this._restored.set(true);
      return Promise.resolve();
    }
    this._restoring.set(true);
    return firstValueFrom(
      this.refreshAccessToken().pipe(
        switchMap(() => this.api.me()),
        tap((user) => this._user.set(user)),
        catchError(() => {
          this.clearSession();
          return of(null);
        }),
        finalize(() => {
          this._restoring.set(false);
          this._restored.set(true);
        }),
      ),
    ).then(() => undefined);
  }

  refreshAccessToken(): Observable<TokenResponse> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }
    const refreshToken = this.tokens.refreshToken();
    if (!refreshToken) {
      return throwError(() => new MedflowError('会话已失效', 'MED-AUTH-003', 401));
    }
    this.refreshInFlight = this.api.refresh(refreshToken).pipe(
      tap((response) => this.tokens.setTokens(response.accessToken, response.refreshToken)),
      finalize(() => {
        this.refreshInFlight = null;
      }),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    return this.refreshInFlight;
  }

  logout(): void {
    const refreshToken = this.tokens.refreshToken();
    this.api
      .logout(refreshToken)
      .pipe(
        catchError(() => of(undefined)),
        finalize(() => {
          this.clearSession();
          void this.router.navigate(['/login']);
        }),
      )
      .subscribe();
  }

  handleUnauthorized(): void {
    this.clearSession();
    const returnUrl = this.router.url;
    void this.router.navigate(['/login'], {
      queryParams: returnUrl && returnUrl !== '/login' ? { returnUrl } : {},
    });
  }

  clearSession(): void {
    this.tokens.clear();
    this._user.set(null);
    this._error.set(null);
  }

  hasPermission(permission: string | readonly string[]): boolean {
    const permissions = this.permissions();
    const required: readonly string[] = typeof permission === 'string' ? [permission] : permission;
    return required.some((item) => permissions.has(item));
  }
}
