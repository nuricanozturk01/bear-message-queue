import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
    canActivate: [guestGuard],
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'instances',
        loadComponent: () =>
          import('./pages/instances/instances.component').then((m) => m.InstancesComponent),
      },
      {
        path: 'instances/:id',
        loadComponent: () =>
          import('./pages/instance-detail/instance-detail.component').then(
            (m) => m.InstanceDetailComponent,
          ),
      },
      {
        path: 'topology',
        loadComponent: () =>
          import('./pages/topology/topology.component').then((m) => m.TopologyComponent),
      },
      {
        path: 'teams',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/teams/teams.component').then((m) => m.TeamsComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./pages/settings/settings.component').then((m) => m.SettingsComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
