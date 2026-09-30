-- Testovací data pro lokální ruční testování (start-dev.sh). Opakované spuštění je bezpečné –
-- vše se vkládá jen pokud to ještě neexistuje (podle e-mailu / názvu / začátku akce).
--
-- Přihlášení: e-mail níž + kód z logu backendu ([DEV] Přihlašovací kód pro ...).
--   organizator@example.com  – organizátor skupiny, stálý člen
--   hrac01..hrac08@example.com – stálí hráči
--   hrac09..hrac12@example.com – náhradníci
--   brankar1, brankar2@example.com – stálí brankáři
-- Kapacita akcí je schválně malá (5 hráčů na tým), ať jde vyzkoušet fronta; cena 2800 Kč/h, stálí aspoň 200 Kč.

BEGIN;

-- --- uživatelé ---
INSERT INTO users (public_name, email, email_verified_at, terms_accepted_at)
SELECT v.name, v.email, now(), now()
FROM (VALUES
  ('Organizátor', 'organizator@example.com'),
  ('Adam Novák', 'hrac01@example.com'),
  ('Bára Svobodová', 'hrac02@example.com'),
  ('Cyril Dvořák', 'hrac03@example.com'),
  ('David Černý', 'hrac04@example.com'),
  ('Eva Procházková', 'hrac05@example.com'),
  ('Filip Kučera', 'hrac06@example.com'),
  ('Gustav Veselý', 'hrac07@example.com'),
  ('Hana Horáková', 'hrac08@example.com'),
  ('Ivan Němec', 'hrac09@example.com'),
  ('Jana Marková', 'hrac10@example.com'),
  ('Karel Pokorný', 'hrac11@example.com'),
  ('Lucie Pospíšilová', 'hrac12@example.com'),
  ('Martin Hájek (brankář)', 'brankar1@example.com'),
  ('Norbert Král (brankář)', 'brankar2@example.com')
) AS v(name, email)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u CROSS JOIN roles r
WHERE r.name = 'ROLE_USER'
  AND (u.email LIKE '%@example.com')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- --- skupina, týmy, místo ---
INSERT INTO sport_groups (name, description)
SELECT 'Večerní hokej (test)', 'Testovací skupina z dev/seed.sql'
WHERE NOT EXISTS (SELECT 1 FROM sport_groups WHERE name = 'Večerní hokej (test)');

INSERT INTO teams (group_id, name, color, sort_order)
SELECT g.id, t.name, t.color, t.sort_order
FROM sport_groups g
CROSS JOIN (VALUES ('Modří', '#1677ff', 0), ('Červení', '#f5222d', 1)) AS t(name, color, sort_order)
WHERE g.name = 'Večerní hokej (test)'
  AND NOT EXISTS (SELECT 1 FROM teams x WHERE x.group_id = g.id AND x.name = t.name);

INSERT INTO venues (group_id, name, address, map_url, latitude, longitude)
SELECT g.id, 'Zimní stadion', 'Nádražní 1, Hrádek nad Nisou', 'https://mapy.com/', 50.852, 14.845
FROM sport_groups g
WHERE g.name = 'Večerní hokej (test)'
  AND NOT EXISTS (SELECT 1 FROM venues v WHERE v.group_id = g.id AND v.name = 'Zimní stadion');

-- --- členství ---
INSERT INTO group_members (group_id, user_id, email, member_type, position, organizer, status, responded_at)
SELECT g.id, u.id, u.email,
       CASE WHEN u.email IN ('hrac09@example.com', 'hrac10@example.com', 'hrac11@example.com', 'hrac12@example.com')
            THEN 'SUBSTITUTE' ELSE 'REGULAR' END,
       CASE WHEN u.email LIKE 'brankar%' THEN 'GOALIE' ELSE 'PLAYER' END,
       u.email = 'organizator@example.com',
       'ACTIVE', now()
FROM sport_groups g
CROSS JOIN users u
WHERE g.name = 'Večerní hokej (test)'
  AND u.email LIKE '%@example.com'
ON CONFLICT (group_id, email) DO NOTHING;

-- --- akce: pátky ve 20:00 (Europe/Prague) v nejbližších 4 týdnech, jen budoucí ---
INSERT INTO events (group_id, venue_id, name, starts_at, duration_minutes, max_players_per_team, max_goalies,
                    signup_deadline, invite_regulars_hours_before, invite_substitutes_hours_before, status,
                    price_per_hour, regular_fee, reminder_hours_before)
SELECT g.id, v.id, 'Večerní hokej', s.starts_at, 60, 5, 2, s.starts_at - interval '24 hours', 96, 48, 'PLANNED',
       2800, 200, 3
FROM sport_groups g
JOIN venues v ON v.group_id = g.id AND v.name = 'Zimní stadion'
CROSS JOIN LATERAL (
  SELECT ((date_trunc('week', now() AT TIME ZONE 'Europe/Prague') + interval '4 days 20 hours' + n * interval '1 week')
          AT TIME ZONE 'Europe/Prague') AS starts_at
  FROM generate_series(0, 3) AS n
) s
WHERE g.name = 'Večerní hokej (test)'
  AND s.starts_at > now() + interval '1 hour'
  AND NOT EXISTS (SELECT 1 FROM events e WHERE e.group_id = g.id AND e.starts_at = s.starts_at);

COMMIT;
