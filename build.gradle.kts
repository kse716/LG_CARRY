// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.isFile) {
        localFile.inputStream().use(::load)
    }
}

val carryBuildRoot = localProperties
    .getProperty("carry.buildRoot")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) }

if (carryBuildRoot != null) {
    layout.buildDirectory.set(carryBuildRoot.resolve("root"))
}

subprojects {
    if (carryBuildRoot != null) {
        val projectBuildName = path.removePrefix(":").replace(':', '_').ifBlank { name }
        layout.buildDirectory.set(carryBuildRoot.resolve(projectBuildName))
    }
}
