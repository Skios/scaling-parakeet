# Dev Toolkit

A modern development toolkit project built with Kotlin/Java using Gradle. This project provides a complete development environment with WildFly application server, PostgreSQL database, RabbitMQ message broker, and pgAdmin management tools, all orchestrated with Docker Compose.

## 🚀 Quick Start

### Prerequisites
- **Java JDK 8 or higher**
- **Gradle 7.x or higher** (Gradle wrapper is included)
- **Docker and Docker Compose**
- **Platform**: Linux/AMD64 (configurable via PLATFORM environment variable)

### Installation
```bash
# Clone the repository
git clone https://github.com/mrcid/dev-toolkit.git

# Navigate to the project directory
cd dev-toolkit

# Start the complete development environment
./gradlew :environments:bootEnvironment
```

## 📋 Available Gradle Tasks

### Root Project Tasks

| Task | Command | Description |
|------|---------|-------------|
| **enforceRootWrapper** | `./gradlew enforceRootWrapper` | Removes unnecessary Gradle wrapper files from subprojects to maintain a clean project structure |

### Environment Module Tasks

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

> 📖 **For detailed environment documentation, see [environments/README.md](environments/README.md)**

## 🏗️ Project Structure

```
dev-toolkit/
├── environments/                    # Environment-specific configurations
│   ├── README.md                   # Detailed environment documentation
│   ├── build.gradle.kts            # Environment module build configuration
│   └── src/main/docker/            # Docker configurations and scripts
├── build.gradle.kts                # Root project build configuration
├── settings.gradle.kts             # Project settings
└── README.md                       # This file
```

## 🔧 Environment Configuration

The development environment can be customized using environment files. For detailed configuration options, see [environments/README.md](environments/README.md).

### Quick Configuration
```bash
# Copy and customize the environment configuration
cp environments/src/main/docker/.env environments/src/main/docker/local.env

# Edit the local configuration
nano environments/src/main/docker/local.env
```

## 🐳 Available Services

The development environment includes:

- **WildFly Application Server** (Port 8080, Management 9990)
- **PostgreSQL Database** (Port 5432)
- **RabbitMQ Message Broker** (Port 5672, Management 15672)
- **pgAdmin Database Management** (Port 5050)

> 📖 **For detailed service documentation and configuration, see [environments/README.md](environments/README.md)**

## 🚀 Usage Examples

### Complete Environment Setup
```bash
# Start everything (downloads images, initializes, starts services)
./gradlew :environments:bootEnvironment
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

# Clean up Gradle wrapper files
./gradlew enforceRootWrapper
```

## 🔍 Troubleshooting

### Common Issues

1. **Script Access Error**: If you get "no such file or directory" errors, the script files may have Windows line endings. The system automatically fixes this.

2. **Docker Not Running**: Ensure Docker Desktop is running and accessible.

3. **Port Conflicts**: If ports are already in use, stop conflicting services or modify the port mappings.

> 📖 **For detailed troubleshooting guide, see [environments/README.md](environments/README.md)**

## 📝 Features

- **Multi-module project structure** with Gradle
- **Environment-specific configurations** with profile support
- **Docker-based development environment** with full orchestration
- **WildFly application server** with complete configuration support
- **PostgreSQL database** with persistent storage
- **RabbitMQ message broker** with management interface
- **Cross-platform support** (configurable via PLATFORM environment variable)
- **Health checks** and automatic service validation
- **Comprehensive logging** and debugging tools

## 📚 Documentation

- **[Main README](README.md)** - Project overview and quick start
- **[Environment Documentation](environments/README.md)** - Detailed environment setup and configuration
- **[Docker Compose Files](environments/src/main/docker/)** - Service configurations

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the MIT License.

## 📞 Contact

Project maintained by mrcid  
Project Link: [https://github.com/mrcid/dev-toolkit](https://github.com/mrcid/dev-toolkit)