import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { vi } from 'vitest';

import { UserEditorDialog } from './admin-dialogs';

describe('UserEditorDialog', () => {
  const close = vi.fn();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserEditorDialog],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            departments: [
              {
                id: 1,
                code: 'DERM',
                name: '皮肤科',
                parentId: null,
                type: 'CLINICAL',
                status: 'ENABLED',
                sortNo: 10,
              },
            ],
            roles: [
              {
                id: 2,
                code: 'DOCTOR',
                name: '医生',
                dataScope: 'DEPT',
                builtIn: true,
                status: 'ENABLED',
                remark: null,
                permissionIds: [],
              },
            ],
          },
        },
        { provide: MatDialogRef, useValue: { close } },
      ],
    }).compileComponents();
    close.mockClear();
  });

  it('should not submit an empty user form', async () => {
    const fixture = TestBed.createComponent(UserEditorDialog);
    await fixture.whenStable();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    );
    buttons.find((button) => button.textContent?.includes('保存'))?.click();
    await fixture.whenStable();

    expect(close).not.toHaveBeenCalled();
  });
});

describe('UserEditorDialog (edit)', () => {
  const close = vi.fn();

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [UserEditorDialog],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            user: {
              id: 8,
              username: 'm1user',
              realName: '测试用户',
              employeeNo: 'M0001',
              deptId: 1,
              deptName: '皮肤科',
              title: '主治医师',
              phone: '13900000001',
              status: 'ENABLED',
              lastLoginAt: null,
              roles: [{ id: 2, code: 'DOCTOR', name: '医生' }],
            },
            departments: [
              {
                id: 1,
                code: 'DERM',
                name: '皮肤科',
                parentId: null,
                type: 'CLINICAL',
                status: 'ENABLED',
                sortNo: 10,
              },
            ],
            roles: [
              {
                id: 2,
                code: 'DOCTOR',
                name: '医生',
                dataScope: 'DEPT',
                builtIn: true,
                status: 'ENABLED',
                remark: null,
                permissionIds: [],
              },
            ],
          },
        },
        { provide: MatDialogRef, useValue: { close } },
      ],
    }).compileComponents();
    close.mockClear();
  });

  it('should submit an edit payload with disabled username', async () => {
    const fixture = TestBed.createComponent(UserEditorDialog);
    await fixture.whenStable();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    );
    buttons.find((button) => button.textContent?.includes('保存'))?.click();
    await fixture.whenStable();

    expect(close).toHaveBeenCalledWith({
      mode: 'edit',
      payload: {
        realName: '测试用户',
        deptId: 1,
        title: '主治医师',
        phone: '13900000001',
        roleIds: [2],
      },
    });
  });
});

describe('UserEditorDialog (create payload)', () => {
  const close = vi.fn();

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [UserEditorDialog],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            departments: [
              {
                id: 1,
                code: 'DERM',
                name: '皮肤科',
                parentId: null,
                type: 'CLINICAL',
                status: 'ENABLED',
                sortNo: 10,
              },
            ],
            roles: [
              {
                id: 2,
                code: 'DOCTOR',
                name: '医生',
                dataScope: 'DEPT',
                builtIn: true,
                status: 'ENABLED',
                remark: null,
                permissionIds: [],
              },
            ],
          },
        },
        { provide: MatDialogRef, useValue: { close } },
      ],
    }).compileComponents();
    close.mockClear();
  });

  it('should bind inputs/select and submit create payload', async () => {
    const fixture = TestBed.createComponent(UserEditorDialog);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const fill = (id: string, value: string): void => {
      const input = element.querySelector<HTMLInputElement>(`#${id}`);
      if (!input) {
        throw new Error(`missing input ${id}`);
      }
      input.value = value;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    };
    fill('user-username', 'newdoctor');
    fill('user-password', 'NewPass@2026');
    fill('user-real-name', '新医生');
    fill('user-employee-no', 'D9001');
    fill('user-title', '住院医师');
    fill('user-phone', '13900000009');

    const select = element.querySelector<HTMLSelectElement>('#user-dept');
    select!.value = '1';
    select!.selectedIndex = 1;
    select!.dispatchEvent(new Event('input', { bubbles: true }));
    select!.dispatchEvent(new Event('change', { bubbles: true }));

    element.querySelector<HTMLInputElement>('mat-checkbox input')?.click();
    await fixture.whenStable();

    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('保存'))
      ?.click();
    await fixture.whenStable();

    expect(close).toHaveBeenCalledWith({
      mode: 'create',
      payload: {
        username: 'newdoctor',
        password: 'NewPass@2026',
        realName: '新医生',
        employeeNo: 'D9001',
        deptId: 1,
        title: '住院医师',
        phone: '13900000009',
        roleIds: [2],
      },
    });
  });
});
