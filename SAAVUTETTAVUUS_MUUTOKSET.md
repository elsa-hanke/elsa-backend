# Saavutettavuuskorjaukset – Elsa-verkkopalvelu

**Versio:** 1.0  
**Päiväys:** 31.5.2026  
**Perusta:** Ulkopuolisen asiantuntijaorganisaation 12/2025 tekemä saavutettavuusarviointi ja prioriteettilistaus

---

## Yhteenveto

Tässä dokumentissa kuvataan kaikki saavutettavuusarvioinnin pohjalta tehdyt korjaukset Elsa-verkkopalvelun frontend-koodiin. Korjaukset on ryhmitelty priorisoidun korjauslistan mukaan. Jokaisen korjauksen yhteydessä on ohje, miten muutos voidaan testata manuaalisesti.

---

## Prioriteetti 1 – Käyttöä estävät ongelmat

### 1. Kalenteripainike (Datepicker) – WCAG 2.1.1, 2.4.6, 2.4.7

**Korjattu tiedosto:** `frontend/src/components/datepicker/datepicker.vue`

**Mitä korjattiin:**
- Poistettu koodi, joka asetti kalenteripainikkeelle `tabindex="-1"`. Aiemmin kalenteripainike ei saanut näppäinfokusta lainkaan, koska tabindex-arvo esti sen fokusoinnin.
- Lisätty kalenteripainikkeelle kuvaava saavutettava nimi (`aria-label="Avaa kalenteri"`) – aiemmin painike ei kertonut toimintaansa apuvälinekäytössä.
- Lisätty `<span class="sr-only">` -elementti painikkeen sisälle, joka kertoo ruudunlukijalle painikkeen tarkoituksen.
- Lisätty Bootstrap Vue:n kalenterinavigointipainikkeiden suomenkieliset selitteet (`label-prev-month`, `label-next-month`, `label-current-month`, `label-today`).
- Muutettu kalenterin piilottama label-elementti CSS:n avulla visuaalisesti piilotetuksi mutta ruudunlukijalle luettavaksi (aiemmin `display: none` piilotti sen myös ruudunlukijalta).

**Miten testata manuaalisesti:**
1. Avaa lomake, jossa on päivämääräkenttä.
2. Siirry Tab-näppäimellä kenttään. Näppäimistöllä pitäisi pystyä siirtymään myös kalenteripainikkeeseen (seuraava Tab-painallus).
3. Paina Enter tai välilyönti kalenteripainikkeella – kalenterin pitäisi avautua.
4. Tarkista ruudunlukijalla (VoiceOver/NVDA): painikkeen nimen pitäisi kuulua "Avaa kalenteri".
5. Kalenterin sisällä nuolinäppäimillä navigointi pitäisi toimia ja kuukauden vaihtonappien nimet kuulua suomeksi.

---

### 2. Haitarielementti (Accordion) – WCAG 1.3.1, 4.1.2, 2.4.7

**Korjattu tiedosto:** `frontend/src/components/accordian/accordian.vue`

**Mitä korjattiin:**
- Muutettu `role="tab"` → `role="button"`. Haitarielementti käyttäytyi aiemmin kuin välilehti (tab), mikä oli semanttisesti väärä rooli. Nyt se on painike, jota voi laajentaa ja supistaa.
- Lisätty `aria-expanded`-attribuutti, joka kertoo ruudunlukijalle, onko haitari auki (`"true"`) vai kiinni (`"false"`).
- Haitareilla, joilla ei ole sisältöä (tyhjä default-slot), `tabindex="-1"` asetetaan automaattisesti, jolloin ne eivät saa turhaan näppäinfokusta.
- Lisätty näkyvä kohdistuskehys (outline) haitariotsikon `:focus`-tilaan.

**Miten testata manuaalisesti:**
1. Navigoi Tab-näppäimellä sivulle, jossa on haitarielementtejä (esim. koulutussuunnitelma-sivu tai kouluttajan arviointisivu).
2. Kun haitari saa fokuksen, sen ympärille pitäisi ilmestyä näkyvä sininen kehys.
3. Paina Enter tai välilyönti – haitarin pitäisi avautua/sulkeutua.
4. Tarkista ruudunlukijalla: haitarin tila pitäisi kuulua "laajennettu" tai "supistettu" (`aria-expanded`).
5. Haitarit, joissa ei ole sisältöä, eivät saa enää näppäinfokusta.

---

### 3. Näkyvä kohdistus puuttuu painikkeista – WCAG 2.4.7

**Korjattu tiedosto:** `frontend/src/styles/app.scss`

**Mitä korjattiin:**
- Poistettu CSS-sääntö `.btn:focus, .btn:active { outline: none !important; box-shadow: none; }`, joka esti kaikkien painikkeiden näkyvän kohdistuksen.
- Korvattu sääntö modernilla toteutuksella, joka:
  - Näyttää sinisen 2px-kehyksen (`outline: 2px solid $primary`) ja box-shadow-effektin, kun painike saa näppäinfokuksen.
  - Piilottaa kehyksen, kun painike aktivoidaan hiirellä (`:focus:not(:focus-visible)`) – tämä estää turhan kehyksen hiirenkäyttäjille.
  - Poistaa kehyksen vain `active`-tilasta (klikkaustilasta).
- Lisätty sama kohdistuksen korjaus navigaatiovälilehtiin (`ul.nav.nav-tabs`).
- Lisätty `scroll-padding-top: 80px` html-elementille, jolloin fokusoidut elementit eivät päädy kiinteän ylätunnisteen taakse vierittäessä.
- Lisätty vastaava kohdistuskorjaus linkeille ja nav-link-elementeille.

**Miten testata manuaalisesti:**
1. Siirry sivulle, jossa on painikkeita (esim. etusivu tai lomakesivu).
2. Paina Tab-näppäintä, kunnes jokin painike saa fokuksen.
3. Painikkeen ympärillä pitäisi näkyä sininen kehys (outline).
4. Klikkaa samaa painiketta hiirellä – kehystä ei pitäisi enää näkyä aktivoinnin jälkeen.
5. Tarkista myös välilehdet (tab-elementit) ja haitarielementit.

---

### 4. Liitetiedoston lisääminen ei onnistu näppäimistöllä – WCAG 2.1.1, 4.1.2

**Korjattu tiedosto:** `frontend/src/components/asiakirjat/asiakirjat-upload.vue`

**Mitä korjattiin:**
- Lisätty tiedostonlatauspainikkeen label-elementille `tabindex="0"`, jolloin se voidaan fokusoida näppäimistöllä.
- Lisätty `role="button"` label-elementille, jolloin ruudunlukija tunnistaa sen painikkeena.
- Lisätty Enter- ja välilyönti-näppäinten kuuntelijat (`@keydown.enter.prevent` ja `@keydown.space.prevent`), jotka käynnistävät tiedoston valinnan näppäimistöllä.
- Korvattu HTML:n `disabled`-attribuutti (joka ei ole label-elementille validi) `aria-disabled`-attribuutilla.
- Lisätty näkyvä kohdistuskehys ladatun tiedoston label-elementille.
- Lisätty `aria-label`-nimet lataus- ja poistopainikkeille tiedoston nimellä (esim. "Lataa tiedostonimi.pdf", "Poista tiedostonimi.pdf").

**Miten testata manuaalisesti:**
1. Siirry sivulle, jossa voi lisätä liitetiedoston (esim. Arviointipyynto tai Asiakirjat-sivu).
2. Paina Tab-näppäintä, kunnes "Lisää liitetiedosto" -painike saa fokuksen – sen ympärille pitäisi ilmestyä kehys.
3. Paina Enter tai välilyönti – tiedostonvalintaikkuna pitäisi avautua.
4. Tarkista ruudunlukijalla: painikkeen roolin pitäisi olla "painike" ja nimen pitäisi vastata painikeen tekstiä.
5. Tarkista, että lataus- ja poistopainikkeet saavat kontekstuaalisen nimen (tiedoston nimi).

---

### 5. Lisätietopainikkeella ei ole saavutettavaa nimeä – WCAG 4.1.2, 2.1.1, 2.4.7

**Korjattu tiedosto:** `frontend/src/components/popover/popover.vue`

**Mitä korjattiin:**
- Lisätty info-painikkeelle `aria-label`-attribuutti. Painike käyttää nyt `title`-propsia tai oletuksena "Lisätietoja" (i18n-avain `lisatietoja`). Voidaan myös ohittaa `aria-label`-propilla.
- Lisätty `aria-expanded`-attribuutti, joka kertoo ruudunlukijalle, onko ponnahdusikkuna auki.
- Lisätty `aria-controls`-attribuutti, joka yhdistää painikkeen ohjaamaansa popover-elementtiin.
- Lisätty ikonille `aria-hidden="true"`, jolloin ruudunlukija ei lue ikonin alt-tekstiä.
- Sulkemispainikkeen `aria-label` muutettu suomeksi ("Sulje").

**Miten testata manuaalisesti:**
1. Siirry lomakesivulle, jossa on info-ikoni-painike (ⓘ).
2. Siirry Tab-näppäimellä painikkeelle.
3. Tarkista ruudunlukijalla: painike pitäisi kuulua nimellä, kuten "Lisätietoja" tai otsikon nimi.
4. Paina Enter tai välilyönti – ponnahdusikkuna avautuu.
5. Tarkista, kuuluuko `aria-expanded`-tila muuttuvan ("laajennettu"/"supistettu").
6. Sulkemispainike pitäisi kuulua "Sulje"-nimellä.

---

### 6. Monivalinta-pudotusvalikko (Multiselect) – WCAG 1.3.1, 1.3.2, 1.4.1, 1.4.3, 1.4.11, 2.1.1, 2.4.3, 2.4.7, 4.1.2

**Korjattu tiedosto:** `frontend/src/components/multiselect/multiselect.vue`

**Mitä korjattiin:**
- Lisätty tyhjennyspainikkeelle `aria-label="Tyhjennä valinta"` – aiemmin painike oli nimetön.
- Lisätty tyhjennyspainikkeelle näkyvä kohdistuskehys (`:focus`-tila).
- Lisätty ikonille `aria-hidden="true"`.
- Muutettu valittujen tagien taustaväri tummennetuksi (#005a8e), jolloin tekstikontrasti on vähintään 4.5:1 (aiemmin kontrasti oli vain ~3.1:1 liian vaalealla sinisellä).
- Lisätty tag-poistopainikkeen (×) kohdistustila, jolloin näkyvä kohdistuskehys ilmestyy.
- Parannettu valitun vaihtoehdon kontrasti listassa (teksti mustaksi, tausta vaaleammaksi tummemmaksi harmaiksi).
- Muutettu korostetun vaihtoehdon taustaväri selkeämmäksi sinertäväksi (#daedf8) ja lisätty sininen kehys (`outline: 2px solid $primary`).

**Miten testata manuaalisesti:**
1. Siirry lomakesivulle, jossa on monivalintakenttä (esim. erikoisala tai koulutuspaikka).
2. Paina Tab-näppäintä, kunnes kenttä saa fokuksen.
3. Paina alanuoli tai kirjoita hakutermi – lista pitäisi avautua, ja korostettu vaihtoehto näkyy kehystettynä.
4. Valitse vaihtoehto Enter-näppäimellä.
5. Tarkista, että valittu arvo näkyy tummana tagina riittävällä kontrastilla.
6. Kohdista tyhjennyspainike (×) ja tarkista, onko sillä näkyvä kehys. Paina Enter – valinnan pitäisi tyhjentyä. Tarkista ruudunlukijalla painikkeen nimi "Tyhjennä valinta".
7. Jos on monivalintatagin poistopainike (×), kohdista se Tab-näppäimellä ja tarkista kohdistuksen näkyvyys.

---

### 7. Virhetila ilmaistu vain värillä – WCAG 1.4.1

**Korjattu tiedostoista:** `frontend/src/components/form-error/form-error.vue`

**Mitä korjattiin:**
- Lisätty lomakeen yhteenvetovirheilmoitukselle `role="alert"` ja `aria-live="assertive"`, jolloin ruudunlukija lukee virheistä heti automaattisesti.
- Lisätty ikonille `aria-hidden="true"` (jotta ruudunlukija ei lue ikonin nimeä erikseen).

**Miten testata manuaalisesti:**
1. Avaa lomake (esim. työskentelyjakson lisäyslomake).
2. Yritä lähettää lomake tyhjänä tai virheellisillä tiedoilla.
3. Ruudunlukijalla (VoiceOver tai NVDA) pitäisi automaattisesti kuulua virheilmoitus.
4. Visuaalisesti virheilmoituksen pitäisi näkyä punaisena tekstinä ikonin kanssa.

---

## Prioriteetti 2 – Käyttöä merkittävästi vaikeuttavat ongelmat

### 8. Hyppylinkki pääsisältöön puuttuu – WCAG 2.4.1

**Korjattu tiedosto:** `frontend/src/app.vue`

**Mitä korjattiin:**
- Lisätty "Hyppää pääsisältöön" -linkki sovelluksen ensimmäiseksi elementiksi.
- Linkki on oletuksena visuaalisesti piilotettu mutta näkyy, kun se saa fokuksen (näppäimistökäyttäjille).
- Linkki vie `#main-content`-ankkuriin, joka on pääsisältöalueen wrapper-elementti.

**Miten testata manuaalisesti:**
1. Avaa mikä tahansa sivu Elsa-palvelussa.
2. Paina Tab-näppäintä sivun alussa.
3. Ensimmäisenä pitäisi ilmestyä sininen "Hyppää pääsisältöön" -painike sivun vasempaan yläkulmaan.
4. Paina Enter – fokuksen pitäisi siirtyä pääsisältöalueen alkuun, ohittaen navigaation.

---

### 9. SPA-näkymänvaihto ei ilmoita ruudunlukijalle – WCAG 2.4.3, 4.1.3

**Korjattu tiedostot:** `frontend/src/app.vue`

**Mitä korjattiin:**
- Lisätty `aria-live="polite"` -alue, joka ilmoittaa sivun otsikon ruudunlukijalle näkymänvaihdon jälkeen.
- Lisätty reittimuutoksen kuuntelija (`@Watch('$route')`), joka:
  - Päivittää live-alueen tekstin sivun otsikolla.
  - Siirtää näppäimistöfokuksen pääsisältöalueen alkuun (käyttää `tabindex="-1"` ja `focus()` väliaikaisesti).

**Miten testata manuaalisesti:**
1. Avaa Elsa-palvelu ja aktivoi ruudunlukija (VoiceOver tai NVDA).
2. Navigoi eri sivuille klikkaamalla navigaatiolinkkejä tai käyttämällä näppäimistöä.
3. Ruudunlukijan pitäisi ilmoittaa uuden sivun otsikko navigoinnin jälkeen.
4. Fokuksen pitäisi siirtyä pääsisältöalueen alkuun jokaisella sivunavigaatiolla.

---

### 10. Valikkopainikkeen aria-label on englanninkielinen – WCAG 3.3.2

**Korjattu tiedosto:** `frontend/src/components/navbar/navbar.vue`

**Mitä korjattiin:**
- Lisätty `b-navbar-toggle` -elementille suomenkielinen `aria-label="Avaa valikko"`.
- Ikonien `font-awesome-icon`-elementeille lisätty `aria-hidden="true"`, jolloin ruudunlukija ei lue ikonien nimiä (kuten "times" tai "bars").

**Miten testata manuaalisesti:**
1. Kaventele selaimen ikkunaa mobiilileveyteen (alle 992px) tai käytä mobiililaitetta.
2. Paina Tab-näppäintä, kunnes hamburger-valikko-painike saa fokuksen.
3. Ruudunlukijalla pitäisi kuulua "Avaa valikko" eikä "Menu" tai englanniksi "toggle navigation".

---

### 11. Profiilivalikon puutteellinen ohjelmallinen rakenne – WCAG 1.3.1

**Korjattu tiedosto:** `frontend/src/components/navbar/navbar.vue`

**Mitä korjattiin:**
- Lisätty `role="menu"` ja `aria-label="Käyttäjävalikko"` valikkosisältö-diviin.
- Kaikille valikkokohteille lisätty `role="menuitem"`.
- Lisätty `aria-current="true"` tällä hetkellä aktiiviselle opinto-oikeudelle ja roolille, jolloin apuvälinekäyttäjä tietää mikä vaihtoehto on valittuna.
- Kaikille check-ikoneille lisätty `aria-hidden="true"`.
- Lisätty profiili-dropdownille suomenkielinen otsikko (`displayName`).

**Miten testata manuaalisesti:**
1. Kirjaudu Elsa-palveluun.
2. Paina Tab-näppäintä, kunnes käyttäjäavatarin dropdown-painike saa fokuksen. Paina Enter.
3. Ruudunlukijalla pitäisi kuulua, että valikko avautuu ("Käyttäjävalikko").
4. Jos käyttäjällä on useita opinto-oikeuksia tai rooleja, aktiivinen vaihtoehto pitäisi kuulua merkittynä (esim. "Erikoistuva lääkäri, valittu").

---

### 12. Sivunavigaation listarakenne on rikki – WCAG 1.3.1, 4.1.2

**Korjattu tiedostot:** `frontend/src/components/sidebar-menu/sidebar-menu.vue`, `frontend/src/components/mobile-nav/mobile-nav.vue`

**Mitä korjattiin:**
- Lisätty `aria-label="Päänavigaatio"` sivunavigaation `<nav>`-elementille (desktop).
- Lisätty `aria-label="Päänavigaatio"` mobiilinavigaation `<b-nav>`-elementille.
- Muutettu "Osaaminen"-navigaatiokohta rikkinäisestä ankkuri-elementistä (linkki ilman `to`-propsia) semanttisesti oikeaksi `<button>`-elementiksi.
  - Lisätty `aria-expanded` ja `aria-controls="osaaminen-toggle"` buttonille.
  - Lisätty näkyvä kohdistuskehys.
  - Lisätty `aria-hidden="true"` ikonipiiloille (chevron-up/down).
- Vastaava korjaus tehty mobiilinavigaatioon.

**Miten testata manuaalisesti:**
1. Avaa Elsa-palvelu ja navigoi sivunavigaatioon Tab-näppäimellä.
2. Ruudunlukijalla pitäisi kuulua navigaatioalueen nimi "Päänavigaatio" kun alueelle navigoidaan.
3. Siirry "Osaaminen"-kohtaan – sen pitäisi käyttäytyä painikkeena linkin sijaan.
4. Paina Enter tai välilyönti "Osaaminen"-painikkeella – alavalikko avautuu/sulkeutuu.
5. Ruudunlukijalla pitäisi kuulua `aria-expanded`-tila: "laajennettu" / "supistettu".
6. Tarkista, ettei samalla sivulla ole tuplanappia (painike+linkki samassa elementissä).

---

### 13. Pakollisten kenttien merkintä epäselvä – WCAG 3.3.2

**Korjattu tiedosto:** `frontend/src/components/form-group/form-group.vue`

**Mitä korjattiin:**
- Lisätty `<span class="sr-only">{{ $t('pakollinen-tieto') }}</span>` asteriskin (*) yhteyteen. Ruudunlukija lukee nyt "pakollinen tieto" näkyvän asteriskin sijaan.
- Lisätty info-ikonille `aria-label="Lisätietoja"`, jolloin ruudunlukija lukee tarkoituksen ikonin fonttinimen sijaan.

**Miten testata manuaalisesti:**
1. Avaa lomake, jossa on pakollisia kenttiä (merkitty *).
2. Navigoi Tab-näppäimellä pakollisen kentän otsikkoon.
3. Ruudunlukijalla pitäisi kuulua "Kentän nimi, pakollinen tieto" pelkän "*" sijaan.
4. Jos kentällä on info-ikoni (ⓘ), sen pitäisi kuulua nimellä "Lisätietoja".

---

### 14. Toimintojen palaute puuttuu (toast/aria-live) – WCAG 4.1.3

**Korjattu tiedosto:** `frontend/src/utils/toast.ts`

**Mitä korjattiin:**
- Lisätty fontawesome-ikoneille `aria-hidden="true"`, jolloin ruudunlukija ei lue ikonien nimiä.
- Asetettu `autoHideDelay: 5000` (onnistumistoiminnot) ja `autoHideDelay: 7000` (virhetoiminnot) – enemmän aikaa kuulla ilmoitus.
- Käytetään Bootstrap Vue:n sisäänrakennettua `aria-live`-tukea (`variant: 'danger'` → `aria-live="assertive"`, `variant: 'success'` → `aria-live="polite"`).

**Miten testata manuaalisesti:**
1. Aktivoi ruudunlukija (VoiceOver/NVDA).
2. Tallenna lomake onnistuneesti – onnistumistoast pitäisi kuulua automaattisesti.
3. Aiheuta virhetilanne (esim. verkkovirhe tai virheellinen lomake) – virhetoast pitäisi kuulua välittömästi.
4. Ilmoituksella on pidemmän näyttöaika, joten ruudunlukijalla on aikaa lukea se.

---

## Prioriteetti 3 – Käyttöä vaikeuttavat ongelmat

### 15. Kirjautumissivulta puuttuu pääsisältömaamerkki – WCAG 1.3.1

**Korjattu tiedosto:** `frontend/src/views/login/login-view.vue`

**Mitä korjattiin:**
- Lisätty kirjautumissivulle `<main id="main-content">` -elementti, johon router-view on kapseloitu. Aiemmin kirjautumissivulla ei ollut lainkaan main-merkkipistettä, mikä poikkesi muun palvelun käytännöstä.

**Miten testata manuaalisesti:**
1. Avaa kirjautumissivu.
2. Tarkista ruudunlukijalla maamerkkiluettelo (VoiceOverissa rotor → Landmarks, NVDA:ssa Insert+F7).
3. Luettelossa pitäisi näkyä "Main" tai "Pääsisältö" -maamerkki.

---

### 16. Scroll-padding: kohdistunut elementti piiloutuu ylätunnisteen taakse – WCAG 2.4.11

**Korjattu tiedosto:** `frontend/src/styles/app.scss`

**Mitä korjattiin:**
- Lisätty `scroll-padding-top: 80px` HTML-elementille. Tämä varmistaa, että kun selain vierittää sivun automaattisesti fokusoidulle elementille (esim. Tab-näppäimellä navigoitaessa), elementti ei päädy kiinteän ylätunnisteen alle.

**Miten testata manuaalisesti:**
1. Avaa sivu, jolla on paljon sisältöä (esim. Työskentelyjaksot-sivu).
2. Paina Tab-näppäintä toistuvasti ja seuraa, siirtyykö fokus sivun alla oleviin elementteihin.
3. Tarkista, ettei fokusoidut elementit päädy piilotetuksi kiinteän ylätunnisteen alle.
4. Tämä on erityisen tärkeä mobiililla ja suurennetuilla näytöillä.

---

## Uudet i18n-lokalisointiavaimet

Seuraavat suomenkieliset avaimet lisättiin `fi.json`-tiedostoon:

| Avain | Suomenkielinen teksti |
|---|---|
| `hyppaa-paasisaltoon` | Hyppää pääsisältöön |
| `avaa-kalenteri` | Avaa kalenteri |
| `kuluva-kuukausi` | Kuluva kuukausi |
| `edellinen-kuukausi` | Edellinen kuukausi |
| `seuraava-kuukausi` | Seuraava kuukausi |
| `tanaan` | Tänään |
| `valittu` | Valittu |
| `tyhjenna-valinta` | Tyhjennä valinta |
| `sulje` | Sulje |
| `lisatietoja` | Lisätietoja |
| `avaa-valikko` | Avaa valikko |
| `sulje-valikko` | Sulje valikko |
| `kayttajavalikko` | Käyttäjävalikko |
| `paanavigaatio` | Päänavigaatio |
| `sivunavigaatio` | Sivunavigaatio |

Vastaavat ruotsinkieliset käännökset lisättiin `sv.json`-tiedostoon.

---

## Yhteenveto muutetuista tiedostoista

| Tiedosto | Korjatut ongelmat |
|---|---|
| `frontend/src/components/datepicker/datepicker.vue` | Kalenterin tabindex, aria-label, suomenkieliset nimet |
| `frontend/src/components/accordian/accordian.vue` | role="button", aria-expanded, näkyvä fokus |
| `frontend/src/styles/app.scss` | Globaalit fokustyylit, scroll-padding-top, sr-only |
| `frontend/src/app.vue` | Skip-link, aria-live-alue, route-kuuntelija |
| `frontend/src/components/multiselect/multiselect.vue` | Kontrasti, aria-label, fokustyylit |
| `frontend/src/components/popover/popover.vue` | aria-label, aria-expanded, aria-controls |
| `frontend/src/components/form-error/form-error.vue` | role="alert", aria-live |
| `frontend/src/components/navbar/navbar.vue` | Suomenkielinen aria-label, role="menu", aria-current |
| `frontend/src/components/mobile-nav/mobile-nav.vue` | aria-label, aria-expanded |
| `frontend/src/components/sidebar-menu/sidebar-menu.vue` | aria-label, semanttinen button |
| `frontend/src/components/form-group/form-group.vue` | sr-only pakollinen-tieto, aria-label |
| `frontend/src/components/asiakirjat/asiakirjat-upload.vue` | tabindex, role="button", näppäinkaappaus |
| `frontend/src/components/asiakirjat/asiakirjat-content.vue` | aria-label lataus/poisto-painikkeille |
| `frontend/src/utils/toast.ts` | aria-hidden ikoneille, autoHideDelay |
| `frontend/src/views/login/login-view.vue` | `<main>` -maamerkki |
| `frontend/src/locales/fi.json` | Uudet saavutettavuusavaimet |
| `frontend/src/locales/sv.json` | Uudet saavutettavuusavaimet (ruotsi) |

---

## Vielä korjaamattomat ongelmat

Seuraavat ongelmat tunnistettiin saavutettavuusarviossa mutta eivät ole mukana tässä korjauserässä. Nämä vaativat laajempia rakenteellisia muutoksia tai erillisen suunnittelun:

### Prioriteetti 1 (käyttöä estävät):
- **Single-select pudotusvalikko**: combobox-rooli, aria-expanded, listbox-rakenne ja ruudunlukijaongelmat vaativat putkiston uudelleensuunnittelun tai kolmannen osapuolen komponenttikirjaston päivityksen.
- **Multiselect: Tehdyt valinnat eivät näy DOM-järjestyksessä oikein**: Tämä vaatii vue-multiselect-komponentin laajaa muutosta tai uuden komponenttikirjaston käyttöönottoa.

### Prioriteetti 2 (käyttöä merkittävästi vaikeuttavat):
- **Otsikointirakenne (h1–h6)**: Monet sivut puuttuvat H1-otsikoita tai otsikkohierarkia on väärä. Tämä vaatii jokaisen sivun manuaalista läpikäyntiä.
- **Lomakkeen lähetysvirhe: fokus ensimmäiseen virhekenttään**: Tämä vaatii lomakedatan validointilogiikan muuttamista jokaisessa lomakekomponentissa.
- **label/for-kytkentä päivämääräkentille**: Bootstrap Vue:n `b-form-datepicker`-komponentti hoitaa label-kytkennän sisäisesti, mutta ulkoinen label-elementti on kytkettävä manuaalisesti.
- **Radiopainikeryhmien fieldset/legend-rakenne**: Vaatii jokaisen radiopainikeryhmän muuttamista.
- **Valintaruuturyhmien fieldset/legend-rakenne**: Sama kuin radiopainikkeet.

### Prioriteetti 3 (käyttöä vaikeuttavat):
- **SVG-piirakkakuvaajien tekstivastineet**: Vaatii erillisen visualisoinnin uudelleensuunnittelun.
- **Taulukkorakenne mobiilissa**: Taulukot muuttuvat korttimuotoon mutta säilyttävät taulukon ohjelmallisen rakenteen.
- **Murupolkuelementin nav-landmark ja erottimet**: b-breadcrumb-komponentin aria-label tulee englanninkielisenä – vaatii jokaiselle sivulle muutoksen tai oman wrapper-komponentin.

---

## Testaussuositus

Korjauksia suositellaan testattavan seuraavilla välineillä:

1. **Näppäimistökäyttö** (Chrome/Firefox): Käy kaikki lomakekentät, painikkeet, haitarit ja navigaatio Tab-näppäimellä läpi ja tarkista näkyvä kohdistus.
2. **NVDA + Firefox/Edge** (Windows): Tarkista erityisesti lomakkeiden virheviestit, toast-ilmoitukset ja navigaatiorakenne.
3. **VoiceOver + Safari** (macOS): Tarkista lomakekentät, haitarit, profiilivalikko ja näkymänvaihtokuulutukset.
4. **TalkBack + Chrome** (Android): Tarkista erikoistujan näkymät mobiililla.
5. **Kontrastin tarkistus**: Käytä axe-selainlaajennusta tai Colour Contrast Analyser -työkalua tarkistamaan jäljellä olevat kontrastiongelmat.

