import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuditPage } from './audit-page';
import { IamApi } from '../data/iam.api';

describe('AuditPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditPage],
      providers: [
        {
          provide: IamApi,
          useValue: {
            listAudit: () =>
              of({
                items: [
                  {
                    id: 1,
                    createdAt: '2026-10-26T05:00:00Z',
                    actorId: 1,
                    actorName: '系统管理员',
                    action: 'USER_CREATED',
                    resourceType: 'USER',
                    resourceId: '8',
                    result: 'SUCCESS',
                    errorCode: null,
                    ip: '127.0.0.1',
                    traceId: 'trace-1',
                  },
                ],
                page: 1,
                size: 20,
                total: 1,
              }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should render audit rows with readable action labels', async () => {
    const fixture = TestBed.createComponent(AuditPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('新建用户');
    expect(text).toContain('系统管理员');
    expect(text).toContain('成功');
    expect(text).toContain('trace-1');
  });
});
