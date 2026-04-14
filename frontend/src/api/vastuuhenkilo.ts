import axios from 'axios'

import {
  KoulutussopimusLomake,
  VastuuhenkilonArvioLomake,
  AloituskeskusteluLomake,
  ValiarviointiLomake,
  KehittamistoimenpiteetLomake,
  LoppukeskusteluLomake,
  Suoritusarviointi,
  KoejaksonVaihe,
  Arviointipyynto,
  Page,
  TerveyskeskuskoulutusjaksonVaihe,
  TerveyskeskuskoulutusjaksonHyvaksyminen,
  ValmistumispyyntoListItem,
  Valmistumispyynto,
  ValmistumispyyntoArviointienTila,
  ValmistumispyyntoLomakeOsaamisenArviointi,
  ValmistumispyyntoVirkailijanTarkistus,
  ValmistumispyyntoHyvaksynta,
  Asiakirja,
  Seurantajakso,
  ErikoistujanEteneminen,
  ErikoistujienSeurantaVastuuhenkiloRajaimet,
  VastuuhenkilonVastuualueet,
  KoulutettavanEteneminen,
  Arviointityokalu,
  ArviointityokaluKategoria
} from '@/types'

export async function getKoejaksot() {
  const path = 'vastuuhenkilo/koejaksot'
  return await axios.get(path)
}

export async function getKoulutussopimus(id: number) {
  const path = `/vastuuhenkilo/koejakso/koulutussopimus/${id}`
  return await axios.get<KoulutussopimusLomake>(path)
}

export async function getAloituskeskustelu(id: number) {
  const path = `/vastuuhenkilo/koejakso/aloituskeskustelu/${id}`
  return await axios.get<AloituskeskusteluLomake>(path)
}

export async function getValiarviointi(id: number) {
  const path = `/vastuuhenkilo/koejakso/valiarviointi/${id}`
  return await axios.get<ValiarviointiLomake>(path)
}

export async function getKehittamistoimenpiteet(id: number) {
  const path = `/vastuuhenkilo/koejakso/kehittamistoimenpiteet/${id}`
  return await axios.get<KehittamistoimenpiteetLomake>(path)
}

export async function getLoppukeskustelu(id: number) {
  const path = `/vastuuhenkilo/koejakso/loppukeskustelu/${id}`
  return await axios.get<LoppukeskusteluLomake>(path)
}

export async function putKoulutussopimus(form: KoulutussopimusLomake) {
  const path = 'vastuuhenkilo/koejakso/koulutussopimus'
  return await axios.put<KoulutussopimusLomake>(path, form)
}

export async function getVastuuhenkilonArvio(id: number) {
  const path = `/vastuuhenkilo/koejakso/vastuuhenkilonarvio/${id}`
  return await axios.get<VastuuhenkilonArvioLomake>(path)
}

export async function putVastuuhenkilonArvio(form: VastuuhenkilonArvioLomake) {
  const path = 'vastuuhenkilo/koejakso/vastuuhenkilonarvio'
  return await axios.put<VastuuhenkilonArvioLomake>(path, form)
}

export async function putSuoritusarviointi(formData: FormData) {
  const path = 'vastuuhenkilo/suoritusarvioinnit'
  return await axios.put<Suoritusarviointi>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function getErikoistujienSeurantaVastuuhenkiloRajaimet() {
  const path = '/vastuuhenkilo/etusivu/erikoistujien-seuranta-rajaimet'
  return await axios.get<ErikoistujienSeurantaVastuuhenkiloRajaimet>(path)
}

export async function getErikoistujienSeuranta(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
  'erikoisalaId.equals'?: number
  'asetusId.equals'?: number
}) {
  const path = `/vastuuhenkilo/etusivu/erikoistujien-seuranta`
  return await axios.get<Page<ErikoistujanEteneminen>>(path, {
    params: {
      ...params
    }
  })
}

export async function getEtusivuKoejaksot() {
  const path = `/vastuuhenkilo/etusivu/koejaksot`
  return await axios.get<KoejaksonVaihe[]>(path)
}

export async function getEtusivuArviointipyynnot() {
  const path = `/vastuuhenkilo/arviointipyynnot`
  return await axios.get<Arviointipyynto[]>(path)
}

export async function getEtusivuValmistumispyynnot() {
  const path = `/vastuuhenkilo/etusivu/valmistumispyynnot`
  return await axios.get<ValmistumispyyntoListItem[]>(path)
}

export async function getEtusivuKoulutettavienValmistumispyynnot() {
  const path = `/vastuuhenkilo/etusivu/yek-valmistumispyynnot`
  return await axios.get<ValmistumispyyntoListItem[]>(path)
}

export async function getEtusivuSeurantajaksot() {
  const path = `/vastuuhenkilo/etusivu/seurantajaksot`
  return await axios.get<Seurantajakso[]>(path)
}

export async function getTerveyskeskuskoulutusjaksot(params: {
  page?: number
  size?: number
  sort: string | null
  'erikoistujanNimi.contains'?: string
  'erikoisalaId.equals'?: number
  avoin?: boolean
}) {
  return await axios.get<Page<TerveyskeskuskoulutusjaksonVaihe>>(
    '/vastuuhenkilo/terveyskeskuskoulutusjaksot',
    {
      params: {
        ...params
      }
    }
  )
}

export async function getTerveyskeskuskoulutusjakso(id: string) {
  const path = `vastuuhenkilo/terveyskeskuskoulutusjakso/${id}`
  return await axios.get<TerveyskeskuskoulutusjaksonHyvaksyminen>(path)
}

export async function putTerveyskeskuskoulutusjakso(id: string, korjausehdotus: string) {
  const path = `vastuuhenkilo/terveyskeskuskoulutusjakson-hyvaksynta/${id}`
  return await axios.put<TerveyskeskuskoulutusjaksonHyvaksyminen>(path, {
    korjausehdotus: korjausehdotus
  })
}

export async function getVastuualueet() {
  const path = 'vastuuhenkilo/vastuualueet'
  return await axios.get<VastuuhenkilonVastuualueet>(path)
}

export async function getValmistumispyynnot(params: {
  page?: number
  size?: number
  sort?: string
  'erikoistujanNimi.contains'?: string
  'tila.equals'?: string
}) {
  return await axios.get<Page<ValmistumispyyntoListItem>>('vastuuhenkilo/valmistumispyynnot', {
    params: {
      ...params
    }
  })
}

export async function getValmistumispyyntoOsaamisenArviointi(id: number) {
  const path = `/vastuuhenkilo/valmistumispyynnon-arviointi/${id}`
  return await axios.get<Valmistumispyynto>(path)
}

export async function getValmistumispyyntoArviointienTila(id: number) {
  const path = `/vastuuhenkilo/valmistumispyynto-arviointien-tila/${id}`
  return await axios.get<ValmistumispyyntoArviointienTila>(path)
}

export async function getValmistumispyyntoHyvaksynta(id: number) {
  const path = `/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${id}`
  return await axios.get<ValmistumispyyntoVirkailijanTarkistus>(path)
}

export async function putValmistumispyyntoHyvaksynta(form: ValmistumispyyntoHyvaksynta) {
  const path = `/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${form.id}`
  return await axios.put<ValmistumispyyntoHyvaksynta>(path, form)
}

export async function putValmistumispyynto(form: ValmistumispyyntoLomakeOsaamisenArviointi) {
  const path = `/vastuuhenkilo/valmistumispyynnon-arviointi/${form.id}`
  return await axios.put<Valmistumispyynto>(path, form)
}

export async function getValmistumispyyntoAsiakirja(
  asiakirjaId: number,
  valmistumispyyntoId: number
) {
  const path = `/vastuuhenkilo/valmistumispyynto/${valmistumispyyntoId}/asiakirja/${asiakirjaId}`
  return await axios.get<Asiakirja>(path)
}

export async function getKoulutettavienSeurantaList(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
  'erikoisalaId.equals'?: number
  'asetusId.equals'?: number
}) {
  return await axios.get<Page<KoulutettavanEteneminen>>(
    '/vastuuhenkilo/etusivu/koulutettavien-seuranta',
    {
      params: {
        ...params
      }
    }
  )
}

export async function getYliopisto() {
  const path = '/vastuuhenkilo/etusivu/yliopisto'
  return await axios.get<string>(path)
}

export async function getArviointityokalutVastuuhenkilo() {
  const path = `/vastuuhenkilo/arviointityokalut`
  return await axios.get<Arviointityokalu[]>(path)
}

export async function getArviointityokaluKategoriatVastuuhenkilo() {
  const path = `/vastuuhenkilo/arviointityokalut/kategoriat`
  return await axios.get<ArviointityokaluKategoria[]>(path)
}
