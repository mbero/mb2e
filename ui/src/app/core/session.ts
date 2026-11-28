import { Injectable, signal } from '@angular/core';

/** Who is signed in, and whether the server has ended the session. */
@Injectable({ providedIn: 'root' })
export class Session {
  readonly login = signal<string | null>(null);
  readonly expired = signal(false);

  signedIn(login: string): void {
    this.login.set(login);
    this.expired.set(false);
  }

  expire(): void {
    this.expired.set(true);
  }
}
