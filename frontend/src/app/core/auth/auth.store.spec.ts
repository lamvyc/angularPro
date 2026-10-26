import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { vi } from 'vitest';

import { AuthStore } from './auth.store';
import { TokenStorage } from './token-storage';
import type { LoginResponse, UserProfile } from './auth.models';

const PROFILE: UserProfile = {
  id: 2,
  username: 'doctor1',
  realName: '张明',
  deptId: 1,
  deptName: '皮肤科',
  title: '主任医师',
  roles: [{ code: 'DOCTOR', name: '医生', dataScope: 'DEPT' }],
  permissions: ['menu:dashboard', 'schedule:read'],
  dataScope: 'DEPT',
  menus: [],
};

const LOGIN: LoginResponse = {
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  expiresIn: 900,
  tokenType: 'Bearer',
  user: PROFILE,
};

describe('AuthStore', () => {
  let store: AuthStore;
  let tokens: TokenStorage;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(AuthStore);
    tokens = TestBed.inject(TokenStorage);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should login, store tokens and expose permissions', () => {
    store.login({ username: 'doctor1', password: 'Medflow@2026' }).subscribe();

    const request = httpMock.expectOne('/api/v1/auth/login');
    expect(request.request.body).toEqual({ username: 'doctor1', password: 'Medflow@2026' });
    request.flush(LOGIN);

    expect(store.isAuthenticated()).toBe(true);
    expect(store.user()?.realName).toBe('张明');
    expect(store.hasPermission('schedule:read')).toBe(true);
    expect(store.hasPermission('system:user:read')).toBe(false);
    expect(tokens.accessToken()).toBe('access-1');
    expect(localStorage.getItem('medflow.refresh-token')).toBe('refresh-1');
  });

  it('should refresh access token only once for concurrent callers', () => {
    tokens.setTokens('access-1', 'refresh-1');
    const values: string[] = [];

    store.refreshAccessToken().subscribe((token) => values.push(token.accessToken));
    store.refreshAccessToken().subscribe((token) => values.push(token.accessToken));
    store.refreshAccessToken().subscribe((token) => values.push(token.accessToken));

    const request = httpMock.expectOne('/api/v1/auth/refresh');
    request.flush({
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
      expiresIn: 900,
      tokenType: 'Bearer',
    });

    expect(values).toEqual(['access-2', 'access-2', 'access-2']);
    expect(tokens.accessToken()).toBe('access-2');
    expect(localStorage.getItem('medflow.refresh-token')).toBe('refresh-2');
  });

  it('should restore the session from refresh token', async () => {
    localStorage.setItem('medflow.refresh-token', 'refresh-1');

    const restored = store.restore();
    httpMock.expectOne('/api/v1/auth/refresh').flush({
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
      expiresIn: 900,
      tokenType: 'Bearer',
    });
    httpMock.expectOne('/api/v1/auth/me').flush(PROFILE);
    await restored;

    expect(store.isAuthenticated()).toBe(true);
    expect(store.user()?.deptName).toBe('皮肤科');
    expect(store.restored()).toBe(true);
  });

  it('should clear the session and navigate to login on logout', async () => {
    tokens.setTokens('access-1', 'refresh-1');
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    store.logout();
    httpMock
      .expectOne('/api/v1/auth/logout')
      .flush(null, { status: 204, statusText: 'No Content' });
    await Promise.resolve();

    expect(store.isAuthenticated()).toBe(false);
    expect(tokens.accessToken()).toBeNull();
    expect(localStorage.getItem('medflow.refresh-token')).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should normalize login errors', async () => {
    const promise = firstValueFrom(store.login({ username: 'admin', password: 'bad' })).catch(
      (error: unknown) => error,
    );
    httpMock
      .expectOne('/api/v1/auth/login')
      .flush(
        { code: 'MED-AUTH-001', detail: '用户名或密码错误' },
        { status: 401, statusText: 'Unauthorized' },
      );

    const error = (await promise) as { code: string; message: string };
    expect(error.code).toBe('MED-AUTH-001');
    expect(error.message).toBe('用户名或密码错误');
    expect(store.error()?.code).toBe('MED-AUTH-001');
  });
});
