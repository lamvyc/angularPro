import type { DataScope } from '../../../core/auth/auth.models';

export interface PageResult<T> {
  readonly items: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly total: number;
}

export interface RoleRef {
  readonly id: number;
  readonly code: string;
  readonly name: string;
}

export interface UserSummary {
  readonly id: number;
  readonly username: string;
  readonly realName: string;
  readonly employeeNo: string | null;
  readonly deptId: number;
  readonly deptName: string;
  readonly title: string | null;
  readonly phone: string | null;
  readonly status: 'ENABLED' | 'DISABLED' | 'LOCKED';
  readonly lastLoginAt: string | null;
  readonly roles: readonly RoleRef[];
}

export interface RoleSummary {
  readonly id: number;
  readonly code: string;
  readonly name: string;
  readonly dataScope: DataScope;
  readonly builtIn: boolean;
  readonly status: string;
  readonly remark: string | null;
  readonly permissionIds: readonly number[];
}

export interface PermissionNode {
  readonly id: number;
  readonly code: string;
  readonly name: string;
  readonly type: 'MENU' | 'BUTTON' | 'API';
  readonly parentId: number | null;
  readonly children: readonly PermissionNode[];
}

export interface Department {
  readonly id: number;
  readonly code: string;
  readonly name: string;
  readonly parentId: number | null;
  readonly type: 'CLINICAL' | 'PHARMACY' | 'ADMIN';
  readonly status: string;
  readonly sortNo: number;
}

export interface AuditRow {
  readonly id: number;
  readonly createdAt: string;
  readonly actorId: number | null;
  readonly actorName: string | null;
  readonly action: string;
  readonly resourceType: string;
  readonly resourceId: string | null;
  readonly result: 'SUCCESS' | 'FAILURE';
  readonly errorCode: string | null;
  readonly ip: string | null;
  readonly traceId: string | null;
}

export interface CreateUserPayload {
  readonly username: string;
  readonly password: string;
  readonly realName: string;
  readonly employeeNo: string | null;
  readonly deptId: number;
  readonly title: string | null;
  readonly phone: string | null;
  readonly roleIds: readonly number[];
}

export interface UpdateUserPayload {
  readonly realName: string;
  readonly deptId: number;
  readonly title: string | null;
  readonly phone: string | null;
  readonly roleIds: readonly number[];
}

export interface CreateRolePayload {
  readonly code: string;
  readonly name: string;
  readonly dataScope: DataScope;
  readonly remark: string | null;
}

export interface UpdateRolePayload {
  readonly name: string;
  readonly dataScope: DataScope;
  readonly remark: string | null;
}

export interface DepartmentPayload {
  readonly code: string;
  readonly name: string;
  readonly parentId: number | null;
  readonly type: 'CLINICAL' | 'PHARMACY' | 'ADMIN';
  readonly sortNo: number;
}
