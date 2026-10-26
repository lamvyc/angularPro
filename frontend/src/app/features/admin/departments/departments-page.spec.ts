import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { DepartmentsPage } from './departments-page';
import { AuthStore } from '../../../core/auth/auth.store';
import { IamApi } from '../data/iam.api';

describe('DepartmentsPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DepartmentsPage],
      providers: [
        {
          provide: IamApi,
          useValue: {
            listDepartments: () =>
              of([
                {
                  id: 1,
                  code: 'DERM',
                  name: '皮肤科',
                  parentId: null,
                  type: 'CLINICAL',
                  status: 'ENABLED',
                  sortNo: 10,
                },
              ]),
          },
        },
        { provide: AuthStore, useValue: { hasPermission: () => true } },
      ],
    }).compileComponents();
  });

  it('should render department directory', async () => {
    const fixture = TestBed.createComponent(DepartmentsPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('DERM');
    expect(text).toContain('皮肤科');
    expect(text).toContain('临床科室');
  });
});
