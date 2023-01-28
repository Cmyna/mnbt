buildscript {
    repositories {
        mavenCentral()
    }
    //extra.kotlin_version = "1.6.0"
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
    }
}

plugins {
    // Apply the java-library plugin to add support for Java Library
    id("java-library")
    id("idea")

    //kotlin
    id("org.jetbrains.kotlin.jvm") version "1.6.0" apply false

    //use jlink to do modular work
    id("org.beryx.jlink") version "2.23.1"


}

// jlink task
// we use jlink to extract the module we want from project
// to make modular work, we need module-info.java at the project root to declare requirements, exports, etc.
jlink {
    // commands args:
    // --compress 2: compress resources with zip format
    // --strip-debug: not shows debug from the output image
    // reference at : https://docs.oracle.com/javase/9/tools/jlink.htm#JSWOR-GUID-CECAC52B-CFEE-46CB-8166-F17A8E9280E9
}


repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    //jcenter()
    mavenCentral()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:28.0-jre")

    // Use JUnit test framework
    implementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.assertj:assertj-core:3.18.1")


    implementation("com.google.code.gson:gson:2.8.6")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")

    // annotation
    implementation(project(":annotation"))
    // annotation processor
    annotationProcessor(project(":processor"))
    testAnnotationProcessor(project(":processor"))


}

sourceSets.main {
    java.srcDirs("src/main/java","src/main/kotlin")
}

sourceSets.test {
    java.srcDirs("src/test/kotlin")
}
