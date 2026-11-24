#!/usr/bin/env bash
# Package only the executable JARs produced by this run's successful Maven verify.
set -euo pipefail
mode="${1:?prepare or verify}"
out="${2:?bundle directory}"
services=(discovery-server auth-service academic-service guidance-service notification-service analytics-service import-service api-gateway)
commit="${GITHUB_SHA:-$(git rev-parse HEAD)}"
if [ "$mode" = prepare ]; then
  mkdir -p "$out"
  printf '%s\n' "$commit" > "$out/commit.txt"
  for service in "${services[@]}"; do
    shopt -s nullglob
    jars=("$service"/target/*.jar)
    shopt -u nullglob
    test "${#jars[@]}" = 1
    test -s "${jars[0]}"
    mkdir -p "$out/portable-jars/$service/target"
    cp "${jars[0]}" "$out/portable-jars/$service/target/"
  done
  (cd "$out"; find portable-jars -type f -name '*.jar' -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS)
elif [ "$mode" != verify ]; then
  exit 2
fi
test "$(cat "$out/commit.txt")" = "$commit"
(cd "$out"; sha256sum -c SHA256SUMS)
test "$(wc -l < "$out/SHA256SUMS")" -eq 8
for service in "${services[@]}"; do
  shopt -s nullglob
  jars=("$out/portable-jars/$service/target/"*.jar)
  shopt -u nullglob
  test "${#jars[@]}" = 1
  test -s "${jars[0]}"
done
echo "JAR verificados, 8 servicios, commit $commit"
