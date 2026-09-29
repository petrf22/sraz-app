#!/usr/bin/env bash
# Rychlý start lokálního prostředí pro ruční testování (stejný spouštěč jako v kvalita-cena, bez
# mobilu, navíc Mailpit). Otevře samostatná terminálová okna — databázi, Mailpit, backend
# a frontend — každé v popředí s živými logy, počká až všechny naběhnou, nahraje testovací data,
# otevře prohlížeč a pak čeká na stisk klávesy. Po stisku korektně ukončí všechny procesy včetně
# zastavení kontejnerů (data ve volume DB zůstávají).
#
# Použití:
#   ./start-dev.sh [--no-seed] [--no-open]
#
#   --no-seed    nenahrávat testovací data z dev/seed.sql (jinak se nahrají vždy — vkládají se
#                jen chybějící záznamy, takže opakované spuštění je bezpečné)
#   --no-open    neotvírat prohlížeč automaticky
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="${XDG_RUNTIME_DIR:-/tmp}/sraz-dev"
MAILPIT_CONTAINER="sraz-dev-mailpit"

SEED=1
OPEN_BROWSER=1

usage() {
  echo "Použití: $0 [--no-seed] [--no-open]"
  echo "  --no-seed    nenahrávat testovací data z dev/seed.sql (jinak se nahrají vždy)"
  echo "  --no-open    neotvírat prohlížeč automaticky"
}

for arg in "$@"; do
  case "$arg" in
    --no-seed) SEED=0 ;;
    --no-open) OPEN_BROWSER=0 ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "Neznámý argument: $arg" >&2
      usage >&2
      exit 1
      ;;
  esac
done

command -v docker >/dev/null || { echo "Chybí docker." >&2; exit 1; }
command -v gnome-terminal >/dev/null || { echo "Chybí gnome-terminal." >&2; exit 1; }

rm -rf "$RUN_DIR"
mkdir -p "$RUN_DIR"

# --- pomocné funkce ---

# Otevře nové terminálové okno v interaktivním login shellu (aby se načetl ~/.bashrc
# včetně nvm — v neinteraktivním shellu se .bashrc vrací hned na začátku). Okno není
# potomkem tohoto skriptu, takže PID svého shellu si musí samo zapsat do pidfile.
# Po skončení příkazu se okno zavře samo (exit $rc) — při neplánovaném pádu (mimo
# řízené ukončení skriptem, poznané podle souboru .stopping) nejdřív počká na klávesu,
# ať jsou vidět poslední logy.
open_window() {
  local title="$1" pidfile="$2" cmd="$3"
  gnome-terminal --title="$title" -- bash -lic \
    "echo \$\$ > '$pidfile'; $cmd; rc=\$?; \
     [ -f '$RUN_DIR/.stopping' ] || { echo; echo '--- proces skončil (kód '\$rc') ---'; read -n1 -r -s -p 'Stiskni klávesu pro zavření okna...'; }; \
     exit \$rc"
}

# Rekurzivně ukončí proces a všechny jeho potomky — gradlew/npm spouští vnuky (java/node),
# které signál jen na hlavní PID nezasáhne. Interaktivní `bash -lic` okna SIGTERM ignorují,
# proto po TERM vlně vždycky přijde druhá vlna s KILL.
kill_tree() {
  local pid="$1" sig="${2:-TERM}"
  [ -n "$pid" ] || return 0
  kill -0 "$pid" 2>/dev/null || return 0
  local child
  for child in $(pgrep -P "$pid" 2>/dev/null || true); do
    kill_tree "$child" "$sig"
  done
  kill -"$sig" "$pid" 2>/dev/null || true
}

kill_pidfile() {
  local pidfile="$1" sig="${2:-TERM}"
  [ -f "$pidfile" ] || return 0
  kill_tree "$(cat "$pidfile")" "$sig"
}

# Čeká, až zadaná podmínka (bash příkaz jako string) začne procházet, s tečkovaným průběhem.
# Při vypršení timeoutu vypíše varování a vrátí 1 — volající rozhoduje, jestli pokračuje.
wait_for() {
  local desc="$1" timeout="$2" cond="$3"
  local waited=0
  echo -n "Čekám na $desc"
  until eval "$cond" >/dev/null 2>&1; do
    if [ "$waited" -ge "$timeout" ]; then
      echo " nedoběhlo do ${timeout}s, pokračuji dál (zkontroluj okno s logy)."
      return 1
    fi
    echo -n "."
    sleep 2
    waited=$((waited + 2))
  done
  echo " OK"
}

# Poslouchá už něco na portu? Typicky instance z dřívějška, kterou tenhle skript nespustil
# (nemá pro ni pidfile) a na konci by ji ani neuměl zastavit — nový proces by na obsazeném portu
# spadl, ale wait_for by to nepoznal (port by dál odpovídal té staré instanci).
port_in_use() {
  (echo >"/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1
}

CLEANED_UP=0
cleanup() {
  [ "$CLEANED_UP" -eq 1 ] && return
  CLEANED_UP=1
  echo
  echo "Ukončuji frontend, backend, Mailpit a databázi..."
  # Napřed signál .stopping, ať se okna po ukončení procesu sama zavřou místo čekání na klávesu.
  touch "$RUN_DIR/.stopping"

  kill_pidfile "$RUN_DIR/frontend.pid"
  kill_pidfile "$RUN_DIR/backend.pid"
  sleep 1
  for pf in "$RUN_DIR/frontend.pid" "$RUN_DIR/backend.pid"; do
    kill_pidfile "$pf" KILL
  done

  docker stop "$MAILPIT_CONTAINER" >/dev/null 2>&1 || true
  (cd "$ROOT_DIR" && docker compose stop) || true
  for pf in "$RUN_DIR/mailpit.pid" "$RUN_DIR/db.pid"; do
    kill_pidfile "$pf"
  done
  sleep 1
  for pf in "$RUN_DIR/mailpit.pid" "$RUN_DIR/db.pid"; do
    kill_pidfile "$pf" KILL
  done

  rm -rf "$RUN_DIR"
  echo "Hotovo."
}
trap cleanup EXIT INT TERM

# --- 1. databáze ---
echo "Spouštím databázi..."
open_window "Sraz — DB" "$RUN_DIR/db.pid" "cd '$ROOT_DIR' && docker compose up"
wait_for "databázi (port 5438)" 60 \
  "docker compose -f '$ROOT_DIR/compose.yaml' exec -T postgres pg_isready -U postgres -d sraz" \
  || true

# --- 2. Mailpit (pozvánky do skupin a na akce; přihlašovací kód jde ve vývoji jen do logu backendu) ---
if port_in_use 1025; then
  echo "Port 1025 už poslouchá (běží tam SMTP mimo tento skript) — Mailpit nespouštím."
else
  echo "Spouštím Mailpit..."
  open_window "Sraz — Mailpit" "$RUN_DIR/mailpit.pid" \
    "docker run --rm --name '$MAILPIT_CONTAINER' -p 127.0.0.1:1025:1025 -p 127.0.0.1:8025:8025 axllent/mailpit"
  wait_for "Mailpit (port 8025)" 60 "curl -sf http://localhost:8025/api/v1/info" || true
fi

# --- 3. backend ---
if port_in_use 8080; then
  echo "Port 8080 už poslouchá (běží tam něco mimo tento skript) — nový backend nespouštím," \
       "používám ten stávající. Toto okno ho po ukončení skriptem nezastaví."
else
  echo "Spouštím backend..."
  # DB už běží z kroku 1, docker compose podporu Bootu vypnout (jinak by ji spravoval sám).
  open_window "Sraz — backend" "$RUN_DIR/backend.pid" \
    "cd '$ROOT_DIR/sraz-be' && SPRING_DOCKER_COMPOSE_ENABLED=false ./gradlew bootRun"
fi
wait_for "backend (port 8080)" 240 "curl -sf http://localhost:8080/actuator/health" || true

# --- 3b. seed dat (volitelně, až po Liquibase migraci) ---
if [ "$SEED" -eq 1 ]; then
  echo "Nahrávám testovací data z dev/seed.sql..."
  docker compose -f "$ROOT_DIR/compose.yaml" exec -T postgres \
    psql -q -v ON_ERROR_STOP=1 -U postgres -d sraz < "$ROOT_DIR/dev/seed.sql"
fi

# --- 4. frontend ---
if port_in_use 4200; then
  echo "Port 4200 už poslouchá (běží tam něco mimo tento skript) — nový frontend nespouštím," \
       "používám ten stávající. Toto okno ho po ukončení skriptem nezastaví."
else
  echo "Spouštím frontend..."
  open_window "Sraz — frontend" "$RUN_DIR/frontend.pid" \
    "source ~/.nvm/nvm.sh; nvm use 24; cd '$ROOT_DIR/sraz-fe'; [ -d node_modules ] || npm ci; npm start -- --open=false"
fi
if wait_for "frontend (port 4200)" 180 "curl -sf http://localhost:4200"; then
  if [ "$OPEN_BROWSER" -eq 1 ]; then
    xdg-open "http://localhost:4200" >/dev/null 2>&1 &
  fi
fi

echo
echo "Běží:"
echo "  web:      http://localhost:4200"
echo "  e-maily:  http://localhost:8025 (Mailpit)"
echo "  GraphiQL: http://localhost:8080/graphiql"
echo "  DB:       localhost:5438 (sraz / postgres / 1234)"
if [ "$SEED" -eq 1 ]; then
  echo
  echo "Testovací účty (dev/seed.sql): organizator@example.com, hrac01..hrac12@example.com,"
  echo "  brankar1/brankar2@example.com — přihlašovací kód najdeš v okně backendu"
  echo "  ([DEV] Přihlašovací kód pro ...)."
fi
echo
read -n1 -r -s -p "Stiskni libovolnou klávesu pro ukončení všech procesů..."
