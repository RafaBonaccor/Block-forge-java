#!/usr/bin/env sh
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SOURCE_DIR="$PROJECT_ROOT/java/src/blockforge"
OUTPUT_DIR="$PROJECT_ROOT/java/out"

if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
  echo "Java/Javac not found. Install JDK 21 or newer and make sure java and javac are on PATH." >&2
  exit 1
fi

if [ ! -d "$SOURCE_DIR" ]; then
  echo "Source directory not found: $SOURCE_DIR" >&2
  exit 1
fi

SOURCE_COUNT=$(find "$SOURCE_DIR" -name "*.java" | wc -l | tr -d " ")
if [ "$SOURCE_COUNT" = "0" ]; then
  echo "No Java sources found in $SOURCE_DIR" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo "Compiling Java sources..."
find "$SOURCE_DIR" -name "*.java" -exec javac -d "$OUTPUT_DIR" {} +

echo "Starting Blockforge Java..."
java -cp "$OUTPUT_DIR" blockforge.Main
