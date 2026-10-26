import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { WorkstationPage } from './workstation-page';
import { WorkstationStore } from '../state/workstation.store';
import { AuthStore } from '../../../core/auth/auth.store';

const ROW = {
  id: 7,
  encounterNo: 'EN20261026000001',
  status: 'IN_PROGRESS' as const,
  patientId: 1,
  patientName: '李雷',
  patientNo: 'P20261026000001',
  deptId: 2,
  deptName: '全科医学科',
  doctorId: 3,
  doctorName: '李静',
  appointmentId: 2,
  appointmentDate: '2026-10-26',
  period: 'PM',
  seqNo: 1,
  chiefComplaint: '发热',
  startedAt: '2026-10-26T06:00:00Z',
  finishedAt: null,
  createdAt: '2026-10-26T05:00:00Z',
};

const DETAIL = {
  encounter: ROW,
  emr: {
    id: 1,
    recordNo: 'EMR20261026000001',
    status: 'DRAFT',
    content: { chiefComplaint: '发热三天' },
    version: 2,
    updatedAt: '2026-10-26T06:01:00Z',
    templateId: 1,
    templateVersionId: 1,
    templateName: '全科门诊病历',
    schema: {
      sections: [
        {
          key: 'chief',
          title: '主诉与现病史',
          fields: [
            {
              key: 'chiefComplaint',
              type: 'textarea',
              label: '主诉',
              required: true,
            },
            { key: 'presentIllness', type: 'textarea', label: '现病史' },
          ],
        },
      ],
    },
    signedAt: null,
    signedBy: null,
    signatureHash: null,
    amendedAt: null,
    amendReason: null,
  },
  diagnoses: [],
  draft: null,
};

describe('WorkstationPage', () => {
  const select = vi.fn();
  const store = {
    queueDate: signal('2026-10-26'),
    queue: signal([ROW]),
    detail: signal(DETAIL),
    selectedId: signal(7),
    content: signal<Record<string, unknown>>({ chiefComplaint: '发热三天' }),
    diagnoses: signal([]),
    versions: signal([]),
    amending: signal(false),
    signed: signal(false),
    editable: signal(true),
    loadingQueue: signal(false),
    loadingDetail: signal(false),
    saving: signal(false),
    error: signal(null),
    message: signal(null),
    conflict: signal(false),
    draftAvailable: signal(false),
    lastSavedAt: signal('09:01:00'),
    canFinish: signal(false),
    loadQueue: vi.fn(),
    select,
    start: vi.fn(),
    updateField: vi.fn(),
    updateDiagnosis: vi.fn(),
    addDiagnosis: vi.fn(),
    removeDiagnosis: vi.fn(),
    saveDiagnoses: vi.fn(),
    saveNow: vi.fn(),
    sign: vi.fn(),
    beginAmend: vi.fn(),
    submitAmend: vi.fn(),
    cancelAmend: vi.fn(),
    loadVersions: vi.fn(),
    fieldError: vi.fn(() => null),
    finish: vi.fn(),
    cancel: vi.fn(),
    restoreDraft: vi.fn(),
    reloadDetail: vi.fn(),
    clearNotice: vi.fn(),
  } as unknown as WorkstationStore;

  beforeEach(async () => {
    select.mockClear();
    await TestBed.configureTestingModule({
      imports: [WorkstationPage],
      providers: [
        { provide: WorkstationStore, useValue: store },
        { provide: AuthStore, useValue: { hasPermission: () => true } },
      ],
    }).compileComponents();
  });

  it('should render queue and EMR editor', async () => {
    const fixture = TestBed.createComponent(WorkstationPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('候诊队列');
    expect(text).toContain('李雷');
    expect(text).toContain('接诊中');
    expect(text).toContain('电子病历');
    expect(text).toContain('主诉');
    expect(text).toContain('结束就诊');
  });

  it('should select an encounter from the queue', async () => {
    const fixture = TestBed.createComponent(WorkstationPage);
    await fixture.whenStable();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.queue-item')
      ?.click();
    expect(select).toHaveBeenCalledWith(7);
  });
});
