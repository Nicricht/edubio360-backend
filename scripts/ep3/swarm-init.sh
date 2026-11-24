#!/usr/bin/env bash
set -euo pipefail
case "${1:-}" in
  manager)
    if [ "$(docker info --format '{{.Swarm.LocalNodeState}}')" = inactive ]; then
      if [ -n "${2:-}" ]; then docker swarm init --advertise-addr "$2" >/dev/null; else docker swarm init >/dev/null; fi
    fi
    docker node ls
    ;;
  worker)
    : "${SWARM_JOIN_TOKEN:?Definir token de worker}"
    : "${2:?IP del manager}"
    if [ "$(docker info --format '{{.Swarm.LocalNodeState}}')" = inactive ]; then
      docker swarm join --token "$SWARM_JOIN_TOKEN" "$2:2377"
    fi
    ;;
  *) echo "Uso: $0 manager [IP] | worker IP_MANAGER" >&2; exit 2 ;;
esac
