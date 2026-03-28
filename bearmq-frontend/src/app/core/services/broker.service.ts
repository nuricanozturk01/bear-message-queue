import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type {
  BindingSummaryDto,
  BrokerRequest,
  CreateUserRequest,
  ExchangeSummaryDto,
  MetricsSummaryDto,
  Page,
  QueueSummaryDto,
  ResourceMetricsDto,
  UserDto,
  VhostMetricsDto,
  VirtualHostInfo,
} from '../models/api.types';

@Injectable({ providedIn: 'root' })
export class BrokerService {
  private readonly base = `${environment.apiUrl}/api/broker`;
  private readonly metricsBase = `${environment.apiUrl}/api/metrics`;
  private readonly adminBase = `${environment.apiUrl}/api/admin`;
  private readonly usersBase = `${environment.apiUrl}/api/users`;
  private readonly settingsBase = `${environment.apiUrl}/api/settings`;

  constructor(private readonly http: HttpClient) {}

  listVhosts(page = 0, size = 20): Observable<Page<VirtualHostInfo>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<Page<VirtualHostInfo>>(`${this.base}/vhost`, { params });
  }

  createVhost(): Observable<VirtualHostInfo> {
    return this.http.post<VirtualHostInfo>(`${this.base}/vhost`, {});
  }

  deleteVhost(vhostId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/vhost/${vhostId}`);
  }

  applyTopology(body: BrokerRequest): Observable<boolean> {
    return this.http.post<boolean>(this.base, body);
  }

  listQueues(vhostId: string): Observable<QueueSummaryDto[]> {
    return this.http.get<QueueSummaryDto[]>(`${this.base}/vhost/${vhostId}/queues`);
  }

  listExchanges(vhostId: string): Observable<ExchangeSummaryDto[]> {
    return this.http.get<ExchangeSummaryDto[]>(`${this.base}/vhost/${vhostId}/exchanges`);
  }

  listBindings(vhostId: string): Observable<BindingSummaryDto[]> {
    return this.http.get<BindingSummaryDto[]>(`${this.base}/vhost/${vhostId}/bindings`);
  }

  patchVhostStatus(vhostId: string, status: string): Observable<VirtualHostInfo> {
    return this.http.patch<VirtualHostInfo>(`${this.base}/vhost/${vhostId}/status`, { status });
  }

  deleteQueue(vhostId: string, queueId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/vhost/${vhostId}/queues/${queueId}`);
  }

  deleteExchange(vhostId: string, exchangeId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/vhost/${vhostId}/exchanges/${exchangeId}`);
  }

  deleteBinding(vhostId: string, bindingId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/vhost/${vhostId}/bindings/${bindingId}`);
  }

  getMessagingApiKey(): Observable<{ api_key: string }> {
    return this.http.get<{ api_key: string }>(`${this.settingsBase}/messaging-api-key`);
  }

  rotateMessagingApiKey(): Observable<{ api_key: string }> {
    return this.http.post<{ api_key: string }>(`${this.adminBase}/messaging-api-key/rotate`, {});
  }

  getMetricsSummary(): Observable<MetricsSummaryDto> {
    return this.http.get<MetricsSummaryDto>(`${this.metricsBase}/summary`);
  }

  getVhostMetrics(vhostId: string): Observable<VhostMetricsDto> {
    return this.http.get<VhostMetricsDto>(`${this.metricsBase}/vhost/${vhostId}`);
  }

  listUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${this.adminBase}/users`);
  }

  createUser(req: CreateUserRequest): Observable<UserDto> {
    return this.http.post<UserDto>(`${this.adminBase}/users`, req);
  }

  changeUserPassword(userId: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.adminBase}/users/${userId}/password`, { newPassword });
  }

  updateUserRole(userId: string, role: 'ADMIN' | 'USER'): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.adminBase}/users/${userId}/role`, { role });
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.adminBase}/users/${userId}`);
  }

  changeOwnPassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.usersBase}/me/password`, { currentPassword, newPassword });
  }

  getResourceMetrics(): Observable<ResourceMetricsDto> {
    return this.http.get<ResourceMetricsDto>(`${this.metricsBase}/resources`);
  }

  streamResourceMetrics(token: string): Observable<ResourceMetricsDto> {
    return new Observable<ResourceMetricsDto>((observer) => {
      const controller = new AbortController();

      (async () => {
        try {
          const response = await fetch(`${this.metricsBase}/resources/stream`, {
            headers: {
              Authorization: `Bearer ${token}`,
              Accept: 'text/event-stream',
            },
            signal: controller.signal,
          });

          if (!response.ok || !response.body) {
            observer.error(new Error(`SSE connect failed: ${response.status}`));
            return;
          }

          const reader  = response.body.getReader();
          const decoder = new TextDecoder();
          let   buffer  = '';

          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() ?? '';

            for (const line of lines) {
              if (line.startsWith('data:')) {
                try {
                  observer.next(JSON.parse(line.slice(5).trim()) as ResourceMetricsDto);
                } catch {
                  void 0;
                }
              }
            }
          }
          observer.complete();
        } catch (err) {
          if (!(err instanceof DOMException && err.name === 'AbortError')) {
            observer.error(err);
          }
        }
      })();

      return () => controller.abort();
    });
  }
}
