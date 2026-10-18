# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/pl/1.1.0/), wersjonowanie: [SemVer](https://semver.org/lang/pl/). Wpisy generuje `/release` z Conventional Commits.

## [Unreleased]

### Added

- Dokumentacja koncepcyjna, plan wdrożenia i roadmapa.
- Konfiguracja agenta Claude Code: CLAUDE.md, subagenci, skille, hooki i uprawnienia.
- Automatyzacja GitHub: CI, `@claude`, review PR, cotygodniowy przegląd kompatybilności, wydania.
- Build Gradle 9.7 (wrapper, JDK 21), katalog wersji i convention plugins `am.kotlin`, `am.feature.api|impl|ui` z testami TestKit.
- Moduł `core:domain`: identyfikatory, `Outcome`, `Clock`, `Logger`, `AppDispatchers`, `ShellCommand`.
- Moduł `core:testing`: `TestClock`, `testDispatchers`, `RecordingLogger`, buildery danych testowych.
- Reguły zależności modułów w Gradle oraz testy architektury Konsist (`tests:architecture`).
- Bramka jakości: Spotless/ktlint, detekt 2.0, progi pokrycia Kover i szybki task `quickCheck` dla hooka `Stop`.
- Moduł `core:process`: `ProcessRunner` i `LoginShellProcessRunner` (login shell, timeouty, ucinanie wyjścia) z `FakeProcessRunner` w `testFixtures`.
- Moduł `core:storage`: `JsonStore` z atomowym zapisem, wersją schematu, migracjami i odkładaniem uszkodzonych plików jako `*.broken`.
- Moduł `app`: okno Compose Desktop z pustym Mission Control, composition root `CoreDeps` i logowanie przez SLF4J (`./gradlew :app:run`).
- Moduł `feature:agent-runtime:api`: kontrakt `AgentRuntime`, `AgentEventSource` i `HookInstaller` z fake'ami i testami kontraktowymi w `testFixtures`.
- Moduł `feature:agent-runtime:impl`: parser `claude agents --json` i `ClaudeCliAdapter` (lista, start w tle, stop, respawn, usuwanie, logi, attach) z fałszywym `claude` do testów.
- Health check `ClaudeCliAdapter.health()`: wersja Claude Code ≥ 2.1.139, włączony Agent View i stan supervisora (`claude --version`, `claude daemon status`, `claude agents --json`).
- Moduł `feature:projects:api`: kontrakty `ProjectsApi` i `ProjectInspector`, reguły `ProjectInputRules` oraz fake'i z testami kontraktowymi w `testFixtures`.
- Moduł `feature:projects:impl`: `JsonProjectStore` utrwala listę projektów w `projects.json` (schemat v1, ADR 0009) przez `JsonStore`, z fabryką `projectsModule(...)`.
- `DirectoryInspector` w `feature:projects:impl`: sprawdzenie katalogu przed dodaniem (gałąź git bez blokad, `CLAUDE.md`, aktywne sesje, nasze hooki w `.claude/settings*.json` wg ADR 0010) z fabryką `projectInspectorModule(...)`.
- Sidebar projektów w oknie aplikacji (`feature:projects:ui`): dodawanie folderu z dysku (⌘⇧O) z raportem inspektora, usuwanie z potwierdzeniem (⌘⌫) i wyborem, czy zatrzymać aktywne sesje, baner przy problemie z `projects.json`.
