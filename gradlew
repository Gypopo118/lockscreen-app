#!/usr/bin/env sh
set -e
if [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
else
  if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
  else
    echo "Gradle not found"
    exit 1
  fi
fi
