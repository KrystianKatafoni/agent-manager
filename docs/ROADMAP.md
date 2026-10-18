# Roadmapa

Źródło prawdy o zakresie i kolejności prac. Czytają ją ludzie i agent (`/deliver`, `/roadmap-sync`).

**Format wpisu** (jedna linia, nie zmieniaj kolejności pól):

```text
- [ ] AM-XXX · Tytuł · moduły: <lista> · zależy: <ID|—> · wymagania: <FR/NFR/ENG> · issue: <#N|—>
```

- `[ ]` do zrobienia, `[~]` w toku (otwarty PR), `[x]` scalone do `main`.
- Agent bierze **pierwszy** wpis `[ ]`, którego wszystkie zależności są `[x]`.
- Zmiana zakresu = PR zmieniający ten plik, z uzasadnieniem w opisie.

## M0 · Fundament repozytorium

- [x] AM-001 · Import dokumentacji koncepcyjnej · moduły: docs · zależy: — · wymagania: — · issue: —
- [x] AM-002 · Agent Claude Code: CLAUDE.md, subagenci, skille, hooki, uprawnienia · moduły: .claude · zależy: AM-001 · wymagania: ENG-C10, ENG sekcja G · issue: #7
- [x] AM-003 · Automatyzacja GitHub: CI, @claude, review PR, maintenance, release, szablony · moduły: .github, scripts · zależy: AM-001 · wymagania: ENG sekcja E · issue: #8

## M1 · Szkielet i bramka jakości

- [x] AM-101 · Gradle wrapper, katalog wersji, convention plugins w build-logic · moduły: build-logic · zależy: AM-002 · wymagania: ENG-M1, ENG-M2 · issue: #9
- [x] AM-102 · core:domain (ID, Outcome, Clock, Logger, ShellCommand) · moduły: core:domain · zależy: AM-101 · wymagania: ENG-M6, ENG-I3, ENG-I4 · issue: #10
- [x] AM-103 · core:testing (TestClock, TestDispatchers, buildery) · moduły: core:testing · zależy: AM-102 · wymagania: ENG-T6 · issue: #11
- [x] AM-104 · Testy architektury Konsist + reguła Gradle impl→impl · moduły: tests:architecture, build-logic · zależy: AM-101 · wymagania: ENG-M3–M8, ENG-T5 · issue: #12
- [x] AM-105 · Spotless/ktlint, detekt, Kover, `./gradlew check` w CI na macOS, szybki task `quickCheck` dla hooka Stop · moduły: build-logic, .github · zależy: AM-104 · wymagania: ENG-C1, ENG sekcja E · issue: #13
- [x] AM-106 · core:process: LoginShellProcessRunner (timeouty, ucinanie wyjścia) · moduły: core:process · zależy: AM-102 · wymagania: ENG-M9 · issue: #14
- [x] AM-107 · core:storage: JsonStore (zapis atomowy, migracje, *.broken) · moduły: core:storage · zależy: AM-102 · wymagania: 11 §8 · issue: #15
- [x] AM-108 · app: okno Compose, composition root, pusty Mission Control · moduły: app · zależy: AM-102 · wymagania: ENG-M8 · issue: #16

## M2 · Integracja z Claude Code

- [x] AM-201 · agent-runtime:api + kontrakt AgentRuntime w testFixtures · moduły: agent-runtime:api · zależy: AM-103 · wymagania: ENG-I1–I8, ENG-T2 · issue: #17
- [x] AM-202 · Parser `claude agents --json` + nagrane fixtures · moduły: agent-runtime:impl · zależy: AM-201 · wymagania: FR-S2, NFR-8 · issue: #18
- [x] AM-203 · ClaudeCliAdapter + fałszywy `claude` do testów · moduły: agent-runtime:impl, agent-runtime:api (testFixtures), core:process · zależy: AM-202, AM-106 · wymagania: FR-S3, FR-S4 · issue: #19
- [x] AM-204 · Health check: wersja ≥ 2.1.139, Agent View, supervisor · moduły: agent-runtime:impl · zależy: AM-203 · wymagania: NFR-9 · issue: #20

## M3 · Projekty

- [~] AM-301 · projects:api + kontrakt · moduły: projects:api · zależy: AM-103 · wymagania: FR-P1, FR-P3 · issue: #21
- [ ] AM-302 · JsonProjectStore · moduły: projects:impl · zależy: AM-301, AM-107 · wymagania: FR-P1, FR-P3 · issue: #22
- [ ] AM-303 · DirectoryInspector (git, CLAUDE.md, sesje, hooki) · moduły: projects:impl · zależy: AM-302, AM-106 · wymagania: FR-P2 · issue: #23
- [ ] AM-304 · projects:ui: sidebar, dodaj z dysku, usuń z dialogiem · moduły: projects:ui · zależy: AM-302, AM-108 · wymagania: FR-P1, FR-P3, FR-P4, FR-P6 · issue: #24

## M4 · Sesje i Mission Control

- [ ] AM-401 · sessions:api (SessionMonitor, SessionCommands) + kontrakty · moduły: sessions:api · zależy: AM-201, AM-301 · wymagania: ENG-I2 · issue: #25
- [ ] AM-402 · SessionStateReducer (czysty, tablicowe testy) · moduły: sessions:impl · zależy: AM-401 · wymagania: FR-S2 · issue: #26
- [ ] AM-403 · Pętla pollingu + snapshot · moduły: sessions:impl · zależy: AM-402, AM-203 · wymagania: FR-S1, NFR-2, NFR-3 · issue: #27
- [ ] AM-404 · Komendy: start, stop, respawn, remove, stopAllIn · moduły: sessions:impl · zależy: AM-403 · wymagania: FR-S3, FR-S4 · issue: #28
- [ ] AM-405 · mission-control:ui: lista sesji, panel szczegółów, pasek trybu · moduły: mission-control:ui · zależy: AM-404, AM-304 · wymagania: FR-S1, FR-S8, FR-S9 · issue: #29
- [ ] AM-406 · Paleta ⌘K i skróty j/k · moduły: command-palette:* · zależy: AM-405 · wymagania: FR-K1, FR-K2 · issue: #30

## M5 · Terminal

- [ ] AM-501 · Spike: JediTerm w SwingPanel (fokus, skróty, HiDPI) · moduły: terminal:impl · zależy: AM-108 · wymagania: FR-T1 · issue: #31
- [ ] AM-502 · terminal:api/impl: shell i attach w PTY · moduły: terminal:api, terminal:impl · zależy: AM-501 · wymagania: FR-T1 · issue: #32
- [ ] AM-503 · terminal:ui: karty per projekt, panele, maksymalizacja · moduły: terminal:ui · zależy: AM-502 · wymagania: FR-T2 · issue: #33
- [ ] AM-504 · Szukanie (regex) i ustawienia terminala · moduły: terminal:* · zależy: AM-503 · wymagania: FR-T3, FR-T7 · issue: #34

## M6 · Hooki i podsumowania

- [ ] AM-601 · KtorHookServer (127.0.0.1, token z Keychain) · moduły: agent-runtime:impl · zależy: AM-203 · wymagania: NFR-4 · issue: #35
- [ ] AM-602 · HookInstaller z backupem i odinstalowaniem · moduły: agent-runtime:impl · zależy: AM-601 · wymagania: NFR-5 · issue: #36
- [ ] AM-603 · Scalanie hooków z pollingiem w reducerze · moduły: sessions:impl · zależy: AM-601, AM-403 · wymagania: NFR-2 · issue: #37
- [ ] AM-604 · Summarizer (last_assistant_message, opcjonalnie Haiku) · moduły: summaries:* · zależy: AM-603 · wymagania: FR-S7 · issue: #38

## M7 · Powiadomienia

- [ ] AM-701 · NotificationPolicy `decide()` + ustawienia · moduły: notifications:api, notifications:impl · zależy: AM-603 · wymagania: FR-N1, FR-N2 · issue: #39
- [ ] AM-702 · Popupy (okno Compose, stos, akcje) · moduły: notifications:ui · zależy: AM-701 · wymagania: FR-N3, FR-S5, FR-S6 · issue: #40
- [ ] AM-703 · MacSayAdapter, szablony, grupowanie · moduły: notifications:impl · zależy: AM-701 · wymagania: FR-N4, FR-N5 · issue: #41
- [ ] AM-704 · Ekran ustawień powiadomień · moduły: notifications:ui · zależy: AM-702, AM-703 · wymagania: FR-N1–N4 · issue: #42
- [ ] AM-705 · Cisza nocna, tryb Skupienia, wyjątki per projekt · moduły: notifications:impl · zależy: AM-704 · wymagania: FR-N6, FR-N7 · issue: #43

## M8 · Wygląd

- [ ] AM-801 · core:design-tokens + katalog 9 schematów i 8 typografii (test kontrastu) · moduły: core:design-tokens, appearance:impl · zależy: AM-102 · wymagania: FR-A1, FR-A2, NFR-6 · issue: #44
- [ ] AM-802 · core:design-system: komponenty i AmTheme · moduły: core:design-system · zależy: AM-801 · wymagania: FR-A1 · issue: #45
- [ ] AM-803 · Ekran Wygląd z podglądem na żywo · moduły: appearance:ui · zależy: AM-802, AM-405 · wymagania: FR-A3, FR-A4 · issue: #46
- [ ] AM-804 · Dzień/noc z macOS, import/eksport theme.json · moduły: appearance:impl · zależy: AM-803 · wymagania: FR-A5, FR-A6 · issue: #47

## M9 · Wydanie 1.0

- [ ] AM-901 · Pakowanie .dmg (packageDmg), ikona, metadane · moduły: app, build-logic · zależy: AM-405 · wymagania: NFR-1 · issue: #48
- [ ] AM-902 · Podpis Developer ID i notaryzacja w CI · moduły: .github · zależy: AM-901 · wymagania: NFR-1 · issue: #49
- [ ] AM-903 · Badge w Docku i ikona w pasku menu · moduły: notifications:impl, app · zależy: AM-705 · wymagania: FR-N1 · issue: #50
- [ ] AM-904 · Komplet testów funkcjonalnych MVP (9 scenariuszy z 12 §4) · moduły: tests:functional · zależy: AM-705, AM-504 · wymagania: ENG-T4 · issue: #51
- [ ] AM-905 · Wydanie v1.0.0 · moduły: — · zależy: AM-902, AM-903, AM-904, AM-804 · wymagania: 09 kryteria MVP · issue: #52
