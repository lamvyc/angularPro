export interface EmrCondition {
  readonly field: string;
  readonly op: 'eq' | 'ne' | 'gte' | 'lte' | 'in';
  readonly value: unknown;
}

export interface EmrFieldDef {
  readonly key: string;
  readonly type: string;
  readonly label: string;
  readonly required?: boolean;
  readonly maxLength?: number;
  readonly min?: number;
  readonly max?: number;
  readonly unit?: string;
  readonly options?: readonly (string | { readonly value: string; readonly label: string })[];
  readonly visibleWhen?: EmrCondition;
  readonly requiredWhen?: EmrCondition;
}

export interface EmrSection {
  readonly key: string;
  readonly title: string;
  readonly fields: readonly EmrFieldDef[];
}

export interface EmrSchema {
  readonly sections: readonly EmrSection[];
}

export interface EncounterRow {
  readonly id: number;
  readonly encounterNo: string;
  readonly status: 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  readonly patientId: number;
  readonly patientName: string;
  readonly patientNo: string;
  readonly deptId: number;
  readonly deptName: string;
  readonly doctorId: number;
  readonly doctorName: string;
  readonly appointmentId: number;
  readonly appointmentDate: string;
  readonly period: string;
  readonly seqNo: number;
  readonly chiefComplaint: string | null;
  readonly startedAt: string | null;
  readonly finishedAt: string | null;
  readonly createdAt: string;
}

export interface EmrView {
  readonly id: number;
  readonly recordNo: string;
  readonly status: 'DRAFT' | 'SIGNED' | 'AMENDED';
  readonly content: Readonly<Record<string, unknown>>;
  readonly version: number;
  readonly updatedAt: string;
  readonly templateId: number;
  readonly templateVersionId: number;
  readonly templateName: string;
  readonly schema: EmrSchema;
  readonly signedAt: string | null;
  readonly signedBy: number | null;
  readonly signatureHash: string | null;
  readonly amendedAt: string | null;
  readonly amendReason: string | null;
}

export interface EmrDraftView {
  readonly content: Readonly<Record<string, unknown>>;
  readonly version: number;
  readonly updatedAt: string;
}

export interface DiagnosisView {
  readonly id: number;
  readonly diagnosisCode: string;
  readonly diagnosisName: string;
  readonly type: 'PRIMARY' | 'SECONDARY';
  readonly note: string | null;
  readonly sortNo: number;
}

export interface EncounterDetail {
  readonly encounter: EncounterRow;
  readonly emr: EmrView | null;
  readonly diagnoses: readonly DiagnosisView[];
  readonly draft: EmrDraftView | null;
}

export interface EmrVersionView {
  readonly versionNo: number;
  readonly status: 'SIGNED' | 'SUPERSEDED';
  readonly content: Readonly<Record<string, unknown>>;
  readonly contentHash: string | null;
  readonly signedBy: number | null;
  readonly signedByName: string | null;
  readonly signedAt: string | null;
  readonly changeReason: string | null;
  readonly createdAt: string;
}

export interface DiagnosisInput {
  readonly code: string;
  readonly name: string;
  readonly type: 'PRIMARY' | 'SECONDARY';
  readonly note: string | null;
}

export interface EmrTemplateRow {
  readonly id: number;
  readonly code: string;
  readonly name: string;
  readonly scopeType: 'HOSPITAL' | 'DEPT';
  readonly deptId: number | null;
  readonly currentVersionId: number | null;
  readonly versionNo: number;
  readonly schemaJson: string;
  readonly changelog: string | null;
  readonly status: 'PUBLISHED' | 'ARCHIVED';
  readonly publishedAt: string | null;
}
