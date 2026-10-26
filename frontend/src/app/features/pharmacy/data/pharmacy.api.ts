import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { type Observable } from 'rxjs';

import type {
  BatchView,
  DrugAdmin,
  DrugPayload,
  MovementView,
  StockSummary,
} from './pharmacy.models';

@Injectable({ providedIn: 'root' })
export class PharmacyApi {
  private readonly http = inject(HttpClient);

  drugs(): Observable<readonly DrugAdmin[]> {
    return this.http.get<readonly DrugAdmin[]>('/api/v1/drugs/admin');
  }

  createDrug(payload: DrugPayload): Observable<DrugAdmin> {
    return this.http.post<DrugAdmin>('/api/v1/drugs', payload);
  }

  updateDrug(id: number, payload: DrugPayload): Observable<DrugAdmin> {
    return this.http.put<DrugAdmin>(`/api/v1/drugs/${id}`, payload);
  }

  setDrugStatus(id: number, status: 'ENABLED' | 'DISABLED'): Observable<DrugAdmin> {
    return this.http.post<DrugAdmin>(`/api/v1/drugs/${id}/status`, { status });
  }

  batches(drugId?: number): Observable<readonly BatchView[]> {
    let params = new HttpParams();
    if (drugId != null) {
      params = params.set('drugId', drugId);
    }
    return this.http.get<readonly BatchView[]>('/api/v1/stock/batches', { params });
  }

  createBatch(payload: {
    drugId: number;
    batchNo: string;
    productionDate: string | null;
    expiryDate: string;
    quantity: number;
  }): Observable<BatchView> {
    return this.http.post<BatchView>('/api/v1/stock/batches', payload);
  }

  summary(): Observable<readonly StockSummary[]> {
    return this.http.get<readonly StockSummary[]>('/api/v1/stock/summary');
  }

  movements(limit = 100): Observable<readonly MovementView[]> {
    return this.http.get<readonly MovementView[]>('/api/v1/stock/movements', {
      params: new HttpParams().set('limit', limit),
    });
  }
}
