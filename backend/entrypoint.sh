#!/bin/bash

# If SPRING_DATASOURCE_URL is already set (e.g. by Render env var), use it directly.
# Otherwise, try to convert DATABASE_URL from postgres:// to jdbc:postgresql:// format.
if [ -z "$SPRING_DATASOURCE_URL" ] && [ -n "$DATABASE_URL" ]; then
  JDBC_URL=$(echo "$DATABASE_URL" | sed -E 's|^postgres(ql)?://([^@]+)@|jdbc:postgresql://|')
  if [[ "$JDBC_URL" != *"sslmode"* ]]; then
    JDBC_URL="${JDBC_URL}?sslmode=require"
  fi
  export SPRING_DATASOURCE_URL="$JDBC_URL"
fi

echo "Starting Task Manager API..."
exec java -Dspring.profiles.active=prod -jar app.jar
