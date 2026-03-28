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
  - [Installation](#installation)
- [Configuration](#configuration)
- [Roadmap](#roadmap)
- [Example usage](#example-usage)
- [Demo architecture](#demo-architecture)
- [Video](#video)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Support](#support)

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

- **PostgreSQL** metadata; **Flyway** scripts under `broker/src/main/resources/db/migration/` (tenants, vhosts, bindings deduplication, roles, settings / API key evolution, etc.).

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
| **Data** | PostgreSQL, Flyway, JPA |

---

## Getting started

### Prerequisites

- **Java** 21  
- **Maven** 3.8+  
- **Node.js** 20+ and **npm** (for `bearmq-frontend`)  
- **Docker** (optional: Postgres via Compose, or use your own DB)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/nuricanozturk01/bearmq.git
   cd bearmq
   ```

2. **Database** — start PostgreSQL (defaults in `application.yml` match the root [`docker-compose.yml`](docker-compose.yml)):

   ```bash
   docker compose up -d
   ```

   Flyway applies schema when the broker starts.

3. **Run the broker** (from `broker/`)

   ```bash
   cd broker
   mvn spring-boot:run
   ```

   On **first run** with an empty database, an **`admin`** user is created and a **one-time random password** is printed in the logs — sign in and change it under **Settings**.

4. **Run the console** (from `bearmq-frontend/`)

   ```bash
   npm install
   npm start
   ```

   Default dev server is proxied to the API (see `proxy.conf.json`).

5. **Build the Spring client** (for app development)

   ```bash
   cd bearmq-spring-client
   mvn install
   ```

6. **Consume the client from Maven** (alternative: published repository)

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

Key knobs in `broker/src/main/resources/application.yml` (and env overrides such as `BEARMQ_SECURITY_JWT_SECRET`):

| Area | Examples |
|------|-----------|
| **HTTP** | `server.port` (default **3333**) |
| **TCP broker** | `bearmq.server.broker.port` (default **6667**), backlog, dequeue wait |
| **Metrics server** | `bearmq.server.metrics.enabled`, `port` (e.g. **6668**) |
| **JWT** | Secret, access / refresh lifetimes |
| **CORS** | `bearmq.security.cors.allowed-origins` (Angular dev on **4200**) |
| **Domain** | `bearmq.domain` used when minting vhost hostnames |

The `demo/*` apps show minimal `bearmq:` client YAML for producers and consumers.

### Container image (broker)

[`broker/Dockerfile`](broker/Dockerfile) builds a multi-stage image (JDK → JRE), exposes **3333** (HTTP) and **6667** (TCP). Pass the same env vars / secrets you would use for production JWT and database URLs.

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
