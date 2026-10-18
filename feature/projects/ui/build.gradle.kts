// Projects sidebar (docs/11 §1, MVI): the list with selection, adding a folder from disk after a
// read-only inspection and removing with a confirmation dialog (AM-304).
plugins {
    id("am.feature.ui")
}

dependencies {
    implementation(project(":feature:projects:api"))
    // AgentRuntime for listing and stopping the sessions of a removed project (FR-P4, AM-304).
    implementation(project(":feature:agent-runtime:api"))
    implementation(project(":core:domain"))

    // FakeProjectsApi, FakeProjectInspector and aProject for the controller and UI tests.
    testImplementation(testFixtures(project(":feature:projects:api")))
    // FakeAgentRuntime and aRuntimeSession for the remove flow.
    testImplementation(testFixtures(project(":feature:agent-runtime:api")))
    // RecordingLogger and aSessionId.
    testImplementation(project(":core:testing"))
    // Compose UI tests (runComposeUiTest) with the desktop runtime of the current OS.
    testImplementation(libs.compose.ui.test)
    testImplementation(compose.desktop.currentOs)
}
