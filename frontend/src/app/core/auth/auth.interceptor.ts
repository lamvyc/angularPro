import { HttpErrorResponse, type HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { toMedflowError } from '../http/problem-detail';
import { AuthStore } from './auth.store';
import { TokenStorage } from './token-storage';

/**
 * 附加 Bearer 令牌；401 时单例刷新并重试一次。
 * 不处理登录/刷新/登出接口，避免刷新风暴。
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const tokens = inject(TokenStorage);
  const store = inject(AuthStore);
  const accessToken = tokens.accessToken();
  const isCredentialEndpoint =
    request.url.endsWith('/api/v1/auth/login') || request.url.endsWith('/api/v1/auth/refresh');
  const isLogoutEndpoint = request.url.endsWith('/api/v1/auth/logout');

  const authenticatedRequest =
    accessToken && !isCredentialEndpoint
      ? request.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
      : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      const shouldRefresh =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !isCredentialEndpoint &&
        !isLogoutEndpoint &&
        tokens.refreshToken() !== null;

      if (!shouldRefresh) {
        return throwError(() => error);
      }

      return store.refreshAccessToken().pipe(
        switchMap((tokensResponse) =>
          next(
            request.clone({
              setHeaders: { Authorization: `Bearer ${tokensResponse.accessToken}` },
            }),
          ),
        ),
        catchError((refreshError: unknown) => {
          store.handleUnauthorized();
          return throwError(() => toMedflowError(refreshError));
        }),
      );
    }),
  );
};
