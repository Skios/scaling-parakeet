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

    // Funzione di utilità per creare la lista degli --env-file
    fun addEnvFiles(command: MutableList<String>): MutableList<String> {
        command.add("--env-file")
        command.add(".env")

        if (isArm && file("arm.env").exists()) {
            command.add("--env-file")
            command.add("arm.env")
        }
        if (file("local.env").exists()) {
            command.add("--env-file")
            command.add("local.env")
        }
        return command
    }

    // Funzione per eseguire comando e aspettare - VERSIONE CORRETTA
    fun executeCommand(cmd: List<String>, description: String) {
        println("🔄 $description")
        println("Comando: ${cmd.joinToString(" ")}")

        val processBuilder = ProcessBuilder(cmd)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw GradleException("❌ Comando fallito con exit code: $exitCode")
        }
        println("✅ $description completato")
    }

    doFirst {
        println("📦 Avvio dell'ambiente di sviluppo Docker Compose...")

        // STEP 1: Esegui wildfly-init
        println("🚀 Avvio container wildfly-init per inizializzazione...")

        var command = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "./docker-compose.wildfly-init.yml"
        )
        addEnvFiles(command)
        command.addAll(listOf("run", "--rm", "wildfly-init"))

        executeCommand(command, "Inizializzazione WildFly")

        // Piccola pausa per assicurarsi che l'inizializzazione sia completata
        println("⏳ Attesa completamento inizializzazione...")
        Thread.sleep(2000)

        // STEP 2: Avvia i servizi principali
        println("🚀 Avvio degli altri servizi dell'environment...")

        command = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "./docker-compose.yml"
        )
        addEnvFiles(command)
        command.addAll(listOf("up", "--detach"))

        executeCommand(command, "Avvio servizi")
    }

    // Il comando principale del task (dummy, il lavoro è fatto in doFirst)
//    commandLine("echo", "Environment startup completed")
    doLast {
        println("🔎 Controllo stato container WildFly...")

        // Container health check configuration
        val maxAttempts = 30  // 5 minutes total (10 seconds * 30)
        val waitInterval = 10000L  // 10 seconds between checks

        // Function to check container health
        fun checkContainerHealth(containerName: String, healthCheck: () -> Boolean): Boolean {
            var attempts = 0
            while (attempts < maxAttempts) {
                if (healthCheck()) {
                    return true
                }
                println("Waiting for $containerName to be ready... (attempt ${attempts + 1}/$maxAttempts)")
                Thread.sleep(waitInterval)
                attempts++
            }
            return false
        }

        // Check WildFly health
        val wildflyHealthy = checkContainerHealth("WildFly") {
            try {
                val processBuilder = ProcessBuilder(
                    "docker", "ps",
                    "--filter", "name=dev-env-wildfly-1",
                    "--format", "{{.Status}}"
                )
                val process = processBuilder.start()

                val output = process.inputStream.bufferedReader().readText().trim()
                val exitCode = process.waitFor()

                exitCode == 0 && output.contains("Up") && !output.contains("unhealthy")
            } catch (e: Exception) {
                println("Errore nel controllo dello stato: ${e.message}")
                false
            }
        }

        if (!wildflyHealthy) {
            throw GradleException("WildFly container failed to start properly or is unhealthy")
        }

        println("🎉 WildFly environment started successfully")
        println("Management console available at: http://localhost:9990")
        println("Username: ${System.getenv("WILDFLY_ADMIN_USER") ?: "admin"}")
        println("Password: ${System.getenv("WILDFLY_ADMIN_PASSWORD") ?: "admin123"}")
    }
}