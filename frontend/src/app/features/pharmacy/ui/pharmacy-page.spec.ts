import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PharmacyPage } from './pharmacy-page';
import { PrescriptionApi } from '../../prescriptions/data/prescription.api';

const HEADER = {
  id: 1,
  prescriptionNo: 'RX20261026000001',
  encounterId: 1,
  patientId: 1,
  patientName: '李雷',
  deptId: 2,
  deptName: '全科医学科',
  doctorId: 3,
  doctorName: '李静',
  type: 'WESTERN',
  status: 'SUBMITTED' as const,
  revisionNo: 1,
  diagnosisSummary: '上感',
  medicalAdvice: null,
  submittedAt: '2026-10-26T06:00:00Z',
  reviewedBy: null,
  reviewerName: null,
  reviewedAt: null,
  rejectReason: null,
  expiresAt: null,
  createdAt: '2026-10-26T05:00:00Z',
  version: 1,
};

describe('PharmacyPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PharmacyPage],
      providers: [
        {
          provide: PrescriptionApi,
          useValue: {
            list: (_encounter?: number, status?: string) =>
              of(status === 'SUBMITTED' ? [HEADER] : []),
            startReview: () => of({ header: HEADER, items: [] }),
            approve: () => of({ header: HEADER, items: [] }),
            reject: () => of({ header: HEADER, items: [] }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should render pending prescriptions', async () => {
    const fixture = TestBed.createComponent(PharmacyPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('处方审核');
    expect(text).toContain('RX20261026000001');
    expect(text).toContain('李雷');
    expect(text).toContain('待审方');
    expect(text).toContain('开始审方');
  });
});
