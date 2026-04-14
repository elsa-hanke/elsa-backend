import axios from 'axios'

import store from '@/store'
import { OmatTiedotLomake, OmatTiedotLomakeErikoistuja } from '@/types'
import { setCookie, getCookie } from '@/utils/cookies'
import { wrapToFormData } from '@/utils/functions'

export const ELSA_API_LOCATION =
  process.env.NODE_ENV === 'production'
    ? `${window.location.protocol}//api.${window.location.hostname}${
        window.location.port ? ':' + window.location.port : ''
      }`
    : ''
axios.defaults.baseURL = `${ELSA_API_LOCATION}/api/`
axios.defaults.withCredentials = true
axios.interceptors.request.use((req) => {
  const hostname = window.location.hostname
  if (hostname !== 'testi.elsapalvelu.fi' && hostname !== 'kehitys.elsapalvelu.fi') {
    return req
  }
  let accessKey
  const queryString = window.location.search
  const urlParams = new URLSearchParams(queryString)
  accessKey = urlParams.get('accessKey')
  if (accessKey) {
    setCookie('ACCESS-KEY', accessKey, 60 * 60 * 24 * 3650)
  } else {
    accessKey = getCookie('ACCESS-KEY')
  }

  if (accessKey) {
    req.headers.common['X-Access-Key'] = accessKey
  }

  return req
})
axios.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    switch (error.response.status) {
      case 401:
      case 403:
        if (window.location.pathname !== '/kirjautuminen' && store.getters['auth/isLoggedIn']) {
          store.dispatch('auth/logout')
          window.location.href = '/kirjautuminen'
        }
        break
    }
    return Promise.reject(error)
  }
)

export async function getKayttaja() {
  return await axios.get('kayttaja')
}

export async function putKayttaja(form: OmatTiedotLomake) {
  return await axios.put('kayttaja', wrapToFormData(form), {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function putErikoistuvaLaakari(form: OmatTiedotLomakeErikoistuja) {
  return await axios.put('erikoistuva-laakari', wrapToFormData(form), {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function localLogout() {
  return await axios.get('local-logout')
}

export async function sloKaytossa() {
  return await axios.get('slo-kaytossa')
}

export async function vaihdaRooli(rooli: string) {
  return await axios.post('vaihda-rooli', wrapToFormData({ rooli: rooli }))
}
