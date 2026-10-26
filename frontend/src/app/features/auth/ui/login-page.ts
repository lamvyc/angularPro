import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthStore } from '../../../core/auth/auth.store';
import { Icon } from '../../../shared/ui/icon/icon';

interface DemoAccount {
  readonly label: string;
  readonly username: string;
}

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    Icon,
  ],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly model = signal({ username: '', password: '' });
  protected readonly loginForm = form(this.model, (path) => {
    required(path.username, { message: '请输入用户名' });
    required(path.password, { message: '请输入密码' });
  });

  protected readonly showPassword = signal(false);
  protected readonly loading = this.store.loading;
  protected readonly error = this.store.error;
  protected readonly passwordType = computed(() => (this.showPassword() ? 'text' : 'password'));

  protected readonly demoAccounts: readonly DemoAccount[] = [
    { label: '管理员', username: 'admin' },
    { label: '医生', username: 'doctor2' },
    { label: '护士', username: 'nurse3' },
    { label: '药师', username: 'pharmacist1' },
    { label: '收费员', username: 'cashier1' },
  ];

  protected fill(account: DemoAccount): void {
    this.model.set({ username: account.username, password: 'Medflow@2026' });
  }

  protected togglePassword(): void {
    this.showPassword.update((value) => !value);
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.loginForm().markAsTouched();
    if (!this.loginForm().valid() || this.loading()) {
      return;
    }
    this.store.login(this.model()).subscribe({
      next: () => {
        void this.router.navigateByUrl(this.returnUrl());
      },
      error: () => {
        // 错误信息由 AuthStore 写入 signals，页面直接渲染
      },
    });
  }

  private returnUrl(): string {
    const value = this.route.snapshot.queryParamMap.get('returnUrl');
    return value !== null && value.startsWith('/') && !value.startsWith('//')
      ? value
      : '/dashboard';
  }
}
