# mb2e dashboard UI

The read-only deployment dashboard of the mb2e server mode. It is an Angular single-page app
served under `/dashboard/`; it reads `/api/me`, `/api/status` and `/api/deployments`.

| Task | Command |
|---|---|
| Install | `npm ci` |
| Unit tests | `npx ng test --watch=false` |
| Build | `npx ng build` (output: `dist/mb2e-dashboard/browser/`) |
| Local run | `npx ng serve`, with `java -jar target/mb2e.jar serve` on port 8080 |

The local run proxies the API and the GitHub login to port 8080 (`proxy.conf.json`). The design
is in `docs/design/dashboard/`.
