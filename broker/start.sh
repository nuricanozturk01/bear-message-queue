#!/usr/bin/env bash
# Run BearMQ broker with all JVM flags Chronicle Queue needs on Java 17+.
# Usage: ./start.sh [optional spring args]
#   Example: ./start.sh --spring.profiles.active=prod
set -euo pipefail

JAR=$(ls target/broker-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo "No broker JAR found. Run: JAVA_HOME=/path/to/jdk21 mvn package -DskipTests"
  exit 1
fi

exec java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
  -jar "$JAR" "$@"
