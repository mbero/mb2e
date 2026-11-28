import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { SAMPLE_DEPLOYMENTS, SAMPLE_STATUS } from '../testing/sample-data';
import { DashboardStore, REFRESH_SECONDS } from './dashboard-store';
import { Session } from './session';
import { sessionInterceptor } from './session-interceptor';

describe('DashboardStore', () => {
  let store: DashboardStore;
  let http: HttpTestingController;

  const answer = () => {
    http.expectOne('/api/status').flush(SAMPLE_STATUS);
    http
      .expectOne(
        (request) => request.url === '/api/deployments' && request.params.get('limit') === '50',
      )
      .flush(SAMPLE_DEPLOYMENTS);
  };

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        DashboardStore,
        provideRouter([]),
        provideHttpClient(withInterceptors([sessionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    store = TestBed.inject(DashboardStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    store.pause();
    vi.useRealTimers();
  });

  it('loads status and deployments at once and then every 15 s', () => {
    store.start();
    expect(store.loading()).toBe(true);
    vi.advanceTimersByTime(0);
    answer();
    expect(store.status()).toEqual(SAMPLE_STATUS);
    expect(store.latest()?.outcome).toBe('failed');
    expect(store.loading()).toBe(false);
    vi.advanceTimersByTime(REFRESH_SECONDS * 1000);
    answer();
    http.verify();
  });

  it('keeps the last data and marks it stale when a poll fails', () => {
    store.start();
    vi.advanceTimersByTime(0);
    answer();
    vi.advanceTimersByTime(REFRESH_SECONDS * 1000);
    http.expectOne('/api/status').flush('down', { status: 503, statusText: 'Service Unavailable' });
    http.match((request) => request.url === '/api/deployments');
    expect(store.failing()).toBe(true);
    expect(store.deployments()).toHaveLength(12);
  });

  it('stops polling once the session has ended', () => {
    store.start();
    vi.advanceTimersByTime(0);
    http
      .expectOne('/api/status')
      .flush({ error: 'Sign in first.' }, { status: 401, statusText: 'Unauthorized' });
    expect(TestBed.inject(Session).expired()).toBe(true);
    vi.advanceTimersByTime(REFRESH_SECONDS * 1000 * 3);
    http.expectNone('/api/status');
  });
});
