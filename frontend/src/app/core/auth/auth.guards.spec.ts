import { TestBed } from '@angular/core/testing';
import {
  provideRouter,
  type ActivatedRouteSnapshot,
  type RouterStateSnapshot,
} from '@angular/router';

import { authGuard, guestGuard, permissionGuard } from './auth.guards';
import { AuthStore } from './auth.store';

const route = { data: {} } as unknown as ActivatedRouteSnapshot;
const state = { url: '/dashboard' } as RouterStateSnapshot;

describe('auth guards', () => {
  it('should redirect unauthenticated users to login with returnUrl', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthStore,
          useValue: { isAuthenticated: () => false, hasPermission: () => false },
        },
      ],
    });

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(String(result)).toContain('/login');
    expect(String(result)).toContain('returnUrl');
  });

  it('should allow authenticated users and redirect guests away from login', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthStore,
          useValue: { isAuthenticated: () => true, hasPermission: () => false },
        },
      ],
    });

    expect(TestBed.runInInjectionContext(() => authGuard(route, state))).toBe(true);
    expect(String(TestBed.runInInjectionContext(() => guestGuard(route, state)))).toContain(
      '/dashboard',
    );
  });

  it('should deny routes when the required permission is missing', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthStore,
          useValue: { isAuthenticated: () => true, hasPermission: () => false },
        },
      ],
    });

    const protectedRoute = {
      data: { permission: 'system:user:read' },
    } as unknown as ActivatedRouteSnapshot;
    expect(
      String(TestBed.runInInjectionContext(() => permissionGuard(protectedRoute, state))),
    ).toContain('/403');
  });
});
