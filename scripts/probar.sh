#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
sh scripts/swipl.sh -q -s tests/integrales_tests.pl -g run_tests -t halt
mkdir -p build
javac -encoding UTF-8 -Xlint:all -d build java/*.java tests/PruebaPuente.java
java -Djava.awt.headless=true -cp build PruebaPuente
