export type DataScope = 'ALL' | 'DEPT' | 'SELF';

export interface MenuNode {
  readonly id: number;
  readonly code: string;
  readonly name: string;
  readonly path: string | null;
  readonly icon: string | null;
  readonly children: readonly MenuNode[];
}

export interface RoleView {
  readonly code: string;
  readonly name: string;
  readonly dataScope: DataScope;
}

export interface UserProfile {
  readonly id: number;
  readonly username: string;
  readonly realName: string;
  readonly deptId: number | null;
  readonly deptName: string | null;
  readonly title: string | null;
  readonly roles: readonly RoleView[];
  readonly permissions: readonly string[];
  readonly dataScope: DataScope;
  readonly menus: readonly MenuNode[];
}

export interface LoginCredentials {
  readonly username: string;
  readonly password: string;
}

export interface LoginResponse {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly expiresIn: number;
  readonly tokenType: string;
  readonly user: UserProfile;
}

export interface TokenResponse {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly expiresIn: number;
  readonly tokenType: string;
}
