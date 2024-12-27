plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    id("io.freefair.lombok") version "8.4"
}

group = "com.catchaybk"
version = "1.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1.7")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    compileOnly("org.projectlombok:lombok:1.18.30") // 添加Lombok依賴
    annotationProcessor("org.projectlombok:lombok:1.18.30") // 添加Lombok註解處理器
}


tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}