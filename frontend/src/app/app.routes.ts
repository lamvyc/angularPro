import type { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./core/layout/app-shell').then((module) => module.AppShell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        title: '工作台 · MedFlow',
        loadComponent: () =>
          import('./features/dashboard/ui/dashboard-page').then((module) => module.DashboardPage),
      },
      {
        path: '**',
        title: '页面不存在 · MedFlow',
        loadComponent: () =>
          import('./features/not-found/not-found-page').then((module) => module.NotFoundPage),
      },
    ],
  },
];
