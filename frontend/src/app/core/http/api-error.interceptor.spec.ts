import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiErrorInterceptor } from './api-error.interceptor';
import { MedflowError } from './problem-detail';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should map Problem Details into MedflowError', () => {
    let caught: MedflowError | undefined;
    http.get('/api/v1/prescriptions/1').subscribe({
      error: (error: MedflowError) => {
        caught = error;
      },
    });

    httpMock.expectOne('/api/v1/prescriptions/1').flush(
      {
        type: 'https://medflow.local/problems/MED-STOCK-001',
        title: '库存不足',
        status: 409,
        detail: '阿莫西林胶囊 可用量 3，申请 10',
        code: 'MED-STOCK-001',
        requestId: 'req-12345678',
        fieldErrors: [],
      },
      { status: 409, statusText: 'Conflict' },
    );

    expect(caught).toBeInstanceOf(MedflowError);
    expect(caught?.code).toBe('MED-STOCK-001');
    expect(caught?.status).toBe(409);
    expect(caught?.message).toBe('阿莫西林胶囊 可用量 3，申请 10');
    expect(caught?.requestId).toBe('req-12345678');
  });

  it('should normalize network failures', () => {
    let caught: MedflowError | undefined;
    http.get('/actuator/health').subscribe({
      error: (error: MedflowError) => {
        caught = error;
      },
    });

    httpMock.expectOne('/actuator/health').error(new ProgressEvent('network error'));

    expect(caught?.code).toBe('NETWORK_ERROR');
    expect(caught?.status).toBe(0);
  });
});
