
plugins {
	java
	`maven-publish`
	id("org.springframework.boot") version "2.4.3"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	id("net.researchgate.release") version "2.8.1"
}


extra["dockerImage"] = "localhost:32000/youmusic/${rootProject.name}"


configure<JavaPluginConvention> {
	sourceCompatibility = JavaVersion.VERSION_11
}

extra["springCloudVersion"] = "Hoxton.SR10"
extra["springBootAdminVersion"] = "2.4.0"

dependencies {

	implementation("org.springframework.boot", "spring-boot-starter-actuator")
	implementation("org.springframework.boot", "spring-boot-starter-webflux")
	implementation("org.springframework.cloud", "spring-cloud-starter")
	implementation("de.codecentric", "spring-boot-admin-starter-server")
	implementation("org.springframework.cloud","spring-cloud-starter-kubernetes")

}

dependencyManagement {
	imports {
		mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}


tasks {

	bootJar {
		archiveFileName.set("app.jar")
		layered()
	}

	processResources {
		filesMatching("bootstrap.*") {
			expand(project.properties)
		}
	}

	register("dockerBuild") {

		group = "docker"
		dependsOn(mutableListOf("build"))

		doLast {
			dockerBuild()
		}
	}

	register("dockerPush") {

		group = "docker"
		dependsOn(mutableListOf("dockerBuild"))

		doLast {
			dockerPush()
		}
	}
}

fun dockerBuild() {

	exec {
		executable("docker")
		args("build", "-t", "${property("dockerImage")}:${project.version}", ".")
	}
}

fun dockerPush() {

	exec {
		executable("docker")
		args("push", "${property("dockerImage")}:${project.version}")
	}
}


repositories {
	mavenCentral()
	mavenLocal()
}


publishing {
	publications {
		create<MavenPublication>("main") {
			artifact(tasks.getByName("bootJar"))
		}
	}
	repositories {
		maven {
			val releases = "https://library.local/repository/releases/"
			val snapshots = "https://library.local/repository/snapshots/"
			url = uri(if (version.toString().endsWith("-SNAPSHOT")) snapshots else releases)
			credentials {
				username = findProperty("nexusUser").toString()
				password = findProperty("nexusPassword").toString()
			}
		}
	}
}


release {
	preTagCommitMessage = "release "
	tagCommitMessage = ""
	newVersionCommitMessage = ""
}

