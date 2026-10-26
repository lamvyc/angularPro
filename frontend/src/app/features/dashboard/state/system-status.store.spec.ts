import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SystemStatusStore } from './system-status.store';

describe('SystemStatusStore', () => {
  let store: SystemStatusStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(SystemStatusStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load health and ping into signals', () => {
    store.refresh();

    expect(store.loading()).toBe(true);

    httpMock.expectOne('/actuator/health').flush({
      status: 'UP',
      components: { db: { status: 'UP' }, redis: { status: 'DOWN' } },
    });
    httpMock
      .expectOne('/api/v1/system/ping')
      .flush({ message: 'pong', serverTime: '2026-10-26T01:30:00Z' });

    expect(store.loading()).toBe(false);
    expect(store.apiState()).toBe('up');
    expect(store.dbState()).toBe('up');
    expect(store.redisState()).toBe('down');
    expect(store.serverTime()).toBe('2026-10-26T01:30:00Z');
    expect(store.lastCheckedAt()).toBeInstanceOf(Date);
  });

  it('should report unknown component states when details are hidden', () => {
    store.refresh();

    httpMock.expectOne('/actuator/health').flush({ status: 'UP' });
    httpMock
      .expectOne('/api/v1/system/ping')
      .flush({ message: 'pong', serverTime: '2026-10-26T01:30:00Z' });

    expect(store.apiState()).toBe('up');
    expect(store.dbState()).toBe('unknown');
    expect(store.redisState()).toBe('unknown');
  });

  it('should expose a normalized error when the request fails', () => {
    store.refresh();

    httpMock
      .expectOne('/api/v1/system/ping')
      .flush({ message: 'pong', serverTime: '2026-10-26T01:30:00Z' });
    httpMock
      .expectOne('/actuator/health')
      .flush(
        { code: 'MED-INTERNAL', detail: '系统繁忙，请稍后重试' },
        { status: 500, statusText: 'Internal Server Error' },
      );

    expect(store.loading()).toBe(false);
    expect(store.apiState()).toBe('down');
    expect(store.error()?.code).toBe('MED-INTERNAL');
    expect(store.error()?.message).toBe('系统繁忙，请稍后重试');
  });
});
