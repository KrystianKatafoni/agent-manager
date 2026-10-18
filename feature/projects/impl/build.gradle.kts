// Adapters of the projects feature (docs/11 §4): JsonProjectStore over core:storage (AM-302).
plugins {
    id("am.feature.impl")
}

dependencies {
    implementation(project(":feature:projects:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:storage"))

    // Contracts, fakes and builders of the port.
    testImplementation(testFixtures(project(":feature:projects:api")))
    // InMemoryJsonStore for the store unit tests.
    testImplementation(testFixtures(project(":core:storage")))
    testImplementation(project(":core:testing"))
}
