#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
chmod +x mvnw
./mvnw clean test
echo "Surefire reports: $(pwd)/target/surefire-reports"
echo "Coverage report:  $(pwd)/target/site/jacoco/index.html"
