import axios from 'axios'
import Vue from 'vue'

import {
  AloituskeskusteluLomake,
  KehittamistoimenpiteetLomake,
  KoulutussopimusLomake,
  LoppukeskusteluLomake,
  Suoritusarviointi,
  Seurantajakso,
  SeurantajaksonTiedot,
  ValiarviointiLomake,
  KoejaksonVaihe,
  Arviointipyynto,
  Katseluoikeus,
  ErikoistujanEteneminen,
  Page,
  ErikoistujienSeurantaKouluttajaRajaimet,
  Arviointityokalu,
  ArviointityokaluKategoria
} from '@/types'

export async function getKoejaksot() {
  const path = `${resolveRolePath()}/koejaksot`
  return await axios.get(path)
}

export async function getKoulutussopimus(id: number) {
  const path = `/kouluttaja/koejakso/koulutussopimus/${id}`
  return await axios.get<KoulutussopimusLomake>(path)
}

export async function putKoulutussopimus(form: KoulutussopimusLomake) {
  const path = 'kouluttaja/koejakso/koulutussopimus'
  return await axios.put<KoulutussopimusLomake>(path, form)
}

export async function getAloituskeskustelu(id: number) {
  const path = `/kouluttaja/koejakso/aloituskeskustelu/${id}`
  return await axios.get<AloituskeskusteluLomake>(path)
}

export async function putAloituskeskustelu(form: AloituskeskusteluLomake) {
  const path = `${resolveRolePath()}/koejakso/aloituskeskustelu`
  return await axios.put<AloituskeskusteluLomake>(path, form)
}

export async function getValiarviointi(id: number) {
  const path = `/kouluttaja/koejakso/valiarviointi/${id}`
  return await axios.get<ValiarviointiLomake>(path)
}

export async function putValiarviointi(form: ValiarviointiLomake) {
  const path = `${resolveRolePath()}/koejakso/valiarviointi`
  return await axios.put<ValiarviointiLomake>(path, form)
}

export async function getKehittamistoimenpiteet(id: number) {
  const path = `/kouluttaja/koejakso/kehittamistoimenpiteet/${id}`
  return await axios.get<KehittamistoimenpiteetLomake>(path)
}

export async function putKehittamistoimenpiteet(form: KehittamistoimenpiteetLomake) {
  const path = `${resolveRolePath()}/koejakso/kehittamistoimenpiteet`
  return await axios.put<KehittamistoimenpiteetLomake>(path, form)
}

export async function getLoppukeskustelu(id: number) {
  const path = `/kouluttaja/koejakso/loppukeskustelu/${id}`
  return await axios.get<LoppukeskusteluLomake>(path)
}

export async function putLoppukeskustelu(form: LoppukeskusteluLomake) {
  const path = `${resolveRolePath()}/koejakso/loppukeskustelu`
  return await axios.put<LoppukeskusteluLomake>(path, form)
}

export async function putSuoritusarviointi(formData: FormData) {
  const path = 'kouluttaja/suoritusarvioinnit'
  return await axios.put<Suoritusarviointi>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function getSeurantajaksot() {
  const path = `${resolveRolePath()}/seurantakeskustelut/seurantajaksot`
  return await axios.get(path)
}

export async function getSeurantajakso(seurantajaksoId: string) {
  const path = `${resolveRolePath()}/seurantakeskustelut/seurantajakso/${seurantajaksoId}`
  return await axios.get<Seurantajakso>(path)
}

export async function getSeurantajaksonTiedot(id: number) {
  const path = `${resolveRolePath()}/seurantakeskustelut/seurantajaksontiedot?id=${id}`
  return await axios.get<SeurantajaksonTiedot>(path)
}

export async function putSeurantajakso(form: Seurantajakso) {
  const path = `${resolveRolePath()}/seurantakeskustelut/seurantajakso/${form.id}`
  return await axios.put<Seurantajakso>(path, form)
}

export async function getErikoistujienSeurantaKouluttajaRajaimet() {
  const path = '/kouluttaja/etusivu/erikoistujien-seuranta-rajaimet'
  return await axios.get<ErikoistujienSeurantaKouluttajaRajaimet>(path)
}

export async function getErikoistujienSeuranta(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
  'erikoisalaId.equals'?: number
  'asetusId.equals'?: number
}) {
  const path = `/kouluttaja/etusivu/erikoistujien-seuranta`
  return await axios.get<Page<ErikoistujanEteneminen>>(path, {
    params: {
      ...params
    }
  })
}

export async function getEtusivuKoejaksot() {
  const path = `/kouluttaja/etusivu/koejaksot`
  return await axios.get<KoejaksonVaihe[]>(path)
}

export async function getEtusivuArviointipyynnot() {
  const path = `/kouluttaja/arviointipyynnot`
  return await axios.get<Arviointipyynto[]>(path)
}

export async function getEtusivuSeurantajaksot() {
  const path = `/kouluttaja/etusivu/seurantajaksot`
  return await axios.get<Seurantajakso[]>(path)
}

export async function getEtusivuVanhenevatKatseluoikeudet() {
  const path = `/kouluttaja/etusivu/vanhenevat-katseluoikeudet`
  return await axios.get<Katseluoikeus[]>(path)
}

export async function getArviointityokalut() {
  const path = `/kouluttaja/arviointityokalut`
  return await axios.get<Arviointityokalu[]>(path)
}

export async function getArviointityokaluKategoriat() {
  const path = `/kouluttaja/arviointityokalut/kategoriat`
  return await axios.get<ArviointityokaluKategoria[]>(path)
}

function resolveRolePath() {
  if (Vue.prototype.$isVastuuhenkilo()) {
    return 'vastuuhenkilo'
  } else {
    return 'kouluttaja'
  }
}
