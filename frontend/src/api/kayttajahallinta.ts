import axios from 'axios'

import {
  ErikoistuvaLaakari,
  Page,
  KayttajahallintaKayttajaWrapper,
  ErikoistuvaLaakariLomake,
  UusiErikoistuvaLaakari,
  KayttajahallintaKayttajaListItem,
  KayttajahallintaRajaimet,
  KayttajahallintaUpdateKayttaja,
  KayttajahallintaVastuuhenkilonTehtavatLomake,
  KayttajahallintaNewKayttaja,
  Yliopisto,
  OpintoopasSimple,
  Kayttaja,
  KayttajahallintaYhdistaKayttajatilejaListItem,
  KayttajienYhdistaminenDTO
} from '@/types'
import { resolveRolePath } from '@/utils/kayttajahallintaRolePathResolver'

export async function getErikoistuvatLaakarit(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
  'erikoisalaId.equals'?: number
  'useaOpintooikeus.equals'?: boolean
}) {
  const path = `${resolveRolePath()}/erikoistuvat-laakarit`
  return await axios.get<Page<KayttajahallintaKayttajaListItem>>(path, {
    params: {
      ...params
    }
  })
}

export async function getVastuuhenkilot(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
  'erikoisalaId.equals'?: number
}) {
  const path = `${resolveRolePath()}/vastuuhenkilot`
  return await axios.get<Page<KayttajahallintaKayttajaListItem>>(path, {
    params: {
      ...params
    }
  })
}

export async function getKouluttajat(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
  'erikoisalaId.equals'?: number
  findAll?: boolean
}) {
  const path = `${resolveRolePath()}/kouluttajat`
  return await axios.get<Page<KayttajahallintaKayttajaListItem>>(path, {
    params: {
      ...params
    }
  })
}

export async function getVirkailijat(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
}) {
  const path = `${resolveRolePath()}/virkailijat`
  return await axios.get<Page<KayttajahallintaKayttajaListItem>>(path, {
    params: {
      ...params
    }
  })
}

export async function getPaakayttajat(params: {
  page?: number
  size?: number
  sort: string | null
  'nimi.contains'?: string
}) {
  const path = `${resolveRolePath()}/paakayttajat`
  return await axios.get<Page<KayttajahallintaKayttajaListItem>>(path, {
    params: {
      ...params
    }
  })
}

export async function getErikoistuvaLaakari(kayttajaId: number | string) {
  const path = `${resolveRolePath()}/erikoistuvat-laakarit/${kayttajaId}`
  return await axios.get<KayttajahallintaKayttajaWrapper>(path)
}

export async function getKayttaja(kayttajaId: number | string) {
  const path = `${resolveRolePath()}/kayttajat/${kayttajaId}`
  return await axios.get<KayttajahallintaKayttajaWrapper>(path)
}

export async function getKorvaavatKouluttajat(kayttajaId: number | string) {
  const path = `${resolveRolePath()}/kayttajat/${kayttajaId}/korvaavat`
  return await axios.get<Kayttaja[]>(path)
}

export async function getErikoistuvaLaakariLomake() {
  const path = `${resolveRolePath()}/erikoistuva-laakari-lomake`
  return await axios.get<ErikoistuvaLaakariLomake>(path)
}

export async function getOpintooppaat() {
  const path = `${resolveRolePath()}/opintooppaat`
  return await axios.get<OpintoopasSimple[]>(path)
}

export async function getYliopistot() {
  const path = `${resolveRolePath()}/yliopistot`
  return await axios.get<Yliopisto[]>(path)
}

export async function postErikoistuvaLaakari(form: UusiErikoistuvaLaakari) {
  const path = `${resolveRolePath()}/erikoistuvat-laakarit`
  return await axios.post<ErikoistuvaLaakari>(path, form)
}

export async function putErikoistuvaLaakariInvitation(erikoistuvaLaakariId: number | string) {
  const path = `${resolveRolePath()}/erikoistuvat-laakarit/${erikoistuvaLaakariId}/kutsu`
  return await axios.put(path)
}

export async function activateKayttaja(kayttajaId: number) {
  const path = `${resolveRolePath()}/kayttajat/${kayttajaId}/aktivoi`
  return await axios.patch(path)
}

export async function passivateKayttaja(kayttajaId: number, reassignedKayttajaId?: number) {
  const path = `${resolveRolePath()}/kayttajat/${kayttajaId}/passivoi`
  return await axios.patch(path, { reassignedKayttajaId: reassignedKayttajaId })
}

export async function deleteKayttaja(kayttajaId: number, reassignedKayttajaId?: number) {
  const path = `${resolveRolePath()}/kayttajat/${kayttajaId}`
  return await axios.delete(path, { data: { reassignedKayttajaId: reassignedKayttajaId } })
}

export async function getKayttajahallintaRajaimet() {
  const path = `${resolveRolePath()}/kayttajat/rajaimet`
  return await axios.get<KayttajahallintaRajaimet>(path)
}

export async function patchErikoistuvaLaakari(
  userId: string,
  form: KayttajahallintaUpdateKayttaja
) {
  const path = `${resolveRolePath()}/erikoistuvat-laakarit/${userId}`
  return await axios.patch(path, form)
}

export async function patchKouluttaja(kayttajaId: number, form: KayttajahallintaUpdateKayttaja) {
  const path = `${resolveRolePath()}/kouluttajat/${kayttajaId}`
  return await axios.patch(path, form)
}

export async function putKouluttajaInvitation(kayttajaId: number) {
  const path = `${resolveRolePath()}/kouluttajat/${kayttajaId}/kutsu`
  return await axios.put(path)
}

export async function patchVirkailija(kayttajaId: number, form: KayttajahallintaUpdateKayttaja) {
  const path = `${resolveRolePath()}/virkailijat/${kayttajaId}`
  return await axios.patch(path, form)
}

export async function patchPaakayttaja(kayttajaId: number, form: KayttajahallintaUpdateKayttaja) {
  const path = `${resolveRolePath()}/paakayttajat/${kayttajaId}`
  return await axios.patch(path, form)
}

export async function getVastuuhenkilonTehtavatForm(yliopistoId: number) {
  const path = `${resolveRolePath()}/vastuuhenkilon-tehtavat-lomake/${yliopistoId}`
  return await axios.get<KayttajahallintaVastuuhenkilonTehtavatLomake>(path)
}

export async function putVastuuhenkilo(kayttajaId: number, form: KayttajahallintaUpdateKayttaja) {
  const path = `${resolveRolePath()}/vastuuhenkilot/${kayttajaId}`
  return await axios.put<KayttajahallintaKayttajaWrapper>(path, form)
}

export async function postVastuuhenkilo(form: KayttajahallintaNewKayttaja) {
  const path = `${resolveRolePath()}/vastuuhenkilot`
  return await axios.post<KayttajahallintaKayttajaWrapper>(path, form)
}

export async function postVirkailija(form: KayttajahallintaNewKayttaja) {
  const path = `${resolveRolePath()}/virkailijat`
  return await axios.post<KayttajahallintaKayttajaWrapper>(path, form)
}

export async function postPaakayttaja(form: KayttajahallintaNewKayttaja) {
  const path = `${resolveRolePath()}/paakayttajat`
  return await axios.post<KayttajahallintaKayttajaWrapper>(path, form)
}

export async function getErikoistujatJaKouluttajat(params: {
  page?: number
  size?: number
  'nimi.contains'?: string
}) {
  const path = `${resolveRolePath()}/erikoistujat-ja-kouluttajat`
  return await axios.get<Page<KayttajahallintaYhdistaKayttajatilejaListItem>>(path, {
    params: {
      ...params
    }
  })
}

export async function yhdistaKayttajatilit(form: KayttajienYhdistaminenDTO) {
  const path = `${resolveRolePath()}/yhdista-kayttajatilit`
  return await axios.patch(path, form)
}
