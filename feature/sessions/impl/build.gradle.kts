// Pure core of the sessions feature (docs/11 §4, §7): SessionStateReducer and the runtime mappings
// (AM-402). The module factory comes with SessionMonitor in AM-403.
plugins {
    id("am.feature.impl")
}

dependencies {
    implementation(project(":feature:sessions:api"))
    // RuntimeSession, RuntimePhase, WaitingFor and RuntimeError, the input of the reducer (AM-402).
    implementation(project(":feature:agent-runtime:api"))
    // Outcome, ProjectId and SessionId.
    implementation(project(":core:domain"))

    // aSession and the other sessions builders.
    testImplementation(testFixtures(project(":feature:sessions:api")))
    // aRuntimeSession for the listings fed to the reducer.
    testImplementation(testFixtures(project(":feature:agent-runtime:api")))
    // T0 and aSessionId.
    testImplementation(project(":core:testing"))
}
