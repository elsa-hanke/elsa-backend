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
`ArkistointiJobCreatorImpl` asettaa määrän koko pyyntölistasta ennen tallennusta.
Saman liiketoiminta-avaimen toistuva luontikutsu palauttaa aiemman jobin ennen
viitteiden käsittelyä: payloadia, asiakirjamäärää tai snapshot-viitteitä ei vaihdeta,
eikä creator luo uusia snapshoteja tai ajastusta. Tämä ei poista kutsujan ennen
creator-kutsua mahdollisesti luomia erillisiä snapshoteja.
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

### Arkistoinnin muuttumattomat snapshotit

Arkistosnapshot tallentaa olemassa olevan asiakirjan tavut, nimen ja MIME-tyypin
sellaisinaan. PDF:t luodaan ja liitteet yhdistetään olemassa olevalla PDF-palvelulla;
snapshot ei normalisoi, rasteroi tai pienennä sisältöä eikä edellytä PDF/A-muotoa.
Oma HTML-pohjainen PDF-tuotanto säilyttää aiemman PDF/A-2b-luontitapansa.
Erillinen laillistamistodistus säilyttää alkuperäisen tiedostomuotonsa, myös kuvat
ja `image/jpg`-MIME-tyypin. Snapshotin tallennus ei ole uusi tiedostovalidaattori:
sisällön hyväksyttävyys tarkistetaan olemassa olevassa lataus- ja PDF-käsittelyssä.
Pakollinen puuttuva tai tyhjä aineisto sekä puuttuvat metatiedot estävät snapshotin
luonnin hallitulla virheellä. Nimen ja MIME-tyypin tulee mahtua 255 merkin sarakkeisiin.

`ArkistointiAsiakirjaException.category` erottaa hylätyn lähdeaineiston
(`INPUT_DOCUMENT`), tallennetun aineiston eheysvirheen (`STORED_DOCUMENT`) ja
odottamattoman käsittelyvirheen (`PROCESSING_FAILURE`). Hyväksynnän HTTP-rajapinta
palauttaa ensimmäisestä luokasta 400 ja kahdesta muusta 500. ProblemDetail-vastaus
sisältää vakaan `ARCHIVE_DOCUMENT_*`-arvon kentässä `errorKey`, kentän
`message: error.<errorKey>`. Dokumenttivirhe ei sisällä operaatiokohtaista
`params`-kenttää, joten sama sopimus ei sido virhettä valmistumispyyntöön. Vastaus ei sisällä
tiedostonimeä, dokumentin sisältöä tai alkuperäistä virhekuvausta.
Dokumenttivirheestä lokitetaan vain koodi ja tarvittaessa luokka, ei poikkeusta.
Virhe peruuttaa hyväksymistransaktion; osittaisia snapshoteja tai jonorivejä ei jää.
Valmistumisen ja YEK-valmistumisen hyväksyntälomakkeet näyttävät dokumenttivirheen
suomenkielisen syyn. Lähdevirheelle (400) annetaan asiakirjan täydentämis- tai
tarkistusohje. Palvelinvirheelle (500) näytetään käsittelyvirheen yleinen ohje,
ei kehotusta korvata käyttäjän liitettä. Käännökset eivät sisällä henkilötietoja.
Dokumenttivirheen viesti ja kuvaus eivät sisällä tiedostonimeä tai sisältöä,
eikä poikkeus säilytä kirjaston alkuperäistä cause-ketjua.
Tietokantapoikkeuksia ei muuteta lähdeaineiston virheiksi.

Creator tarkistaa tallennetun sisällön, metatiedot ja SHA-256-tiivisteen aina
tietokannan asiakirjasta. Pelkkä DTO tai `archiveSnapshot=true` ei riitä:
palvelu tarkistaa tietokannan lipun ja
käsittelee muuttuvan lähdedokumentin aina erillisenä arkistokopiona.
Jobin nimi, MIME-tyyppi ja tiiviste otetaan lopullisesta snapshotista.
Loader tarkistaa tiivisteen ennen lähetystä. Snapshotilla on oma tavutaulukko ja
tietokannan suojaukset estävät sen sisällön ja metatietojen muuttamisen tai poistamisen.
Job, snapshotit ja ajastus tallentuvat samassa transaktiossa; rollback ei jätä
osittaista arkistointia. PDF/A-konvertoinnin sivu- tai pikselirajoja ei ole.

Valmistumisen hyväksyntä luo PDF:t samalla palvelulla sekä synkronisessa että
asynkronisessa tilassa. Asynkroninen haara jäädyttää valmiin yhteenvedon ja
liitekoosteen sellaisinaan sekä laillistamistodistuksen alkuperäisessä muodossa;
se ei kokoa liitteitä uudelleen eikä käytä erillistä konvertteria. Integraatiotestit
tarkistavat snapshotin tavujen, nimen ja MIME-tyypin yhtäsuuruuden valmistuneen
asiakirjan kanssa, 30/100/201-sivuiset PDF:t ilman upotettuja fontteja, CMYK-kuvat,
sekä hyväksynnän rollbackin ja synkronisen ZIP-lähetyksen ilman jonorivejä.

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
