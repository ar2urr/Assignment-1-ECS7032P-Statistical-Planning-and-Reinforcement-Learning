#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd "$(dirname "$0")/.." && pwd)
tag_dir=$(cd "${1:?Usage: bash scripts/build-experiments.sh /path/to/TabletopGames}" && pwd)
[[ "$(git -C "$tag_dir" rev-parse HEAD)" == "$(cat "$project_dir/TAG_COMMIT")" ]] || {
    echo "TAG revision does not match TAG_COMMIT" >&2; exit 1;
}
maven_options=()
[[ -z "${MAVEN_REPO:-}" ]] || maven_options+=("-Dmaven.repo.local=$MAVEN_REPO")
mkdir -p "$project_dir/build"
"${MVN:-mvn}" -B -q -f "$tag_dir/pom.xml" "${maven_options[@]}" \
    -DskipTests compile dependency:build-classpath -DincludeScope=test \
    -Dmdep.outputFile="$project_dir/build/dependencies.txt"
classes_dir=$(mktemp -d "$project_dir/build/experiments.XXXXXX")
# Freeze framework bytecode so another Maven compilation cannot disrupt a live study.
if [[ -n "${TAG_JAR:-}" ]]; then
    cp "$TAG_JAR" "$classes_dir/framework.jar"
else
    jar --create --file "$classes_dir/framework.jar" -C "$tag_dir/target/classes" .
fi
framework_path="$classes_dir/framework.jar:$(cat "$project_dir/build/dependencies.txt")"
javac --release 21 -proc:none -cp "$framework_path" -d "$classes_dir" \
    "$project_dir"/sushigoagent/*.java "$project_dir"/experiments/coursework/experiments/*.java
printf '%s\n' "$classes_dir:$framework_path" > "$project_dir/build/study-classpath.txt"
