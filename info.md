# Info k aplikaci

## Nastavení pro https://railway.com/
```
SPRING_DATASOURCE_URL="jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}"
SPRING_DATASOURCE_USERNAME="${{Postgres.PGUSER}}"
SPRING_DATASOURCE_PASSWORD="${{Postgres.PGPASSWORD}}"
APP_MAGIC_LINK_TOKEN_URL="https://${{RAILWAY_PUBLIC_DOMAIN}}/verify-token/"
APP_MAIL_RESEND_API_KEY="<resend-api-key>"
APP_MAGIC_LINK_MAIL_FROM="Neodpovídejte <noreply@prani-pani-doktorce.cz>"
JWT_SECRET="<base64-jwt-secret>"
```