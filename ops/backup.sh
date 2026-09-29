#!/usr/bin/env bash
# Zálohovací skript pro produkci (převzato z kvalita-cena, docs/nasazeni.md). Zálohuje jediné, co
# appka drží mimo Docker image: databázi přes pg_dump (Sraz nemá nahrávané soubory). Necílí přímo
# na offsite úložiště — to dělá ops/pull-backup.sh z lokálního PC, ať jeden neúspěšný upload
# nezastaví ani lokální zálohu.
#
# Spouštět z cronu jako uživatel, který smí `docker compose` (typicky ten, co appku nasadil):
#   0 3 * * * /home/<user>/sraz-app/ops/backup.sh >> /var/backups/sraz/backup.log 2>&1
# Log NIKDY do /var/log/ — ten patří root:syslog, běžný uživatel do něj nemůže zapisovat
# (ověřeno v produkci kvalita-cena 2026-08-28: cron s přesměrováním do /var/log/ 5 dní tiše
# selhával hned na `>>`, appka byla bez automatické zálohy). BACKUP_ROOT je naopak vlastněný
# tímhle uživatelem, takže tam log zapsat jde.
#
# Před prvním nasazením do cronu OVĚŘIT OBNOVU na čistou instanci (docs/nasazeni.md) — záloha,
# která se nikdy nezkusila obnovit, není záloha.
#
# Retence: RETENTION_DAYS (výchozí 14) i RETENTION_COPIES (výchozí 14, strop pro případ ručního
# spuštění vícekrát za den) — obojí popsáno v ops/README.md, "Retence a úklid". MIN_FREE_MB
# (výchozí 2048) je pojistka proti zaplnění disku: skript radši skončí chybou bez zápisu než
# aby zálohou nebo pádem uprostřed ní shodil i běžící appku.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$REPO_DIR/compose.prod.yaml"
ENV_FILE="$REPO_DIR/.env"
BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/sraz}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
RETENTION_COPIES="${RETENTION_COPIES:-14}"
MIN_FREE_MB="${MIN_FREE_MB:-2048}"
TIMESTAMP="$(date +%F-%H%M)"

# shellcheck disable=SC1091
source "$REPO_DIR/ops/lib-retention.sh"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Chybí $ENV_FILE — bez POSTGRES_USER nejde spustit pg_dump." >&2
  exit 1
fi
# shellcheck disable=SC1090
set -a && source "$ENV_FILE" && set +a

mkdir -p "$BACKUP_ROOT"
rotate_log "$BACKUP_ROOT/backup.log" 524288 2000

# Smazat rozdělaný soubor TOHOTO běhu při jakémkoli selhání — jinak po pádu uprostřed `pg_dump`
# zůstane useknutý .gz, který vypadá jako platná záloha (najde se to až při obnově).
cleanup_partial() {
  rm -f "$BACKUP_ROOT/db-$TIMESTAMP.sql.gz"
}
trap 'cleanup_partial' ERR

# Radši žádná nová záloha než zaplněný disk, který by shodil i běžící appku — kontrola PŘED
# jakýmkoli zápisem, ne až po pg_dump.
free_mb="$(df -Pm "$BACKUP_ROOT" | awk 'NR==2 {print $4}')"
if (( free_mb < MIN_FREE_MB )); then
  echo "[$TIMESTAMP] CHYBA: na disku zbývá jen ${free_mb} MB (limit MIN_FREE_MB=${MIN_FREE_MB} MB) — záloha se nespouští." >&2
  exit 1
fi

echo "[$TIMESTAMP] Zálohuji databázi..."
docker compose -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d sraz \
  | gzip > "$BACKUP_ROOT/db-$TIMESTAMP.sql.gz"

echo "[$TIMESTAMP] Mažu lokální zálohy starší než $RETENTION_DAYS dní..."
find "$BACKUP_ROOT" -maxdepth 1 -type f -name 'db-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete

# Strop i na POČET kopií (vedle stáří výš) — skript se dá spustit ručně i vícekrát za den, kdy
# by časová retence žádnou z nich zatím nesmazala.
echo "[$TIMESTAMP] Ořezávám na max. $RETENTION_COPIES kopií..."
prune_count "$BACKUP_ROOT" 'db-*.sql.gz' "$RETENTION_COPIES"

echo "[$TIMESTAMP] Hotovo: $BACKUP_ROOT/db-$TIMESTAMP.sql.gz"

# --- Offsite kopie ---
# Stejně jako u kvalita-cena: zálohy si stahuje lokální PC (ops/pull-backup.sh, cron na LOKÁLNÍM
# stroji) a odtud jdou ručně na externí disk/NAS — ne placený cloud.
