export interface DrugAdmin {
  readonly id: number;
  readonly code: string;
  readonly name: string;
  readonly genericName: string | null;
  readonly spec: string;
  readonly unit: string;
  readonly dosageForm: string | null;
  readonly manufacturer: string | null;
  readonly category: string;
  readonly unitPrice: number;
  readonly prescriptionType: string;
  readonly status: 'ENABLED' | 'DISABLED';
}

export interface DrugPayload {
  readonly code: string;
  readonly name: string;
  readonly genericName: string | null;
  readonly spec: string;
  readonly unit: string;
  readonly dosageForm: string | null;
  readonly manufacturer: string | null;
  readonly category: string;
  readonly unitPrice: number;
  readonly prescriptionType: string;
}

export interface BatchView {
  readonly id: number;
  readonly drugId: number;
  readonly drugName: string;
  readonly batchNo: string;
  readonly productionDate: string | null;
  readonly expiryDate: string;
  readonly stockQty: number;
  readonly reservedQty: number;
  readonly available: number;
  readonly status: string;
}

export interface StockSummary {
  readonly drugId: number;
  readonly drugName: string;
  readonly stockQty: number;
  readonly reservedQty: number;
  readonly available: number;
  readonly batches: number;
}

export interface MovementView {
  readonly id: number;
  readonly movementNo: string;
  readonly drugName: string;
  readonly batchNo: string;
  readonly type: string;
  readonly quantity: number;
  readonly beforeQty: number;
  readonly afterQty: number;
  readonly bizType: string;
  readonly bizId: number;
  readonly remark: string | null;
  readonly createdAt: string;
}
