import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("idea")

    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.4.20"

    id("org.beryx.jlink") version "2.23.1"
}

allprojects {

    repositories {
        mavenCentral()
    }

    tasks.create<Delete>("clean build") {
        group = "build"
        delete = setOf("build", "out")
    }

//    gradle.projectsEvaluated {
//        tasks.withType(JavaCompile::class.java) {
//            options.compilerArgs = listOf("-Xlint","-verbose","-XprintRounds","-XprintProcessorInfo","-Xmaxerrs", "100000")
//        }
//    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf()
        }
    }
}



