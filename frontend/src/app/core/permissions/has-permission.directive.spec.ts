import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AuthStore } from '../auth/auth.store';
import { HasPermissionDirective } from './has-permission.directive';

@Component({
  selector: 'app-directive-host',
  imports: [HasPermissionDirective],
  template: `
    <button id="create" *appHasPermission="'system:user:create'">新建</button>
    <span id="always">始终显示</span>
  `,
})
class Host {}

describe('HasPermissionDirective', () => {
  it('should remove and restore the element when permission changes', async () => {
    const permissions = signal(new Set<string>());
    const store = {
      hasPermission: (permission: string | readonly string[]) => {
        const required = typeof permission === 'string' ? [permission] : permission;
        return required.every((item) => permissions().has(item));
      },
    } as unknown as AuthStore;

    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: AuthStore, useValue: store }],
    }).compileComponents();

    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    let element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('#create')).toBeNull();
    expect(element.querySelector('#always')).not.toBeNull();

    permissions.set(new Set(['system:user:create']));
    fixture.detectChanges();
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('#create')?.textContent).toContain('新建');
  });
});
