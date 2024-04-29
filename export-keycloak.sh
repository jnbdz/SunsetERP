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

# Export a Keycloak realm
export_realm() {
  local realm="${1:-SunsetERP}"
  local realmFile="${realm}-realm.json"
  local realmExportPath="${2:-./config/${realmFile}}"

  # Check if the realm is provided
  if [ -z "${realm}" ]; then
    echo "Usage: export <realm>"
    return 1
  fi

  # Check if Podman or Docker is installed
  if [ -z "$(command -v podman)" ]; then
    if [ -z "$(command -v docker)" ]; then
      echo "Podman and Docker are not installed. Please install one of them."
      return 1
    fi
  fi

  # Check if the Keycloak container is running
  if [ -z "$(command -v podman)" ]; then
    if [ -z "$(docker ps | grep -i keycloak)" ]; then
      echo "Keycloak container is not running. Please start it."
      return 1
    fi
  else
    if [ -z "$(podman ps | grep -i keycloak)" ]; then
      echo "Keycloak container is not running. Please start it."
      return 1
    fi
  fi

  local kcPath="/opt/keycloak"
  local logOff="--log-level off"
  local cmd="${kcPath}/bin/kc.sh export ${logOff} --file ${kcPath}/${realm}-realm.json --realm ${realm} 2>/dev/null && cat ${kcPath}/${realm}-realm.json"

  # Export the realm
  if [ -z "$(command -v podman)" ]; then
    docker exec -it keycloak bash -c "${cmd}" > "${realmExportPath}"
  else
    podman exec -it keycloak bash -c "${cmd}" > "${realmExportPath}"
  fi
}
export_realm "$@"