import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import type { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import type { AuthResponse } from '../models/api.types';

let refreshing = false;
const refreshDone$ = new BehaviorSubject<string | null>(null);

function isAuthEndpoint(url: string): boolean {
  return (
    url.includes('/api/auth/login') ||
    url.includes('/api/auth/register') ||
    url.includes('/api/auth/refresh')
  );
}

function addBearer(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.accessToken();

  if (isAuthEndpoint(req.url)) {
    return next(req);
  }

  const outgoing = token ? addBearer(req, token) : req;

  return next(outgoing).pipe(
    catchError((err: unknown): Observable<never> => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || isAuthEndpoint(req.url)) {
        return throwError(() => err);
      }

      if (refreshing) {
        return refreshDone$.pipe(
          filter((t): t is string => t !== null),
          take(1),
          switchMap((newToken) => next(addBearer(req, newToken)) as Observable<never>),
        );
      }

      refreshing = true;
      refreshDone$.next(null);

      return (auth.refresh() as Observable<AuthResponse>).pipe(
        switchMap((res: AuthResponse) => {
          refreshing = false;
          refreshDone$.next(res.access_token);
          return next(addBearer(req, res.access_token)) as Observable<never>;
        }),
        catchError((refreshErr: unknown) => {
          refreshing = false;
          auth.logout();
          return throwError(() => refreshErr);
        }),
      );
    }),
  );
};
