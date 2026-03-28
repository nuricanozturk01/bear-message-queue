# BearMQ

<div align="center">

<img src="media/bearmq.png" width="220" alt="BearMQ logo"/>

**A lightweight message broker and control plane: TCP queues, virtual hosts, and an Angular console.**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031?style=flat&logo=angular&logoColor=white)](https://angular.dev/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

---

## Contents

1. [What it is](#what-it-is)
2. [Quick start (Docker)](#quick-start-docker)
3. [Run options](#run-options)
4. [First login & usage](#first-login--usage)
5. [Local development](#local-development)
6. [Configuration](#configuration)
7. [Repository layout](#repository-layout)
8. [REST API cheat sheet](#rest-api-cheat-sheet)
9. [Screenshots](#screenshots)
10. [Video](#video)
11. [Troubleshooting](#troubleshooting)
12. [Roadmap](#roadmap)
13. [License & support](#license--support)

---

## What it is

BearMQ is a **Spring Boot 3.5** application: **TCP** messaging (Chronicle Queue), **virtual host / exchange / queue / binding** topology, a **JWT**-protected **REST API**, and a bundled **Angular 21** UI. Code is split with Spring Modulith (`api`, `server`, `shared`). Storage is **file H2** or **PostgreSQL** with Flyway.

---

## Quick start (Docker)

Published image (Repsy):

```bash
docker run -d \
  --name bearmq \
  -p 3333:3333 \
  -p 6667:6667 \
  repo.repsy.io/nuricanozturk/bearmq/bearmq:26.03.3
```

To build the same stack locally instead: `docker build -t bearmq .` from the repo root, then use image name `bearmq` in `docker run`.

- **Web UI & API:** http://localhost:3333  
- **TCP broker:** `localhost:6667`

**Admin user:** `admin`. If `BEARMQ_ADMIN_INITIAL_PASSWORD` is not set, a random password is logged once:

```bash
docker logs bearmq | grep "Password :"
```

For a fixed password and persistent data, use the [Docker + H2](#docker--embedded-h2) example below.

---

## Run options

### Docker + embedded H2

No external database; good for try-out and dev.

```bash
docker run -d \
  --name bearmq \
  -p 3333:3333 \
  -p 6667:6667 \
  -e BEARMQ_ADMIN_INITIAL_PASSWORD=YourSecurePassword123 \
  -v bearmq-data:/app/data \
  repo.repsy.io/nuricanozturk/bearmq/bearmq:26.03.3
```

The volume keeps H2 files and (with the default image settings) Chronicle data under `/app/data`.

Optional H2 TCP for external tools: set `H2_TCP_SERVER_ENABLED=true` and publish port `9092`; align the JDBC URL with [`application.yml`](broker/src/main/resources/application.yml).

### Docker + PostgreSQL

```bash
docker network create bearmq-network

docker run -d \
  --name bearmq-postgres \
  --network bearmq-network \
  -e POSTGRES_DB=bearmq \
  -e POSTGRES_USER=bearmq \
  -e POSTGRES_PASSWORD=bearmq123 \
  -p 5432:5432 \
  postgres:16-alpine

docker run -d \
  --name bearmq \
  --network bearmq-network \
  -p 3333:3333 \
  -p 6667:6667 \
  -e DB_URL=jdbc:postgresql://bearmq-postgres:5432/bearmq \
  -e DB_USERNAME=bearmq \
  -e DB_PASSWORD=bearmq123 \
  -e BEARMQ_ADMIN_INITIAL_PASSWORD=YourSecurePassword123 \
  -v bearmq-data:/app/data \
  repo.repsy.io/nuricanozturk/bearmq/bearmq:26.03.3
```

### Docker Compose

```bash
docker compose up -d
```

Set `BEARMQ_ADMIN_INITIAL_PASSWORD` in [`docker-compose.yml`](docker-compose.yml) before the first run. Postgres only (broker on the host):

```bash
docker compose up -d postgres
```

### Pre-built image

Default registry image: **`repo.repsy.io/nuricanozturk/bearmq/bearmq:26.03.3`** (used in the `docker run` examples above). Bump the tag when you release a new version.

---

## First login & usage

1. Open http://localhost:3333 (or `SERVER_PORT`).
2. Sign in as `admin` with the password you set or the one from the logs; change it under **Settings**.

**Example: login and create a virtual host**

```bash
curl -s -X POST 'http://localhost:3333/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<password>"}'
```

Then:

```bash
curl -s -X POST 'http://localhost:3333/api/broker/vhost' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json'
```

Use the **messaging API key** from the UI for TCP `AUTH`. Spring client samples live under [`demo/`](demo/).

---

## Local development

**Needs:** Java 21, Maven 3.8+, Node 20+ (only if you run the Angular dev server separately), Docker optional.

1. **Broker:** `cd broker && mvn spring-boot:run` — set `DB_*` for Postgres, or use default file H2. For DB only: `docker compose up -d postgres`.
2. **UI (separate dev server):** `cd bearmq-frontend && npm install && npm start`
3. **Client library:** `cd bearmq-spring-client && mvn install`

The all-in-one Docker image embeds the Angular build into the broker JAR; `SpaFallbackFilter` serves `index.html` for client routes.

Published Maven coordinates for `bearmq-spring-client` depend on your repository; point `pom.xml` at your server.

---

## Configuration

| Variable | Purpose |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | JDBC; omit for file H2 |
| `SERVER_PORT` | HTTP (default `3333`) |
| `BEARMQ_BROKER_TCP_PORT` | TCP broker (default `6667`) |
| `BEARMQ_STORAGE_DIR` | Chronicle storage |
| `BEARMQ_DOMAIN` | Host part for generated vhost names |
| `BEARMQ_ADMIN_INITIAL_PASSWORD` | First-run admin when DB is empty (min 6 chars if set) |
| `BEARMQ_SECURITY_JWT_SECRET` | JWT signing secret (required in production) |
| `H2_TCP_SERVER_ENABLED`, `H2_TCP_SERVER_PORT` | H2 TCP server (dev / tools) |

Defaults and Chronicle JVM flags (`--add-opens` / `--add-exports`): [`broker/src/main/resources/application.yml`](broker/src/main/resources/application.yml).

- Root [`Dockerfile`](Dockerfile): full stack (UI + broker).  
- [`broker/Dockerfile`](broker/Dockerfile): broker JAR only.

---

## Repository layout

| Path | Role |
|------|------|
| [`broker/`](broker/) | Spring Boot: REST, TCP broker, Flyway, metrics |
| [`bearmq-frontend/`](bearmq-frontend/) | Angular SPA |
| [`bearmq-spring-client/`](bearmq-spring-client/) | `@EnableBear`, `BearTemplate`, listeners |
| [`demo/`](demo/) | Sample producer / consumer |
| [`media/`](media/) | Logo, architecture diagram, UI screenshots |

---

## REST API cheat sheet

| Area | Examples |
|------|----------|
| Auth | `POST /api/auth/login`, `POST /api/auth/refresh` |
| Broker | `POST /api/broker` (topology); `GET/POST/PATCH/DELETE …/api/broker/vhost…` |
| Metrics | `GET /api/metrics/*`, SSE resource stream |
| Settings | `GET /api/settings/messaging-api-key`, `PUT /api/users/me/password` |
| Admin | User CRUD, `POST /api/admin/messaging-api-key/rotate` |

Exact paths: `broker/src/main/java/com/bearmq/api/`.

**Migrations:** Flyway uses `classpath:db/migration/{vendor}` (`h2` or `postgresql`). Older DBs with different Flyway history may need a fresh schema or manual repair.

---

## Screenshots

### Logo & demo architecture

<p align="center">
  <strong>Logo</strong><br/>
  <img src="media/bearmq.png" width="280" alt="BearMQ logo full"/>
</p>

<p align="center">
  <strong>Demo architecture</strong><br/>
  <img src="media/demo.png" width="900" alt="BearMQ demo architecture diagram"/>
</p>

### Dark theme (`media/dark`)

<p align="center"><strong>Login</strong><br/><img src="media/dark/login.png" width="900" alt="BearMQ UI dark theme — Login"/></p>

<p align="center"><strong>Dashboard</strong><br/><img src="media/dark/dashboard.png" width="900" alt="BearMQ UI dark theme — Dashboard"/></p>

<p align="center"><strong>Instances</strong><br/><img src="media/dark/instances.png" width="900" alt="BearMQ UI dark theme — Instances list"/></p>

<p align="center"><strong>Instance detail (1)</strong><br/><img src="media/dark/instance-details-1.png" width="900" alt="BearMQ UI dark theme — Instance detail 1"/></p>

<p align="center"><strong>Instance detail (2)</strong><br/><img src="media/dark/instance-details-2.png" width="900" alt="BearMQ UI dark theme — Instance detail 2"/></p>

<p align="center"><strong>Instance detail (3)</strong><br/><img src="media/dark/instance-details-3.png" width="900" alt="BearMQ UI dark theme — Instance detail 3"/></p>

<p align="center"><strong>Instance detail (4)</strong><br/><img src="media/dark/instance-details-4.png" width="900" alt="BearMQ UI dark theme — Instance detail 4"/></p>

<p align="center"><strong>Instance detail (5)</strong><br/><img src="media/dark/instance-details-5.png" width="900" alt="BearMQ UI dark theme — Instance detail 5"/></p>

<p align="center"><strong>Instance detail (6)</strong><br/><img src="media/dark/instance-details-6.png" width="900" alt="BearMQ UI dark theme — Instance detail 6"/></p>

<p align="center"><strong>Topology</strong><br/><img src="media/dark/topology.png" width="900" alt="BearMQ UI dark theme — Topology"/></p>

<p align="center"><strong>Teams</strong><br/><img src="media/dark/teams.png" width="900" alt="BearMQ UI dark theme — Teams"/></p>

<p align="center"><strong>Settings</strong><br/><img src="media/dark/settings.png" width="900" alt="BearMQ UI dark theme — Settings"/></p>

### Light theme (`media/light`)

<p align="center"><strong>Login</strong><br/><img src="media/light/login.png" width="900" alt="BearMQ UI light theme — Login"/></p>

<p align="center"><strong>Password reset</strong><br/><img src="media/light/password-reset.png" width="900" alt="BearMQ UI light theme — Password reset"/></p>

<p align="center"><strong>Dashboard</strong><br/><img src="media/light/dashboard.png" width="900" alt="BearMQ UI light theme — Dashboard"/></p>

<p align="center"><strong>Instances list</strong><br/><img src="media/light/instances-list.png" width="900" alt="BearMQ UI light theme — Instances list"/></p>

<p align="center"><strong>Instance detail (1)</strong><br/><img src="media/light/instance-detail-1.png" width="900" alt="BearMQ UI light theme — Instance detail 1"/></p>

<p align="center"><strong>Instance detail (2)</strong><br/><img src="media/light/instance-detail-2.png" width="900" alt="BearMQ UI light theme — Instance detail 2"/></p>

<p align="center"><strong>Instance detail (3)</strong><br/><img src="media/light/instance-detail-3.png" width="900" alt="BearMQ UI light theme — Instance detail 3"/></p>

<p align="center"><strong>Instance detail (4)</strong><br/><img src="media/light/instance-detail-4.png" width="900" alt="BearMQ UI light theme — Instance detail 4"/></p>

<p align="center"><strong>Instance detail (5)</strong><br/><img src="media/light/instance-detail-5.png" width="900" alt="BearMQ UI light theme — Instance detail 5"/></p>

<p align="center"><strong>Instance detail (6)</strong><br/><img src="media/light/instance-detail-6.png" width="900" alt="BearMQ UI light theme — Instance detail 6"/></p>

<p align="center"><strong>Topology</strong><br/><img src="media/light/topology.png" width="900" alt="BearMQ UI light theme — Topology"/></p>

<p align="center"><strong>Team</strong><br/><img src="media/light/team.png" width="900" alt="BearMQ UI light theme — Team admin"/></p>

---

## Video

[![BearMQ intro](https://img.youtube.com/vi/KRjMb-gj2uM/0.jpg)](https://www.youtube.com/watch?v=KRjMb-gj2uM)

*Click the thumbnail to open the video on YouTube.*

---

## Troubleshooting

| Issue | What to check |
|--------|----------------|
| Port in use | `ss -tlnp \| grep -E ':3333\|:6667'` |
| DB errors | Postgres running, `DB_URL` host (`bearmq-postgres` on Docker network vs `localhost` on host) |
| No admin / login fails | Empty DB on first boot, password length if using env, `docker logs bearmq` |

---

## Roadmap

**Done (high level):** TCP broker, Spring client, JWT & roles, Flyway, metrics + SSE, Angular console, vhost lifecycle, global messaging API key, Modulith tests.

**Open:** stronger durability, performance tuning / benchmarks, optional public self-service registration.

---

## License & support

Licensed under the [MIT License](LICENSE). Thanks to the Spring and Chronicle communities.

<a href="https://www.buymeacoffee.com/nuricanozturk" target="_blank" rel="noopener noreferrer">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy me a coffee" width="200" height="60">
</a>
