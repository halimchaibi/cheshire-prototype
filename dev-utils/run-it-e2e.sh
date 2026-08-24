#!/usr/bin/env bash
# Run Calcite pipeline integration tests and OS observability end-to-end tests.
# Default Maven profile skips tests; this script always uses -Ptest.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MVNW=./mvnw
COMMON_ARGS=(-Ptest -Dspotless.check.skip=true -Dsurefire.failIfNoSpecifiedTests=false)
# Isolate registries when OS session tests share a JVM
FORK_ARGS=(-DforkCount=1 -DreuseForks=false)

usage() {
  cat <<'EOF'
Usage: dev-utils/run-it-e2e.sh [target]

Targets:
  all       Calcite IT + OS E2E (default)
  calcite   CalciteQueryEnginePipelineIntegrationTest + CalciteSqlExecutorIntegrationTest
  e2e       OsObservabilityEndToEndTest (+ OsObservabilityConfigTest)
  module    Full cheshire-query-engine-calcite test suite
  help      Show this help

Examples:
  ./dev-utils/run-it-e2e.sh
  ./dev-utils/run-it-e2e.sh calcite
  ./dev-utils/run-it-e2e.sh e2e
EOF
}

run_calcite_it() {
  echo "==> Calcite pipeline integration tests"
  "$MVNW" -pl cheshire-query-engine-calcite -am test \
    "${COMMON_ARGS[@]}" \
    -Dtest=CalciteQueryEnginePipelineIntegrationTest,CalciteSqlExecutorIntegrationTest
}

run_os_e2e() {
  echo "==> OS observability end-to-end tests"
  "$MVNW" -pl cheshire-usecase-os-observability -am test \
    "${COMMON_ARGS[@]}" \
    "${FORK_ARGS[@]}" \
    -Dtest=OsObservabilityEndToEndTest,OsObservabilityConfigTest
}

run_calcite_module() {
  echo "==> Full cheshire-query-engine-calcite test suite"
  "$MVNW" -pl cheshire-query-engine-calcite -am test "${COMMON_ARGS[@]}"
}

TARGET="${1:-all}"

case "$TARGET" in
  all)
    run_calcite_it
    run_os_e2e
    ;;
  calcite)
    run_calcite_it
    ;;
  e2e)
    run_os_e2e
    ;;
  module)
    run_calcite_module
    ;;
  help|-h|--help)
    usage
    exit 0
    ;;
  *)
    echo "Unknown target: $TARGET" >&2
    usage >&2
    exit 1
    ;;
esac

echo "==> Done ($TARGET)"
