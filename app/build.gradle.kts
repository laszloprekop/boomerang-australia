plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "boomerang.AppKt"
}

tasks.named<Test>("test") {
    // Tests tagged "legacy-bug" fail on purpose: they show where the original breaks the rules.
    useJUnitPlatform {
        excludeTags("legacy-bug")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// The tests that show the faults of the original BoomerangAustralia.java. Expected to FAIL.
// Not part of `test`, `check` or `build`. Run with: ./gradlew legacyBugs
tasks.register<Test>("legacyBugs") {
    description = "Runs the tests tagged legacy-bug, which fail against the original program."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("legacy-bug")
    }
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showStackTraces = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
