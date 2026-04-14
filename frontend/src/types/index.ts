import { LocaleMessages } from 'vue-i18n'

import {
  ArvioinninPerustuminen,
  ArviointiasteikkoTyyppi,
  ArviointiasteikonTasoTyyppi,
  ErikoisalaTyyppi,
  KaytannonKoulutusTyyppi,
  KehittamistoimenpideKategoria,
  TyoskentelyjaksoTyyppi,
  PoissaolonSyyTyyppi,
  SeurantajaksoTila,
  OpintooikeusTila,
  OpintosuoritusTyyppiEnum,
  LomakeTyypit,
  LomakeTilat,
  KayttajatiliTila,
  ReassignedVastuuhenkilonTehtavaTyyppi,
  AvoinAsiaTyyppi,
  TerveyskeskuskoulutusjaksonTila,
  ValmistumispyynnonTila,
  ArviointityokaluKysymysTyyppi,
  ArviointityokaluTila,
  ArviointityokaluKysymysVaihtoehtoTyyppi
} from '@/utils/constants'
import { ValmistumispyynnonHyvaksyjaRole } from '@/utils/roles'

export type Opintooikeus = {
  id: number
  opintooikeudenMyontamispaiva: string
  opintooikeudenPaattymispaiva: string
  opiskelijatunnus: string
  osaamisenArvioinninOppaanPvm: string
  yliopistoNimi: string
  erikoisalaId: number
  erikoisalaNimi: string
  erikoisalaLiittynytElsaan: boolean | null
  asetus: Asetus
  tila: OpintooikeusTila
  opintoopasId: number
  opintoopasNimi: string
}

export type ErikoistuvaLaakari = {
  id: number | null
  nimi: string
  sahkoposti: string
  puhelinnumero: string | null
  syntymaaika: string
  kayttajaId: number
  yliopisto: string
  yliopistoId: number
  opintooikeudet: Opintooikeus[]
  opintooikeusKaytossaId: number
}

export interface UserAccount {
  activated: boolean
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  authorities: string[]
  erikoistuvaLaakari: ErikoistuvaLaakari
  id: string
  login: string
  langKey: string
  eppn: string
  createdBy: string
  createdDate: string
  lastModifiedBy: string
  lastModifiedDate: string
}

export interface ArviointipyyntoLomake {
  tyoskentelyjaksot: Tyoskentelyjakso[]
  kunnat: Kunta[]
  erikoisalat: Erikoisala[]
  arvioitavanKokonaisuudenKategoriat: ArvioitavanKokonaisuudenKategoria[]
  kouluttajatAndVastuuhenkilot: Kayttaja[]
}

export interface SuoritemerkintaLomake {
  tyoskentelyjaksot: Tyoskentelyjakso[]
  kunnat: Kunta[]
  erikoisalat: Erikoisala[]
  suoritteenKategoriat: SuoritteenKategoria[]
  arviointiasteikko: Arviointiasteikko
}

export interface TyoskentelyjaksoLomake {
  kunnat: Kunta[]
  erikoisalat: Erikoisala[]
  reservedAsiakirjaNimet: string[]
}

export interface Koulutussuunnitelma {
  id: number | null
  motivaatiokirje: string | null
  motivaatiokirjeYksityinen: boolean
  opiskeluJaTyohistoria: string | null
  opiskeluJaTyohistoriaYksityinen: boolean
  vahvuudet: string | null
  vahvuudetYksityinen: boolean
  tulevaisuudenVisiointi: string | null
  tulevaisuudenVisiointiYksityinen: boolean
  osaamisenKartuttaminen: string | null
  osaamisenKartuttaminenYksityinen: boolean
  elamankentta: string | null
  elamankenttaYksityinen: boolean
  koulutussuunnitelmaAsiakirja?: Asiakirja | null
  motivaatiokirjeAsiakirja?: Asiakirja | null
  koulutussuunnitelmaFile?: File | null
  koulutussuunnitelmaAsiakirjaUpdated: boolean
  motivaatiokirjeFile?: File | null
  motivaatiokirjeAsiakirjaUpdated: boolean
  muokkauspaiva: string | null
}

export interface Koulutusjakso {
  id: number
  nimi: string
  muutOsaamistavoitteet: string | null
  luotu: string | null
  tallennettu: string | null
  lukittu: boolean | null
  tyoskentelyjaksot: Tyoskentelyjakso[]
  osaamistavoitteet: ArvioitavaKokonaisuus[]
  koulutussuunnitelma: Koulutussuunnitelma
}

export interface KoulutusjaksoForm {
  id: number | null
  nimi: string | null
  muutOsaamistavoitteet: string | null
  luotu: string | null
  tallennettu: string | null
  lukittu: boolean | null
  tyoskentelyjaksot: Partial<Tyoskentelyjakso>[]
  osaamistavoitteet: ArvioitavaKokonaisuus[]
  koulutussuunnitelma: Koulutussuunnitelma | null
}

export interface KoulutusjaksoLomake {
  tyoskentelyjaksot: Tyoskentelyjakso[]
  kunnat: Kunta[]
  arvioitavanKokonaisuudenKategoriat: ArvioitavanKokonaisuudenKategoria[]
}

export interface Tyoskentelyjakso {
  id?: number | null
  alkamispaiva: string
  paattymispaiva: string | null
  minPaattymispaiva: string | null
  maxAlkamispaiva: string | null
  osaaikaprosentti: number | null
  kaytannonKoulutus: KaytannonKoulutusTyyppi
  hyvaksyttyAiempaanErikoisalaan: boolean | null
  tyoskentelypaikka: Tyoskentelypaikka
  omaaErikoisalaaTukevaId?: number
  omaaErikoisalaaTukeva: Erikoisala | null
  tapahtumia?: boolean
  liitettyKoejaksoon?: boolean
  asiakirjat?: Asiakirja[]
  label?: string
  poissaolot?: Keskeytysaika[] | null
  liitettyTerveyskeskuskoulutusjaksoon?: boolean
}

export interface TyoskentelyjaksoForm {
  id?: number | null
  alkamispaiva: string | null
  paattymispaiva: string | null
  minPaattymispaiva: string | null
  maxAlkamispaiva: string | null
  osaaikaprosentti: number | null
  kaytannonKoulutus: KaytannonKoulutusTyyppi | null
  hyvaksyttyAiempaanErikoisalaan: boolean | null
  tyoskentelypaikka: TyoskentelypaikkaForm
  omaaErikoisalaaTukeva: Erikoisala | null
  asiakirjat?: Asiakirja[]
  label?: string
}

export interface Tyoskentelypaikka {
  id: number
  nimi: string
  tyyppi: TyoskentelyjaksoTyyppi
  muuTyyppi: string | null
  kuntaId: string
  kunta: Kunta
}

export interface TyoskentelypaikkaForm {
  nimi: string | null
  tyyppi: TyoskentelyjaksoTyyppi | null
  muuTyyppi: string | null
  kunta: Kunta | null
}

export interface Kunta {
  id?: number | null
  abbreviation: string | null
  shortName?: string
  longName?: string
  description?: string
  kortnamn?: string
  korvaavaKoodi?: string
  langtNamn?: string
  maakunta?: string
  sairaanhoitopiiri?: string
}

export interface Erikoisala {
  id: number
  nimi: string | null
  tyyppi: ErikoisalaTyyppi | null
  vastuuhenkilonTehtavatyypit: VastuuhenkilonTehtava[]
}

export interface Opintoopas {
  id?: number | null
  nimi: string | null
  nimiSv: string | null
  voimassaoloAlkaa: string
  voimassaoloPaattyy: string | null
  kaytannonKoulutuksenVahimmaispituusVuodet: number
  kaytannonKoulutuksenVahimmaispituusKuukaudet: number
  terveyskeskuskoulutusjaksonVahimmaispituusVuodet: number
  terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet: number
  terveyskeskuskoulutusjaksonMaksimipituusVuodet?: number
  terveyskeskuskoulutusjaksonMaksimipituusKuukaudet?: number
  yliopistosairaalajaksonVahimmaispituusVuodet: number
  yliopistosairaalajaksonVahimmaispituusKuukaudet: number
  yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet: number
  yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet: number
  yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet?: number
  yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet?: number
  erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: number
  erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara: number
  erikoisalanVaatimaJohtamisopintojenVahimmaismaara: number
  erikoisala: Erikoisala
  arviointiasteikkoId: number
  arviointiasteikkoNimi: string
}

export interface UusiOpintoopas {
  nimi: string | null
  nimiSv: string | null
  voimassaoloAlkaa: string | null
  voimassaoloPaattyy: string | null
  kaytannonKoulutuksenVahimmaispituusVuodet: number | null
  kaytannonKoulutuksenVahimmaispituusKuukaudet: number | null
  terveyskeskuskoulutusjaksonVahimmaispituusVuodet: number | null
  terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet: number | null
  terveyskeskuskoulutusjaksonMaksimipituusVuodet?: number | null
  terveyskeskuskoulutusjaksonMaksimipituusKuukaudet?: number | null
  yliopistosairaalajaksonVahimmaispituusVuodet: number | null
  yliopistosairaalajaksonVahimmaispituusKuukaudet: number | null
  yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet: number | null
  yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet: number | null
  yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet?: number | null
  yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet?: number | null
  erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: number | null
  erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara: number | null
  erikoisalanVaatimaJohtamisopintojenVahimmaismaara: number | null
  erikoisala: Erikoisala | null
  arviointiasteikkoId: number | null
}

export interface OpintoopasSimple {
  id?: number | null
  nimi: string | null
  voimassaoloAlkaa: string
  voimassaoloPaattyy: string | null
  erikoisalaId: number | null
}

export interface OpintosuorituksetWrapper {
  opintosuoritukset: Opintosuoritus[] | null
  johtamisopinnotSuoritettu: number | null
  johtamisopinnotVaadittu: number | null
  sateilysuojakoulutuksetSuoritettu: number | null
  sateilysuojakoulutuksetVaadittu: number | null
}

export interface Opintosuoritus {
  id: number | null
  nimi_fi: string | null
  nimi_sv: string | null
  kurssikoodi: string | null
  tyyppi: OpintosuoritusTyyppi | null
  suorituspaiva: string
  opintopisteet: number | null
  hyvaksytty: boolean | null
  arvio_fi: string | null
  arvio_sv: string | null
  vanhenemispaiva: string | null
  yliopistoOpintooikeusId: string | null
  osakokonaisuudet: OpintosuoritusOsakokonaisuus[] | null
}

export interface OpintosuoritusTyyppi {
  id: number
  nimi: OpintosuoritusTyyppiEnum
}

export interface OpintosuoritusOsakokonaisuus {
  id: number | null
  nimi_fi: string | null
  nimi_sv: string | null
  kurssikoodi: string | null
  suorituspaiva: string
  opintopisteet: number | null
  hyvaksytty: boolean | null
  arvio_fi: string | null
  arvio_sv: string | null
  vanhenemispaiva: string | null
}

export type Koulutuspaikka = {
  erikoisala: string
  nimi: string
  id: number | null
  koulutussopimusOmanYliopistonKanssa: boolean | null
  yliopisto: string
  yliopistoId: number | null
}

export type Kouluttaja = {
  id: number | null
  kayttajaId: number | null
  kuittausaika: string
  lahiosoite: string
  nimi: string
  nimike: string
  postitoimipaikka: string
  puhelin: string
  sahkoposti: string
  sopimusHyvaksytty: boolean
  toimipaikka: string
}

export type Vastuuhenkilo = {
  id: number | null
  kuittausaika: string
  nimi: string
  nimike: string
  sopimusHyvaksytty: boolean
  puhelin?: string
  sahkoposti?: string
}

export interface KoulutussopimusLomake {
  erikoistuvanNimi: string
  erikoistuvanErikoisala?: string
  erikoistuvanOpiskelijatunnus?: string
  erikoistuvanPuhelinnumero: string
  erikoistuvanSahkoposti: string
  erikoistuvanSyntymaaika: string
  erikoistuvanYliopisto?: string
  erikoistuvanYliopistoId?: number | null
  erikoistuvanAvatar?: string
  id: number | null
  koejaksonAlkamispaiva: string
  korjausehdotus: string
  vastuuhenkilonKorjausehdotus: string
  kouluttajat: Kouluttaja[]
  koulutuspaikat: Koulutuspaikka[]
  lahetetty: boolean
  muokkauspaiva: string
  opintooikeudenMyontamispaiva?: string
  opintooikeudenPaattymispaiva?: string
  erikoistuvanAllekirjoitusaika?: string
  yliopistot: Yliopisto[]
  vastuuhenkilo: Vastuuhenkilo | null
}

export type KoejaksonVaiheHyvaksyja = {
  id: number | null
  kayttajaUserId: string | null
  kuittausaika: string
  nimi: string
  nimike: string | null
  sopimusHyvaksytty: boolean
}

export type BlobDataResult = {
  data?: any
  contentType?: string
  error?: boolean
}

export interface AloituskeskusteluLomake {
  erikoistuvanErikoisala?: string
  erikoistuvanNimi: string
  erikoistuvanOpiskelijatunnus?: string
  erikoistuvanSahkoposti: string
  erikoistuvanYliopisto: string
  erikoistuvanAvatar?: string
  id: number | null
  koejaksonAlkamispaiva: string
  koejaksonOsaamistavoitteet: string
  koejaksonPaattymispaiva: string
  koejaksonSuorituspaikka: string
  koejaksonToinenSuorituspaikka: string
  korjausehdotus: string
  lahetetty: boolean
  lahiesimies: KoejaksonVaiheHyvaksyja
  lahikouluttaja: KoejaksonVaiheHyvaksyja
  muokkauspaiva: string
  suoritettuKokoaikatyossa: boolean | null
  tyotunnitViikossa: number | null
  erikoistuvanKuittausaika?: string
}

export interface ValiarviointiLomake {
  edistyminenTavoitteidenMukaista: boolean | null
  erikoistuvanKuittausaika?: string
  erikoistuvanErikoisala: string
  erikoistuvanNimi: string
  erikoistuvanOpiskelijatunnus: string
  erikoistuvanYliopisto: string
  erikoistuvanAvatar?: string
  id: number | null
  kehittamistoimenpideKategoriat: KehittamistoimenpideKategoria[] | null
  kehittamistoimenpiteet: string
  korjausehdotus: string
  lahiesimies: KoejaksonVaiheHyvaksyja
  lahikouluttaja: KoejaksonVaiheHyvaksyja
  muokkauspaiva: string
  muuKategoria: string | null
  vahvuudet: string
  koejaksonOsaamistavoitteet: string
}

export interface KehittamistoimenpiteetLomake {
  erikoistuvanKuittausaika?: string
  erikoistuvanErikoisala: string
  erikoistuvanNimi: string
  erikoistuvanOpiskelijatunnus: string
  erikoistuvanYliopisto: string
  erikoistuvanAvatar?: string
  id: number | null
  kehittamistoimenpiteetRiittavat: boolean | null
  korjausehdotus: string
  lahiesimies: KoejaksonVaiheHyvaksyja
  lahikouluttaja: KoejaksonVaiheHyvaksyja
  muokkauspaiva: string
  kehittamistoimenpideKategoriat: KehittamistoimenpideKategoria[]
  muuKategoria: string | null
  kehittamistoimenpiteetKuvaus: string | null
}

export interface LoppukeskusteluLomake {
  erikoistuvanKuittausaika?: string
  erikoistuvanErikoisala: string
  erikoistuvanNimi: string
  erikoistuvanOpiskelijatunnus: string
  erikoistuvanYliopisto: string
  erikoistuvanAvatar?: string
  esitetaanKoejaksonHyvaksymista: boolean | null
  id: number | null
  jatkotoimenpiteet: string | null
  korjausehdotus: string
  lahiesimies: KoejaksonVaiheHyvaksyja
  lahikouluttaja: KoejaksonVaiheHyvaksyja
  muokkauspaiva: string
  koejaksonOsaamistavoitteet: string
  edistyminenTavoitteidenMukaista: boolean
  kehittamistoimenpideKategoriat: []
  muuKategoria: string | null
  vahvuudet: string
  kehittamistoimenpiteet: string
  kehittamistoimenpiteetRiittavat: boolean
  koejaksonPaattymispaiva: string
}

export interface VastuuhenkilonArvioLomake {
  erikoistuvanErikoisala: string
  erikoistuvanNimi: string
  erikoistuvanOpiskelijatunnus: string
  erikoistuvanYliopisto: string
  erikoistuvanAvatar?: string
  erikoistuvanSahkoposti?: string
  erikoistuvanPuhelinnumero?: string
  koejaksoHyvaksytty: boolean | null
  id: number | null
  muokkauspaiva: string
  muutOpintooikeudet: Opintooikeus[]
  paataOpintooikeudet: boolean
  vastuuhenkilo: Vastuuhenkilo | null
  virkailijanKorjausehdotus?: string | null
  vastuuhenkilonKorjausehdotus?: string | null
  hylattyArviointiKaytyLapiKeskustellen: boolean | null
  vastuuhenkilonKuittausaika?: string
  koulutussopimusHyvaksytty?: boolean
  vastuuhenkilonPuhelinnumero?: string
  vastuuhenkilonSahkoposti?: string
  erikoistuvanKuittausaika?: string | null
  virkailija?: KoejaksonVaiheHyvaksyja
  lisatiedotVirkailijalta?: string | null
  virkailijanYhteenveto?: string | null
  koejaksonSuorituspaikat?: TyoskentelyjaksotTable | null
  aloituskeskustelu?: AloituskeskusteluLomake
  valiarviointi?: ValiarviointiLomake
  kehittamistoimenpiteet?: KehittamistoimenpiteetLomake
  loppukeskustelu?: LoppukeskusteluLomake
  tila?: string
  perusteluHylkaamiselle?: string | null
  asiakirjat?: Asiakirja[] | null
  arkistoitava?: boolean
}

export interface VastuuhenkilonArvioLomakeErikoistuva {
  muutOpintooikeudet: Opintooikeus[]
  vastuuhenkilo: Vastuuhenkilo | null
  koulutussopimusHyvaksytty: boolean
  paataOpintooikeudet: boolean
  tyoskentelyjaksoLiitetty: boolean
  tyoskentelyjaksonPituusRiittava: boolean
  tyotodistusLiitetty: boolean
}

export interface PoissaoloLomake {
  tyoskentelyjaksot: any[]
  poissaolonSyyt: any[]
}

export interface Poissaolo {
  id?: number
  alkamispaiva?: string
  paattymispaiva?: string
  poissaoloprosentti?: number
  poissaolonSyyId: number
  tyoskentelyjaksoId: number
  poissaolonSyy?: PoissaolonSyy
  tyoskentelyjakso?: Tyoskentelyjakso
  kokoTyoajanPoissaolo?: boolean
}

export interface PoissaolonSyy {
  id?: number | null
  nimi: string
  vahennystyyppi: PoissaolonSyyTyyppi
  vahennetaanKerran: boolean
  voimassaolonAlkamispaiva: string
  voimassaolonPaattymispaiva?: string | null
}

export interface Koejakso {
  koulutusSopimuksenTila: string | null
  koulutussopimus: KoulutussopimusLomake
  aloituskeskustelunTila: string | null
  aloituskeskustelu: AloituskeskusteluLomake
  valiarvioinninTila: string | null
  valiarviointi: ValiarviointiLomake
  kehittamistoimenpiteidenTila: string | null
  kehittamistoimenpiteet: KehittamistoimenpiteetLomake
  loppukeskustelunTila: string | null
  loppukeskustelu: LoppukeskusteluLomake
  vastuuhenkilonArvionTila: string | null
  vastuuhenkilonArvio: VastuuhenkilonArvioLomake
}

export interface Asiakirja {
  id?: number | null
  nimi: string
  lisattypvm?: string
  contentType?: string
  data?: Promise<ArrayBuffer>
  disablePreview?: boolean
  disableDownload?: boolean
  disableDelete?: boolean
  isDirty: boolean
}

export type AsiakirjatTableField = {
  key: string
  label: string
  class?: string
  sortable?: boolean
  width?: number
  disabled?: boolean
}

export interface KoejaksonTyoskentelyjakso {
  id: number
  formattedNimi: string
  paattymispaiva: string
  disableDelete: boolean
}

export interface KoejaksonVaiheHyvaksynta {
  nimiAndNimike: string
  pvm: string
}

export type KoejaksonVaiheButtonStates = {
  primaryButtonLoading: boolean
  secondaryButtonLoading: boolean
}

export interface KayttajaAuthority {
  name: string
}

export interface Kayttaja {
  id?: number
  nimi: string
  etunimi: string
  sukunimi: string
  sahkoposti: string
  eppn: string
  puhelin: string
  avatar: string
  userId: string
  authorities: KayttajaAuthority[]
  activeAuthority: string
  nimike: string
  yliopistotAndErikoisalat: KayttajaYliopistoErikoisala[]
  yliopistot: Yliopisto[]
  tila: KayttajatiliTila
}

export interface KayttajaYliopistoErikoisala {
  id?: number
  kayttajaId?: number
  yliopisto: Yliopisto
  erikoisala: Erikoisala
  vastuuhenkilonTehtavat: VastuuhenkilonTehtava[]
}

export interface KayttajaYliopistoErikoisalat {
  yliopisto: Yliopisto
  erikoisalat?: Erikoisala[]
}

export interface Yliopisto {
  id?: number
  nimi: string
  erikoisalat: Erikoisala[]
}
export interface Asetus {
  id?: number
  nimi: string
}

export interface KouluttajaValtuutus {
  id?: number
  alkamispaiva: string
  paattymispaiva: string
  valtuutettu: Kayttaja
}

export type OmatTiedotLomake = {
  email: string | null
  phoneNumber: string | null
  avatar: any
  avatarUpdated: boolean
  nimike: string | null
  kayttajanYliopistotJaErikoisalat: KayttajaYliopistoErikoisalat[]
}

export type OmatTiedotLomakeErikoistuja = {
  email: string | null
  phoneNumber: string | null
  avatar: any
  avatarUpdated: boolean
  laillistamispaiva?: string | null
  laillistamispaivanLiite?: File | null
  laakarikoulutusSuoritettuSuomiTaiBelgia: boolean
  laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: boolean
}

export type LaillistamistiedotLomakeKoulutettava = {
  laillistamistiedotAdded: boolean
  ensimmainenTyoskentelyjakso: boolean
  laillistamispaiva?: string | null
  laillistamispaivanLiite?: File | null
  laakarikoulutusSuoritettuSuomiTaiBelgia: boolean
  laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: boolean
}

export type Laillistamistiedot = {
  laillistamispaiva?: string | null
  laillistamistodistus?: string | null
  laillistamistodistusNimi?: string | null
  laillistamistodistusTyyppi?: string | null
  laakarikoulutusSuoritettuSuomiTaiBelgia?: boolean | null
  laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia?: boolean | null
}

export type Kayttajatiedot = {
  nimike: string | null
  kayttajanYliopistot?: Yliopisto[] | null
  kayttajanYliopistotJaErikoisalat?: KayttajaYliopistoErikoisalat[] | null
  yliopistot?: Yliopisto[] | null
  erikoisalat?: Erikoisala[] | null
}

export type Arviointipyynto = {
  id: number
  erikoistujanNimi: string
  pyynnonAika: string
}

export type AvoinAsia = {
  id: number
  tyyppi: AvoinAsiaTyyppi
  asia: string
  pvm: string
}

export type Suoritusarviointi = {
  id?: number
  tapahtumanAjankohta: string
  arvioitavaTapahtuma: string | null
  pyynnonAika: string
  lisatiedot: string | null
  itsearviointiVaativuustaso: number
  sanallinenItsearviointi: string
  itsearviointiAika: string
  vaativuustaso: number
  sanallinenArviointi: string
  arviointiAika: string
  lukittu: boolean
  kommentit: SuoritusarvioinninKommentti[]
  arvioinninAntajaId: number
  arviointiasteikko: Arviointiasteikko
  tyoskentelyjaksoId: number
  arvioinninSaaja: Kayttaja
  arvioinninAntaja: Kayttaja
  arvioitavatKokonaisuudet: SuoritusarvioinninArvioitavaKokonaisuus[]
  tyoskentelyjakso: Tyoskentelyjakso
  arviointityokalut: Arviointityokalu[]
  arviointiPerustuu: ArvioinninPerustuminen
  muuPeruste: string
  arviointiAsiakirjat: Asiakirja[]
  itsearviointiAsiakirjat: Asiakirja[]
  arviointityokaluVastaukset: SuoritusarviointiArviointityokaluVastaus[]
  keskenerainen: boolean
}

export type SuoritusarviointiArviointityokaluVastaus = {
  id: number
  suoritusarviointiId: number
  arviointityokaluId: number
  arviointityokaluKysymysId: number
  tekstiVastaus: string | null
  valittuVaihtoehtoId: number | null
}

export type SuoritusarviointiByKokonaisuus = {
  id?: number
  tapahtumanAjankohta: string
  arvioitavaTapahtuma: string | null
  pyynnonAika: string
  lisatiedot: string | null
  itsearviointiVaativuustaso: number
  itsearviointiArviointiasteikonTaso: number
  sanallinenItsearviointi: string
  itsearviointiAika: string
  vaativuustaso: number
  arviointiasteikonTaso: number
  sanallinenArviointi: string
  arviointiAika: string
  lukittu: boolean
  kommentit: SuoritusarvioinninKommentti[]
  arvioinninAntajaId: number
  arvioitavaKokonaisuusId: number
  arviointiasteikko: Arviointiasteikko
  tyoskentelyjaksoId: number
  arvioinninSaaja: Kayttaja
  arvioinninAntaja: Kayttaja
  tyoskentelyjakso: Tyoskentelyjakso
  arviointityokalut: Arviointityokalu[]
  arviointiPerustuu: ArvioinninPerustuminen
  muuPeruste: string
  arviointiAsiakirjat: Asiakirja[]
  itsearviointiAsiakirjat: Asiakirja[]
}

export type SuoritusarvioinninArvioitavaKokonaisuus = {
  id?: number
  itsearviointiArviointiasteikonTaso: number | ArviointiasteikonTaso
  arviointiasteikonTaso: number | ArviointiasteikonTaso | undefined
  arvioitavaKokonaisuusId: number
  arvioitavaKokonaisuus: ArvioitavaKokonaisuus
  suoritusarviointiId: number
}

export type SuoritusarvioinninKommentti = {
  id?: number
  teksti: string
  luontiaika: string
  muokkausaika: string
  kommentoija: Kayttaja
  suoritusarviointiId: number
}

export type ArvioitavatKokonaisuudetList = {
  erikoisalaId: number
  erikoisalaNimi: string
  id?: number
  nimi?: string
  vanhentuneet: ArvioitavaKokonaisuus[]
  voimassaolevat: ArvioitavaKokonaisuus[]
}

export type ArvioitavaKokonaisuus = {
  id?: number
  nimi: string
  nimiSv: string
  kuvaus: string
  kuvausSv: string
  voimassaoloAlkaa: string
  voimassaoloLoppuu: string
  erikoisalaId: number
  kategoria: ArvioitavanKokonaisuudenKategoria
  arvioinnit: SuoritusarviointiByKokonaisuus[]
  visible: boolean
}

export type UusiArvioitavaKokonaisuus = {
  nimi: string | null
  nimiSv: string | null
  kuvaus: string | null
  kuvausSv: string | null
  voimassaoloAlkaa: string | null
  voimassaoloLoppuu: string | null
  kategoria: ArvioitavanKokonaisuudenKategoriaWithErikoisala | null
}

export type ArvioitavaKokonaisuusWithErikoisala = {
  id?: number
  nimi: string
  nimiSv: string
  kuvaus: string
  kuvausSv: string
  voimassaoloAlkaa: string
  voimassaoloLoppuu: string
  kategoria: ArvioitavanKokonaisuudenKategoriaWithErikoisala
  voiPoistaa: boolean
}

export type ArvioitavanKokonaisuudenKategoria = {
  id?: number
  nimi: string
  nimiSv: string
  jarjestysnumero: number
  arvioitavatKokonaisuudet: ArvioitavaKokonaisuus[]
  visible: boolean
}

export type UusiArvioitavanKokonaisuudenKategoria = {
  nimi: string | null
  nimiSv: string | null
  jarjestysnumero: number | null
  erikoisala: Erikoisala | null
}

export type ArvioitavanKokonaisuudenKategoriaWithErikoisala = {
  id?: number
  nimi: string
  nimiSv: string
  jarjestysnumero: number
  erikoisala: Erikoisala
}

export type Arviointiasteikko = {
  id?: number
  nimi: ArviointiasteikkoTyyppi
  tasot: ArviointiasteikonTaso[]
}

export type ArviointiasteikonTaso = {
  nimi: ArviointiasteikonTasoTyyppi
  taso: number
}

export type Arviointityokalu = {
  id?: number
  nimi: string | null
  kategoria: ArviointityokaluKategoria | null
  ohjeteksti: string | null
  liite: File | null | any
  kysymykset: ArviointityokaluKysymys[]
  liitetiedostonNimi?: string
  liitetiedostonTyyppi?: string
  tila: ArviointityokaluTila
  kaytossa: boolean
}

export type ArviointityokaluKysymys = {
  id?: number
  otsikko: string | null
  tyyppi: ArviointityokaluKysymysTyyppi
  vaihtoehdot: ArviointityokaluKysymysVaihtoehto[]
  pakollinen: boolean
  tekstikenttavastaus?: string
  jarjestysnumero: number
}

export type ArviointityokaluKysymysVaihtoehto = {
  id?: number
  teksti: string
  valittu: boolean
  tyyppi: ArviointityokaluKysymysVaihtoehtoTyyppi
}

export type SuoritusarviointiForm = {
  vaativuustaso: Vaativuustaso | null | undefined
  arvioitavatKokonaisuudet: SuoritusarvioinninArvioitavaKokonaisuus[] | null | undefined
  sanallinenArviointi: string | null
  arviointityokalut?: Arviointityokalu[]
  arviointityokaluVastaukset?: SuoritusarviointiArviointityokaluVastaus[]
  arviointiPerustuu?: ArvioinninPerustuminen | null
  muuPeruste?: string | null
  perustuuMuuhun?: boolean
  keskenerainen?: boolean
}

export type SuoritusarviointiFilter = {
  tyoskentelyjakso?: Tyoskentelyjakso | null
  arvioitavaKokonaisuus?: ArvioitavaKokonaisuus | null
  kouluttajaOrVastuuhenkilo?: Kayttaja | null
}

export type SuoritusarvioinnitOptions = {
  tyoskentelyjaksot: Tyoskentelyjakso[]
  arvioitavatKokonaisuudet: ArvioitavaKokonaisuus[]
  kouluttajatAndVastuuhenkilot: Kayttaja[]
}

export type Vaativuustaso = {
  arvo: number
  nimi: string
  kuvaus: string
}

export type SuoritteenKategoria = {
  id?: number
  nimi: string
  nimiSv: string
  erikoisalaId: number
  suoritteet: Suorite[]
  jarjestysnumero: number
}

export type UusiSuoritteenKategoria = {
  nimi: string | null
  nimiSv: string | null
  jarjestysnumero: number | null
  erikoisala: Erikoisala | null
}

export type SuoritteenKategoriaWithErikoisala = {
  id?: number
  nimi: string
  nimiSv: string
  jarjestysnumero: number
  erikoisala: Erikoisala
}

export type Suorite = {
  id: number
  nimi: string
  nimiSv: string
  voimassaolonAlkamispaiva: string
  voimassaolonPaattymispaiva: string
  kategoriaId: number
  vaadittulkm?: number
  suoritettulkm?: number
}

export type UusiSuorite = {
  nimi: string | null
  nimiSv: string | null
  voimassaolonAlkamispaiva: string | null
  voimassaolonPaattymispaiva: string | null
  vaadittulkm: number | null
  kategoria: SuoritteenKategoriaWithErikoisala | null
}

export type SuoriteWithErikoisala = {
  id?: number
  nimi: string
  nimiSv: string
  voimassaolonAlkamispaiva: string
  voimassaolonPaattymispaiva: string
  vaadittulkm?: number
  kategoria: SuoritteenKategoriaWithErikoisala
  voiPoistaa: boolean
}

export type Suoritemerkinta = {
  id?: number
  suorituspaiva: string
  arviointiasteikonTaso?: number | ArviointiasteikonTaso
  vaativuustaso?: number | Vaativuustaso
  lisatiedot: string
  lukittu?: boolean
  suoriteId?: number
  tyoskentelyjaksoId?: number
  suorite: Suorite
  tyoskentelyjakso: Tyoskentelyjakso
  arviointiasteikko?: Arviointiasteikko
}

export type SuoritemerkinnanSuorite = {
  arviointiasteikonTaso?: number | ArviointiasteikonTaso | null
  vaativuustaso?: number | Vaativuustaso | null
  suorite: Suorite | null
}

export interface ToggleableSuoritemerkinta extends Suoritemerkinta {
  showDetails: boolean
}

export type SuoritteetTable = {
  suoritteenKategoriat: SuoritteenKategoria[]
  aiemmatKategoriat: SuoritteenKategoria[]
  suoritemerkinnat: Suoritemerkinta[]
  arviointiasteikko: Arviointiasteikko
}

export interface SuoritteenKategoriaRow {
  nimi: string
  visible: boolean
  suoritteet: SuoriteRow[]
}

export interface SuoriteRow extends Suorite {
  visible: boolean
  merkinnat: Suoritemerkinta[]
}

export type SuoritemerkintaRow = {
  suoritemerkinta?: ToggleableSuoritemerkinta
  details: boolean
  suorite?: Suorite
  hasDetails?: boolean
  lastDetails?: boolean
  nimi?: string
  vaadittulkm?: number
  suoritettulkm?: number
}

export type SuoritemerkintaWithDetails = {
  suoritemerkinta: ToggleableSuoritemerkinta
  details: boolean
  lastDetails?: boolean
}

export interface BarChartRow {
  text: string | LocaleMessages
  color: string
  backgroundColor: string
  value: number
  minRequired: number
  maxLength?: number
  highlight: boolean
  showMax: boolean
}

export interface Palaute {
  palautteenAihe: string | null
  palauteYliopisto: string | null
  palaute: string | null
  anonyymiPalaute: boolean
}

export interface Teoriakoulutukset {
  teoriakoulutukset: Teoriakoulutus[]
  erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: number
}

export interface Teoriakoulutus {
  id?: number
  koulutuksenNimi: string | null
  koulutuksenPaikka: string | null
  alkamispaiva: string
  paattymispaiva: string | null
  erikoistumiseenHyvaksyttavaTuntimaara: number | null
  todistukset: Asiakirja[]
}

export interface PaivakirjaAihekategoria {
  id?: number
  nimi: string
  jarjestysnumero: number | null
  kuvaus: string | null
  teoriakoulutus: boolean
  muunAiheenNimi: boolean
}

export interface Paivakirjamerkinta {
  id?: number
  paivamaara: string
  oppimistapahtumanNimi: string | null
  muunAiheenNimi: string | null
  reflektio: string | null
  yksityinen: boolean
  aihekategoriat: PaivakirjaAihekategoria[]
  teoriakoulutus: Teoriakoulutus | null
}

export interface PaivakirjamerkintaForm {
  id?: number
  paivamaara: string | null
  oppimistapahtumanNimi: string | null
  muunAiheenNimi: string | null
  reflektio: string | null
  yksityinen: boolean
  aihekategoriat: PaivakirjaAihekategoria[]
  teoriakoulutus: Teoriakoulutus | null
}

export interface PaivakirjamerkintaRajaimet {
  aihekategoriat: PaivakirjaAihekategoria[]
}

export interface PaivakirjamerkintaLomake {
  aihekategoriat: PaivakirjaAihekategoria[]
  teoriakoulutukset: Teoriakoulutus[]
}

export interface ElsaError {
  errorKey: string
  message: string
}

export type HakaYliopisto = {
  nimi: string
  hakaId: string
}

export interface KayttajahallintaKayttajaWrapper {
  kayttaja?: Kayttaja
  erikoistuvaLaakari?: ErikoistuvaLaakari
  avoimiaTehtavia?: boolean
}

export interface KayttajahallintaUpdateKayttaja {
  etunimi?: string | null
  sukunimi?: string | null
  sahkoposti?: string | null
  sahkopostiUudelleen?: string | null
  opintooikeudet?: KayttajahallintaUpdateOpintooikeus[]
  eppn?: string | null
  yliopistotAndErikoisalat?: KayttajaYliopistoErikoisala[]
  reassignedTehtavat?: ReassignedVastuuhenkilonTehtava[]
}

export interface KayttajahallintaUpdateOpintooikeus {
  id?: number
  opintoopas?: OpintoopasSimple | number | null
  osaamisenArvioinninOppaanPvm?: string
}

export interface KayttajahallintaNewKayttaja {
  etunimi: string | null
  sukunimi: string | null
  sahkoposti: string | null
  sahkopostiUudelleen: string | null
  eppn: string | null
  yliopisto?: Yliopisto | null
  yliopistotAndErikoisalat?: KayttajaYliopistoErikoisala[]
  reassignedTehtavat?: ReassignedVastuuhenkilonTehtava[]
}

export interface KayttajahallintaKayttajaListItem {
  kayttajaId: number
  etunimi: string
  sukunimi: string
  syntymaaika: string
  yliopistotAndErikoisalat: YliopistoErikoisalaPair[]
  kayttajatilinTila: string
}

export interface KayttajahallintaYhdistaKayttajatilejaListItem {
  kayttajaId: number
  etunimi: string
  sukunimi: string
  syntymaaika: string
  yliopistotAndErikoisalat: YliopistoErikoisalaPair[]
  kayttajatilinTila: string
  sahkoposti: string
  authorities: string[]
}

export interface YliopistoErikoisalaPair {
  yliopisto: string
  erikoisala: string
}

export interface UusiErikoistuvaLaakari {
  etunimi: string | null
  sukunimi: string | null
  sahkopostiosoite: string | null
  sahkopostiosoiteUudelleen: string | null
  yliopisto: Yliopisto | null
  yliopistoId?: number
  erikoisala: Erikoisala | null
  erikoisalaId?: number | null
  opiskelijatunnus: string | null
  opintooikeusAlkaa: string | null
  opintooikeusPaattyy: string | null
  asetusId: number | null
  opintoopas: OpintoopasSimple | null
  opintoopasId?: number | null
  osaamisenArvioinninOppaanPvm: string | null
}

export interface ErikoistuvaLaakariLomake {
  yliopistot: Yliopisto[]
  erikoisalat: Erikoisala[]
  asetukset: Asetus[]
  opintooppaat: OpintoopasSimple[]
}

export type Seurantajakso = {
  id?: number
  alkamispaiva: string
  paattymispaiva: string
  koulutusjaksot: Koulutusjakso[]
  omaArviointi?: string | null
  lisahuomioita?: string | null
  seuraavanJaksonTavoitteet?: string | null
  kouluttaja?: Kayttaja | null
  seurantakeskustelunYhteisetMerkinnat?: string | null
  seuraavanKeskustelunAjankohta?: string | null
  edistyminenTavoitteidenMukaista?: boolean | null
  huolenaiheet?: string | null
  kouluttajanArvio?: string | null
  erikoisalanTyoskentelyvalmiudet?: string | null
  jatkotoimetJaRaportointi?: string | null
  hyvaksytty?: boolean | null
  korjausehdotus?: string | null
  erikoistuvanNimi: string | null
  erikoistuvanErikoisalaNimi: string | null
  erikoistuvanOpiskelijatunnus: string | null
  erikoistuvanYliopistoNimi: string | null
  erikoistuvanAvatar?: string
  tallennettu?: string | null
  tila?: SeurantajaksoTila | null
  aiemmatJaksot?: Seurantajakso[]
}

export type SeurantajaksoHakuForm = {
  alkamispaiva: string | null
  paattymispaiva: string | null
  koulutusjaksot: Partial<Koulutusjakso>[]
}

export type SeurantajaksonArviointiKategoria = {
  nimi: string
  jarjestysnumero: number
  arvioitavatKokonaisuudet: SeurantajaksonArviointiKokonaisuus[]
}

export type SeurantajaksonArviointiKokonaisuus = {
  nimi: string
  arvioinnit: SeurantajaksonArviointi[]
}

export type SeurantajaksonArviointi = {
  arvioitavaTapahtuma: string
  arviointiasteikonTaso: number
  tapahtumanAjankohta: string
  arviointiasteikko: Arviointiasteikko
  suoritusarviointiId: number
}

export type SeurantajaksonTiedot = {
  koulutusjaksot: Koulutusjakso[]
  arvioinnit: SeurantajaksonArviointiKategoria[]
  arviointienMaara: number
  suoritemerkinnat: SeurantajaksonSuoritemerkinta[]
  suoritemerkinnatMaara: number
  teoriakoulutukset: Teoriakoulutus[]
}

export type SeurantajaksonSuoritemerkinta = {
  suorite: string
  suoritemerkinnat: Suoritemerkinta[]
}

export interface PageSort {
  empty: boolean
  unsorted: boolean
  sorted: boolean
}
export interface Page<T> {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

export type Keskeytysaika = {
  id?: number
  alkamispaiva: string
  paattymispaiva: string
  poissaoloprosentti: number
  poissaolonSyyId?: number
  tyoskentelyjaksoId?: number
  poissaolonSyy: PoissaolonSyy
  tyoskentelyjakso: Tyoskentelyjakso
}

export type TyoskentelyjaksotTilastotKoulutustyypit = {
  terveyskeskusVaadittuVahintaan: number
  terveyskeskusMaksimipituus?: number
  terveyskeskusSuoritettu: number
  yliopistosairaalaVaadittuVahintaan: number
  yliopistosairaalaSuoritettu: number
  yliopistosairaaloidenUlkopuolinenVaadittuVahintaan: number
  yliopistosairaaloidenUlkopuolinenMaksimipituus?: number
  yliopistosairaaloidenUlkopuolinenSuoritettu: number
  yhteensaVaadittuVahintaan: number
  yhteensaSuoritettu: number
}

export type TyoskentelyjaksotKoulutustyypit = {
  terveyskeskus: Tyoskentelyjakso[]
  yliopistosairaala: Tyoskentelyjakso[]
  yliopistosairaaloidenUlkopuolinen: Tyoskentelyjakso[]
}

export interface TyoskentelyjaksotTilastotKaytannonKoulutus {
  kaytannonKoulutus: KaytannonKoulutusTyyppi
  suoritettu: number
}

export interface TyoskentelyjaksotTilastotTyoskentelyjaksot {
  id: number
  suoritettu: number
}

export interface TyoskentelyjaksotTilastot {
  tyoskentelyaikaYhteensa: number
  arvioErikoistumiseenHyvaksyttavista: number
  arvioPuuttuvastaKoulutuksesta: number
  koulutustyypit: TyoskentelyjaksotTilastotKoulutustyypit
  kaytannonKoulutus: TyoskentelyjaksotTilastotKaytannonKoulutus[]
  tyoskentelyjaksot: TyoskentelyjaksotTilastotTyoskentelyjaksot[]
  poissaoloaikaYhteensa: number
  tyokertymaYhteensa: number
}

export interface TyoskentelyjaksotTable {
  poissaolonSyyt: PoissaolonSyy[]
  tyoskentelyjaksot: Tyoskentelyjakso[]
  keskeytykset: Keskeytysaika[]
  tilastot: TyoskentelyjaksotTilastot
  terveyskeskuskoulutusjaksonTila: TerveyskeskuskoulutusjaksonTila
  terveyskeskuskoulutusjaksonKorjausehdotus: string | null
  terveyskeskuskoulutusjaksonHyvaksymispvm: string | null
}

export interface KayttajaErikoisalaPerYliopisto {
  yliopistoNimi: string
  erikoisalat: string[]
}

export interface ErikoistujanEteneminen {
  erikoistuvaLaakariId: number
  erikoistuvaLaakariEtuNimi: string
  erikoistuvaLaakariSukuNimi: string
  erikoistuvaLaakariSyntymaaika: string
  tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastot
  arviointienKeskiarvo: number
  arviointienLkm: number
  arvioitavienKokonaisuuksienLkm: number
  seurantajaksotLkm: number
  seurantajaksonHuoletLkm: number
  suoritemerkinnatLkm: number
  vaaditutSuoritemerkinnatLkm: number
  koejaksoTila: LomakeTilat
  opintooikeudenMyontamispaiva: string
  opintooikeudenPaattymispaiva: string
  asetus: string
  erikoisala: string
  terveyskeskuskoulutusjaksoSuoritettu: boolean
  opintooikeusId: number
}

export interface ErikoistujienSeurantaVastuuhenkiloRajaimet {
  kayttajaYliopistoErikoisalat: KayttajaErikoisalaPerYliopisto[]
  erikoisalat: string[]
}

export interface ErikoistujienSeurantaKouluttajaRajaimet {
  kayttajaYliopistoErikoisalat: KayttajaErikoisalaPerYliopisto[]
  erikoisalat: string[]
}

export interface ErikoistujienSeuranta {
  kayttajaYliopistoErikoisalat: KayttajaErikoisalaPerYliopisto[]
  erikoisalat: string[]
  erikoistujienEteneminen: ErikoistujanEteneminen[]
}

export interface KaytonAloitusModel {
  sahkoposti: string | null
  sahkopostiUudelleen: string | null
  opintooikeusId?: number | null
}

export interface ErikoistujienSeurantaVirkailijaRajaimet {
  erikoisalat: Erikoisala[]
  asetukset: Asetus[]
}

export interface ErikoistujanEteneminenVirkailija {
  erikoistuvaLaakariId: number
  etunimi: string
  sukunimi: string
  syntymaaika: string
  erikoisala: string
  asetus: string
  koejaksoTila: LomakeTilat
  opintooikeudenMyontamispaiva: string
  opintooikeudenPaattymispaiva: string
  tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastot
  teoriakoulutuksetSuoritettu: number
  teoriakoulutuksetVaadittu: number
  johtamisopinnotSuoritettu: number
  johtamisopinnotVaadittu: number
  sateilysuojakoulutuksetSuoritettu: number
  sateilysuojakoulutuksetVaadittu: number
  valtakunnallisetKuulustelutSuoritettuLkm: number
  terveyskeskuskoulutusjaksoSuoritettu: boolean
  opintooikeusId: number
}

export interface KoulutettavanEteneminen {
  erikoistuvaLaakariId: number
  etunimi: string
  sukunimi: string
  syntymaaika: string
  erikoisala: string
  asetus: string
  opintooikeudenMyontamispaiva: string
  opintooikeudenPaattymispaiva: string
  tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastot
  teoriakoulutuksetSuoritettu: boolean
  yekSuoritettu: boolean
  opintooikeusId: number
}

export type SortByEnum = {
  name: string
  value: number
}

export interface ErikoistumisenEdistyminen {
  arviointienKeskiarvo: number
  arvioitavatKokonaisuudetVahintaanYksiArvioLkm: number
  arvioitavienKokonaisuuksienLkm: number
  arviointiasteikko: Arviointiasteikko
  suoritemerkinnatLkm: number
  vaaditutSuoritemerkinnatLkm: number
  osaalueetSuoritettuLkm: number
  osaalueetVaadittuLkm: number
  tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastot
  teoriakoulutuksetSuoritettu: number
  teoriakoulutuksetVaadittu: number
  johtamisopinnotSuoritettu: number
  johtamisopinnotVaadittu: number
  sateilysuojakoulutuksetSuoritettu: number
  sateilysuojakoulutuksetVaadittu: number
  koejaksoTila: string
  koejaksonSuoritusmerkintaExists: boolean
  valtakunnallisetKuulustelutSuoritettuLkm: number
  opintooikeudenMyontamispaiva: string
  opintooikeudenPaattymispaiva: string
  terveyskeskuskoulutusjaksoSuoritettu: boolean
  laakarikoulutusSuoritettuSuomiTaiBelgia: boolean
  yekSuoritusPvm: string
}

export interface HyvaksyttyKoejaksonVaihe {
  id: number
  tyyppi: LomakeTyypit
  pvm: string
}

export interface KoejaksonVaihe {
  id: number
  tyyppi: LomakeTyypit
  tila: LomakeTilat
  erikoistuvanNimi: string
  erikoistuvanAvatar?: string
  pvm: string
  hyvaksytytVaiheet: HyvaksyttyKoejaksonVaihe[]
}

export interface VastuuhenkilonTehtava {
  id: number
  nimi: string
}

export interface KayttajahallintaRajaimet {
  erikoisalat: Erikoisala[]
}

export interface VastuuhenkilonErikoisalaAndTehtavat {
  erikoisala: Erikoisala
  tehtavat: VastuuhenkilonTehtava[]
}

export interface ReassignedVastuuhenkilonTehtava {
  label?: string
  kayttajaYliopistoErikoisala?: KayttajaYliopistoErikoisala | null
  tehtavaId: number | null
  tyyppi: ReassignedVastuuhenkilonTehtavaTyyppi
}

export interface ErikoisalaForVastuuhenkilonTehtavat {
  erikoisalaId: number
  reassignedTehtavat: ReassignedVastuuhenkilonTehtava[]
}

export interface VastuuhenkilonTehtavatLomake {
  yliopistotAndErikoisalat: Partial<KayttajaYliopistoErikoisala>[]
  erikoisalatForTehtavat: ErikoisalaForVastuuhenkilonTehtavat[]
}

export interface KayttajahallintaVastuuhenkilonTehtavatLomake {
  erikoisalat: Erikoisala[]
  vastuuhenkilot: Kayttaja[]
}

export interface Katseluoikeus {
  erikoistujanNimi: string
  vanhenemispaiva: string
}

export type TerveyskeskuskoulutusjaksonHyvaksyminen = {
  id?: number
  erikoistuvanErikoisala: string
  erikoistuvanNimi: string
  erikoistuvanAvatar?: string
  erikoistuvanOpiskelijatunnus: string
  erikoistuvanSyntymaaika: string
  erikoistuvanYliopisto: string
  terveyskeskuskoulutusjaksonKesto: number
  laillistamispaiva: string
  laillistamispaivanLiite?: string
  laillistamispaivanLiitteenNimi?: string
  laillistamispaivanLiitteenTyyppi?: string
  asetus: string
  tyoskentelyjaksot: Tyoskentelyjakso[]
  yleislaaketieteenVastuuhenkilonNimi: string
  yleislaaketieteenVastuuhenkilonNimike: string
  tila: TerveyskeskuskoulutusjaksonTila
  virkailijanKorjausehdotus: string
  vastuuhenkilonKorjausehdotus: string
  lisatiedotVirkailijalta: string
  virkailijanNimi: string
  virkailijanNimike: string
  virkailijanKuittausaika: string
  vastuuhenkilonNimi: string
  vastuuhenkilonNimike: string
  vastuuhenkilonKuittausaika: string
  opintooikeusId?: number
  erikoisalaId?: number
}

export type TerveyskeskuskoulutusjaksonHyvaksyntaForm = {
  laillistamispaiva: string | null
  laillistamispaivanLiite: File | null
  tyoskentelyjaksoAsiakirjat: TyoskentelyjaksonAsiakirjat[]
}

export type TyoskentelyjaksonAsiakirjat = {
  id?: number | null
  addedFiles: File[]
  deletedFiles: number[]
}

export type TerveyskeskuskoulutusjaksonVaihe = {
  id: number
  tila: TerveyskeskuskoulutusjaksonTila
  erikoistuvanNimi: string
  pvm: string
}

export interface ValmistumispyyntoVaatimuksetLomake {
  tyoskentelyjaksot?: boolean
  tyotodistukset?: boolean
  kuulusteluJaJohtamisopinnot?: boolean
  teoriakoulutus?: boolean
  osaamisenArvioinnit?: boolean
  palautekysely?: boolean
  tkjakso?: boolean
  tyoskentelyjaksotpaatetty?: boolean
}

export interface ValmistumispyyntoVaatimuksetLomakeYek {
  tyoskentelyjaksot?: boolean
  tyotodistukset?: boolean
  teoriakoulutus?: boolean
}

export interface Valmistumispyynto {
  id: number | null
  tila?: ValmistumispyynnonTila
  muokkauspaiva?: string
  erikoistujanAvatar?: string
  erikoistujanNimi?: string
  erikoistujanOpiskelijatunnus?: string
  erikoistujanSyntymaaika?: string
  erikoistujanErikoisala?: string
  erikoistujanYliopisto?: string
  erikoistujanLaillistamispaiva?: string
  erikoistujanLaillistamistodistus?: string
  erikoistujanLaillistamistodistusNimi?: string
  erikoistujanLaillistamistodistusTyyppi?: string
  selvitysVanhoistaSuorituksista?: string
  erikoistujanAsetus?: string
  opintooikeusId?: number
  opintooikeudenMyontamispaiva?: string
  erikoistujanKuittausaika?: string
  vastuuhenkiloOsaamisenArvioijaNimi?: string
  vastuuhenkiloOsaamisenArvioijaNimike?: string
  vastuuhenkiloOsaamisenArvioijaKuittausaika?: string
  vastuuhenkiloOsaamisenArvioijaPalautusaika?: string
  vastuuhenkiloOsaamisenArvioijaKorjausehdotus?: string
  virkailijaNimi?: string
  virkailijanKuittausaika?: string
  virkailijanPalautusaika?: string
  virkailijanKorjausehdotus?: string
  virkailijanSaate: string
  virkailijanYhteenveto?: string | null
  vastuuhenkiloHyvaksyjaNimi?: string
  vastuuhenkiloHyvaksyjaNimike?: string
  vastuuhenkiloHyvaksyjaKuittausaika?: string
  vastuuhenkiloHyvaksyjaPalautusaika?: string
  vastuuhenkiloHyvaksyjaKorjausehdotus?: string
  yhteenvetoAsiakirjaId?: number | null
  liitteetAsiakirjaId?: number | null
  erikoistujanTiedotAsiakirjaId?: number | null
  selvitysVanhentuneistaSuorituksista: string | null
  arkistoitava: boolean
}

export interface ValmistumispyyntoArviointienTila {
  hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour: boolean
  hasArvioitaviaKokonaisuuksiaWithoutArviointi: boolean
}

export interface ValmistumispyyntoSuoritustenTila {
  erikoisalaTyyppi?: ErikoisalaTyyppi
  vanhojaTyoskentelyjaksojaOrSuorituksiaExists?: boolean
  kuulusteluVanhentunut?: boolean
}

export interface ValmistumispyyntoLomakeErikoistuja {
  selvitysVanhentuneistaSuorituksista: string | null
  laillistamispaiva: string | null
  laillistamistodistus: File | null
  erikoistujanPuhelinnumero: string | null
  erikoistujanSahkoposti: string | null
}

export interface ValmistumispyyntoListItem {
  id: number
  erikoistujanNimi: string
  tila: ValmistumispyynnonTila
  tapahtumanAjankohta: string
  isAvoinForCurrentKayttaja: boolean
  rooli: ValmistumispyynnonHyvaksyjaRole
}

export interface ValmistumispyyntoLomakeOsaamisenArviointi {
  id: number | null
  osaaminenRiittavaValmistumiseen: boolean | null
  korjausehdotus: string | null
}

export interface ValmistumispyyntoVirkailijanTarkistus {
  id: number | null
  yekSuoritettu: boolean
  yekSuorituspaiva: string | null
  ptlSuoritettu: boolean
  ptlSuorituspaiva: string | null
  aiempiElKoulutusSuoritettu: boolean
  aiempiElKoulutusSuorituspaiva: string | null
  ltTutkintoSuoritettu: boolean
  ltTutkintoSuorituspaiva: string | null
  virkailijanSaate: string | null
  tyoskentelyjaksotTilastot: TyoskentelyjaksotTilastotKoulutustyypit
  tyoskentelyjaksot: TyoskentelyjaksotKoulutustyypit | null
  terveyskeskustyoHyvaksyttyPvm: string | null
  terveyskeskustyoHyvaksyntaId: number | null
  terveyskeskustyoOpintosuoritusId: number | null
  terveyskeskustyoTarkistettu: boolean
  yliopistosairaalanUlkopuolinenTyoTarkistettu: boolean
  yliopistosairaalatyoTarkistettu: boolean
  kokonaistyoaikaTarkistettu: boolean
  teoriakoulutusSuoritettu: number
  teoriakoulutusVaadittu: number
  teoriakoulutusTarkistettu: boolean
  sateilusuojakoulutusSuoritettu: number
  sateilusuojakoulutusVaadittu: number
  johtamiskoulutusSuoritettu: number
  johtamiskoulutusVaadittu: number
  kuulustelut: Opintosuoritus[] | null
  koejaksoHyvaksyttyPvm: string | null
  koejaksoEiVaadittu?: boolean
  suoritustenTila: ValmistumispyyntoSuoritustenTila | null
  lisatiedotVastuuhenkilolle: string | null
  kommentitVirkailijoille: string | null
  keskenerainen: boolean
  korjausehdotus?: string | null
  tutkimustyotaTehty: boolean
  valmistumispyynto?: Valmistumispyynto
  virkailijanYhteenveto?: string | null
}

export interface ValmistumispyynnonVirkailijanTarkistusLomake {
  id: number | null
  yekSuoritettu: boolean
  yekSuorituspaiva: string | null
  ptlSuoritettu: boolean
  ptlSuorituspaiva: string | null
  aiempiElKoulutusSuoritettu: boolean
  aiempiElKoulutusSuorituspaiva: string | null
  ltTutkintoSuoritettu: boolean
  ltTutkintoSuorituspaiva: string | null
  terveyskeskustyoTarkistettu: boolean
  yliopistosairaalanUlkopuolinenTyoTarkistettu: boolean
  yliopistosairaalatyoTarkistettu: boolean
  kokonaistyoaikaTarkistettu: boolean
  teoriakoulutusTarkistettu: boolean
  koejaksoEiVaadittu?: boolean
  kommentitVirkailijoille: string | null
  lisatiedotVastuuhenkilolle?: string | null
  keskenerainen?: boolean
  korjausehdotus?: string | null
  laillistamispaiva: string | null
  laillistamistodistus: File | null
  virkailijanYhteenveto?: string | null
}

export interface ValmistumispyyntoHyvaksynta {
  id: number | null
  korjausehdotus?: string | null
  sahkoposti?: string | null
  puhelinnumero?: string | null
}

export interface UusiKouluttaja {
  etunimi: string
  sukunimi: string
  sahkoposti: string
}

export type Ilmoitus = {
  id?: number
  teksti: string | null
}

export type SuoritemerkintaFilter = {
  tyoskentelyjakso?: Tyoskentelyjakso | null
  suorite?: Suorite | null
  suorituspaivaAlkaa?: string | null
  suorituspaivaPaattyy?: string | null
}

export type SuoritemerkinnatOptions = {
  tyoskentelyjaksot: Tyoskentelyjakso[]
  suoritteet: Suorite[]
}

export interface Kurssikoodi {
  id: number | null
  tunniste: string | null
  tyyppi: OpintosuoritusTyyppi | null
}

export interface VastuuhenkilonVastuualueet {
  terveyskeskuskoulutusjakso: boolean
  yekTerveyskeskuskoulutusjakso: boolean
  valmistuminen: boolean
  yekValmistuminen: boolean
}

export interface TyokertymaLaskuriTyoskentelyjaksoForm {
  id?: number | null
  alkamispaiva: string | null
  paattymispaiva: string | null
  minPaattymispaiva: string | null
  maxAlkamispaiva: string | null
  osaaikaprosentti: number | null
  kahdenvuodenosaaikaprosentti: number | null
  kaytannonKoulutus: KaytannonKoulutusTyyppi | null
  tyoskentelypaikka: TyokertymaLaskuriTyoskentelypaikkaForm
  label?: string
  poissaolot: TyokertymaLaskuriPoissaolo[]
}

export interface TyokertymaLaskuriPoissaolo {
  id?: number
  alkamispaiva: string
  paattymispaiva?: string
  poissaoloprosentti?: number
  poissaolonSyyId: number
  tyoskentelyjaksoId: number
  poissaolonSyy: PoissaolonSyy
  tyoskentelyjakso: {
    id: number
    paattymispaiva: string
    osaaikaprosentti: number
    kahdenvuodenosaaikaprosentti: number
    kaytannonKoulutus?: KaytannonKoulutusTyyppi
  }
  kokoTyoajanPoissaolo?: boolean
}

export interface TyokertymaLaskuriTyoskentelypaikkaForm {
  nimi: string | null
  tyyppi: TyoskentelyjaksoTyyppi | null
  muuTyyppi: string | null
}

export interface TyokertymaLaskuriTyoskentelyjakso {
  id: number
  tyoskentelypaikka: TyokertymaLaskuriTyoskentelypaikkaForm
  alkamispaiva: string
  paattymispaiva: string
  osaaikaprosentti: number
  kahdenvuodenosaaikaprosentti: number
  kaytannonKoulutus: KaytannonKoulutusTyyppi
  poissaolot: TyokertymaLaskuriPoissaolo[]
}

export interface TyokertymaLaskuriTyoskentelyjaksotTable {
  tyoskentelyjaksot: TyokertymaLaskuriTyoskentelyjakso[]
  tilastot: TyoskentelyjaksotTilastot
  terveyskeskuskoulutusjaksonKorjausehdotus: string | null
  terveyskeskuskoulutusjaksonHyvaksymispvm: string | null
}

export interface HyvaksiluettavatCounterData {
  hyvaksiluettavatDays: Map<string, number>
  hyvaksiluettavatPerYearMap: Map<number, number>
}

export interface YhdistaKayttajatilejaForm {
  erikoistujaKayttajaId: number
  kouluttajaKayttajaId: number
  yhteinenSahkoposti: string
  yhteinenSahkopostiUudelleen: string
  formValid: boolean
}

export interface KayttajienYhdistaminenDTO {
  ensimmainenKayttajaId: number
  toinenKayttajaId: number
  yhteinenSahkoposti: string
}

export type PaaKayttajaArviointityokalu = {
  id?: number
  nimi: string
  kategoriaId: number
  kayttajaId: number
  luontiaika: string
  muokkausaika: string
}

export type ArviointityokaluKategoria = {
  id?: number
  nimi: string | null
}

export type FileUploadText = {
  text: string
  isLink: boolean
  link: string
  linkType: 'upload' | 'url' | 'navigation' | null
}
