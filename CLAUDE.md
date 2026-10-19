# CLAUDE.md — Agent Manager

Natywna aplikacja macOS (Kotlin + Compose Desktop) do zarządzania lokalnymi agentami Claude Code. Ten plik czyta każda sesja Claude Code w repo. Trzymaj go krótkim; szczegóły są w `docs/`.

## Gdzie co jest

| Potrzebujesz | Plik |
| --- | --- |
| **Stan projektu, środowisko, kontekst (czytaj na starcie)** | `docs/15-stan-projektu.md` |
| Co budujemy (wymagania FR/NFR) | `docs/02-wymagania.md` |
| Jak budujemy (zasady ENG-xx, DoD) | `docs/10-wymagania-inzynierskie.md` |
| Moduły, API, reguły zależności | `docs/11-koncepcja-techniczna.md` |
| Jak testujemy | `docs/12-strategia-testow.md` |
| Kolejność prac | `docs/ROADMAP.md` |
| Proces dostarczania, wydania | `docs/13-plan-wdrozenia.md`, `docs/14-agent-claude-code.md` |
| Decyzje architektoniczne | `docs/adr/` |
| Plan bieżącego zadania | `docs/plans/AM-XXX.md` |

## Złote zasady

1. **Jedno zadanie z roadmapy naraz**, na własnej gałęzi, zakończone jednym PR. Nie dokładaj „przy okazji” zmian spoza planu.
2. **Granice modułów są święte.** `ui` → `api`; `impl` → `api`; `api` → `core:domain`; `impl` widzi tylko `app`. Klasy w `impl` są `internal` (poza fabryką modułu). Zmiana `api` innego feature = najpierw pytanie do człowieka.
3. **Najpierw testy**, potem kod. Każda nowa logika ma test jednostkowy, każdy interfejs `api` test kontraktowy, każda funkcja użytkownika test funkcjonalny.
4. **Bez prawdziwego Claude Code, sieci i zegara systemowego w testach.** Fake'i z `testFixtures`, fałszywy `claude` na `PATH`, `runTest` i `TestClock`.
5. **Błędy przez `Outcome`**, nie wyjątki, na granicach modułów. Zakazane: `!!`, `GlobalScope`, `runBlocking` poza `main`/testami, `println`.
6. **Nigdy nie pushuj do `main`, nie używaj `--force`, `--no-verify` ani `gh pr merge`.** Scala człowiek. Hooki to blokują; nie próbuj ich obchodzić. Jedyny wyjątek: po rebase własnej gałęzi zadania (`feat/*`, `fix/*`, `chore/*`, `docs/*`) wolno `git push --force-with-lease`.
7. **Nie zmieniaj konfiguracji agenta** (`.claude/settings.json`, `.claude/hooks/`) bez wyraźnej prośby człowieka.

## Polecenia

```bash
./gradlew check                          # pełna bramka (format, detekt, architektura, testy, pokrycie)
./gradlew :feature:<x>:impl:test         # testy jednego modułu
./gradlew :tests:architecture:test       # reguły modułów (Konsist; pominięte = moduł jeszcze nie istnieje)
./gradlew :tests:functional:test         # scenariusze UI
./gradlew quickCheck                     # szybka bramka: format + detekt + architektura (hook Stop)
./gradlew spotlessApply                  # formatowanie
./gradlew :app:run                       # uruchomienie aplikacji
```

Gradle 9.7 z wrapperem i JDK 21 (toolchain). Convention plugins: `am.kotlin` (z `am.quality`: Spotless/ktlint, detekt, Kover), `am.feature.api`, `am.feature.impl`, `am.feature.ui`, `am.app` w `build-logic/`; reguły detekt w `config/detekt/detekt.yml`; wersje tylko w `gradle/libs.versions.toml`. Repozytoria deklaruje wyłącznie `settings.gradle.kts`.

## Git i GitHub

- Gałęzie: `feat/AM-XXX-opis`, `fix/…`, `chore/…`, `docs/…`, `release/vX.Y.Z`.
- Commity: Conventional Commits **po angielsku** (`feat(sessions): add state reducer`), stopka `Refs: #N`. Małe, logiczne commity.
- PR: opis **po polsku** wg `.github/pull_request_template.md`, `Closes #N`. Wysyłaj przez `/ship`.
- Kod, nazwy i KDoc po angielsku. Dokumentacja w `docs/` po polsku.

## Agenci i skille w tym repo

- `claude --agent am-lead` — sesja prowadząca, deleguje do subagentów: `architect`, `test-engineer`, `implementer`, `reviewer`, `shipper`, `release-manager`, `maintainer`. Planuje Opus (`am-lead`, `architect`); testy, implementację, bramkę, push i PR robi Sonnet (`test-engineer`, `implementer`, `shipper`).
- Skille: `/deliver [AM-XXX]`, `/ship`, `/new-module <feature>`, `/adr <tytuł>`, `/review-pr <nr>`, `/release`, `/compat-check`, `/triage <opis>`, `/roadmap-sync`.

## Definition of Done (skrót)

`./gradlew check` zielony · testy na każdym poziomie, którego dotyczy zmiana · KDoc dla nowego `api` · ADR dla decyzji · plan w `docs/plans/` · wpis w roadmapie zaktualizowany · PR wymienia zmienione moduły.

## Lekcje

Krótkie wnioski z review i błędów agenta, dopisywane przez PR (jedna linia każdy).

- W sesjach Claude Code w chmurze GitHub działa tylko przez REST: zamiast `gh pr create` / `gh issue create|close|comment` używaj `gh api repos/KrystianKatafoni/agent-manager/{pulls,issues}…` (GraphQL jest zablokowany).
- W sesjach w chmurze Maven Central i repozytoria Gradle bywają niedostępne (allowlista sieci); wtedy weryfikacją buildu jest CI w PR.
- Komunikaty wyjątków kotlinx.serialization cytują fragment wejścia — nie wolno ich logować ani zwracać w błędach; używaj stałych kategorii powodu.
- Przy `allWarningsAsErrors` nie używaj przestarzałego API: w Compose MP 1.12 testy UI importują `androidx.compose.ui.test.v2.runComposeUiTest`. Źródła Compose (`*-desktop`) są na repo1.maven.org, więc API da się sprawdzić bez `dl.google.com`.
