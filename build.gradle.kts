import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "net.myna.mnbt"


plugins {
    java
    id("java-library")
    id("idea")

    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.4.20"

    id("maven-publish")
}

allprojects {

    repositories {
        mavenCentral()
    }

    tasks.create<Delete>("clean build") {
        group = "build"
        delete = setOf("build", "out")
    }

    apply {
        // fix issue like https://stackoverflow.com/questions/62498917/gradle-kotlin-dsl-build-script-fails-when-java-sourcecompatibility-defined-in
        plugin("java")
    }

    java.sourceCompatibility = JavaVersion.VERSION_11
    java.targetCompatibility = JavaVersion.VERSION_11

//    gradle.projectsEvaluated {
//        tasks.withType(JavaCompile::class.java) {
//            options.compilerArgs = listOf("-Xlint","-verbose","-XprintRounds","-XprintProcessorInfo","-Xmaxerrs", "100000")
//        }
//    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf()
        }
    }



}

//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            from(components["java"])
//        }
//    }
//}





