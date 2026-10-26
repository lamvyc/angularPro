import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { type Observable } from 'rxjs';

import type { EmrTemplateRow } from '../../workstation/data/workstation.models';

export interface EmrTemplatePayload {
  readonly code?: string;
  readonly name?: string;
  readonly scopeType: 'HOSPITAL' | 'DEPT';
  readonly deptId?: number | null;
  readonly schema: Record<string, unknown>;
  readonly changelog?: string | null;
}

@Injectable({ providedIn: 'root' })
export class EmrTemplateApi {
  private readonly http = inject(HttpClient);

  list(): Observable<readonly EmrTemplateRow[]> {
    return this.http.get<readonly EmrTemplateRow[]>('/api/v1/emr-templates');
  }

  versions(id: number): Observable<readonly EmrTemplateRow[]> {
    return this.http.get<readonly EmrTemplateRow[]>(`/api/v1/emr-templates/${id}/versions`);
  }

  create(payload: EmrTemplatePayload): Observable<EmrTemplateRow> {
    return this.http.post<EmrTemplateRow>('/api/v1/emr-templates', payload);
  }

  publish(id: number, payload: EmrTemplatePayload): Observable<EmrTemplateRow> {
    return this.http.put<EmrTemplateRow>(`/api/v1/emr-templates/${id}`, {
      schema: payload.schema,
      changelog: payload.changelog,
    });
  }

  archive(id: number): Observable<EmrTemplateRow> {
    return this.http.post<EmrTemplateRow>(`/api/v1/emr-templates/${id}/archive`, {});
  }
}
