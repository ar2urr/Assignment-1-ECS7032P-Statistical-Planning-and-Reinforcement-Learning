#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd "$(dirname "$0")/.." && pwd)
tag_dir=$(cd "${1:?Usage: bash scripts/check.sh /path/to/TabletopGames}" && pwd)
expected_revision=$(cat "$project_dir/TAG_COMMIT")
actual_revision=$(git -C "$tag_dir" rev-parse HEAD)
[[ "$actual_revision" == "$expected_revision" ]] || {
    echo "Expected TAG $expected_revision; found $actual_revision" >&2; exit 1;
}
maven_command="${MVN:-mvn}"
maven_options=()
[[ -z "${MAVEN_REPO:-}" ]] || maven_options+=("-Dmaven.repo.local=$MAVEN_REPO")
mkdir -p "$project_dir/build"
"$maven_command" -B -q -f "$tag_dir/pom.xml" "${maven_options[@]}" \
    -DskipTests compile dependency:build-classpath -DincludeScope=test \
    -Dmdep.outputFile="$project_dir/build/dependencies.txt"
framework_path="$tag_dir/target/classes:$(cat "$project_dir/build/dependencies.txt")"
# Compile into a new directory so deleted sources cannot leave stale classes.
classes_dir=$(mktemp -d "$project_dir/build/check.XXXXXX")
trap 'rm -rf "$classes_dir"' EXIT
sources=("$project_dir"/sushigoagent/*.java "$project_dir"/experiments/coursework/experiments/*.java)
tests=()
test_classes=()
while IFS= read -r path; do
    tests+=("$path")
    relative=${path#"$project_dir/tests/"}
    class=${relative%.java}
    test_classes+=("${class//\//.}")
done < <(find "$project_dir/tests" -name '*Test.java' -type f | sort)
javac -proc:none -cp "$framework_path" -d "$classes_dir" "${sources[@]}" "${tests[@]}"
cd "$tag_dir"
java -Xms128m -Xmx512m -Djava.awt.headless=true -Dcoursework.project="$project_dir" \
    -cp "$classes_dir:$framework_path" org.junit.runner.JUnitCore "${test_classes[@]}"
