/** The outcome of one deployment, as the server reports it. */
export type Outcome = 'deployed' | 'up_to_date' | 'refused' | 'failed';

/** What the deploy worker does now. */
export type ServiceState = 'idle' | 'running' | 'waiting';

/** The answer of GET /api/status. */
export interface ServiceStatus {
  state: ServiceState;
  startedAt: string;
  uptimeSeconds: number;
  sourceRepo: string;
  targetRepo: string;
  siteUrl: string | null;
  maxAttempts: number;
}

/** One entry of GET /api/deployments; times are ISO-8601 instants in UTC. */
export interface Deployment {
  finishedAt: string;
  outcome: Outcome;
  trigger: string;
  attempts: number;
  sourceCommit: string | null;
  targetCommit: string | null;
  cause: string | null;
}

/** The answer of GET /api/me. */
export interface Me {
  login: string;
}
