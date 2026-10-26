import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { apiErrorInterceptor } from '../http/api-error.interceptor';
import { authInterceptor } from './auth.interceptor';
import { TokenStorage } from './token-storage';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tokens: TokenStorage;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([apiErrorInterceptor, authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(TokenStorage);
    tokens.setTokens('old-access', 'refresh-token');
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should attach bearer token', () => {
    http.get('/api/v1/staff/doctors').subscribe();

    const request = httpMock.expectOne('/api/v1/staff/doctors');
    expect(request.request.headers.get('Authorization')).toBe('Bearer old-access');
    request.flush([]);
  });

  it('should attach token to logout but not to login', () => {
    http.post('/api/v1/auth/logout', {}).subscribe();
    const logout = httpMock.expectOne('/api/v1/auth/logout');
    expect(logout.request.headers.get('Authorization')).toBe('Bearer old-access');
    logout.flush(null, { status: 204, statusText: 'No Content' });

    http.post('/api/v1/auth/login', { username: 'admin', password: 'x' }).subscribe();
    const login = httpMock.expectOne('/api/v1/auth/login');
    expect(login.request.headers.get('Authorization')).toBeNull();
    login.flush({});
  });

  it('should refresh once and retry all concurrent 401 requests', () => {
    const urls = ['/api/v1/a', '/api/v1/b', '/api/v1/c'];
    urls.forEach((url) => http.get(url).subscribe());

    urls.forEach((url) =>
      httpMock
        .expectOne(url)
        .flush({ code: 'MED-AUTH-001' }, { status: 401, statusText: 'Unauthorized' }),
    );

    const refresh = httpMock.expectOne('/api/v1/auth/refresh');
    refresh.flush({
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
      expiresIn: 900,
      tokenType: 'Bearer',
    });

    urls.forEach((url) => {
      const retry = httpMock.expectOne(url);
      expect(retry.request.headers.get('Authorization')).toBe('Bearer new-access');
      retry.flush({ ok: true });
    });

    expect(tokens.accessToken()).toBe('new-access');
    expect(localStorage.getItem('medflow.refresh-token')).toBe('new-refresh');
  });
});
