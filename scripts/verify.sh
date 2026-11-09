#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
java -version
mvn -version
docker info > /dev/null
mvn -B clean verify
printf '\nReportes: */target/surefire-reports, */target/failsafe-reports, */target/site/jacoco\n'
