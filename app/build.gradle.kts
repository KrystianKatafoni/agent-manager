// Composition root (ENG-M8, docs/11 §9): the only module that knows implementations. Builds the
// desktop window; `./gradlew :app:run` starts the app.
plugins {
    id("am.app")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(project(":core:testing"))
    testImplementation(libs.compose.ui.test)
}

compose.desktop {
    application {
        mainClass = "dev.agentmanager.app.MainKt"
    }
}
