import { BreakpointObserver, type BreakpointState } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AppShell } from './app-shell';
import { SystemStatusStore } from '../../features/dashboard/state/system-status.store';
import type { MedflowError } from '../http/problem-detail';
import type { ServiceState } from '../../features/dashboard/data/system.models';

function storeStub() {
  return {
    apiState: signal<ServiceState>('up'),
    dbState: signal<ServiceState>('up'),
    redisState: signal<ServiceState>('unknown'),
    loading: signal(false),
    error: signal<MedflowError | null>(null),
    lastCheckedAt: signal<Date | null>(null),
    serverTime: signal<string | null>(null),
    refresh: vi.fn(),
    startAutoRefresh: vi.fn(),
  } as unknown as SystemStatusStore;
}

describe('AppShell', () => {
  it('should render navigation and start status polling', async () => {
    const store = storeStub();
    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: [
        provideRouter([]),
        { provide: SystemStatusStore, useValue: store },
        {
          provide: BreakpointObserver,
          useValue: {
            observe: () => of<BreakpointState>({ matches: false, breakpoints: {} }),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppShell);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('MedFlow');
    expect(text).toContain('工作台');
    expect(text).toContain('预约挂号');
    expect(text).toContain('M6');
    expect(text).toContain('API 正常');
    expect(store.startAutoRefresh).toHaveBeenCalledOnce();
  });
});
