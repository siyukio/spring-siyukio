# Docker

This document describes how to start the PostgreSQL service using Docker.

## Colima

Check the current Colima status:

```bash
colima status
```

Start Colima if it is not running:

```bash
colima start
```

## Start PostgreSQL

Start the PostgreSQL service defined in `postgres/docker-compose.yml`:

```bash
docker compose -f postgres/docker-compose.yml up -d
```

Stop and remove the service:

```bash
docker compose -f postgres/docker-compose.yml down
```
