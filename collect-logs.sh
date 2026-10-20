#!/usr/bin/env bash

FROM=$1
TO=$2

SOURCE_DIR="./test-output"
TARGET_DIR="./collected-logs"

rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR"

find "$SOURCE_DIR" \
  -type f \
  -name "*_run.log" \
  -newermt "$FROM" \
  ! -newermt "$TO" \
  -exec cp {} "$TARGET_DIR" \;

echo ">>> Логи за период собраны в: $TARGET_DIR"
