#!/usr/bin/env bash
set -a  # Automatically export all variables
source .env.dev
set +a
#podman run --rm -d -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:24.0.3 start-dev

./gradlew quarkusDev

# -v "$(pwd)"/config/keycloak-keystore.jks:/etc/keycloak-keystore.jks \
# --https-key-store-file=/etc/keycloak-keystore.jks \
# --hostname-strict=false \

# podman run --rm -it -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin --entrypoint /bin/bash quay.io/keycloak/keycloak:24.0.3

# docker run --name keycloak -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -p 8543:8443 -v "$(pwd)"/config/keycloak-keystore.jks:/etc/keycloak-keystore.jks quay.io/keycloak/keycloak:{keycloak.version} start  --hostname-strict=false --https-key-store-file=/etc/keycloak-keystore.jks