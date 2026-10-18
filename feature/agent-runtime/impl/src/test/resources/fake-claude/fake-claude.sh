#!/bin/sh
# Fake `claude` for the ClaudeCliAdapter tests (AM-203, health AM-204). POSIX sh and awk only: no jq, no Python.
#
# Usage: fake-claude.sh <state-dir> <claude arguments...>
# FakeClaude.kt installs it behind an absolute `bin/claude` wrapper, so the real `claude` never runs.
#
# State files in <state-dir>:
#   sessions.tsv  one session per line, tab separated, `-` for an empty field:
#                 id, sessionId, name, cwd, state, waitingFor, startedAt (epoch ms), pid
#   logs/<id>     output of `claude logs <id>`
#   counter       last number used for a new id (ids are `printf %08x`)
#   now           epoch ms used as startedAt of new sessions (no system clock)
#   version       printed by `claude --version`
#   agent-view    enabled | env | setting: how Agent View is switched (off by env or by setting)
#   daemon        running | stopped | absent: answer of `claude daemon status` (absent = older CLI)
#   mode          normal | garbage | hang | fail <code> <stderr...> | bg-child
#   calls.log     argv of every call: arguments end with \037, calls end with \036
set -u

state=$1
shift

US=$(printf '\037')
RS=$(printf '\036')
TAB=$(printf '\t')
sessions="$state/sessions.tsv"

for arg in "$@"; do
    printf '%s%s' "$arg" "$US" >>"$state/calls.log"
done
printf '%s' "$RS" >>"$state/calls.log"

die() {
    printf 'fake-claude: %s\n' "$1" >&2
    exit "${2:-1}"
}

# die_raw <message>: like the real CLI, without the fake-claude prefix, exit 1.
die_raw() {
    printf '%s\n' "$1" >&2
    exit 1
}

# state_word <file> <default>: first word of a state file.
state_word() {
    if [ -s "$state/$1" ]; then sed -n '1p' "$state/$1" | cut -d ' ' -f 1; else printf '%s' "$2"; fi
}

[ -f "$sessions" ] || : >"$sessions"
agent_view=$(state_word agent-view enabled)
daemon=$(state_word daemon running)

mode=normal
if [ -s "$state/mode" ]; then
    mode=$(sed -n '1p' "$state/mode" | cut -d ' ' -f 1)
fi

case $mode in
garbage)
    printf 'this is not json {\n'
    exit 0
    ;;
hang)
    printf '%s\n' "$$" >"$state/hang.pid"
    sleep 30 &
    printf '%s\n' "$!" >>"$state/hang.pid"
    wait
    exit 0
    ;;
fail)
    code=$(sed -n '1p' "$state/mode" | cut -d ' ' -f 2)
    sed '1s/^fail [0-9]* \{0,1\}//' "$state/mode" >&2
    exit "$code"
    ;;
normal | bg-child) ;;
*)
    die "unknown mode $mode" 2
    ;;
esac

has_session() {
    cut -f 1 "$sessions" | grep -qxF -- "$1"
}

# need_id <verb> [<id>]: exactly one id that the supervisor knows.
need_id() {
    [ $# -eq 2 ] || die "usage: claude $1 <id>" 2
    has_session "$2" || die "no session $2"
}

# update_session <id> <state> <pid>: also clears waitingFor.
update_session() {
    awk -F "$TAB" -v OFS="$TAB" -v id="$1" -v st="$2" -v pid="$3" \
        '$1 == id { $5 = st; $6 = "-"; $8 = pid } { print }' "$sessions" >"$sessions.tmp"
    mv "$sessions.tmp" "$sessions"
}

remove_session() {
    awk -F "$TAB" -v id="$1" '$1 != id' "$sessions" >"$sessions.tmp"
    mv "$sessions.tmp" "$sessions"
    rm -f "$state/logs/$1"
}

# list_json <all>: the `kind: background` format of fixtures/agents-json/synthetic/background-all-phases.json.
list_json() {
    awk -F "$TAB" -v all="$1" '
        function esc(s,    out, i, c) {
            out = ""
            for (i = 1; i <= length(s); i++) {
                c = substr(s, i, 1)
                if (c == "\\" || c == "\"") out = out "\\"
                out = out c
            }
            return out
        }
        function field(key, value) { return "\"" key "\": \"" esc(value) "\"" }
        BEGIN { printf "["; n = 0 }
        NF >= 8 {
            if (all != 1 && ($5 == "done" || $5 == "failed" || $5 == "stopped")) next
            printf "%s\n  {%s, \"kind\": \"background\"", (n++ ? "," : ""), field("id", $1)
            if ($2 != "-") printf ", %s", field("sessionId", $2)
            printf ", %s, %s, %s", field("name", $3), field("cwd", $4), field("state", $5)
            if ($6 != "-") printf ", %s", field("waitingFor", $6)
            printf ", \"startedAt\": %s", $7
            if ($8 != "-") printf ", \"pid\": %s", $8
            printf "}"
        }
        END { if (n) printf "\n"; printf "]\n" }
    ' "$sessions"
}

# 2.1.302 messages of a switched-off Agent View; the CLI ends with exit 1.
agent_view_disabled() {
    label="'claude agents'"
    for arg in "$@"; do
        [ "$arg" = --json ] && label="'claude agents --json'"
    done
    case $agent_view in
    env) die_raw "$label is disabled by CLAUDE_CODE_DISABLE_AGENT_VIEW." ;;
    setting) die_raw "$label is disabled by the 'disableAgentView' setting." ;;
    *) die "unknown agent-view $agent_view" 2 ;;
    esac
}

agents() {
    [ "$agent_view" = enabled ] || agent_view_disabled "$@"
    json=0
    all=0
    while [ $# -gt 0 ]; do
        case $1 in
        --json) json=1 ;;
        --all) all=1 ;;
        *) die "unknown option '$1' for agents" ;;
        esac
        shift
    done
    [ "$json" -eq 1 ] || die "only 'agents --json' is supported" 2
    list_json "$all"
}

need_value() {
    [ $# -ge 2 ] || die "option '$1' needs a value"
}

# Options of 2.1.302 `claude --help`; anything else is rejected like the real option parser does.
start_bg() {
    name=
    have_prompt=0
    prompt=
    while [ $# -gt 0 ]; do
        case $1 in
        --bg | --background) ;;
        -n | --name)
            need_value "$@"
            name=$2
            shift
            ;;
        --agent | --model)
            need_value "$@"
            shift
            ;;
        --permission-mode)
            need_value "$@"
            case $2 in
            acceptEdits | auto | bypassPermissions | manual | dontAsk | plan) ;;
            *) die "invalid permission mode '$2'" ;;
            esac
            shift
            ;;
        -p | --print) die "--bg cannot be used with --print" ;;
        --)
            shift
            [ $# -eq 1 ] || die "expected exactly one prompt after --"
            prompt=$1
            have_prompt=1
            break
            ;;
        -*) die "unknown option '$1'" ;;
        *)
            [ "$have_prompt" -eq 0 ] || die "unexpected argument"
            prompt=$1
            have_prompt=1
            ;;
        esac
        shift
    done
    [ "$have_prompt" -eq 1 ] && [ -n "$prompt" ] || die "a prompt is required with --bg"

    n=$(cat "$state/counter" 2>/dev/null || printf 0)
    while :; do
        n=$((n + 1))
        id=$(printf '%08x' "$n")
        has_session "$id" || break
    done
    printf '%s\n' "$n" >"$state/counter"
    [ -n "$name" ] || name=$id
    now=$(cat "$state/now")
    cwd=$(pwd -P)

    if [ "$mode" = bg-child ]; then
        # Keeps stdout and stderr open after this process exits, like the real background service.
        sleep 30 &
        printf '%s\n' "$!" >"$state/bg-child.pid"
    fi

    printf '%s\t-\t%s\t%s\tworking\t-\t%s\t%s\n' "$id" "$name" "$cwd" "$now" "$$" >>"$sessions"
    printf 'Starting background service…\n'
    printf 'backgrounded · %s · %s\n' "$id" "$name"
}

# `claude daemon status` of 2.1.302; without --json, which the CLI ignores with a warning.
daemon_cmd() {
    [ "$daemon" != absent ] || die "unknown command daemon" 2
    [ "${1-}" = status ] && [ $# -eq 1 ] || die "usage: claude daemon status" 2
    case $daemon in
    running)
        printf 'pid:     698\n'
        printf 'version: %s\n' "$(cat "$state/version")"
        printf 'uptime:  1m 2s\n'
        printf 'origin:  agents\n'
        printf 'bg sessions:\n'
        printf '  control.sock: reachable\n'
        ;;
    stopped)
        printf 'not running\n'
        exit 1
        ;;
    *) die "unknown daemon $daemon" 2 ;;
    esac
}

case ${1-} in
daemon)
    shift
    daemon_cmd "$@"
    ;;
--version | -v)
    printf '%s (Claude Code)\n' "$(cat "$state/version")"
    ;;
agents)
    shift
    agents "$@"
    ;;
stop | kill)
    need_id "$@"
    update_session "$2" stopped -
    ;;
respawn)
    need_id "$@"
    update_session "$2" working "$$"
    ;;
rm)
    need_id "$@"
    remove_session "$2"
    ;;
logs)
    need_id "$@"
    if [ -f "$state/logs/$2" ]; then cat "$state/logs/$2"; fi
    ;;
attach)
    need_id "$@"
    printf 'fake-claude: attached %s\n' "$2"
    ;;
*)
    for arg in "$@"; do
        case $arg in
        --) break ;;
        --bg | --background)
            start_bg "$@"
            exit 0
            ;;
        esac
    done
    die "only background sessions are supported" 2
    ;;
esac
