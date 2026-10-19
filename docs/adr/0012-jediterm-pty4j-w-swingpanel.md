# ADR 0012: JediTerm i pty4j w SwingPanel jako terminal aplikacji

- **Status:** proponowana
- **Data:** 2026-10-19
- **Dotyczy:** `feature:terminal:*`, `app`, `settings.gradle.kts`, `gradle/libs.versions.toml`

## Kontekst

ADR 0001 wybrał JediTerm + pty4j w `SwingPanel`. docs/09 oznacza osadzenie w Compose jako ryzyko średnie (fokus, skróty, HiDPI); plan B to osobne okno Swing. Spike AM-501 sprawdza to na macOS (plan: `docs/plans/AM-501.md`).

Fakty o bibliotekach (stan na 2026-10-19):

| Biblioteka | Wersja | Źródło | Licencja |
| --- | --- | --- | --- |
| `org.jetbrains.jediterm:jediterm-core`, `jediterm-ui` | 3.76 | tylko `packages.jetbrains.team/maven/p/ij/intellij-dependencies` (brak w Maven Central) | POM: LGPL-3.0; README: LGPLv3 lub Apache 2.0 do wyboru |
| `org.jetbrains.pty4j:pty4j` | 0.14.0 | Maven Central | EPL-1.0; zależy od JNA 5.19.1 (Apache 2.0/LGPL) |

## Decyzja

*Do uzupełnienia po spike'u (TAK / plan B / NIE według kryterium z planu AM-501).*

Propozycja przy wyniku TAK:

1. Terminal to `JediTermWidget` z `pty4j` w `feature:terminal:impl`, osadzany w Compose przez `SwingPanel` w `terminal:ui`. Typy JediTerm i pty4j nie wychodzą poza `terminal:impl` (ENG-I8). `terminal:api` udostępnia tylko `javax.swing.JComponent` (docs/11 §3).
2. Repozytorium JetBrains dodajemy w `settings.gradle.kts` wyłącznie przez `exclusiveContent` dla grupy `org.jetbrains.jediterm`.
3. Licencje: JediTerm na Apache 2.0, pty4j na EPL-1.0; informacje o licencjach w pakiecie `.dmg` (AM-901).
4. Skróty aplikacji (⌘K i inne) przy fokusie w terminalu: mechanizm wybrany w spike'u (F3).

## Wyniki spike'a

*Tabela obserwacji F1–B1 z planu AM-501 (TAK/NIE + uwaga), wersja macOS, model Maca, ekran, rozmiar dodanych jarów.*

## Rozważone alternatywy

| Opcja | Plusy | Minusy |
| --- | --- | --- |
| **JediTerm w `SwingPanel`** | Terminal w układzie okna (panele, tryb terminala z docs/06) | Ryzyko fokusu, skrótów i kolejności rysowania nad Compose |
| JediTerm w osobnym oknie Swing (plan B) | Brak problemów interop Compose–Swing | Brak paneli w Mission Control; osobne okno do zarządzania |
| Jary JediTerm w repo zamiast repozytorium JetBrains | Brak nowego repozytorium | Ręczne aktualizacje, binaria w git, brak metadanych POM |
| JediTerm budowany ze źródeł (included build) | Tylko Maven Central | Dłuższy build, utrzymanie builda obcego projektu |

## Konsekwencje

- Nowe repozytorium Maven w buildzie (ograniczone do jednej grupy); w sesjach w chmurze może być poza allowlistą sieci, wtedy build weryfikuje CI.
- pty4j wypakowuje natywne biblioteki do katalogu tymczasowego; podpis i notaryzacja (AM-902) muszą to uwzględnić (`pty4j.preferred.native.folder`).
- Okno testowe `app/src/terminalSpike` żyje do AM-503, potem jest usuwane.
- Wynik NIE oznacza nowego ADR zastępującego ADR 0001 w części „Terminal”.
