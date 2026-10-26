import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize } from 'rxjs/operators';

import { AuthStore } from '../../../core/auth/auth.store';
import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { Icon } from '../../../shared/ui/icon/icon';
import { AppointmentApi } from '../../appointments/data/appointment.api';
import type { DoctorSchedule } from '../../appointments/data/appointment.models';

function tomorrow(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-schedules-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    Icon,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './schedules-page.html',
  styleUrl: './schedules-page.scss',
})
export class SchedulesPage {
  private readonly api = inject(AppointmentApi);
  protected readonly auth = inject(AuthStore);

  protected readonly date = signal(tomorrow());
  protected readonly schedules = signal<readonly DoctorSchedule[]>([]);
  protected readonly loading = signal(false);
  protected readonly actionBusy = signal(false);
  protected readonly error = signal<MedflowError | null>(null);
  protected readonly message = signal<string | null>(null);

  protected readonly model = signal({
    scheduleDate: tomorrow(),
    period: 'AM',
    startTime: '08:00',
    endTime: '12:00',
    room: '诊室 1',
    slotCount: '8',
    capacityPerSlot: '1',
    registrationFee: '20',
  });

  protected readonly editor = form(this.model, (path) => {
    required(path.scheduleDate, { message: '请选择日期' });
    required(path.period, { message: '请选择时段' });
    required(path.startTime, { message: '请输入开始时间' });
    required(path.endTime, { message: '请输入结束时间' });
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .listSchedules(this.date())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (schedules) => {
          this.schedules.set(schedules);
          this.error.set(null);
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected setDate(event: Event): void {
    this.date.set((event.target as HTMLInputElement).value);
    this.load();
  }

  protected create(): void {
    this.editor().markAsTouched();
    if (!this.editor().valid() || this.actionBusy()) {
      return;
    }
    const value = this.model();
    this.actionBusy.set(true);
    this.api
      .createSchedule({
        scheduleDate: value.scheduleDate,
        period: value.period as 'AM' | 'PM',
        startTime: value.startTime,
        endTime: value.endTime,
        room: value.room.trim() || null,
        slotCount: Number(value.slotCount),
        capacityPerSlot: Number(value.capacityPerSlot),
        registrationFee: Number(value.registrationFee),
      })
      .pipe(finalize(() => this.actionBusy.set(false)))
      .subscribe({
        next: (schedule) => {
          this.message.set(
            `排班已发布：${schedule.doctorName} · ${schedule.scheduleDate} ${schedule.period}`,
          );
          this.date.set(schedule.scheduleDate);
          this.load();
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected close(schedule: DoctorSchedule): void {
    this.actionBusy.set(true);
    this.api
      .closeSchedule(schedule.id)
      .pipe(finalize(() => this.actionBusy.set(false)))
      .subscribe({
        next: () => {
          this.message.set('排班已关闭，不再接受新预约');
          this.load();
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  protected canClose(schedule: DoctorSchedule): boolean {
    return (
      this.auth.hasPermission('schedule:manage') &&
      schedule.status === 'PUBLISHED' &&
      schedule.doctorId === this.auth.user()?.id
    );
  }

  protected clearNotice(): void {
    this.error.set(null);
    this.message.set(null);
  }
}
