plugins {
    id("base")
}

// Funzione per eseguire comando e aspettare - VERSIONE CORRETTA
fun executeCommand(cmd: List<String>, workingDir: File, description: String) {
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

// Funzione per eseguire comando con timeout - VERSIONE CON TIMEOUT
fun executeCommandWithTimeout(cmd: List<String>, workingDir: File, description: String, timeoutMinutes: Long = 2) {
    println("🔄 $description")
    println("Comando: ${cmd.joinToString(" ")}")

    val processBuilder = ProcessBuilder(cmd)
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)

    val process = processBuilder.start()

    // Aspetta con timeout
    val completed = process.waitFor(timeoutMinutes, java.util.concurrent.TimeUnit.MINUTES)

    if (!completed) {
        println("⏰ Timeout di $timeoutMinutes minuti raggiunto, terminazione forzata del processo...")
        process.destroyForcibly()
        throw GradleException("❌ Comando terminato per timeout dopo $timeoutMinutes minuti")
    }

    val exitCode = process.exitValue()
    if (exitCode != 0) {
        throw GradleException("❌ Comando fallito con exit code: $exitCode")
    }
    println("✅ $description completato")
}

// Funzione di utilità per creare la lista degli --env-file
fun addEnvFiles(command: MutableList<String>): MutableList<String> {
    val isArm = System.getProperty("os.arch").contains("aarch64") ||
            System.getProperty("os.arch").contains("arm")

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

// Funzione per leggere i profili dal file .env
fun getActiveProfiles(): List<String> {
    val envFile = file("./src/main/docker/.env")
    if (!envFile.exists()) {
        println("⚠️  File .env non trovato, uso profilo 'dev' di default")
        return listOf("dev")
    }

    val envContent = envFile.readText()
    val profilesLine = envContent.lines()
        .find { it.startsWith("COMPOSE_PROFILES=") && !it.startsWith("#") }

    return if (profilesLine != null) {
        val profiles = profilesLine.substringAfter("=").trim()
        profiles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    } else {
        println("⚠️  COMPOSE_PROFILES non trovato in .env, uso profilo 'dev' di default")
        listOf("dev")
    }
}

// Funzione per assicurarsi che le directory esistano
fun ensureDirectoriesExist(workingDir: File) {
    val directories = listOf(
        "wildfly/domain/configuration",
        "wildfly/standalone/configuration",
        "wildfly/modules",
        "data/postgres",
        "data/rabbitmq",
        "data/redis",
        "nginx/conf.d",
        "nginx/ssl"
    )

    directories.forEach { dir ->
        val dirFile = workingDir.resolve(dir)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
            println("   ✅ Created: $dir")
        }
    }
}

// Funzione per controllare la salute dei container
fun checkContainerHealth(containerName: String, filterName: String): Boolean {
    val maxAttempts = 30  // 5 minuti totali (10 secondi * 30)
    val waitInterval = 10000L  // 10 secondi tra i controlli

    var attempts = 0
    while (attempts < maxAttempts) {
        try {
            val processBuilder = ProcessBuilder(
                "docker", "ps",
                "--filter", "name=$filterName",
                "--format", "{{.Status}}"
            )
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0 && output.contains("Up") && !output.contains("unhealthy")) {
                println("✅ $containerName is healthy")
                return true
            }
        } catch (e: Exception) {
            println("Errore nel controllo dello stato di $containerName: ${e.message}")
        }

        println("Waiting for $containerName to be ready... (attempt ${attempts + 1}/$maxAttempts)")
        Thread.sleep(waitInterval)
        attempts++
    }
    return false
}

// Funzione per stampare informazioni sui servizi
fun printServiceInfo(profiles: List<String>) {
    println("\n📋 Services Information:")
    println("=".repeat(50))

    if (profiles.any { it in listOf("wildfly", "dev", "full") }) {
        println("🔥 WildFly:")
        println("   - Application: http://localhost:8080")
        println("   - Management: http://localhost:9990")
        println("   - Debug Port: 8787")
        println("   - Username: ${System.getenv("WILDFLY_ADMIN_USER") ?: "admin"}")
        println("   - Password: ${System.getenv("WILDFLY_ADMIN_PASSWORD") ?: "admin123"}")
    }

    if (profiles.any { it in listOf("database", "postgres", "dev", "full", "wildfly") }) {
        println("🐘 PostgreSQL:")
        println("   - Host: localhost:5432")
        println("   - Database: ${System.getenv("POSTGRES_DB") ?: "appdb"}")
        println("   - Username: ${System.getenv("POSTGRES_USER") ?: "dbuser"}")
        println("   - Password: ${System.getenv("POSTGRES_PASSWORD") ?: "dbpass123"}")
    }

    if (profiles.any { it in listOf("messaging", "rabbitmq", "dev", "full") }) {
        println("🐰 RabbitMQ:")
        println("   - AMQP: localhost:5672")
        println("   - Management: http://localhost:15672")
        println("   - Username: ${System.getenv("RABBITMQ_DEFAULT_USER") ?: "admin"}")
        println("   - Password: ${System.getenv("RABBITMQ_DEFAULT_PASS") ?: "rabbitmq123"}")
    }

    println("=".repeat(50))
}

tasks.register<Exec>("downloadImages") {
    group = "environment"
    description = "Scarica le immagini Docker necessarie"

    workingDir = file("./src/main/docker")

    // Comando dummy
    commandLine("docker", "version")

    doFirst {
        val activeProfiles = getActiveProfiles()

        println("📦 Download immagini Docker per profili: ${activeProfiles.joinToString(", ")}")

        var command = mutableListOf("docker", "compose", "-f", "docker-compose.yml")

        // Aggiungi i profili al comando
        activeProfiles.forEach { profile ->
            command.addAll(listOf("--profile", profile))
        }

        addEnvFiles(command)
        command.add("pull")

        executeCommand(command, workingDir, "Download immagini Docker")
    }
}

tasks.register<Exec>("initEnvironment") {
    group = "environment"
    description = "Esegue il container wildfly-init per setup iniziale"

    workingDir = file("./src/main/docker")

    // Comando dummy che viene eseguito dopo doFirst
    commandLine("docker", "version")

    doFirst {
        val activeProfiles = getActiveProfiles()

        // Verifica se è necessario il wildfly-init
        val needsWildflyInit = activeProfiles.any {
            it in listOf("wildfly", "dev", "full")
        }

        if (!needsWildflyInit) {
            println("⚠️  Nessun profilo richiede WildFly init. Profili attivi: ${activeProfiles.joinToString(", ")}")
            return@doFirst
        }

        println("📁 Verificando e creando directory necessarie...")
        ensureDirectoriesExist(workingDir)

        // Controlla se i file di configurazione esistono già
        val wildflyStandaloneConfig = workingDir.resolve("wildfly/standalone/configuration")
        val wildflyDomainConfig = workingDir.resolve("wildfly/domain/configuration")

        if (wildflyStandaloneConfig.exists() && wildflyStandaloneConfig.listFiles()?.isNotEmpty() == true &&
            wildflyDomainConfig.exists() && wildflyDomainConfig.listFiles()?.isNotEmpty() == true) {
            println("✅ File di configurazione WildFly già presenti, skip inizializzazione")
            return@doFirst
        }

        println("🚀 Avvio container wildfly-init per inizializzazione...")

        var command = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env-init", // Nome progetto diverso per evitare conflitti
            "-f", "./docker-compose.wildfly-init.yml"
        )
        addEnvFiles(command)
        command.addAll(listOf("run", "--rm", "wildfly-init"))

        try {
            // Usa la funzione con timeout per evitare che si blocchi
            executeCommandWithTimeout(command, workingDir, "Inizializzazione WildFly", 5)
            println("✅ Inizializzazione WildFly completata con successo")
        } catch (e: Exception) {
            println("❌ Errore durante l'inizializzazione: ${e.message}")

            // Cleanup in caso di errore
            println("🧹 Pulizia container rimasti...")
            try {
                val cleanupCommand = listOf(
                    "docker", "compose",
                    "--project-name=dev-env-init",
                    "-f", "./docker-compose.wildfly-init.yml",
                    "down", "--remove-orphans", "--timeout", "10"
                )
                executeCommandWithTimeout(cleanupCommand, workingDir, "Cleanup container init", 2)
            } catch (cleanupError: Exception) {
                println("⚠️  Errore durante cleanup: ${cleanupError.message}")
            }

            throw e
        }

        // Verifica che i file siano stati effettivamente copiati
        println("🔍 Verifica file copiati...")
        val expectedFiles = listOf(
            "wildfly/standalone/configuration/standalone.xml",
            "wildfly/domain/configuration/domain.xml"
        )

        var allFilesPresent = true
        expectedFiles.forEach { filePath ->
            val file = workingDir.resolve(filePath)
            if (file.exists()) {
                println("   ✅ $filePath")
            } else {
                println("   ❌ $filePath - MANCANTE")
                allFilesPresent = false
            }
        }

        if (!allFilesPresent) {
            throw GradleException("❌ Alcuni file di configurazione non sono stati copiati correttamente")
        }

        println("✅ Tutti i file di configurazione sono presenti")
    }
}

tasks.register<Exec>("startEnvironment") {
    group = "environment"
    description = "Avvia i servizi Docker in background con profili"

    workingDir = file("./src/main/docker")

    // Comando dummy che viene eseguito dopo doFirst
    commandLine("docker", "version")

    doFirst {
        val activeProfiles = getActiveProfiles()

        println("🚀 Avvio dei servizi dell'environment con profili: ${activeProfiles.joinToString(", ")}")

        var command = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "./docker-compose.yml"
        )

        // Aggiungi i profili al comando
        activeProfiles.forEach { profile ->
            command.addAll(listOf("--profile", profile))
        }

        addEnvFiles(command)
        command.addAll(listOf("up", "--detach"))

        executeCommand(command, workingDir, "Avvio servizi con profili")
    }

    doLast {
        val activeProfiles = getActiveProfiles()

        // Health checks
        println("🔎 Controllo stato dei container...")

        if (activeProfiles.any { it in listOf("wildfly", "dev", "full") }) {
            if (!checkContainerHealth("WildFly", "dev-env-wildfly-1")) {
                throw GradleException("WildFly container failed to start properly or is unhealthy")
            }
        }

        if (activeProfiles.any { it in listOf("database", "postgres", "dev", "full", "wildfly") }) {
            if (!checkContainerHealth("PostgreSQL", "dev-env-postgres-1")) {
                println("⚠️  PostgreSQL potrebbe non essere completamente pronto")
            }
        }

        if (activeProfiles.any { it in listOf("messaging", "rabbitmq", "dev", "full") }) {
            if (!checkContainerHealth("RabbitMQ", "dev-env-rabbitmq-1")) {
                println("⚠️  RabbitMQ potrebbe non essere completamente pronto")
            }
        }

        println("🎉 Environment started successfully with profiles: ${activeProfiles.joinToString(", ")}")
        printServiceInfo(activeProfiles)
    }
}

tasks.register<Exec>("stopEnvironment") {
    group = "environment"
    description = "Ferma tutti i container dell'environment"

    workingDir = file("./src/main/docker")

    // Comando dummy
    commandLine("docker", "version")

    doFirst {
        println("🛑 Fermando l'environment...")
        val command = listOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "docker-compose.yml",
            "down",
            "--remove-orphans"
        )

        executeCommand(command, workingDir, "Stop environment")
    }
}

tasks.register<Exec>("resetEnvironment") {
    group = "environment"
    description = "Ferma e rimuove tutti i container e volumes"

    workingDir = file("./src/main/docker")

    // Comando dummy
    commandLine("docker", "version")

    doFirst {
        println("🛑 Arresto completo ambiente Docker...")
        val downCmd = listOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "docker-compose.yml",
            "down", "--volumes", "--remove-orphans"
        )
        executeCommand(downCmd, workingDir, "Arresto environment")

        println("🧹 Pulizia cartelle locali...")

        // Rimuovere configurazioni WildFly
        val dirsToDelete = listOf(
            workingDir.resolve("wildfly/domain"),
            workingDir.resolve("wildfly/standalone"),
            workingDir.resolve("wildfly/modules"),
            workingDir.resolve("data")
        )

        dirsToDelete.forEach { dir ->
            if (dir.exists()) {
                println("🔸 Cancellazione: ${dir.absolutePath}")
                dir.deleteRecursively()
            } else {
                println("⚠️ Cartella non trovata: ${dir.absolutePath}")
            }
        }

        println("✅ Reset completo dell'ambiente.")
    }
}

tasks.register<Exec>("statusEnvironment") {
    group = "environment"
    description = "Mostra lo status di tutti i container"

    workingDir = file("./src/main/docker")

    // Comando dummy
    commandLine("docker", "version")

    doFirst {
        val activeProfiles = getActiveProfiles()

        println("📊 Status Environment - Profili attivi: ${activeProfiles.joinToString(", ")}")

        val command = listOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "docker-compose.yml",
            "ps"
        )

        executeCommand(command, workingDir, "Status containers")
    }
}

tasks.register("logsEnvironment") {
    group = "environment"
    description = "Mostra i logs dei container attivi"

    doLast {
        val workingDir = file("./src/main/docker")

        println("📜 Mostra logs dei container...")
        val command = listOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "docker-compose.yml",
            "logs", "--tail=100", "--follow"
        )

        // Per i logs, usiamo ProcessBuilder direttamente per mantenere l'interattività
        ProcessBuilder(command)
            .directory(workingDir)
            .inheritIO()
            .start()
            .waitFor()
    }
}

tasks.register<Exec>("bootEnvironment") {
    group = "environment"
    description = "Esegue init completo dell'ambiente (download, init, avvio)"

    workingDir = file("./src/main/docker")

    // Comando dummy
    commandLine("docker", "version")

    doFirst {
        val activeProfiles = getActiveProfiles()
        println("🚀 Boot completo environment con profili: ${activeProfiles.joinToString(", ")}")

        // Esegui downloadImages
        println("\n=== STEP 1: Download Images ===")
        var command = mutableListOf("docker", "compose", "-f", "docker-compose.yml")
        activeProfiles.forEach { profile ->
            command.addAll(listOf("--profile", profile))
        }
        addEnvFiles(command)
        command.add("pull")
        executeCommand(command, workingDir, "Download immagini Docker")

        // Esegui initEnvironment se necessario
        val needsWildflyInit = activeProfiles.any {
            it in listOf("wildfly", "dev", "full")
        }

        if (needsWildflyInit) {
            println("\n=== STEP 2: Initialize WildFly ===")
            println("📁 Verificando e creando directory necessarie...")
            ensureDirectoriesExist(workingDir)

            println("🚀 Avvio container wildfly-init per inizializzazione...")
            command = mutableListOf(
                "docker", "compose",
                "--project-name=dev-env",
                "-f", "./docker-compose.wildfly-init.yml"
            )
            addEnvFiles(command)
            command.addAll(listOf("run", "--rm", "wildfly-init"))
            executeCommand(command, workingDir, "Inizializzazione WildFly")

            println("⏳ Attesa completamento inizializzazione...")
            Thread.sleep(2000)
        }

        // Esegui startEnvironment
        println("\n=== STEP 3: Start Services ===")
        println("🚀 Avvio dei servizi dell'environment con profili: ${activeProfiles.joinToString(", ")}")
        command = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "./docker-compose.yml"
        )
        activeProfiles.forEach { profile ->
            command.addAll(listOf("--profile", profile))
        }
        addEnvFiles(command)
        command.addAll(listOf("up", "--detach"))
        executeCommand(command, workingDir, "Avvio servizi con profili")
    }

    doLast {
        val activeProfiles = getActiveProfiles()

        // Health checks
        println("\n=== STEP 4: Health Checks ===")
        println("🔎 Controllo stato dei container...")

        if (activeProfiles.any { it in listOf("wildfly", "dev", "full") }) {
            if (!checkContainerHealth("WildFly", "dev-env-wildfly-1")) {
                throw GradleException("WildFly container failed to start properly or is unhealthy")
            }
        }

        if (activeProfiles.any { it in listOf("database", "postgres", "dev", "full", "wildfly") }) {
            if (!checkContainerHealth("PostgreSQL", "dev-env-postgres-1")) {
                println("⚠️  PostgreSQL potrebbe non essere completamente pronto")
            }
        }

        if (activeProfiles.any { it in listOf("messaging", "rabbitmq", "dev", "full") }) {
            if (!checkContainerHealth("RabbitMQ", "dev-env-rabbitmq-1")) {
                println("⚠️  RabbitMQ potrebbe non essere completamente pronto")
            }
        }

        println("🎉 Boot environment completato con successo!")
        printServiceInfo(activeProfiles)
    }
}

// Mantieni il task originale per compatibilità
tasks.register<Exec>("old_startEnvironment") {
    group = "environment"
    description = "Launch the development environment via docker compose (versione originale)"

    workingDir("./src/main/docker")

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

        executeCommand(command, workingDir, "Inizializzazione WildFly")

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

        executeCommand(command, workingDir, "Avvio servizi")
    }

    commandLine("docker", "version")  // Comando dummy

    doLast {
        if (!checkContainerHealth("WildFly", "dev-env-wildfly-1")) {
            throw GradleException("WildFly container failed to start properly or is unhealthy")
        }

        println("🎉 WildFly environment started successfully")
        println("Management console available at: http://localhost:9990")
        println("Username: ${System.getenv("WILDFLY_ADMIN_USER") ?: "admin"}")
        println("Password: ${System.getenv("WILDFLY_ADMIN_PASSWORD") ?: "admin123"}")
    }
}