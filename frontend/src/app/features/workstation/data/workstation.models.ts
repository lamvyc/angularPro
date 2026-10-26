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
  readonly status: string;
  readonly content: Readonly<Record<string, unknown>>;
  readonly version: number;
  readonly updatedAt: string;
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

export interface DiagnosisInput {
  readonly code: string;
  readonly name: string;
  readonly type: 'PRIMARY' | 'SECONDARY';
  readonly note: string | null;
}
