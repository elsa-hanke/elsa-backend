#!/bin/bash
set -o pipefail

OUTPUT_FILE="/tmp/gradle-test-output.txt"

./gradlew externalIntegrationTests 2>&1 | tee "$OUTPUT_FILE"
GRADLE_EXIT=${PIPESTATUS[0]}

FAILED_TESTS=$(grep -E "^\s+Test .* FAILED" "$OUTPUT_FILE")
if [ -n "$FAILED_TESTS" ]; then
    echo ""
    echo "=================================================="
    echo "FAILED TESTS:"
    echo "$FAILED_TESTS"
    echo "=================================================="
fi

exit $GRADLE_EXIT

