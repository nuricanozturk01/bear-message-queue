# BearMQ

<div align="center">

<img src="media/bearmq.png" width="220" alt="BearMQ logo"/>

**A lightweight message broker and control plane for JVM-centric, queue-driven architectures.**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031?style=flat&logo=angular&logoColor=white)](https://angular.dev/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

---

## Table of contents

- [Quick start](#quick-start)
- [Installation](#installation)
  - [Using Docker with H2 (embedded)](#using-docker-with-h2-embedded)
  - [Using Docker with PostgreSQL](#using-docker-with-postgresql)
  - [Using Docker Compose with PostgreSQL](#using-docker-compose-with-postgresql)
  - [Manual installation](#manual-installation)
- [Usage](#usage)
- [Troubleshooting](#troubleshooting)
- [Publishing the Docker image (Repsy)](#publishing-the-docker-image-repsy)
- [Overview](#overview)
- [Project layout](#project-layout)
- [What's included](#whats-included)
- [Brand assets and UI themes](#brand-assets-and-ui-themes)
  - [Repository media (root)](#repository-media-root)
  - [Dark theme (`media/dark`)](#dark-theme-mediadark)
  - [Light theme (`media/light`)](#light-theme-medialight)
- [Features](#features)
- [Getting started](#getting-started)
  - [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Roadmap](#roadmap)
- [Example usage](#example-usage)
- [Demo architecture](#demo-architecture)
- [Video](#video)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Support](#support)

---

## Quick start

The fastest way to run BearMQ is a single container with **embedded H2** (no external database). From the **repository root**:

```bash
docker build -t bearmq .
docker run -d \
  --name bearmq \
  -p 3333:3333 \
  -p 6667:6667 \
  bearmq
```

Access:

- **Web UI and REST API**: [http://localhost:3333](http://localhost:3333)
- **TCP broker**: `localhost:6667`

To inspect H2 from the host while the container runs (same pattern as [Repsy](https://github.com/repsyio/repsy)): `-e H2_TCP_SERVER_ENABLED=true -p 9092:9092`, then JDBC `jdbc:h2:tcp://localhost:9092//app/data/bearmq` (matches the image `DB_URL` file path).

**Default admin user**

- **Username**: `admin`
- **Password**: if `BEARMQ_ADMIN_INITIAL_PASSWORD` is not set, a random password is generated on first startup. Retrieve it from the logs:

```bash
docker logs bearmq | grep "Password :"
```

For a predictable password and persistent data, use [Using Docker with H2 (embedded)](#using-docker-with-h2-embedded) below.

Full environment variable reference: [Configuration](#configuration).

---

## Installation

Same layout as [Repsy](https://github.com/repsyio/repsy): Docker with H2, Docker with PostgreSQL, Docker Compose, then manual dev setup.

### Using Docker with H2 (embedded)

No external database. Suitable for evaluation and development.

```bash
docker build -t bearmq .
docker run -d \
  --name bearmq \
  -p 3333:3333 \
  -p 6667:6667 \
  -e BEARMQ_ADMIN_INITIAL_PASSWORD=YourSecurePassword123 \
  -v bearmq-data:/app/data \
  bearmq
```

The `-v bearmq-data:/app/data` flag persists the H2 database across container restarts. The image sets **`BEARMQ_STORAGE_DIR=/app/data/queues`**, so **Chronicle queue files** stay under the same volume as H2. Without a volume, data is lost when the container is removed.

### Using Docker with PostgreSQL

1. Create a shared network:

```bash
docker network create bearmq-network
```

2. Start PostgreSQL:

```bash
docker run -d \
  --name bearmq-postgres \
  --network bearmq-network \
  -e POSTGRES_DB=bearmq \
  -e POSTGRES_USER=bearmq \
  -e POSTGRES_PASSWORD=bearmq123 \
  -p 5432:5432 \
  postgres:16-alpine
```

3. Start BearMQ:

```bash
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
  bearmq
```

### Using Docker Compose with PostgreSQL

From the repository root, [`docker-compose.yml`](docker-compose.yml) defines **postgres** and **bearmq** (image built from the root [`Dockerfile`](Dockerfile)):

```bash
docker compose up -d
```

Adjust `BEARMQ_ADMIN_INITIAL_PASSWORD` in the compose file before the first run, or set it in your environment.

To start **only PostgreSQL** (for example when running the broker with `mvn spring-boot:run` on the host):

```bash
docker compose up -d postgres
```

### Manual installation

**Prerequisites:** Java 21, Maven 3.8+, Node.js 20+ and npm (for the Angular console in dev), Docker (optional).

1. Clone the repository and enter it.
2. **Database**: export `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` for PostgreSQL, or omit them to use file H2 under `./data/bearmq.*` (see [`application.yml`](broker/src/main/resources/application.yml)). For Postgres only, you can run `docker compose up -d postgres` and point JDBC at `localhost:5432`.
3. **Broker**: `cd broker && mvn spring-boot:run`
4. **Console (dev)**: `cd bearmq-frontend && npm install && npm start` — or use the all-in-one Docker image and open port **3333**.

On first run with an empty database, user **`admin`** is created; set **`BEARMQ_ADMIN_INITIAL_PASSWORD`** (at least 6 characters) or read the random password from the logs.

5. **Spring client**: `cd bearmq-spring-client && mvn install`

Published client coordinates (example):

```xml
<repositories>
  <repository>
    <id>repsy</id>
    <name>BearMQ client repository</name>
    <url>https://repo.nuricanozturk.com/bearmq-spring-client</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.bearmq</groupId>
    <artifactId>bearmq-spring-client</artifactId>
    <version>0.0.1</version>
  </dependency>
</dependencies>
```

---

## Usage

### First login

1. Open [http://localhost:3333](http://localhost:3333) (or your `SERVER_PORT`).
2. Sign in with **username** `admin` and the password from **`BEARMQ_ADMIN_INITIAL_PASSWORD`** or from `docker logs bearmq | grep "Password :"` on first run.

Change the password under **Settings** after login.

---

## Troubleshooting

**Port already in use**

```bash
# Linux / macOS
ss -tlnp | grep -E ':3333|:6667'
```

**Database connection failed**

- Check that PostgreSQL is running: `docker ps | grep postgres`
- Check logs: `docker logs bearmq-postgres`
- Verify `DB_URL` uses the correct hostname (`bearmq-postgres` on Docker networks, `localhost` from the host)

**Admin user not created or cannot log in**

- Ensure the database was empty on first startup
- Verify `BEARMQ_ADMIN_INITIAL_PASSWORD` is at least 6 characters when set
- Inspect application logs: `docker logs bearmq`

**Follow logs**

```bash
docker logs -f bearmq
docker logs -f bearmq-postgres
```

---

## Publishing the Docker image (Repsy)

Build from the **repository root** (same as [Repsy](https://github.com/repsyio/repsy) publishing flow). Replace the image name with the full path shown in your [Repsy](https://github.com/repsyio/repsy) registry UI (pattern similar to `repo.repsy.io/repsy/os/repsy:26.03.0`).

```bash
docker build -t bearmq:local .

docker tag bearmq:local repo.repsy.io/<namespace>/<type>/bearmq:<tag>

docker login repo.repsy.io
docker push repo.repsy.io/<namespace>/<type>/bearmq:<tag>
```

After publishing, others can run:

```bash
docker pull repo.repsy.io/<namespace>/<type>/bearmq:<tag>
docker run -d --name bearmq -p 3333:3333 -p 6667:6667 repo.repsy.io/<namespace>/<type>/bearmq:<tag>
```

---

## Overview

BearMQ is a **message broker** plus a **control plane**: TCP messaging over **virtual hosts**, with **exchanges**, **queues**, and **bindings**, backed by a **Spring Boot 3.5** application (**Spring Modulith**: `api`, `server`, `shared`), a **JWT**-secured **REST API**, **PostgreSQL** (Flyway migrations), and an **Angular 21** console with **dark / light** themes.

**Highlights**

| Area | What you get |
|------|----------------|
| **Broker** | TCP server; per–virtual-host credentials; Chronicle Queue–backed queue storage; runtime load/unload when vhosts are activated, paused, or removed. |
| **Topology** | Apply JSON schema from API or UI; list queues, exchanges, bindings; soft-delete individual resources; reload broker runtime after changes. |
| **Tenants & security** | **ADMIN** / **USER** roles; access + refresh tokens; first-run **admin** bootstrap (random password logged once); self-service password change; admin user lifecycle (including safe-guards for last admin / self-delete). |
| **Virtual hosts** | Create instances, paginated list, per-instance **status** (e.g. active / paused / inactive), connection details, **metrics**, **code snippets** tab; **admins** can **delete** an instance (soft delete + runtime unload). |
| **Global messaging key** | Shared **messaging API key** for TCP `AUTH` (stored in app settings); dashboard shows key; **admin** can **rotate** it. |
| **Metrics** | REST: summary, per-vhost, cluster **resource** stats; **SSE** stream for live resource metrics (used by the dashboard). |
| **Engineering** | `ModulithStructureTest` (module boundaries), slice tests (`WebMvcTest`, services), Docker-based **smoke** tests where applicable. |

---

## Project layout

| Path | Role |
|------|------|
| [`broker/`](broker/) | Single deployable **Spring Boot** app: REST API, embedded TCP broker thread, optional metrics server, Flyway, Modulith modules. |
| [`bearmq-frontend/`](bearmq-frontend/) | **Angular 21** SPA: dashboard, instances, topology, teams (admin), settings, login; proxies to the API in dev. |
| [`bearmq-spring-client/`](bearmq-spring-client/) | Spring Boot **client** library (`@EnableBear`, `@BearListener`, `BearTemplate`, etc.). |
| [`demo/`](demo/) | Sample **producer / consumer** apps wired to the client. |
| [`media/`](media/) | Logo, architecture diagram, **dark** / **light** UI screenshots ([below](#brand-assets-and-ui-themes)). |

---

## What's included

### Broker & TCP

- Virtual host–scoped **AUTH** using vhost name, username, password (Base64 in API responses), plus the **global messaging API key** policy configured on the server.
- **Exchanges** (e.g. topic, fanout), **queues**, **bindings**; message routing and persistence via **Chronicle Queue** (with documented **JVM** `--add-opens` / `--add-exports`).
- **Transactional domain events** (`VirtualHostActivatedEvent`, `VirtualHostDeletedEvent`) so the in-memory/runtime graph stays in sync after status changes, topology edits, or instance deletion.

### REST API (summary)

| Prefix | Capabilities |
|--------|----------------|
| `POST /api/auth/login`, `POST /api/auth/refresh` | JWT **access** and **refresh** tokens (public). |
| `POST /api/broker` | Apply **topology** (exchanges, queues, bindings) for a vhost. |
| `GET/POST/PATCH/DELETE …/api/broker/vhost` | **List** (paged), **create**, **status** patch, **delete** instance; **list** queues / exchanges / bindings; **delete** queue, exchange, or binding by id. |
| `GET /api/metrics/*`, `GET /api/metrics/resources/stream` | **Summary**, **per-vhost**, **resource** metrics; **SSE** stream (`text/event-stream`). |
| `GET /api/settings/messaging-api-key` | Current **global messaging API key** (authenticated). |
| `PUT /api/users/me/password` | **Change own password**. |
| `GET/POST/PUT/DELETE /api/admin/users…`, `POST /api/admin/messaging-api-key/rotate` | **Admin**: list/create/delete users, set password, set **role**, **rotate** messaging API key. |

*(Exact paths follow the controllers under `broker/src/main/java/com/bearmq/api/`.)*

### Angular console

- **Dashboard**: messaging API key (reveal/copy), optional live **resource metrics** (HTTP + SSE).
- **Instances**: paginated table/cards; **instance detail** with tabs (connection, queues, exchanges, bindings, metrics, **code snippets** for Java/Kotlin/Scala/Python/curl).
- **Topology**: JSON editor to **apply** the same broker schema as the REST API.
- **Teams** (ADMIN): user list and administration aligned with the admin API.
- **Settings**: **Account security** (change password).
- **Themes**: persistent **light / dark** toggle; assets under [`media/dark`](media/dark) and [`media/light`](media/light).

### Data & migrations

- **H2** (default, file-based) or **PostgreSQL**; **Flyway** loads **`V001__bearmq_schema.sql`** only — under `db/migration/h2` or `db/migration/postgresql` depending on JDBC URL (`classpath:db/migration/{vendor}` in [`application.yml`](broker/src/main/resources/application.yml)). Each file is the full current schema (no incremental history). Existing databases that already ran older `V1`–`V6` scripts need a **new empty schema** or manual Flyway history cleanup before switching to `V001`.

---

## Brand assets and UI themes

Screenshots live under [`media/`](media/) (paths from repo root — they render on GitHub as embedded images below).

### Repository media (root)

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

## Features

See **[What's included](#whats-included)** for the full breakdown. At a glance:

| Layer | Stack |
|-------|--------|
| **Broker** | Java 21, Spring Boot 3.5, Spring Modulith, TCP + Chronicle Queue |
| **API** | REST, JWT, Spring Security, CORS, paged vhost list (Spring Data `Page` DTO mode) |
| **UI** | Angular 21, Bootstrap 5, SSE client for metrics |
| **Client** | `bearmq-spring-client` (`@EnableBear`, `@BearListener`, topology beans) |
| **Data** | **H2** or **PostgreSQL** via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`; Flyway **single baseline** per vendor (`V001__bearmq_schema.sql` in `db/migration/h2` or `postgresql`), JPA |

---

## Getting started

For **Docker**, start with [Quick start](#quick-start) and [Installation](#installation). The sections below summarize local development.

### Prerequisites

- **Java** 21  
- **Maven** 3.8+  
- **Node.js** 20+ and **npm** (for `bearmq-frontend` when not using the unified image)  
- **Docker** (optional: all-in-one image, Compose stack, or Postgres-only via `docker compose up -d postgres`)

Clone:

```bash
git clone https://github.com/nuricanozturk01/bearmq.git
cd bearmq
```

Then follow [Manual installation](#manual-installation) for broker, frontend, and client steps.

The unified image builds the Angular app into `broker/src/main/resources/static/`; `SpaFallbackFilter` serves `index.html` for client-side routes (same idea as [Repsy](https://github.com/repsyio/repsy)).

---

## Configuration

### JVM options (Chronicle / NIO)

The broker may require reflective access for Chronicle Queue and NIO. Example:

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED
--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED
```

### Application settings

YAML defaults live in [`broker/src/main/resources/application.yml`](broker/src/main/resources/application.yml). For containers and ops, prefer **environment variables** (same pattern as [Repsy](https://github.com/repsyio/repsy)):

| Variable | Purpose | Default (when unset) |
|----------|---------|------------------------|
| **`DB_URL`** | JDBC URL (H2 file or PostgreSQL) | H2 file under `./data/bearmq` (Dockerfile: `/app/data/bearmq`) |
| **`DB_USERNAME`** | DB user | `sa` (H2) |
| **`DB_PASSWORD`** | DB password | empty |
| **`SERVER_PORT`** | HTTP (REST + static UI) | `3333` |
| **`BEARMQ_BROKER_TCP_PORT`** | TCP broker listen port | `6667` |
| **`BEARMQ_STORAGE_DIR`** | Chronicle queue storage | `./data/queues` (Docker: `/app/data/queues`) |
| **`BEARMQ_DOMAIN`** | Host used when minting vhost names | `localhost:6667` |
| **`BEARMQ_ADMIN_INITIAL_PASSWORD`** | First-run admin password (only when DB is empty); min 6 chars if set | _(empty → random password logged once)_ |
| **`BEARMQ_SECURITY_JWT_SECRET`** | JWT signing secret | dev default in YAML (set in production) |
| **`H2_TCP_SERVER_ENABLED`** | Starts the H2 TCP server (same idea as [Repsy](https://github.com/repsyio/repsy); dev / DB tools only) | `false` |
| **`H2_TCP_SERVER_PORT`** | H2 TCP listen port | `9092` |

With **`H2_TCP_SERVER_ENABLED=true`** and an H2 **`DB_URL`**, connect from DBeaver or IntelliJ using a **TCP** JDBC URL, for example `jdbc:h2:tcp://localhost:9092/./data/bearmq` when the broker uses `jdbc:h2:file:./data/bearmq` (adjust the path segment to match your `DB_URL`; Docker file DB is under `/app/data/bearmq`).

Also configurable in YAML: metrics server, JWT lifetimes, CORS origins, broker backlog / dequeue wait.

The `demo/*` apps show minimal `bearmq:` client YAML for producers and consumers.

### Container image (broker)

[`broker/Dockerfile`](broker/Dockerfile) (if used standalone) builds the broker JAR only. The **root** [`Dockerfile`](Dockerfile) builds the all-in-one image (frontend + broker). Pass production **`DB_*`**, **`BEARMQ_SECURITY_JWT_SECRET`**, and **`BEARMQ_ADMIN_INITIAL_PASSWORD`** via `-e` or your orchestrator.

---

## Roadmap

| Status | Item |
|--------|------|
| Done | MVP broker and Spring client |
| Done | JWT login + refresh; role-based **ADMIN** / **USER** |
| Done | Broker + optional metrics server; REST control plane |
| Done | Flyway migrations; global **messaging API key** + rotation |
| Done | Virtual host lifecycle: create, status, instance delete, topology CRUD with runtime reload |
| Done | Metrics API, **SSE** resource stream, dashboard integration |
| Done | Angular console: instances, topology, teams, settings, code snippets; **dark / light** ([screenshots](#brand-assets-and-ui-themes)) |
| Done | Spring Modulith module checks + automated tests (controller, facade, smoke) |
| Open | Stronger durability guarantees (fewer message-loss edge cases) |
| Open | Performance tuning and formal benchmarks |
| Open | Public self-service **register** endpoint (if you want open signup; today onboarding is admin-driven after first login) |

---

## Example usage

### Login and create a virtual host

Sign in (use your **`admin`** password from first-run logs, or any user created via **Teams** / admin API):

```bash
curl -s -X POST 'http://localhost:3333/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<your-password>"}'
```

Copy `access_token` from the JSON response.

Create a virtual host:

```bash
curl -s -X POST 'http://localhost:3333/api/broker/vhost' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json'
```

Example response shape:

```json
{
  "id": "01K3C3WPE87JQ4XNWHR2YM208E",
  "name": "nuricanozturk-yaiumxoup",
  "username": "awrbjvzwc",
  "password": "RkZMcGhTS2pN",
  "domain": "xghqwinaj.localhost",
  "url": "xghqwinaj.localhost"
}
```

> `password` may be **Base64-encoded** in the API; decode in your client or copy from the UI.

### Spring Boot client (`application.yml`)

```yaml
bearmq:
  username: awrbjvzwc
  password: RkZMcGhTS2pN
  host: localhost
  port: 6667
  virtual-host: nuricanozturk-yaiumxoup
  api-key: <global-messaging-api-key>
```

Use the **messaging API key** from the dashboard (`/api/settings/messaging-api-key`) or after an admin **rotation**. Enable the client with `@EnableBear`, declare exchanges / queues / bindings as beans, and use `@BearListener` on consumers. See [`demo/`](demo/) for full examples.

---

## Demo architecture

The architecture diagram is embedded in [Repository media (root)](#repository-media-root) (`media/demo.png`) so it is not duplicated here.

---

## Video

[![BearMQ intro](https://img.youtube.com/vi/KRjMb-gj2uM/0.jpg)](https://www.youtube.com/watch?v=KRjMb-gj2uM)

*Click the image to open the video on YouTube.*

---

## License

BearMQ is released under the [MIT License](LICENSE).

---

## Acknowledgments

Thanks to contributors and to the **Spring** and **Chronicle** ecosystems that this project builds on.

---

## Support

If BearMQ is useful in your work, you can support ongoing development:

<a href="https://www.buymeacoffee.com/nuricanozturk" target="_blank" rel="noopener noreferrer">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy me a coffee" width="200" height="60">
</a>
