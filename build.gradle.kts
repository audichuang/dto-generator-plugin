plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.4.0"
    id("io.freefair.lombok") version "8.4"
}

group = "com.catchaybk"
version = "1.3.0"

repositories {
    mavenCentral()
    intellijPlatform.defaultRepositories()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    
    intellijPlatform {
        local("/Applications/IntelliJ IDEA.app")
        bundledPlugin("com.intellij.java")
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java")
            include("com/catchaybk/dtogeneratorplugin/core/**")
            include("com/catchaybk/dtogeneratorplugin/intellij/**")
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set("DTO Generator")
        version.set(project.version.toString())
        
        vendor {
            name.set("AudiChuang")
            email.set("audi51408@gmail.com")
            url.set("https://www.google.com")
        }
        
        description.set("IntelliJ IDEA plugin for generating DTO classes with validation annotations")
        
        ideaVersion {
            sinceBuild.set("241")
            untilBuild.set("243.*")
        }
        
        changeNotes.set("""
            <ul>
                <li>UI現代化改進</li>
                <li>修復空指針異常問題</li>
                <li>升級到IntelliJ Platform Gradle Plugin 2.x</li>
            </ul>
        """)
    }
}