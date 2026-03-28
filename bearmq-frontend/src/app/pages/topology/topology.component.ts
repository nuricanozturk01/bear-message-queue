import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BrokerService } from '../../core/services/broker.service';
import { ToastService } from '../../core/services/toast.service';
import type { BrokerRequest, VirtualHostInfo } from '../../core/models/api.types';

@Component({
  selector: 'app-topology',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './topology.component.html',
  styleUrl: './topology.component.scss',
})
export class TopologyComponent {
  private readonly broker = inject(BrokerService);
  private readonly toast = inject(ToastService);

  readonly vhosts = signal<VirtualHostInfo[]>([]);
  readonly selectedVhostName = signal('');
  readonly jsonPayload = signal('');
  readonly jsonError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly loadError = signal(false);

  constructor() {
    this.broker.listVhosts(0, 200).subscribe({
      next: (p) => {
        this.vhosts.set(p.content);
        if (p.content.length > 0) {
          this.selectedVhostName.set(p.content[0].name);
          this.jsonPayload.set(this.defaultJson(p.content[0].name));
        }
      },
      error: () => {
        this.loadError.set(true);
        this.toast.error('Could not load instances. Please refresh.');
      },
    });
  }

  onSelectVhost(name: string): void {
    this.selectedVhostName.set(name);
    this.jsonPayload.set(this.defaultJson(name));
    this.jsonError.set(null);
  }

  validateJson(): boolean {
    try {
      JSON.parse(this.jsonPayload());
      this.jsonError.set(null);
      return true;
    } catch (e) {
      this.jsonError.set(e instanceof Error ? e.message : 'Invalid JSON');
      return false;
    }
  }

  submit(): void {
    if (!this.validateJson()) return;

    let body: BrokerRequest;
    try {
      body = JSON.parse(this.jsonPayload()) as BrokerRequest;
    } catch {
      return;
    }

    this.submitting.set(true);
    this.broker.applyTopology(body).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Topology applied successfully.');
      },
      error: (err: { error?: { message?: string } }) => {
        this.submitting.set(false);
        const msg = err.error?.message ?? 'Request failed. Check the JSON and try again.';
        this.toast.error(msg);
      },
    });
  }

  private defaultJson(vhostName = 'YOUR_VHOST_NAME'): string {
    return JSON.stringify(
      {
        vhost: vhostName,
        schemaVersion: 1,
        exchanges: [
          {
            name: 'events',
            type: 'TOPIC',
            durable: true,
            internal: false,
            delayed: false,
            args: {},
          },
        ],
        queues: [
          {
            name: 'jobs',
            durable: true,
            exclusive: false,
            auto_delete: false,
            arguments: {},
          },
        ],
        bindings: [
          {
            source: 'events',
            destination: 'jobs',
            destination_type: 'QUEUE',
            routing_key: '#',
          },
        ],
      },
      null,
      2,
    );
  }
}
