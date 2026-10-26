import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { type Observable } from 'rxjs';

import type {
  AppointmentView,
  ChargeView,
  CreatePatientPayload,
  DoctorSchedule,
  Patient,
  PatientPage,
  ScheduleSlot,
} from './appointment.models';

@Injectable({ providedIn: 'root' })
export class AppointmentApi {
  private readonly http = inject(HttpClient);

  searchPatients(keyword: string): Observable<PatientPage> {
    let params = new HttpParams().set('page', 1).set('size', 10);
    if (keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    return this.http.get<PatientPage>('/api/v1/patients', { params });
  }

  createPatient(payload: CreatePatientPayload): Observable<Patient> {
    return this.http.post<Patient>('/api/v1/patients', payload);
  }

  listSchedules(date: string): Observable<readonly DoctorSchedule[]> {
    return this.http.get<readonly DoctorSchedule[]>('/api/v1/schedules', {
      params: new HttpParams().set('date', date),
    });
  }

  listSlots(scheduleId: number): Observable<readonly ScheduleSlot[]> {
    return this.http.get<readonly ScheduleSlot[]>(`/api/v1/schedules/${scheduleId}/slots`);
  }

  closeSchedule(scheduleId: number): Observable<DoctorSchedule> {
    return this.http.post<DoctorSchedule>(`/api/v1/schedules/${scheduleId}/close`, {});
  }

  createSchedule(payload: {
    scheduleDate: string;
    period: 'AM' | 'PM';
    startTime: string;
    endTime: string;
    room: string | null;
    slotCount: number;
    capacityPerSlot: number;
    registrationFee: number;
  }): Observable<DoctorSchedule> {
    return this.http.post<DoctorSchedule>('/api/v1/schedules', payload);
  }

  book(patientId: number, slotId: number, idempotencyKey: string): Observable<AppointmentView> {
    return this.http.post<AppointmentView>(
      '/api/v1/appointments',
      { patientId, slotId },
      { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) },
    );
  }

  listAppointments(date: string, status = ''): Observable<readonly AppointmentView[]> {
    let params = new HttpParams().set('date', date);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<readonly AppointmentView[]>('/api/v1/appointments', { params });
  }

  cancel(appointmentId: number, reason: string): Observable<AppointmentView> {
    return this.http.post<AppointmentView>(`/api/v1/appointments/${appointmentId}/cancel`, {
      reason,
    });
  }

  checkIn(appointmentId: number): Observable<AppointmentView> {
    return this.http.post<AppointmentView>(`/api/v1/appointments/${appointmentId}/check-in`, {});
  }

  pay(chargeId: number, paymentMethod = 'CASH'): Observable<ChargeView> {
    return this.http.post<ChargeView>(`/api/v1/charges/${chargeId}/pay`, { paymentMethod });
  }
}
