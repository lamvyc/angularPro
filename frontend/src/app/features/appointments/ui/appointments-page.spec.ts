import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { AppointmentsPage } from './appointments-page';
import { AppointmentStore } from '../state/appointment.store';
import { AuthStore } from '../../../core/auth/auth.store';

const SCHEDULE = {
  id: 10,
  deptId: 2,
  deptName: '全科医学科',
  doctorId: 3,
  doctorName: '李静',
  scheduleDate: '2026-10-27',
  period: 'AM' as const,
  startTime: '08:00:00',
  endTime: '12:00:00',
  room: '诊室 1',
  registrationFee: 20,
  status: 'PUBLISHED',
  availableCount: 4,
};

const SLOT = {
  id: 100,
  scheduleId: 10,
  seqNo: 1,
  startTime: '08:00:00',
  endTime: '08:30:00',
  capacity: 1,
  bookedCount: 0,
  status: 'OPEN',
  available: 1,
};

const PATIENT = {
  id: 1,
  patientNo: 'P20261026000001',
  name: '李雷',
  gender: 'MALE',
  birthDate: '1990-01-01',
  idCardMasked: '310101********1234',
  phone: '13900000001',
  address: null,
  allergyHistory: null,
  status: 'ACTIVE',
};

const APPOINTMENT = {
  id: 1,
  appointmentNo: 'AP20261026000001',
  status: 'BOOKED' as const,
  patientId: 1,
  patientName: '李雷',
  deptId: 2,
  deptName: '全科医学科',
  doctorId: 3,
  doctorName: '李静',
  scheduleId: 10,
  slotId: 100,
  appointmentDate: '2026-10-27',
  period: 'AM' as const,
  seqNo: 1,
  encounterId: null,
  chargeId: 5,
  chargeStatus: 'UNPAID' as const,
  chargeAmount: 20,
  createdAt: '2026-10-26T05:00:00Z',
};

describe('AppointmentsPage', () => {
  const book = vi.fn();
  const selectedScheduleId = signal<number | null>(10);
  const selectedPatientId = signal<number | null>(1);
  const schedules = signal([SCHEDULE]);
  const patients = signal([PATIENT]);
  const store = {
    bookingDate: signal('2026-10-27'),
    listDate: signal('2026-10-26'),
    statusFilter: signal(''),
    schedules,
    slots: signal([SLOT]),
    selectedScheduleId,
    selectedSlotId: signal<number | null>(100),
    patientKeyword: signal(''),
    patients,
    selectedPatientId,
    appointments: signal([APPOINTMENT]),
    loadingSchedules: signal(false),
    loadingSlots: signal(false),
    loadingPatients: signal(false),
    loadingAppointments: signal(false),
    actionBusy: signal(false),
    error: signal(null),
    message: signal<string | null>(null),
    selectedSchedule: computed(() => schedules()[0] ?? null),
    selectedPatient: computed(() => patients()[0] ?? null),
    canSubmit: computed(() => true),
    setBookingDate: vi.fn(),
    setListDate: vi.fn(),
    setStatusFilter: vi.fn(),
    loadSchedules: vi.fn(),
    loadAppointments: vi.fn(),
    selectSchedule: vi.fn(),
    selectSlot: vi.fn(),
    searchPatients: vi.fn(),
    selectPatient: vi.fn(),
    createPatient: vi.fn(),
    book,
    checkIn: vi.fn(),
    cancel: vi.fn(),
    pay: vi.fn(),
    clearNotice: vi.fn(),
  } as unknown as AppointmentStore;

  beforeEach(async () => {
    book.mockClear();
    await TestBed.configureTestingModule({
      imports: [AppointmentsPage],
      providers: [
        { provide: AppointmentStore, useValue: store },
        { provide: AuthStore, useValue: { hasPermission: () => true } },
      ],
    }).compileComponents();
  });

  it('should render booking context and appointment list', async () => {
    const fixture = TestBed.createComponent(AppointmentsPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('预约挂号');
    expect(text).toContain('李静');
    expect(text).toContain('李雷');
    expect(text).toContain('AP20261026000001');
    expect(text).toContain('已预约');
    expect(text).toContain('待收费');
  });

  it('should call book when confirmation is clicked', async () => {
    const fixture = TestBed.createComponent(AppointmentsPage);
    await fixture.whenStable();
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('确认预约'))
      ?.click();

    expect(book).toHaveBeenCalledOnce();
  });
});
