import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, type Observable } from 'rxjs';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { AppointmentApi } from '../data/appointment.api';
import type {
  AppointmentView,
  CreatePatientPayload,
  DoctorSchedule,
  Patient,
  ScheduleSlot,
} from '../data/appointment.models';

function tomorrow(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

@Injectable({ providedIn: 'root' })
export class AppointmentStore {
  private readonly api = inject(AppointmentApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly bookingDate = signal(tomorrow());
  readonly listDate = signal(today());
  readonly statusFilter = signal('');
  readonly schedules = signal<readonly DoctorSchedule[]>([]);
  readonly slots = signal<readonly ScheduleSlot[]>([]);
  readonly selectedScheduleId = signal<number | null>(null);
  readonly selectedSlotId = signal<number | null>(null);
  readonly patientKeyword = signal('');
  readonly patients = signal<readonly Patient[]>([]);
  readonly selectedPatientId = signal<number | null>(null);
  readonly appointments = signal<readonly AppointmentView[]>([]);
  readonly loadingSchedules = signal(false);
  readonly loadingSlots = signal(false);
  readonly loadingPatients = signal(false);
  readonly loadingAppointments = signal(false);
  readonly actionBusy = signal(false);
  readonly error = signal<MedflowError | null>(null);
  readonly message = signal<string | null>(null);

  readonly selectedSchedule = computed(
    () => this.schedules().find((schedule) => schedule.id === this.selectedScheduleId()) ?? null,
  );
  readonly selectedPatient = computed(
    () => this.patients().find((patient) => patient.id === this.selectedPatientId()) ?? null,
  );
  readonly canSubmit = computed(
    () => this.selectedPatientId() !== null && this.selectedSlotId() !== null,
  );

  constructor() {
    this.loadSchedules();
    this.loadAppointments();
  }

  setBookingDate(value: string): void {
    this.bookingDate.set(value);
    this.selectedScheduleId.set(null);
    this.selectedSlotId.set(null);
    this.slots.set([]);
    this.loadSchedules();
  }

  setListDate(value: string): void {
    this.listDate.set(value);
    this.loadAppointments();
  }

  setStatusFilter(value: string): void {
    this.statusFilter.set(value);
    this.loadAppointments();
  }

  loadSchedules(): void {
    this.loadingSchedules.set(true);
    this.error.set(null);
    this.api
      .listSchedules(this.bookingDate())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingSchedules.set(false)),
      )
      .subscribe({
        next: (schedules) => this.schedules.set(schedules),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  selectSchedule(scheduleId: number): void {
    this.selectedScheduleId.set(scheduleId);
    this.selectedSlotId.set(null);
    this.loadingSlots.set(true);
    this.api
      .listSlots(scheduleId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingSlots.set(false)),
      )
      .subscribe({
        next: (slots) => this.slots.set(slots),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  selectSlot(slotId: number): void {
    this.selectedSlotId.set(slotId);
  }

  searchPatients(): void {
    this.loadingPatients.set(true);
    this.api
      .searchPatients(this.patientKeyword())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingPatients.set(false)),
      )
      .subscribe({
        next: (page) => {
          this.patients.set(page.items);
          this.selectedPatientId.set(page.items.length === 1 ? page.items[0]!.id : null);
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  selectPatient(patientId: number): void {
    this.selectedPatientId.set(patientId);
  }

  createPatient(payload: CreatePatientPayload): void {
    this.actionBusy.set(true);
    this.api
      .createPatient(payload)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionBusy.set(false)),
      )
      .subscribe({
        next: (patient) => {
          this.patients.set([patient, ...this.patients()]);
          this.selectedPatientId.set(patient.id);
          this.patientKeyword.set(patient.patientNo);
          this.message.set(`患者 ${patient.name} 已建档（${patient.patientNo}）`);
          this.error.set(null);
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  book(): void {
    const patientId = this.selectedPatientId();
    const slotId = this.selectedSlotId();
    if (patientId === null || slotId === null || this.actionBusy()) {
      return;
    }
    const key = this.newIdempotencyKey();
    this.actionBusy.set(true);
    this.error.set(null);
    this.api
      .book(patientId, slotId, key)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionBusy.set(false)),
      )
      .subscribe({
        next: (appointment) => {
          this.message.set(
            `预约成功：${appointment.appointmentNo} · ${appointment.doctorName} · ${appointment.appointmentDate} ${appointment.period}`,
          );
          this.selectedSlotId.set(null);
          if (this.selectedScheduleId() !== null) {
            this.selectSchedule(this.selectedScheduleId()!);
          }
          this.loadSchedules();
          this.loadAppointments();
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  loadAppointments(): void {
    this.loadingAppointments.set(true);
    this.api
      .listAppointments(this.listDate(), this.statusFilter())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingAppointments.set(false)),
      )
      .subscribe({
        next: (appointments) => this.appointments.set(appointments),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  checkIn(appointment: AppointmentView): void {
    this.runAction(this.api.checkIn(appointment.id), '签到成功，已进入候诊队列');
  }

  cancel(appointment: AppointmentView, reason: string): void {
    this.runAction(this.api.cancel(appointment.id, reason), '预约已取消，号源已释放');
  }

  pay(appointment: AppointmentView): void {
    if (appointment.chargeId === null) {
      return;
    }
    this.runAction(this.api.pay(appointment.chargeId), '收费成功');
  }

  clearNotice(): void {
    this.message.set(null);
    this.error.set(null);
  }

  private runAction(stream: Observable<unknown>, message: string): void {
    this.actionBusy.set(true);
    this.error.set(null);
    stream
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionBusy.set(false)),
      )
      .subscribe({
        next: () => {
          this.message.set(message);
          this.loadAppointments();
          this.loadSchedules();
        },
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  private newIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID();
    }
    return `idem-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
