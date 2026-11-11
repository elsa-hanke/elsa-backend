// ELSAINSI-73: Pitää välilehden välimuistissa olevan aktiivisen kontekstin (aktiivinen rooli + käytössä oleva
// opinto-oikeus) synkronoituna palvelimen kanssa.
//
// Palvelimella aktiivinen rooli ja käytössä oleva opinto-oikeus ovat käyttäjäkohtaisia, eivät välilehtikohtaisia.
// Kun ne vaihtuvat toisessa välilehdessä / ikkunassa (tai taustalla opintotietojen tuonnissa), auki jäänyt
// välilehti luottaisi muuten vanhentuneeseen tietoon: reittitarkistus päästäisi läpi ja palvelin hylkäisi
// pyynnön ("Opinto-oikeutta ei löydy") tai pahimmillaan tallentaisi tiedot väärälle opinto-oikeudelle.
//
// Tarkistus tehdään:
//  1. jokaisen sovelluksen sisäisen navigoinnin yhteydessä (ennen kuin reitin komponentti hakee tietoja),
//  2. kun välilehti/ikkuna saa takaisin näkyvyyden tai fokuksen,
//  3. kun toinen välilehti ilmoittaa vaihtaneensa kontekstia (BroadcastChannel / storage-event),
//  4. ennen jokaista muokkaavaa pyyntöä (POST/PUT/PATCH/DELETE) ja epäonnistuneen pyynnön jälkeen.
// Jos konteksti on vaihtunut, sivu ladataan kokonaan uudelleen - samoin kuin roolia vaihtava välilehti tekee.

import axios, { InternalAxiosRequestConfig } from 'axios'
import VueRouter from 'vue-router'

import { getKayttaja } from '@/api'
import { getErikoistuvaLaakari } from '@/api/erikoistuva'
import store from '@/store'
import {
  isLocalActiveContextChangeInProgress,
  onActiveContextChanged
} from '@/utils/active-context-channel'
import { ELSA_ROLE } from '@/utils/roles'

const RELOAD_GUARD_KEY = 'elsa-active-context-reload'
const RELOAD_GUARD_MS = 5000

const MUTATING_METHODS = ['post', 'put', 'patch', 'delete']

// Pyynnöt, jotka eivät saa käynnistää tarkistusta (tarkistuksen omat pyynnöt, kontekstin vaihto, uloskirjaus).
const EXCLUDED_URL_PATTERNS = [
  /(^|\/)kayttaja(\?|$)/,
  /(^|\/)kayttaja-impersonated(\?|$)/,
  /(^|\/)erikoistuva-laakari(\?|$)/,
  /(^|\/)erikoistuva-laakari\/opinto-oikeus\//,
  /(^|\/)vaihda-rooli(\?|$)/,
  /logout/,
  /slo-kaytossa/
]

let pendingCheck: Promise<boolean> | null = null
let reloading = false

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function contextKey(account: any): string | null {
  if (!account) {
    return null
  }
  return [
    account.activeAuthority ?? '',
    account.erikoistuvaLaakari?.opintooikeusKaytossaId ?? '',
    account.impersonated ? '1' : '0'
  ].join('|')
}

// Haetaan tiedot täsmälleen samalla tavalla kuin auth/authorize, jotta avaimet ovat vertailukelpoisia
// (muuten vertailu voisi aina erota ja aiheuttaa turhia uudelleenlatauksia).
async function fetchServerContextKey(): Promise<string | null> {
  const { data } = await getKayttaja()
  if (
    data.authorities?.includes(ELSA_ROLE.ErikoistuvaLaakari) ||
    data.authorities?.includes(ELSA_ROLE.YEKKoulutettava)
  ) {
    data.erikoistuvaLaakari = (await getErikoistuvaLaakari()).data
  }
  return contextKey(data)
}

function shouldSkipCheck() {
  return reloading || isLocalActiveContextChangeInProgress() || !store.getters['auth/isLoggedIn']
}

/** Palauttaa true, jos palvelimen aktiivinen konteksti poikkeaa tämän välilehden välimuistista. */
export function isActiveContextStale(): Promise<boolean> {
  if (shouldSkipCheck()) {
    return Promise.resolve(false)
  }
  if (!pendingCheck) {
    pendingCheck = (async () => {
      try {
        const local = contextKey(store.getters['auth/account'])
        const server = await fetchServerContextKey()
        return local !== null && server !== null && local !== server
      } catch (err) {
        // Verkko- tai 401-virheet hoidetaan muualla (axios-interceptor) - ei estetä käyttöä tarkistuksen takia.
        return false
      } finally {
        pendingCheck = null
      }
    })()
  }
  return pendingCheck
}

function reloadAllowed(): boolean {
  try {
    const last = Number(sessionStorage.getItem(RELOAD_GUARD_KEY) || 0)
    if (Date.now() - last < RELOAD_GUARD_MS) {
      // Suoja uudelleenlatausluuppia vastaan.
      return false
    }
    sessionStorage.setItem(RELOAD_GUARD_KEY, String(Date.now()))
  } catch (err) {
    // sessionStorage ei käytettävissä - sallitaan
  }
  return true
}

/**
 * Lataa sivun uudelleen (valinnaisesti annettuun osoitteeseen). Palauttaa true, jos uudelleenlataus käynnistettiin.
 * Jos uudelleenlataus estetään luuppisuojan takia, päivitetään vähintään välimuistissa oleva käyttäjätieto,
 * jolloin reittitarkistukset (role-specific-route) toimivat ajantasaisella tiedolla.
 */
async function resync(href?: string): Promise<boolean> {
  if (reloadAllowed()) {
    reloading = true
    if (href) {
      window.location.assign(href)
    } else {
      window.location.reload()
    }
    return true
  }
  await store.dispatch('auth/authorize')
  return false
}

async function checkAndResync() {
  if (document.visibilityState === 'hidden') {
    // Taustalla olevaa välilehteä ei ladata uudelleen (tallentamattomat tiedot) -
    // tarkistus tehdään, kun välilehti tulee näkyviin tai käyttäjä navigoi / tallentaa.
    return
  }
  if (await isActiveContextStale()) {
    await resync()
  }
}

function isExcludedUrl(url?: string) {
  return !url || EXCLUDED_URL_PATTERNS.some((pattern) => pattern.test(url))
}

function neverSettle<T>(): Promise<T> {
  // Sivu on latautumassa uudelleen - ei päästetä pyyntöä/virhettä eteenpäin (ei turhia virheilmoituksia).
  return new Promise<T>(() => undefined)
}

export function installActiveContextSync(router: VueRouter) {
  // 1. Sovelluksen sisäinen navigointi
  router.beforeEach(async (to, from, next) => {
    // Ensimmäinen navigointi: tila haetaan juuri nyt (auth/authorize) - ei tarvetta tarkistaa.
    if (from.matched.length === 0 || shouldSkipCheck()) {
      next()
      return
    }
    if (await isActiveContextStale()) {
      if (await resync(router.resolve(to.fullPath).href)) {
        next(false)
        return
      }
    }
    next()
  })

  // 2. Välilehti / ikkuna aktivoituu uudelleen
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      checkAndResync()
    }
  })
  window.addEventListener('focus', () => {
    checkAndResync()
  })

  // 3. Toinen välilehti vaihtoi kontekstia
  onActiveContextChanged(() => {
    checkAndResync()
  })

  // 4a. Ennen muokkaavaa pyyntöä: ei koskaan tallenneta vanhentuneella kontekstilla
  axios.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
    const method = (config.method ?? 'get').toLowerCase()
    if (!MUTATING_METHODS.includes(method) || isExcludedUrl(config.url) || shouldSkipCheck()) {
      return config
    }
    if (await isActiveContextStale()) {
      if (await resync()) {
        return neverSettle<InternalAxiosRequestConfig>()
      }
      throw new Error('Aktiivinen rooli tai opinto-oikeus on vaihtunut. Tallennusta ei tehty.')
    }
    return config
  })

  // 4b. Epäonnistuneen pyynnön jälkeen: syynä voi olla vanhentunut konteksti
  axios.interceptors.response.use(
    (response) => response,
    async (error) => {
      const status = error?.response?.status
      if (
        status &&
        status >= 400 &&
        status !== 401 &&
        status !== 403 &&
        !isExcludedUrl(error.config?.url) &&
        !shouldSkipCheck()
      ) {
        if ((await isActiveContextStale()) && (await resync())) {
          return neverSettle()
        }
      }
      return Promise.reject(error)
    }
  )
}
