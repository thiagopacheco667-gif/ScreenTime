// Arquivo raiz: só declara os plugins. Cada módulo aplica os que usa.
// AGP 9 tem Kotlin embutido: o plugin org.jetbrains.kotlin.android NÃO é usado.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
