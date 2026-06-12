#!/bin/bash
set -o pipefail

OUTPUT_FILE="/tmp/gradle-test-output.txt"

./gradlew externalIntegrationTests 2>&1 | tee "$OUTPUT_FILE"
GRADLE_EXIT=${PIPESTATUS[0]}

FAILED_TESTS=$(awk '/^[^ ]/ { class=$0 } /FAILED/ { print class " >" $0 }' "$OUTPUT_FILE")
if [ -n "$FAILED_TESTS" ]; then
    echo "" >&2
    echo "ERROR ==================================================" >&2
    echo "ERROR FAILED TESTS:" >&2
    while IFS= read -r line; do
        echo "ERROR $line" >&2
    done <<< "$FAILED_TESTS"
    echo "ERROR ==================================================" >&2
fi

exit $GRADLE_EXIT

