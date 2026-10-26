import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, of, switchMap, type Observable } from 'rxjs';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { WorkstationApi } from '../data/workstation.api';
import { PrescriptionApi } from '../../prescriptions/data/prescription.api';
import type { PrescriptionHeader } from '../../prescriptions/data/prescription.models';
import type {
  DiagnosisInput,
  EmrVersionView,
  EncounterDetail,
  EncounterRow,
} from '../data/workstation.models';

export interface EditableDiagnosis {
  readonly key: number;
  readonly code: string;
  readonly name: string;
  readonly type: 'PRIMARY' | 'SECONDARY';
  readonly note: string;
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

@Injectable({ providedIn: 'root' })
export class WorkstationStore {
  private readonly api = inject(WorkstationApi);
  private readonly prescriptionApi = inject(PrescriptionApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly queueDate = signal(today());
  readonly queue = signal<readonly EncounterRow[]>([]);
  readonly detail = signal<EncounterDetail | null>(null);
  readonly selectedId = signal<number | null>(null);
  readonly content = signal<Record<string, unknown>>({});
  readonly diagnoses = signal<readonly EditableDiagnosis[]>([]);
  readonly versions = signal<readonly EmrVersionView[]>([]);
  readonly prescriptions = signal<readonly PrescriptionHeader[]>([]);
  readonly loadingQueue = signal(false);
  readonly loadingDetail = signal(false);
  readonly saving = signal(false);
  readonly error = signal<MedflowError | null>(null);
  readonly message = signal<string | null>(null);
  readonly conflict = signal(false);
  readonly draftAvailable = signal(false);
  readonly lastSavedAt = signal<string | null>(null);
  readonly amending = signal(false);
  private amendReason = '';
  private diagnosesDirty = false;
  private saveTimer: ReturnType<typeof setTimeout> | null = null;
  private diagnosisKey = 0;

  readonly selectedEncounter = computed(() => this.detail()?.encounter ?? null);
  readonly signed = computed(() => {
    const status = this.detail()?.emr?.status;
    return status === 'SIGNED' || status === 'AMENDED';
  });
  readonly editable = computed(() => !this.signed() || this.amending());
  readonly canFinish = computed(() => {
    if (this.detail()?.encounter.status !== 'IN_PROGRESS' || !this.signed() || this.amending()) {
      return false;
    }
    const diagnoses = this.diagnoses();
    return diagnoses.filter((item) => item.type === 'PRIMARY').length === 1;
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.cancelTimer());
  }

  loadQueue(): void {
    this.loadingQueue.set(true);
    this.api
      .queue(this.queueDate())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingQueue.set(false)),
      )
      .subscribe({
        next: (queue) => this.queue.set(queue),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  select(encounterId: number): void {
    this.selectedId.set(encounterId);
    this.loadingDetail.set(true);
    this.error.set(null);
    this.conflict.set(false);
    this.amending.set(false);
    this.api
      .detail(encounterId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingDetail.set(false)),
      )
      .subscribe({
        next: (detail) => this.applyDetail(detail),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  start(): void {
    const id = this.selectedId();
    if (id === null) {
      return;
    }
    this.saving.set(true);
    this.api
      .start(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (detail) => {
          this.message.set('已开始接诊');
          this.applyDetail(detail);
          this.loadQueue();
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  updateField(field: string, value: unknown): void {
    if (!this.editable()) {
      return;
    }
    this.content.update((content) => ({ ...content, [field]: value }));
    if (!this.signed()) {
      this.scheduleSave();
    }
  }

  updateDiagnosis(index: number, field: keyof EditableDiagnosis, event: Event): void {
    const value = (event.target as HTMLInputElement | HTMLSelectElement).value;
    this.diagnoses.update((items) =>
      items.map((item, i) => (i === index ? { ...item, [field]: value } : item)),
    );
    this.diagnosesDirty = true;
  }

  addDiagnosis(): void {
    this.diagnoses.update((items) => [
      ...items,
      {
        key: ++this.diagnosisKey,
        code: '',
        name: '',
        type: items.length === 0 ? 'PRIMARY' : 'SECONDARY',
        note: '',
      },
    ]);
    this.diagnosesDirty = true;
  }

  removeDiagnosis(index: number): void {
    this.diagnoses.update((items) => items.filter((_, i) => i !== index));
    this.diagnosesDirty = true;
  }

  saveDiagnoses(): void {
    const id = this.selectedId();
    if (id === null) {
      return;
    }
    this.saving.set(true);
    this.api
      .saveDiagnoses(id, this.toDiagnosisInputs())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (diagnoses) => {
          this.diagnosesDirty = false;
          this.message.set('诊断已保存');
          this.diagnoses.set(
            diagnoses.map((item) => ({
              key: ++this.diagnosisKey,
              code: item.diagnosisCode,
              name: item.diagnosisName,
              type: item.type,
              note: item.note ?? '',
            })),
          );
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  saveNow(manual = true): void {
    this.cancelTimer();
    const id = this.selectedId();
    const detail = this.detail();
    if (id === null || detail?.emr == null || this.saving() || this.signed()) {
      return;
    }
    this.saving.set(true);
    this.api
      .saveEmr(id, this.content(), detail.emr.version)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (emr) => {
          this.detail.update((current) => (current ? { ...current, emr } : current));
          this.lastSavedAt.set(new Date().toLocaleTimeString('zh-CN', { hour12: false }));
          this.conflict.set(false);
          if (manual) {
            this.message.set('病历草稿已保存');
          }
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  sign(): void {
    const id = this.selectedId();
    const emr = this.detail()?.emr;
    if (id === null || emr == null || this.saving() || this.signed()) {
      return;
    }
    this.cancelTimer();
    this.saving.set(true);
    this.api
      .sign(id, this.content(), emr.version)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (signed) => {
          this.detail.update((current) => (current ? { ...current, emr: signed } : current));
          this.content.set({ ...signed.content });
          this.lastSavedAt.set(new Date().toLocaleTimeString('zh-CN', { hour12: false }));
          this.message.set('病历已签署，内容不可直接修改');
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  beginAmend(reason: string): void {
    this.amendReason = reason;
    this.amending.set(true);
    this.message.set('修订模式：修改完成后提交修订，将生成新版本');
  }

  cancelAmend(): void {
    this.amending.set(false);
    this.amendReason = '';
    const detail = this.detail();
    if (detail?.emr) {
      this.content.set({ ...detail.emr.content });
    }
  }

  submitAmend(): void {
    const id = this.selectedId();
    const emr = this.detail()?.emr;
    if (id === null || emr == null || this.saving() || !this.amending()) {
      return;
    }
    this.saving.set(true);
    this.api
      .amend(id, this.content(), emr.version, this.amendReason)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (amended) => {
          this.detail.update((current) => (current ? { ...current, emr: amended } : current));
          this.content.set({ ...amended.content });
          this.amending.set(false);
          this.message.set('修订已签署，旧版本已置为 SUPERSEDED');
          this.loadVersions();
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  loadVersions(): void {
    const id = this.selectedId();
    if (id === null) {
      return;
    }
    this.api
      .versions(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (versions) => this.versions.set(versions),
        error: (error: unknown) => this.error.set(toMedflowError(error)),
      });
  }

  finish(): void {
    const id = this.selectedId();
    if (id === null || this.saving() || !this.canFinish()) {
      return;
    }
    this.cancelTimer();
    const source$: Observable<unknown> = this.diagnosesDirty
      ? this.api.saveDiagnoses(id, this.toDiagnosisInputs())
      : of(null);
    this.saving.set(true);
    source$
      .pipe(
        switchMap(() => this.api.finish(id)),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (finished) => {
          this.message.set('就诊已结束');
          this.applyDetail(finished);
          this.loadQueue();
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  cancel(reason: string): void {
    const id = this.selectedId();
    if (id === null) {
      return;
    }
    this.api
      .cancel(id, reason)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.message.set('就诊已取消');
          this.applyDetail(detail);
          this.loadQueue();
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  restoreDraft(): void {
    const draft = this.detail()?.draft;
    if (!draft) {
      return;
    }
    this.content.set({ ...draft.content });
    this.lastSavedAt.set('来自 Redis 草稿');
    this.draftAvailable.set(false);
  }

  reloadDetail(): void {
    const id = this.selectedId();
    if (id !== null) {
      this.select(id);
    }
  }

  clearNotice(): void {
    this.error.set(null);
    this.message.set(null);
  }

  fieldError(field: string): string | null {
    return this.error()?.fieldErrors.find((item) => item.field === `content.${field}`)?.message ?? null;
  }

  private scheduleSave(): void {
    this.cancelTimer();
    this.saveTimer = setTimeout(() => this.saveNow(false), 2000);
  }

  private cancelTimer(): void {
    if (this.saveTimer !== null) {
      clearTimeout(this.saveTimer);
      this.saveTimer = null;
    }
  }

  private applyDetail(detail: EncounterDetail): void {
    this.detail.set(detail);
    this.conflict.set(false);
    this.lastSavedAt.set(null);
    this.content.set(detail.emr ? { ...detail.emr.content } : {});
    this.diagnoses.set(
      detail.diagnoses.map((item) => ({
        key: ++this.diagnosisKey,
        code: item.diagnosisCode,
        name: item.diagnosisName,
        type: item.type,
        note: item.note ?? '',
      })),
    );
    this.diagnosesDirty = false;
    this.draftAvailable.set(
      detail.draft !== null && JSON.stringify(detail.draft.content) !== JSON.stringify(detail.emr?.content),
    );
    this.loadPrescriptions(detail.encounter.id);
  }

  loadPrescriptions(encounterId?: number): void {
    const id = encounterId ?? this.selectedId();
    if (id === null) {
      return;
    }
    this.prescriptionApi.list(id).subscribe({
      next: (prescriptions) => this.prescriptions.set(prescriptions),
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  submitPrescription(prescription: PrescriptionHeader): void {
    const key = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `rx-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    this.prescriptionApi.submit(prescription.id, prescription.version, key).subscribe({
      next: () => {
        this.message.set('处方已提交，等待药师审方');
        this.loadPrescriptions();
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  cancelPrescription(prescription: PrescriptionHeader, reason: string): void {
    this.prescriptionApi.cancel(prescription.id, prescription.version, reason).subscribe({
      next: () => {
        this.message.set('处方已取消');
        this.loadPrescriptions();
      },
      error: (error: unknown) => this.error.set(toMedflowError(error)),
    });
  }

  private toDiagnosisInputs(): DiagnosisInput[] {
    return this.diagnoses().map((item) => ({
      code: item.code.trim(),
      name: item.name.trim(),
      type: item.type,
      note: item.note.trim() || null,
    }));
  }

  private handleError(error: unknown): void {
    const normalized = toMedflowError(error);
    if (normalized.code === 'MED-STATE-002') {
      this.conflict.set(true);
      this.message.set('病历已被其他窗口更新，请重新加载后再合并。');
      return;
    }
    this.error.set(normalized);
  }
}
