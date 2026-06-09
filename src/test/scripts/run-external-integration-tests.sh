#!/bin/bash
set -o pipefail

OUTPUT_FILE="/tmp/gradle-test-output.txt"

./gradlew externalIntegrationTests 2>&1 | tee "$OUTPUT_FILE"
GRADLE_EXIT=${PIPESTATUS[0]}

FAILED_TESTS=$(grep -E "^\s+Test .* FAILED" "$OUTPUT_FILE")
if [ -n "$FAILED_TESTS" ]; then
    printf '\n' >&2
    printf '\033[1;31m==================================================\n' >&2
    printf 'FAILED TESTS:\n' >&2
    printf '%s\n' "$FAILED_TESTS" >&2
    printf '==================================================\033[0m\n' >&2
fi

exit $GRADLE_EXIT

