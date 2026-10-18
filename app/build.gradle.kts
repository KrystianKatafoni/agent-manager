// Composition root (ENG-M8, docs/11 §9): the only module that knows implementations. Builds the
// desktop window; `./gradlew :app:run` starts the app.
plugins {
    id("am.app")
}

dependencies {
    implementation(project(":core:domain"))
    // ProcessRunner and JsonStorage for the feature modules (AM-304).
    implementation(project(":core:process"))
    implementation(project(":core:storage"))
    // Projects: the list, the inspector and the sidebar (AM-304).
    implementation(project(":feature:projects:api"))
    implementation(project(":feature:projects:impl"))
    implementation(project(":feature:projects:ui"))
    // The Claude Code runtime behind the inspector and the remove dialog (AM-304).
    implementation(project(":feature:agent-runtime:api"))
    implementation(project(":feature:agent-runtime:impl"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(project(":core:testing"))
    testImplementation(libs.compose.ui.test)
    // FakeProjectsApi, FakeProjectInspector and FakeAgentRuntime for the window test.
    testImplementation(testFixtures(project(":feature:projects:api")))
    testImplementation(testFixtures(project(":feature:agent-runtime:api")))
}

compose.desktop {
    application {
        mainClass = "dev.agentmanager.app.MainKt"
    }
}
