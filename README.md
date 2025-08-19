# ShedLock Multi-Container Demo

Demonstrates scheduled job coordination between multiple Spring Boot containers using ShedLock.

## Quick Start

```bash
# Start demo
cd docker
docker compose build && docker compose up -d

# Stop demo
docker compose down

# Complete cleanup
docker compose down -v && docker system prune -a
```

## Access URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Portainer** | http://localhost:9000 | Docker Management UI |
| **MailHog** | http://localhost:8025 | Email Catcher Web UI |
| **Adminer** | http://localhost:8083 | Database Admin Tool |
| **Container 1 API** | http://localhost:8080/api/test/health | Spring Boot Instance #1 |
| **Container 2 API** | http://localhost:8081/api/test/health | Spring Boot Instance #2 |
| **Container 3 API** | http://localhost:8082/api/test/health | Spring Boot Instance #3 |

### Database Access (Adminer)
- **Server**: `mysql`
- **Username**: `root` 
- **Password**: `password`
- **Database**: `schedule_demo`

## Container Management

### Start Containers
```bash
cd docker
docker compose up -d
```

### Stop Containers  
```bash
# Stop containers (keep data)
docker compose down

# Stop + remove volumes (delete database)
docker compose down -v
```

### Complete Docker Cleanup
```bash
# Nuclear option - removes everything
docker system prune -a
```

### Monitor Logs
```bash
# All containers
docker compose logs -f

# Specific container
docker compose logs -f demo-schedule-1

# Filter for ShedLock events
docker compose logs -f | grep "ShedLock"

# Filter for scheduled jobs
docker compose logs -f | grep "Scheduled"
```

## Demo Results

### Without ShedLock (Problem)
- **3 containers** = **3 emails** every minute
- Each container executes jobs independently  
- **MailHog shows**: 3 different emails from 3 containers

### With ShedLock (Solution)
- **3 containers** = **1 email** every minute
- Only one container executes jobs (coordinated via MySQL)
- **MailHog shows**: 1 email from winning container
- **Logs show**: ACQUIRED/FAILED coordination between containers

## Manual Testing

### Send Test Email
```bash
# Send email from specific container
curl http://localhost:8080/api/test/send-email
curl http://localhost:8081/api/test/send-email
curl http://localhost:8082/api/test/send-email
```

### Check Container Health
```bash
curl http://localhost:8080/api/test/health
curl http://localhost:8081/api/test/health  
curl http://localhost:8082/api/test/health
```

## Architecture

```
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Spring Boot #1  │ │ Spring Boot #2  │ │ Spring Boot #3  │
│ Port: 8080      │ │ Port: 8081      │ │ Port: 8082      │
│ Container-1 #1  │ │ Container-2 #2  │ │ Container-3 #3  │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                    ┌─────────────────┐
                    │ MySQL + ShedLock│
                    │ (Coordination)  │
                    │ + Adminer       │
                    └─────────────────┘
                             │
                    ┌─────────────────┐
                    │     MailHog     │
                    │ (Email Catcher) │
                    └─────────────────┘
```

## ShedLock Configuration

**Lock Provider**: MySQL JdbcTemplate with Database Time  
**Lock Table**: `shedlock`  
**Lock Duration**: Max 9min, Min 1min for email job | Max 25s, Min 10s for heartbeat  
**Coordination**: Database-based distributed locking  
**Logging**: Custom event logging with container identification  
**Timezone**: Europe/Berlin for all containers

## Log Examples

```
ShedLock ACQUIRED: send-test-email [container-1 #1]
ShedLock FAILED: send-test-email [container-2 #2] - already locked by another instance
ShedLock RELEASED: send-test-email (2s) [container-1 #1]
```

---

**Result**: Perfect coordination between containers in Kubernetes/Docker environments!