import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "net.myna.mnbt"


plugins {
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

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            from(components["java"])
//        }
//    }
//}





