plugins {
    id("base")
}

group = "it.mrcid"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks.register("enforceRootWrapper") {
    group = "build"
    description = "Removes unnecessary Gradle wrapper files from subprojects"
    
    doLast {
        fileTree(rootDir).matching {
            include("*/gradlew", "*/gradlew.bat", "*/gradle/wrapper/*")
        }.forEach { file ->
            if (file.parentFile != rootDir) {
                println("Deleting unnecessary wrapper file: ${file.absolutePath}")
                file.delete()
            }
        }
    }
}