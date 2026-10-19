// Terminal feature (AM-501 spike, AM-502): JediTerm widget over a pty4j PTY. Only the spike factory is public.
plugins {
    id("am.feature.impl")
}

dependencies {
    // Outcome, ShellCommand and Logger.
    implementation(project(":core:domain"))
    implementation(libs.jediterm.core)
    implementation(libs.jediterm.ui)
    implementation(libs.pty4j)

    testImplementation(project(":core:testing"))
}
