set dotenv-load := true

build:
  ./gradlew clean build -x test -x integrationTest

test:
  ./gradlew clean test integrationTest

itest:
  ./gradlew clean integrationTest

start-db:
  docker-compose -f src/main/docker/postgresql.yml up -d

stop-db:
  docker-compose -f src/main/docker/postgresql.yml down -v

sb: start-db
 ./gradlew bootRun

rb:
  pkill -f bootRun || true
  just sb

sf:
  cd ../elsa-frontend && yarn serve

br:
  just build
  just sb &
  just sf &
  wait

r:
  just sb &
  just sf &
  wait

e2e:
  cd e2e && yarn run cy:open

e2e-run:
  cd e2e && yarn run cy:run

