# Environment Module Documentation

This module provides a complete development environment with Docker Compose orchestration, including WildFly application server, PostgreSQL database, RabbitMQ message broker, and pgAdmin management tools.

> 📖 **Back to [Main README](../README.md)**

## 🚀 Quick Start

### Prerequisites
- **Docker and Docker Compose** installed and running
- **Java JDK 8 or higher** (for Gradle tasks)
- **Gradle 7.x or higher** (Gradle wrapper is included)

### One-Command Setup
```bash
# From the project root directory
./gradlew :environments:bootEnvironment
```

This command will:
1. Download all required Docker images
2. Initialize WildFly configuration files
3. Start all services with health checks
4. Display service information and access URLs

## 📋 Available Gradle Tasks

| Task | Command | Description |
|------|---------|-------------|
| **bootEnvironment** | `./gradlew :environments:bootEnvironment` | **Complete environment setup** - Downloads images, initializes WildFly, and starts all services |
| **startEnvironment** | `./gradlew :environments:startEnvironment` | Starts the development environment using Docker Compose |
| **stopEnvironment** | `./gradlew :environments:stopEnvironment` | Stops all Docker containers |
| **resetEnvironment** | `./gradlew :environments:resetEnvironment` | Stops and removes all containers and volumes (complete reset) |
| **statusEnvironment** | `./gradlew :environments:statusEnvironment` | Shows the status of all running containers |
| **logsEnvironment** | `./gradlew :environments:logsEnvironment` | Shows logs from all active containers |
| **initEnvironment** | `./gradlew :environments:initEnvironment` | Initializes WildFly configuration files |
| **downloadImages** | `./gradlew :environments:downloadImages` | Downloads all required Docker images |
| **debugEnvironment** | `./gradlew :environments:debugEnvironment` | Debug environment configuration and status |

## 🏗️ Module Structure

```
environments/
├── README.md                   # This file
├── build.gradle.kts            # Module build configuration
└── src/main/docker/
    ├── docker-compose.yml              # Main Docker Compose configuration
    ├── docker-compose.wildfly.yml      # WildFly service configuration
    ├── docker-compose.postgres.yml     # PostgreSQL service configuration
    ├── docker-compose.rabbitmq.yml     # RabbitMQ service configuration
    ├── docker-compose.wildfly-init.yml # WildFly initialization container
    ├── .env                            # Base environment configuration
    ├── arm.env                         # ARM-specific configuration
    ├── wildfly/                        # WildFly configurations
    │   ├── domain/configuration/       # Domain mode configs
    │   ├── standalone/configuration/   # Standalone mode configs
    │   ├── modules/                    # Custom modules
    │   └── scripts/init/               # Initialization scripts
    ├── data/                           # Persistent data directories
    │   ├── postgres/                   # PostgreSQL data
    │   ├── rabbitmq/                   # RabbitMQ data
    │   └── redis/                      # Redis data
    └── nginx/                          # Nginx configurations
```

## 🔧 Environment Configuration

### Environment Variables

| Variable | Description | Default Value | Required |
|----------|-------------|---------------|----------|
| **Platform Configuration** |
| `PLATFORM` | Docker platform to use | `linux/amd64` | No |
| **WildFly Configuration** |
| `WILDFLY_VERSION` | WildFly version to use | `26.1.3.Final-jdk11` | No |
| `WILDFLY_MODE` | WildFly operation mode | `domain` | No |
| `WILDFLY_ADMIN_USER` | WildFly admin username | `admin` | No |
| `WILDFLY_ADMIN_PASSWORD` | WildFly admin password | `password` | No |
| `WILDFLY_DOMAIN_CONFIG` | Domain configuration file | `domain.xml` | No |
| `WILDFLY_HOST_CONFIG` | Host configuration file | `host.xml` | No |
| `WILDFLY_STANDALONE_CONFIG` | Standalone configuration file | `standalone.xml` | No |
| **PostgreSQL Configuration** |
| `POSTGRES_VERSION` | PostgreSQL version | `latest` | No |
| `POSTGRES_DB` | Database name | `postgres` | No |
| `POSTGRES_USER` | Database username | `postgres` | No |
| `POSTGRES_PASSWORD` | Database password | `postgres` | No |
| **RabbitMQ Configuration** |
| `RABBITMQ_VERSION` | RabbitMQ version | `latest` | No |
| **Docker Compose Configuration** |
| `COMPOSE_PROFILES` | Active profiles | `wildfly,postgres,rabbitmq` | No |

### Environment Files Precedence
Environment variables are loaded in the following order (later files override earlier ones):
1. `.env` (base configuration)
2. `arm.env` (if running on ARM architecture)
3. `local.env` (if exists)

### Example Environment Files

#### .env (Base Configuration)
```bash
# Platform configuration
PLATFORM=linux/amd64

# WildFly Configuration
WILDFLY_VERSION=26.1.3.Final-jdk11
WILDFLY_MODE=domain
WILDFLY_ADMIN_USER=admin
WILDFLY_ADMIN_PASSWORD=password
WILDFLY_DOMAIN_CONFIG=domain.xml
WILDFLY_HOST_CONFIG=host.xml
WILDFLY_STANDALONE_CONFIG=standalone.xml

# PostgreSQL Configuration
POSTGRES_VERSION=latest
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# RabbitMQ Configuration
RABBITMQ_VERSION=latest

COMPOSE_PROFILES=wildfly,postgres,rabbitmq
```

#### arm.env (ARM-specific Configuration)
```bash
# Platform configuration
PLATFORM=linux/arm64

# WildFly Configuration
WILDFLY_VERSION=26.1.3.Final-jdk11
```

#### local.env (Local Overrides)
```bash
# PostgreSQL Configuration
POSTGRES_DB=myapp
POSTGRES_USER=myuser
POSTGRES_PASSWORD=mypassword

# Custom profiles
COMPOSE_PROFILES=wildfly,postgres
```

## 🐳 Available Services

### 1. WildFly Application Server
- **HTTP Port**: 8080
- **Management Port**: 9990
- **Debug Port**: 8787
- **Server Group Port**: 8788
- **Management Console**: http://localhost:9990
- **Features**:
  - Full Java EE support
  - Configurable admin user and password
  - Management console access
  - Debug support
  - Domain and Standalone modes
  - Custom configuration files support
  - Custom modules support

### 2. PostgreSQL Database
- **Port**: 5432
- **Features**:
  - Custom configuration support
  - Persistent storage
  - Accessible via pgAdmin
  - Configurable database name, user, and password

### 3. RabbitMQ Message Broker
- **Port**: 5672
- **Management Port**: 15672
- **Management Console**: http://localhost:15672
- **Features**:
  - Message queuing support
  - AMQP protocol
  - Web management interface
  - Configurable version

### 4. pgAdmin Database Management Tool
- **Port**: 5050
- **Web Interface**: http://localhost:5050
- **Features**:
  - Graphical PostgreSQL database management
  - Query tool
  - Server management
  - User management

## 🎯 Docker Compose Profiles

Docker Compose profiles allow you to selectively enable or disable services. Control this with the `COMPOSE_PROFILES` environment variable.

### Available Profiles
| Profile | Description | Service |
|---------|-------------|---------|
| `wildfly` | WildFly application server | WildFly |
| `postgres` | PostgreSQL database | PostgreSQL |
| `rabbitmq` | RabbitMQ message broker | RabbitMQ |
| `pgadmin` | pgAdmin database management | pgAdmin |

### Profile Usage Examples

1. **Default Configuration** (All services)
```bash
COMPOSE_PROFILES=wildfly,postgres,rabbitmq
```

2. **Backend Only** (WildFly and PostgreSQL)
```bash
COMPOSE_PROFILES=wildfly,postgres
```

3. **Database Only** (PostgreSQL)
```bash
COMPOSE_PROFILES=postgres
```

4. **Message Broker Only**
```bash
COMPOSE_PROFILES=rabbitmq
```

5. **Full Stack with pgAdmin**
```bash
COMPOSE_PROFILES=wildfly,postgres,rabbitmq,pgadmin
```

## 🚀 Usage Examples

### Complete Environment Setup
```bash
# Start everything (downloads images, initializes, starts services)
./gradlew :environments:bootEnvironment
```

### Step-by-Step Setup
```bash
# 1. Download Docker images
./gradlew :environments:downloadImages

# 2. Initialize WildFly (if needed)
./gradlew :environments:initEnvironment

# 3. Start services
./gradlew :environments:startEnvironment
```

### Environment Management
```bash
# Check status
./gradlew :environments:statusEnvironment

# View logs
./gradlew :environments:logsEnvironment

# Stop services
./gradlew :environments:stopEnvironment

# Complete reset (removes all data)
./gradlew :environments:resetEnvironment
```

### Troubleshooting
```bash
# Debug environment configuration
./gradlew :environments:debugEnvironment
```

## 🔍 Troubleshooting

### Common Issues

1. **Script Access Error**: If you get "no such file or directory" errors, the script files may have Windows line endings. The system automatically fixes this, but you can manually convert line endings if needed.

2. **Docker Not Running**: Ensure Docker Desktop is running and accessible.

3. **Port Conflicts**: If ports are already in use, stop conflicting services or modify the port mappings in the Docker Compose files.

4. **Permission Issues**: On Linux/macOS, ensure Docker has proper permissions to access the project directory.

5. **WildFly Initialization Failures**: If WildFly initialization fails, check:
   - Docker has enough memory allocated
   - No conflicting containers are running
   - The script files have proper Unix line endings

### Debug Commands
```bash
# Check environment configuration
./gradlew :environments:debugEnvironment

# Check Docker status
docker --version
docker compose version

# Check running containers
docker ps -a

# Check container logs
docker logs dev-env-wildfly-1
docker logs dev-env-postgres-1
docker logs dev-env-rabbitmq-1
```

### Health Checks

The system performs automatic health checks for:
- **WildFly**: Checks if the application server is responding on port 8080
- **PostgreSQL**: Checks if the database is accepting connections
- **RabbitMQ**: Checks if the message broker is operational

If health checks fail, the system will:
1. Display warning messages
2. Continue with startup (for non-critical services)
3. Provide debugging information

## 🔧 Advanced Configuration

### Custom WildFly Configuration

You can customize WildFly by:

1. **Modifying environment variables** in your `.env` or `local.env` file
2. **Adding custom configuration files** to the `wildfly/` directory
3. **Adding custom modules** to the `wildfly/modules/` directory

### Custom PostgreSQL Configuration

1. **Modify database settings** via environment variables
2. **Add custom initialization scripts** to the `data/postgres/` directory
3. **Customize PostgreSQL configuration** by modifying the Docker Compose file

### Custom RabbitMQ Configuration

1. **Modify RabbitMQ settings** via environment variables
2. **Add custom plugins** by extending the Docker image
3. **Configure exchanges and queues** via the management interface

## 📝 Features

- **Multi-service orchestration** with Docker Compose
- **Profile-based service selection** for flexible configurations
- **Automatic health checks** and service validation
- **Persistent data storage** for all services
- **Cross-platform support** (AMD64/ARM64)
- **Comprehensive logging** and debugging tools
- **Easy environment management** with Gradle tasks
- **Custom configuration support** for all services

## 🔗 Related Documentation

- **[Main README](../README.md)** - Project overview and quick start
- **[Docker Compose Files](src/main/docker/)** - Service configurations
- **[WildFly Documentation](https://docs.wildfly.org/)** - Official WildFly documentation
- **[PostgreSQL Documentation](https://www.postgresql.org/docs/)** - Official PostgreSQL documentation
- **[RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)** - Official RabbitMQ documentation 