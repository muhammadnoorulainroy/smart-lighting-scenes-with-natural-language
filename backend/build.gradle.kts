// Smart Lighting Scenes - Backend Build Configuration
// Spring Boot 3.5.6 | Java 21 | Gradle 8.x

import java.time.Instant

plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
	checkstyle
	pmd
	id("com.github.spotbugs") version "6.0.26"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Smart Lighting Scenes with Natural Language - Backend API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot starters
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	
	// WebSocket messaging support
	implementation("org.springframework:spring-messaging")
	
	// Session management
	implementation("org.springframework.session:spring-session-data-redis")
	
	// Database
	implementation("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	
	// JWT authentication
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
	
	// MQTT for IoT devices
	implementation("org.eclipse.paho:org.eclipse.paho.mqttv5.client:1.2.5")
	implementation("org.springframework.integration:spring-integration-mqtt:6.2.0")
	
	// OpenAI for natural language processing
	implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	
	// YAML parsing
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
	implementation("org.yaml:snakeyaml:2.2")
	
	// Utilities
	implementation("org.apache.commons:commons-lang3:3.14.0")
	implementation("com.google.guava:guava:32.1.3-jre")
	implementation("org.modelmapper:modelmapper:3.2.0")
	implementation("me.paulschwarz:spring-dotenv:4.0.0")
	
	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	
	// Development tools
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	
	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:testcontainers:1.19.3")
	testImplementation("org.testcontainers:postgresql:1.19.3")
	testImplementation("org.testcontainers:junit-jupiter:1.19.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showStandardStreams = false
	}
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	archiveFileName.set("${project.name}-${project.version}.jar")
	
	manifest {
		attributes(mapOf(
			"Implementation-Title" to project.name,
			"Implementation-Version" to project.version,
			"Built-By" to System.getProperty("user.name"),
			"Build-JDK" to System.getProperty("java.version"),
			"Build-Timestamp" to Instant.now().toString()
		))
	}
}

tasks.register("deps") {
	group = "help"
	description = "Display dependency tree"
	doLast {
		println("Run './gradlew dependencies' to view the dependency tree")
	}
}

// Static Code Analysis Configuration

checkstyle {
	toolVersion = "10.20.1"
	configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
	isIgnoreFailures = false
	maxWarnings = 0
}

pmd {
	toolVersion = "7.7.0"
	isConsoleOutput = true
	isIgnoreFailures = false
	rulesMinimumPriority = 3
	ruleSets = listOf()
	ruleSetFiles = files("${rootDir}/config/pmd/pmd-rules.xml")
}

spotbugs {
	toolVersion = "4.8.6"
	ignoreFailures = false
	showStackTraces = true
	showProgress = true
	effort = com.github.spotbugs.snom.Effort.MAX
	reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
	excludeFilter = file("${rootDir}/config/spotbugs/spotbugs-exclude.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
	reports.create("html") {
		required = true
		outputLocation = file("${layout.buildDirectory.get()}/reports/spotbugs/${name}.html")
	}
	reports.create("xml") {
		required = false
	}
}

tasks.withType<Checkstyle> {
	reports {
		xml.required = false
		html.required = true
	}
}

tasks.withType<Pmd> {
	reports {
		xml.required = false
		html.required = true
	}
}

tasks.register("analyze") {
	group = "verification"
	description = "Run all static code analysis tools"
	dependsOn("checkstyleMain", "pmdMain", "spotbugsMain")
}

// Javadoc Configuration
tasks.withType<Javadoc> {
	options {
		this as StandardJavadocDocletOptions
		encoding = "UTF-8"
		docEncoding = "UTF-8"
		charSet = "UTF-8"
		addStringOption("Xdoclint:none", "-quiet")
		windowTitle = "Smart Lighting API Documentation"
		docTitle = "Smart Lighting Scenes API"
		header = "<b>Smart Lighting Scenes</b>"
		bottom = "Copyright &copy; 2025 Smart Lighting Team. MIT License."
		links("https://docs.oracle.com/en/java/javase/21/docs/api/")
		links("https://docs.spring.io/spring-framework/docs/current/javadoc-api/")
	}
	source = sourceSets["main"].allJava
	classpath = configurations["compileClasspath"]
	setDestinationDir(file("${layout.buildDirectory.get()}/docs/javadoc"))
}

tasks.register("docs") {
	group = "documentation"
	description = "Generate Javadoc API documentation"
	dependsOn("javadoc")
	doLast {
		println("Javadoc generated at: ${layout.buildDirectory.get()}/docs/javadoc/index.html")
	}
}
