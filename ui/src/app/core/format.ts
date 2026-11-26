/** Formats a number of seconds as "3d 4h 12m"; under a minute it shows seconds. */
export function uptime(seconds: number): string {
  if (seconds < 60) {
    return `${Math.max(0, Math.floor(seconds))}s`;
  }
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return [d ? `${d}d` : '', d || h ? `${h}h` : '', `${m}m`].filter(Boolean).join(' ');
}

/** Formats the time from an instant to now as "8 min ago", "3 h ago", "yesterday" or "4 days ago". */
export function relative(iso: string, now: number): string {
  const seconds = Math.max(0, Math.round((now - Date.parse(iso)) / 1000));
  if (seconds < 60) {
    return `${seconds} s ago`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes} min ago`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours} h ago`;
  }
  const days = Math.floor(hours / 24);
  return days === 1 ? 'yesterday' : `${days} days ago`;
}

/** Formats an instant in the browser time zone as "25 Nov, 22:14:03". */
export function absolute(iso: string): string {
  const date = new Date(iso);
  const day = date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
  const time = date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  return `${day}, ${time}`;
}

/** Formats an instant in the browser time zone as "21:52". */
export function clock(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', hour12: false });
}

/** Shows "1", or "2 of 3" when the push needed more than one attempt. */
export function attempts(count: number, max: number): string {
  return count > 1 ? `${count} of ${max}` : `${count}`;
}

/** Turns the repository "owner/name" into the address of one commit on GitHub. */
export function commitUrl(repo: string, sha: string): string {
  return `https://github.com/${repo}/commit/${sha}`;
}

/** The host part of an address: "https://me.github.io/blog" gives "me.github.io/blog". */
export function hostOf(url: string): string {
  return url.replace(/^https?:\/\//, '');
}
