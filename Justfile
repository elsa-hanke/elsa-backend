version:
  java --version && ./gradlew --version

build:
  ./gradlew clean build

build-skiptest:
  ./gradlew build -x test -x integrationTest

test:
  ./gradlew clean test

itest:
  ./gradlew clean integrationTest

start-db:
  docker-compose -f src/main/docker/postgresql.yml up -d

start-backend: start-db
 ./gradlew bootRun
