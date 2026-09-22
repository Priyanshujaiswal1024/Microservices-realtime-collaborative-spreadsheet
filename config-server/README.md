# Config Server

Spring Cloud Config Server providing centralized configuration management for all microservices.

## 🎯 Features

- **Git-backed configuration** - Version-controlled config files
- **Local filesystem fallback** - Works without Git for development
- **Dynamic refresh** - Update configuration without restarts
- **Environment-specific configs** - dev, test, prod profiles
- **Encryption support** - Secure sensitive values

## 🚀 Quick Start

### 1. Setup Configuration Repository

**Linux/Mac:**
```bash
cd config-server
./setup-config-repo.sh
```

**Windows:**
```powershell
cd config-server
.\setup-config-repo.ps1
```

This creates a Git repository at `~/collab-spreadsheet-config` (or `%USERPROFILE%\collab-spreadsheet-config` on Windows) with all configuration files.

### 2. Start Config Server

```bash
mvn spring-boot:run
```

The server will start on port **8888**.

### 3. Verify Configuration

Test that configuration is being served:

```bash
# Get user-service configuration
curl http://localhost:8888/user-service/default

# Get sheet-service configuration
curl http://localhost:8888/sheet-service/default

# Get specific profile
curl http://localhost:8888/user-service/dev
```

## 📁 Configuration Structure

### Git Repository Layout

```
~/collab-spreadsheet-config/
├── application.yml          # Global config for all services
├── user-service.yml         # User service specific config
├── sheet-service.yml        # Sheet service specific config
├── collab-service.yml       # Collaboration service config
├── comment-service.yml      # Comment service config
├── notification-service.yml # Notification service config
├── audit-service.yml        # Audit service config
└── api-gateway.yml          # API Gateway config
```

### Profile-Specific Configuration

Create profile-specific files:
- `application-dev.yml` - Development environment
- `application-test.yml` - Test environment
- `application-prod.yml` - Production environment

## 🔧 Configuration Files

### application.yml (Global)
Common configuration for all services:
- Jackson serialization settings
- Kafka connection defaults
- Redis connection defaults
- Eureka client settings
- Management endpoints
- Resilience4j defaults

### Service-Specific Files

Each service has its own configuration file with:
- Server port
- Database connection
- Flyway migration settings
- Service-specific properties
- Kafka consumer/producer settings
- Redis database index

## 🔐 Security Best Practices

### 1. JWT Secret

**Never commit secrets to Git!**

Set via environment variable:
```bash
export JWT_SECRET="your-very-long-random-secret-at-least-256-bits"
```

Or use encrypted values:
```bash
# Install Spring Cloud CLI
spring encrypt mysecret --key myencryptionkey

# Use in config
jwt:
  secret: '{cipher}AQAEncryptedValue...'
```

### 2. Database Passwords

Use environment variables:
```yaml
datasource:
  password: ${DB_PASSWORD}
```

Set in environment:
```bash
export DB_PASSWORD=your-db-password
```

### 3. Git Repository Security

For production, use a private Git repository with SSH keys or access tokens:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: git@github.com:your-org/config-repo.git
          default-label: main
          private-key: |
            -----BEGIN RSA PRIVATE KEY-----
            ...
            -----END RSA PRIVATE KEY-----
```

## 🔄 Dynamic Configuration Refresh

Services can refresh configuration without restart:

1. Make changes to config files in Git repository
2. Commit and push changes
3. Trigger refresh on a service:

```bash
curl -X POST http://localhost:8081/actuator/refresh
```

Or use Spring Cloud Bus to refresh all services at once.

## 🌍 Environment-Specific Configuration

### Local Development
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Production
```bash
java -jar app.jar --spring.profiles.active=prod
```

### Multiple Profiles
```bash
--spring.profiles.active=prod,mysql,kafka
```

## 📊 Configuration Precedence

Config Server resolves properties in this order (highest to lowest priority):

1. `{service-name}-{profile}.yml` (e.g., `user-service-prod.yml`)
2. `{service-name}.yml` (e.g., `user-service.yml`)
3. `application-{profile}.yml` (e.g., `application-prod.yml`)
4. `application.yml`

## 🧪 Testing Configuration

### Test Config Server
```bash
# Check health
curl http://localhost:8888/actuator/health

# List all configurations
curl http://localhost:8888/actuator/env
```

### Test Service Configuration
```bash
# Get raw configuration
curl http://localhost:8888/user-service/default | jq

# Get specific property
curl http://localhost:8888/user-service/default | jq '.propertySources[0].source."server.port"'
```

## 🐛 Troubleshooting

### Config Server won't start

**Problem:** Git repository not found

**Solution:** Run setup script to create repository:
```bash
./setup-config-repo.sh  # Linux/Mac
.\setup-config-repo.ps1 # Windows
```

**Problem:** Port 8888 already in use

**Solution:** Change port in `config-server/src/main/resources/application.yml`:
```yaml
server:
  port: 8889
```

### Service can't fetch configuration

**Problem:** Connection refused

**Solution:** Ensure Config Server is running:
```bash
curl http://localhost:8888/actuator/health
```

**Problem:** Configuration not found

**Solution:** Check file naming matches service name:
- Service name in `application.yml`: `spring.application.name=user-service`
- Config file name: `user-service.yml`

### Configuration changes not reflected

**Problem:** Service using cached config

**Solution:**
1. Restart the service, OR
2. Call refresh endpoint: `curl -X POST http://localhost:8081/actuator/refresh`

## 📦 Directory Structure

```
config-server/
├── pom.xml
├── README.md
├── setup-config-repo.sh       # Unix setup script
├── setup-config-repo.ps1      # Windows setup script
├── src/
│   └── main/
│       ├── java/
│       │   └── com/collab/spreadsheet/config/
│       │       └── ConfigServerApplication.java
│       └── resources/
│           ├── application.yml
│           └── config/        # Default configs (fallback)
│               ├── application.yml
│               ├── user-service.yml
│               ├── sheet-service.yml
│               ├── collab-service.yml
│               ├── comment-service.yml
│               ├── notification-service.yml
│               ├── audit-service.yml
│               └── api-gateway.yml
```

## 🔗 Integration with Services

Services connect to Config Server via bootstrap configuration:

**bootstrap.yml:**
```yaml
spring:
  application:
    name: user-service
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000
```

## 📚 Additional Resources

- [Spring Cloud Config Documentation](https://spring.io/projects/spring-cloud-config)
- [Encryption and Decryption](https://cloud.spring.io/spring-cloud-config/reference/html/#_encryption_and_decryption)
- [Spring Cloud Bus](https://spring.io/projects/spring-cloud-bus)

## 🎓 Best Practices

1. **Never commit secrets** - Use environment variables or encryption
2. **Use profiles** - Separate dev/test/prod configurations
3. **Version control** - Keep config in Git for audit trail
4. **Document changes** - Use meaningful commit messages
5. **Test changes** - Verify config before deploying to production
6. **Use refresh scope** - Mark beans with `@RefreshScope` for dynamic updates
7. **Monitor config server** - Use actuator endpoints and alerts
8. **Backup configurations** - Git provides version history

---

**Part of the Real-Time Collaborative Spreadsheet Platform**
