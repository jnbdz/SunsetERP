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
#
#/ export-keycloak.sh - Helper script to run Quarkus in development mode with Keycloak
#/
#/ Usage:
#/   export-keycloak.sh [arguments...]
#/
#/ Examples:
#/   export-keycloak.sh clone git@github.com:user/repo.git - Clones using a selectable SSH host
#/   export-keycloak.sh remote set-url origin git@github.com:user/repo.git - Changes remote origin using a selectable SSH host
#/
#/ OPTIONS:
#/   -h, --help - Show this help message and exit
#/   -r, --realm - Realm name to export
#/   -o, --output-path - Path to export the realm
#/   -e, --container-engine - Keycloak container engine (podman or docker)
#/
#

# Export a Keycloak realm
export_realm() {
  local realm="SunsetERP"
  local realmFile="${realm}-realm.json"
  local realmExportPath="./config/${realmFile}"
  local containerEngine="podman"

  while [[ $# -gt 0 ]]; do
        case "$1" in
          -h|--help)
            echoHelp
            exit 0
            ;;
          -r|--realm)
            realm="${2}"
            shift
            ;;
          -o|--output-path)
            realmExportPath="${2}"
            shift
            ;;
          -e|--container-engine)
            containerEngine="${2}"
            shift
            ;;
          *)
            echo "Unknown option: $1"
            exit 1
            ;;
        esac
      done

  # Check if the realm is provided
  if [ -z "${realm}" ]; then
    echo "Usage: export <realm>"
    return 1
  fi

  # Check if the container engine provided is installed
  if [ "${containerEngine}" == "docker" ] && [ -z "$(command -v "${containerEngine}")" ]; then
    echo "Trying Podman since Docker does not seemed to be installed."
    containerEngine="podman"
  fi

  if [ "${containerEngine}" == "podman" ] && [ -z "$(command -v "${containerEngine}")" ]; then
    echo "Trying Docker since Podman does not seemed to be installed."
    containerEngine="docker"
  fi

  if [ "${containerEngine}" == "docker" ] && [ -z "$(command -v "${containerEngine}")" ]; then
    echo "Docker and Podman are not installed."
    exit 1
  fi

  if [ "${containerEngine}" != "podman" ] && [ "${containerEngine}" != "docker" ] && [ -n "$(command -v "${containerEngine}")" ]; then
    echo "Container engine ${containerEngine} is not installed."
  fi

  # Check if Podman or Docker is installed
  if [ -z "$(command -v podman)" ]; then
    if [ -z "$(command -v docker)" ]; then
      echo "Podman and Docker are not installed. Please install one of them."
      exit 1
    fi
  fi

  # Check if the Keycloak container is running
  if [ -z "$("${containerEngine}" ps | grep -i keycloak)" ]; then
    echo "Keycloak container is not running. Please start it."
    return 1
  fi

  local kcPath="/opt/keycloak"
  local logOff="--log-level off"
  local cmd="${kcPath}/bin/kc.sh export ${logOff} --file ${kcPath}/${realmFile} --realm ${realm} 2>/dev/null && cat ${kcPath}/${realmFile}"

  # Export the realm
  #if [ -z "$(command -v podman)" ]; then
  #  docker exec -it keycloak bash -c "${cmd}" > "${realmExportPath}"
  #else
  #  podman exec -it keycloak bash -c "${cmd}" > "${realmExportPath}"
  #fi

  "${containerEngine}" exec -it keycloak bash -c "${cmd}" > "${realmExportPath}"

  exit 0
}
export_realm "$@"

# Function to display usage information
echoHelp() {
  grep '^#/' "$0" | cut -c 4-
}