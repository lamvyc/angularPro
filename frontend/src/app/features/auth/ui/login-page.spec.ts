import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { LoginPage } from './login-page';
import { AuthStore } from '../../../core/auth/auth.store';
import type { UserProfile } from '../../../core/auth/auth.models';
import type { MedflowError } from '../../../core/http/problem-detail';

@Component({ template: '' })
class Blank {}

const PROFILE: UserProfile = {
  id: 2,
  username: 'doctor2',
  realName: '张明',
  deptId: 1,
  deptName: '皮肤科',
  title: '主任医师',
  roles: [{ code: 'DOCTOR', name: '医生', dataScope: 'DEPT' }],
  permissions: ['menu:dashboard'],
  dataScope: 'DEPT',
  menus: [],
};

describe('LoginPage', () => {
  const login = vi.fn(() => of(PROFILE));
  const store = {
    login,
    loading: signal(false),
    error: signal<MedflowError | null>(null),
  } as unknown as AuthStore;

  beforeEach(async () => {
    login.mockClear();
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([{ path: 'dashboard', component: Blank }]),
        { provide: AuthStore, useValue: store },
      ],
    }).compileComponents();
  });

  it('should show validation messages when submitting empty form', async () => {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const submit = Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent?.trim() === '登录',
    );
    submit?.click();
    await fixture.whenStable();

    expect(element.textContent).toContain('请输入用户名');
    expect(element.textContent).toContain('请输入密码');
    expect(login).not.toHaveBeenCalled();
  });

  it('should fill demo doctor account and submit credentials', async () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const doctorChip = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.account-chip'),
    ).find((button) => button.textContent?.trim() === '医生');
    doctorChip?.click();
    await fixture.whenStable();

    const submit = Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent?.trim() === '登录',
    );
    submit?.click();
    await fixture.whenStable();

    expect(login).toHaveBeenCalledWith({ username: 'doctor2', password: 'Medflow@2026' });
    expect(navigate).toHaveBeenCalledWith('/dashboard');
  });
});
