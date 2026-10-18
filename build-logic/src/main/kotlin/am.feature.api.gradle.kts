// Public contract of a feature: explicit visibility everywhere, contracts and fakes in testFixtures.
plugins {
    id("am.kotlin")
    `java-test-fixtures`
}

kotlin {
    explicitApi()
}
