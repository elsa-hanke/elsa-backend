# ELSA-palvelun backend

## Asynkronisen arkistoinnin koodirakenne

Vaiheen 1 rajapinnat ja tietotyypit on jaettu vastuiden mukaan paketin
`service.arkistointi.job` alle:

- `creation`: jobin luontipyyntö sekä luonnin ja transaktionaalisen jonotuksen rajapinnat.
- `document`: asiakirjaviite, ladatun asiakirjan tietotyyppi ja latauksen rajapinta.
- `delivery`: adapterin rajapinta, toimituspyyntö ja toimituksen tulos.
- `processing`: jobin käsittelijän rajapinta.

Yhteinen `ArkistointiError` on job-paketin juuressa. Spring-konfiguraatio
`ArkistointiDbSchedulerConfiguration` ja jonotuksen toteutus
`DbSchedulerArkistointiJobScheduler` ovat erillisissä tiedostoissa paketissa
`config.arkistointi`. Testit seuraavat vastaavaa pakettijakoa.
Tämä vaihe määrittelee sopimukset; snapshotien ja käsittelijän toteutukset
kuuluvat myöhempiin tehtäviin.

## Asynkronisen arkistoinnin avaimet

`ArkistointiJob.key` (`job_key`) on paikallinen liiketoiminta-avain: sen avulla
creator löytää saman operaation aiemmin luodun jobin. Sitä ei käytetä ulkoisen
arkiston idempotenssiavaimena, koska liiketoimintatunnisteet voivat toistua
eri ympäristöissä tai tietokannan uudelleenluonnin jälkeen.

`ArkistointiJob.id` on UUID, joka syntyy jobin ensimmäisessä tallennuksessa ja
tunnistaa sekä jobin että sen toimituksen. Erillistä toimitusavainsaraketta ei ole.
Pääavain ja asiakirjaviitteiden `job_id` määritellään UUID-tyyppisiksi alkuperäisessä
luontimigraatiossa. Jobin ID on muuttumaton myös tietokannassa. PostgreSQL:n
oletusarvo luo UUID:n myös SQL-lisäyksille, joissa ID:tä ei anneta erikseen.

Processorin tulee välittää tallennettu jobin ID arvona `ArkistointiSubmission.jobId`.
`ArkistointiSubmission.key` palauttaa saman UUID:n ilman erillistä avainkenttää.
Scheduler- ja processor-rajapinnat käyttävät UUID:tä; db-schedulerin `task_instance`
on sen kanoninen merkkijono. Adapteri lähettää UUID:n kanonisena merkkijonona.
Kaikki automaattiset ja manuaaliset uusinnat käyttävät samaa tallennettua avainta.
Vastaanottajan on deduplikoitava myös rinnakkaiset ja epävarman onnistumisen
jälkeiset toimitukset atomisesti. Toimitussemantiikka on at-least-once.

Vaihe 1 määrittelee mallin ja sopimuksen; creatorin ja processorin kytkentä tehdään
niiden omissa tehtävissä. UUID ei estä tuotantotietokannan kopion lähettämistä:
kopiossa avaimet säilyvät samoina, joten ympäristöt on eristettävä ja kopion
workerin käynnistäminen estettävä erikseen.

## Arkistointitoimituksen muuttumaton sisältö

Tietokanta estää jobin UUID:n, yliopiston, case-tyypin, payloadin,
liiketoiminta-avaimen, luontiajan ja `expected_document_count`-arvon muuttamisen.
Tilaa, virheitä, ulkoista tunnistetta ja käsittelyn aikaleimoja voidaan edelleen
päivittää. Asiakirjaviitteen kaikkien kenttien muuttaminen ja viitteen tai jobin
poistaminen on estetty; samojen arvojen uudelleenkirjoitus on sallittu.

`ArkistointiJob.expectedDocumentCount` on alkuperäisen toimituksen asiakirjamäärä,
ei schedulerin laskuri. Creatorin on asetettava se pyyntöön kuuluvien viitteiden
määräksi ennen ensimmäistä tallennusta. Arvon on oltava positiivinen, eikä sillä
ole oletusta. Job, kaikki viitteet ja ajastus tallennetaan samaan transaktioon.
PostgreSQL:n `DEFERRABLE INITIALLY DEFERRED` -constraint-triggerit tarkistavat
viitteiden lopullisen määrän commitissa: tyhjä, vajaa tai ylimääräinen joukko ei
commitoidu. Tämä sallii JPA:n lisätä ensin jobin ja vasta sitten sen viitteet.
Lisäykset lukitsevat saman job-rivin, joten rinnakkaiset viitelisäykset
sarjallistuvat. Valmiiseen toimitukseen ei voi lisätä uusia viitteitä.
Tarkistusten pakottaminen välittömiksi `SET CONSTRAINTS` -komennolla edellyttää,
että koko manifesti on jo muodostettu.

Rollback ei tarvitse DELETE-oikeutta: se peruu myös jo lisätyt jobit, viitteet ja
ajastukset. Retentionin hallittu poistopolku toteutetaan erikseen. Suojaus koskee
tavallista SQL/JPA-DML:ää, ei tietokannan omistajaa, triggerien poistamista tai
`TRUNCATE`-komentoa; sovelluksen ajonaikaisen roolin ei tule sallia näitä ohituksia.
Vaiheen 1 viitesuojaus ei vielä takaa itse lähdeasiakirjan tavujen
muuttumattomuutta: snapshotien suojaus ja SHA-256-tarkistus kuuluvat seuraavaan
asiakirjatehtävään.

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
