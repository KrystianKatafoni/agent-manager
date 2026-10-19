// Terminal feature (AM-501 spike, AM-502): JediTerm widget over a pty4j PTY. Only the spike factory is public.
plugins {
    id("am.feature.impl")
}

dependencies {
    // Outcome, ShellCommand and Logger.
    implementation(project(":core:domain"))
    // TerminalApi, TerminalSession, TerminalId and TerminalError (AM-502); the module factory returns them (AM-502).
    implementation(project(":feature:terminal:api"))
    // Dispatchers.Swing: widgets are built and disposed on the event thread.
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.jediterm.core)
    implementation(libs.jediterm.ui)
    implementation(libs.pty4j)

    // TerminalApiContract, run against the PTY implementation.
    testImplementation(testFixtures(project(":feature:terminal:api")))
    // RecordingLogger.
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
