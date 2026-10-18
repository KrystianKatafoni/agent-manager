package dev.agentmanager.projects.ui

/** UI texts from `i18n/projects.properties` (NFR-10), loaded like `app.Strings`, without a system-locale fallback. */
internal object ProjectsStrings {
    val keys: Set<String> get() = TODO("AM-304")

    operator fun get(key: String): String = TODO("AM-304: $key")

    /** [text] with its arguments filled in. */
    fun format(text: UiText): String = TODO("AM-304: ${text.key}")
}
