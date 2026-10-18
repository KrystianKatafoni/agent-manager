# Kompatybilność z Claude Code

Agent Manager steruje supervisorem sesji Claude Code (Agent View, research preview). Ten plik aktualizuje skill `/compat-check` (co tydzień w GitHub Actions) albo człowiek po ręcznym teście.

| Pozycja | Wartość |
| --- | --- |
| Minimalna wersja (Agent View, `claude agents`) | 2.1.139 |
| Wersja, na której oparto dokumentację | 2.1.290 (dokumentacja Agent View, październik 2026) |
| Ostatnio przetestowana ręcznie | 2.1.302 (2026-10-18, Linux; tylko `claude agents --json [--all]` z sesją interaktywną, bez sesji w tle) |
| Ostatni automatyczny przegląd | — |

## Elementy, od których zależymy

| Element | Używany w | Na co uważać |
| --- | --- | --- |
| `claude agents --json [--all] [--cwd]` | `agent-runtime:impl` (parser; adapter nie używa `--cwd`, które jest dopiero od 2.1.141, i filtruje `under` sam) | pola `id`, `sessionId`, `name`, `cwd`, `state`, `status`, `waitingFor`; nowe wartości `state`. Zaobserwowane w 2.1.302 (sesja interaktywna): `pid`, `cwd`, `kind`, `startedAt` (epoch millis), `sessionId`, `name`, `status`; brak `id`, `state`, `waitingFor`. Pola sesji w tle wciąż niezweryfikowane nagraniem (fixtures: `feature/agent-runtime/impl/src/test/resources/fixtures/agents-json/`) |
| `claude --bg`, `--name`, `--agent`, `--model`, `--permission-mode`, `--resume` | `agent-runtime:impl` (`ClaudeCliAdapter`) | zmiany flag; adapter parsuje linię `backgrounded · <id> · <name>` (z dokumentacji, niezweryfikowane nagraniem). `--permission-mode` w 2.1.302 przyjmuje `acceptEdits`, `auto`, `bypassPermissions`, `manual`, `dontAsk`, `plan` — dla `DEFAULT` flaga jest pomijana. Prompt idzie po `--` |
| `claude attach / logs / stop / respawn / rm` | `agent-runtime:impl`, `terminal` | zmiany zachowania, nowe potwierdzenia. 2.1.302: `attach`/`logs` przyjmują `<id\|name>`, `stop`/`rm` `<id>`, `respawn` `<id>\|--all`; `rm` usuwa też worktree i może odmówić (`kept <id>` → `CommandFailed`). Kod wyjścia i komunikat dla nieznanego id nieopisane — adapter sprawdza listę ponownie |
| Hooki HTTP: `Notification`, `Stop`, `StopFailure`, `SessionStart`, `SessionEnd` | `agent-runtime:impl` (serwer), `sessions:impl` | nazwy pól (`session_id`, `last_assistant_message`), typy powiadomień |
| `disableAgentView`, `CLAUDE_CODE_DISABLE_AGENT_VIEW` | health check | wyłączony backend |

## Historia przeglądów

| Data | Wersja Claude Code | Wynik | Issue |
| --- | --- | --- | --- |
