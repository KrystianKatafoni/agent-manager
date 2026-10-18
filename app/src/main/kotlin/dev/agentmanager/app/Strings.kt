package dev.agentmanager.app

import java.util.Locale
import java.util.ResourceBundle

/** UI texts from `i18n/strings.properties` (NFR-10: Polish now, ready for de/en bundles). */
internal object Strings {
    // No fallback to the JVM default locale: an English system must still get the requested Polish texts.
    private val bundle: ResourceBundle =
        ResourceBundle.getBundle(
            "i18n.strings",
            Locale.forLanguageTag("pl"),
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES),
        )

    val keys: Set<String> get() = bundle.keySet()

    operator fun get(key: String): String = bundle.getString(key)
}
