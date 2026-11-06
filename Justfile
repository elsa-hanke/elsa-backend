set dotenv-load := true

build:
  ./gradlew clean build -x test -x integrationTest

test:
  ./gradlew clean test integrationTest detekt detektTest

itest:
  ./gradlew clean integrationTest

detekt:
  ./gradlew clean detekt

detektTest:
  ./gradlew clean detektTest

init:
  @docker volume create elsa_db_data > /dev/null 2>&1 || true
  @docker network create elsa > /dev/null 2>&1 || true

clean-db:
  #!/usr/bin/env sh
  docker volume rm elsa_db_data || true

start-db: init
  docker-compose -f infra/postgresql.yml up -d

stop-db:
  docker-compose -f infra/postgresql.yml down

startb: kill8080 start-db
 ./gradlew bootRun

startb-replica:
  @exec env -u APPLICATION_SECURITY_ENCODED_KEY just --dotenv-path .env.replica _startb-replica

_startb-replica: kill8080
  #!/usr/bin/env sh
  set -eu

  if [ "${REPLICA_CONNECTION_CONFIRMED:-}" != "yes" ]; then
    echo "Refusing to connect: set REPLICA_CONNECTION_CONFIRMED=yes after verifying that the target is the dev replica." >&2
    exit 1
  fi

  : "${REPLICA_DB_URL:?Set REPLICA_DB_URL to the JDBC URL of the dev replica or local SSM tunnel.}"
  : "${REPLICA_DB_USERNAME:?Set REPLICA_DB_USERNAME to the replica-only database user.}"
  : "${REPLICA_DB_PASSWORD:?Set REPLICA_DB_PASSWORD to the replica-only database password.}"

  export SPRING_PROFILES_ACTIVE=dev,replica

  ./gradlew bootRun

srt:
  @exec just --dotenv-path .env.replica _start-replica-tunnel

_start-replica-tunnel:
  #!/usr/bin/env sh
  set -eu

  : "${AWS_PROFILE:?Set AWS_PROFILE to the approved dev AWS SSO profile.}"
  : "${REPLICA_SSM_TARGET:?Set REPLICA_SSM_TARGET to the dev managed-instance or bastion ID.}"
  : "${REPLICA_DB_HOST:?Set REPLICA_DB_HOST to the private replica RDS hostname.}"

  replica_db_port="${REPLICA_DB_PORT:-5432}"
  replica_local_port="${REPLICA_LOCAL_PORT:-15432}"

  exec aws ssm start-session \
    --profile "$AWS_PROFILE" \
    --target "$REPLICA_SSM_TARGET" \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"host\":[\"$REPLICA_DB_HOST\"],\"portNumber\":[\"$replica_db_port\"],\"localPortNumber\":[\"$replica_local_port\"]}"

restartb:
  just kill8080
  pkill -f bootRun || true
  just startb

frontend-install:
  cd frontend && yarn install --ignore-scripts

frontend-test:
  cd frontend && yarn test:unit

frontend-lint:
  cd frontend && yarn lint

frontend-lintfix:
  cd frontend && yarn lint:fix

frontend-build:
  cd frontend && yarn build --mode production-test

frontend-build-prod:
  cd frontend && yarn build

startf: frontend-install
  cd frontend && yarn serve

kill8080:
  @lsof -ti:8080 | xargs kill -9 2>/dev/null || true

kill9060:
  @lsof -ti:9060 | xargs kill -9 2>/dev/null || true

sbom:
  ./gradlew clean cyclonedxBom -x test -x integrationTest

sscan: sbom
  grype -o table --fail-on high ./build/reports/cyclonedx-direct/bom.json

br:
  #!/usr/bin/env sh
  trap 'kill 0' INT TERM EXIT
  just build
  just startb &
  just startf &
  wait

# Current checkout with GHA archive settings and an isolated disposable database.
br-e2e:
  #!/usr/bin/env bash
  set -euo pipefail
  for port in 8080 9060; do
    if lsof -tiTCP:"$port" -sTCP:LISTEN >/dev/null; then
      echo "Port $port is occupied. Stop the existing backend/frontend before just br-e2e." >&2
      exit 1
    fi
  done
  # Give each child its own process group so Ctrl-C also stops Gradle/Vue descendants.
  set -m
  backend_pid=''
  frontend_pid=''
  cleanup() {
    trap - INT TERM EXIT
    [ -z "$backend_pid" ] || kill -- -"$backend_pid" 2>/dev/null || true
    [ -z "$frontend_pid" ] || kill -- -"$frontend_pid" 2>/dev/null || true
  }
  trap cleanup EXIT
  trap 'exit 130' INT
  trap 'exit 143' TERM
  just startb-e2e &
  backend_pid=$!
  just startf &
  frontend_pid=$!
  while kill -0 "$backend_pid" 2>/dev/null && kill -0 "$frontend_pid" 2>/dev/null; do
    sleep 1
  done
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    wait "$backend_pid"
  else
    wait "$frontend_pid"
  fi

start-db-e2e:
  E2E_DB_PORT=127.0.0.1:15434 docker compose -p elsa-e2e-source -f infra/docker-compose-cicd.yml up -d --wait --wait-timeout 60 db

stop-db-e2e:
  E2E_DB_PORT=127.0.0.1:15434 docker compose -p elsa-e2e-source -f infra/docker-compose-cicd.yml down

startb-e2e: start-db-e2e
  ./gradlew bootRun --args='--spring.profiles.active=dev,e2e-archive --spring.config.additional-location=file:./infra/application-e2e.yml --spring.datasource.url=jdbc:postgresql://127.0.0.1:15434/elsaBackend --spring.datasource.username=elsaBackend --spring.datasource.password= --server.port=8080 --server.servlet.session.cookie.secure=false'

brr:
  #!/usr/bin/env sh
  set -eu
  trap 'kill 0' INT TERM EXIT
  just build
  just startb-replica &
  just startf &
  wait

r:
  #!/usr/bin/env sh
  trap 'kill 0' INT TERM EXIT
  just startb &
  just startf &
  wait

# Open against br-e2e; always target its disposable DB and the source frontend.
e2e:
  cd e2e && env CYPRESS_DB_HOST=127.0.0.1 CYPRESS_DB_PORT=15434 CYPRESS_DB_NAME=elsaBackend CYPRESS_DB_USER=elsaBackend CYPRESS_DB_PASSWORD= yarn run cy:open --config baseUrl=http://localhost:9060

e2e-open: e2e

e2e-run:
  cd e2e && env CYPRESS_DB_HOST=127.0.0.1 CYPRESS_DB_PORT=15434 CYPRESS_DB_NAME=elsaBackend CYPRESS_DB_USER=elsaBackend CYPRESS_DB_PASSWORD= yarn run cy:run --browser chrome --config baseUrl=http://localhost:9060

# Optional reproduction against published images, as in GHA.
e2e-images: up-e2e
  cd e2e && env CYPRESS_DB_HOST=127.0.0.1 CYPRESS_DB_PORT=15433 CYPRESS_DB_NAME=elsaBackend CYPRESS_DB_USER=elsaBackend CYPRESS_DB_PASSWORD= yarn run cy:open --config baseUrl=http://localhost:18080

up-e2e:
  E2E_DB_PORT=127.0.0.1:15433 E2E_FRONTEND_PORT=18080 E2E_BACKEND_PORT=19080 docker compose -p elsa-e2e-local -f ./infra/docker-compose-cicd.yml up -d --wait --wait-timeout 300

down-e2e:
  E2E_DB_PORT=15433 E2E_FRONTEND_PORT=18080 E2E_BACKEND_PORT=19080 docker compose -p elsa-e2e-local -f ./infra/docker-compose-cicd.yml down

pullt:
  echo $(aws ecr get-login-password --profile elsadev)|docker login --password-stdin --username AWS 939452229770.dkr.ecr.eu-west-1.amazonaws.com
  docker-compose -f ./infra/docker-compose-cicd.yml pull --ignore-pull-failures
  echo $(aws ecr get-login-password --profile elsashared)|docker login --password-stdin --username AWS 654795471143.dkr.ecr.eu-west-1.amazonaws.com
  docker-compose -f ./infra/docker-compose-cicd.yml pull --ignore-pull-failures

psql +args='':
  @docker run                                                 \
    --rm -it                                                  \
    --network elsa                                            \
    -v $(pwd)/.psqlrc:/root/.psqlrc                           \
    -v $(pwd):/app                                            \
    -w=/app                                                   \
    postgres:16-alpine                                        \
    psql -h elsabackend-postgresql                            \
         -p 5432                                              \
         -U elsaBackend                                       \
         {{ args }}

psqlr +args='':
  @exec just --dotenv-path .env.replica -- _psqlr {{ args }}

_psqlr +args='':
  #!/usr/bin/env sh
  set -eu

  : "${REPLICA_DB_URL:?Set REPLICA_DB_URL to the JDBC URL of the local replica tunnel.}"
  : "${REPLICA_DB_USERNAME:?Set REPLICA_DB_USERNAME to the replica-only database user.}"
  : "${REPLICA_DB_PASSWORD:?Set REPLICA_DB_PASSWORD to the replica-only database password.}"

  replica_psql_url="${REPLICA_DB_URL#jdbc:}"
  case "$replica_psql_url" in
    postgresql://localhost:*/*|postgresql://127.0.0.1:*/*) ;;
    *)
      echo "Refusing to connect: REPLICA_DB_URL must use localhost or 127.0.0.1." >&2
      exit 1
      ;;
  esac

  export PGPASSWORD="$REPLICA_DB_PASSWORD"
  exec psql "$replica_psql_url" --username "$REPLICA_DB_USERNAME" {{ args }}
