import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { InventoryPage } from './inventory-page';
import { PharmacyApi } from '../data/pharmacy.api';

describe('InventoryPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryPage],
      providers: [
        {
          provide: PharmacyApi,
          useValue: {
            drugs: () => of([
              {
                id: 1,
                code: 'AMOX500',
                name: '阿莫西林胶囊',
                genericName: '阿莫西林',
                spec: '0.5g*24粒',
                unit: '盒',
                dosageForm: '胶囊',
                manufacturer: '合成药厂',
                category: 'WESTERN',
                unitPrice: 18.5,
                prescriptionType: 'RX',
                status: 'ENABLED',
              },
            ]),
            batches: () => of([
              {
                id: 1,
                drugId: 1,
                drugName: '阿莫西林胶囊',
                batchNo: 'AMOX-2026A',
                productionDate: '2026-01-01',
                expiryDate: '2027-06-30',
                stockQty: 100,
                reservedQty: 2,
                available: 98,
                status: 'ACTIVE',
              },
            ]),
            summary: () => of([
              { drugId: 1, drugName: '阿莫西林胶囊', stockQty: 100, reservedQty: 2, available: 98, batches: 1 },
            ]),
            movements: () => of([
              {
                id: 1,
                movementNo: 'MV001',
                drugName: '阿莫西林胶囊',
                batchNo: 'AMOX-2026A',
                type: 'IN',
                quantity: 100,
                beforeQty: 0,
                afterQty: 100,
                bizType: 'BATCH',
                bizId: 1,
                remark: '入库',
                createdAt: '2026-10-26T06:00:00Z',
              },
            ]),
          },
        },
      ],
    }).compileComponents();
  });

  it('should render inventory and movements', async () => {
    const fixture = TestBed.createComponent(InventoryPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('药品与库存');
    expect(text).toContain('阿莫西林胶囊');
    expect(text).toContain('AMOX-2026A');
    expect(text).toContain('98');
    expect(text).toContain('入库');
  });
});
