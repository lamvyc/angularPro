import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, of, switchMap, type Observable } from 'rxjs';

import { toMedflowError, type MedflowError } from '../../../core/http/problem-detail';
import { WorkstationApi } from '../data/workstation.api';
import type { DiagnosisInput, EncounterDetail, EncounterRow } from '../data/workstation.models';

const EMR_FIELDS = [
  'chiefComplaint',
  'presentIllness',
  'pastHistory',
  'physicalExam',
  'auxiliaryExam',
  'treatmentPlan',
] as const;

export type EmrField = (typeof EMR_FIELDS)[number];

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
  private readonly destroyRef = inject(DestroyRef);

  readonly queueDate = signal(today());
  readonly queue = signal<readonly EncounterRow[]>([]);
  readonly detail = signal<EncounterDetail | null>(null);
  readonly selectedId = signal<number | null>(null);
  readonly content = signal<Record<string, string>>(this.emptyContent());
  readonly diagnoses = signal<readonly EditableDiagnosis[]>([]);
  readonly loadingQueue = signal(false);
  readonly loadingDetail = signal(false);
  readonly saving = signal(false);
  readonly error = signal<MedflowError | null>(null);
  readonly message = signal<string | null>(null);
  readonly conflict = signal(false);
  readonly draftAvailable = signal(false);
  readonly lastSavedAt = signal<string | null>(null);
  private diagnosesDirty = false;
  private saveTimer: ReturnType<typeof setTimeout> | null = null;
  private diagnosisKey = 0;

  readonly selectedEncounter = computed(() => this.detail()?.encounter ?? null);
  readonly canFinish = computed(() => {
    const content = this.content();
    const diagnoses = this.diagnoses();
    return (
      (content['chiefComplaint'] ?? '').trim().length > 0 &&
      diagnoses.filter((item) => item.type === 'PRIMARY').length === 1
    );
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

  updateField(field: EmrField, event: Event): void {
    const value = (event.target as HTMLTextAreaElement | HTMLInputElement).value;
    this.content.update((content) => ({ ...content, [field]: value }));
    this.scheduleSave();
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
    const inputs = this.toDiagnosisInputs();
    this.saving.set(true);
    this.api
      .saveDiagnoses(id, inputs)
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
    if (id === null || detail?.emr == null || this.saving()) {
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
            this.message.set('病历已保存');
          }
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  finish(): void {
    const id = this.selectedId();
    if (id === null || this.saving()) {
      return;
    }
    this.cancelTimer();
    const detail = this.detail();
    const emr = detail?.emr ?? null;
    if (emr === null) {
      this.error.set(null);
      this.message.set('病历尚未创建，请先开始接诊');
      return;
    }
    this.saving.set(true);
    const save$: Observable<unknown> = this.api.saveEmr(id, this.content(), emr.version);
    const finish$: Observable<EncounterDetail> = this.api.finish(id);
    const source$: Observable<unknown> = this.diagnosesDirty
      ? this.api.saveDiagnoses(id, this.toDiagnosisInputs())
      : of(null);
    source$
      .pipe(
        switchMap(() => save$),
        switchMap(() => finish$),
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
    this.content.set(this.toStringContent(draft.content));
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
    const content = detail.emr ? this.toStringContent(detail.emr.content) : this.emptyContent();
    this.content.set(content);
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
  }

  private toDiagnosisInputs(): DiagnosisInput[] {
    return this.diagnoses().map((item) => ({
      code: item.code.trim(),
      name: item.name.trim(),
      type: item.type,
      note: item.note.trim() || null,
    }));
  }

  private emptyContent(): Record<string, string> {
    return Object.fromEntries(EMR_FIELDS.map((field) => [field, '']));
  }

  private toStringContent(content: Readonly<Record<string, unknown>>): Record<string, string> {
    return Object.fromEntries(
      EMR_FIELDS.map((field) => [field, content[field] == null ? '' : String(content[field])]),
    );
  }

  private handleError(error: unknown): void {
    const normalized = toMedflowError(error);
    if (normalized.code === 'MED-STATE-002') {
      this.conflict.set(true);
      this.message.set('有病历更新冲突：服务器版本已变化，请重新加载后合并。');
      return;
    }
    this.error.set(normalized);
  }
}
