#!/bin/bash

# Convert Render's DATABASE_URL to Spring JDBC format
# Render gives: postgresql://user:password@host:port/dbname
# Spring needs: jdbc:postgresql://host:port/dbname?sslmode=require
if [ -n "$DATABASE_URL" ]; then
  JDBC_URL=$(echo "$DATABASE_URL" | sed -E 's|^postgres(ql)?://([^@]+)@|jdbc:postgresql://|')
  # Append sslmode if not already present
  if [[ "$JDBC_URL" != *"sslmode"* ]]; then
    JDBC_URL="${JDBC_URL}?sslmode=require"
  fi
  export SPRING_DATASOURCE_URL="$JDBC_URL"
  echo "Database URL configured: jdbc:postgresql://****"
fi

exec java -Dspring.profiles.active=prod -jar app.jar
