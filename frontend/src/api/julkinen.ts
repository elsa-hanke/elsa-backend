import axios from 'axios'

import { Ilmoitus } from '@/types'

export async function getSeuraavaPaivitys() {
  const path = '/julkinen/seuraava-paivitys'
  return await axios.get<string>(path)
}

export async function getIlmoitukset() {
  const path = '/julkinen/ilmoitukset'
  return await axios.get<Ilmoitus[]>(path)
}

export async function getIlmoitus(id: string) {
  const path = `/julkinen/ilmoitukset/${id}`
  return await axios.get<Ilmoitus>(path)
}
