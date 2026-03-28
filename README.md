# BearMQ

<div align="center">

<img src="media/bearmq.png" width="220" alt="BearMQ logo"/>

**A lightweight message broker and control plane for JVM-centric, queue-driven architectures.**

[![Spring](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

---

## Table of contents

- [Overview](#overview)
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

BearMQ is a message queue system built around **virtual hosts**, **exchanges**, **queues**, and **bindings**, with a **Spring Boot** broker, **REST API**, **metrics**, and an **Angular** management UI. It targets teams that want a clear topology model and a small operational footprint on the JVM.

**Highlights**

| Area | What you get |
|------|----------------|
| **Topology** | Declarative exchanges, queues, and bindings (including topic-style routing). |
| **Runtime** | Broker process with persistence-oriented queue storage (e.g. Chronicle Queue). |
| **Platform** | Multi-tenant style accounts, JWT-backed API, optional Docker-based local stack. |
| **Observability** | Metrics endpoints and UI dashboards for instances and resources. |

---

## Brand assets and UI themes

All promotional screenshots and logos live under [`media/`](media/). Paths are relative to the **repository root** so they render correctly on GitHub.

### Repository media (root)

| File | Role |
|------|------|
| [`media/bearmq.png`](media/bearmq.png) | Primary **logo** used in the README header and branding. |
| [`media/demo.png`](media/demo.png) | **Architecture / demo** diagram illustrating components and data flow. |

### Dark theme (`media/dark`)

Screens captured with the **dark** UI theme (GitHub-dark inspired palette in the console). Use these for docs, slides, or store listings when you want a dark presentation.

| File | Suggested use |
|------|----------------|
| [`media/dark/login.png`](media/dark/login.png) | Sign-in screen. |
| [`media/dark/dashboard.png`](media/dark/dashboard.png) | Main dashboard (overview, API key, metrics snapshot). |
| [`media/dark/instances.png`](media/dark/instances.png) | Virtual host (**instances**) list. |
| [`media/dark/instance-details-1.png`](media/dark/instance-details-1.png) | Instance detail — tab / section **1**. |
| [`media/dark/instance-details-2.png`](media/dark/instance-details-2.png) | Instance detail — tab / section **2**. |
| [`media/dark/instance-details-3.png`](media/dark/instance-details-3.png) | Instance detail — tab / section **3**. |
| [`media/dark/instance-details-4.png`](media/dark/instance-details-4.png) | Instance detail — tab / section **4**. |
| [`media/dark/instance-details-5.png`](media/dark/instance-details-5.png) | Instance detail — tab / section **5**. |
| [`media/dark/instance-details-6.png`](media/dark/instance-details-6.png) | Instance detail — tab / section **6**. |
| [`media/dark/topology.png`](media/dark/topology.png) | Topology editor (JSON / schema apply). |
| [`media/dark/teams.png`](media/dark/teams.png) | Teams / user administration (admin). |
| [`media/dark/settings.png`](media/dark/settings.png) | Account security (e.g. password change). |

> **Naming note:** Dark theme instance shots use the prefix `instance-details-*` (plural “details”).

### Light theme (`media/light`)

Screens captured with the **light** UI theme. Pair with `media/dark` when you need before/after or theme comparison.

| File | Suggested use |
|------|----------------|
| [`media/light/login.png`](media/light/login.png) | Sign-in screen. |
| [`media/light/password-reset.png`](media/light/password-reset.png) | Password / credential recovery flow (where applicable). |
| [`media/light/dashboard.png`](media/light/dashboard.png) | Main dashboard. |
| [`media/light/instances-list.png`](media/light/instances-list.png) | Virtual host (**instances**) list. |
| [`media/light/instance-detail-1.png`](media/light/instance-detail-1.png) | Instance detail — tab / section **1**. |
| [`media/light/instance-detail-2.png`](media/light/instance-detail-2.png) | Instance detail — tab / section **2**. |
| [`media/light/instance-detail-3.png`](media/light/instance-detail-3.png) | Instance detail — tab / section **3**. |
| [`media/light/instance-detail-4.png`](media/light/instance-detail-4.png) | Instance detail — tab / section **4**. |
| [`media/light/instance-detail-5.png`](media/light/instance-detail-5.png) | Instance detail — tab / section **5**. |
| [`media/light/instance-detail-6.png`](media/light/instance-detail-6.png) | Instance detail — tab / section **6**. |
| [`media/light/topology.png`](media/light/topology.png) | Topology editor. |
| [`media/light/team.png`](media/light/team.png) | Team / user administration (admin). |

> **Naming note:** Light theme instance shots use `instance-detail-*` (singular “detail”) and a dedicated `instances-list.png` for the list view.

### Using theme assets in documentation

- Prefer **dark** or **light** consistently per document; mix only when comparing themes.
- When adding new screenshots, keep the same folder convention: `media/dark/…` and `media/light/…`.
- Update the tables above if you add or rename files so the README stays the single source of truth.

---

## Features

| Component | Details |
|-----------|---------|
| **Broker** | TCP messaging, virtual hosts, exchanges, queues, bindings. |
| **API** | Spring Web MVC REST API; JWT authentication. |
| **Persistence** | PostgreSQL for metadata; queue storage integration (e.g. Chronicle Queue). |
| **Client** | `bearmq-spring-client` for Spring Boot apps (`@EnableBear`, `@BearListener`, templates). |
| **UI** | Angular app: dashboard, instances, topology, teams (admin), settings; **dark / light** themes (see [UI themes](#brand-assets-and-ui-themes)). |
| **Ops** | Docker Compose for local dependencies; configurable broker and metrics ports. |

---

## Getting started

### Prerequisites

- **Java** 21  
- **Maven** 3.8+  
- **Docker** (optional, for Compose-based dependencies)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/nuricanozturk01/bearmq.git
   cd bearmq
   ```

2. **Start infrastructure** (if using Docker Compose)

   ```bash
   docker compose up -d
   ```

3. **Build the Spring client** (for local development)

   ```bash
   cd bearmq-spring-client
   mvn install
   ```

4. **Consume the client from Maven** (alternative: published repository)

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

Runtime behaviour is controlled via `application.yml` (and profiles): database, broker ports, security (JWT, CORS), metrics, and feature flags. See the `broker` and `demo` modules for concrete examples.

---

## Roadmap

| Status | Item |
|--------|------|
| Done | MVP broker and client |
| Done | Spring Security and JWT |
| Done | Split broker server, metrics, and REST API |
| Done | Metrics API and UI |
| Done | Admin / teams flows in UI |
| Done | Landing / SaaS-oriented UI and dark–light themes ([screenshots](#brand-assets-and-ui-themes)) |
| Done | Retry and broader test coverage |
| Open | Stronger durability guarantees (reduce message loss scenarios) |
| Open | Performance tuning and benchmarks |

---

## Example usage

### Register and create a virtual host

Register (adjust host and payload to your environment):

```bash
curl --location 'http://localhost:3333/api/auth/register' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "username": "nuricanozturk",
    "password": "Test123"
  }'
```

Create a virtual host (use a valid `Authorization` bearer token from login):

```bash
curl --location --request POST 'http://localhost:3333/api/broker/vhost' \
  --header 'Authorization: Bearer <access_token>' \
  --header 'Content-Type: application/json'
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
```

Enable the client on your application class with `@EnableBear`, declare exchanges/queues/bindings as beans, and use `@BearListener` on consumer methods. See the `demo` module for full examples.

---

## Demo architecture

<p align="center">
  <img src="media/demo.png" alt="BearMQ demo architecture diagram" width="85%"/>
</p>

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
