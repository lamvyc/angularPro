import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { type Observable } from 'rxjs';

import type {
  AuditRow,
  CreateRolePayload,
  CreateUserPayload,
  Department,
  DepartmentPayload,
  PageResult,
  PermissionNode,
  RoleSummary,
  UpdateRolePayload,
  UpdateUserPayload,
  UserSummary,
} from './iam.models';

export interface UserQuery {
  readonly page: number;
  readonly size: number;
  readonly keyword?: string;
  readonly deptId?: number | null;
  readonly status?: string;
}

export interface AuditQuery {
  readonly page: number;
  readonly size: number;
  readonly action?: string;
  readonly actorId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class IamApi {
  private readonly http = inject(HttpClient);

  listUsers(query: UserQuery): Observable<PageResult<UserSummary>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.keyword) {
      params = params.set('keyword', query.keyword);
    }
    if (query.deptId) {
      params = params.set('deptId', query.deptId);
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    return this.http.get<PageResult<UserSummary>>('/api/v1/users', { params });
  }

  createUser(payload: CreateUserPayload): Observable<UserSummary> {
    return this.http.post<UserSummary>('/api/v1/users', payload);
  }

  updateUser(id: number, payload: UpdateUserPayload): Observable<UserSummary> {
    return this.http.put<UserSummary>(`/api/v1/users/${id}`, payload);
  }

  changeUserStatus(id: number, status: 'ENABLED' | 'DISABLED'): Observable<void> {
    return this.http.patch<void>(`/api/v1/users/${id}/status`, { status });
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.post<void>(`/api/v1/users/${id}/reset-password`, { newPassword });
  }

  listRoles(): Observable<readonly RoleSummary[]> {
    return this.http.get<readonly RoleSummary[]>('/api/v1/roles');
  }

  createRole(payload: CreateRolePayload): Observable<RoleSummary> {
    return this.http.post<RoleSummary>('/api/v1/roles', payload);
  }

  updateRole(id: number, payload: UpdateRolePayload): Observable<RoleSummary> {
    return this.http.put<RoleSummary>(`/api/v1/roles/${id}`, payload);
  }

  assignPermissions(id: number, permissionIds: readonly number[]): Observable<void> {
    return this.http.put<void>(`/api/v1/roles/${id}/permissions`, { permissionIds });
  }

  listPermissions(): Observable<readonly PermissionNode[]> {
    return this.http.get<readonly PermissionNode[]>('/api/v1/permissions/tree');
  }

  listDepartments(): Observable<readonly Department[]> {
    return this.http.get<readonly Department[]>('/api/v1/departments');
  }

  createDepartment(payload: DepartmentPayload): Observable<Department> {
    return this.http.post<Department>('/api/v1/departments', payload);
  }

  updateDepartment(id: number, payload: Omit<DepartmentPayload, 'code'>): Observable<Department> {
    return this.http.put<Department>(`/api/v1/departments/${id}`, payload);
  }

  listAudit(query: AuditQuery): Observable<PageResult<AuditRow>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.action) {
      params = params.set('action', query.action);
    }
    if (query.actorId) {
      params = params.set('actorId', query.actorId);
    }
    return this.http.get<PageResult<AuditRow>>('/api/v1/audit-logs', { params });
  }
}
