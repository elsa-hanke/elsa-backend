# ELSA-palvelun backend

## Asynkronisen arkistoinnin koodirakenne

Asynkronisen arkistoinnin rajapinnat ja toteutukset on jaettu vastuiden mukaan paketin
`service.arkistointi.job` alle:

- `creation`: jobin luontipyyntö, luonnin rajapinta ja toteutus sekä transaktionaalisen jonotuksen rajapinta.
- `document`: asiakirjaviitteet, muuttumattomien snapshotien luonti, lataus ja eheystarkistus sekä asiakirjavirheet.
- `delivery`: adapterin rajapinta, adapterien valinta, toimituspyyntö ja toimituksen tulos.
- `processing`: jobin käsittely ja tilapäivitykset, suorituksen ajantasaisuuden tarkistus sekä virheiden ja uudelleenyritysten käsittely.

Yhteinen `ArkistointiError` on job-paketin juuressa. Spring-konfiguraatio
`ArkistointiDbSchedulerConfiguration` ja jonotuksen toteutus
`DbSchedulerArkistointiJobScheduler` ovat erillisissä tiedostoissa paketissa
`config.arkistointi`. Testit seuraavat vastaavaa pakettijakoa.
`ArkistointiJobProcessorImpl` yhdistää tilapalvelun, asiakirjojen latauksen ja
adapterille lähettämisen. Snapshotien luonti ja jobin luonti ovat erillään
taustakäsittelystä. Uudelleenyritysten asetukset ovat `config.arkistointi`-paketissa.

Valmistumisen hyväksymishetken tiedot määrittelee
`service.arkistointi.valmistuminen.ValmistuminenArkistointiPayload`.
Hyväksynnän transaktiotestit ja niiden HTTP-apufunktiot ovat vastaavassa
testipaketissa `service.arkistointi.valmistuminen`.

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

Processor välittää tallennetun jobin ID:n arvona `ArkistointiSubmission.jobId`.
`ArkistointiSubmission.key` palauttaa saman UUID:n ilman erillistä avainkenttää.
Scheduler-rajapinta käyttää UUID:tä; db-schedulerin `task_instance` on sen kanoninen
merkkijono. Processor saa `ArkistointiJobExecution`-olion, joka sisältää UUID:n
sekä kyseisen scheduler-suorituksen version vanhentuneiden tulosten torjumiseksi.
Adapteri lähettää UUID:n kanonisena merkkijonona.
Kaikki automaattiset ja manuaaliset uusinnat käyttävät samaa tallennettua avainta.
Vastaanottajan on deduplikoitava myös rinnakkaiset ja epävarman onnistumisen
jälkeiset toimitukset atomisesti. Toimitussemantiikka on at-least-once.

Creator ja processor käyttävät tätä samaa pysyvää toimitusavainta.
UUID ei estä tuotantotietokannan kopion lähettämistä:
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
eikä creator luo uusia snapshoteja tai ajastusta. Valmistumisen async-viimeistely
tarkistaa saman avaimen jo ennen PDF-tuotantoa, ilmoituksia ja snapshotien luontia,
joten olemassa olevan jobin toistuva sisäinen käsittely ei luo ylimääräisiä kopioita.
Hyväksynnän rivin lukitus ja creatorin lopullinen avaintarkistus säilyvät;
pelkkä ennakkohaku ei korvaa niitä.
Lisäykset lukitsevat saman job-rivin, joten rinnakkaiset viitelisäykset
sarjallistuvat. Valmiiseen toimitukseen ei voi lisätä uusia viitteitä.
Tarkistusten pakottaminen välittömiksi `SET CONSTRAINTS` -komennolla edellyttää,
että koko manifesti on jo muodostettu.

Rollback ei tarvitse DELETE-oikeutta: se peruu myös jo lisätyt jobit, viitteet ja
ajastukset. Retentionin hallittu poistopolku toteutetaan erikseen. Suojaus koskee
tavallista SQL/JPA-DML:ää, ei tietokannan omistajaa, triggerien poistamista tai
`TRUNCATE`-komentoa; sovelluksen ajonaikaisen roolin ei tule sallia näitä ohituksia.
Vaiheen 1 viitesuojausta täydentävät tässä asiakirjatehtävässä snapshotien
muuttumattomuussuojaus ja SHA-256-tarkistus.

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
Jobin asiakirjat valitaan yliopiston ja asian tyypin asiakirja-asetuksista samalla
tavalla kuin synkronisessa SÄHKE-paketissa. Tampereen valmistumisessa arkistoidaan
vain yhteenveto ja liitteet, Helsingin valmistumisessa vain yhteenveto.
Async-polku ei lisää synkronisesta toimituksesta puuttuvia asiakirjoja:
erillistä laillistamistodistusta ei lueta snapshotia varten eikä sen puuttuminen
estä arkistointijonoon siirtämistä kummassakaan yliopistossa.
Snapshotin tallennus ei ole uusi tiedostovalidaattori:
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

Creator tarkistaa sisällön, metatiedot ja SHA-256-tiivisteen repositoryn palauttamasta
asiakirjasta. JPA voi palauttaa nykyisen transaktion persistence contextissa jo olevan
entiteetin, joten tämä ei takaa tavujen uutta lukua tietokannasta. Pelkkä DTO tai
`archiveSnapshot=true` ei riitä: palvelu tarkistaa tietokannan lipun ja
käsittelee muuttuvan lähdedokumentin aina erillisenä arkistokopiona.
Jobin nimi, MIME-tyyppi ja tiiviste otetaan lopullisesta snapshotista.
Loader lukee tavut erikseen tietokannasta JDBC:llä ja tarkistaa tiivisteen ennen
lähetystä. Snapshotilla on oma tavutaulukko ja
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

### Koejakson asynkroninen arkistointi

`service.arkistointi.koejakso.KoejaksoArkistointiService` liittää kaksi nykyistä
arkistointitapahtumaa yhteiseen jonoon: vastuuhenkilön hyväksymän koulutussopimuksen
(`SOPIMUS`, asiakirjatyyppi `SOPIMUS`) ja lopullisen vastuuhenkilön arvion
(`KOEJAKSO`, asiakirjatyyppi `ARVIOINTI`). Myös koejakson hylkäävä lopullinen arvio
arkistoidaan. Korjattavaksi palauttaminen ja välivaiheiden kuittaukset eivät luo jobia.

Valinta tehdään yliopisto- ja asiatyyppikohtaisella asetuksella
`application.arkistointi.<yliopisto>.metadata.cases.<sopimus|koejakso>.async-enabled`.
Oletus on `false`: nykyinen synkroninen lähetys säilyy. Jos koko integraatio tai
asiatyyppi ei ole käytössä, jonoon ei lisätä tehtävää. Asynkroninen käyttöönotto
edellyttää kyseistä asiatyyppiä tukevaa yliopiston adapteria.
Adapteri ilmoittaa tuetut asiatyypit `supportedCaseTypes`-joukossa. Worker tarkistaa
käynnistyessään jokaisen käyttöön otetun yliopisto–asiatyyppiparin. Jo jonossa oleva
tukematon asiatyyppi päätyy tilaan `VAATII_TOIMENPITEITA` ilman lähetystä tai uusintaa.
Pelkkä jobien tuottaminen ei edellytä paikallista toimitusadapteria.

Hyväksyntä, tavallisen ladattavan PDF:n luonti, sen muuttumaton snapshot, job ja
`scheduled_tasks` tallentuvat samassa transaktiossa. Payload sisältää hyväksymishetken
henkilö-, opinto-oikeus-, tarkastaja- ja hyväksyjätiedot sekä arvion lopputuloksen.
Worker käyttää tallennettua sisältöä eikä muodosta PDF:ää uudelleen.
Payloadin `documents`-lista tallentaa myös `SOPIMUS`- ja `KOEJAKSO`-toimituksille
alkuperäisen `sourceDocumentId`-tunnisteen ja muuttumattoman `snapshotDocumentId`-tunnisteen
sekä `recordType`- ja `jarjestysnumero`-arvot. Worker lataa tavut snapshotin tunnisteella;
Louhi-adapterin tulee käyttää tallennettua alkuperäistä tunnistetta SÄHKE-XML:n
`Record.nativeId`-kentässä ja tarkistaa vastaavuus jobin asiakirjaviitteisiin.
Alkuperäisen dokumentin myöhempi muutos ei muuta tätä sidosta tai toimitettavia tavuja.
XML-kytkentä toteutetaan adapterin tehtävässä; nykyinen synkroninen polku säilyy.
Sähköposti-ilmoitukset lähetetään commitin jälkeen.
Hyväksymistransaktio peruuntuu myös checked-poikkeuksilla, kuten payloadin
sarjoitusvirheellä.

Asynkronisessa tilassa päivitykset lukitsevat sopimuksen tai arvion rivin ennen
tilan tarkistamista. Lopullisesti hyväksyttyä käsittelyä ei voi tällöin toistaa
tai muokata tämän päivityspolun kautta, ja aikaisemmat hyväksynnät tarkistetaan.
Synkronisessa tilassa säilyvät aiemmat päivityssäännöt ilman uusia
ennakkolukituksia tai hyväksyntätarkistuksia. Synkroninen lähetys käyttää suoraan
alkuperäistä asiakirjaa ja aiempia metatietoja eikä muodosta async-payloadia.
HTTP-rajapinnan nykyiset hyväksyntätarkistukset säilyvät molemmissa tiloissa.
Paikalliset liiketoiminta-avaimet ovat
`koejakso-sopimus:<YLIOPISTO>:<sopimusId>` ja
`koejakso-arvio:<YLIOPISTO>:<arvioId>`; toimitusavaimena säilyy jobin UUID.
Koejakson jonopolku tarkistaa olemassa olevan jobin liiketoiminta-avaimella ennen
snapshotin luontia. Toistuva kutsu ei luo irrallista snapshotia eikä muuta olemassa
olevaa jobia tai sen ajastusta. Tarkistus ei korvaa hyväksynnän rivilukitusta tai
jobin avaimen yksilöllisyysrajoitetta.

### Valmistumisen asynkroninen arkistointi

Valmistumisen hyväksyntä käyttää oletuksena nykyistä arkistointitoimitusta. Uusi jono otetaan käyttöön yliopistokohtaisesti asetuksella `application.arkistointi.<yliopisto>.metadata.cases.valmistuminen.async-enabled=true`; myös integraation `kaytossa`-asetuksen on oltava päällä. Yliopiston avain on `tre`, `hki`, `oulu`, `turku` tai `uef`.

Pidä `async-enabled=false`, kunnes kyseisen yliopiston job-adapteri on toteutettu ja otettu käyttöön. `ArkistointiJobProcessor` on toteutettu, mutta konkreettiset toimitusadapterit ovat erillisiä tehtäviä. Pelkkä tämän muutoksen käyttöönotto ei kytke nykyisiä Siilo/Louhi-toimituksia pois. Jonopolku tallentaa hyväksynnän, asiakirjakopiot, hyväksymishetken metatiedot ja db-scheduler-ajastuksen samaan transaktioon.

Valmistumisen payloadin `documents`-lista tallentaa jokaisesta jonoon valitusta
asiakirjasta `sourceDocumentId`-, `snapshotDocumentId`-, `recordType`- ja
`jarjestysnumero`-arvot. Alkuperäinen tunniste ja snapshotin tunniste sidotaan
toisiinsa snapshotin luonnissa. Payload on tietokannassa muuttumaton; tunnisteita
ei päätellä myöhemmin valmistumispyynnön nykyisistä asiakirjoista.
Worker lataa tavut edelleen snapshot-viitteen kautta. Louhi-adapterin XML-kytkennän
tulee käyttää tallennettua `sourceDocumentId`-arvoa SÄHKE-`Record.nativeId`-kentässä
ja tarkistaa, että payloadin asiakirjat vastaavat jobin snapshot-viitteitä.
Tämä vaihe tallentaa tunnisteet; niiden käyttö Louhi-XML:ssä toteutetaan adapterin tehtävässä.

Asetuksen palauttaminen arvoon `false` koskee uusia hyväksyntöjä. Jo jonossa olevat jobit säilyvät ja tarvitsevat edelleen taustakäsittelyn; niitä ei toimiteta automaattisesti vanhan synkronisen polun kautta.

Async-hyväksyntä tallentaa muuttumattoman kopion samasta valmiista yhteenvedosta
ja liitekoosteesta yliopiston ja tapaustyypin synkronista toimitusta vastaavan
asiakirjavalinnan mukaisesti: Tampereelle yhteenveto ja liitekooste, Helsingille
vain yhteenveto. Erillistä laillistamistodistusta ei lueta tai vaadita.
Alkuperäisiä liitteitä ei kysellä tai yhdistetä uudelleen arkistokopiota varten. Taustakäsittelijä
lataa snapshotin JDBC:llä, tarkistaa tiivisteen ja välittää sisällön adapterille
sellaisenaan. Muunnoksia tai PDF/A-vaatimusta ei lisätä lähetyksen tai uusintojen
yhteydessä. Testit kattavat myös tavallisen pitkän PDF:n ja CMYK-JPEG:n täsmällisen
sisällön säilymisen uudelleenyrityksessä, vaikka adapteri muuttaisi saamansa puskurin.

Worker varmistaa execution-version ja siirtää jobin `KASITTELYSSA`-tilaan lyhyessä
transaktiossa, josta palautetaan vain tallennettu payload ja asiakirjojen skalaariset
viitteet. Snapshotien tavut ladataan ja SHA-256 tarkistetaan vasta commitin jälkeen,
ilman scheduler- tai job-rivien lukkoja. Ennen adapterikutsua alkuperäinen execution
ja jobin tila tarkistetaan uudelleen lyhyessä transaktiossa. Myös tuloksen tallennus
säilyttää execution-version tarkistuksen. Tarkistuksen ja ulkoisen kutsun väliin jää
silti kilpailuikkuna: adapterin on deduplikoitava myös samanaikaiset toimitukset
samalla job-UUID:lla. Testit kattavat vanhan latauksen jatkumisen uuden executionin
käynnistyttyä tai valmistuttua sekä vanhan latauksen virheen.

`replica`-profiili poistaa db-scheduler-workerin käytöstä erikseen
(`db-scheduler.enabled=false`); pelkkä `application.scheduling.enabled=false`
ei pysäytä sitä. Myös `dev`-profiilissa worker on oletuksena pois käytöstä.
Paikallisella testitietokannalla sen voi ottaa nimenomaisesti käyttöön asetuksella
`DB_SCHEDULER_ENABLED=true` tai `--db-scheduler.enabled=true`. Älä ota workeria
käyttöön production-datan kopiolla: jo jonossa olevat jobit voivat edetä, vaikka
uusien jobien luonti on estetty `async-enabled`- tai `kaytossa`-asetuksilla.

Taustakäsittelijä lataa tallennetun payloadin ja tarkistussummalla varmistetut asiakirjakopiot. Job siirtyy tilaan `KASITTELYSSA` lyhyessä transaktiossa; adapterin verkkokutsu suoritetaan transaktion ulkopuolella. Onnistuminen tallentaa tilan `LAHETETTY`, ulkoisen tunnisteen ja valmistumisajan ennen db-scheduler-suorituksen poistamista. Pysyvä virhe, puuttuva adapteri tai virheellinen aineisto johtaa tilaan `VAATII_TOIMENPITEITA` ilman automaattista uusintaa.

Kun db-scheduler-worker on käytössä (myös ilman erillistä `enabled`-asetusta),
jokainen `kaytossa=true` ja tapauskohtainen `async-enabled=true` -yhdistelmä
vaatii yliopiston asynkronisen `service.arkistointi.job.delivery.ArkistointiAdapter`-beanin.
Puuttuva adapteri estää käynnistyksen ennen workerin aloitusta; ulkoiseen arkistoon
ei tehdä käynnistystarkistuksessa verkkokutsua. Saman yliopiston adapterit eivät
saa olla päällekkäisiä. Workerista erillinen jonoon kirjoittava solmu voi käyttää
asetusta `db-scheduler.enabled=false` ilman paikallisia adaptereita.

Jo jonossa olevan jobin puuttuva adapteri tallentaa turvallisen virhekoodin
`ARCHIVE_ADAPTER_MISSING` ja tilan `VAATII_TOIMENPITEITA`, eikä sitä uusita
automaattisesti. Adapterin asentaminen ja sovelluksen uudelleenkäynnistys eivät
palauta lopputilaan päätynyttä jobia jonoon. Korjauksen jälkeen tarvitaan hallittu
manuaalinen uusinta samalla job-UUID:lla/toimitusavaimella, payloadilla ja
asiakirjasnapshoteilla. Manuaalinen uusinta on erillinen toteutettava toiminto,
jonka on oltava valmis ennen async-tilan production-käyttöönottoa. Lopputiloja ei
palauteta automaattisesti massana. Huoltokatkoa varten pysäytä worker; adapterin
poistaminen tai uusien jobien luonnin poistaminen käytöstä ei ole jonon tauotus.

Tausta-ajossa dokumentin `INPUT_DOCUMENT`- ja `STORED_DOCUMENT`-virheet ovat
pysyviä: tallennettua aineistoa ei generoida tai korjata uusintayrityksellä.
`PROCESSING_FAILURE` sekä tietokantayhteyden häiriöt käyttävät db-schedulerin
rajattuja uusintayrityksiä. Processor säilyttää vain virhekoodin ja kiinteän
turvallisen kuvauksen; alkuperäistä kuvausta, tiedostonimeä tai cause-ketjua
ei välitetä schedulerin lokiin tai jobin virhetietoihin. Sama luokittelu koskee
myös alkuperäisistä liitteistä yhdistetyn snapshotin luontivirheitä.

Käsittelijä lukee asiakirjan tavut suoraan tietokannasta ja siirtää adapterille vain tallennetut metatiedot sekä tarkistetun tavutaulukon. Adapterin tulee käsitellä tavutaulukkoa vain luettavana; uusintayritys lukee alkuperäiset tavut uudelleen tietokannasta.

Tilapäisten virheiden uusinnat käyttävät db-schedulerin `consecutive_failures`-laskuria ja failure/dead-execution-käsittelijöitä. Asetukset ovat `application.arkistointi.retry.max-attempts` (oletus 25, sisältää ensimmäisen yrityksen sekä keskeytyneet suoritukset), `initial-delay` (1m), `multiplier` (2.0) ja `max-delay` (4h). Oletuksilla ensimmäisen ja 25. yrityksen väliin kertyy 24 viivettä: 1, 2, 4, 8, 16, 32, 64 ja 128 minuuttia sekä 16 neljän tunnin viivettä, yhteensä enintään 68 tuntia 15 minuuttia ennen jitterin vaikutusta. Tämä on alle kolmen vuorokauden suunniteltu odotusaika; suoritusten kesto, schedulerin viiveet ja käyttökatkot voivat pidentää todellista aikaa. Asetus ei ole ehdoton 72 tunnin määräaika. Viimeisen sallitun yrityksen epäonnistuminen johtaa tilaan `VAATII_TOIMENPITEITA`. Tietokantahäiriön aikana suoritus säilyy palautettavana, kunnes myös lopputila voidaan tallentaa.

Jitter asetetaan arvolla `application.arkistointi.retry.jitter-factor` (oletus `0.1`).
Jokaisella uusinnalla viive arvotaan erikseen 90–100 prosenttiin rajatusta
eksponentiaalisesta viiveestä; neljän tunnin kohdalla vaihteluväli on 3 h 36 min – 4 h.
Jitter lyhentää viiveitä eikä kasvata yllä kuvattua enimmäisodotusaikaa. Arvo `0`
poistaa jitterin käytöstä. Kelvollinen kerroin on vähintään 0 ja alle 1; viive on
aina vähintään yksi millisekunti.

Async-tulokset päivittävät commitin jälkeen nykyisiä `arkistointi.requests.total`-
ja `arkistointi.errors.total`-mittareita sekä `arkistointi.async.attempts.total`-mittaria
rajatuilla yliopisto-, tapaustyyppi- ja tulostageilla. Virhemittari laskee epäonnistuneita
käsittely-yrityksiä, myös palautettuja keskeytyneitä suorituksia. SNS-varoitus lähetetään
asetuksen `application.arkistointi.retry.alert-after-attempts` kohdalla (oletus 3);
aiemmat ja myöhemmät tilapäiset virheet eivät lähetä yrityskohtaista SNS-viestiä.
Pysyvä virhe tai yritysrajan täyttyminen lähettää erillisen toimenpideilmoituksen heti
`VAATII_TOIMENPITEITA`-tilan commitin jälkeen. Viesti sisältää vain job-UUID:n,
yliopiston, tapaustyypin, yritysnumeron, tuloksen ja turvallisen virhekoodin.
Rollback ja vanhentunut suoritus eivät julkaise tulosta. Ilmoituksen tai mittarin
virhe ei muuta arkistointitulosta tai estä schedulerin kuittausta.
Havainnointi on best effort: prosessin kaatuminen commitin jälkeen voi hukata viestin,
ja saman version rinnakkaiset handlerit voivat tuottaa saman varoituksen uudelleen.
Pysyvää ilmoitusjonoa, deduplikointia tai palautumisviestiä ei vielä ole;
ne kuuluvat erilliseen valvonta- ja hallintatehtävään.

Jobin tilan commit ja db-schedulerin `reschedule`/`remove` ovat erillisiä vaiheita.
Jos ajastuksen päivitys epäonnistuu commitin jälkeen, poimittu suoritus jää
db-schedulerin dead-execution-palautuksen käsiteltäväksi. Jo tallennettua lopputilaa
ei lähetetä uudelleen. Tietokantahäiriö voi viivästyttää palautumista.
Versiotarkistus estää vanhentuneen suorituksen tilapäivityksen, mutta saman version
kaksi handleria voivat ehtiä tallentaa tilan ennen ajastuksen päivitystä.
Ajastuksen päivityksessä kirjaston oma versiotarkistus sallii vain yhden onnistua;
hävinnyt operaatio ei muuta uudempaa aikataulua tai kasvata laskuria uudelleen.

Toimitus on at-least-once: jos ulkoinen arkisto hyväksyy aineiston mutta paikallinen onnistumisen tallennus keskeytyy, adapteria voidaan kutsua uudelleen samalla `ArkistointiSubmission.key`-avaimella. Adapterin ja vastaanottavan järjestelmän on estettävä kaksoistoimitus tällä avaimella atomisesti myös samanaikaisissa pyynnöissä: palautettavaksi tulkittu vanha suoritus voi edelleen olla verkkokutsussa uuden alkaessa. Pelkkä olemassaolon tarkistus ennen luontia ei riitä. Pelkkä db-scheduler ei takaa exactly-once-toimitusta. Adapterin virhekoodit ja kuvaukset eivät saa sisältää henkilötietoja, tiedostonimiä, tunnuksia tai HTTP-vastausten sisältöä.

Processor saa käynnistyksessä muuttumattoman suoritustunnisteen (`task_name`, `task_instance`, `version`). Sekä aloitus, onnistuminen että kaikki virhe- ja palautuskäsittelijän tilamuutokset lukitsevat ensin vastaavan poimitun `scheduled_tasks`-rivin ja sitten job-rivin samassa transaktiossa. Vanhentunut suoritus ei muuta jobia eikä uudempaa ajastusta; ennen lähetystä havaittu vanhentunut suoritus ei kutsu adapteria. Processorissa havaittu vanhentuminen päättyy ilman scheduler-rivin kuittausta, ja normaalissa kuittauksessa db-scheduler tarkistaa edelleen version. Lukitukset vapautuvat ennen verkkokutsua ja ennen db-schedulerin reschedule/remove-toimintoa. Versiotarkistus ei peruuta jo alkanutta ulkoista pyyntöä. Manuaalisessa uusinnassa on huomioitava erikseen suorituksen identiteetti: samaan task-instanssiin poiston jälkeen luotavan rivin version ei oleteta kasvavan edellisestä.

Adapterin `Success.externalId` on joko `null` tai ei-tyhjä, kelvollinen Unicode-merkkijono,
jonka pituus on enintään 255 UTF-16-koodiyksikköä (sama mitta kuin entiteetin
`@Size`-validoinnissa). Pelkkä tyhjätila, NUL ja irralliset surrogaatit eivät kelpaa.
Tunniste tallennetaan sellaisenaan, ilman lyhentämistä, trimmausta tai hajautusta.
Virheellinen onnistumisvastaus johtaa koodiin `ARCHIVE_ADAPTER_RESPONSE_INVALID` ja
tilaan `VAATII_TOIMENPITEITA`: arkisto on voinut jo hyväksyä toimituksen, joten
tilanne on selvitettävä ennen uutta lähetystä. Tunnistetta tai vastausrunkoa ei tallenneta virhekuvaukseen.

Failure handler tallentaa myös pysyvät virheet. Pysyvän virheen alkuperäinen turvallinen
koodi säilyy myös viimeisellä yrityksellä. Tallennuksen epäonnistuessa koodi ja pysyvä
luokittelu säilyvät handlerin välittämässä poikkeuksessa. Scheduler-riviä ei kuitata eikä
ajasteta uudelleen, jos tilan commit epäonnistuu.
Jos tulosta ei ole saatu tallennettua, dead-execution-palautuminen ei voi tietää
aiemmin saatua pysyvää tulosta, vaikka prosessi olisi edelleen käynnissä:
adapterikutsu voidaan uusia samalla avaimella. Handler ei tee paikallista automaattista
tallennusuusintaa eikä säilytä tulosta omassa muistirekisterissä.
Tämä rajoitus koskee myös virheellistä onnistumisvastausta; paikallista exactly-once-takuuta ei ole.

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
