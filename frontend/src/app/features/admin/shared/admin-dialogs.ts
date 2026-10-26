import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, disabled, form, minLength, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import type { DataScope } from '../../../core/auth/auth.models';
import type {
  CreateRolePayload,
  CreateUserPayload,
  Department,
  DepartmentPayload,
  PermissionNode,
  RoleSummary,
  UpdateRolePayload,
  UpdateUserPayload,
  UserSummary,
} from '../data/iam.models';

export interface UserEditorData {
  readonly user?: UserSummary;
  readonly departments: readonly Department[];
  readonly roles: readonly RoleSummary[];
}

export type UserEditorResult =
  | { readonly mode: 'create'; readonly payload: CreateUserPayload }
  | { readonly mode: 'edit'; readonly payload: UpdateUserPayload };

@Component({
  selector: 'app-user-editor-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.user ? '编辑用户' : '新建用户' }}</h2>
    <mat-dialog-content>
      <form class="dialog-form">
        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>用户名</mat-label>
            <input matInput [formField]="editor.username" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>姓名</mat-label>
            <input matInput [formField]="editor.realName" />
          </mat-form-field>
        </div>

        @if (!data.user) {
          <mat-form-field appearance="outline">
            <mat-label>初始密码</mat-label>
            <input matInput type="password" [formField]="editor.password" />
            @if (editor.password().touched() && editor.password().invalid()) {
              <mat-hint class="hint-error">密码至少 8 位</mat-hint>
            }
          </mat-form-field>
        }

        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>工号</mat-label>
            <input matInput [formField]="editor.employeeNo" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>职称</mat-label>
            <input matInput [formField]="editor.title" />
          </mat-form-field>
        </div>

        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>科室</mat-label>
            <select matNativeControl [formField]="editor.deptId">
              <option value="">请选择科室</option>
              @for (dept of data.departments; track dept.id) {
                <option [value]="dept.id">{{ dept.name }}</option>
              }
            </select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>手机号</mat-label>
            <input matInput [formField]="editor.phone" />
          </mat-form-field>
        </div>

        <div class="roles-block">
          <span class="field-label">角色</span>
          <div class="role-grid">
            @for (role of data.roles; track role.id) {
              <mat-checkbox
                [checked]="model().roleIds.includes(role.id)"
                (change)="toggleRole(role.id, $event.checked)"
              >
                {{ role.name }} <code>{{ role.code }}</code>
              </mat-checkbox>
            }
          </div>
          @if (roleError()) {
            <p class="hint-error">{{ roleError() }}</p>
          }
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()">保存</button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog-form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 520px;
      padding-top: 8px;
    }

    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }

    .roles-block {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 10px 2px 4px;
    }

    .field-label {
      color: var(--mf-muted);
      font-size: 11px;
      font-weight: 650;
      letter-spacing: 0.05em;
    }

    .role-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 4px 12px;
    }

    code {
      color: var(--mf-muted);
      font-family: var(--mf-mono);
      font-size: 10px;
    }

    .hint-error {
      color: var(--mf-danger);
      font-size: 11px;
    }

    @media (max-width: 640px) {
      .dialog-form {
        min-width: 0;
      }

      .two-col,
      .role-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class UserEditorDialog {
  protected readonly data = inject<UserEditorData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserEditorDialog, UserEditorResult>);

  protected readonly model = signal({
    username: this.data.user?.username ?? '',
    password: '',
    realName: this.data.user?.realName ?? '',
    employeeNo: this.data.user?.employeeNo ?? '',
    deptId: this.data.user?.deptId?.toString() ?? '',
    title: this.data.user?.title ?? '',
    phone: this.data.user?.phone ?? '',
    roleIds: (this.data.user?.roles.map((role) => role.id) ?? []) as number[],
  });

  protected readonly editor = form(this.model, (path) => {
    required(path.realName, { message: '请输入姓名' });
    required(path.deptId, { message: '请选择科室' });
    if (this.data.user) {
      disabled(path.username);
    } else {
      required(path.username, { message: '请输入用户名' });
      required(path.password, { message: '请输入初始密码' });
      minLength(path.password, 8, { message: '密码至少 8 位' });
    }
  });

  protected readonly roleError = signal<string | null>(null);

  protected toggleRole(roleId: number, checked: boolean): void {
    this.model.update((value) => ({
      ...value,
      roleIds: checked
        ? [...new Set([...value.roleIds, roleId])]
        : value.roleIds.filter((id) => id !== roleId),
    }));
    if (this.model().roleIds.length > 0) {
      this.roleError.set(null);
    }
  }

  protected submit(): void {
    this.editor().markAsTouched();
    if (!this.editor().valid()) {
      return;
    }
    if (this.model().roleIds.length === 0) {
      this.roleError.set('至少分配一个角色');
      return;
    }
    const value = this.model();
    const deptId = Number(value.deptId);
    if (!Number.isFinite(deptId)) {
      return;
    }
    const common = {
      realName: value.realName.trim(),
      deptId,
      title: blankToNull(value.title),
      phone: blankToNull(value.phone),
      roleIds: value.roleIds,
    };
    if (this.data.user) {
      this.dialogRef.close({ mode: 'edit', payload: common });
      return;
    }
    this.dialogRef.close({
      mode: 'create',
      payload: {
        ...common,
        username: value.username.trim(),
        password: value.password,
        employeeNo: blankToNull(value.employeeNo),
      },
    });
  }
}

@Component({
  selector: 'app-reset-password-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>重置密码</h2>
    <mat-dialog-content>
      <p class="tip">重置后该用户全部刷新令牌会立即失效，需要使用新密码重新登录。</p>
      <mat-form-field appearance="outline" class="full">
        <mat-label>新密码</mat-label>
        <input matInput type="password" [formField]="editor.newPassword" />
        @if (editor.newPassword().touched() && editor.newPassword().invalid()) {
          <mat-hint class="tip-error">密码至少 8 位</mat-hint>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()">确认重置</button>
    </mat-dialog-actions>
  `,
  styles: `
    .tip {
      margin: 0 0 14px;
      color: var(--mf-muted);
      font-size: 12px;
      line-height: 1.6;
    }

    .full {
      width: 100%;
      min-width: 340px;
    }

    .tip-error {
      color: var(--mf-danger);
      font-size: 11px;
    }
  `,
})
export class ResetPasswordDialog {
  private readonly dialogRef = inject(MatDialogRef<ResetPasswordDialog, string | undefined>);
  protected readonly model = signal({ newPassword: '' });
  protected readonly editor = form(this.model, (path) => {
    required(path.newPassword, { message: '请输入新密码' });
    minLength(path.newPassword, 8, { message: '密码至少 8 位' });
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (this.editor().valid()) {
      this.dialogRef.close(this.model().newPassword);
    }
  }
}

export interface RoleEditorData {
  readonly role?: RoleSummary;
}

export type RoleEditorResult =
  | { readonly mode: 'create'; readonly payload: CreateRolePayload }
  | { readonly mode: 'edit'; readonly payload: UpdateRolePayload };

@Component({
  selector: 'app-role-editor-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data.role ? '编辑角色' : '新建角色' }}</h2>
    <mat-dialog-content>
      <form class="form">
        <mat-form-field appearance="outline">
          <mat-label>角色编码</mat-label>
          <input matInput [formField]="editor.code" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>角色名称</mat-label>
          <input matInput [formField]="editor.name" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>数据范围</mat-label>
          <select matNativeControl [formField]="editor.dataScope">
            <option value="ALL">ALL · 全院</option>
            <option value="DEPT">DEPT · 本科室</option>
            <option value="SELF">SELF · 仅本人</option>
          </select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>备注</mat-label>
          <textarea matInput rows="2" [formField]="editor.remark"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()">保存</button>
    </mat-dialog-actions>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 420px;
      padding-top: 8px;
    }
  `,
})
export class RoleEditorDialog {
  protected readonly data = inject<RoleEditorData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RoleEditorDialog, RoleEditorResult>);

  protected readonly model = signal({
    code: this.data.role?.code ?? '',
    name: this.data.role?.name ?? '',
    dataScope: (this.data.role?.dataScope ?? 'DEPT') as DataScope,
    remark: this.data.role?.remark ?? '',
  });

  protected readonly editor = form(this.model, (path) => {
    required(path.name, { message: '请输入角色名称' });
    required(path.dataScope, { message: '请选择数据范围' });
    if (this.data.role) {
      disabled(path.code);
    } else {
      required(path.code, { message: '请输入角色编码' });
    }
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (!this.editor().valid()) {
      return;
    }
    const value = this.model();
    const remark = blankToNull(value.remark);
    if (this.data.role) {
      this.dialogRef.close({
        mode: 'edit',
        payload: { name: value.name.trim(), dataScope: value.dataScope, remark },
      });
      return;
    }
    this.dialogRef.close({
      mode: 'create',
      payload: {
        code: value.code.trim().toUpperCase(),
        name: value.name.trim(),
        dataScope: value.dataScope,
        remark,
      },
    });
  }
}

interface FlatPermission {
  readonly node: PermissionNode;
  readonly depth: number;
}

export interface PermissionDialogData {
  readonly role: RoleSummary;
  readonly tree: readonly PermissionNode[];
}

@Component({
  selector: 'app-permission-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatCheckboxModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>授权 · {{ data.role.name }}</h2>
    <mat-dialog-content>
      <p class="tip">勾选父节点会同时勾选其下所有按钮/接口权限。保存后相关用户权限缓存立即失效。</p>
      <div class="permission-list">
        @for (item of flat(); track item.node.id) {
          <div class="permission-row" [style.padding-left.px]="12 + item.depth * 20">
            <mat-checkbox
              [checked]="selected().has(item.node.id)"
              (change)="toggle(item.node, $event.checked)"
            >
              <span class="name">{{ item.node.name }}</span>
              <code class="code">{{ item.node.code }}</code>
              <span class="type" [class]="'type-' + item.node.type.toLowerCase()">
                {{ typeLabel(item.node.type) }}
              </span>
            </mat-checkbox>
          </div>
        }
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <span class="count">已选 {{ selected().size }} 项</span>
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()">保存授权</button>
    </mat-dialog-actions>
  `,
  styles: `
    .tip {
      margin: 0 0 10px;
      color: var(--mf-muted);
      font-size: 12px;
    }

    .permission-list {
      max-height: 52vh;
      overflow-y: auto;
      padding-right: 6px;
    }

    .permission-row {
      border-bottom: 1px dashed var(--mf-border);
      padding: 4px 0;
    }

    .name {
      font-size: 12.5px;
    }

    .code {
      margin-left: 6px;
      color: var(--mf-muted);
      font-family: var(--mf-mono);
      font-size: 10px;
    }

    .type {
      margin-left: 6px;
      padding: 1px 6px;
      border-radius: 999px;
      background: #eef1f6;
      color: var(--mf-muted);
      font-size: 9.5px;
    }

    .type-menu {
      background: var(--mf-primary-soft);
      color: var(--mf-primary);
    }

    .type-button {
      background: var(--mf-warning-soft);
      color: var(--mf-warning);
    }

    .count {
      margin-right: auto;
      color: var(--mf-muted);
      font-size: 11px;
    }
  `,
})
export class PermissionDialog {
  protected readonly data = inject<PermissionDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<PermissionDialog, number[]>);
  protected readonly selected = signal(new Set<number>(this.data.role.permissionIds));
  protected readonly flat = computed(() => flatten(this.data.tree));

  protected toggle(node: PermissionNode, checked: boolean): void {
    const ids = collectIds(node);
    this.selected.update((current) => {
      const next = new Set(current);
      ids.forEach((id) => (checked ? next.add(id) : next.delete(id)));
      return next;
    });
  }

  protected typeLabel(type: PermissionNode['type']): string {
    return type === 'MENU' ? '菜单' : type === 'BUTTON' ? '按钮' : '接口';
  }

  protected submit(): void {
    this.dialogRef.close([...this.selected()]);
  }
}

export interface DepartmentEditorData {
  readonly department?: Department;
}

@Component({
  selector: 'app-department-editor-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data.department ? '编辑科室' : '新建科室' }}</h2>
    <mat-dialog-content>
      <form class="form">
        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>科室编码</mat-label>
            <input matInput [formField]="editor.code" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>科室名称</mat-label>
            <input matInput [formField]="editor.name" />
          </mat-form-field>
        </div>
        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>类型</mat-label>
            <select matNativeControl [formField]="editor.type">
              <option value="CLINICAL">临床科室</option>
              <option value="PHARMACY">药房</option>
              <option value="ADMIN">行政</option>
            </select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>排序</mat-label>
            <input matInput type="number" [formField]="editor.sortNo" />
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>取消</button>
      <button mat-flat-button type="button" (click)="submit()">保存</button>
    </mat-dialog-actions>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 440px;
      padding-top: 8px;
    }

    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
  `,
})
export class DepartmentEditorDialog {
  protected readonly data = inject<DepartmentEditorData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<DepartmentEditorDialog, DepartmentPayload>);
  protected readonly model = signal({
    code: this.data.department?.code ?? '',
    name: this.data.department?.name ?? '',
    type: this.data.department?.type ?? 'CLINICAL',
    sortNo: String(this.data.department?.sortNo ?? 100),
  });

  protected readonly editor = form(this.model, (path) => {
    required(path.name, { message: '请输入名称' });
    required(path.type, { message: '请选择类型' });
    if (this.data.department) {
      disabled(path.code);
    } else {
      required(path.code, { message: '请输入编码' });
    }
  });

  protected submit(): void {
    this.editor().markAsTouched();
    if (!this.editor().valid()) {
      return;
    }
    const value = this.model();
    this.dialogRef.close({
      code: value.code.trim().toUpperCase(),
      name: value.name.trim(),
      parentId: null,
      type: value.type as DepartmentPayload['type'],
      sortNo: Number(value.sortNo) || 0,
    });
  }
}

function blankToNull(value: string): string | null {
  return value.trim().length === 0 ? null : value.trim();
}

function flatten(nodes: readonly PermissionNode[], depth = 0): FlatPermission[] {
  const result: FlatPermission[] = [];
  for (const node of nodes) {
    result.push({ node, depth });
    result.push(...flatten(node.children, depth + 1));
  }
  return result;
}

function collectIds(node: PermissionNode): number[] {
  const ids = [node.id];
  for (const child of node.children) {
    ids.push(...collectIds(child));
  }
  return ids;
}
