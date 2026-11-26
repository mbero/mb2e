import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Session } from './session';
import { sessionInterceptor } from './session-interceptor';

describe('sessionInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  const visited: unknown[] = [];

  beforeEach(() => {
    visited.length = 0;
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    router.navigate = (commands: unknown[]) => {
      visited.push(commands);
      return Promise.resolve(true);
    };
  });

  it('marks the session as expired on a 401 from the API', () => {
    http.get('/api/status').subscribe({ error: () => undefined });
    backend.expectOne('/api/status').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(TestBed.inject(Session).expired()).toBe(true);
  });

  it('opens the denied page on a 403 to a read, but not on a CSRF refusal of a write', () => {
    http.get('/api/me').subscribe({ error: () => undefined });
    backend.expectOne('/api/me').flush({}, { status: 403, statusText: 'Forbidden' });
    http.post('/api/logout', null).subscribe({ error: () => undefined });
    backend.expectOne('/api/logout').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(visited).toEqual([['/denied']]);
  });
});
