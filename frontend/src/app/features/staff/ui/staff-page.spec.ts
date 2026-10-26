import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { StaffPage } from './staff-page';
import { AuthStore } from '../../../core/auth/auth.store';
import type { UserProfile } from '../../../core/auth/auth.models';
import { StaffStore } from '../state/staff.store';

const PROFILE: UserProfile = {
  id: 2,
  username: 'doctor1',
  realName: '张明',
  deptId: 1,
  deptName: '皮肤科',
  title: '主任医师',
  roles: [{ code: 'DOCTOR', name: '医生', dataScope: 'DEPT' }],
  permissions: ['schedule:read'],
  dataScope: 'DEPT',
  menus: [],
};

describe('StaffPage', () => {
  const load = vi.fn();
  const store = {
    doctors: signal([
      { id: 2, realName: '张明', deptId: 1, deptName: '皮肤科', title: '主任医师' },
    ]),
    loading: signal(false),
    error: signal(null),
    total: signal(1),
    load,
  } as unknown as StaffStore;
  const auth = { user: signal(PROFILE) } as unknown as AuthStore;

  beforeEach(async () => {
    load.mockClear();
    await TestBed.configureTestingModule({
      imports: [StaffPage],
      providers: [
        { provide: StaffStore, useValue: store },
        { provide: AuthStore, useValue: auth },
      ],
    }).compileComponents();
  });

  it('should render scoped doctors and load on init', async () => {
    const fixture = TestBed.createComponent(StaffPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(load).toHaveBeenCalledOnce();
    expect(text).toContain('张明');
    expect(text).toContain('皮肤科');
    expect(text).toContain('共 1 位医生');
  });
});
