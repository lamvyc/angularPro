export interface PrescriptionReservation {
  readonly id: number;
  readonly prescriptionId: number;
  readonly itemId: number;
  readonly drugId: number;
  readonly drugName: string;
  readonly batchId: number;
  readonly batchNo: string;
  readonly expiryDate: string;
  readonly quantity: number;
  readonly returnedQty: number;
  readonly status: 'HELD' | 'CONSUMED' | 'RELEASED';
}
