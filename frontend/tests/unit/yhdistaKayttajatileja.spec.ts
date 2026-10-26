import { createLocalVue, shallowMount } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'
import VueI18n from 'vue-i18n'

import { yhdistaKayttajatilit } from '@/api/kayttajahallinta'
import { toastFail, toastSuccess } from '@/utils/toast'
import YhdistaKayttajatileja from '@/views/kayttajahallinta/yhdista-kayttajatileja.vue'

jest.mock('@/api/kayttajahallinta', () => ({ yhdistaKayttajatilit: jest.fn() }))
jest.mock('@/utils/toast', () => ({ toastFail: jest.fn(), toastSuccess: jest.fn() }))
jest.mock('@/store', () => ({ dispatch: jest.fn() }))

const fi = jest.requireActual('@/locales/fi.json')
const localVue = createLocalVue()
localVue.use(BootstrapVue)
localVue.use(VueI18n)

const mergeAccounts = yhdistaKayttajatilit as jest.MockedFunction<typeof yhdistaKayttajatilit>

function render() {
  const i18n = new VueI18n({ locale: 'fi', messages: { fi } })
  return shallowMount(YhdistaKayttajatileja, {
    localVue,
    mocks: { $router: { push: jest.fn() } },
    mixins: [
      {
        beforeCreate() {
          this.$t = i18n.t.bind(i18n)
        }
      }
    ],
    data: () => ({
      form: {
        erikoistujaKayttajaId: 101,
        kouluttajaKayttajaId: 102,
        yhteinenSahkoposti: 'trainer@example.com',
        yhteinenSahkopostiUudelleen: 'trainer@example.com',
        formValid: true
      }
    })
  })
}

describe('Account merge feedback', () => {
  beforeEach(() => jest.clearAllMocks())

  it('shows the unsupported-role reason returned by the backend and stays on the form', async () => {
    mergeAccounts.mockRejectedValueOnce({
      response: {
        status: 400,
        data: {
          message: 'error.invalidauthorities',
          detail: 'Valituilla käyttäjillä on roolit joiden yhdistystä ei tällä hetkellä tueta',
          params: 'user'
        }
      }
    })
    const wrapper = render()

    await (wrapper.vm as any).yhdistaKayttajatilit()

    expect(mergeAccounts).toHaveBeenCalledWith({
      ensimmainenKayttajaId: 101,
      toinenKayttajaId: 102,
      yhteinenSahkoposti: 'trainer@example.com'
    })
    expect(toastFail).toHaveBeenCalledWith(
      wrapper.vm,
      expect.stringContaining(
        'Valituilla käyttäjillä on roolit joiden yhdistystä ei tällä hetkellä tueta'
      )
    )
    expect(toastFail).not.toHaveBeenCalledWith(
      wrapper.vm,
      expect.stringContaining('error.invalidauthorities')
    )
    expect(toastSuccess).not.toHaveBeenCalled()
    expect(wrapper.vm.$router.push).not.toHaveBeenCalled()
    wrapper.destroy()
  })

  it('also displays other localized backend validation reasons', async () => {
    mergeAccounts.mockRejectedValueOnce({
      response: {
        status: 400,
        data: { message: 'error.dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja' }
      }
    })
    const wrapper = render()

    await (wrapper.vm as any).yhdistaKayttajatilit()

    expect(toastFail).toHaveBeenCalledWith(
      wrapper.vm,
      expect.stringContaining('samalla sähköpostilla löytyy jo toinen käyttäjä')
    )
    wrapper.destroy()
  })

  it.each([
    ['network failure', new Error('Network Error')],
    ['missing reason', { response: { status: 400, data: {} } }],
    ['server failure', { response: { status: 500, data: { detail: 'Internal exception' } } }]
  ])('keeps the general fallback for a %s', async (_, error) => {
    mergeAccounts.mockRejectedValueOnce(error)
    const wrapper = render()

    await (wrapper.vm as any).yhdistaKayttajatilit()

    expect(toastFail).toHaveBeenCalledWith(
      wrapper.vm,
      fi['kayttajatilien-yhdistaminen-epaonnistui']
    )
    expect(wrapper.vm.$router.push).not.toHaveBeenCalled()
    wrapper.destroy()
  })

  it('shows success and returns to user management after a successful merge', async () => {
    mergeAccounts.mockResolvedValueOnce({} as Awaited<ReturnType<typeof yhdistaKayttajatilit>>)
    const wrapper = render()

    await (wrapper.vm as any).yhdistaKayttajatilit()

    expect(toastSuccess).toHaveBeenCalledWith(
      wrapper.vm,
      fi['kayttajatilien-yhdistaminen-onnistui']
    )
    expect(toastFail).not.toHaveBeenCalled()
    expect(wrapper.vm.$router.push).toHaveBeenCalledWith({ name: 'kayttajahallinta' })
    wrapper.destroy()
  })
})
