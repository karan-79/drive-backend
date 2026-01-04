plugins {
	java
	id("org.springframework.boot") version "3.4.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.project"
version = "0.0.1"

tasks.bootJar {
	archiveBaseName.set("drive")
}

tasks.jar {
	archiveClassifier.set("plain")
	enabled = false
}

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
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security:3.4.5")


	// https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-api
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	// https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-impl
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	// https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-jackson
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	implementation("org.springframework.boot:spring-boot-docker-compose")
	implementation("org.flywaydb:flyway-core")
	implementation("org.apache.tika:tika-core:3.2.3")
	implementation("net.coobird:thumbnailator:0.4.21")
	implementation("org.apache.pdfbox:pdfbox:3.0.6")

	runtimeOnly("org.flywaydb:flyway-database-postgresql:11.8.0")
	implementation("org.postgresql:postgresql:42.7.5")
	implementation("com.google.firebase:firebase-admin:9.5.0")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	// https://mvnrepository.com/artifact/software.amazon.awssdk/s3
	implementation("software.amazon.awssdk:s3:2.31.77")
	testImplementation("org.testcontainers:localstack")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
