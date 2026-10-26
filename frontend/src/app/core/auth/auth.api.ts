import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { timeout, type Observable } from 'rxjs';

import type { LoginCredentials, LoginResponse, TokenResponse, UserProfile } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly timeoutMs = 8000;

  login(credentials: LoginCredentials): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/v1/auth/login', credentials)
      .pipe(timeout(this.timeoutMs));
  }

  refresh(refreshToken: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/api/v1/auth/refresh', { refreshToken })
      .pipe(timeout(this.timeoutMs));
  }

  logout(refreshToken: string | null): Observable<void> {
    return this.http
      .post<void>('/api/v1/auth/logout', refreshToken ? { refreshToken } : {})
      .pipe(timeout(this.timeoutMs));
  }

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/v1/auth/me').pipe(timeout(this.timeoutMs));
  }
}
