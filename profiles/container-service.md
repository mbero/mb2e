# Profile — container service

**Read this when you change the `Dockerfile`, `docker-compose.yml`,
`.env.example`, a `MB2E_*` setting, or when the running service misbehaves.**

## Where the knowledge goes

| You learned | Write it to |
|---|---|
| A new or changed `MB2E_*` setting | `README.md` "Server mode" settings table **and** `.env.example`, in the same change |
| How a failure shows and how to react | `docs/lessons-learned/server-deployment-playbook.md`, one failure mode per section |
| A measured runtime fact (start time, stop time, exit code, memory) | the playbook "Current state", with the date and the command that measured it |
| A choice about the image, the volume, or the replicas | a decision record in `docs/decisions/` |
| How a push becomes a deployed site | `docs/knowledge/server-mode.md` |

## Rules

- Measure before you write. A container fact needs the command and its output:
  `docker compose ps`, `docker inspect --format '{{.State.ExitCode}}'`, `curl -fsS .../healthz`.
- Never write a token, a webhook secret, a host name or a real repository URL of
  the operator into `docs/`. Use `OWNER/REPO` and `<your-host>`.
- A setting has one default. Change it in `ServerProperties` and nowhere else.
- Use dummy values for a local container check, and delete the `.env` afterwards.
- A change to the image or the compose file is verified with `docker build`,
  `docker compose config`, and a `healthy` container before it is handed over.

## Incident notes

Follow `.agents/procedures/incident-playbook.md`. Add the outcome to the
playbook as a new failure mode, or as evidence under an existing one.
