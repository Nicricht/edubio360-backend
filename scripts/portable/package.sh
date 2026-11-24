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
python3 scripts/portable/monitoring.py "$out"
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

# Additional images are separate from the already validated basic package.
mapfile -t monitor_images < <(comm -13 <(docker compose -f "$out/compose.json" config --images | sort -u) <(docker compose -f "$out/compose-monitoring.json" config --images | sort -u))
test "${#monitor_images[@]}" = 7
for image in "${monitor_images[@]}"; do
  docker pull "$image"
  test "$(docker image inspect --format '{{.Os}}/{{.Architecture}}' "$image")" = linux/amd64
done
docker save "${monitor_images[@]}" | gzip -1 > "$out/imagenes-docker/monitoring.tar.gz"
(cd "$out/imagenes-docker" && sha256sum monitoring.tar.gz > SHA256-monitoring.txt)
python3 scripts/ep3/ci-metrics.py > "$out/ci-metrics.prom"
