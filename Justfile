set dotenv-load := true

build:
  ./gradlew clean build -x test -x integrationTest

test:
  ./gradlew clean test integrationTest

itest:
  ./gradlew clean integrationTest

init:
  @docker volume create elsa_db_data > /dev/null 2>&1 || true
  @docker network create elsa > /dev/null 2>&1 || true

clean-db:
  #!/usr/bin/env sh
  docker volume rm elsa_db_data || true

start-db: init
  docker-compose -f src/main/docker/postgresql.yml up -d

stop-db:
  docker-compose -f src/main/docker/postgresql.yml down

startb: kill8080 start-db
 ./gradlew bootRun

restartb:
  just kill8080
  pkill -f bootRun || true
  just startb

startf:
  cd ../elsa-frontend && yarn serve

kill8080:
  @lsof -ti:8080 | xargs kill -9 2>/dev/null || true

kill9060:
  @lsof -ti:9060 | xargs kill -9 2>/dev/null || true

br:
  #!/usr/bin/env sh
  trap 'kill 0' INT TERM EXIT
  just build
  just startb &
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

