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
#/ run.sh - Helper script to run Quarkus in development mode with Keycloak
#/
#/ Usage:
#/   run.sh [arguments...]
#/
#/ OPTIONS:
#/   -h, --help                       Show this help message and exit
#/   -e, --container-engine <engine>  Specify which container engine to use (docker or podman)
#/   -f, --force                      Force a restart of the Keycloak container and restart the Quarkus application
#/   -p, --profile <profile>          Use a specific profile for running services
#/   --cache <cache>                  Enable or disable caching features
#/   --db <database>                  Specify which database to use
#/   --logging <logging>              Enable or disable logging
#/   --metrics <metrics>              Enable or disable metrics
#/   --search <search>                Enable or disable search features
#/   --sso <sso_provider>             Specify single sign-on provider
#/   --tracing <tracing>              Enable or disable tracing
#/
#
run() {
  local profile="dev"

  local cache=false
  local db="derby"
  local logging=false
  local metrics=false
  local search=false
  local sso="keycloak"
  local tracing=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h|--help)
        echoHelp
        exit 0
        ;;
      -e|--container-engine)
        containerEngine="${2}"
        shift
        ;;
      -f|--force)
        restart_quarkus
        restart_keycloak
        shift
        ;;
      -p|--profile)
        profile="${2}"
        shift
        ;;
      --cache)
        cache="${2}"
        shift
        ;;
      --db)
        db="${2}"
        shift
        ;;
      --logging)
        logging="${2}"
        shift
        ;;
      --metrics)
        metrics="${2}"
        shift
        ;;
      --search)
        search="${2}"
        shift
        ;;
      --sso)
        sso="${2}"
        shift
        ;;
      --tracing)
        tracing="${2}"
        shift
        ;;
      *)
        echo "Unknown option: $1"
        exit 1
        ;;
    esac
  done

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

  set -a  # Automatically export all variables
  source .env.dev
  set +a
  podman-compose -f ./config/compose/sso/keycloak."${profile}".compose.yml up -d
  . ./keycloak.sh
  ./gradlew quarkusDev
}
run "$@"

# Function to display usage information
echoHelp() {
  grep '^#/' "$0" | cut -c 4-
}

loadServices() {
  local composeFiles=()
  [[ "$db" != "" ]] && composeFiles+=("./compose/db/${db}.compose.yml")
  [[ "$cache" != "" ]] && composeFiles+=("./compose/cache/${cache}.compose.yml")
  [[ "$logging" != "" ]] && composeFiles+=("./compose/logging/${logging}.compose.yml")
  [[ "$metrics" == true ]] && composeFiles+=("./compose/metrics/prometheus.compose.yml")
  [[ "$search" != "" ]] && composeFiles+=("./compose/search/${search}.compose.yml")
  [[ "$sso" != "" ]] && composeFiles+=("./compose/sso/keycloak.${profile}.compose.yml")
  [[ "$tracing" == true ]] && composeFiles+=("./compose/tracing/jaeger.compose.yml")

  for file in "${composeFiles[@]}"; do
    if [ -f "$file" ]; then
      echo "Starting service using $file"
      "${containerEngine}-compose" -f "$file" up -d
    else
      echo "Compose file $file does not exist."
    fi
  done
}

# Function to restart Quarkus application
restart_quarkus() {
  echo "Restarting Quarkus application..."
  ./gradlew --stop  # Stop any running Quarkus gradle task
  ./gradlew quarkusDev --daemon  # Start the Quarkus development server in the background
}

# Function to restart Keycloak container
restart_keycloak() {
  echo "Restarting Keycloak container..."
  podman-compose -f ./config/compose/sso/keycloak."$profile".compose.yml restart keycloak
}