# Produkční obraz webu (stejný setup jako kvalita-cena) – Caddy servíruje statický Angular build
# a zároveň proxuje /api a /graphql na backend (Caddyfile), aby web i API byly na stejném originu.
#
# `npm run codegen` se tu záměrně NESPOUŠTÍ – vygenerované typy jsou součást repozitáře (CI hlídá,
# že odpovídají schématu), image se staví jen z toho, co je commitnuté.
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM caddy:2-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist/sraz/browser /srv
