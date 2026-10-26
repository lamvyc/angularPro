import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { UsersPage } from './users-page';
import { AuthStore } from '../../../core/auth/auth.store';
import { IamApi } from '../data/iam.api';
import type { UserSummary } from '../data/iam.models';

const USER: UserSummary = {
  id: 2,
  username: 'doctor1',
  realName: '张明',
  employeeNo: 'D0001',
  deptId: 1,
  deptName: '皮肤科',
  title: '主任医师',
  phone: '13800000002',
  status: 'ENABLED',
  lastLoginAt: '2026-10-26T01:00:00Z',
  roles: [{ id: 2, code: 'DOCTOR', name: '医生' }],
};

describe('UsersPage', () => {
  const api = {
    listUsers: () => of({ items: [USER], page: 1, size: 20, total: 1 }),
    listDepartments: () => of([{ id: 1, code: 'DERM', name: '皮肤科', type: 'CLINICAL' }]),
    listRoles: () => of([]),
    createUser: () => of(USER),
    updateUser: () => of(USER),
    changeUserStatus: () => of(undefined),
    resetPassword: () => of(undefined),
  } as unknown as IamApi;
  const auth = { hasPermission: () => true } as unknown as AuthStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersPage],
      providers: [
        { provide: IamApi, useValue: api },
        { provide: AuthStore, useValue: auth },
      ],
    }).compileComponents();
  });

  it('should render users with roles and status', async () => {
    const fixture = TestBed.createComponent(UsersPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('张明');
    expect(text).toContain('doctor1');
    expect(text).toContain('皮肤科');
    expect(text).toContain('医生');
    expect(text).toContain('启用');
    expect(text).toContain('共 1 条');
  });
});
