plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.compose.gradle.plugin)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // TestKit builds resolve the same catalog as the real build.
    systemProperty("am.versionCatalog", rootDir.resolve("../gradle/libs.versions.toml").canonicalPath)
    // Each TestKit build downloads dependencies; keep the suite bounded.
    maxParallelForks = 1
}
