import axios from 'axios'

import {
  ErikoistujanEteneminenVirkailija,
  ErikoistujienSeurantaVirkailijaRajaimet,
  KoejaksonVaihe,
  VastuuhenkilonArvioLomake,
  Page,
  Erikoisala,
  TerveyskeskuskoulutusjaksonHyvaksyminen,
  TerveyskeskuskoulutusjaksonVaihe,
  ValmistumispyyntoListItem,
  ValmistumispyyntoVirkailijanTarkistus,
  ValmistumispyynnonVirkailijanTarkistusLomake,
  Asiakirja,
  TerveyskeskuskoulutusjaksonHyvaksyntaForm,
  Kurssikoodi,
  OpintosuoritusTyyppi,
  KoulutettavanEteneminen
} from '@/types'
import { wrapToFormData } from '@/utils/functions'

export async function getErikoistujienSeurantaRajaimet() {
  const path = '/virkailija/etusivu/erikoistujien-seuranta-rajaimet'
  return await axios.get<ErikoistujienSeurantaVirkailijaRajaimet>(path)
}

export async function getYliopisto() {
  const path = '/virkailija/etusivu/yliopisto'
  return await axios.get<string>(path)
}

export async function getKoejaksot(params: {
  page?: number
  size?: number
  sort: string | null
  'erikoistujanNimi.contains'?: string
  'erikoisalaId.equals'?: number
  avoin?: boolean
}) {
  return await axios.get<Page<KoejaksonVaihe>>('/virkailija/koejaksot', {
    params: {
      ...params
    }
  })
}

export async function getVastuuhenkilonArvio(id: number) {
  const path = `/virkailija/koejakso/vastuuhenkilonarvio/${id}`
  return await axios.get<VastuuhenkilonArvioLomake>(path)
}

export async function getErikoistujienSeurantaList(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
  'erikoisalaId.equals'?: number
  'asetusId.equals'?: number
}) {
  return await axios.get<Page<ErikoistujanEteneminenVirkailija>>(
    '/virkailija/etusivu/erikoistujien-seuranta',
    {
      params: {
        ...params
      }
    }
  )
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
    '/virkailija/etusivu/koulutettavien-seuranta',
    {
      params: {
        ...params
      }
    }
  )
}

export async function getEtusivuKoejaksot() {
  const path = '/virkailija/etusivu/koejaksot'
  return await axios.get<KoejaksonVaihe[]>(path)
}

export async function getEtusivuValmistumispyynnot() {
  const path = `/virkailija/etusivu/valmistumispyynnot`
  return await axios.get<ValmistumispyyntoListItem[]>(path)
}

export async function getEtusivuKoulutettavienValmistumispyynnot() {
  const path = `/virkailija/etusivu/koulutettavien-valmistumispyynnot`
  return await axios.get<ValmistumispyyntoListItem[]>(path)
}

export async function getValmistumispyynnot(params: {
  page?: number
  size?: number
  sort?: string
  'erikoistujanNimi.contains'?: string
  'tila.equals'?: string
}) {
  return await axios.get<Page<ValmistumispyyntoListItem>>('virkailija/valmistumispyynnot', {
    params: {
      ...params
    }
  })
}

export async function putVastuuhenkilonArvio(form: VastuuhenkilonArvioLomake) {
  const path = '/virkailija/koejakso/vastuuhenkilonarvio'
  return await axios.put<VastuuhenkilonArvioLomake>(path, form)
}

export async function getErikoisalat() {
  const path = `/virkailija/erikoisalat`
  return await axios.get<Erikoisala[]>(path)
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
    '/virkailija/terveyskeskuskoulutusjaksot',
    {
      params: {
        ...params
      }
    }
  )
}

export async function getTerveyskeskuskoulutusjakso(id: string) {
  const path = `virkailija/terveyskeskuskoulutusjakso/${id}`
  return await axios.get<TerveyskeskuskoulutusjaksonHyvaksyminen>(path)
}

export async function putTerveyskeskuskoulutusjakso(
  id: string,
  form: TerveyskeskuskoulutusjaksonHyvaksyntaForm,
  korjausehdotus?: string,
  lisatiedotVirkailijalta?: string
) {
  const formData = wrapToFormData({
    korjausehdotus: korjausehdotus || null,
    lisatiedotVirkailijalta: lisatiedotVirkailijalta || null
  })
  if (form.laillistamispaiva != null) formData.append('laillistamispaiva', form.laillistamispaiva)
  if (form.laillistamispaivanLiite != null)
    formData.append(
      'laillistamispaivanLiite',
      form.laillistamispaivanLiite,
      form.laillistamispaivanLiite?.name
    )
  const emptyBody =
    !korjausehdotus &&
    !lisatiedotVirkailijalta &&
    form.laillistamispaiva == null &&
    form.laillistamispaivanLiite == null
  const path = `virkailija/terveyskeskuskoulutusjakson-hyvaksynta/${id}`
  return await axios.put<TerveyskeskuskoulutusjaksonHyvaksyminen>(path, emptyBody ? {} : formData, {
    headers: {
      'Content-Type': emptyBody ? 'application/json' : 'multipart/form-data'
    },
    timeout: 120000
  })
}

export async function getValmistumispyyntoTarkistus(id: number) {
  const path = `/virkailija/valmistumispyynnon-tarkistus/${id}`
  return await axios.get<ValmistumispyyntoVirkailijanTarkistus>(path)
}

export async function putValmistumispyynto(form: ValmistumispyynnonVirkailijanTarkistusLomake) {
  const formData = wrapToFormData(form)
  if (form.laillistamistodistus) {
    formData.append('laillistamistodistus', form.laillistamistodistus)
  }
  const path = `/virkailija/valmistumispyynnon-tarkistus/${form.id}`
  return await axios.put<ValmistumispyynnonVirkailijanTarkistusLomake>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function getValmistumispyyntoAsiakirja(
  asiakirjaId: number,
  valmistumispyyntoId: number
) {
  const path = `/virkailija/valmistumispyynto/${valmistumispyyntoId}/asiakirja/${asiakirjaId}`
  return await axios.get<Asiakirja>(path)
}

export async function getKurssikoodit() {
  const path = `/virkailija/kurssikoodit`
  return await axios.get<Kurssikoodi[]>(path)
}

export async function getKurssikoodi(id: string) {
  const path = `/virkailija/kurssikoodit/${id}`
  return await axios.get<Kurssikoodi>(path)
}

export async function getOpintosuoritusTyypit() {
  const path = `/virkailija/kurssikoodit/tyypit`
  return await axios.get<OpintosuoritusTyyppi[]>(path)
}

export async function postKurssikoodi(form: Kurssikoodi) {
  const path = 'virkailija/kurssikoodit'
  return await axios.post<Kurssikoodi>(path, form)
}

export async function putKurssikoodi(form: Kurssikoodi) {
  const path = `virkailija/kurssikoodit`
  return await axios.put<Kurssikoodi>(path, form)
}

export async function deleteKurssikoodi(id: string) {
  const path = `/virkailija/kurssikoodit/${id}`
  return await axios.delete<Kurssikoodi>(path)
}
