# dev-toolkit

## Description
A modern development toolkit project built with Kotlin/Java using Gradle. This project is organized as a multi-module build system, designed to provide development environment utilities and tools.

## Environment Variables

| Variable | Description | Default Value | Required |
|----------|-------------|---------------|----------|
| **Platform Configuration** |
| `PLATFORM` | Docker platform to use | `linux/amd64` | No |
| **WildFly Configuration** |
| `WILDFLY_VERSION` | WildFly version to use | `latest` | No |
| `WILDFLY_MODE` | WildFly operation mode (domain/standalone) | `domain` | No |
| `WILDFLY_ADMIN_USER` | WildFly admin username | `admin` | No |
| `WILDFLY_ADMIN_PASSWORD` | WildFly admin password | `admin123` | No |
| `WILDFLY_DOMAIN_CONFIG` | Domain mode configuration file | `domain.xml` | No |
| `WILDFLY_HOST_CONFIG` | Domain mode host configuration file | `host.xml` | No |
| `WILDFLY_STANDALONE_CONFIG` | Standalone mode configuration file | `standalone.xml` | No |
| **PostgreSQL Configuration** |
| `POSTGRES_VERSION` | PostgreSQL version to use | `latest` | No |
| `POSTGRES_DB` | PostgreSQL database name | `appdb` | No |
| `POSTGRES_USER` | PostgreSQL username | `appuser` | No |
| `POSTGRES_PASSWORD` | PostgreSQL password | `apppass` | No |
| **RabbitMQ Configuration** |
| `RABBITMQ_VERSION` | RabbitMQ version to use | `latest` | No |
| **pgAdmin Configuration** |
| `PGADMIN_DEFAULT_EMAIL` | pgAdmin admin email | `admin@admin.com` | No |
| `PGADMIN_DEFAULT_PASSWORD` | pgAdmin admin password | `admin` | No |
| **Docker Compose Configuration** |
| `COMPOSE_PROFILES` | Active Docker Compose profiles | `wildfly,postgres,rabbitmq,pgadmin` | No |

### Environment Files Precedence
Environment variables are loaded in the following order (later files override earlier ones):
1. `.env` (base configuration)
2. `arm.env` (if running on ARM architecture)
3. `local.env` (if exists)

### Example Environment Files

#### .env (Base Configuration)
```bash
# Platform configuration
PLATFORM: linux/amd64

# WildFly Configuration
WILDFLY_VERSION: latest
WILDFLY_MODE: domain
WILDFLY_ADMIN_USER: admin
WILDFLY_ADMIN_PASSWORD: admin123
WILDFLY_DOMAIN_CONFIG: domain.xml
WILDFLY_HOST_CONFIG: host.xml
WILDFLY_STANDALONE_CONFIG: standalone.xml

# PostgreSQL Configuration
POSTGRES_VERSION: latest
POSTGRES_DB: appdb
POSTGRES_USER: appuser
POSTGRES_PASSWORD: apppass

# RabbitMQ Configuration
RABBITMQ_VERSION: latest

# pgAdmin Configuration
PGADMIN_DEFAULT_EMAIL: admin@admin.com
PGADMIN_DEFAULT_PASSWORD: admin

COMPOSE_PROFILES: wildfly,postgres,rabbitmq,pgadmin
```

#### arm.env (ARM-specific Configuration)
```bash
# Platform configuration
PLATFORM: linux/arm64

# WildFly Configuration
WILDFLY_VERSION: 26.1.3.Final-jdk11
```

#### local.env (Local Overrides)
```bash
# PostgreSQL Configuration
POSTGRES_DB: myapp
POSTGRES_USER: myuser
POSTGRES_PASSWORD: mypassword

# pgAdmin Configuration
PGADMIN_DEFAULT_EMAIL: me@example.com
PGADMIN_DEFAULT_PASSWORD: mypgadminpass
```

## Project Structure
```
.
├── environments/     # Environment-specific configurations and tools
│   └── src/main/docker/
│       ├── docker-compose.yml         # Main Docker Compose configuration
│       ├── docker-compose.wildfly.yml # WildFly service configuration
│       ├── docker-compose.postgres.yml# PostgreSQL service configuration
│       ├── docker-compose.rabbitmq.yml# RabbitMQ service configuration
│       ├── docker-compose.pgadmin.yml # pgAdmin service configuration
│       ├── wildfly/                   # WildFly specific configurations
│       │   ├── domain/               # Domain mode configurations
│       │   │   └── configuration/    # domain.xml, host.xml, etc.
│       │   └── standalone/          # Standalone mode configurations
│       │       └── configuration/    # standalone.xml, etc.
│       ├── postgres/                  # PostgreSQL specific configurations
│       └── pgadmin/                   # pgAdmin specific configurations
│           └── data/                  # pgAdmin persistent data
├── gradle/          # Gradle wrapper and build utilities
└── build.gradle.kts # Main build configuration
```

## Requirements
- Java JDK 8 or higher
- Gradle 7.x or higher (Gradle wrapper is included)
- Docker and Docker Compose
- Platform: Linux/AMD64 (configurable via PLATFORM environment variable)

## Installation
```bash
# Clone the repository
git clone https://github.com/mrcid/dev-toolkit.git

# Navigate to the project directory
cd dev-toolkit

# Build the project using the Gradle wrapper
./gradlew build
```

## Available Tasks

### Root Project Tasks
- **enforceRootWrapper**: Removes unnecessary Gradle wrapper files from subprojects to maintain a clean project structure.

### Environment Module Tasks
- **startEnvironment**: Launches the development environment using Docker Compose. This task:
  - Sets up the working directory for Docker operations
  - Checks for environment-specific configurations (local.env)
  - Starts the Docker containers in detached mode
  - Configures WildFly with custom admin user and password

### Available Services
The development environment includes the following services:

1. **WildFly Application Server**
   - HTTP Port: 8080
   - Management Port: 9990
   - Debug Port: 8787
   - Server Group Port: 8788
   - Configuration:
     - Mode: Domain or Standalone (configurable via WILDFLY_MODE)
     - Custom domain.xml and host.xml support
     - Custom standalone.xml support
     - Custom modules support
   - Features:
     - Full Java EE support
     - Configurable admin user and password
     - Management console access
     - Debug support

2. **PostgreSQL Database**
   - Custom configuration support
   - Persistent storage
   - Accessible via pgAdmin

3. **RabbitMQ Message Broker**
   - Message queuing support
   - AMQP protocol

4. **pgAdmin Database Management Tool**
   - Web Interface Port: 5050
   - Features:
     - Graphical PostgreSQL database management
     - Query tool
     - Server management
     - User management
   - Default credentials:
     - Email: admin@admin.com
     - Password: admin

## Usage
The project uses Gradle for build automation. Common commands:

Build the project: `./gradlew build`

Clean build files: `./gradlew clean`

Run tests: `./gradlew test`

Start development environment: `./gradlew :environments:startEnvironment`

## Environment Configuration
The environment can be customized using the following files:
- `local.env`: Local environment variables override
- `arm.env`: ARM-specific configurations
- `.env`: Default environment variables

### WildFly Configuration
You can customize WildFly by setting the following environment variables in your `.env` or `local.env` file:

```bash
# WildFly Configuration
WILDFLY_VERSION: latest
WILDFLY_MODE: domain          # or 'standalone'
WILDFLY_ADMIN_USER: admin
WILDFLY_ADMIN_PASSWORD: admin123
WILDFLY_DOMAIN_CONFIG: domain.xml
WILDFLY_HOST_CONFIG: host.xml
WILDFLY_STANDALONE_CONFIG: standalone.xml
```

### pgAdmin Configuration
You can customize pgAdmin by setting the following environment variables:

```bash
# pgAdmin Configuration
PGADMIN_DEFAULT_EMAIL: admin@admin.com
PGADMIN_DEFAULT_PASSWORD: admin
```

To connect to PostgreSQL from pgAdmin:
1. Open http://localhost:5050 in your browser
2. Log in with the credentials from your .env file
3. Add a new server with the following details:
   - Host: postgres
   - Port: 5432
   - Database: ${POSTGRES_DB}
   - Username: ${POSTGRES_USER}
   - Password: ${POSTGRES_PASSWORD}

## Features
- Multi-module project structure
- Environment-specific configurations
- Gradle-based build system
- Maven Central repository integration
- Docker-based development environment setup
- WildFly application server integration with full configuration support
- PostgreSQL database support with pgAdmin management
- RabbitMQ message broker integration
- Cross-platform support (configurable via PLATFORM environment variable)

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
This project is open source and available under the MIT License.

## Contact
Project maintained by mrcid
Project Link: [https://github.com/mrcid/dev-toolkit](https://github.com/mrcid/dev-toolkit)

### Docker Compose Profiles
Docker Compose profiles allow you to selectively enable or disable services in your development environment. This is controlled by the `COMPOSE_PROFILES` environment variable.

#### Available Profiles
| Profile | Description | Service |
|---------|-------------|---------|
| `wildfly` | Enables WildFly application server | WildFly |
| `postgres` | Enables PostgreSQL database | PostgreSQL |
| `rabbitmq` | Enables RabbitMQ message broker | RabbitMQ |
| `pgadmin` | Enables pgAdmin database management | pgAdmin |

#### Profile Usage Examples

1. **Default Configuration** (All services enabled)
```bash
COMPOSE_PROFILES: wildfly,postgres,rabbitmq,pgadmin
```

2. **Backend Only** (WildFly and PostgreSQL)
```bash
COMPOSE_PROFILES: wildfly,postgres
```

3. **Database Only** (PostgreSQL and pgAdmin)
```bash
COMPOSE_PROFILES: postgres,pgadmin
```

4. **Message Broker Only**
```bash
COMPOSE_PROFILES: rabbitmq
```

#### Profile Configuration Tips
- Profiles can be combined by separating them with commas
- Services not included in active profiles will not be started
- The order of profiles doesn't matter
- You can override profiles in your `local.env` file
- Each service's configuration is still loaded from its respective `.env` file

Example `local.env` with custom profiles:
```bash
# Enable only WildFly and PostgreSQL
COMPOSE_PROFILES: wildfly,postgres

# Customize the enabled services
WILDFLY_VERSION: 26.1.3.Final
POSTGRES_DB: myapp
```