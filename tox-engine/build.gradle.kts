plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}
tasks.test {
    useJUnitPlatform()
    systemProperty("java.library.path", rootProject.file("native/build").absolutePath)
}

tasks.register<JavaExec>("publicNetworkProbe") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.fgchat.tox.PublicNetworkProbeKt")
    systemProperty("java.library.path", rootProject.file("native/build").absolutePath)
}
