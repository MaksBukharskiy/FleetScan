plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.fleetScan"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") {
        exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-jaxb-annotations")
    }

    runtimeOnly("com.mysql:mysql-connector-j:9.3.0")
    runtimeOnly("com.h2database:h2")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    runtimeOnly("com.sun.xml.bind:jaxb-impl:2.3.9")

    implementation("org.telegram:telegrambots-spring-boot-starter:6.8.0") {
        exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-jaxb-annotations")
    }

    implementation("net.sourceforge.tess4j:tess4j:5.8.0")
    implementation ("org.springframework.boot:spring-boot-starter-webflux")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-jaxb-annotations")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

tasks.test {
    useJUnitPlatform()
}
