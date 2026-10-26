import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { timeout, type Observable } from 'rxjs';

import type { HealthResponse, PingResponse } from './system.models';

/**
 * 系统状态接口。健康检查走 Actuator，不使用业务接口，避免把探针逻辑写进业务控制器。
 */
@Injectable({ providedIn: 'root' })
export class SystemApi {
  private readonly http = inject(HttpClient);

  health(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>('/actuator/health').pipe(timeout(6000));
  }

  ping(): Observable<PingResponse> {
    return this.http.get<PingResponse>('/api/v1/system/ping').pipe(timeout(6000));
  }
}
