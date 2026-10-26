import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RolesPage } from './roles-page';
import { AuthStore } from '../../../core/auth/auth.store';
import { IamApi } from '../data/iam.api';

describe('RolesPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RolesPage],
      providers: [
        {
          provide: IamApi,
          useValue: {
            listRoles: () =>
              of([
                {
                  id: 1,
                  code: 'ADMIN',
                  name: '系统管理员',
                  dataScope: 'ALL',
                  builtIn: true,
                  status: 'ENABLED',
                  remark: null,
                  permissionIds: [1, 2],
                },
              ]),
            listPermissions: () => of([]),
          },
        },
        { provide: AuthStore, useValue: { hasPermission: () => true } },
      ],
    }).compileComponents();
  });

  it('should render roles and protected admin assignment', async () => {
    const fixture = TestBed.createComponent(RolesPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('系统管理员');
    expect(text).toContain('全院');
    expect(text).toContain('管理员权限受保护');
  });
});
