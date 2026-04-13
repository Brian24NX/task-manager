#!/bin/bash

# Convert Render's DATABASE_URL to Spring JDBC format
# Render gives: postgresql://user:password@host:port/dbname
# Spring needs: jdbc:postgresql://host:port/dbname
if [ -n "$DATABASE_URL" ]; then
  JDBC_URL=$(echo "$DATABASE_URL" | sed -E 's|^postgres(ql)?://([^@]+)@|jdbc:postgresql://|')
  export JDBC_DATABASE_URL="$JDBC_URL"
  echo "Database configured: PostgreSQL"
else
  echo "WARNING: DATABASE_URL not set, using H2 fallback"
fi

exec java -Dspring.profiles.active=prod -jar app.jar
