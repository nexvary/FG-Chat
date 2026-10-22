plugins { kotlin("jvm") version "2.0.21" }
kotlin { jvmToolchain(17) }
tasks.test { useJUnitPlatform() }
dependencies { testImplementation(kotlin("test")); testImplementation("org.junit.jupiter:junit-jupiter:5.11.3"); testRuntimeOnly("org.junit.platform:junit-platform-engine:5.11.3") }
