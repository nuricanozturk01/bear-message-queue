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
2. [Spring client for your app (Maven / Gradle)](#spring-client-for-your-app-maven--gradle)
3. [Quick start (Docker)](#quick-start-docker)
4. [Run options](#run-options)
5. [Configuration](#configuration)
6. [Screenshots](#screenshots)
7. [Video](#video)
8. [License & support](#license--support)

---

## What it is

BearMQ is a **Spring Boot 3.5** application: **TCP** messaging (Chronicle Queue), **virtual host / exchange / queue / binding** topology, a **JWT**-protected **REST API**, and a bundled **Angular 21** UI. Code is split with Spring Modulith (`api`, `server`, `shared`). Storage is **file H2** or **PostgreSQL** with Flyway.

---

## Spring client for your app (Maven / Gradle)

If you build **producers or consumers** in Java/Kotlin, add **`bearmq-spring-client`** to your app **first** (then configure `bearmq:` in `application.yml` and use `@EnableBear`, `@BearListener`, `BearTemplate`, etc. — see [`demo/`](demo/)).

### Maven

In your **`pom.xml`**, register the repository and the dependency:

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

### Gradle (Kotlin DSL)

```kotlin
repositories {
  maven {
    name = "bearmq-client"
    url = uri("https://repo.nuricanozturk.com/bearmq-spring-client")
  }
}

dependencies {
  implementation("com.bearmq:bearmq-spring-client:0.0.1")
}
```

## Quick start (Docker)

Published image (Repsy):

```bash
docker run -d \
  --name bearmq \
  -p 3333:3333 \
  -p 6667:6667 \
  repo.repsy.io/nuricanozturk/bearmq/bearmq:26.03.3
```
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
  -e BEARMQ_ADMIN_INITIAL_PASSWORD=Test123 \
  -v bearmq-data:/app/data \
  repo.repsy.io/nuricanozturk/bearmq/bearmq:26.03.3
```

The volume keeps H2 files and (with the default image settings) Chronicle data under `/app/data`.

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

---

## Screenshots

### Demo architecture

<p align="center">
  <strong>Demo architecture</strong><br/>
  <img src="media/demo.png" width="900" alt="BearMQ demo architecture diagram"/>
</p>

### Dark theme (`media/dark`)

<p align="center"><strong>Login</strong><br/><img src="media/dark/login.png" width="900" alt="BearMQ UI dark theme — Login"/></p>

<p align="center"><strong>Dashboard</strong><br/><img src="media/dark/dashboard.png" width="900" alt="BearMQ UI dark theme — Dashboard"/></p>

<p align="center"><strong>Instances</strong><br/><img src="media/dark/instances.png" width="900" alt="BearMQ UI dark theme — Instances list"/></p>

<p align="center"><strong>Instance detail (1)</strong><br/><img src="media/dark/instance-details-1.png" width="900" alt="BearMQ UI dark theme — Instance detail 1"/></p>

<p align="center"><strong>Preview (1.5)</strong><br/><img src="media/dark/preview.png" width="900" alt="BearMQ UI dark theme — Preview"/></p>

<p align="center"><strong>Instance detail (2)</strong><br/><img src="media/dark/instance-details-2.png" width="900" alt="BearMQ UI dark theme — Instance detail 2"/></p>

<p align="center"><strong>Queue Messages (2.5)</strong><br/><img src="media/dark/messages.png" width="900" alt="BearMQ UI dark theme — Instance detail 2.5"/></p>

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

<p align="center"><strong>Preview (1.5)</strong><br/><img src="media/light/preview.png" width="900" alt="BearMQ UI light theme — Preview"/></p>

<p align="center"><strong>Instance detail (2)</strong><br/><img src="media/light/instance-detail-2.png" width="900" alt="BearMQ UI light theme — Instance detail 2"/></p>

<p align="center"><strong>Queue Messages (2.5)</strong><br/><img src="media/light/messages.png" width="900" alt="BearMQ UI light theme — Instance detail 2.5"/></p>

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

## License & support

Licensed under the [MIT License](LICENSE). Thanks to the Spring and Chronicle communities.

<a href="https://www.buymeacoffee.com/nuricanozturk" target="_blank" rel="noopener noreferrer">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy me a coffee" width="200" height="60">
</a>
