import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Deployment, Me, ServiceStatus } from './models';

/** The JSON API of the mb2e server; every path is absolute, outside the /dashboard/ base. */
@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  me(): Observable<Me> {
    return this.http.get<Me>('/api/me');
  }

  status(): Observable<ServiceStatus> {
    return this.http.get<ServiceStatus>('/api/status');
  }

  deployments(limit: number): Observable<Deployment[]> {
    return this.http.get<Deployment[]>('/api/deployments', { params: new HttpParams().set('limit', limit) });
  }

  /** Ends the session; Angular sends the XSRF-TOKEN cookie value as the X-XSRF-TOKEN header. */
  logout(): Observable<void> {
    return this.http.post<void>('/api/logout', null);
  }
}
