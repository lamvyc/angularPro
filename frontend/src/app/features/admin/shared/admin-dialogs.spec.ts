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
