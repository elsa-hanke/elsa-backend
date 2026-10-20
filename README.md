# ELSA-palvelun backend

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

### Arkistoinnin kuvasnapshotit

Arkistosnapshot tukee PDF-, JPEG- ja PNG-tiedostoja; `image/jpg` käsitellään
JPEG-aliasmuotona kuten tiedostojen latauksessa. Vain arkistokopio normalisoidaan
PDF/A-2b-muotoon. Alkuperäisen tiedoston tavut, nimi ja MIME-tyyppi eivät muutu.
Tavallinen PDF-yhdistäminen ei käytä tätä kuvien normalisointia tai sen rajoja.
Pakollisen laillistamistodistuksen puuttuminen tai virheellinen sisältö estää
snapshotin luonnin hallitulla virheellä; puutteellista aineistoa ei hyväksytä.

`ArkistointiAsiakirjaException.category` erottaa hylätyn lähdeaineiston
(`INPUT_DOCUMENT`), tallennetun aineiston eheysvirheen (`STORED_DOCUMENT`) ja
odottamattoman käsittelyvirheen (`PROCESSING_FAILURE`). Hyväksynnän HTTP-rajapinta
palauttaa ensimmäisestä luokasta 400 ja kahdesta muusta 500. ProblemDetail-vastaus
sisältää vakaan `ARCHIVE_DOCUMENT_*`-arvon kentässä `errorKey`, kentän
`message: error.<errorKey>` sekä `params: valmistumispyynto`. Vastaus ei sisällä
tiedostonimeä, dokumentin sisältöä tai alkuperäistä virhekuvausta.
Dokumenttivirheestä lokitetaan vain koodi ja tarvittaessa luokka, ei poikkeusta.
Virhe peruuttaa hyväksymistransaktion; osittaisia snapshoteja tai jonorivejä ei jää.
Frontendin käännökset ja virheen näyttäminen toteutetaan erillisessä tehtävässä.
Dokumenttivirheen viesti ja kuvaus eivät sisällä tiedostonimeä tai sisältöä,
eikä poikkeus säilytä kirjaston alkuperäistä cause-ketjua. Esimerkiksi palvelimen
ICC-profiilin puuttuminen on käsittelyvirhe, ei virheellinen käyttäjän tiedosto.
Tietokantapoikkeuksia ei muuteta lähdeaineiston virheiksi.

Normalisointi validoi lopulliset PDF/A-tavut. Snapshot-palvelu liittää viitteeseen
sisäisen todisteen, joka sitoo tarkistuksen dokumentin ID:hen, metatietoihin ja
SHA-256-tiivisteeseen. Creator tarkistaa tallennetun sisällön ja tiivisteen,
mutta ei validoi samaa hyväksyttyä PDF/A-tulosta uudelleen. Viitteen `copy()`
ei kopioi todistetta; ilman sitä olemassa oleva snapshot validoidaan uudelleen.
Pelkkä `archiveSnapshot=true` ei riitä: palvelu tarkistaa tietokannan lipun ja
käsittelee muuttuvan lähdedokumentin aina erillisenä arkistokopiona.
Jobin nimi, MIME-tyyppi ja tiiviste otetaan lopullisesta snapshotista.
Loader tarkistaa edelleen tiivisteen ennen lähetystä. Snapshotin luonnin transaktiorajat
ja normalisoinnin fallback säilyvät.

`ArchiveImageNormalizer` tarkistaa ennen rasterin lukemista enintään 20 MiB:n
tiedostokoon, 16 384 pikselin sivun ja 40 megapikselin lähdekuvan rajat. Lisäksi
256 MiB:n laskennallinen työmuistibudjetti varaa 8 tavua/lähdepikseli,
kaksi kopiota pakatusta aineistosta ja 32 tavua/dekoodattu pikseli.
Tämä on konservatiivinen hyväksymisarvio, ei takuu JVM:n kokonaismuistinkulutuksesta;
myös näitä kokorajoja pienempi kuva voidaan hylätä muistibudjetin perusteella.
Dekooderi pienentää kuvaa tasaisesti jo lukuvaiheessa enintään 10 megapikseliin
ja 4096 pikselin sivuun sekä jäljellä olevaan muistibudjettiin. Esimerkiksi
4000 × 3000 pikselin kuva luetaan 2000 × 1500 pikselin rasteriksi. Jos rajoihin
mahtuminen vaatisi yli nelinkertaisen pienennyksen kummallakin akselilla,
kuva hylätään hallitusti liian voimakkaan laadun heikentämisen sijaan.
Värimalli muunnetaan sRGB:ksi, läpinäkyvyys valkoista taustaa vasten ja EXIF-suunta
huomioidaan. PDF-tiedostojen aiemmat rasterointirajat säilyvät ennallaan.

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

Pidä `async-enabled=false`, kunnes `ArkistointiJobProcessor` ja kyseisen yliopiston job-adapteri on toteutettu ja otettu käyttöön. Pelkkä tämän muutoksen käyttöönotto ei kytke nykyisiä Siilo/Louhi-toimituksia pois. Jonopolku tallentaa hyväksynnän, asiakirjakopiot, hyväksymishetken metatiedot ja db-scheduler-ajastuksen samaan transaktioon.

Asetuksen palauttaminen arvoon `false` koskee uusia hyväksyntöjä. Jo jonossa olevat jobit säilyvät ja tarvitsevat edelleen taustakäsittelyn; niitä ei toimiteta automaattisesti vanhan synkronisen polun kautta. Nykyinen opiskelijan tiedosto säilyy muuttumattomana myös laillistamistodistuksen normalisoinnissa.

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
