import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { AuthResponse } from '../models/api.types';

const SK_ACCESS = 'bearmq_access_token';
const SK_REFRESH = 'bearmq_refresh_token';
const SK_API_KEY = 'bearmq_api_key';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly accessTokenSig = signal<string | null>(sessionStorage.getItem(SK_ACCESS));
  private readonly apiKeySig = signal<string | null>(sessionStorage.getItem(SK_API_KEY));

  readonly accessToken = this.accessTokenSig.asReadonly();
  readonly apiKey = this.apiKeySig.asReadonly();
  readonly isAuthenticated = computed(() => !!this.accessTokenSig());
  readonly username = computed(() => this.parseClaim('username'));
  readonly userId = computed(() => this.parseClaim('sub'));
  readonly role = computed(() => this.parseClaim('role') ?? 'USER');
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  login(username: string, password: string): Observable<AuthResponse> {
    const url = `${environment.apiUrl}/api/auth/login`;
    return this.http.post<AuthResponse>(url, { username, password }).pipe(tap((r) => this.persist(r)));
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = sessionStorage.getItem(SK_REFRESH);
    if (!refreshToken) {
      throw new Error('No refresh token');
    }
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/api/auth/refresh`, {
        refresh_token: refreshToken,
      })
      .pipe(tap((r) => this.persist(r)));
  }

  setMessagingApiKey(key: string): void {
    sessionStorage.setItem(SK_API_KEY, key);
    this.apiKeySig.set(key);
  }

  logout(): void {
    sessionStorage.removeItem(SK_ACCESS);
    sessionStorage.removeItem(SK_REFRESH);
    sessionStorage.removeItem(SK_API_KEY);
    this.accessTokenSig.set(null);
    this.apiKeySig.set(null);
    void this.router.navigateByUrl('/login');
  }

  private parseClaim(key: string): string | null {
    const token = this.accessTokenSig();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1])) as Record<string, unknown>;
      return typeof payload[key] === 'string' ? (payload[key] as string) : null;
    } catch {
      return null;
    }
  }

  private persist(res: AuthResponse): void {
    sessionStorage.setItem(SK_ACCESS, res.access_token);
    sessionStorage.setItem(SK_REFRESH, res.refresh_token);
    sessionStorage.setItem(SK_API_KEY, res.api_key);
    this.accessTokenSig.set(res.access_token);
    this.apiKeySig.set(res.api_key);
  }
}
