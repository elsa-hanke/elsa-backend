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

### Arkistoinnin kuvasnapshotit

Arkistosnapshot tukee PDF-, JPEG- ja PNG-tiedostoja; `image/jpg` käsitellään
JPEG-aliasmuotona kuten tiedostojen latauksessa. Vain arkistokopio normalisoidaan
PDF/A-2b-muotoon. Alkuperäisen tiedoston tavut, nimi ja MIME-tyyppi eivät muutu.
Tavallinen PDF-yhdistäminen ei käytä tätä kuvien normalisointia tai sen rajoja.
Pakollisen laillistamistodistuksen puuttuminen tai virheellinen sisältö estää
snapshotin luonnin hallitulla virheellä; puutteellista aineistoa ei hyväksytä.

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

Pidä `async-enabled=false`, kunnes kyseisen yliopiston job-adapteri on toteutettu ja otettu käyttöön. `ArkistointiJobProcessor` on toteutettu, mutta konkreettiset toimitusadapterit ovat erillisiä tehtäviä. Pelkkä tämän muutoksen käyttöönotto ei kytke nykyisiä Siilo/Louhi-toimituksia pois. Jonopolku tallentaa hyväksynnän, asiakirjakopiot, hyväksymishetken metatiedot ja db-scheduler-ajastuksen samaan transaktioon.

Asetuksen palauttaminen arvoon `false` koskee uusia hyväksyntöjä. Jo jonossa olevat jobit säilyvät ja tarvitsevat edelleen taustakäsittelyn; niitä ei toimiteta automaattisesti vanhan synkronisen polun kautta. Nykyinen opiskelijan tiedosto säilyy muuttumattomana myös laillistamistodistuksen normalisoinnissa.

Async-hyväksynnän arkistoitava liitekooste muodostetaan alkuperäisistä liitteistä:
jokainen liite normalisoidaan erikseen PDF/A-2b-muotoon ja osat yhdistetään yhdeksi
tarkistetuksi, muuttumattomaksi snapshotiksi. Tavallinen liitekooste ja synkroninen
arkistointipolku käyttävät edelleen tavallista PDF-yhdistelyä. Arkistokopiossa
PDF-liitteet ovat ensin ja kuvat niiden jälkeen kuten tavallisessa koosteessa.
Vain liite, jonka PDF/A-muunnos ei onnistu sivut kopioimalla, rasteroidaan;
muiden liitteiden tekstikerros säilyy. Yhdistämisen virhe ei käynnistä koko
koosteen rasterointia. Väliaikaisia PDF/A-osia ei tallenneta tietokantaan.

Rasterointi käyttää 150 DPI:tä, enintään 200 sivua lähtö-PDF:ää kohti ja
10 megapikseliä rasteroitavaa sivua kohti. Koko liitekoosteen yhteinen budjetti
on 50 megapikseliä rasterointiin ja normalisoituihin kuviin; tekstinä kopioidut
PDF-sivut eivät kuluta tätä budjettia. Osat käsitellään peräkkäin. Kuvien
nykyiset koko-, dekoodaus- ja muistibudjettirajat säilyvät. Yksittäinen pitkä
PDF, joka vaatii kokonaan rasteroinnin, voi edelleen ylittää budjetin.

Taustakäsittelijä lataa tallennetun payloadin ja tarkistussummalla varmistetut asiakirjakopiot. Job siirtyy tilaan `KASITTELYSSA` lyhyessä transaktiossa; adapterin verkkokutsu suoritetaan transaktion ulkopuolella. Onnistuminen tallentaa tilan `LAHETETTY`, ulkoisen tunnisteen ja valmistumisajan ennen db-scheduler-suorituksen poistamista. Pysyvä virhe, puuttuva adapteri tai virheellinen aineisto johtaa tilaan `VAATII_TOIMENPITEITA` ilman automaattista uusintaa.

Käsittelijä lukee asiakirjan tavut suoraan tietokannasta ja siirtää adapterille vain tallennetut metatiedot sekä tarkistetun tavutaulukon. Adapterin tulee käsitellä tavutaulukkoa vain luettavana; uusintayritys lukee alkuperäiset tavut uudelleen tietokannasta.

Tilapäisten virheiden uusinnat käyttää db-schedulerin `consecutive_failures`-laskuria ja failure/dead-execution-käsittelijöitä. Asetukset ovat `application.arkistointi.retry.max-attempts` (oletus 5, sisältää ensimmäisen yrityksen sekä keskeytyneet suoritukset), `initial-delay` (1m), `multiplier` (2.0) ja `max-delay` (1h). Viimeisen sallitun yrityksen epäonnistuminen johtaa tilaan `VAATII_TOIMENPITEITA`. Tietokantahäiriön aikana suoritus säilyy palautettavana, kunnes myös lopputila voidaan tallentaa.

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
