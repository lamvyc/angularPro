import { Directive, effect, inject, input, TemplateRef, ViewContainerRef } from '@angular/core';

import { AuthStore } from '../auth/auth.store';

/**
 * 无权限时从 DOM 中移除元素（而非隐藏）。后端仍会独立校验权限。
 *
 * 用法：<button *appHasPermission="'system:user:create'">新建</button>
 */
@Directive({
  selector: '[appHasPermission]',
})
export class HasPermissionDirective {
  private readonly store = inject(AuthStore);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);

  readonly appHasPermission = input.required<string | readonly string[]>();

  constructor() {
    effect(() => {
      const allowed = this.store.hasPermission(this.appHasPermission());
      this.viewContainer.clear();
      if (allowed) {
        this.viewContainer.createEmbeddedView(this.template);
      }
    });
  }
}
