export interface AuthResponse {
  access_token: string;
  refresh_token: string;
  api_key: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface VirtualHostInfo {
  id: string;
  name: string;
  username: string;
  password: string;
  domain: string;
  url: string;
  createdAt: string; // ISO 8601 instant from backend
  status: string;
}

export interface QueueSummaryDto {
  id: string;
  name: string;
  actualName: string;
  durable: boolean;
  exclusive: boolean;
  autoDelete: boolean;
  status: string;
}

/** Non-destructive peek: does not consume messages. */
export interface PeekedMessageDto {
  sequence: number;
  json?: unknown;
  text?: string;
  base64?: string;
}

export interface QueuePeekResponseDto {
  runtimeLoaded: boolean;
  queueName: string;
  truncated: boolean;
  messages: PeekedMessageDto[];
}

export interface ExchangeSummaryDto {
  id: string;
  name: string;
  actualName: string;
  type: string;
  durable: boolean;
  internal: boolean;
  status: string;
}

export interface BindingSummaryDto {
  id: string;
  sourceExchangeName: string;
  destinationType: 'EXCHANGE' | 'QUEUE';
  destinationName: string;
  routingKey: string;
  status: string;
}

export interface BrokerRequest {
  vhost: string;
  schemaVersion: number;
  exchanges: ExchangeRequest[];
  queues: QueueRequest[];
  bindings: BindRequest[];
}

export interface QueueRequest {
  name: string;
  durable: boolean;
  exclusive: boolean;
  auto_delete: boolean;
  arguments: Record<string, unknown>;
}

export interface ExchangeRequest {
  name: string;
  type: string;
  durable: boolean;
  internal: boolean;
  delayed: boolean;
  args: Record<string, unknown>;
}

export interface BindRequest {
  source: string;
  destination: string;
  destination_type: string;
  routing_key: string;
}

export interface MetricsSummaryDto {
  usedVhosts: number;
  usedQueues: number;
  usedExchanges: number;
}

export interface QueueMetricDto {
  id: string;
  name: string;
  status: string;
  runtimeLoaded: boolean;
  /** -1 when the queue is not loaded in broker runtime */
  approximatePendingMessages: number;
  approximatePendingCapped: boolean;
}

export interface VhostMetricsDto {
  vhostId: string;
  vhostName: string;
  runtimeLoaded: boolean;
  queueCount: number;
  exchangeCount: number;
  bindingCount: number;
  queues: QueueMetricDto[];
}

export interface UserDto {
  id: string;
  username: string;
  role: 'ADMIN' | 'USER';
  status: string;
  createdAt: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  role: 'ADMIN' | 'USER';
}

export interface ResourceMetricsDto {
  heapUsedMb: number;
  heapCommittedMb: number;
  heapMaxMb: number;
  heapUsedPct: number;
  nonHeapUsedMb: number;
  processCpuPct: number;
  systemCpuPct: number;
  threadCount: number;
  availableProcessors: number;
  uptimeSeconds: number;
}
