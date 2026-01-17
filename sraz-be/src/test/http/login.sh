#!/bin/bash
# login.sh – spust:  ./login.sh  alice  secret

HOST=${HOST:-http://localhost:8080}   # přepiš, když běží jinde
USER=${1:-alice}
PASS=${2:-secret}

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST "$HOST/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")

echo "$RESPONSE"

body=${response%$'\n'*}
code=${response##*$'\n'}

echo "HTTP $code"
echo "$body"   # <-- uvidíš, jestli to je JSON, nebo třeba HTML chyovka

#BODY=$(echo "$RESPONSE" | sed '$d')
#CODE=$(echo "$RESPONSE" | tail -n1)
#
#echo "HTTP $CODE"
#echo "$BODY" | jq .   # vyžaduje jq; bez něj: echo "$BODY"

###
# /home/petr/pracovni/java/sraz-app/backend/src/test/http/login.sh 'admin@example.com' 'admin'
###r