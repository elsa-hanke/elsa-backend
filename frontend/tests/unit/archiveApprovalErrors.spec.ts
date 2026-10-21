import { createLocalVue, shallowMount } from '@vue/test-utils'
import Vue, { CreateElement, VueConstructor } from 'vue'
import VueI18n from 'vue-i18n'
import Vuelidate from 'vuelidate'

import { putValmistumispyyntoHyvaksynta } from '@/api/vastuuhenkilo'
import ConfirmationModal from '@/components/modal/confirmation-modal.vue'
import YekApproval from '@/views/valmistumispyynnot-yek/vastuuhenkilo/valmistumispyynnon-hyvaksynta-yek.vue'
import Approval from '@/views/valmistumispyynnot/vastuuhenkilo/valmistumispyynnon-hyvaksynta.vue'

jest.mock('@/api', () => ({ ELSA_API_LOCATION: '' }))
jest.mock('@/api/vastuuhenkilo', () => ({
  getValmistumispyyntoHyvaksynta: jest.fn(),
  putValmistumispyyntoHyvaksynta: jest.fn()
}))
jest.mock('@/store', () => ({
  getters: { 'auth/account': { email: 'test@example.org', phoneNumber: '0401234567' } }
}))
jest.mock('@/components/erikoistuva-details/erikoistuva-details.vue', () => ({
  name: 'ErikoistuvaDetails',
  render: (h: CreateElement) => h('div')
}))
jest.mock('@/views/opintosuoritukset/opintosuoritus-tab.vue', () => ({
  name: 'OpintosuoritusTab',
  render: (h: CreateElement) => h('div')
}))

const fi = jest.requireActual('@/locales/fi.json')
Vue.use(VueI18n)
const localVue = createLocalVue()
localVue.use(VueI18n)
localVue.use(Vuelidate)
const save = putValmistumispyyntoHyvaksynta as jest.Mock
const codes = [
  'ARCHIVE_DOCUMENT_MISSING',
  'ARCHIVE_DOCUMENT_EMPTY',
  'ARCHIVE_DOCUMENT_INVALID',
  'ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH',
  'ARCHIVE_DOCUMENT_PROCESSING_FAILED'
]

describe.each<[string, VueConstructor<Vue>]>([
  ['EL/EHL', Approval],
  ['YEK', YekApproval]
])('%s approval archive errors', (_, component) => {
  const render = () => {
    const toast = jest.fn()
    const replace = jest.fn()
    const i18n = new VueI18n({ locale: 'fi', fallbackLocale: 'fi', messages: { fi } })
    const wrapper = shallowMount(component, {
      localVue,
      i18n,
      mocks: {
        $route: { params: {} },
        $router: { replace },
        $bvToast: { toast },
        $date: (value: string) => value
      },
      stubs: {
        'b-breadcrumb': true,
        'b-container': true,
        'b-row': true,
        'b-col': true,
        'b-spinner': true
      },
      data: () => ({ valmistumispyynto: { id: 42 } })
    })
    return { wrapper, toast, replace, i18n }
  }

  beforeEach(() => jest.clearAllMocks())

  it.each(codes)('shows a localized message for %s through the approval handler', async (code) => {
    const { wrapper, toast, replace, i18n } = render()
    const status = codes.indexOf(code) < 3 ? 400 : 500
    save.mockRejectedValueOnce({
      response: {
        status,
        data: { errorKey: code, message: `error.${code}`, detail: 'private-file.pdf' }
      }
    })

    wrapper.findComponent(ConfirmationModal).vm.$emit('submit')
    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(i18n.te(`error.${code}`)).toBe(true)
    expect(save).toHaveBeenCalledTimes(1)
    expect(toast).toHaveBeenCalledTimes(1)
    const vnode = toast.mock.calls[0][0]
    const message = vnode.children.map((child: { text?: string }) => child.text ?? '').join('')
    const expectedKey = status === 500 ? 'ARCHIVE_DOCUMENT_PROCESSING_FAILED' : code
    expect(message).toContain(String(i18n.t(`error.${expectedKey}`)))
    expect(message).toContain(String(i18n.t('valmistumispyynto-hyvaksynta-lahetys-epaonnistui')))
    expect(message).not.toContain('ARCHIVE_DOCUMENT_')
    expect(message).not.toContain('private-file.pdf')
    expect(message).not.toContain('undefined')
    expect(replace).not.toHaveBeenCalled()
    expect(wrapper.emitted('skipRouteExitConfirm')).toBeUndefined()
    expect((wrapper.vm as any).sending).toBe(false)
    wrapper.destroy()
  })

  it('does not instruct the user to replace documents for a server-side missing-document error', async () => {
    const { wrapper, toast, i18n } = render()
    save.mockRejectedValueOnce({
      response: { status: 500, data: { errorKey: codes[0], message: `error.${codes[0]}` } }
    })

    await (wrapper.vm as any).onSend()

    const message = toast.mock.calls[0][0].children
      .map((child: { text?: string }) => child.text ?? '')
      .join('')
    expect(message).toContain(String(i18n.t('error.ARCHIVE_DOCUMENT_PROCESSING_FAILED')))
    expect(message).not.toContain(String(i18n.t('error.ARCHIVE_DOCUMENT_MISSING')))
    wrapper.destroy()
  })

  it('keeps the generic message on network failure and allows a successful retry', async () => {
    const { wrapper, toast, replace, i18n } = render()
    save.mockRejectedValueOnce(new Error('network failure'))
    await (wrapper.vm as any).onSend()
    const message = toast.mock.calls[0][0].children
      .map((child: { text?: string }) => child.text ?? '')
      .join('')
    expect(message).toBe(String(i18n.t('valmistumispyynto-hyvaksynta-lahetys-epaonnistui')))

    save.mockResolvedValueOnce({ data: { id: 42 } })
    await (wrapper.vm as any).onSend()

    expect(save).toHaveBeenCalledTimes(2)
    expect(replace).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('skipRouteExitConfirm')).toEqual([[true]])
    expect((wrapper.vm as any).sending).toBe(false)
    wrapper.destroy()
  })
})
