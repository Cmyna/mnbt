plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.10"

    id("maven-publish")
}

version "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(kotlin("reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.myna.mnbt"
            artifactId = "annotation"
            version = "alpha-1.0"

            from(components["java"])
        }
    }
}