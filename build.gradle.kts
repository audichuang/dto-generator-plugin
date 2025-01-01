plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    id("io.freefair.lombok") version "8.4"
}

group = "com.catchaybk"
version = "1.2.1"

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
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java")
            // 添加核心代碼目錄
            include("com/catchaybk/dtogeneratorplugin/core/**")
            // 添加 IDE 特定實現目錄
            include("com/catchaybk/dtogeneratorplugin/intellij/**")
        }
    }
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
}