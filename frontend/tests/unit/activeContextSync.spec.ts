/* eslint-disable no-unused-vars, @typescript-eslint/no-var-requires, @typescript-eslint/no-explicit-any */
// ELSAINSI-73: aktiivisen kontekstin (rooli + käytössä oleva opinto-oikeus) synkronointi välilehtien välillä.

jest.mock('@/api', () => ({ getKayttaja: jest.fn() }))
jest.mock('@/api/erikoistuva', () => ({ getErikoistuvaLaakari: jest.fn() }))
jest.mock('@/store', () => ({
  __esModule: true,
  default: {
    getters: { 'auth/isLoggedIn': true, 'auth/account': null },
    dispatch: jest.fn()
  }
}))

const EL = 'ROLE_ERIKOISTUVA_LAAKARI'
const YEK = 'ROLE_YEK_KOULUTETTAVA'
const KOULUTTAJA = 'ROLE_KOULUTTAJA'

type Account = {
  activeAuthority: string
  authorities: string[]
  impersonated?: boolean
  erikoistuvaLaakari?: { opintooikeusKaytossaId: number | null }
}

const flush = () => new Promise((resolve) => setTimeout(resolve, 0))

function account(
  activeAuthority: string,
  authorities: string[],
  opintooikeusKaytossaId?: number,
  impersonated = false
): Account {
  return {
    activeAuthority,
    authorities,
    impersonated,
    ...(opintooikeusKaytossaId !== undefined
      ? { erikoistuvaLaakari: { opintooikeusKaytossaId } }
      : {})
  }
}

function setup(local: Account | null, server: Account, options: { loggedIn?: boolean } = {}) {
  jest.resetModules()

  const api = require('@/api')
  const erikoistuvaApi = require('@/api/erikoistuva')
  const store = require('@/store').default
  const axios = require('axios').default
  const channel = require('@/utils/active-context-channel')

  store.getters['auth/isLoggedIn'] = options.loggedIn ?? true
  store.getters['auth/account'] = local
  store.dispatch.mockReset()

  // Palautetaan aina uusi olio (fetch mutatoi datan kuten auth/authorize)
  api.getKayttaja.mockImplementation(async () => ({
    data: {
      activeAuthority: server.activeAuthority,
      authorities: server.authorities,
      impersonated: server.impersonated ?? false
    }
  }))
  erikoistuvaApi.getErikoistuvaLaakari.mockImplementation(async () => ({
    data: { opintooikeusKaytossaId: server.erikoistuvaLaakari?.opintooikeusKaytossaId ?? null }
  }))

  const requestUse = jest.spyOn(axios.interceptors.request, 'use')
  const responseUse = jest.spyOn(axios.interceptors.response, 'use')

  const documentListeners: Record<string, () => void> = {}
  const windowListeners: Record<string, (...args: any[]) => void> = {}
  jest
    .spyOn(document, 'addEventListener')
    .mockImplementation((type: string, listener: any) => (documentListeners[type] = listener))
  jest
    .spyOn(window, 'addEventListener')
    .mockImplementation((type: string, listener: any) => (windowListeners[type] = listener))

  const router = {
    beforeEach: jest.fn(),
    resolve: jest.fn((path: string) => ({ href: path }))
  }

  const { installActiveContextSync, isActiveContextStale } = require('@/utils/active-context-sync')
  installActiveContextSync(router)

  const guard = router.beforeEach.mock.calls[0][0]
  const requestInterceptor = requestUse.mock.calls[0][0] as (...args: any[]) => Promise<any>
  const responseErrorInterceptor = responseUse.mock.calls[0][1] as (...args: any[]) => Promise<any>

  const navigate = async (from = { matched: [{}] }, to = { fullPath: '/tyoskentelyjaksot' }) => {
    const next = jest.fn()
    await guard(to, from, next)
    return next
  }

  return {
    api,
    erikoistuvaApi,
    store,
    channel,
    router,
    navigate,
    requestInterceptor,
    responseErrorInterceptor,
    documentListeners,
    windowListeners,
    isActiveContextStale
  }
}

describe('active-context-sync (ELSAINSI-73)', () => {
  let assign: jest.Mock
  let reload: jest.Mock

  beforeEach(() => {
    sessionStorage.clear()
    assign = jest.fn()
    reload = jest.fn()
    jest.spyOn(window, 'location', 'get').mockReturnValue({
      ...window.location,
      assign,
      reload
    } as any)
    jest.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible')
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  describe('yhden roolin käyttäjä, jolla useita välilehtiä (ei regressiota)', () => {
    it('kouluttaja: navigointi jatkuu, ei uudelleenlatausta eikä erikoistujan tietojen hakua', async () => {
      const kouluttaja = account(KOULUTTAJA, [KOULUTTAJA])
      const { navigate, erikoistuvaApi } = setup(kouluttaja, kouluttaja)

      const next = await navigate()

      expect(next).toHaveBeenCalledWith()
      expect(assign).not.toHaveBeenCalled()
      expect(reload).not.toHaveBeenCalled()
      expect(erikoistuvaApi.getErikoistuvaLaakari).not.toHaveBeenCalled()
    })

    it('erikoistuva yhdellä opinto-oikeudella: navigointi jatkuu ilman uudelleenlatausta', async () => {
      const el = account(EL, [EL], 1)
      const { navigate } = setup(el, el)

      const next = await navigate()

      expect(next).toHaveBeenCalledWith()
      expect(assign).not.toHaveBeenCalled()
    })

    it('välilehden fokus ei lataa sivua uudelleen, kun konteksti ei ole muuttunut', async () => {
      const el = account(EL, [EL], 1)
      const { windowListeners, documentListeners } = setup(el, el)

      windowListeners.focus()
      documentListeners.visibilitychange()
      await flush()

      expect(reload).not.toHaveBeenCalled()
    })

    it('tallennus (POST) päästetään läpi muuttumattomana, kun konteksti ei ole muuttunut', async () => {
      const el = account(EL, [EL], 1)
      const { requestInterceptor } = setup(el, el)
      const config = { method: 'post', url: 'erikoistuva-laakari/tyoskentelyjaksot' }

      await expect(requestInterceptor(config)).resolves.toBe(config)
      expect(reload).not.toHaveBeenCalled()
    })

    it('virheellinen pyyntö palauttaa alkuperäisen virheen, kun konteksti ei ole muuttunut', async () => {
      const el = account(EL, [EL], 1)
      const { responseErrorInterceptor } = setup(el, el)
      const error = { response: { status: 500 }, config: { url: 'erikoistuva-laakari/x' } }

      await expect(responseErrorInterceptor(error)).rejects.toBe(error)
      expect(reload).not.toHaveBeenCalled()
    })

    it('tarkistuksen verkkovirhe ei estä navigointia', async () => {
      const el = account(EL, [EL], 1)
      const { navigate, api } = setup(el, el)
      api.getKayttaja.mockRejectedValue(new Error('Network Error'))

      const next = await navigate()

      expect(next).toHaveBeenCalledWith()
      expect(assign).not.toHaveBeenCalled()
    })
  })

  describe('tarkistusta ei tehdä turhaan', () => {
    it('ensimmäisellä navigoinnilla (sivun lataus) ei tehdä tarkistusta', async () => {
      const el = account(EL, [EL], 1)
      const { navigate, api } = setup(el, el)

      const next = await navigate({ matched: [] })

      expect(next).toHaveBeenCalledWith()
      expect(api.getKayttaja).not.toHaveBeenCalled()
    })

    it('kirjautumaton käyttäjä: ei tarkistusta', async () => {
      const el = account(EL, [EL], 1)
      const { navigate, api } = setup(null, el, { loggedIn: false })

      await navigate()

      expect(api.getKayttaja).not.toHaveBeenCalled()
    })

    it('GET-pyyntöjä ja kontekstia vaihtavia pyyntöjä ei tarkisteta etukäteen', async () => {
      const el = account(EL, [EL], 1)
      const { requestInterceptor, api } = setup(el, el)

      await requestInterceptor({ method: 'get', url: 'erikoistuva-laakari/tyoskentelyjaksot' })
      await requestInterceptor({ method: 'post', url: 'vaihda-rooli' })
      await requestInterceptor({ method: 'patch', url: 'erikoistuva-laakari/opinto-oikeus/2' })

      expect(api.getKayttaja).not.toHaveBeenCalled()
    })

    it('piilossa olevaa välilehteä ei ladata uudelleen fokuksen/ilmoituksen perusteella', async () => {
      jest.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')
      const { windowListeners, api } = setup(account(YEK, [EL, YEK], 2), account(EL, [EL, YEK], 1))

      windowListeners.focus()
      windowListeners.storage({ key: 'elsa-active-context-changed' })
      await flush()

      expect(api.getKayttaja).not.toHaveBeenCalled()
      expect(reload).not.toHaveBeenCalled()
    })

    it('samanaikaiset tarkistukset yhdistetään yhdeksi pyynnöksi', async () => {
      const el = account(EL, [EL], 1)
      const { isActiveContextStale, api } = setup(el, el)

      await Promise.all([isActiveContextStale(), isActiveContextStale(), isActiveContextStale()])

      expect(api.getKayttaja).toHaveBeenCalledTimes(1)
    })
  })

  describe('vanhentunut konteksti (toinen välilehti vaihtoi roolia / opinto-oikeutta)', () => {
    it('rooli vaihtunut YEK -> EL: navigointi keskeytetään ja kohdesivu ladataan kokonaan', async () => {
      const { navigate } = setup(account(YEK, [EL, YEK], 2), account(EL, [EL, YEK], 1))

      const next = await navigate(undefined, { fullPath: '/yektyoskentelyjaksot' })

      expect(next).toHaveBeenCalledWith(false)
      expect(assign).toHaveBeenCalledWith('/yektyoskentelyjaksot')
    })

    it('saman roolin opinto-oikeus vaihtunut: sivu ladataan uudelleen', async () => {
      const { navigate } = setup(account(EL, [EL], 1), account(EL, [EL], 3))

      const next = await navigate()

      expect(next).toHaveBeenCalledWith(false)
      expect(assign).toHaveBeenCalled()
    })

    it('käyttäjän tietojen katselu (impersonointi) aloitettu toisessa välilehdessä: ladataan uudelleen', async () => {
      const { navigate } = setup(
        account(KOULUTTAJA, [KOULUTTAJA]),
        account(KOULUTTAJA, [KOULUTTAJA], undefined, true)
      )

      const next = await navigate()

      expect(next).toHaveBeenCalledWith(false)
    })

    it('välilehti saa fokuksen: sivu ladataan uudelleen', async () => {
      const { windowListeners } = setup(account(YEK, [EL, YEK], 2), account(EL, [EL, YEK], 1))

      windowListeners.focus()
      await flush()

      expect(reload).toHaveBeenCalledTimes(1)
    })

    it('toisen välilehden ilmoitus (storage-event): sivu ladataan uudelleen', async () => {
      const { windowListeners } = setup(account(YEK, [EL, YEK], 2), account(EL, [EL, YEK], 1))

      windowListeners.storage({ key: 'elsa-active-context-changed' })
      await flush()

      expect(reload).toHaveBeenCalledTimes(1)
    })

    it('tallennusta (POST) ei lähetetä vanhentuneella kontekstilla', async () => {
      const { requestInterceptor } = setup(account(YEK, [EL, YEK], 2), account(EL, [EL, YEK], 1))
      let settled = false

      requestInterceptor({ method: 'post', url: 'yek-koulutettava/tyoskentelyjaksot' }).then(
        () => (settled = true),
        () => (settled = true)
      )
      await flush()

      expect(reload).toHaveBeenCalledTimes(1)
      expect(settled).toBe(false)
    })

    it('epäonnistunut pyyntö vanhentuneella kontekstilla: ladataan uudelleen ilman virheilmoitusta', async () => {
      const { responseErrorInterceptor } = setup(
        account(YEK, [EL, YEK], 2),
        account(EL, [EL, YEK], 1)
      )
      let settled = false

      responseErrorInterceptor({
        response: { status: 500 },
        config: { url: 'yek-koulutettava/tyoskentelyjaksot-taulukko' }
      }).then(
        () => (settled = true),
        () => (settled = true)
      )
      await flush()

      expect(reload).toHaveBeenCalledTimes(1)
      expect(settled).toBe(false)
    })

    it('401/403 käsitellään edelleen olemassa olevalla uloskirjauslogiikalla', async () => {
      const { responseErrorInterceptor, api } = setup(
        account(YEK, [EL, YEK], 2),
        account(EL, [EL, YEK], 1)
      )
      const error = { response: { status: 401 }, config: { url: 'yek-koulutettava/x' } }

      await expect(responseErrorInterceptor(error)).rejects.toBe(error)
      expect(api.getKayttaja).not.toHaveBeenCalled()
    })

    it('uudelleenlatausluuppisuoja: toinen lataus 5 s sisällä korvataan käyttäjätietojen päivityksellä', async () => {
      const first = setup(account(YEK, [EL, YEK], 2), account(EL, [EL, YEK], 1))
      await first.navigate()
      expect(assign).toHaveBeenCalledTimes(1)

      // Sivu latautui, mutta tila on (hypoteettisesti) edelleen eri - ei uutta latausta
      const second = setup(account(YEK, [EL, YEK], 2), account(EL, [EL, YEK], 1))
      const next = await second.navigate()

      expect(assign).toHaveBeenCalledTimes(1)
      expect(second.store.dispatch).toHaveBeenCalledWith('auth/authorize')
      expect(next).toHaveBeenCalledWith()
    })
  })

  describe('kontekstin vaihto tässä välilehdessä', () => {
    it('vaihdon aikana tarkistuksia ei tehdä (välilehti latautuu itse uudelleen)', async () => {
      const { navigate, channel, api } = setup(
        account(YEK, [EL, YEK], 2),
        account(EL, [EL, YEK], 1)
      )

      await channel.runActiveContextChange(async () => 'ok')
      const next = await navigate()

      expect(next).toHaveBeenCalledWith()
      expect(api.getKayttaja).not.toHaveBeenCalled()
    })

    it('epäonnistunut vaihto palauttaa tarkistukset käyttöön', async () => {
      const { navigate, channel, api } = setup(
        account(YEK, [EL, YEK], 2),
        account(EL, [EL, YEK], 1)
      )
      const error = new Error('400')

      await expect(channel.runActiveContextChange(async () => Promise.reject(error))).rejects.toBe(
        error
      )
      await navigate()

      expect(api.getKayttaja).toHaveBeenCalled()
    })
  })
})
