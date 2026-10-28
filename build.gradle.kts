plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.android.application") version "8.7.3" apply false
}

subprojects {
    tasks.register("exportRuntimeDependencies") {
        doLast {
            val configuration = configurations.findByName("debugRuntimeClasspath") ?: configurations.findByName("runtimeClasspath") ?: error("No runtime configuration")
            val modules = configuration.incoming.resolutionResult.allComponents.mapNotNull { it.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier }
            val report = layout.buildDirectory.file("reports/dependencies/maven.tsv").get().asFile
            report.parentFile.mkdirs()
            report.writeText(modules.map { "${it.group}:${it.module}\t${it.version}" }.distinct().sorted().joinToString("\n"))
        }
    }
}
