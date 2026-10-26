export type HealthStatus = 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';

export interface HealthComponent {
  readonly status: HealthStatus;
}

export interface HealthResponse {
  readonly status: HealthStatus;
  readonly components?: Readonly<Record<string, HealthComponent>>;
}

export interface PingResponse {
  readonly message: string;
  readonly serverTime: string;
}

export type ServiceState = 'up' | 'down' | 'unknown';
