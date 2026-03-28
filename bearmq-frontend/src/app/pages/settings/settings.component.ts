import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrokerService } from '../../core/services/broker.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  private readonly broker = inject(BrokerService);
  private readonly toast = inject(ToastService);

  readonly changePwForm = signal({ current: '', next: '', next2: '' });
  readonly changePwBusy = signal(false);

  submitChangePw(): void {
    const f = this.changePwForm();
    if (f.next !== f.next2) {
      this.toast.error('New passwords do not match.');
      return;
    }
    if (f.next.length < 6) {
      this.toast.error('Password must be at least 6 characters.');
      return;
    }
    this.changePwBusy.set(true);
    this.broker.changeOwnPassword(f.current, f.next).subscribe({
      next: () => {
        this.changePwBusy.set(false);
        this.changePwForm.set({ current: '', next: '', next2: '' });
        this.toast.success('Password changed successfully.');
      },
      error: (e: { error?: { message?: string } }) => {
        this.changePwBusy.set(false);
        this.toast.error(e.error?.message ?? 'Failed to change password.');
      },
    });
  }
}
