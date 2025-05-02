#!/usr/bin/env bash

./mvnw -DskipTests -Pnative spring-boot:build-image

#docker run docker.io/library/oracle:0.0.1-SNAPSHOT

docker run -e SPRING_AI_OPENAI_API_KEY=$SPRING_AI_OPENAI_API_KEY  \
  -e SPRING_DATASOURCE_URL=jdbc:oracle:thin:@host.docker.internal:1521/FREEPDB1 docker.io/library/oracle:0.0.1-SNAPSHOT
