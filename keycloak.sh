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

# lsof -i -P -n | grep LISTEN | grep -i 8543

# TODO: Check if the port 8543 is already in use and throw an error if it is. Make it easier to detect the problem.
# TODO: In the `else` check if it is container is running. podman ps | grep -i keycloak

podman run --rm \
  -p 8543:8443 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v ./config/SunsetERP-realm.json:/opt/keycloak/conf/SunsetERP-realm.json \
  -v "$(pwd)"/config/keycloak-keystore.jks:/etc/keycloak-keystore.jks \
  --name keycloak \
  quay.io/keycloak/keycloak:24.0.3 \
  -Dkeycloak.migration.action=import \
  -Dkeycloak.migration.provider=singleFile \
  -Dkeycloak.migration.file=/opt/keycloak/conf/SunsetERP-realm.json \
  -Dkeycloak.migration.strategy=OVERWRITE_EXISTING \
  start --hostname-strict=false --https-key-store-file=/etc/keycloak-keystore.jks --optimized

# old:
# -v ./sunseterp-client.json:/opt/keycloak/conf/sunseterp-client.json \

# Export realm:
# podman exec -it keycloak bash -c "/opt/keycloak/bin/kc.sh export --log-level off --file /opt/keycloak/bin/SunsetERP-realm.json --realm SunsetERP 2>/dev/null && cat /opt/keycloak/bin/SunsetERP-realm.json" > SunsetERP-realm-2.json