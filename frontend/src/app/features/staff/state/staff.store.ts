import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { StaffApi, type Doctor } from '../data/staff.api';

@Injectable({ providedIn: 'root' })
export class StaffStore {
  private readonly api = inject(StaffApi);
  private readonly destroyRef = inject(DestroyRef);

  private readonly _doctors = signal<readonly Doctor[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<MedflowError | null>(null);

  readonly doctors = this._doctors.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly total = computed(() => this._doctors().length);

  load(): void {
    this._loading.set(true);
    this._error.set(null);
    this.api
      .doctors()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this._loading.set(false)),
      )
      .subscribe({
        next: (doctors) => this._doctors.set(doctors),
        error: (error: unknown) => this._error.set(toMedflowError(error)),
      });
  }
}
