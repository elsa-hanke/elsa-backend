# Saavutettavuuskorjaukset – Elsa-verkkopalvelu

**Versio:** 2.0 (päivitetty PDF-raportin pohjalta)
**Päiväys:** 31.5.2026  
**Perusta:** Gofore Oy:n saavutettavuusarviointi WCAG 2.2 A/AA, 9.12.2025 (Ruut Kiiskilä, Mikko Pärnä)  
**Priorisointi:** Asiakkaan prioriteettilistaus (priorisoitu korjausjärjestys 1–6)

---

## Yhteenveto

Tässä dokumentissa kuvataan kaikki Gofore Oy:n saavutettavuusraportin ja asiakkaan prioriteettilistauksen pohjalta toteutetut korjaukset Elsa-verkkopalvelun frontend-koodiin. Jokaiselle korjaukselle on kirjattu:
- Viittaus PDF-raportin sivuun
- Korjattu tiedosto
- Tekniset muutokset
- Testausohjeet

Lisäksi on toteutettu automaattiset saavutettavuustestit (`cypress-axe`), jotka löytyvät hakemistosta `e2e/cypress/e2e/saavutettavuus/`.

---

## Prioriteetti 1 – Käyttöä estävät ongelmat (kaikki käyttäjäroolit)

### 1. Näkyvä kohdistus puuttuu painikkeista – WCAG 2.4.7 (s. 9–10)

**Korjattu tiedosto:** `frontend/src/styles/app.scss`

**Tekninen muutos:**  
Poistettu CSS-sääntö `.btn:focus, .btn:active { outline: none !important; box-shadow: none; }`.  
Korvattu seuraavin säännöin:
- `.btn:focus` → näyttää `outline: 2px solid $primary` + `box-shadow`
- `.btn:focus:not(:focus-visible)` → poistaa kehyksen hiirenkäyttäjiltä (moderni selaintuki)
- `.btn:active` → ei kehystä klikkaustilassa
- Vastaava korjaus `ul.nav.nav-tabs li a.nav-link:focus` -elementeille
- Lisätty `.sr-only`-apuluokka koko sovelluksen käyttöön
- Lisätty `a:focus` ja `.nav-link:focus` fokustyylit

Lisätty myös `scroll-padding-top: 80px` estämään kohdistetun elementin piiloutuminen kiinteän ylätunnisteen taakse (WCAG 2.4.11*).

**Manuaalinen testaus:**
1. Paina Tab etusivulla – jokainen painikeinteraktio saa sinisen kehyksen.
2. Klikkaa samaa painiketta hiirellä – kehystä ei näy (`:focus-visible`).
3. Tarkista välilehdet ja haitarit.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/yleinen-sivurakenne.cy.ts` → "Näkyvä kohdistus painikkeissa"

---

### 2. Kalenteripainike ei fokusoidu näppäimistöllä – WCAG 2.1.1, 2.4.6, 2.4.7 (s. 32)

**Korjattu tiedosto:** `frontend/src/components/datepicker/datepicker.vue`

**Tekninen muutos:**
- Poistettu `mounted()`-koukussa suoritettu `button.setAttribute('tabindex', '-1')` — tämä esti täysin kalenteripainikkeen fokusoinnin.
- Lisätty `label-button-aria-label="$t('avaa-kalenteri')"` → painikkeen nimi suomeksi.
- Lisätty `<span class="sr-only">{{ $t('avaa-kalenteri') }}</span>` painikkeen sisälle ruudunlukijaa varten.
- Lisätty `label-prev-month`, `label-next-month`, `label-current-month`, `label-today`, `label-selected`.
- Muutettu piilotustyyli `display: none` → `position: absolute; clip: rect(0,0,0,0)` (ruudunlukija pääsee käsiksi).
- Ikonille `aria-hidden="true"`.

**Manuaalinen testaus:**
1. Siirry työskentelyjakson lisäämislomakkeelle.
2. Tab-näppäimellä pitäisi päästä kalenteripainikkeeseen.
3. Enter tai välilyönti avaa kalenterin.
4. VoiceOver/NVDA: painike kuuluu "Avaa kalenteri".
5. Kalenterin navigointipainikkeet kuuluvat suomeksi.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/datepicker.cy.ts`

---

### 3. Liitetiedoston lisääminen ei onnistu näppäimistöllä – WCAG 2.1.1, 4.1.2 (s. 30)

**Korjattu tiedosto:** `frontend/src/components/asiakirjat/asiakirjat-upload.vue`

**Tekninen muutos:**
- `tabindex="0"` ja `role="button"` label-elementille.
- `@keydown.enter.prevent="triggerFileInput"` ja `@keydown.space.prevent="triggerFileInput"`.
- `triggerFileInput()`-metodi kutsuu `input.click()`.
- `disabled` → `aria-disabled` (ei validi attribuutti labelilla).
- Näkyvä `focus`-tyyli labelille.

**Manuaalinen testaus:**
1. Siirry arviointipyyntölomakkeelle.
2. Tab liitetiedoston lisäyspainikkeelle → kehys näkyy.
3. Enter tai välilyönti → tiedostonvalintaikkuna avautuu.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/liitetiedosto.cy.ts`

---

### 4. Lataus/poisto-painikkeiden nimet puuttuvat – WCAG 4.1.2 (s. 30)

**Korjattu tiedosto:** `frontend/src/components/asiakirjat/asiakirjat-content.vue`

**Tekninen muutos:**
- Latauspainike: `:aria-label="\`${$t('lataa')} ${row.item.nimi}\`"`
- Poistopainike: `:aria-label="\`${$t('poista')} ${row.item.nimi}\`"`
- Ikoneille `aria-hidden="true"`.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/liitetiedosto.cy.ts` → "latauspainikkeella on aria-label"

---

### 5. Lisätietopainikkeilla ei saavutettavaa nimeä – WCAG 4.1.2, 2.1.1 (s. 31)

**Korjattu tiedosto:** `frontend/src/components/popover/popover.vue`

**Tekninen muutos:**
- `:aria-label="buttonAriaLabel"` → lukee `title`-propsista tai `$t('lisatietoja')`.
- `:aria-expanded="String(popoverShow)"`.
- `:aria-controls="\`${uid}-popover\`"` ja `id` popoverille.
- Sulkemispainike: `aria-label="Close"` → `$t('sulje')`.
- Ikonille `aria-hidden="true"`.

**Manuaalinen testaus:**
1. Siirry sivulle, jossa on info-ikoni (ⓘ).
2. Tab siihen → ruudunlukija lukee "Lisätietoja" tai otsikon nimen.
3. Enter → ponnahdusikkuna avautuu, `aria-expanded="true"`.
4. Sulje → `aria-expanded="false"`.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/popover.cy.ts`

---

### 6. Haitarielementit – väärä rooli ja puuttuva tila – WCAG 1.3.1, 4.1.2 (s. 43)

**Korjattu tiedosto:** `frontend/src/components/accordian/accordian.vue`

**Tekninen muutos:**
- `role="tab"` → `role="button"`.
- `:aria-expanded="$slots.default ? String(visible) : undefined"`.
- Tyhjille haitareille `tabindex="-1"`.
- `:focus`-tyyli: `outline: 2px solid $primary; outline-offset: -2px`.

**Manuaalinen testaus:**
1. Tab haitarille → sininen kehys.
2. Enter/välilyönti → avautuu/sulkeutuu.
3. Ruudunlukija: "laajennettu" / "supistettu".
4. Ei enää roolia "välilehti".

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/accordion.cy.ts`

---

### 7. Monivalinta-pudotusvalikko – kontrasti ja nimet – WCAG 1.3.1–4.1.2 (s. 35–36)

**Korjattu tiedosto:** `frontend/src/components/multiselect/multiselect.vue`

**Tekninen muutos:**
- Tyhjennyspainike: `aria-label="Tyhjennä valinta"`, ikoni `aria-hidden`.
- Tyhjennyspainike: näkyvä `:focus`-tyyli.
- Tagien taustaväri → `#005a8e` (kontrasti ≥ 4.5:1 valkoisella tekstillä).
- Tag-poistopainike: `:focus` → `outline: 2px solid $white`.
- Korostetun vaihtoehdon väri → sinertävä `#daedf8` + sininen kehys.
- Valitun vaihtoehdon väri → mustavalkoinen (`#c0c0c0` tausta + musta teksti).

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/arviointipyynto-a11y.cy.ts` → "multiselect-tyhjennyspainikkeen aria-label"

---

### 8. Virhetila ilmaistu vain värillä – WCAG 1.4.1 (s. 11)

**Korjattu tiedosto:** `frontend/src/components/form-error/form-error.vue`

**Tekninen muutos:**
- `role="alert"`, `aria-live="assertive"`, `aria-atomic="true"`.
- Ikonille `aria-hidden="true"`.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/lomakevirheet.cy.ts`

---

## Prioriteetti 2 – Käyttöä merkittävästi vaikeuttavat

### 9. Hyppylinkki pääsisältöön puuttuu – WCAG 2.4.1 (s. 24)

**Korjattu tiedosto:** `frontend/src/app.vue`

`<a href="#main-content" class="skip-to-main">Hyppää pääsisältöön</a>` sovelluksen ensimmäiseksi elementiksi. Oletuksena `top: -9999px`, fokusoitaessa `top: 0`.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/yleinen-sivurakenne.cy.ts`

---

### 10. SPA-navigointi ei ilmoita ruudunlukijalle – WCAG 2.4.3, 4.1.3 (s. 15)

**Korjattu tiedosto:** `frontend/src/app.vue`

`aria-live="polite"` -alue + `@Watch('$route')` siirtää fokuksen `#main-content`-elementtiin ja päivittää live-alueen sivun otsikolla.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/yleinen-sivurakenne.cy.ts`

---

### 11. Valikkopainikkeen aria-label englanninkielinen – WCAG 3.3.2 (s. 56)

**Korjattu tiedosto:** `frontend/src/components/navbar/navbar.vue`

`aria-label="$t('avaa-valikko')"` → "Avaa valikko". Ikonit `aria-hidden="true"`.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/navigaatio.cy.ts`

---

### 12. Profiilivalikon puutteellinen ohjelmallinen rakenne – WCAG 1.3.1 (s. 39)

**Korjattu tiedosto:** `frontend/src/components/navbar/navbar.vue`

`role="menu"` + `aria-label="Käyttäjävalikko"` valikkosisältöön. `role="menuitem"` kohteille. `aria-current="true"` aktiiviselle valinnalle.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/navigaatio.cy.ts`

---

### 13. Sivunavigaation listarakenne rikki – WCAG 1.3.1, 4.1.2 (s. 38)

**Korjattu tiedostot:** `sidebar-menu.vue`, `mobile-nav.vue`

`aria-label="Päänavigaatio"` navigaatioelementeille. "Osaaminen" muutettu `<button>`-elementiksi `aria-expanded`-attribuutilla.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/navigaatio.cy.ts`

---

### 14. Pakollisten kenttien merkintä epäselvä – WCAG 3.3.2 (s. 27)

**Korjattu tiedosto:** `frontend/src/components/form-group/form-group.vue`

`<span class="sr-only">pakollinen tieto</span>` asteriskin yhteyteen. Info-ikonille `aria-label="Lisätietoja"`.

**Automaattitesti:** `e2e/cypress/e2e/saavutettavuus/lomakevirheet.cy.ts`

---

### 15. Toimintojen palaute puuttuu (toast) – WCAG 4.1.3 (s. 25)

**Korjattu tiedosto:** `frontend/src/utils/toast.ts`

Ikonit `aria-hidden`. `autoHideDelay: 5000/7000`. BV käyttää `aria-live` sisäisesti varianttikohtaisesti.

---

### 16. Kirjautumissivulta puuttuu main-maamerkki – WCAG 1.3.1 (s. 48)

**Korjattu tiedosto:** `frontend/src/views/login/login-view.vue`

Lisätty `<main id="main-content">`.

---

### 17. Kohdistunut elementti piiloutuu ylätunnisteen alle – WCAG 2.4.11* (s. 61)

**Korjattu tiedosto:** `frontend/src/styles/app.scss`

`scroll-padding-top: 80px` HTML-elementille.

---

## Automaattiset saavutettavuustestit

Sijainti: `e2e/cypress/e2e/saavutettavuus/`

| Testitiedosto | Testattava alue |
|---|---|
| `yleinen-sivurakenne.cy.ts` | Skip-link, main-landmark, nav aria-label, SPA-ilmoitus, fokus |
| `datepicker.cy.ts` | Kalenteripainike: fokusoitavuus, nimi, navigointi |
| `accordion.cy.ts` | Haitari: rooli, aria-expanded, näppäimistö, fokus |
| `liitetiedosto.cy.ts` | Tiedostolatauspainike: tabindex, role, aria-label |
| `popover.cy.ts` | Info-painike: aria-label, aria-expanded, fokus |
| `navigaatio.cy.ts` | Navbar: suomenkielinen label, role="menu", sivunavigaatio |
| `lomakevirheet.cy.ts` | role="alert", aria-live, sr-only pakollinen |
| `arviointipyynto-a11y.cy.ts` | Integraatiotesti: kaikki korjatut komponentit |

`cy.checkPageA11y()` ajaa axe-core WCAG 2.1 AA -säännöt.  
Tuntemat puutteet on poissuljettu `KNOWN_UNFIXED_VIOLATIONS`-listalla `ui.ts`:ssä.

### Asentaminen

```bash
cd e2e
yarn add --dev cypress-axe axe-core
```

---

## Vielä korjaamattomat ongelmat – yksityiskohtaiset estäjät

### 🔴 Prioriteetti 1 – Estävät

#### Single-select ja multiselect: combobox-rooli, aria-tilat, DOM-järjestys (s. 33–36)
**WCAG:** 1.3.1, 1.3.2, 1.4.3, 1.4.11, 2.4.3, 4.1.2  
**Estäjä:** `vue-multiselect@2.1.6` ei toteuta WAI-ARIA 1.1 combobox-patternia. NVDA lukee kaikki vaihtoehdot "blank". Tagien DOM-sijainti on kirjastokoodissa eikä konfiguroitavissa. Korjaus = kirjastovaihto `@vueform/multiselect` tai vastaavaan. Vaatii kaikkien ~50 käyttöpaikan regressiotestauksen.  
**Arvioitu työmäärä:** 5–7 päivää.

#### Ei-natiivit pudotusvalikot mobiiliruudunlukijalla (s. 55)
**WCAG:** 4.1.2  
**Estäjä:** TalkBack/iOS VoiceOver eivät tue `vue-multiselect`-pohjaista komponenttia. Vaatii saman kirjastovaihdon.

---

### 🟡 Prioriteetti 2 – Merkittävät

#### Lomakkeen lähetysvirhe: fokus ensimmäiseen virhekenttään (s. 16)
**WCAG:** 1.3.1  
**Estäjä:** Noin 20 lomakekomponenttia, joissa on oma Vuelidate-validaatiologiikka. Korjausmalli on selvä (`this.$el.querySelector('.is-invalid')?.focus()`) mutta vaatii muutoksen jokaiseen erikseen.  
**Arvioitu työmäärä:** 2–3 päivää.

#### Label/for-kytkentä puuttuu päivämääräkentiltä (s. 20)
**WCAG:** 1.3.1  
**Estäjä:** `b-form-datepicker` (Bootstrap Vue) yliajaa ulkoiset label-kytkennät sisäisellä rakenteellaan. Korjaus vaatisi päivämääräkomponentin korvaamisen tai Bootstrap Vue:n laajentamisen.  
**Arvioitu työmäärä:** 1–2 päivää.

#### Otsikointirakenne puutteellinen (s. 18)
**WCAG:** 1.3.1  
**Estäjä:** Sivukohtainen ongelma ~15 sivulla. Ei yhtä komponenttia korjattavaksi – vaatii jokaisen sivun manuaalisen tarkistuksen.  
**Arvioitu työmäärä:** 2–3 päivää.

#### Radiopainikeryhmät ilman fieldset/legend (s. 41)
**WCAG:** 1.3.1  
**Estäjä:** Toistuu useilla lomakkeilla ja erityisesti arviointityökalut-modaali-ikkunoissa, joissa haitari+radio-yhdistelmä on monimutkainen.  
**Arvioitu työmäärä:** 1–2 päivää.

#### Valintaruuturyhmät ilman fieldset/legend (s. 42)
**WCAG:** 1.3.1  
**Estäjä:** Sama kuin radiopainikeryhmillä. Lisäksi Valmistumispyyntö-sivulla erityinen kohdistuksen katoaminen painikkeen poistuessa DOM:sta.  
**Arvioitu työmäärä:** 1 päivä.

#### Virheviestit eivät ohjelmallisesti kytketty kenttiin (s. 16)
**WCAG:** 1.3.1  
**Estäjä:** `elsa-form-group`-komponentti ei välitä `aria-describedby`-kytkentää automaattisesti. Vaatii komponentin laajentamisen ja kaikkien lomakekäyttöpaikkojen päivittämisen.  
**Arvioitu työmäärä:** 1–2 päivää.

#### Kontekstinmuutos kouluttajan hakukentässä (s. 23)
**WCAG:** 3.2.2  
**Estäjä:** Kouluttajan etusivun live-haku siirtää kohdistusta odottamattomasti. Vaatii debounce-logiikan lisäämisen ja hakupainikkeen lisäämistä tai ohjetekstin lisäämistä.  
**Arvioitu työmäärä:** 0.5–1 päivä.

#### Arviointityökalun valinnasta avautuu suoraan modaali (s. 23)
**WCAG:** 3.2.2  
**Estäjä:** Kouluttajan arviointinäkymässä valinta pudotusvalikosta avaa suoraan modaalin. Vaatii UX-päätöksen (erillinen painike) ja logiikan muutoksen.  
**Arvioitu työmäärä:** 0.5–1 päivä.

---

### 🟠 Prioriteetti 3 – Vaikeuttavat

#### SVG-piirakkakuvaajien tekstivastineet (s. 28)
**WCAG:** 1.1.1  
**Estäjä:** `vue-apexcharts` ei tue SVG-sektoreiden ARIA-annotointia. Vaatii kirjastovaihdon tai SVG-post-prosessointia.  
**Arvioitu työmäärä:** 3–5 päivää.

#### Murupolun nav-landmark ja erottimet (s. 40)
**WCAG:** 1.3.1, 4.1.2  
**Estäjä:** `b-breadcrumb` renderöi `aria-label="breadcrumb"` englanniksi. Korjaus: oma wrapper-komponentti tai jokaisen käyttöpaikan muuttaminen.  
**Arvioitu työmäärä:** 0.5 päivää.

#### Riittämätön tekstikontrasti (s. 12–13)
**WCAG:** 1.4.3  
**Puutteet:** `$red: #fb462f` (3.5:1), `$green: #41b257` toasteissa (2.7:1), harmaat ohjetekstit `#808080` (3.9:1)  
**Estäjä:** Globaalit värimuuttujat `_variables.scss`. Muutos vaikuttaa koko sovellukseen, vaatii UI-suunnittelijan hyväksynnän ja visuaalisen regressiotestauksen.  
**Arvioitu työmäärä:** 1 päivä + visuaalinen tarkistus.

#### Riittämätön kontrasti UI-elementeissä (s. 14)
**WCAG:** 1.4.11  
**Puutteet:** Lomakekenttien ääriviivat `#ced4da` (1.5:1), pudotusvalikoiden nuolikuvake `#999999` (2.8:1)  
**Estäjä:** Globaali muuttuja. Muutos vaikuttaa kaikkiin lomakekenttiin visuaalisesti.  
**Arvioitu työmäärä:** 0.5 päivää.

#### Taulukoiden esitys mobiilissa (s. 60)
**WCAG:** 1.3.1  
**Estäjä:** Bootstrap Vue `b-table` stacked-moodissa menettää taulukkosemantiikan. Vaatii custom-responsiivisen toteutuksen.  
**Arvioitu työmäärä:** 2–3 päivää.

---

## Muutetut tiedostot

| Tiedosto | Muutos |
|---|---|
| `components/datepicker/datepicker.vue` | tabindex=-1 poistettu, aria-label, suomenkieliset nimet |
| `components/accordian/accordian.vue` | role="button", aria-expanded, fokustyylit |
| `styles/app.scss` | Fokustyylit, scroll-padding-top, .sr-only |
| `app.vue` | Skip-link, aria-live, route-watch |
| `components/multiselect/multiselect.vue` | aria-label, tagikontrastit, fokustyylit |
| `components/popover/popover.vue` | aria-label, aria-expanded, aria-controls |
| `components/form-error/form-error.vue` | role="alert", aria-live="assertive" |
| `components/navbar/navbar.vue` | aria-label="Avaa valikko", role="menu", aria-current |
| `components/mobile-nav/mobile-nav.vue` | aria-label, aria-expanded |
| `components/sidebar-menu/sidebar-menu.vue` | aria-label="Päänavigaatio", `<button>` |
| `components/form-group/form-group.vue` | sr-only pakollinen, aria-label |
| `components/asiakirjat/asiakirjat-upload.vue` | tabindex, role="button", Enter/Space |
| `components/asiakirjat/asiakirjat-content.vue` | aria-label lataukselle/poistolle |
| `utils/toast.ts` | aria-hidden, autoHideDelay |
| `views/login/login-view.vue` | `<main>`-maamerkki |
| `locales/fi.json` | 15 uutta saavutettavuusavainta |
| `locales/sv.json` | 15 uutta avainta |
| `e2e/package.json` | cypress-axe + axe-core |
| `e2e/cypress/support/e2e.ts` | cypress-axe import |
| `e2e/cypress/support/commands/ui.ts` | checkPageA11y, assertFocusVisible |
| `e2e/cypress/e2e/saavutettavuus/*.cy.ts` | 8 uutta testitiedostoa |
| `e2e/cypress/e2e/arvioinnit/arviointipyynto.cy.ts` | cy.checkPageA11y() lisätty |

---

## Seuraavat askeleet (suositus)

1. **Heti (sprint 1):** `vue-multiselect` → saavutettava vaihtoehto – suurin yksittäinen estäjä (koskee kaikkia rooleja, prio 1).
2. **Sprint 2:** Lomakefokus virhetilanteessa, `aria-describedby`-kytkennät, radiopaikeryhmät, kontekstinmuutokset kouluttajan näkymissä.
3. **Sprint 3:** Otsikointiaudit kaikilla sivuilla, kontrasteKorjaukset UI-suunnittelijan kanssa.
4. **Sprint 4:** SVG-kuvaajien tekstivastineet, taulukkosemantiikka mobiilissa.
