export type PrescriptionStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'DISPENSED'
  | 'COMPLETED';

export interface Drug {
  readonly id: number;
  readonly code: string;
  readonly name: string;
  readonly spec: string;
  readonly unit: string;
  readonly unitPrice: number;
  readonly prescriptionType: string;
}

export interface PrescriptionHeader {
  readonly id: number;
  readonly prescriptionNo: string;
  readonly encounterId: number;
  readonly patientId: number;
  readonly patientName: string;
  readonly deptId: number;
  readonly deptName: string;
  readonly doctorId: number;
  readonly doctorName: string;
  readonly type: string;
  readonly status: PrescriptionStatus;
  readonly revisionNo: number;
  readonly diagnosisSummary: string | null;
  readonly medicalAdvice: string | null;
  readonly submittedAt: string | null;
  readonly reviewedBy: number | null;
  readonly reviewerName: string | null;
  readonly reviewedAt: string | null;
  readonly rejectReason: string | null;
  readonly expiresAt: string | null;
  readonly createdAt: string;
  readonly version: number;
}

export interface PrescriptionItem {
  readonly id: number;
  readonly drugId: number;
  readonly drugName: string;
  readonly spec: string;
  readonly dosage: string;
  readonly frequency: string;
  readonly route: string;
  readonly days: number;
  readonly quantity: number;
  readonly unit: string;
  readonly unitPrice: number;
  readonly amount: number;
  readonly note: string | null;
}

export interface PrescriptionView {
  readonly header: PrescriptionHeader;
  readonly items: readonly PrescriptionItem[];
}

export interface PrescriptionItemPayload {
  readonly drugId: number;
  readonly dosage: string;
  readonly frequency: string;
  readonly route: string;
  readonly days: number;
  readonly quantity: number;
  readonly note: string | null;
}

export interface PrescriptionPayload {
  readonly type: string;
  readonly diagnosisSummary: string | null;
  readonly medicalAdvice: string | null;
  readonly items: readonly PrescriptionItemPayload[];
}
