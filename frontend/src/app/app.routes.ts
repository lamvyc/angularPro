import type { Routes } from '@angular/router';

import { authGuard, guestGuard, permissionGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: 'login',
    title: '登录 · MedFlow',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/ui/login-page').then((module) => module.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
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
        path: 'staff',
        title: '科室人员 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'schedule:read' },
        loadComponent: () =>
          import('./features/staff/ui/staff-page').then((module) => module.StaffPage),
      },
      {
        path: 'appointments',
        title: '预约挂号 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'appointment:read' },
        loadComponent: () =>
          import('./features/appointments/ui/appointments-page').then(
            (module) => module.AppointmentsPage,
          ),
      },
      {
        path: 'workstation',
        title: '医生工作站 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'encounter:read' },
        loadComponent: () =>
          import('./features/workstation/ui/workstation-page').then(
            (module) => module.WorkstationPage,
          ),
      },
      {
        path: 'schedules',
        title: '排班管理 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'schedule:read' },
        loadComponent: () =>
          import('./features/schedules/ui/schedules-page').then((module) => module.SchedulesPage),
      },
      {
        path: 'admin/users',
        title: '用户管理 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'system:user:read' },
        loadComponent: () =>
          import('./features/admin/users/users-page').then((module) => module.UsersPage),
      },
      {
        path: 'admin/roles',
        title: '角色权限 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'system:role:read' },
        loadComponent: () =>
          import('./features/admin/roles/roles-page').then((module) => module.RolesPage),
      },
      {
        path: 'admin/departments',
        title: '科室管理 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'system:dept:read' },
        loadComponent: () =>
          import('./features/admin/departments/departments-page').then(
            (module) => module.DepartmentsPage,
          ),
      },
      {
        path: 'admin/emr-templates',
        title: '病历模板 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'emr:template:manage' },
        loadComponent: () =>
          import('./features/admin/emr-templates/emr-templates-page').then(
            (module) => module.EmrTemplatesPage,
          ),
      },
      {
        path: 'admin/audit',
        title: '审计日志 · MedFlow',
        canActivate: [permissionGuard],
        data: { permission: 'system:audit:read' },
        loadComponent: () =>
          import('./features/admin/audit/audit-page').then((module) => module.AuditPage),
      },
      {
        path: '403',
        title: '无权限 · MedFlow',
        loadComponent: () =>
          import('./features/forbidden/forbidden-page').then((module) => module.ForbiddenPage),
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
