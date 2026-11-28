import { attempts, commitUrl, hostOf, relative, uptime } from './format';

describe('format', () => {
  it('shows uptime in days, hours and minutes', () => {
    expect(uptime(274320 + 720)).toBe('3d 4h 24m');
    expect(uptime(3700)).toBe('1h 1m');
    expect(uptime(59)).toBe('59s');
    expect(uptime(86400)).toBe('1d 0h 0m');
  });

  it('shows the time since an instant', () => {
    const now = Date.parse('2026-11-25T21:22:03Z');
    expect(relative('2026-11-25T21:21:58Z', now)).toBe('5 s ago');
    expect(relative('2026-11-25T21:14:03Z', now)).toBe('8 min ago');
    expect(relative('2026-11-25T18:05:21Z', now)).toBe('3 h ago');
    expect(relative('2026-11-24T20:40:33Z', now)).toBe('yesterday');
    expect(relative('2026-11-20T20:40:33Z', now)).toBe('5 days ago');
  });

  it('shows the attempts as "n of max" only after a retry', () => {
    expect(attempts(1, 3)).toBe('1');
    expect(attempts(2, 3)).toBe('2 of 3');
  });

  it('builds commit links and site hosts', () => {
    expect(commitUrl('me/notes', 'abc')).toBe('https://github.com/me/notes/commit/abc');
    expect(hostOf('https://me.github.io/blog')).toBe('me.github.io/blog');
  });
});
