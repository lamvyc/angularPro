import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { type Observable } from 'rxjs';

import type {
  DiagnosisInput,
  DiagnosisView,
  EmrView,
  EncounterDetail,
  EncounterRow,
} from './workstation.models';

@Injectable({ providedIn: 'root' })
export class WorkstationApi {
  private readonly http = inject(HttpClient);

  queue(date: string): Observable<readonly EncounterRow[]> {
    return this.http.get<readonly EncounterRow[]>('/api/v1/encounters/queue', {
      params: new HttpParams().set('date', date),
    });
  }

  detail(id: number): Observable<EncounterDetail> {
    return this.http.get<EncounterDetail>(`/api/v1/encounters/${id}`);
  }

  start(id: number): Observable<EncounterDetail> {
    return this.http.post<EncounterDetail>(`/api/v1/encounters/${id}/start`, {});
  }

  saveEmr(id: number, content: Record<string, unknown>, version: number): Observable<EmrView> {
    return this.http.put<EmrView>(`/api/v1/encounters/${id}/emr`, { content, version });
  }

  saveDiagnoses(id: number, diagnoses: readonly DiagnosisInput[]): Observable<readonly DiagnosisView[]> {
    return this.http.put<readonly DiagnosisView[]>(`/api/v1/encounters/${id}/diagnoses`, {
      diagnoses,
    });
  }

  finish(id: number): Observable<EncounterDetail> {
    return this.http.post<EncounterDetail>(`/api/v1/encounters/${id}/finish`, {});
  }

  cancel(id: number, reason: string): Observable<EncounterDetail> {
    return this.http.post<EncounterDetail>(`/api/v1/encounters/${id}/cancel`, { reason });
  }
}
