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
