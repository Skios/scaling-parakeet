plugins {
    id("base")
}

tasks.register<Exec>("startEnvironment") {
    group = "environment"
    description = "Launch the development environment via docker compose"

    //Setup WorkingDir
    workingDir("./src/main/docker")

    //Generating command with check if override file is created
    if (file("./src/main/docker/local.env").exists()) {
        commandLine("docker","compose", "--project-name=dev-env", "--env-file", "local.env", "-f", "./docker-compose.yml", "up", "--detach")
    }else{
        commandLine("docker","compose", "--project-name=dev-env", "-f", "./docker-compose.yml", "up", "--detach")
    }
}