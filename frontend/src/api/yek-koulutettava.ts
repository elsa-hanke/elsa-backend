import axios from 'axios'

import {
  AvoinAsia,
  ErikoistumisenEdistyminen,
  LaillistamistiedotLomakeKoulutettava,
  Opintosuoritus,
  TerveyskeskuskoulutusjaksonHyvaksyminen,
  Tyoskentelyjakso,
  TyoskentelyjaksoLomake,
  Valmistumispyynto,
  ValmistumispyyntoLomakeErikoistuja,
  ValmistumispyyntoSuoritustenTila
} from '@/types'
import { wrapToFormData } from '@/utils/functions'

export async function getTyoskentelyjakso(id: number | string) {
  const path = `yek-koulutettava/tyoskentelyjaksot/${id}`
  return await axios.get<Tyoskentelyjakso>(path)
}

export async function getTyoskentelyjaksoLomake() {
  const path = 'yek-koulutettava/tyoskentelyjakso-lomake'
  return await axios.get<TyoskentelyjaksoLomake>(path)
}

export async function putTyoskentelyjakso(formData: FormData) {
  const path = 'yek-koulutettava/tyoskentelyjaksot'
  await axios.put(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function putKoulutettavaLaillistamispaiva(form: LaillistamistiedotLomakeKoulutettava) {
  return await axios.put('yek-koulutettava/laillistamispaiva', wrapToFormData(form), {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function getYekTeoriakoulutukset() {
  const path = 'yek-koulutettava/teoriakoulutukset'
  return await axios.get<Opintosuoritus[]>(path)
}

export async function getYekValmistumispyynto() {
  const path = 'yek-koulutettava/valmistumispyynto'
  return await axios.get<Valmistumispyynto>(path)
}

export async function getYekValmistumispyyntoSuoritustenTila() {
  const path = 'yek-koulutettava/valmistumispyynto-suoritusten-tila'
  return await axios.get<ValmistumispyyntoSuoritustenTila>(path)
}

export async function postYekValmistumispyynto(form: ValmistumispyyntoLomakeErikoistuja) {
  const formData = wrapToFormData(form)
  if (form.laillistamistodistus) {
    formData.append('laillistamistodistus', form.laillistamistodistus)
  }
  const path = 'yek-koulutettava/valmistumispyynto'
  return await axios.post<Valmistumispyynto>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function putYekValmistumispyynto(form: ValmistumispyyntoLomakeErikoistuja) {
  const formData = wrapToFormData(form)
  if (form.laillistamistodistus) {
    formData.append('laillistamistodistus', form.laillistamistodistus)
  }
  const path = 'yek-koulutettava/valmistumispyynto'
  return await axios.put<Valmistumispyynto>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function getErikoistumisenEdistyminen() {
  const path = 'yek-koulutettava/etusivu/erikoistumisen-edistyminen'
  return await axios.get<ErikoistumisenEdistyminen>(path)
}

export async function getAvoimetAsiat() {
  const path = 'yek-koulutettava/etusivu/avoimet-asiat'
  return await axios.get<AvoinAsia[]>(path)
}

export async function getTerveyskeskuskoulutusjakso() {
  const path = 'yek-koulutettava/tyoskentelyjaksot/terveyskeskuskoulutusjakso'
  return await axios.get<TerveyskeskuskoulutusjaksonHyvaksyminen>(path)
}
