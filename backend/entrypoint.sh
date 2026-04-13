#!/bin/bash

# Convert Render's DATABASE_URL to Spring JDBC format
# Render gives: postgres://user:password@host:port/dbname
# Spring needs: jdbc:postgresql://host:port/dbname (with separate user/password)
if [ -n "$DATABASE_URL" ]; then
  # Replace postgres:// or postgresql:// with jdbc:postgresql://
  JDBC_URL=$(echo "$DATABASE_URL" | sed -E 's|^postgres(ql)?://([^@]+)@|jdbc:postgresql://|')
  export SPRING_DATASOURCE_URL="$JDBC_URL"
fi

exec java -Dspring.profiles.active=prod -jar app.jar
