group = "ori"
version = "1.0.0"

plugins {
    id("com.android.library") version "9.0.1"
    `maven-publish`
}

android {
    namespace = "ori"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.window:window:1.4.0")
    implementation("com.caverock:androidsvg:1.4")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "activity"
            }
        }

        repositories {
            maven {
                name = "MavenLocal"
                url = uri("file://${project.rootDir}/maven")
            }
        }
    }
}
