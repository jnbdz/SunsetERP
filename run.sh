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
#/
#

# Function to display usage information
echoHelp() {
  grep '^#/' "$0" | cut -c 4-
}

verifyContainerEngine() {
  if ! command -v "${containerEngine}" &>/dev/null; then
    echo "Container engine ${containerEngine} not found. Trying alternate."
    containerEngine=$(if command -v podman &>/dev/null; then echo "podman"; elif command -v docker &>/dev/null; then echo "docker"; else echo ""; fi)

    if [ -z "${containerEngine}" ]; then
      echo "Neither Docker nor Podman is installed. Please install one of them."
      exit 1
    else
      echo "Using ${containerEngine} as the container engine."
    fi
  fi
}

loadEnv() {
  set -a  # Automatically export all variables
  source .env.dev
  set +a
}

loadServices() {
  local composeConfigPath="./config/compose"
  local composeFiles=()
  ## DB
  [[ "${SUNSETERP_MYSQL_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/db/mysql.compose.yml")
  [[ "${SUNSETERP_POSTGRESQL_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/db/postgresql.compose.yml")
  [[ "${SUNSETERP_MARIADB_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/db/mariadb.compose.yml")
  [[ "${SUNSETERP_PERCONA_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/db/percona.compose.yml")

  ## Cache
  [[ "${SUNSETERP_REDIS_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/cache/redis.compose.yml")
  [[ "${SUNSETERP_IGNITE_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/cache/ignite.compose.yml")
  [[ "${SUNSETERP_HAZELCAST_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/cache/hazelcast.compose.yml")

  ## Logging
  [[ "${SUNSETERP_LOGSTASH_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/logging/logstash-oss.compose.yml")
  [[ "${SUNSETERP_FLUENTD_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/logging/fluentd.compose.yml")
  [[ "${SUNSETERP_FILEBEAT_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/logging/beats.compose.yml")
  [[ "${SUNSETERP_FLUME_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/logging/flume.compose.yml")

  ## Metrics
  [[ "${SUNSETERP_PROMETHEUS_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/metrics/prometheus.compose.yml")

  ## Tracing
  [[ "${SUNSETERP_JAEGER_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/tracing/jaeger.compose.yml")

  ## SSO
  [[ "${SUNSETERP_KEYCLOAK_ACTIVATE}" == "true" ]] && composeFiles+=("${composeConfigPath}/sso/keycloak.dev.compose.yml")

  for file in "${composeFiles[@]}"; do
    if [ -f "$file" ]; then
      echo "Starting service using $file"
      "${containerEngine}-compose" -f "$file" up -d
    else
      echo "Compose file $file does not exist."
    fi
  done
}

run() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h|--help)
        echoHelp
        exit 0
        ;;
      -e|--container-engine)
        containerEngine="${2}"
        shift 2
        ;;
      *)
        echo "Unknown option: $1"
        exit 1
        ;;
    esac
  done

  verifyContainerEngine
  loadEnv
  loadServices

  echo "Start SunsetERP"
  ./gradlew quarkusDev  # Start the Quarkus development server in the background
}

# Call run function if the script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run "$@"
fi