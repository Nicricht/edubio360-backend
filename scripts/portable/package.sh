#!/usr/bin/env bash
set -euo pipefail
frontend_dir="${1:?Indica el directorio del frontend}"
out="${2:-EDUBIO360_PORTABLE}"
export IMAGE_TAG=portable
export IMAGE_PREFIX=edubio360
export MYSQL_ROOT_PASSWORD="$(openssl rand -hex 20)"
export RABBITMQ_USER=edubio
export RABBITMQ_PASSWORD="$(openssl rand -hex 20)"
export JWT_SECRET="$(openssl rand -hex 32)"
mkdir -p "$out/imagenes-docker" "$out/mysql" "$out/codigo"
bash scripts/ep3/build-images.sh
docker build -t edubio360/frontend:portable "$frontend_dir"
docker pull mysql:8.0
docker pull rabbitmq:3.13-management
docker compose -p edubio360-portable -f docker-compose.yml config --no-path-resolution --format json > "$out/compose-build.json"
python3 scripts/portable/compose.py "$out"
rm "$out/compose-build.json"
cp -r mysql/init "$out/mysql/"
cp scripts/portable/*.bat scripts/portable/LEEME.txt "$out/"
git archive --format=zip HEAD > "$out/codigo/backend.zip"
git -C "$frontend_dir" archive --format=zip HEAD > "$out/codigo/frontend.zip"
printf 'Backend: %s\nFrontend: %s\nPlataforma: linux/amd64\n' "$(git rev-parse HEAD)" "$(git -C "$frontend_dir" rev-parse HEAD)" > "$out/VERSIONES.txt"
mapfile -t images < <(docker compose -f "$out/compose.json" config --images | sort -u)
test "${#images[@]}" = 11
for image in "${images[@]}"; do
  test "$(docker image inspect --format '{{.Os}}/{{.Architecture}}' "$image")" = linux/amd64
done
docker save "${images[@]}" | gzip -1 > "$out/imagenes-docker/edubio360.tar.gz"
(cd "$out/imagenes-docker" && sha256sum edubio360.tar.gz > SHA256.txt)
