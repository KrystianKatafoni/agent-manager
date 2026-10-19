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
    // Sessions and Mission Control for the selected project (AM-405).
    implementation(project(":feature:sessions:api"))
    implementation(project(":feature:sessions:impl"))
    implementation(project(":feature:mission-control:ui"))
    // The ⌘K palette: registry, commands and the overlay (AM-408, ADR 0011).
    implementation(project(":feature:command-palette:api"))
    implementation(project(":feature:command-palette:impl"))
    implementation(project(":feature:command-palette:ui"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(project(":core:testing"))
    testImplementation(libs.compose.ui.test)
    // FakeProjectsApi and FakeProjectInspector for the window test.
    testImplementation(testFixtures(project(":feature:projects:api")))
    // FakeSessions behind the sidebar counts and Mission Control in the window test (AM-405, AM-305).
    testImplementation(testFixtures(project(":feature:sessions:api")))
    // FakeCommandRegistry and letterChord for the project commands and the palette scenarios (AM-408).
    testImplementation(testFixtures(project(":feature:command-palette:api")))
}

// AM-501 spike: a separate source set, not part of `main` or the .dmg. `check` compiles it so it does not rot.
// Run with `./gradlew :app:runTerminalSpike [-PspikeBlending=true]`; AM-503 removes it (docs/plans/AM-501.md).
val terminalSpike =
    kotlin.target.compilations.create("terminalSpike") {
        associateWith(kotlin.target.compilations.getByName("main"))
    }

configurations["terminalSpikeImplementation"].extendsFrom(configurations["implementation"])
configurations["terminalSpikeRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])

dependencies {
    "terminalSpikeImplementation"(project(":feature:terminal:api"))
    "terminalSpikeImplementation"(project(":feature:terminal:impl"))
}

tasks.register<JavaExec>("runTerminalSpike") {
    group = "application"
    description = "Opens the AM-501 JediTerm-in-SwingPanel test window (-PspikeBlending=true enables interop blending)."
    mainClass = "dev.agentmanager.app.terminalspike.TerminalSpikeMainKt"
    classpath = terminalSpike.output.allOutputs + terminalSpike.runtimeDependencyFiles
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    if (providers.gradleProperty("spikeBlending").orNull == "true") {
        systemProperty("compose.interop.blending", "true")
    }
}

tasks.named("check") { dependsOn("compileTerminalSpikeKotlin") }

compose.desktop {
    application {
        mainClass = "dev.agentmanager.app.MainKt"
    }
}
