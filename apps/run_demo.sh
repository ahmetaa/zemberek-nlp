#!/usr/bin/env bash
# Launches the Zemberek NLP Web Demo Studio (http://127.0.0.1:8080) from zemberek-apps.
# Usage:
#   ./apps/run_demo.sh [--port 8080] [--host 127.0.0.1] [--model-dir <dir>] [--norm-root <dir>]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

CLASSES_DIR="${REPO_ROOT}/apps/target/demo_classes"
RAW_CP_FILE="${REPO_ROOT}/apps/target/demo_raw.cp"
DEPS_CP_FILE="${REPO_ROOT}/apps/target/demo_deps.cp"
SENTINEL_FILE="${CLASSES_DIR}/.full_compile_done"

mkdir -p "${CLASSES_DIR}"

if [[ ! -f "${DEPS_CP_FILE}" || "${REPO_ROOT}/apps/pom.xml" -nt "${DEPS_CP_FILE}" ]]; then
  echo "[run_demo.sh] Resolving third-party Maven dependencies for zemberek-apps..."
  rm -f "${RAW_CP_FILE}"
  mvn -f "${REPO_ROOT}/pom.xml" -pl apps -am -q compile dependency:build-classpath \
    -Dmdep.outputFile="${RAW_CP_FILE}" \
    -Dmdep.appendOutput=true
  tr ':' '\n' < "${RAW_CP_FILE}" \
    | sed '/^$/d' \
    | grep '\.jar$' \
    | grep -v '/zemberek-' \
    | sort -u \
    | paste -sd: - > "${DEPS_CP_FILE}"
fi

DEPS_CP="$(cat "${DEPS_CP_FILE}")"
RESOURCES_CP="${REPO_ROOT}/apps/src/main/resources:${REPO_ROOT}/normalization/src/main/resources:${REPO_ROOT}/morphology/src/main/resources:${REPO_ROOT}/tokenization/src/main/resources:${REPO_ROOT}/core/src/main/resources"

if [[ ! -f "${SENTINEL_FILE}" ]]; then
  echo "[run_demo.sh] Compiling Zemberek core/tokenization/lm/morphology/normalization/apps demo..."
  SRC_LIST="${REPO_ROOT}/apps/target/demo_sources_all.txt"
  find \
    "${REPO_ROOT}/core/src/main/java" \
    "${REPO_ROOT}/tokenization/src/main/java" \
    "${REPO_ROOT}/lm/src/main/java" \
    "${REPO_ROOT}/morphology/src/main/java" \
    "${REPO_ROOT}/morphology/src/main/generated/java" \
    "${REPO_ROOT}/normalization/src/main/java" \
    "${REPO_ROOT}/apps/src/main/java/zemberek/apps/demo" \
    -name '*.java' > "${SRC_LIST}"
  echo "${REPO_ROOT}/apps/src/main/java/zemberek/apps/ConsoleApp.java" >> "${SRC_LIST}"
  echo "${REPO_ROOT}/apps/src/main/java/zemberek/apps/ApplicationRunner.java" >> "${SRC_LIST}"

  javac --release 21 -encoding UTF-8 -cp "${DEPS_CP}" -d "${CLASSES_DIR}" @"${SRC_LIST}"
  touch "${SENTINEL_FILE}"
else
  INC_LIST="${REPO_ROOT}/apps/target/demo_sources_inc.txt"
  find \
    "${REPO_ROOT}/normalization/src/main/java" \
    "${REPO_ROOT}/apps/src/main/java/zemberek/apps/demo" \
    -name '*.java' > "${INC_LIST}"
  echo "${REPO_ROOT}/apps/src/main/java/zemberek/apps/ConsoleApp.java" >> "${INC_LIST}"
  echo "${REPO_ROOT}/apps/src/main/java/zemberek/apps/ApplicationRunner.java" >> "${INC_LIST}"

  javac --release 21 -encoding UTF-8 -cp "${CLASSES_DIR}:${DEPS_CP}" -d "${CLASSES_DIR}" @"${INC_LIST}"
fi

exec java -Xmx4g \
  -cp "${CLASSES_DIR}:${RESOURCES_CP}:${DEPS_CP}" \
  zemberek.apps.demo.ZemberekDemoServer "$@"
