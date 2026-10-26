import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { type Observable } from 'rxjs';

export interface Doctor {
  readonly id: number;
  readonly realName: string;
  readonly deptId: number;
  readonly deptName: string;
  readonly title: string | null;
}

@Injectable({ providedIn: 'root' })
export class StaffApi {
  private readonly http = inject(HttpClient);

  doctors(): Observable<readonly Doctor[]> {
    return this.http.get<readonly Doctor[]>('/api/v1/staff/doctors');
  }
}
