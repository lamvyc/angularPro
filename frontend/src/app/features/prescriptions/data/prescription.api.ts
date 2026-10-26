import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { type Observable } from 'rxjs';

import type { PrescriptionReservation } from './prescription-reservation.model';
import type {
  Drug,
  PrescriptionHeader,
  PrescriptionPayload,
  PrescriptionView,
} from './prescription.models';

@Injectable({ providedIn: 'root' })
export class PrescriptionApi {
  private readonly http = inject(HttpClient);

  drugs(): Observable<readonly Drug[]> {
    return this.http.get<readonly Drug[]>('/api/v1/drugs');
  }

  list(encounterId?: number, status?: string): Observable<readonly PrescriptionHeader[]> {
    let params = new HttpParams();
    if (encounterId != null) {
      params = params.set('encounterId', encounterId);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<readonly PrescriptionHeader[]>('/api/v1/prescriptions', { params });
  }

  reservations(id: number): Observable<readonly PrescriptionReservation[]> {
    return this.http.get<readonly PrescriptionReservation[]>(
      `/api/v1/prescriptions/${id}/reservations`,
    );
  }

  dispense(id: number): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(`/api/v1/prescriptions/${id}/dispense`, {});
  }

  returnStock(
    id: number,
    key: string,
    reason: string,
    lines: readonly { reservationId: number; quantity: number }[],
  ): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(
      `/api/v1/prescriptions/${id}/returns`,
      { reason, lines },
      { headers: new HttpHeaders({ 'Idempotency-Key': key }) },
    );
  }

  detail(id: number): Observable<PrescriptionView> {
    return this.http.get<PrescriptionView>(`/api/v1/prescriptions/${id}`);
  }

  create(encounterId: number, payload: PrescriptionPayload): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(
      `/api/v1/prescriptions/encounters/${encounterId}`,
      payload,
    );
  }

  update(id: number, payload: PrescriptionPayload): Observable<PrescriptionView> {
    return this.http.put<PrescriptionView>(`/api/v1/prescriptions/${id}`, payload);
  }

  submit(id: number, version: number, key: string): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(
      `/api/v1/prescriptions/${id}/submit`,
      { version },
      { headers: new HttpHeaders({ 'Idempotency-Key': key }) },
    );
  }

  startReview(id: number, version: number): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(`/api/v1/prescriptions/${id}/review/start`, { version });
  }

  approve(id: number, version: number): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(`/api/v1/prescriptions/${id}/review/approve`, {
      version,
    });
  }

  reject(id: number, version: number, reason: string): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(`/api/v1/prescriptions/${id}/review/reject`, {
      version,
      reason,
    });
  }

  cancel(id: number, version: number, reason: string): Observable<PrescriptionView> {
    return this.http.post<PrescriptionView>(`/api/v1/prescriptions/${id}/cancel`, {
      version,
      reason,
    });
  }
}
