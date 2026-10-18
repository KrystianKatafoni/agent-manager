---
name: deliver
description: Dowozi jedno zadanie z docs/ROADMAP.md od wyboru do otwartego PR — issue, gałąź, plan (architect), testy (test-engineer), implementacja (implementer), bramka ./gradlew check, review (reviewer), commity, push i PR. Użyj, gdy człowiek prosi o realizację kolejnego lub wskazanego zadania z roadmapy.
argument-hint: "[AM-XXX]"
allowed-tools: Bash(git *), Bash(gh *), Bash(./gradlew *), Bash(python3 scripts/*), Read, Grep, Glob, Edit, Write
---

# /deliver — dowiezienie zadania z roadmapy

Zadanie: `$ARGUMENTS` (puste = następne dostępne).

## Stan wejściowy

- Gałąź: !`git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "?"`
- Zmiany: !`git status --short 2>/dev/null | head -15 || true`
- W toku: !`grep -E '^- \[~\] AM-[0-9]' docs/ROADMAP.md 2>/dev/null | head -5 || true`
- Do zrobienia: !`grep -E '^- \[ \] AM-[0-9]' docs/ROADMAP.md 2>/dev/null | head -8 || true`

## Krok 0 · Warunki

1. Brak niezatwierdzonych zmian. Jeśli są: zapytaj człowieka (dokończyć je, odłożyć, porzucić). Nie usuwaj ich sam.
2. `git switch main && git pull --ff-only`.
3. Jeśli istnieje wpis `[~]` (otwarty PR), zapytaj, czy kontynuować tamten PR zamiast zaczynać nowe zadanie.

## Krok 1 · Wybór zadania

- Z argumentem: weź wpis o tym ID. Bez argumentu: **pierwszy** wpis `[ ]`, którego wszystkie ID z pola `zależy:` mają `[x]`.
- Pokaż człowiekowi wybraną linię i jednym zdaniem, co powstanie. Jeśli zadanie jest niejasne albo za duże (~400+ linii kodu), zaproponuj doprecyzowanie lub podział i **zatrzymaj się**.
- Issue: jeśli pole `issue:` to `—`, uruchom `python3 scripts/roadmap_sync.py --only AM-XXX`, który utworzy issue i wpisze jego numer.

## Krok 2 · Gałąź

`git switch -c feat/AM-XXX-<krótki-opis-kebab>` (dla poprawek `fix/…`, dla zadań porządkowych `chore/…`).

## Krok 3 · Plan → subagent `architect`

Przekaż: ID, pełną linię z roadmapy, wymagania z pola `wymagania:`. Oczekuj pliku `docs/plans/AM-XXX.md` i listy pytań blokujących.

- Pytania blokujące (zmiana `api` innego feature, nowy ADR, zmiana zakresu) → przedstaw człowiekowi i **czekaj**.
- Przeczytaj plan sam. Odrzuć go, jeśli lista modułów jest szersza, niż wymaga zadanie.

## Krok 4 · Testy → subagent `test-engineer`

Przekaż ścieżkę planu. Oczekiwany wynik: testy kompilują się i nie przechodzą z powodu brakującej implementacji.

## Krok 5 · Implementacja → subagent `implementer`

Przekaż ścieżkę planu i listę czerwonych testów. Oczekiwany wynik: zielone `./gradlew check`.

## Krok 6 · Weryfikacja (sam)

1. `git diff --stat main...` — tylko moduły z planu (+ `docs/plans`, roadmapa, CHANGELOG).
2. `./gradlew check` — zielone. Jeśli nie: przekaż błędy `implementer`; maks. 3 rundy, potem eskalacja do człowieka z opisem.

## Krok 7 · Review → subagent `reviewer`

Przekaż ID i ścieżkę planu. Uwagi **blokujące** wróć do `implementer` / `test-engineer` i powtórz kroki 6–7. Sugestie wpisz do opisu PR jako „do rozważenia”.

## Krok 8 · Dokumentacja

- Roadmapa: zmień `[ ]` na `[~]` dla AM-XXX.
- `CHANGELOG.md` → `[Unreleased]`: jedna linia po polsku, jeśli zmiana jest widoczna dla użytkownika.
- Wniosek z review, który powinien zapobiec powtórce błędu → jedna linia w sekcji „Lekcje” w `CLAUDE.md`.

## Krok 9 · Wysyłka

Wywołaj skill `/ship`. Zwróć człowiekowi: link do PR, wynik bramki, werdykt review, otwarte kwestie, następne zadanie z roadmapy.

Nigdy nie scalaj PR i nie pushuj do `main`.
