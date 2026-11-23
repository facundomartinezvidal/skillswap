#!/usr/bin/env bash
# Levanta MySQL con Docker, compila y abre la UI de SkillSwap.
#
#   ./correr.sh                       # MySQL + app (usuario por defecto: user-mateo)
#   ./correr.sh --usuario user-ana    # cualquier argumento se le pasa a la app
#   ./correr.sh --reset               # borra la base para volver a la demo original
#
# Necesita Docker corriendo y Maven. Para correr sin MySQL: mvn compile exec:java -Dexec.args="--memoria"
set -euo pipefail
cd "$(dirname "$0")"

if [[ "${1:-}" == "--reset" ]]; then
    shift
    docker compose down -v
fi

docker compose up -d --wait
mvn -q compile exec:java -Dexec.args="$*"
