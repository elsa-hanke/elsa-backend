# ELSA-palvelun backend

### Justfile-komennot 

Projektissa on valmiita kehityskomentoja `Justfile`-tiedostossa.

- `just br` (build-run): ajaa ensin buildin ja käynnistää sen jälkeen backendin sekä frontendin
- `just r` (run): käynnistää backendin ja frontendin ilman build-vaihetta
- `just restartb`: käynnistää backendin uudelleen (tappaa ensin portin 8080 prosessin ja aiemman `bootRun`-prosessin) .
- `just psql`: avaa PostgreSQL:n `psql`-yhteyden Docker-kontista. Lisäparametreja voi antaa muodossa `just psql -- +args='-c "SELECT 1"'`.

## Kehittäminen

Käynnistä Postgres tietokanta komennolla:

```
docker-compose -f infra/postgresql.yml up -d
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

Testaa tuotantoversion toimivuus komennolla:

```
java --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar build/libs/*.jar
```

## Tuotantoversion kontin rakentaminen

Rakenna tuotantoversion kontti komennolla:

```
./gradlew bootJar -Pprod jibDockerBuild
```

## Testaaminen

Suorita applikaation testit komennolla:

```
./gradlew test integrationTest jacocoTestReport
```

Tietokantaa käyttävät testit käynnistävät automaattisesti PostgreSQL 16.9 -kontin
Testcontainersilla. Dockerin on oltava käynnissä myös paikallisessa ajossa.
Erillistä profiilia tai `-Ptestcontainers`-valitsinta ei tarvita. Testit käyttävät
väliaikaista tietokantaa, joka poistetaan testiprosessin päättyessä. Kontti jaetaan
saman testiprosessin Spring-kontekstien kesken; ShedLock-testit käyttävät omaa
väliaikaista tietokantaansa. Tietokantaa tarvitsemattomat testit eivät käynnistä
PostgreSQL-konttia.

Testiskeema luodaan tuotannon Liquibase-muutoksista ilman Hibernaten automaattisia
skeemamuutoksia. Testien `test-master.xml` suorittaa lisäksi kahden
sarakekohtaisen sekvenssin kohdistuksen ladatun viitedatan suurimpaan ID-arvoon.
Envers käyttää samoja audit-taulujen ja revision-sarakkeiden nimiä kuin tuotannossa.

### Ulkoiset integraatiotestit

Ulkoiset integraatiotestit testaavat etäpalveluita eivätkä käytä paikallista
tietokantaa. `external-integration`-profiili poistaa JDBC-, JPA- ja Liquibase-
autokonfiguraation käytöstä, joten CodeBuildissa ei tarvita Dockeria.

Ulkoiset integraatiotestit suoritetaan CodeBuildissa komennolla:

```
./gradlew externalIntegrationTests
```

Opintotietointegraatioiden testihenkilöt annetaan `EXTERNAL_INTEGRATION_<PALVELU>_HETU`-ympäristömuuttujilla. Henkilötunnuksia ei tulosteta testilokeihin. Vakaita testidatan tarkistuspisteitä voi määrittää palvelukohtaisesti seuraavilla valinnaisilla ympäristömuuttujilla:

- `EXTERNAL_INTEGRATION_<PALVELU>_EXPECTED_STUDY_RIGHT_ID`
- `EXTERNAL_INTEGRATION_<PALVELU>_EXPECTED_COURSE_CODE`
- `EXTERNAL_INTEGRATION_<PALVELU>_EXPECTED_PROGRAMME_IDENTIFIER`

`<PALVELU>` on `PEPPI_OULU`, `PEPPI_TURKU`, `PEPPI_UEF`, `SISU_HY` tai `SISU_TRE`. Yliopisto ja palvelukohtainen `assert-erikoisala-tunniste-list`-sopimus tarkistetaan aina, vaikka valinnaisia tarkistuspisteitä ei olisi määritetty.

Arkistoinnin ulkoiset testit luovat oikeita testiaineistoja Helsingin Siiloon ja Tampereen Louheen. Tapaus- ja tiedostonimet sisältävät `CODEBUILD_BUILD_ID`:stä muodostetun korrelaatiotunnisteen, jotta lähetykset voidaan jäljittää.

### Valmistumisen asynkroninen arkistointi

Valmistumisen hyväksyntä käyttää oletuksena nykyistä arkistointitoimitusta. Uusi jono otetaan käyttöön yliopistokohtaisesti asetuksella `application.arkistointi.<yliopisto>.metadata.cases.valmistuminen.async-enabled=true`; myös integraation `kaytossa`-asetuksen on oltava päällä. Yliopiston avain on `tre`, `hki`, `oulu`, `turku` tai `uef`.

Pidä `async-enabled=false`, kunnes kyseisen yliopiston job-adapteri on toteutettu ja otettu käyttöön. `ArkistointiJobProcessor` on toteutettu, mutta konkreettiset toimitusadapterit ovat erillisiä tehtäviä. Pelkkä tämän muutoksen käyttöönotto ei kytke nykyisiä Siilo/Louhi-toimituksia pois. Jonopolku tallentaa hyväksynnän, asiakirjakopiot, hyväksymishetken metatiedot ja db-scheduler-ajastuksen samaan transaktioon.

Asetuksen palauttaminen arvoon `false` koskee uusia hyväksyntöjä. Jo jonossa olevat jobit säilyvät ja tarvitsevat edelleen taustakäsittelyn; niitä ei toimiteta automaattisesti vanhan synkronisen polun kautta. Nykyinen opiskelijan tiedosto säilyy muuttumattomana myös laillistamistodistuksen normalisoinnissa.

Taustakäsittelijä lataa tallennetun payloadin ja tarkistussummalla varmistetut asiakirjakopiot. Job siirtyy tilaan `KASITTELYSSA` lyhyessä transaktiossa; adapterin verkkokutsu suoritetaan transaktion ulkopuolella. Onnistuminen tallentaa tilan `LAHETETTY`, ulkoisen tunnisteen ja valmistumisajan ennen db-scheduler-suorituksen poistamista. Pysyvä virhe, puuttuva adapteri tai virheellinen aineisto johtaa tilaan `VAATII_TOIMENPITEITA` ilman automaattista uusintaa.

Tilapäisten virheiden uusinnat käyttää db-schedulerin `consecutive_failures`-laskuria ja failure/dead-execution-käsittelijöitä. Asetukset ovat `application.arkistointi.retry.max-attempts` (oletus 5, sisältää ensimmäisen yrityksen sekä keskeytyneet suoritukset), `initial-delay` (1m), `multiplier` (2.0) ja `max-delay` (1h). Viimeisen sallitun yrityksen epäonnistuminen johtaa tilaan `VAATII_TOIMENPITEITA`. Tietokantahäiriön aikana suoritus säilyy palautettavana, kunnes myös lopputila voidaan tallentaa.

Toimitus on at-least-once: jos ulkoinen arkisto hyväksyy aineiston mutta paikallinen onnistumisen tallennus keskeytyy, adapteria voidaan kutsua uudelleen samalla `ArkistointiSubmission.key`-avaimella. Adapterin ja vastaanottavan järjestelmän on estettävä kaksoistoimitus tällä avaimella tai tarkistettava aiemman toimituksen tulos. Pelkkä db-scheduler ei takaa exactly-once-toimitusta. Adapterin virhekoodit ja kuvaukset eivät saa sisältää henkilötietoja, tiedostonimiä, tunnuksia tai HTTP-vastausten sisältöä.

Virhe- ja palautuskäsittelijä lukitsee nykyisen `scheduled_tasks`-version tilapäisesti samassa transaktiossa jobin tilamuutoksen kanssa. Vanhentunut käsittelijä ei muuta jobia eikä uudempaa ajastusta. Kaikki jobin tilamuutokset lukitsevat job-rivin ennen tilan tarkistamista, joten odottanut kirjoittaja näkee myös sillä välin tallennetun lopputilan. Lukitukset vapautuvat ennen verkkokutsua ja ennen db-schedulerin reschedule/remove-toimintoa.

### Koodin laadun analysointi

Sonaria käytetään koodin laadun analysointiin. Voit käynnistää paikallisen Sonar palvelimen (
käytettävissä http://localhost:9001) komennolla:

```
docker-compose -f src/main/docker/sonar.yml up -d
```

Asenna Dependency-Check lisäosa Sonarin marketplacesta. Tämän jälkeen suorita analyysi:

```
./gradlew -Pprod clean check jacocoTestReport dependencyCheckAnalyze sonarqube
```
