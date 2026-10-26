export type AppointmentStatus =
  'BOOKED' | 'CHECKED_IN' | 'IN_CONSULTATION' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Patient {
  readonly id: number;
  readonly patientNo: string;
  readonly name: string;
  readonly gender: string;
  readonly birthDate: string;
  readonly idCardMasked: string | null;
  readonly phone: string | null;
  readonly address: string | null;
  readonly allergyHistory: string | null;
  readonly status: string;
}

export interface PatientPage {
  readonly items: readonly Patient[];
  readonly page: number;
  readonly size: number;
  readonly total: number;
}

export interface DoctorSchedule {
  readonly id: number;
  readonly deptId: number;
  readonly deptName: string;
  readonly doctorId: number;
  readonly doctorName: string;
  readonly scheduleDate: string;
  readonly period: 'AM' | 'PM';
  readonly startTime: string;
  readonly endTime: string;
  readonly room: string | null;
  readonly registrationFee: number;
  readonly status: string;
  readonly availableCount: number;
}

export interface ScheduleSlot {
  readonly id: number;
  readonly scheduleId: number;
  readonly seqNo: number;
  readonly startTime: string;
  readonly endTime: string;
  readonly capacity: number;
  readonly bookedCount: number;
  readonly status: string;
  readonly available: number;
}

export interface AppointmentView {
  readonly id: number;
  readonly appointmentNo: string;
  readonly status: AppointmentStatus;
  readonly patientId: number;
  readonly patientName: string;
  readonly deptId: number;
  readonly deptName: string;
  readonly doctorId: number;
  readonly doctorName: string;
  readonly scheduleId: number;
  readonly slotId: number;
  readonly appointmentDate: string;
  readonly period: 'AM' | 'PM';
  readonly seqNo: number;
  readonly encounterId: number | null;
  readonly chargeId: number | null;
  readonly chargeStatus: 'UNPAID' | 'PAID' | 'REFUNDED' | 'CANCELLED' | null;
  readonly chargeAmount: number | null;
  readonly createdAt: string;
}

export interface ChargeView {
  readonly id: number;
  readonly orderNo: string;
  readonly bizType: string;
  readonly bizId: number;
  readonly amount: number;
  readonly status: string;
  readonly paymentMethod: string | null;
}

export interface CreatePatientPayload {
  readonly name: string;
  readonly gender: string;
  readonly birthDate: string;
  readonly idCardNo: string | null;
  readonly phone: string | null;
  readonly address: string | null;
  readonly allergyHistory: string | null;
}
