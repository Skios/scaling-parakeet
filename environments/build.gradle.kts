plugins {
    id("base")
}

tasks.register<Exec>("startEnvironment") {
    group = "environment"
    description = "Launch the development environment via docker compose"

    //Setup WorkingDir
    workingDir("./src/main/docker")

    // Detect architecture
    val isArm = System.getProperty("os.arch").contains("aarch64") || 
                System.getProperty("os.arch").contains("arm")

    // Build command with environment files in order
    val command = mutableListOf(
        "docker", "compose",
        "--project-name=dev-env",
        "-f", "./docker-compose.yml"
    )

    // Add .env file (base configuration)
    command.add("--env-file")
    command.add(".env")

    // Add arm.env if running on ARM architecture
    if (isArm && file("arm.env").exists()) {
        command.add("--env-file")
        command.add("arm.env")
    }

    // Add local.env if it exists
    if (file("local.env").exists()) {
        command.add("--env-file")
        command.add("local.env")
    }

    // Add up command
    command.add("up")
    command.add("--detach")

    // Execute command
    commandLine(command)

    doLast {
        // Wait for WildFly to start
        Thread.sleep(15000)
        
        // Check if WildFly is running
        val wildflyStatus = exec {
            commandLine("docker", "ps", "--filter", "name=dev-env-wildfly-1", "--format", "{{.Status}}")
        }
        
        if (!wildflyStatus.toString().contains("Up")) {
            throw GradleException("WildFly container failed to start properly")
        }
        
        println("WildFly environment started successfully")
        println("Management console available at: http://localhost:9990")
        println("Username: ${System.getenv("WILDFLY_ADMIN_USER") ?: "admin"}")
        println("Password: ${System.getenv("WILDFLY_ADMIN_PASSWORD") ?: "admin123"}")
        
        // Print pgAdmin information if available
        if (file("local.env").exists() || (isArm && file("arm.env").exists())) {
            println("\npgAdmin available at: http://localhost:5050")
            println("Email: ${System.getenv("PGADMIN_DEFAULT_EMAIL") ?: "admin@admin.com"}")
            println("Password: ${System.getenv("PGADMIN_DEFAULT_PASSWORD") ?: "admin"}")
        }
    }
}