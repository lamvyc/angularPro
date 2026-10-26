import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { EmrTemplatesPage } from './emr-templates-page';
import { EmrTemplateApi } from './emr-template.api';

describe('EmrTemplatesPage', () => {
  const template = {
    id: 1,
    code: 'GENERAL_FIRST',
    name: '全科门诊病历',
    scopeType: 'HOSPITAL' as const,
    deptId: null,
    currentVersionId: 1,
    versionNo: 1,
    schemaJson: '{"sections":[]}',
    changelog: '内置模板',
    status: 'PUBLISHED' as const,
    publishedAt: '2026-10-26T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmrTemplatesPage],
      providers: [
        {
          provide: EmrTemplateApi,
          useValue: {
            list: () => of([template]),
            create: () => of(template),
            publish: () => of(template),
            archive: () => of({ ...template, status: 'ARCHIVED' }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should render templates', async () => {
    const fixture = TestBed.createComponent(EmrTemplatesPage);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('病历模板');
    expect(text).toContain('全科门诊病历');
    expect(text).toContain('GENERAL_FIRST');
    expect(text).toContain('v1');
    expect(text).toContain('已发布');
  });
});
