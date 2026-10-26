import { BreakpointObserver, type BreakpointState } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AppShell } from './app-shell';
import { AuthStore } from '../auth/auth.store';
import type { MenuNode, UserProfile } from '../auth/auth.models';
import { SystemStatusStore } from '../../features/dashboard/state/system-status.store';
import type { MedflowError } from '../http/problem-detail';
import type { ServiceState } from '../../features/dashboard/data/system.models';

function storeStub() {
  return {
    apiState: signal<ServiceState>('up'),
    dbState: signal<ServiceState>('up'),
    redisState: signal<ServiceState>('unknown'),
    loading: signal(false),
    error: signal<MedflowError | null>(null),
    lastCheckedAt: signal<Date | null>(null),
    serverTime: signal<string | null>(null),
    refresh: vi.fn(),
    startAutoRefresh: vi.fn(),
  } as unknown as SystemStatusStore;
}

const MENUS: readonly MenuNode[] = [
  {
    id: 1,
    code: 'menu:dashboard',
    name: '工作台',
    path: '/dashboard',
    icon: 'dashboard',
    children: [],
  },
  { id: 2, code: 'menu:staff', name: '科室人员', path: '/staff', icon: 'users', children: [] },
  {
    id: 3,
    code: 'menu:system',
    name: '系统管理',
    path: null,
    icon: 'lock',
    children: [
      {
        id: 4,
        code: 'system:dept:read',
        name: '科室管理',
        path: '/admin/departments',
        icon: 'clipboard',
        children: [],
      },
    ],
  },
];

function authStub() {
  const user = {
    id: 2,
    username: 'doctor1',
    realName: '张明',
    deptId: 1,
    deptName: '皮肤科',
    title: '主任医师',
    roles: [{ code: 'DOCTOR', name: '医生', dataScope: 'DEPT' }],
    permissions: ['menu:dashboard', 'schedule:read', 'system:dept:read'],
    dataScope: 'DEPT',
    menus: MENUS,
  } as UserProfile;
  return {
    user: signal(user),
    menus: signal(MENUS),
    roles: signal(user.roles),
    displayName: signal('张明'),
    departmentName: signal('皮肤科'),
    initials: signal('张'),
    isAuthenticated: signal(true),
    logout: vi.fn(),
  } as unknown as AuthStore;
}

describe('AppShell', () => {
  it('should render navigation and start status polling', async () => {
    const store = storeStub();
    const auth = authStub();
    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: [
        provideRouter([]),
        { provide: SystemStatusStore, useValue: store },
        { provide: AuthStore, useValue: auth },
        {
          provide: BreakpointObserver,
          useValue: {
            observe: () => of<BreakpointState>({ matches: false, breakpoints: {} }),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppShell);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('MedFlow');
    expect(text).toContain('工作台');
    expect(text).toContain('科室人员');
    expect(text).toContain('系统管理');
    expect(text).toContain('科室管理');
    expect(text).toContain('张明');
    expect(text).toContain('皮肤科');
    expect(text).toContain('API 正常');
    expect(store.startAutoRefresh).toHaveBeenCalledOnce();
  });
});
