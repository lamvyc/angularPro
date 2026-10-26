import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { SchedulesPage } from './schedules-page';
import { AppointmentApi } from '../../appointments/data/appointment.api';
import { AuthStore } from '../../../core/auth/auth.store';

describe('SchedulesPage', () => {
  const schedule = {
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SchedulesPage],
      providers: [
        {
          provide: AppointmentApi,
          useValue: {
            listSchedules: () => of([schedule]),
            createSchedule: () => of(schedule),
            closeSchedule: () => of(schedule),
          },
        },
        {
          provide: AuthStore,
          useValue: {
            hasPermission: () => true,
            user: signal({ id: 3 }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should render schedule form and existing schedules', async () => {
    const fixture = TestBed.createComponent(SchedulesPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('发布新排班');
    expect(text).toContain('李静');
    expect(text).toContain('开放');
    expect(text).toContain('余 4');
  });
});
