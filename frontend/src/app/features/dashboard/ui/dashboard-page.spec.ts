import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { DashboardPage } from './dashboard-page';
import { AuthStore } from '../../../core/auth/auth.store';
import type { UserProfile } from '../../../core/auth/auth.models';
import { SystemStatusStore } from '../state/system-status.store';
import type { MedflowError } from '../../../core/http/problem-detail';
import type { ServiceState } from '../data/system.models';

const PROFILE = {
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
} as UserProfile;

function authStub() {
  return {
    user: signal(PROFILE),
    roles: signal(PROFILE.roles),
    displayName: signal('张明'),
    departmentName: signal('皮肤科'),
    permissions: signal(new Set(PROFILE.permissions)),
  } as unknown as AuthStore;
}

describe('DashboardPage', () => {
  const refresh = vi.fn();
  const store = {
    apiState: signal<ServiceState>('up'),
    dbState: signal<ServiceState>('up'),
    redisState: signal<ServiceState>('down'),
    loading: signal(false),
    error: signal<MedflowError | null>(null),
    lastCheckedAt: signal<Date | null>(new Date('2026-10-26T09:30:00+08:00')),
    serverTime: signal<string | null>('2026-10-26T01:30:00Z'),
    refresh,
    startAutoRefresh: vi.fn(),
  } as unknown as SystemStatusStore;

  beforeEach(async () => {
    refresh.mockClear();
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        { provide: SystemStatusStore, useValue: store },
        { provide: AuthStore, useValue: authStub() },
      ],
    }).compileComponents();
  });

  it('should render service states and roadmap', async () => {
    const fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('系统状态');
    expect(text).toContain('MySQL 数据库');
    expect(text).toContain('异常');
    expect(text).toContain('M1');
    expect(text).toContain('M6');
    expect(text).toContain('药房库存');
    expect(text).toContain('张明');
    expect(text).toContain('皮肤科');
  });

  it('should refresh when the primary action is clicked', async () => {
    const fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    );
    const target = buttons.find((button) => button.textContent?.includes('刷新系统状态'));
    target?.click();

    expect(refresh).toHaveBeenCalledOnce();
  });
});
