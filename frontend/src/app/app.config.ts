import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeZhHans from '@angular/common/locales/zh-Hans';
import {
  inject,
  LOCALE_ID,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import type { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';

import { AuthStore } from './core/auth/auth.store';
import { authInterceptor } from './core/auth/auth.interceptor';
import { apiErrorInterceptor } from './core/http/api-error.interceptor';
import { routes } from './app.routes';

registerLocaleData(localeZhHans);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
    ),
    provideHttpClient(withFetch(), withInterceptors([apiErrorInterceptor, authInterceptor])),
    provideAppInitializer(() => inject(AuthStore).restore()),
    { provide: LOCALE_ID, useValue: 'zh-Hans' },
  ],
};
