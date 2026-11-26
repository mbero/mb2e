import { Deployment, ServiceStatus } from '../core/models';

/** The sample data of the design mockup (Main.dc.html): 12 runs with every outcome and trigger. */
export const SAMPLE_STATUS: ServiceStatus = {
  state: 'idle',
  startedAt: '2026-11-22T17:10:00Z',
  uptimeSeconds: 274320,
  sourceRepo: 'mbero/notes',
  targetRepo: 'mbero/mbero.github.io',
  siteUrl: 'https://mbero.github.io',
  maxAttempts: 3,
};

const row = (
  finishedAt: string,
  outcome: Deployment['outcome'],
  trigger: string,
  attempts: number,
  sourceCommit: string,
  targetCommit: string | null,
  cause: string | null,
): Deployment => ({ finishedAt, outcome, trigger, attempts, sourceCommit, targetCommit, cause });

export const SAMPLE_DEPLOYMENTS: Deployment[] = [
  row(
    '2026-11-25T21:14:03Z',
    'failed',
    'push',
    3,
    'a41c9e2',
    null,
    'git push rejected 3 times\nremote: Permission to mbero/mbero.github.io.git denied.\nfatal: Authentication failed (token expired)',
  ),
  row('2026-11-25T20:52:40Z', 'deployed', 'push', 1, '7d03b1f', '9f8e7d6', null),
  row('2026-11-25T20:31:12Z', 'up_to_date', 'poll', 1, '7d03b1f', null, null),
  row('2026-11-25T19:47:55Z', 'deployed', 'push', 2, 'c2e8a90', '3b61f0c', null),
  row(
    '2026-11-25T18:05:21Z',
    'refused',
    'push',
    1,
    'e19d4a7',
    null,
    'No note carries the publish tag (#publish).\nNothing to publish. The site stays as it is.',
  ),
  row('2026-11-25T17:58:02Z', 'deployed', 'push', 1, '5fa20c3', '81d7e4a', null),
  row('2026-11-25T15:30:00Z', 'up_to_date', 'poll', 1, '5fa20c3', null, null),
  row('2026-11-25T13:20:03Z', 'deployed', 'poll', 1, '0b9e6d1', 'd44c1b2', null),
  row(
    '2026-11-25T13:12:47Z',
    'failed',
    'push',
    1,
    '0b9e6d1',
    null,
    'network: could not resolve host github.com\nThe next trigger will try again.',
  ),
  row('2026-11-25T08:41:18Z', 'up_to_date', 'startup', 1, '0b9e6d1', null, null),
  row(
    '2026-11-24T22:02:09Z',
    'refused',
    'push',
    1,
    'f7713ac',
    null,
    'Front matter error in posts/angular-signals.md:\n  missing required field "title"',
  ),
  row('2026-11-24T20:40:33Z', 'deployed', 'push', 1, '8a2b5e0', 'c90f3a7', null),
];
