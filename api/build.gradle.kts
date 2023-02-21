

plugins {
    // Apply the java-library plugin to add support for Java Library
    id("java-library")
    id("idea")

    //kotlin
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.4.20"

    id("me.champeau.jmh") version "0.6.8"

    id("maven-publish")
}


repositories {
    //jcenter()
    mavenCentral()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:28.0-jre")

    // Use JUnit test framework
    //implementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.assertj:assertj-core:3.18.1")
    // JMH in test
    testImplementation("org.openjdk.jmh:jmh-core:1.36")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.36")
    kaptTest("org.openjdk.jmh:jmh-generator-annprocess:1.36")
//    testImplementation("org.openjdk.jmh:jmh-kotlin-benchmark-archetype:1.36")
//    kaptTest("org.openjdk.jmh:jmh-kotlin-benchmark-archetype:1.36")

//    implementation("com.google.code.gson:gson:2.8.6")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")

    // annotation
    implementation(project(":annotation"))
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

sourceSets.test {
    java.srcDirs("src/test/kotlin")
    resources.srcDirs("src/test/resources")
}

// the handle error when use ./gradlew build ----- Execution failed for task ':api:processTestResources'.
// > Entry nbt_data/regions/r.3.4.mca is a duplicate but no duplicate handling strategy has been set
tasks.processTestResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.myna.mnbt"
            artifactId = "api"
            version = "alpha-1.0"

            from(components["java"])
        }
    }
}

