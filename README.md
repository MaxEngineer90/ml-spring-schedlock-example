# ShedLock Multi-Container Demo 🔒

Demonstrates scheduled job coordination between multiple Spring Boot containers using ShedLock.

## 🚀 Quick Start

```bash
# Start demo
cd docker
docker compose build && docker compose up -d

# Stop demo
docker compose down

# Complete cleanup
docker compose down -v && docker system prune -a
```

## 🌐 Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Portainer** | http://localhost:9000 | `admin` / `admin123` |
| **MailHog** | http://localhost:8025 | - |
| **Container 1 API** | http://localhost:8080/api/test/health | - |
| **Container 2 API** | http://localhost:8081/api/test/health | - |
| **Container 3 API** | http://localhost:8082/api/test/health | - |

## 📊 Container Management

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

# Filter for scheduled jobs
docker compose logs -f | grep "Scheduled job"
```

## 🎯 Demo Results

### ❌ WITHOUT ShedLock (Problem)
- **3 containers** = **3 emails** every 2 minutes
- Each container executes jobs independently  
- **MailHog shows**: 3 different emails from 3 containers

### ✅ WITH ShedLock (Solution)
- **3 containers** = **1 email** every 2 minutes
- Only one container executes jobs (coordinated via MySQL)
- **MailHog shows**: 1 email from winning container

## 🔧 Manual Email Test

```bash
# Send email from specific container
curl 'http://localhost:8080/api/test/send-email-to?email=test@demo.com'
curl 'http://localhost:8081/api/test/send-email-to?email=test@demo.com'  
curl 'http://localhost:8082/api/test/send-email-to?email=test@demo.com'
```

## 📁 Architecture

```
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Spring Boot #1  │ │ Spring Boot #2  │ │ Spring Boot #3  │
│ Port: 8080      │ │ Port: 8081      │ │ Port: 8082      │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                    ┌─────────────────┐
                    │ MySQL + ShedLock│
                    │ (Coordination)  │
                    └─────────────────┘
                             │
                    ┌─────────────────┐
                    │     MailHog     │
                    │ (Email Catcher) │
                    └─────────────────┘
```

## 🔒 ShedLock Configuration

**Lock Provider**: MySQL JdbcTemplate  
**Lock Table**: `shedlock`  
**Lock Duration**: Max 9min, Min 1min  
**Coordination**: Database-based distributed locking

---

**🎉 Result**: Perfect coordination between containers in Kubernetes/Docker environments!
