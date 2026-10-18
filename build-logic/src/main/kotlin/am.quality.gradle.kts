import dev.agentmanager.buildlogic.QualityThresholds
import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension

// Quality gate for every Kotlin module (ENG-C1, docs/12 §7): ktlint via Spotless, detekt with the
// repo config, Kover line coverage. `quickCheck` is the fast subset run by the Claude Code Stop hook.
plugins {
    id("com.diffplug.spotless")
    id("dev.detekt")
    id("org.jetbrains.kotlinx.kover")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.kts")
        ktlint(ktlintVersion)
    }
}

detekt {
    buildUponDefaultConfig.set(true)
    parallel.set(true)
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
}

kover {
    reports {
        verify {
            rule("Line coverage") {
                minBound(QualityThresholds.lineCoverage(path))
            }
        }
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}

tasks.register("quickCheck") {
    group = "verification"
    description = "Fast gate for the agent: formatting and static analysis, no tests."
    dependsOn("spotlessCheck", tasks.withType<Detekt>())
}
