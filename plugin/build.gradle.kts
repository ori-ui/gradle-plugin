group = "ori"
version = "1.0.0"

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("oriPlugin") {
            id = "ori.plugin"
            implementationClass = "ori.OriPlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:9.0.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "plugin"
        }
    }

    repositories {
        maven {
            name = "GitHubPackagesPlugin"
            url = uri("https://maven.pkg.github.com/ori-ui/gradle-plugin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
