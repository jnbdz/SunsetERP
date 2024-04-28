#!/usr/bin/env sh

#
# Copyright 2024 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#podman run \
#  --rm \
#  -p 8080:8080 \
#  -e KEYCLOAK_DATABASE_VENDOR=dev-file \
#  -e KEYCLOAK_USER=admin \
#  -e KEYCLOAK_PASSWORD=admin \
#  --name keycloak bitnami/keycloak:latest

#podman run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:24.0.3 start-dev

#podman run --rm \
#  --name keycloak \
#  -e KEYCLOAK_ADMIN=admin \
#  -e KEYCLOAK_ADMIN_PASSWORD=admin \
#  -p 8543:8443 \
#  -v "$(pwd)"/config/keycloak-keystore.jks:/etc/keycloak-keystore.jks \
#  quay.io/keycloak/keycloak:24.0.3 start  \
#  --hostname-strict=false \
#  --https-key-store-file=/etc/keycloak-keystore.jks




  podman run --rm \
    -p 8543:8443 \
    -e KEYCLOAK_ADMIN=admin \
    -e KEYCLOAK_ADMIN_PASSWORD=admin \
    -v ./sunseterp-client.json:/opt/keycloak/conf/sunseterp-client.json \
    -v "$(pwd)"/config/keycloak-keystore.jks:/etc/keycloak-keystore.jks \
    --name keycloak \
    quay.io/keycloak/keycloak:24.0.3 \
    -Dkeycloak.migration.action=import \
    -Dkeycloak.migration.provider=singleFile \
    -Dkeycloak.migration.file=/opt/keycloak/conf/sunseterp-client.json \
    -Dkeycloak.migration.strategy=OVERWRITE_EXISTING \
    start --hostname-strict=false --https-key-store-file=/etc/keycloak-keystore.jks --optimized