import { HttpErrorResponse } from '@angular/common/http';

export interface ProblemFieldError {
  readonly field: string;
  readonly message: string;
}

export interface ProblemDetail {
  readonly type?: string;
  readonly title?: string;
  readonly status?: number;
  readonly detail?: string;
  readonly instance?: string;
  readonly code?: string;
  readonly requestId?: string;
  readonly fieldErrors?: readonly ProblemFieldError[];
}

/**
 * 前端统一的业务错误。组件与 Store 只依赖该类型，不直接处理 HttpErrorResponse。
 */
export class MedflowError extends Error {
  constructor(
    message: string,
    readonly code: string,
    readonly status: number,
    readonly requestId?: string,
    readonly fieldErrors: readonly ProblemFieldError[] = [],
  ) {
    super(message);
    this.name = 'MedflowError';
  }
}

export function toMedflowError(error: unknown): MedflowError {
  if (error instanceof MedflowError) {
    return error;
  }
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return new MedflowError('无法连接服务器，请确认后端服务已启动', 'NETWORK_ERROR', 0);
    }
    const body = isProblemDetail(error.error) ? error.error : null;
    return new MedflowError(
      body?.detail ?? body?.title ?? `请求失败（HTTP ${error.status}）`,
      body?.code ?? `HTTP_${error.status}`,
      error.status,
      body?.requestId,
      body?.fieldErrors ?? [],
    );
  }
  if (error instanceof Error) {
    return new MedflowError(error.message, 'UNKNOWN_ERROR', 0);
  }
  return new MedflowError('发生未知错误，请稍后重试', 'UNKNOWN_ERROR', 0);
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  return typeof value === 'object' && value !== null;
}
