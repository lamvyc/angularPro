import { catchError, throwError } from 'rxjs';
import type { HttpInterceptorFn } from '@angular/common/http';

import { toMedflowError } from './problem-detail';

/**
 * 把所有 HTTP 失败归一为 MedflowError，保留后端 Problem Details 的 code/requestId/fieldErrors。
 * 不在此处弹提示或跳转：由页面决定展示方式，避免重复提示。
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(catchError((error: unknown) => throwError(() => toMedflowError(error))));
