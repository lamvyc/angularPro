import { Injectable, signal } from '@angular/core';

const REFRESH_KEY = 'medflow.refresh-token';

@Injectable({ providedIn: 'root' })
export class TokenStorage {
  private readonly access = signal<string | null>(null);

  readonly accessToken = this.access.asReadonly();

  refreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  }

  setTokens(accessToken: string, refreshToken: string | null): void {
    this.access.set(accessToken);
    if (refreshToken) {
      try {
        localStorage.setItem(REFRESH_KEY, refreshToken);
      } catch {
        // 隐私模式下 localStorage 不可写时仅保留内存会话
      }
    }
  }

  clear(): void {
    this.access.set(null);
    try {
      localStorage.removeItem(REFRESH_KEY);
    } catch {
      // ignore
    }
  }
}
