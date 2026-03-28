import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrokerService } from '../../core/services/broker.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import type { CreateUserRequest, UserDto } from '../../core/models/api.types';

@Component({
  selector: 'app-teams',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './teams.component.html',
  styleUrl: './teams.component.scss',
})
export class TeamsComponent {
  private readonly broker = inject(BrokerService);
  private readonly toast = inject(ToastService);
  readonly auth = inject(AuthService);

  readonly users = signal<UserDto[]>([]);
  readonly showAddUserModal = signal(false);
  readonly addUserForm = signal({
    username: '',
    password: '',
    role: 'USER' as 'ADMIN' | 'USER',
  });
  readonly addUserBusy = signal(false);
  readonly adminResetTarget = signal<UserDto | null>(null);
  readonly adminResetForm = signal({ next: '', next2: '' });
  readonly adminResetBusy = signal(false);
  readonly pendingDeleteUser = signal<UserDto | null>(null);
  readonly deleteUserBusy = signal(false);

  constructor() {
    this.loadUsers();
  }

  loadUsers(): void {
    this.broker.listUsers().subscribe({
      next: (u) => this.users.set(u),
      error: () => this.toast.error('Could not load users.'),
    });
  }

  isSelf(u: UserDto): boolean {
    return this.auth.userId() === u.id;
  }

  submitAddUser(): void {
    const f = this.addUserForm();
    if (!f.username || !f.password) return;
    this.addUserBusy.set(true);
    const req: CreateUserRequest = {
      username: f.username,
      password: f.password,
      role: f.role,
    };
    this.broker.createUser(req).subscribe({
      next: (u) => {
        this.addUserBusy.set(false);
        this.users.update((list) => [u, ...list]);
        this.showAddUserModal.set(false);
        this.addUserForm.set({ username: '', password: '', role: 'USER' });
        this.toast.success(`User "${u.username}" created.`);
      },
      error: (e: { error?: { message?: string } }) => {
        this.addUserBusy.set(false);
        this.toast.error(e.error?.message ?? 'Could not create user.');
      },
    });
  }

  openAdminResetModal(user: UserDto): void {
    this.adminResetForm.set({ next: '', next2: '' });
    this.adminResetTarget.set(user);
  }

  closeAdminResetModal(): void {
    this.adminResetTarget.set(null);
  }

  submitAdminResetPassword(): void {
    const user = this.adminResetTarget();
    if (!user) return;
    const f = this.adminResetForm();
    if (f.next !== f.next2) {
      this.toast.error('Passwords do not match.');
      return;
    }
    if (f.next.length < 6) {
      this.toast.error('Password must be at least 6 characters.');
      return;
    }
    this.adminResetBusy.set(true);
    this.broker.changeUserPassword(user.id, f.next).subscribe({
      next: () => {
        this.adminResetBusy.set(false);
        this.closeAdminResetModal();
        this.toast.success('Password updated.');
      },
      error: () => {
        this.adminResetBusy.set(false);
        this.toast.error('Failed to update password.');
      },
    });
  }

  toggleUserRole(user: UserDto): void {
    if (this.isSelf(user)) return;
    const next = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    this.broker.updateUserRole(user.id, next).subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.toast.success(`Role updated to ${next}.`);
      },
      error: () => this.toast.error('Failed to update role.'),
    });
  }

  confirmDeleteUser(user: UserDto): void {
    if (this.isSelf(user)) return;
    this.pendingDeleteUser.set(user);
  }

  cancelDeleteUser(): void {
    this.pendingDeleteUser.set(null);
  }

  executeDeleteUser(): void {
    const user = this.pendingDeleteUser();
    if (!user) return;
    this.deleteUserBusy.set(true);
    this.broker.deleteUser(user.id).subscribe({
      next: () => {
        this.deleteUserBusy.set(false);
        this.users.update((list) => list.filter((u) => u.id !== user.id));
        this.cancelDeleteUser();
        this.toast.success(`User "${user.username}" removed.`);
      },
      error: (e: { error?: { message?: string } }) => {
        this.deleteUserBusy.set(false);
        this.toast.error(e.error?.message ?? 'Could not delete user.');
      },
    });
  }
}
