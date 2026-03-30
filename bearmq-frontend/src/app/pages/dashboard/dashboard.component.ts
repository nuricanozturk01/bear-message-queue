import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { BrokerService } from '../../core/services/broker.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import type { MetricsSummaryDto, ResourceMetricsDto } from '../../core/models/api.types';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnDestroy {
  private readonly broker = inject(BrokerService);
  private readonly toast = inject(ToastService);
  readonly auth = inject(AuthService);

  readonly metrics = signal<MetricsSummaryDto | null>(null);
  readonly resources = signal<ResourceMetricsDto | null>(null);
  readonly loading = signal(true);
  readonly apiKeyRevealed = signal(false);
  readonly copyHint = signal<string | null>(null);
  readonly rotateBusy = signal(false);

  private resourcesSub?: Subscription;
  private resourceMetricsSub?: Subscription;

  constructor() {
    this.broker.getMetricsSummary().subscribe({
      next: (m) => {
        this.metrics.set(m);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.broker.getMessagingApiKey().subscribe({
      next: (r) => this.auth.setMessagingApiKey(r.api_key),
      error: () => undefined,
    });

    this.resourceMetricsSub = this.broker.getResourceMetrics().subscribe({
      next: (r) => {
        this.resources.set(r);
        this.openResourceStream();
      },
      error: () => undefined,
    });
  }

  ngOnDestroy(): void {
    this.resourceMetricsSub?.unsubscribe();
    this.resourcesSub?.unsubscribe();
  }

  private openResourceStream(): void {
    const token = this.auth.accessToken();
    if (!token) return;
    this.resourcesSub = this.broker.streamResourceMetrics(token).subscribe({
      next: (r) => this.resources.set(r),
      error: () => undefined,
    });
  }

  toggleReveal(): void {
    this.apiKeyRevealed.update((v) => !v);
  }

  async copyApiKey(): Promise<void> {
    const key = this.auth.apiKey();
    if (!key) return;
    try {
      await navigator.clipboard.writeText(key);
      this.copyHint.set('Copied!');
      setTimeout(() => this.copyHint.set(null), 2000);
    } catch {
      this.copyHint.set('Failed');
    }
  }

  rotateApiKey(): void {
    this.rotateBusy.set(true);
    this.broker.rotateMessagingApiKey().subscribe({
      next: (r) => {
        this.rotateBusy.set(false);
        this.auth.setMessagingApiKey(r.api_key);
        this.toast.success('Messaging API key rotated.');
      },
      error: (e: { error?: { message?: string } }) => {
        this.rotateBusy.set(false);
        this.toast.error(e.error?.message ?? 'Could not rotate API key.');
      },
    });
  }

  quotaColor(pct: number): string {
    if (pct >= 90) return 'danger';
    if (pct >= 70) return 'warning';
    return 'success';
  }

  formatUptime(seconds: number): string {
    if (seconds < 60) return `${seconds}s`;
    const m = Math.floor(seconds / 60);
    if (m < 60) return `${m}m`;
    const h = Math.floor(m / 60);
    const rem = m % 60;
    if (h < 24) return rem > 0 ? `${h}h ${rem}m` : `${h}h`;
    const d = Math.floor(h / 24);
    const rh = h % 24;
    return rh > 0 ? `${d}d ${rh}h` : `${d}d`;
  }
}
