import '@/plugins/registerComponentHooks'

import { createLocalVue, mount } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'
import Vuelidate from 'vuelidate'

import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'

jest.mock('@/api/erikoistuva', () => ({ postLahikouluttaja: jest.fn() }))
jest.mock('@/store', () => ({ dispatch: jest.fn() }))

const localVue = createLocalVue()
localVue.use(BootstrapVue)
localVue.use(Vuelidate)

const trainer = { id: 101, nimi: 'Test Trainer', kayttajaUserId: 'trainer' }
const supervisor = { id: 102, nimi: 'Test Supervisor', kayttajaUserId: 'supervisor' }

const renderSelectors = (initialTrainer: typeof trainer | null = null) =>
  mount(KoulutuspaikanArvioijat, {
    localVue,
    propsData: {
      lahikouluttaja: initialTrainer,
      lahiesimies: null,
      kouluttajat: [trainer, supervisor],
      allowDuplicates: true
    },
    mocks: { $t: (key: string) => key },
    stubs: {
      ElsaFormGroup: { template: '<div><slot uid="assessor" /></div>' },
      KouluttajaForm: true,
      'font-awesome-icon': true,
      'font-awesome-layers': true
    }
  })

describe('Saved trial-period assessors', () => {
  it.each<[string, typeof trainer]>([
    ['different people', supervisor],
    ['the same person in both roles', trainer]
  ])('shows a draft loaded after mounting with %s', async (_, savedSupervisor) => {
    const wrapper = renderSelectors()
    expect(wrapper.findAll('.multiselect__single')).toHaveLength(0)

    await wrapper.setProps({ lahikouluttaja: trainer, lahiesimies: savedSupervisor })

    expect(wrapper.findAll('.multiselect__single').wrappers.map((w) => w.text())).toEqual([
      trainer.nimi,
      savedSupervisor.nimi
    ])
    expect(wrapper.emitted('lahikouluttajaSelect')).toBeUndefined()
    expect(wrapper.emitted('lahiesimiesSelect')).toBeUndefined()
    expect((wrapper.vm as any).validateForm()).toBe(true)
    wrapper.destroy()
  })

  it('shows values available at mount and replaces or clears them when a different draft is loaded', async () => {
    const wrapper = renderSelectors(trainer)
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.multiselect__single').text()).toBe(trainer.nimi)

    await wrapper.setProps({ lahikouluttaja: supervisor, lahiesimies: trainer })
    expect(wrapper.findAll('.multiselect__single').wrappers.map((w) => w.text())).toEqual([
      supervisor.nimi,
      trainer.nimi
    ])

    await wrapper.setProps({ lahikouluttaja: null, lahiesimies: null })
    expect(wrapper.findAll('.multiselect__single')).toHaveLength(0)
    expect((wrapper.vm as any).validateForm()).toBe(false)
    wrapper.destroy()
  })

  it('passes user changes and cleared selections back to the parent form', async () => {
    const wrapper = renderSelectors(trainer)
    await wrapper.vm.$nextTick()
    const selectors = wrapper.findAllComponents(ElsaFormMultiselect)
    selectors.at(1).vm.$emit('input', supervisor)
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('lahiesimiesSelect')).toEqual([[supervisor]])

    await selectors.at(0).find('.clear-button').trigger('click')
    expect(wrapper.emitted('lahikouluttajaSelect')).toEqual([[null]])
    expect(selectors.at(0).find('.multiselect__single').exists()).toBe(false)
    wrapper.destroy()
  })
})
