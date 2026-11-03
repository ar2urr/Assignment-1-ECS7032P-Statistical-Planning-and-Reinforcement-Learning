#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd "$(dirname "$0")/.." && pwd)
tag_dir=$(cd "${1:?Usage: bash scripts/check-stage1.sh /path/to/TabletopGames}" && pwd)
expected_revision=$(cat "$project_dir/TAG_COMMIT")
actual_revision=$(git -C "$tag_dir" rev-parse HEAD)
if [[ "$actual_revision" != "$expected_revision" ]]; then
    echo "Expected TAG $expected_revision; found $actual_revision" >&2
    exit 1
fi

# In Stage 1 the only intended source difference is the package declaration.
for source in BasicMCTSPlayer BasicMCTSParams BasicTreeNode; do
    diff -u <(sed 's/^package players.basicMCTS;$/package sushigoagent;/' \
        "$tag_dir/src/main/java/players/basicMCTS/$source.java") \
        "$project_dir/sushigoagent/$source.java"
done

maven_command="${MVN:-mvn}"
maven_options=()
if [[ -n "${MAVEN_REPO:-}" ]]; then
    maven_options+=("-Dmaven.repo.local=$MAVEN_REPO")
fi
mkdir -p "$project_dir/build/classes" "$project_dir/build/test-classes"
"$maven_command" -B -q -f "$tag_dir/pom.xml" "${maven_options[@]}" \
    -DskipTests compile dependency:build-classpath \
    -DincludeScope=test -Dmdep.outputFile="$project_dir/build/dependencies.txt"

dependency_path=$(cat "$project_dir/build/dependencies.txt")
framework_path="$tag_dir/target/classes:$dependency_path"
javac -cp "$framework_path" -d "$project_dir/build/classes" \
    "$project_dir"/sushigoagent/*.java
javac -cp "$project_dir/build/classes:$framework_path" \
    -d "$project_dir/build/test-classes" \
    "$project_dir/tests/coursework/validation/Stage1SmokeTest.java"

# TAG resolves game resources relative to its checkout.
cd "$tag_dir"
java -Djava.awt.headless=true -Dcoursework.project="$project_dir" \
    -cp "$project_dir/build/classes:$project_dir/build/test-classes:$framework_path" \
    org.junit.runner.JUnitCore coursework.validation.Stage1SmokeTest
