#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test-classes
javac -encoding UTF-8 -d build/test-classes src/main/java/org/astral/*.java src/test/java/org/astral/*.java
java -cp build/test-classes org.astral.SkyMathTest
java -cp build/test-classes org.astral.StarCatalogTest
