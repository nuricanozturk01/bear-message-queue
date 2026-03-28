import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { BrokerService } from '../../core/services/broker.service';
import { ToastService } from '../../core/services/toast.service';
import type { VirtualHostInfo } from '../../core/models/api.types';

@Component({
  selector: 'app-instances',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './instances.component.html',
  styleUrl: './instances.component.scss',
})
export class InstancesComponent {
  private readonly broker = inject(BrokerService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly items = signal<VirtualHostInfo[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly creating = signal(false);

  private readonly pageSize = 10;

  constructor() {
    this.load(0);
  }

  load(p: number): void {
    this.loading.set(true);
    this.broker.listVhosts(p, this.pageSize).subscribe({
      next: (res) => {
        this.items.set(res.content);
        this.page.set(res.number);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Could not load instances.');
        this.loading.set(false);
      },
    });
  }

  prev(): void {
    const p = this.page();
    if (p > 0) {
      this.load(p - 1);
    }
  }

  next(): void {
    const p = this.page();
    if (p < this.totalPages() - 1) {
      this.load(p + 1);
    }
  }

  create(): void {
    this.creating.set(true);
    this.broker.createVhost().subscribe({
      next: (v) => {
        this.creating.set(false);
        this.toast.success('Instance created successfully.');
        void this.router.navigate(['/instances', v.id]);
      },
      error: (err: { error?: { message?: string } }) => {
        this.creating.set(false);
        this.toast.error(err.error?.message ?? 'Could not create instance.');
      },
    });
  }

  open(v: VirtualHostInfo): void {
    void this.router.navigate(['/instances', v.id]);
  }
}
