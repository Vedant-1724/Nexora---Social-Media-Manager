#!/usr/bin/env sh

set -eu

WRAPPER_DIR="$(CDPATH= cd -- "$(dirname "$0")/.mvn/wrapper" && pwd)"
DISTRIBUTION_DIR="$WRAPPER_DIR/apache-maven"
DISTRIBUTION_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
MAVEN_HOME="$DISTRIBUTION_DIR/apache-maven-3.9.9"
ARCHIVE_PATH="$WRAPPER_DIR/apache-maven-3.9.9-bin.zip"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$DISTRIBUTION_DIR"
  if [ ! -f "$ARCHIVE_PATH" ]; then
    curl -fsSL "$DISTRIBUTION_URL" -o "$ARCHIVE_PATH"
  fi
  unzip -oq "$ARCHIVE_PATH" -d "$DISTRIBUTION_DIR"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
