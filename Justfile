set dotenv-load := true

build:
  ./gradlew clean build -x test -x integrationTest

test:
  ./gradlew clean test integrationTest detekt detektTest

itest:
  ./gradlew clean integrationTest

detekt:
  ./gradlew clean detekt

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

startb-replica: kill8080
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
  export SPRING_LIQUIBASE_ENABLED=false
  export APPLICATION_SCHEDULING_ENABLED=false
  export APPLICATION_STUDY_DATA_INTEGRATIONS_ENABLED=false
  export APPLICATION_MAIL_ENABLED=false
  export MANAGEMENT_CLOUDWATCH2_METRICS_EXPORT_ENABLED=false

  ./gradlew -Preplica bootRun

start-replica-tunnel:
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

br-replica:
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

e2e:
  cd e2e && yarn run cy:open

up-e2e: kill8080
  docker compose -f ./infra/docker-compose-cicd.yml up -d

down-e2e:
  docker compose -f ./infra/docker-compose-cicd.yml down

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
