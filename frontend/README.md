# ELSA-palvelun frontend

## Projektin asennus

```
yarn install
```

### Käynnistä kehitysympäristö

```
yarn serve
```

### Käännä tuotantoversio

```
yarn build
```

### Suorita yksikkötestit

```
yarn test:unit
```

### Suorita E2E-testit

```
yarn test:e2e
```

### Suorita tyylivirheiden tarkistus

```
yarn lint
```

## Rich text -sisällön turvallisuus

`RichTextHtmlSanitizer` (backend), `utils/richText.ts` ja `RichTextContent` (frontend)
rajaavat ilmoitusten `teksti`-, arvioitavien kokonaisuuksien `kuvaus`/`kuvausSv`- ja
arviointityökalujen `ohjeteksti`-kenttien HTML:n. Palvelut puhdistavat sisällön ennen
tallennusta ja mapperit myös vanhojen tietueiden DTO:t, mukaan lukien sisäkkäiset DTO:t.
Lukeminen ei muuta entityjä eikä kirjoita puhdistettua sisältöä tietokantaan.
Selaimen renderöinti puhdistetaan erikseen DOMPurifylla. `restricted-rich-text` rajaa
näiden kenttien editorin asetukset ja puhdistaa editorille annettavan vanhan HTML:n.
Muiden editorien toiminta ei muutu.

Sallitut otsikot, korostukset, listat, taulukot ja HTTP(S)/mailto/suhteelliset linkit
säilyvät. Vapaa CSS, aktiiviset elementit ja tapahtumankäsittelijät poistetaan.
Taulukon ulkoasu tulee komponentin CSS:stä. Tyhjäksi puhdistuva ilmoitus hylätään
HTTP 400 -vastauksella; valinnaiset kuvaukset voivat olla tyhjiä.

Muut `v-html`-kohdat tarkastettiin: käännöstekstit ja niiden linkit tulevat
versionhallituista locale-tiedostoista ja koodin URL-vakioista (vanha asetus,
työskentelyjaksot, koulutussuunnitelma, teoriakoulutukset, koejakson tavoitteet ja
koulutuspaikan linkki). Virkailijan yhteenvetojen neljä näkymää käyttävät jo
DOMPurify-sanitointia. Jos näiden lähteet muuttuvat API- tai käyttäjäsyötteiksi,
ne on käsiteltävä epäluotettavana HTML:nä.

Yhteinen regressioaineisto: `src/test/resources/security/rich-text-cases.json`
(repository root). Sen ajavat sekä JVM- että Jest-testit. Lisäksi testataan
palvelujen tallennusraja, versiointi, suorat HTTP-pyynnöt, sisäkkäiset DTO:t,
näkymien DOM ja anonyymi julkinen GET oikean security filter chainin läpi.
Testit kuuluvat nykyisiin CI-tehtäviin `test`, `integrationTest` ja `yarn test:unit`.

Productionin HTML-vastauksen otsakkeet tarkistettiin 22.10.2026 read-only HEAD
-pyynnöllä osoitteesta `https://elsapalvelu.fi/`. CloudFrontin CSP sallii edelleen
`script-src 'unsafe-inline' 'unsafe-eval'`; sitä ei pidä pitää tämän XSS-korjauksen
varmistuksena. CSP:n tiukentaminen vaatii erillisen yhteensopivuustarkastuksen.
Productionin käyttöönottoa tai tietokannan sisältöä ei tarkistettu eikä muutettu.

Vanhan sisällön read-only tarkistuskysely on `scripts/security/audit-rich-text.sql`.
Sitä ei suoriteta automaattisesti. Osumat vaativat manuaalisen arvioinnin eivätkä
osoita hyökkäystä; kysely ei löydä kaikkia obfuskoituja hyötykuormia. Mahdollinen
pysyvä siivous päätetään erikseen alkuperäiset tiedot säilyttäen.

Rajoitettu editori puhdistaa ulkoa ladatun HTML:n, mutta palauttaa TinyMCE:n oman
`input`-arvon muuttumattomana `v-model`-kierroksella. Näin named-entityjen ja
Unicode-merkkien erilainen serialisointi ei korvaa editorin sisältöä kesken
kirjoittamisen. Wrapperin Jest-testit ja Cypressin oikea tekstinsyöttö kattavat
tämän sekä kohdistimen ja undo/redo-toimintojen säilymisen.

Versioinnin nykyinen tallennuskäytäntö: uuden arviointityökaluversion tai korvaavan
arvioitavan kokonaisuuden tallennus puhdistaa myös aiemman version HTML-kentät,
kun vanha versio poistetaan käytöstä. Pelkkä lukeminen ei kirjoita tietokantaan.
Jos historiallinen HTML pitää säilyttää täsmälleen alkuperäisenä, tämä käytäntö on
muutettava erikseen; nykyiset regressiotestit edellyttävät puhdistusta tallennettaessa.
