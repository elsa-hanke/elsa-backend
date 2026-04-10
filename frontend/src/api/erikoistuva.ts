import axios from 'axios'

import {
  AloituskeskusteluLomake,
  KehittamistoimenpiteetLomake,
  Koejakso,
  Koulutusjakso,
  KoulutusjaksoLomake,
  KoulutussopimusLomake,
  Koulutussuunnitelma,
  LoppukeskusteluLomake,
  Teoriakoulutukset,
  Teoriakoulutus,
  Tyoskentelyjakso,
  TyoskentelyjaksoLomake,
  Seurantajakso,
  SeurantajaksonTiedot,
  ValiarviointiLomake,
  VastuuhenkilonArvioLomake,
  VastuuhenkilonArvioLomakeErikoistuva,
  Kayttaja,
  Paivakirjamerkinta,
  PaivakirjamerkintaLomake,
  Page,
  PaivakirjamerkintaRajaimet,
  KaytonAloitusModel,
  ErikoistuvaLaakari,
  ErikoistumisenEdistyminen,
  AvoinAsia,
  TerveyskeskuskoulutusjaksonHyvaksyminen,
  TerveyskeskuskoulutusjaksonHyvaksyntaForm,
  ValmistumispyyntoSuoritustenTila,
  ValmistumispyyntoLomakeErikoistuja,
  Valmistumispyynto,
  Suoritusarviointi
} from '@/types'
import { wrapToFormData } from '@/utils/functions'

export async function getErikoistuvaLaakari() {
  const path = 'erikoistuva-laakari'
  return await axios.get<ErikoistuvaLaakari>(path)
}

export async function getKoejakso() {
  const path = '/erikoistuva-laakari/koejakso'
  return await axios.get<Koejakso>(path)
}

export async function postKoulutussopimus(form: KoulutussopimusLomake) {
  const path = 'erikoistuva-laakari/koejakso/koulutussopimus'
  return await axios.post<KoulutussopimusLomake>(path, form)
}

export async function putKoulutussopimus(form: KoulutussopimusLomake) {
  const path = 'erikoistuva-laakari/koejakso/koulutussopimus'
  return await axios.put<KoulutussopimusLomake>(path, form)
}

export async function deleteKoulutussopimus(form: KoulutussopimusLomake) {
  const path = `erikoistuva-laakari/koejakso/koulutussopimus/${form.id}`
  return await axios.delete<KoulutussopimusLomake>(path)
}

export async function postAloituskeskustelu(form: AloituskeskusteluLomake) {
  const path = 'erikoistuva-laakari/koejakso/aloituskeskustelu'
  return await axios.post<AloituskeskusteluLomake>(path, form)
}

export async function putAloituskeskustelu(form: AloituskeskusteluLomake) {
  const path = 'erikoistuva-laakari/koejakso/aloituskeskustelu'
  return await axios.put<AloituskeskusteluLomake>(path, form)
}

export async function deleteAloituskeskustelu(form: AloituskeskusteluLomake) {
  const path = `erikoistuva-laakari/koejakso/aloituskeskustelu/${form.id}`
  return await axios.delete<AloituskeskusteluLomake>(path)
}

export async function postValiarviointi(form: ValiarviointiLomake) {
  const path = 'erikoistuva-laakari/koejakso/valiarviointi'
  return await axios.post<ValiarviointiLomake>(path, form)
}

export async function putValiarviointi(form: ValiarviointiLomake) {
  const path = 'erikoistuva-laakari/koejakso/valiarviointi'
  return await axios.put<ValiarviointiLomake>(path, form)
}

export async function deleteValiarviointi(form: ValiarviointiLomake) {
  const path = `erikoistuva-laakari/koejakso/valiarviointi/${form.id}`
  return await axios.delete<ValiarviointiLomake>(path)
}

export async function postKehittamistoimenpiteet(form: KehittamistoimenpiteetLomake) {
  const path = 'erikoistuva-laakari/koejakso/kehittamistoimenpiteet'
  return await axios.post<KehittamistoimenpiteetLomake>(path, form)
}

export async function putKehittamistoimenpiteet(form: KehittamistoimenpiteetLomake) {
  const path = 'erikoistuva-laakari/koejakso/kehittamistoimenpiteet'
  return await axios.put<KehittamistoimenpiteetLomake>(path, form)
}

export async function deleteKehittamistoimenpiteet(form: KehittamistoimenpiteetLomake) {
  const path = `erikoistuva-laakari/koejakso/kehittamistoimenpiteet/${form.id}`
  return await axios.delete<KehittamistoimenpiteetLomake>(path)
}

export async function postLoppukeskustelu(form: LoppukeskusteluLomake) {
  const path = 'erikoistuva-laakari/koejakso/loppukeskustelu'
  return await axios.post<LoppukeskusteluLomake>(path, form)
}

export async function putLoppukeskustelu(form: LoppukeskusteluLomake) {
  const path = 'erikoistuva-laakari/koejakso/loppukeskustelu'
  return await axios.put<LoppukeskusteluLomake>(path, form)
}

export async function deleteLoppukeskustelu(form: LoppukeskusteluLomake) {
  const path = `erikoistuva-laakari/koejakso/loppukeskustelu/${form.id}`
  return await axios.delete<LoppukeskusteluLomake>(path)
}

export async function postVastuuhenkilonArvio(formData: FormData) {
  return await axios.post<VastuuhenkilonArvioLomake>(
    'erikoistuva-laakari/koejakso/vastuuhenkilonarvio',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }
  )
}

export async function putVastuuhenkilonArvio(formData: FormData) {
  return await axios.put<VastuuhenkilonArvioLomake>(
    'erikoistuva-laakari/koejakso/vastuuhenkilonarvio',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }
  )
}

export async function deleteVastuuhenkilonArvio(id: number) {
  return await axios.delete<VastuuhenkilonArvioLomake>(
    `erikoistuva-laakari/koejakso/vastuuhenkilonarvio/${id}`
  )
}

export async function getKouluttajat() {
  const path = '/erikoistuva-laakari/kouluttajat'
  return await axios.get<Kayttaja[]>(path)
}

export async function getKouluttajatJaVastuuhenkilot() {
  const path = '/erikoistuva-laakari/kouluttajat-vastuuhenkilot'
  return await axios.get<Kayttaja[]>(path)
}

export async function getTyoskentelyjakso(id: number | string) {
  const path = `erikoistuva-laakari/tyoskentelyjaksot/${id}`
  return await axios.get<Tyoskentelyjakso>(path)
}

export async function getTyoskentelyjaksoLomake() {
  const path = 'erikoistuva-laakari/tyoskentelyjakso-lomake'
  return await axios.get<TyoskentelyjaksoLomake>(path)
}

export async function putTyoskentelyjakso(formData: FormData) {
  const path = 'erikoistuva-laakari/tyoskentelyjaksot'
  await axios.put(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function deleteTyoskentelyjakso(id: number | string) {
  const path = `erikoistuva-laakari/tyoskentelyjaksot/${id}`
  await axios.delete(path)
}

export async function getKoulutussopimusLomake() {
  const path = 'erikoistuva-laakari/koulutussopimus-lomake'
  return await axios.get<KoulutussopimusLomake>(path)
}

export async function getYliopistot() {
  const path = '/yliopistot'
  return await axios.get(path)
}

export async function getHakaYliopistot() {
  const path = '/haka-yliopistot'
  return await axios.get(path)
}

export async function getVastuuhenkilonArvioLomake() {
  const path = '/erikoistuva-laakari/vastuuhenkilonarvio-lomake'
  return await axios.get<VastuuhenkilonArvioLomakeErikoistuva>(path)
}

export async function getKoulutussuunnitelma() {
  const path = 'erikoistuva-laakari/koulutussuunnitelma'
  return await axios.get<Koulutussuunnitelma>(path)
}

export async function putKoulutussuunnitelma(form: Koulutussuunnitelma) {
  return await axios.put<Koulutussuunnitelma>(
    'erikoistuva-laakari/koulutussuunnitelma',
    wrapToFormData(form),
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }
  )
}

export async function getKoulutusjaksoLomake() {
  const path = 'erikoistuva-laakari/koulutussuunnitelma/koulutusjakso-lomake'
  return await axios.get<KoulutusjaksoLomake>(path)
}

export async function postKoulutusjakso(form: Koulutusjakso) {
  const path = 'erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot'
  return await axios.post<Koulutusjakso>(path, form)
}

export async function putKoulutusjakso(form: Koulutusjakso) {
  const path = `erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot/${form.id}`
  return await axios.put<Koulutusjakso>(path, form)
}

export async function getKoulutusjakso(koulutusjaksoId: number | string) {
  const path = `erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot/${koulutusjaksoId}`
  return await axios.get<Koulutusjakso>(path)
}

export async function getKoulutusjaksot() {
  const path = 'erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot'
  return await axios.get<Koulutusjakso[]>(path)
}

export async function getTeoriakoulutukset() {
  const path = 'erikoistuva-laakari/teoriakoulutukset'
  return await axios.get<Teoriakoulutukset>(path)
}

export async function getTeoriakoulutus(teoriakoulutusId: number | string) {
  const path = `erikoistuva-laakari/teoriakoulutukset/${teoriakoulutusId}`
  return await axios.get<Teoriakoulutus>(path)
}

export async function postTeoriakoulutus(form: Teoriakoulutus, todistusFiles: File[]) {
  const formData = wrapToFormData(form)
  todistusFiles.forEach((todistusFile) => {
    formData.append('todistusFiles', todistusFile)
  })
  return await axios.post<Teoriakoulutus>('erikoistuva-laakari/teoriakoulutukset', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function putTeoriakoulutus(
  form: Teoriakoulutus,
  todistusFiles: File[],
  deletedAsiakirjaIds: number[]
) {
  const formData = wrapToFormData(form)
  todistusFiles.forEach((todistusFile) => {
    formData.append('todistusFiles', todistusFile)
  })
  formData.append('deletedAsiakirjaIdsJson', JSON.stringify(deletedAsiakirjaIds))
  return await axios.put<Teoriakoulutus>(
    `erikoistuva-laakari/teoriakoulutukset/${form.id}`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }
  )
}

export async function getSeurantajaksot() {
  const path = 'erikoistuva-laakari/seurantakeskustelut/seurantajaksot'
  return await axios.get<Seurantajakso[]>(path)
}

export async function getSeurantajakso(seurantajaksoId: string) {
  const path = `erikoistuva-laakari/seurantakeskustelut/seurantajakso/${seurantajaksoId}`
  return await axios.get<Seurantajakso>(path)
}

export async function getSeurantajaksonTiedot(
  alkupvm: string,
  loppupvm: string,
  koulutusjaksot: number[]
) {
  const path = `erikoistuva-laakari/seurantakeskustelut/seurantajaksontiedot?alkamispaiva=${alkupvm}&paattymispaiva=${loppupvm}&koulutusjaksot=${koulutusjaksot}`
  return await axios.get<SeurantajaksonTiedot>(path)
}

export async function postSeurantajakso(form: Seurantajakso) {
  const path = 'erikoistuva-laakari/seurantakeskustelut/seurantajakso'
  return await axios.post<Seurantajakso>(path, form)
}

export async function putSeurantajakso(form: Seurantajakso) {
  const path = `erikoistuva-laakari/seurantakeskustelut/seurantajakso/${form.id}`
  return await axios.put<Seurantajakso>(path, form)
}

export async function deleteSeurantajakso(id?: number) {
  const path = `erikoistuva-laakari/seurantakeskustelut/seurantajakso/${id}`
  return await axios.delete(path)
}

export async function postLahikouluttaja(form: Kayttaja) {
  const path = 'erikoistuva-laakari/lahikouluttajat'
  return await axios.post<Kayttaja>(path, form)
}

export async function getPaivakirjamerkinnatRajaimet() {
  const path = 'erikoistuva-laakari/paivakirjamerkinnat-rajaimet'
  return await axios.get<PaivakirjamerkintaRajaimet>(path)
}

export async function getPaivakirjamerkintaLomake() {
  const path = 'erikoistuva-laakari/paivakirjamerkinta-lomake'
  return await axios.get<PaivakirjamerkintaLomake>(path)
}

export async function postPaivakirjamerkinta(form: Paivakirjamerkinta) {
  const path = 'erikoistuva-laakari/paivakirjamerkinnat'
  return await axios.post<Paivakirjamerkinta>(path, form)
}

export async function putPaivakirjamerkinta(form: Paivakirjamerkinta) {
  const path = `erikoistuva-laakari/paivakirjamerkinnat/${form.id}`
  return await axios.put<Paivakirjamerkinta>(path, form)
}

export async function getPaivakirjamerkinta(paivakirjamerkintaId: string) {
  const path = `erikoistuva-laakari/paivakirjamerkinnat/${paivakirjamerkintaId}`
  return await axios.get<Paivakirjamerkinta>(path)
}

export async function deletePaivittainenMerkinta(paivakirjamerkintaId?: number) {
  const path = `erikoistuva-laakari/paivakirjamerkinnat/${paivakirjamerkintaId}`
  return await axios.delete(path)
}

export async function getPaivittaisetMerkinnat(params: {
  page?: number
  size?: number
  sort?: string
  'aihekategoriaId.equals'?: number
  'paivamaara.greaterThanOrEqual'?: string
  'paivamaara.lessThanOrEqual'?: string
}) {
  return await axios.get<Page<Paivakirjamerkinta>>('erikoistuva-laakari/paivakirjamerkinnat', {
    params: {
      ...params
    }
  })
}

export async function putKaytonAloitusLomake(form: KaytonAloitusModel) {
  const path = `erikoistuva-laakari/kaytonaloitus`
  return await axios.put<KaytonAloitusModel>(path, form)
}

export async function getErikoistumisenEdistyminen() {
  const path = 'erikoistuva-laakari/etusivu/erikoistumisen-edistyminen'
  return await axios.get<ErikoistumisenEdistyminen>(path)
}

export async function patchOpintooikeusKaytossa(opintooikeusId: number) {
  const path = `erikoistuva-laakari/opinto-oikeus/${opintooikeusId}`
  return await axios.patch(path)
}

export async function getAvoimetAsiat() {
  const path = 'erikoistuva-laakari/etusivu/avoimet-asiat'
  return await axios.get<AvoinAsia[]>(path)
}

export async function getTerveyskeskuskoulutusjakso() {
  const path = 'erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakso'
  return await axios.get<TerveyskeskuskoulutusjaksonHyvaksyminen>(path)
}

export async function postTerveyskeskuskoulutusjakso(
  form: TerveyskeskuskoulutusjaksonHyvaksyntaForm
) {
  const path = 'erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta'
  return await axios.post<TerveyskeskuskoulutusjaksonHyvaksyminen>(path, form)
}

export async function getValmistumispyynto() {
  const path = 'erikoistuva-laakari/valmistumispyynto'
  return await axios.get<Valmistumispyynto>(path)
}

export async function getValmistumispyyntoSuoritustenTila() {
  const path = 'erikoistuva-laakari/valmistumispyynto-suoritusten-tila'
  return await axios.get<ValmistumispyyntoSuoritustenTila>(path)
}

export async function postValmistumispyynto(form: ValmistumispyyntoLomakeErikoistuja) {
  const formData = wrapToFormData(form)
  if (form.laillistamistodistus) {
    formData.append('laillistamistodistus', form.laillistamistodistus)
  }
  const path = 'erikoistuva-laakari/valmistumispyynto'
  return await axios.post<Valmistumispyynto>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function putValmistumispyynto(form: ValmistumispyyntoLomakeErikoistuja) {
  const formData = wrapToFormData(form)
  if (form.laillistamistodistus) {
    formData.append('laillistamistodistus', form.laillistamistodistus)
  }
  const path = 'erikoistuva-laakari/valmistumispyynto'
  return await axios.put<Valmistumispyynto>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function postMuokkausoikeudet(muokkausoikeudet: boolean) {
  const path = 'erikoistuva-laakari/muokkausoikeudet'
  const formData = wrapToFormData({ muokkausoikeudet: muokkausoikeudet })
  return await axios.post(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export async function putSuoritusarviointi(formData: FormData) {
  const path = 'erikoistuva-laakari/suoritusarvioinnit'
  return await axios.put<Suoritusarviointi>(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
