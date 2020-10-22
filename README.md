# ELSA-palvelun backend

[![Build Status](https://dev.azure.com/elsa-hanke/ELSA/_apis/build/status/elsa-hanke.elsa-backend?branchName=main)](https://dev.azure.com/elsa-hanke/ELSA/_build/latest?definitionId=4&branchName=main)


## Kehittäminen

Käynnistä Keycloak komennolla:
```
docker-compose -f src/main/docker/keycloak.yml up -d
```

Käynnistä Postgres tietokanta komennolla:
```
docker-compose -f src/main/docker/postgresql.yml up -d
```

Käynnistä applikaatio dev profiililla:
```
./gradlew
```


## Tuotantoversion rakentaminen

Rakenna tuotantoversion jar komennolla:
```
./gradlew -Pprod clean bootJar
```


## Testaaminen

Suorita applikaation testit komennolla:
```
./gradlew test integrationTest jacocoTestReport
```

### Koodin laadun analysointi
Sonaria käytetään koodin laadun analysointiin. Voit käynnistää paikallisen Sonar palvelimen (käytettävissä http://localhost:9001) komennolla:
```
docker-compose -f src/main/docker/sonar.yml up -d
```
Asenna Dependency-Check lisäosa Sonarin marketplacesta. Tämän jälkeen suorita analyysi:
```
./gradlew -Pprod clean check jacocoTestReport dependencyCheckAnalyze sonarqube
```
