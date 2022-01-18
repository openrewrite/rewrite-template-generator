import nl.javadude.gradle.plugins.license.LicenseExtension
import java.util.*

plugins {
    application
    `java-library`
    `maven-publish`

    id("com.github.hierynomus.license") version "0.16.1"
    id("org.owasp.dependencycheck") version "6.5.3"
}

dependencyCheck {
    analyzers.assemblyEnabled = false
    failBuildOnCVSS = 9.0F
}

group = "org.openrewrite.tools"

repositories {
    mavenLocal()
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)
        cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
    }
}

dependencies {
    implementation("info.picocli:picocli:latest.release")

    "annotationProcessor"("info.picocli:picocli-codegen:latest.release")

    implementation("org.ow2.asm:asm:latest.release")
    implementation("org.ow2.asm:asm-util:latest.release")

    implementation("org.openrewrite:rewrite-core:latest.integration")
    implementation("org.openrewrite:rewrite-java:latest.integration")
    implementation("org.openrewrite:rewrite-java-8:latest.integration")
    implementation("org.openrewrite:rewrite-java-11:latest.integration")
    implementation("org.openrewrite:rewrite-xml:latest.integration")
    implementation("org.openrewrite:rewrite-properties:latest.integration")
    implementation("org.openrewrite:rewrite-yaml:latest.integration")
    implementation("org.openrewrite:rewrite-maven:latest.integration")

    implementation("org.slf4j:slf4j-nop:latest.release")

    implementation("ch.qos.logback:logback-classic:1.0.13")

//    // https://github.com/oracle/graal/issues/1943
//    implementation("org.codehaus.janino:janino:3.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation("org.openrewrite:rewrite-test:latest.integration")
    testImplementation("org.assertj:assertj-core:latest.release")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(listOf("-Aproject=${project.group}:${project.name}"))
}

application {
    mainClass.set("org.openrewrite.cli.RewriteTemplateGenerator")
}

configure<LicenseExtension> {
    ext.set("year", Calendar.getInstance().get(Calendar.YEAR))
    skipExistingHeaders = true
    header = project.rootProject.file("gradle/licenseHeader.txt")
    mapping(mapOf("kt" to "SLASHSTAR_STYLE", "java" to "SLASHSTAR_STYLE"))
    strictCheck = true
}
