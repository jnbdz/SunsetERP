#!/usr/bin/env bash

#####################################################################
# Licensed to the SiteNetSoft + SunsetERP (SNS+SERP) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SNS+SERP licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#####################################################################

# Run Keycloak with a realm
run_keycloak() {
  local realm="${1:-SunsetERP}"
  local realmFile="${realm}-realm.json"
  local realmExportPath="${2:-./config/${realmFile}}"
  local keycloakVersion="24.0.3"

  # Check if the realm is provided
  if [ -z "${realm}" ]; then
    echo "Usage: run_keycloak <realm>"
    exit 1
  fi

  # Check if Podman or Docker is installed
  if [ -z "$(command -v podman)" ]; then
    if [ -z "$(command -v docker)" ]; then
      echo "Podman and Docker are not installed. Please install one of them."
      exit 1
    fi
  fi

  # Check if the Keycloak container is running
  if [ -n "$(command -v podman)" ]; then
    if [ -n "$(docker ps | grep -i keycloak)" ]; then
      echo "Keycloak container is running."
      exit 1
    fi
  else
    if [ -n "$(podman ps | grep -i keycloak)" ]; then
      echo "Keycloak container is running."
      exit 1
    fi
  fi

  # Check if the port is already in use
  if [ -n "$(lsof -i -P -n | grep LISTEN | grep -i 8543)" ]; then
    echo "Port 8543 is already in use. Please stop the process using the port."
    echo ""
    lsof -i -P -n | grep LISTEN | awk '{ print $1 " " $2 }' | column -t
    echo ""
    exit 1
  fi

  podman run --rm \
    -p 8543:8443 \
    -e KEYCLOAK_ADMIN=admin \
    -e KEYCLOAK_ADMIN_PASSWORD=admin \
    -v ./config/SunsetERP-realm.json:/opt/keycloak/conf/SunsetERP-realm.json \
    -v "$(pwd)"/config/keycloak-keystore.jks:/etc/keycloak-keystore.jks \
    --name keycloak \
    quay.io/keycloak/keycloak:${keycloakVersion} \
    -Dkeycloak.migration.action=import \
    -Dkeycloak.migration.provider=singleFile \
    -Dkeycloak.migration.file=/opt/keycloak/conf/SunsetERP-realm.json \
    -Dkeycloak.migration.strategy=OVERWRITE_EXISTING \
    start --hostname-strict=false --https-key-store-file=/etc/keycloak-keystore.jks --optimized

  # old:
  # -v ./sunseterp-client.json:/opt/keycloak/conf/sunseterp-client.json \

}
run_keycloak "$@"