plugins {
    id("base")
}

// Funzione per eseguire comando e aspettare con output in tempo reale
fun executeCommand(cmd: List<String>, workingDir: File, description: String) {
    println("\n" + "=".repeat(60))
    println("🔄 $description")
    println("📂 Directory: ${workingDir.absolutePath}")
    println("🔧 Comando: ${cmd.joinToString(" ")}")
    println("=".repeat(60))

    val processBuilder = ProcessBuilder(cmd)
        .directory(workingDir)
        .redirectErrorStream(true) // Combina stdout e stderr

    val process = processBuilder.start()

    // Leggi l'output in tempo reale
    val reader = process.inputStream.bufferedReader()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        println("   $line")
        System.out.flush() // Forza il flush per vedere l'output immediatamente
    }

    val exitCode = process.waitFor()

    if (exitCode != 0) {
        println("❌ ERRORE: Comando fallito con exit code: $exitCode")
        throw GradleException("❌ Comando fallito: $description (exit code: $exitCode)")
    }
    println("✅ $description completato con successo")
}

// Funzione per eseguire comando con output ottimizzato (per docker pull)
fun executeCommandWithProgressOptimization(cmd: List<String>, workingDir: File, description: String) {
    println("\n" + "=".repeat(60))
    println("🔄 $description")
    println("📂 Directory: ${workingDir.absolutePath}")
    println("🔧 Comando: ${cmd.joinToString(" ")}")
    println("=".repeat(60))

    val processBuilder = ProcessBuilder(cmd)
        .directory(workingDir)
        .redirectErrorStream(true)

    val process = processBuilder.start()
    val reader = process.inputStream.bufferedReader()

    var line: String?
    var currentService = ""
    var lastWasProgress = false
    val progressLines = mutableSetOf<String>() // Track quali layer stanno scaricando

    while (reader.readLine().also { line = it } != null) {
        val currentLine = line!!.trim()

        // Skip righe completamente vuote
        if (currentLine.isEmpty()) continue

        // Rileva inizio pull di un servizio
        if (currentLine.contains("Pulling") && !currentLine.contains("Downloading") && !currentLine.contains("Extracting")) {
            currentService = currentLine.substringAfter("Pulling ").substringBefore(" ").trim()
            println("📦 $currentLine")
            lastWasProgress = false
            continue
        }

        // Gestisci le diverse tipologie di output
        when {
            // Linee di progresso (Downloading/Extracting con percentuali o MB)
            currentLine.contains("Downloading") || currentLine.contains("Extracting") -> {
                // Mostra il progresso ma raggruppa per layer
                val layerId = currentLine.substringBefore(":").trim()
                val progressInfo = currentLine.substringAfter(":").trim()

                // Se è la prima volta che vediamo questo layer, mostra su nuova riga
                if (!progressLines.contains(layerId)) {
                    progressLines.add(layerId)
                    println("   ⏬ $layerId: $progressInfo")
                } else {
                    // Aggiorna solo se ci sono cambiamenti significativi nella percentuale
                    if (progressInfo.contains("%") || progressInfo.contains("MB") || progressInfo.contains("KB")) {
                        print("\r   ⏬ $layerId: $progressInfo")
                        if (progressInfo.contains("100%") || progressInfo.contains("complete")) {
                            println() // Nuova riga quando completo
                            progressLines.remove(layerId)
                        } else {
                            System.out.flush()
                        }
                    }
                }
                lastWasProgress = true
            }

            // Status di completamento
            currentLine.contains("Pull complete") -> {
                if (lastWasProgress) println() // Nuova riga dopo il progresso
                val layerId = currentLine.substringBefore(":").trim()
                println("   ✅ Pull complete: $layerId")
                progressLines.remove(layerId)
                lastWasProgress = false
            }

            // Layer già esistenti
            currentLine.contains("Already exists") -> {
                if (lastWasProgress) println()
                val layerId = currentLine.substringBefore(":").trim()
                println("   📋 Already exists: $layerId")
                lastWasProgress = false
            }

            // Download completo
            currentLine.contains("Download complete") -> {
                // Non stampare ogni singolo "download complete", è spam
                val layerId = currentLine.substringBefore(":").trim()
                progressLines.remove(layerId)
                // Solo un punto per indicare progresso
                print(".")
                System.out.flush()
                lastWasProgress = true
            }

            // Verifica checksum (skip, è spam)
            currentLine.contains("Verifying Checksum") -> {
                // Skip silenziosamente
            }

            // Status finali importanti
            currentLine.contains("Status:") ||
                    currentLine.contains("Downloaded newer image") ||
                    currentLine.contains("Image is up to date") -> {
                if (lastWasProgress) println()
                println("   📋 $currentLine")
                lastWasProgress = false
            }

            // Digest e informazioni finali
            currentLine.contains("sha256:") || currentLine.contains("Digest:") -> {
                if (lastWasProgress) println()
                println("   🔑 ${currentLine.take(80)}${if (currentLine.length > 80) "..." else ""}")
                lastWasProgress = false
            }

            // Errori e warning
            currentLine.lowercase().contains("error") ||
                    currentLine.lowercase().contains("warning") -> {
                if (lastWasProgress) println()
                println("   ⚠️  $currentLine")
                lastWasProgress = false
            }

            // Altre informazioni importanti (ma non spam)
            currentLine.contains("latest:") ||
                    currentLine.startsWith("docker.io/") ||
                    currentLine.startsWith("ghcr.io/") ||
                    currentLine.contains("Pulled") -> {
                if (lastWasProgress) println()
                println("   📄 $currentLine")
                lastWasProgress = false
            }

            // Skip righe repetitive o poco utili
            currentLine.contains("Waiting") ||
                    currentLine.contains("Retrying") ||
                    currentLine.contains("Layer already exists") -> {
                // Skip silenziosamente per ridurre spam
            }

            // Tutto il resto (ma con filtro per evitare troppo spam)
            else -> {
                if (currentLine.length > 5 && !currentLine.matches(Regex("^[a-f0-9]{12}$"))) {
                    if (lastWasProgress) println()
                    println("   🔧 $currentLine")
                    lastWasProgress = false
                }
            }
        }
    }

    if (lastWasProgress) {
        println() // Assicurati che termini con una nuova riga
    }

    val exitCode = process.waitFor()

    if (exitCode != 0) {
        println("❌ ERRORE: Comando fallito con exit code: $exitCode")
        throw GradleException("❌ Comando fallito: $description (exit code: $exitCode)")
    }
    println("✅ $description completato con successo")
}

// Funzione per eseguire comando con timeout e output in tempo reale
fun executeCommandWithTimeout(cmd: List<String>, workingDir: File, description: String, timeoutMinutes: Long = 2) {
    println("\n" + "=".repeat(60))
    println("🔄 $description (timeout: $timeoutMinutes min)")
    println("📂 Directory: ${workingDir.absolutePath}")
    println("🔧 Comando: ${cmd.joinToString(" ")}")
    println("=".repeat(60))

    val processBuilder = ProcessBuilder(cmd)
        .directory(workingDir)
        .redirectErrorStream(true)

    val process = processBuilder.start()

    // Thread per leggere l'output
    val outputThread = Thread {
        try {
            val reader = process.inputStream.bufferedReader()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println("   $line")
                System.out.flush()
            }
        } catch (e: Exception) {
            // Il processo potrebbe essere terminato
        }
    }
    outputThread.start()

    // Aspetta con timeout
    val completed = process.waitFor(timeoutMinutes, java.util.concurrent.TimeUnit.MINUTES)

    if (!completed) {
        println("⏰ TIMEOUT: $timeoutMinutes minuti raggiunti, terminazione forzata...")
        process.destroyForcibly()
        outputThread.interrupt()
        throw GradleException("❌ Comando terminato per timeout dopo $timeoutMinutes minuti: $description")
    }

    outputThread.join(1000) // Aspetta che finisca di leggere l'output

    val exitCode = process.exitValue()
    if (exitCode != 0) {
        println("❌ ERRORE: Comando fallito con exit code: $exitCode")
        throw GradleException("❌ Comando fallito: $description (exit code: $exitCode)")
    }
    println("✅ $description completato con successo")
}

// Funzione di utilità per creare la lista degli --env-file
fun addEnvFiles(command: MutableList<String>, workingDir: File): MutableList<String> {
    val isArm = System.getProperty("os.arch").contains("aarch64") ||
            System.getProperty("os.arch").contains("arm")

    command.add("--env-file")
    command.add(".env")

    if (isArm && workingDir.resolve("arm.env").exists()) {
        command.add("--env-file")
        command.add("arm.env")
    }
    if (workingDir.resolve("local.env").exists()) {
        command.add("--env-file")
        command.add("local.env")
    }
    return command
}

// Funzione per leggere i profili dal file .env
fun getActiveProfiles(workingDir: File): List<String> {
    val envFile = workingDir.resolve(".env")
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

// Funzione per controllare la salute dei container con output dettagliato
fun checkContainerHealth(containerName: String, filterName: String): Boolean {
    val maxAttempts = 30  // 5 minuti totali (10 secondi * 30)
    val waitInterval = 10000L  // 10 secondi tra i controlli

    println("\n🔍 Controllo salute container: $containerName")

    var attempts = 0
    while (attempts < maxAttempts) {
        try {
            print("   Tentativo ${attempts + 1}/$maxAttempts... ")
            val processBuilder = ProcessBuilder(
                "docker", "ps",
                "--filter", "name=$filterName",
                "--format", "{{.Names}} {{.Status}}"
            )
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                if (output.isEmpty()) {
                    println("❌ Container non trovato")
                } else {
                    println("Status: $output")
                    if (output.contains("Up") && !output.contains("unhealthy")) {
                        println("✅ $containerName is healthy")
                        return true
                    }
                }
            } else {
                println("❌ Errore comando docker (exit code: $exitCode)")
            }
        } catch (e: Exception) {
            println("❌ Errore nel controllo dello stato di $containerName: ${e.message}")
        }

        if (attempts < maxAttempts - 1) {
            print("   ⏳ Attesa ${waitInterval/1000} secondi prima del prossimo controllo...")
            Thread.sleep(waitInterval)
            println(" fatto")
        }
        attempts++
    }

    println("❌ $containerName non è diventato healthy dopo $maxAttempts tentativi")
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

tasks.register("debugEnvironment") {
    group = "environment"
    description = "Debug dell'ambiente - mostra configurazione e stato"

    doLast {
        println("\n" + "=".repeat(60))
        println("🔍 DEBUG ENVIRONMENT CONFIGURATION")
        println("=".repeat(60))

        val workingDir = file("./src/main/docker")
        println("📂 Working directory: ${workingDir.absolutePath}")
        println("📂 Directory exists: ${workingDir.exists()}")

        if (workingDir.exists()) {
            println("📁 Contents:")
            workingDir.listFiles()?.forEach { file ->
                println("   ${if (file.isDirectory()) "📁" else "📄"} ${file.name}")
            }
        }

        // Controlla file .env
        val envFile = workingDir.resolve(".env")
        println("\n🔧 Environment file (.env):")
        println("   Path: ${envFile.absolutePath}")
        println("   Exists: ${envFile.exists()}")

        if (envFile.exists()) {
            println("   Content:")
            envFile.readLines().take(10).forEach { line ->
                println("     $line")
            }
        }

        // Controlla profili attivi
        println("\n🏷️  Active profiles:")
        try {
            val activeProfiles = getActiveProfiles(workingDir)
            println("   Profiles: ${activeProfiles.joinToString(", ")}")
        } catch (e: Exception) {
            println("   ❌ Error reading profiles: ${e.message}")
        }

        // Controlla Docker
        println("\n🐳 Docker status:")
        try {
            val dockerVersion = ProcessBuilder("docker", "--version")
                .start()
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
            println("   Docker: $dockerVersion")
        } catch (e: Exception) {
            println("   ❌ Docker not available: ${e.message}")
        }

        try {
            val dockerComposeVersion = ProcessBuilder("docker", "compose", "--version")
                .start()
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
            println("   Docker Compose: $dockerComposeVersion")
        } catch (e: Exception) {
            println("   ❌ Docker Compose not available: ${e.message}")
        }

        // Controlla container esistenti
        println("\n📦 Existing containers (dev-env project):")
        try {
            val process = ProcessBuilder(
                "docker", "ps", "-a",
                "--filter", "label=com.docker.compose.project=dev-env",
                "--format", "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
            ).directory(workingDir).start()

            val output = process.inputStream.bufferedReader().readText()
            if (output.trim().isEmpty()) {
                println("   No containers found")
            } else {
                println("   $output")
            }
        } catch (e: Exception) {
            println("   ❌ Error checking containers: ${e.message}")
        }

        println("\n=".repeat(60))
        println("🎯 CONSIGLI:")
        println("1. Esegui 'gradle debugEnvironment' se hai problemi")
        println("2. Esegui 'gradle downloadImages' per scaricare le immagini")
        println("3. Esegui 'gradle bootEnvironment' per avviare tutto")
        println("4. Esegui 'gradle statusEnvironment' per vedere lo stato")
        println("=".repeat(60))
    }
}

tasks.register("downloadImages") {
    group = "environment"
    description = "Scarica le immagini Docker necessarie"

    doLast {
        println("\n🚀 INIZIO TASK: downloadImages")
        val workingDir = file("./src/main/docker")

        println("📂 Verifico directory di lavoro: ${workingDir.absolutePath}")
        if (!workingDir.exists()) {
            throw GradleException("❌ Directory non trovata: ${workingDir.absolutePath}")
        }

        val activeProfiles = getActiveProfiles(workingDir)
        println("🏷️  Profili attivi: ${activeProfiles.joinToString(", ")}")

        println("📦 Inizio download immagini Docker...")

        val command = mutableListOf("docker", "compose", "-f", "docker-compose.yml")

        // Aggiungi i profili al comando
        activeProfiles.forEach { profile ->
            command.addAll(listOf("--profile", profile))
        }

        addEnvFiles(command, workingDir)
        command.add("pull")

        // Usa la funzione ottimizzata per il download
        executeCommandWithProgressOptimization(command, workingDir, "Download immagini Docker")
        println("🎉 COMPLETATO: downloadImages")
    }
}

tasks.register("initEnvironment") {
    group = "environment"
    description = "Esegue il container wildfly-init per setup iniziale"

    doLast {
        val workingDir = file("./src/main/docker")
        val activeProfiles = getActiveProfiles(workingDir)

        // Verifica se è necessario il wildfly-init
        val needsWildflyInit = activeProfiles.any {
            it in listOf("wildfly", "dev", "full")
        }

        if (!needsWildflyInit) {
            println("⚠️  Nessun profilo richiede WildFly init. Profili attivi: ${activeProfiles.joinToString(", ")}")
            return@doLast
        }

        println("📁 Verificando e creando directory necessarie...")
        ensureDirectoriesExist(workingDir)

        // Controlla se i file di configurazione esistono già
        val wildflyStandaloneConfig = workingDir.resolve("wildfly/standalone/configuration")
        val wildflyDomainConfig = workingDir.resolve("wildfly/domain/configuration")

        if (wildflyStandaloneConfig.exists() && wildflyStandaloneConfig.listFiles()?.isNotEmpty() == true &&
            wildflyDomainConfig.exists() && wildflyDomainConfig.listFiles()?.isNotEmpty() == true) {
            println("✅ File di configurazione WildFly già presenti, skip inizializzazione")
            return@doLast
        }

        println("🚀 Avvio container wildfly-init per inizializzazione...")

        val command = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env-init", // Nome progetto diverso per evitare conflitti
            "-f", "docker-compose.wildfly-init.yml"
        )
        addEnvFiles(command, workingDir)
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
                    "-f", "docker-compose.wildfly-init.yml",
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

tasks.register("startEnvironment") {
    group = "environment"
    description = "Avvia i servizi Docker in background con profili"

    // Dipende dall'inizializzazione
    dependsOn("initEnvironment")

    doLast {
        val workingDir = file("./src/main/docker")
        val activeProfiles = getActiveProfiles(workingDir)

        println("🚀 Avvio dei servizi dell'environment con profili: ${activeProfiles.joinToString(", ")}")

        val command = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "docker-compose.yml"
        )

        // Aggiungi i profili al comando
        activeProfiles.forEach { profile ->
            command.addAll(listOf("--profile", profile))
        }

        addEnvFiles(command, workingDir)
        command.addAll(listOf("up", "--detach"))

        executeCommand(command, workingDir, "Avvio servizi con profili")

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

tasks.register("stopEnvironment") {
    group = "environment"
    description = "Ferma tutti i container dell'environment"

    doLast {
        val workingDir = file("./src/main/docker")

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

tasks.register("resetEnvironment") {
    group = "environment"
    description = "Ferma e rimuove tutti i container e volumes"

    doLast {
        val workingDir = file("./src/main/docker")

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

tasks.register("statusEnvironment") {
    group = "environment"
    description = "Mostra lo status di tutti i container"

    doLast {
        val workingDir = file("./src/main/docker")
        val activeProfiles = getActiveProfiles(workingDir)

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

tasks.register("bootEnvironment") {
    group = "environment"
    description = "Esegue init completo dell'ambiente (download, init, avvio)"

    doLast {
        val workingDir = file("./src/main/docker")
        val activeProfiles = getActiveProfiles(workingDir)

        println("\n" + "🚀".repeat(20))
        println("🚀 BOOT COMPLETO ENVIRONMENT")
        println("🏷️  Profili: ${activeProfiles.joinToString(", ")}")
        println("🚀".repeat(20))

        // STEP 1: Download Images
        println("\n" + "=".repeat(50))
        println("📦 STEP 1/3: Download Images")
        println("=".repeat(50))

        val downloadCmd = mutableListOf("docker", "compose", "-f", "docker-compose.yml")
        activeProfiles.forEach { profile ->
            downloadCmd.addAll(listOf("--profile", profile))
        }
        addEnvFiles(downloadCmd, workingDir)
        downloadCmd.add("pull")
        executeCommandWithProgressOptimization(downloadCmd, workingDir, "Download immagini Docker")

        // STEP 2: Initialize (se necessario)
        val needsWildflyInit = activeProfiles.any { it in listOf("wildfly", "dev", "full") }

        if (needsWildflyInit) {
            println("\n" + "=".repeat(50))
            println("🔧 STEP 2/3: Initialize WildFly")
            println("=".repeat(50))

            println("📁 Verificando e creando directory necessarie...")
            ensureDirectoriesExist(workingDir)

            val wildflyStandaloneConfig = workingDir.resolve("wildfly/standalone/configuration")
            val wildflyDomainConfig = workingDir.resolve("wildfly/domain/configuration")

            if (!(wildflyStandaloneConfig.exists() && wildflyStandaloneConfig.listFiles()?.isNotEmpty() == true &&
                        wildflyDomainConfig.exists() && wildflyDomainConfig.listFiles()?.isNotEmpty() == true)) {

                val initCmd = mutableListOf(
                    "docker", "compose",
                    "--project-name=dev-env",
                    "-f", "docker-compose.wildfly-init.yml"
                )
                addEnvFiles(initCmd, workingDir)
                initCmd.addAll(listOf("run", "--rm", "wildfly-init"))
                executeCommand(initCmd, workingDir, "Inizializzazione WildFly")

                println("⏳ Pausa di stabilizzazione (2 secondi)...")
                Thread.sleep(2000)
            } else {
                println("✅ File di configurazione WildFly già presenti - skip inizializzazione")
            }
        } else {
            println("\n📋 STEP 2/3: Skipped (nessun profilo richiede WildFly)")
        }

        // STEP 3: Start Services
        println("\n" + "=".repeat(50))
        println("🚀 STEP 3/3: Start Services")
        println("=".repeat(50))

        val startCmd = mutableListOf(
            "docker", "compose",
            "--project-name=dev-env",
            "-f", "docker-compose.yml"
        )
        activeProfiles.forEach { profile ->
            startCmd.addAll(listOf("--profile", profile))
        }
        addEnvFiles(startCmd, workingDir)
        startCmd.addAll(listOf("up", "--detach"))
        executeCommand(startCmd, workingDir, "Avvio servizi con profili")

        // Health checks
        println("\n" + "=".repeat(50))
        println("🔍 VERIFICA FINALE: Health Checks")
        println("=".repeat(50))

        var allHealthy = true

        if (activeProfiles.any { it in listOf("wildfly", "dev", "full") }) {
            if (!checkContainerHealth("WildFly", "dev-env-wildfly-1")) {
                allHealthy = false
                println("❌ WildFly non è diventato healthy")
            }
        }

        if (activeProfiles.any { it in listOf("database", "postgres", "dev", "full", "wildfly") }) {
            if (!checkContainerHealth("PostgreSQL", "dev-env-postgres-1")) {
                println("⚠️  PostgreSQL potrebbe non essere completamente pronto (continuo comunque)")
            }
        }

        if (activeProfiles.any { it in listOf("messaging", "rabbitmq", "dev", "full") }) {
            if (!checkContainerHealth("RabbitMQ", "dev-env-rabbitmq-1")) {
                println("⚠️  RabbitMQ potrebbe non essere completamente pronto (continuo comunque)")
            }
        }

        if (!allHealthy) {
            throw GradleException("❌ Alcuni container critici non sono healthy")
        }

        println("\n" + "🎉".repeat(20))
        println("🎉 BOOT ENVIRONMENT COMPLETATO CON SUCCESSO!")
        println("🎉".repeat(20))
        printServiceInfo(activeProfiles)
    }
}