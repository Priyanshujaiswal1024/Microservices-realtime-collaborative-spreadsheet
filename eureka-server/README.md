# Eureka Server

Netflix Eureka Server for service discovery and registration in the microservices architecture.

## 🎯 Purpose

Eureka Server acts as a service registry where all microservices:
1. **Register** themselves on startup
2. **Discover** other services dynamically
3. **Heartbeat** to indicate they're alive
4. **Deregister** on shutdown

This enables:
- Dynamic service discovery (no hardcoded URLs)
- Client-side load balancing
- Fault tolerance (automatic removal of failed instances)
- Zero-downtime deployments

## 🚀 Quick Start

### 1. Start Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
```

The server will start on port **8761**.

### 2. Access Dashboard

Open your browser to: **http://localhost:8761**

You'll see:
- Registered services
- Instance status
- Replicas info
- General info

## 📊 Dashboard

The Eureka dashboard shows:

### Instances Currently Registered
All services that have successfully registered:
- **Application name** (e.g., USER-SERVICE, SHEET-SERVICE)
- **Status** (UP, DOWN, OUT_OF_SERVICE)
- **Instance ID** (unique identifier)
- **Availability Zone**
- **Metadata** (custom key-value pairs)

### General Info
- Environment, Data center
- Current time
- Uptime
- Lease expiration enabled
- Renews threshold
- Renews (last min)

### Instance Info
For each service instance:
- **Home Page URL** - Root URL of the service
- **Status Page URL** - Actuator health endpoint
- **Health Check URL** - For monitoring

## 🔧 Configuration

### Development Settings

```yaml
eureka:
  server:
    enable-self-preservation: false  # Disable for faster eviction
    eviction-interval-timer-in-ms: 5000
```

- **Self-preservation disabled** - Services removed quickly when they stop
- **Fast eviction** - 5-second intervals for cleanup

### Production Settings

```yaml
eureka:
  server:
    enable-self-preservation: true   # Protect against network issues
    eviction-interval-timer-in-ms: 60000
```

- **Self-preservation enabled** - Prevents mass eviction during network glitches
- **Slower eviction** - 60-second intervals (more conservative)

## 🔌 Registering Services with Eureka

### 1. Add Dependencies

In service `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 2. Create bootstrap.yml

Create `src/main/resources/bootstrap.yml` in your service:

```yaml
spring:
  application:
    name: user-service  # Change per service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
```

### 3. Enable Eureka Client (Optional)

Spring Boot auto-configures Eureka client. Optionally add:

```java
@SpringBootApplication
@EnableDiscoveryClient  // Optional - auto-enabled with dependency
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### 4. Start Service

```bash
cd user-service
mvn spring-boot:run
```

Check dashboard - service should appear in ~30 seconds.

## 🔍 Service Discovery in Action

### Calling Another Service

Instead of hardcoded URLs:

```java
// ❌ Bad - hardcoded URL
String url = "http://localhost:8082/workbooks";
```

Use service name with load balancer:

```java
// ✅ Good - dynamic discovery
@Autowired
private RestTemplate restTemplate;

@LoadBalanced
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

// Use service name instead of host:port
String url = "http://sheet-service/workbooks";
ResponseEntity<Workbook> response = restTemplate.getForEntity(url, Workbook.class);
```

Spring Cloud automatically:
1. Resolves `sheet-service` to actual instances via Eureka
2. Load-balances if multiple instances exist
3. Fails over to another instance if one is down

## 📈 Health Checks

Eureka uses heartbeats to track service health:

### Heartbeat Interval
Services send heartbeat every **10 seconds** (default 30s, we set 10s for faster detection)

### Lease Expiration
If no heartbeat received within **30 seconds**, instance is marked as DOWN

### Eviction
DOWN instances are removed from registry after **eviction interval** (5s dev, 60s prod)

## 🛡️ Self-Preservation Mode

### What is it?
A safety mechanism that prevents Eureka from removing all services during network issues.

### When does it activate?
When < 85% of services send heartbeats in the last minute.

### Development (Disabled)
```yaml
enable-self-preservation: false
```
- Faster eviction for testing
- Services removed immediately when stopped

### Production (Enabled)
```yaml
enable-self-preservation: true
```
- Protects against false positives
- Shows warning: "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT"

## 🔄 High Availability Setup

For production, run multiple Eureka servers:

### Peer-to-Peer Replication

**Eureka Server 1:**
```yaml
eureka:
  instance:
    hostname: eureka1
  client:
    service-url:
      defaultZone: http://eureka2:8761/eureka/,http://eureka3:8761/eureka/
```

**Eureka Server 2:**
```yaml
eureka:
  instance:
    hostname: eureka2
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka3:8761/eureka/
```

### Client Configuration
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka2:8761/eureka/,http://eureka3:8761/eureka/
```

Clients register with all servers for redundancy.

## 🐛 Troubleshooting

### Service not appearing in Eureka

**Problem:** Service starts but doesn't show in dashboard

**Solutions:**
1. Check service logs for Eureka registration errors
2. Verify `eureka.client.service-url.defaultZone` is correct
3. Ensure Eureka server is running before starting services
4. Wait 30 seconds (default registration delay)
5. Check network connectivity between service and Eureka

### Service shows as DOWN

**Problem:** Service registered but status is DOWN

**Solutions:**
1. Check service health endpoint: `http://localhost:8081/actuator/health`
2. Verify heartbeat is being sent (check logs)
3. Ensure lease configuration is correct
4. Check if service is actually healthy

### Service takes too long to register

**Problem:** Service takes 30+ seconds to appear

**Solution:** Reduce registration intervals:
```yaml
eureka:
  client:
    registry-fetch-interval-seconds: 5
    instance-info-replication-interval-seconds: 5
  instance:
    lease-renewal-interval-in-seconds: 5
```

### Self-preservation mode activated

**Problem:** Dashboard shows preservation mode warning

**Solutions:**
1. **Development:** Disable self-preservation
2. **Production:** Investigate why services aren't sending heartbeats
3. Check network issues
4. Verify services are healthy

### Stale instances not removed

**Problem:** Stopped services still shown as UP

**Solutions:**
1. Reduce eviction interval (dev only):
   ```yaml
   eureka:
     server:
       eviction-interval-timer-in-ms: 5000
   ```
2. Ensure self-preservation is disabled (dev only)
3. Manually deregister: DELETE to `/eureka/apps/{APP-NAME}/{INSTANCE-ID}`

## 📊 Monitoring & Metrics

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8761/actuator/health

# Metrics
curl http://localhost:8761/actuator/metrics

# Prometheus metrics
curl http://localhost:8761/actuator/prometheus
```

### Registry API

```bash
# Get all registered services
curl http://localhost:8761/eureka/apps

# Get specific service
curl http://localhost:8761/eureka/apps/USER-SERVICE

# Get specific instance
curl http://localhost:8761/eureka/apps/USER-SERVICE/user-service:instance-id
```

## 🎓 Best Practices

1. **Start Eureka first** - Before any services
2. **Use meaningful service names** - Lowercase with hyphens (user-service, not UserService)
3. **Enable actuator** - For health checks and monitoring
4. **Set proper timeouts** - Balance between fast failure detection and false positives
5. **Use HA setup in production** - Multiple Eureka servers
6. **Monitor Eureka** - Track registration/deregistration events
7. **Graceful shutdown** - Ensures proper deregistration
8. **Use prefer-ip-address** - When services run in containers
9. **Set instance-id** - Unique per instance for debugging
10. **Enable security in production** - Add Spring Security to Eureka dashboard

## 🔒 Security (Production)

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Configure:
```yaml
spring:
  security:
    user:
      name: admin
      password: ${EUREKA_PASSWORD}

eureka:
  client:
    service-url:
      defaultZone: http://admin:${EUREKA_PASSWORD}@localhost:8761/eureka/
```

## 📦 Docker Deployment

```dockerfile
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY target/eureka-server.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t collab-eureka-server .
docker run -p 8761:8761 collab-eureka-server
```

## 🧪 Testing Eureka

### Manual Test

1. Start Eureka Server
2. Start any service (e.g., user-service)
3. Check dashboard: http://localhost:8761
4. Stop the service
5. Wait 30-60 seconds
6. Verify service removed from dashboard

### Automated Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerTest {
    
    @LocalServerPort
    private int port;
    
    @Test
    void eurekaServerIsUp() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }
}
```

## 📚 Additional Resources

- [Netflix Eureka Wiki](https://github.com/Netflix/eureka/wiki)
- [Spring Cloud Netflix Documentation](https://spring.io/projects/spring-cloud-netflix)
- [Eureka 2.0 Architecture](https://github.com/Netflix/eureka/wiki/Eureka-2.0-Architecture-Overview)

---

**Part of the Real-Time Collaborative Spreadsheet Platform**
