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

gradle.rootProject {
    tasks.register("enforceRootWrapper") {
        doLast {
            fileTree(rootDir).matching {
                include("*/gradlew", "*/gradlew.bat", "*/gradle/wrapper/*")
            }.forEach {
                println("Deleting unnecessary wrapper in ${it.parent}")
                it.delete()
            }
        }
    }
}