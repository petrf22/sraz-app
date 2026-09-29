# Provozní skripty

Stejné skripty jako v `kvalita-cena` (jedna sada postupů pro obě aplikace), upravené pro Sraz:
databáze `sraz`, žádná média, web i actuator na jedné doméně. Checklist nasazení je
v `docs/nasazeni.md`, tady je to, co se skutečně spouští.

## `deploy.sh`

Nasazení vydané verze na server: `./ops/deploy.sh <verze>` (např. `./ops/deploy.sh 0.1.0`).
Aktualizuje repo na tag `vX.Y.Z` (musí už existovat, viz `docs/nasazeni.md`, „Vydání verze"),
sekvenčně sestaví backend a web (ne najednou — na malé instanci riziko OOM), appku spustí a ověří:
kontejnery běží, backend nastartoval bez chyby v logu, a pokud je v `.env` nastavená skutečná
adresa (ne výchozí `*localhost*`), i zvenčí — `/actuator/health` je `UP` a `/actuator/info` hlásí
přesně tuhle verzi (`version` v `sraz-be/build.gradle`) a commit. Zastaví se (bez checkoutu) na
necommitnutých změnách v repu na serveru.

```bash
./ops/deploy.sh 0.1.0
```

## `prod-prompt.sh`

Prompt serveru s bílým štítkem `PRODUKCE` na červeném pozadí, ať nejde zaměnit terminál na
produkci s lokálním PC. **Sourcuje se**, nespouští (proto nemá `+x` ani `set -e`).

```bash
scp ops/prod-prompt.sh sraz@<server>:~/.prod-prompt.sh
ssh sraz@<server>
printf '\n# Prompt produkčního serveru (zdroj: ops/prod-prompt.sh v repu)\n[ -f ~/.prod-prompt.sh ] && . ~/.prod-prompt.sh\n' >> ~/.bashrc
```

Řádek patří **na konec `~/.bashrc`** (výchozí Ubuntu blok si `PS1` nastavuje sám). Soubor se
kopíruje, ne sourcuje z checkoutu — ten stojí na vydaném tagu a mění se s každým nasazením.

## `backup.sh`

Zálohuje databázi (`pg_dump`) do `/var/backups/sraz` (přepiš proměnnou `BACKUP_ROOT`, pokud má
být jinde). Offsite kopii řeší `pull-backup.sh` z lokálního PC.

Cron (na serveru, jako uživatel s právem na `docker compose`):

```bash
crontab -e
# 0 3 * * * /home/sraz/sraz-app/ops/backup.sh >> /var/backups/sraz/backup.log 2>&1
```

**Log nikdy do `/var/log/`** — patří `root:syslog`, běžný uživatel do něj nemůže zapisovat,
přesměrování selže dřív, než se skript spustí, a cron to nijak nenahlásí (u kvalita-cena tak
5 dní neproběhla žádná záloha).

## Retence a úklid

Oba skripty sdílejí `lib-retention.sh` (rotace logu, mazání starých záloh). Timestamp je vždy
z NÁZVU souboru (`date +%F-%H%M`), ne z `mtime`.

**Na serveru** (`backup.sh`):
- `RETENTION_DAYS` (14) — časová retence.
- `RETENTION_COPIES` (14) — strop na počet kopií (ruční spuštění vícekrát za den).
- `MIN_FREE_MB` (2048) — při nedostatku místa skončí chybou BEZ zápisu.
- Při selhání `pg_dump` smaže rozdělaný soubor (`trap ... ERR`), ať nezůstane useknutý `.gz`.
- `backup.log` se rotuje in-place (512 kB / 2000 řádků).

**Na lokálu** (`pull-backup.sh`) — GFS: denní 30 dní, týdenní 180 dní, měsíční 730 dní
(`RETENTION_DAILY_DAYS`/`RETENTION_WEEKLY_DAYS`/`RETENTION_MONTHLY_DAYS`). Po stažení kontroluje
stáří nejnovější zálohy (víc než 2 dny = `VAROVÁNÍ` + desktopová notifikace).

**Zkouška nasucho**: `PRUNE_DRY_RUN=1` vypíše, co by se smazalo, ale nic nesmaže.

## `pull-backup.sh`

Stáhne zálohy ze serveru na lokální stroj do `backup/` (gitignored); odtud je ruční kopie na
externí disk a NAS.

Cron na LOKÁLNÍM stroji (ne na serveru), `SERVER_HOST` je povinný:

```bash
crontab -e
# 0 21 * * * SERVER_HOST=<IP serveru> /home/<user>/sraz-app/ops/pull-backup.sh >> /home/<user>/sraz-app/backup/pull.log 2>&1
```

### Vyhrazený klíč pro `pull-backup.sh`

Skript používá vlastní SSH klíč **bez hesla** (`~/.ssh/id_ed25519_sraz_backup`, přepisitelné
`BACKUP_SSH_KEY`) — cron ho nemusí odemykat přes klíčenku (u kvalita-cena varianta s klíčenkou
selhala hned při prvním automatickém běhu). Na serveru je klíč omezený přes `rrsync` jen na čtení
záloh — v `~sraz/.ssh/authorized_keys` jeden řádek:

```
command="/usr/bin/rrsync -ro /var/backups/sraz",restrict ssh-ed25519 AAAA... pull-backup sraz
```

Zřízení (jednorázově):

```bash
ssh-keygen -t ed25519 -N "" -f ~/.ssh/id_ed25519_sraz_backup -C "pull-backup sraz"
# obsah ~/.ssh/id_ed25519_sraz_backup.pub vlož na server jako JEDEN řádek do
# ~sraz/.ssh/authorized_keys ve tvaru výš (command=...,restrict PŘED veřejným klíčem).
```

Ověřit, že klíč nic jiného neumí — tohle musí být vždy odmítnuto:
`ssh -F /dev/null -i ~/.ssh/id_ed25519_sraz_backup -o IdentitiesOnly=yes sraz@<host> 'id'`.
(`-F /dev/null` je nutné, jinak se ze `~/.ssh/config` přidá neomezený klíč a restrikce se
neuplatní.)

## Zkouška obnovy — PŘED zapnutím cronu

Záloha, která se nikdy nezkusila obnovit, není záloha. Nejjednodušší je dočasná instance:

1. `git clone`, `cp .env.example .env`, doplnit `POSTGRES_USER`/`POSTGRES_PASSWORD` a
   `JWT_SECRET` (libovolné, instance je dočasná).
2. `docker compose -f compose.prod.yaml up -d postgres` a počkat, až naběhne.
3. Obnovit databázi:
   ```bash
   zcat db-<timestamp>.sql.gz | \
     docker compose -f compose.prod.yaml exec -T postgres psql -U "$POSTGRES_USER" -d sraz
   ```
4. `docker compose -f compose.prod.yaml up -d`, ověřit v UI, že skupiny, akce a přihlášky ze
   zálohy skutečně existují.
5. Smazat dočasnou instanci.

## Nouzový přístup bez fungujícího SMTP

`APP_AUTH_OTP_MAIL_ENABLED=false` v `.env` (zakomentované v `.env.example`) — přihlašovací kód se
místo e-mailu vypíše do logu backendu (`docker compose -f compose.prod.yaml logs backend`). Pozvánky
na akce ale dál odcházejí e-mailem, takže bez SMTP je to jen berlička pro první přihlášení
provozovatele. Vrátit zpět (smazat proměnnou) dřív, než appku uvidí kdokoli další.
