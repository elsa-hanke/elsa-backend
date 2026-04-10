import axios from 'axios'

import {
  Page,
  KayttajahallintaKayttajaListItem,
  Erikoisala,
  Opintoopas,
  Arviointiasteikko,
  ArvioitavanKokonaisuudenKategoria,
  ArvioitavanKokonaisuudenKategoriaWithErikoisala,
  ArvioitavaKokonaisuusWithErikoisala,
  SuoritteenKategoria,
  SuoritteenKategoriaWithErikoisala,
  SuoriteWithErikoisala,
  Ilmoitus,
  ArviointityokaluKategoria,
  Arviointityokalu
} from '@/types'

export async function getErikoistuvatLaakarit(params: {
  page?: number
  size?: number
  sort: string | null
}) {
  const path = 'tekninen-paakayttaja/erikoistuvat-laakarit'
  return await axios.get<Page<KayttajahallintaKayttajaListItem>>(path, {
    params: {
      ...params
    }
  })
}

export async function getErikoisalat() {
  const path = `/tekninen-paakayttaja/erikoisalat`
  return await axios.get<Erikoisala[]>(path)
}

export async function getErikoisala(id: string) {
  const path = `/tekninen-paakayttaja/erikoisalat/${id}`
  return await axios.get<Erikoisala>(path)
}

export async function getOpintooppaat(erikoisalaId: string) {
  const path = `/tekninen-paakayttaja/erikoisalat/${erikoisalaId}/oppaat`
  return await axios.get<Opintoopas[]>(path)
}

export async function getUusinOpas(erikoisalaId: string) {
  const path = `/tekninen-paakayttaja/erikoisalat/${erikoisalaId}/uusinopas`
  return await axios.get<Opintoopas>(path)
}

export async function getOpinoopas(id: string) {
  const path = `/tekninen-paakayttaja/opintoopas/${id}`
  return await axios.get<Opintoopas>(path)
}

export async function postOpinoopas(form: Opintoopas) {
  const path = `/tekninen-paakayttaja/opintoopas`
  return await axios.post<Opintoopas>(path, form)
}

export async function putOpinoopas(form: Opintoopas) {
  const path = `/tekninen-paakayttaja/opintoopas`
  return await axios.put<Opintoopas>(path, form)
}

export async function getArviointiasteikot() {
  const path = `/tekninen-paakayttaja/arviointiasteikot`
  return await axios.get<Arviointiasteikko[]>(path)
}

export async function getArvioitavanKokonaisuudenKategoriat(erikoisalaId: string) {
  const path = `/tekninen-paakayttaja/erikoisalat/${erikoisalaId}/arvioitavankokonaisuudenkategoriat`
  return await axios.get<ArvioitavanKokonaisuudenKategoria[]>(path)
}

export async function getArvioitavatKokonaisuudet(erikoisalaId: string) {
  const path = `/tekninen-paakayttaja/erikoisalat/${erikoisalaId}/arvioitavatkokonaisuudet`
  return await axios.get<ArvioitavanKokonaisuudenKategoria[]>(path)
}

export async function getArvioitavanKokonaisuudenKategoria(id: string) {
  const path = `/tekninen-paakayttaja/arvioitavankokonaisuudenkategoria/${id}`
  return await axios.get<ArvioitavanKokonaisuudenKategoriaWithErikoisala>(path)
}

export async function postArvioitavanKokonaisuudenKategoria(
  form: ArvioitavanKokonaisuudenKategoriaWithErikoisala
) {
  const path = `/tekninen-paakayttaja/arvioitavankokonaisuudenkategoria`
  return await axios.post<ArvioitavanKokonaisuudenKategoriaWithErikoisala>(path, form)
}

export async function putArvioitavanKokonaisuudenKategoria(
  form: ArvioitavanKokonaisuudenKategoriaWithErikoisala
) {
  const path = `/tekninen-paakayttaja/arvioitavankokonaisuudenkategoria`
  return await axios.put<ArvioitavanKokonaisuudenKategoriaWithErikoisala>(path, form)
}

export async function getArvioitavaKokonaisuus(id: string) {
  const path = `/tekninen-paakayttaja/arvioitavakokonaisuus/${id}`
  return await axios.get<ArvioitavaKokonaisuusWithErikoisala>(path)
}

export async function postArvioitavaKokonaisuus(form: ArvioitavaKokonaisuusWithErikoisala) {
  const path = `/tekninen-paakayttaja/arvioitavakokonaisuus`
  return await axios.post<ArvioitavaKokonaisuusWithErikoisala>(path, form)
}

export async function putArvioitavaKokonaisuus(form: ArvioitavaKokonaisuusWithErikoisala) {
  const path = `/tekninen-paakayttaja/arvioitavakokonaisuus`
  return await axios.put<ArvioitavaKokonaisuusWithErikoisala>(path, form)
}

export async function deleteArvioitavaKokonaisuus(id: string) {
  const path = `/tekninen-paakayttaja/arvioitavatkokonaisuudet/${id}`
  return await axios.delete<SuoriteWithErikoisala>(path)
}

export async function getSuoritteenKategoriat(erikoisalaId: string) {
  const path = `/tekninen-paakayttaja/erikoisalat/${erikoisalaId}/suoritteenkategoriat`
  return await axios.get<SuoritteenKategoria[]>(path)
}

export async function getSuoritteet(erikoisalaId: string) {
  const path = `/tekninen-paakayttaja/erikoisalat/${erikoisalaId}/suoritteet`
  return await axios.get<SuoritteenKategoria[]>(path)
}

export async function getSuoritteenKategoria(id: string) {
  const path = `/tekninen-paakayttaja/suoritteenkategoria/${id}`
  return await axios.get<SuoritteenKategoriaWithErikoisala>(path)
}

export async function postSuoritteenKategoria(form: SuoritteenKategoriaWithErikoisala) {
  const path = `/tekninen-paakayttaja/suoritteenkategoria`
  return await axios.post<SuoritteenKategoriaWithErikoisala>(path, form)
}

export async function putSuoritteenKategoria(form: SuoritteenKategoriaWithErikoisala) {
  const path = `/tekninen-paakayttaja/suoritteenkategoria`
  return await axios.put<SuoritteenKategoriaWithErikoisala>(path, form)
}

export async function getSuorite(id: string) {
  const path = `/tekninen-paakayttaja/suorite/${id}`
  return await axios.get<SuoriteWithErikoisala>(path)
}

export async function postSuorite(form: SuoriteWithErikoisala) {
  const path = `/tekninen-paakayttaja/suorite`
  return await axios.post<SuoriteWithErikoisala>(path, form)
}

export async function putSuorite(form: SuoriteWithErikoisala) {
  const path = `/tekninen-paakayttaja/suorite`
  return await axios.put<SuoriteWithErikoisala>(path, form)
}

export async function deleteSuorite(id: string) {
  const path = `/tekninen-paakayttaja/suoritteet/${id}`
  return await axios.delete<SuoriteWithErikoisala>(path)
}

export async function postIlmoitus(ilmoitus: Ilmoitus) {
  const path = `/tekninen-paakayttaja/ilmoitukset`
  return await axios.post<Ilmoitus>(path, ilmoitus)
}

export async function putIlmoitus(ilmoitus: Ilmoitus) {
  const path = `/tekninen-paakayttaja/ilmoitukset`
  return await axios.put<Ilmoitus>(path, ilmoitus)
}

export async function deleteIlmoitus(id: number) {
  const path = `/tekninen-paakayttaja/ilmoitukset/${id}`
  return await axios.delete<Ilmoitus>(path)
}

export async function postArviointityokalutKategoria(form: ArviointityokaluKategoria) {
  const path = `/tekninen-paakayttaja/arviointityokalut/kategoria`
  return await axios.post<ArviointityokaluKategoria>(path, form)
}

export async function getArviointityokalutKategoriat() {
  const path = `/tekninen-paakayttaja/arviointityokalut/kategoriat`
  return await axios.get<ArviointityokaluKategoria[]>(path)
}

export async function getArviointityokalutKategoria(id: string) {
  const path = `/tekninen-paakayttaja/arviointityokalut/kategoria/${id}`
  return await axios.get<ArviointityokaluKategoria>(path)
}

export async function patchArviointityokalutKategoria(form: ArviointityokaluKategoria) {
  const path = `/tekninen-paakayttaja/arviointityokalut/kategoria`
  return await axios.patch<ArviointityokaluKategoria>(path, form)
}

export async function deleteArviointityokalutKategoria(id: number) {
  const path = `/tekninen-paakayttaja/arviointityokalut/kategoria/${id}`
  return await axios.delete<ArviointityokaluKategoria>(path)
}

export async function getArviointityokalut() {
  const path = `/tekninen-paakayttaja/arviointityokalut`
  return await axios.get<Arviointityokalu[]>(path)
}

export async function getPoistetutArviointityokalut() {
  const path = `/tekninen-paakayttaja/poistetut-arviointityokalut`
  return await axios.get<Arviointityokalu[]>(path)
}

export async function getArviointityokalu(id: number) {
  const path = `/tekninen-paakayttaja/arviointityokalu/${id}`
  return await axios.get<Arviointityokalu>(path)
}

export async function postArviointityokalu(form: Arviointityokalu) {
  const formData = new FormData()
  formData.append('data', JSON.stringify(form))
  if (form.liite) {
    formData.append('liiteData', form.liite)
  }
  const path = `/tekninen-paakayttaja/arviointityokalu`
  return await axios.post<Arviointityokalu>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function patchArviointityokalu(form: Arviointityokalu) {
  const formData = new FormData()
  formData.append('data', JSON.stringify(form))
  if (form.liite) {
    formData.append('liiteData', form.liite)
  }
  const path = `/tekninen-paakayttaja/arviointityokalu`
  return await axios.patch<Arviointityokalu>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function deleteArviointityokalu(id: number) {
  const path = `/tekninen-paakayttaja/arviointityokalu/${id}`
  return await axios.delete<Arviointityokalu>(path)
}

export async function palautaArviointityokalu(id: number) {
  const path = `/tekninen-paakayttaja/arviointityokalu/${id}/palauta`
  return await axios.patch(path)
}
