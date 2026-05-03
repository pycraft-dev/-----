plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
}

/** KAPT annotation processors resolve Kotlin stubs via kotlinx-metadata-jvm — pin to Kotlin compiler major. */
subprojects {
    afterEvaluate {
        if (!plugins.hasPlugin("org.jetbrains.kotlin.kapt")) return@afterEvaluate
        dependencies.add("kapt", libs.kotlin.metadata.jvm)
    }
}
