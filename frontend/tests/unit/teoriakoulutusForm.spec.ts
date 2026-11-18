import { createLocalVue, mount } from '@vue/test-utils'
import axios from 'axios'
import { BootstrapVue } from 'bootstrap-vue'
import VueI18n from 'vue-i18n'

import TeoriakoulutusForm from '@/forms/teoriakoulutus-form.vue'

const fi = jest.requireActual('@/locales/fi.json')

jest.mock('axios')

const localVue = createLocalVue()
localVue.use(BootstrapVue)
localVue.use(VueI18n)

async function render() {
  ;(axios.get as jest.Mock).mockResolvedValue({ data: [] })
  const wrapper = mount(TeoriakoulutusForm, {
    localVue,
    i18n: new VueI18n({ locale: 'fi', messages: { fi } }),
    stubs: {
      ElsaFormDatepicker: {
        template: '<div />',
        methods: { validateForm: () => true }
      },
      AsiakirjatUpload: true,
      AsiakirjatContent: true,
      'font-awesome-icon': true
    }
  })
  await wrapper.vm.$nextTick()
  await wrapper.findAll('input[type="text"]').at(0).setValue('Koulutus')
  await wrapper.findAll('input[type="text"]').at(1).setValue('Helsinki')
  return wrapper
}

describe('Teoriakoulutus field length validation', () => {
  it.each([0, 1])('rejects 256 characters in text field %s and accepts 255', async (index) => {
    const wrapper = await render()
    const input = wrapper.findAll('input[type="text"]').at(index)
    const feedbackId = input.attributes('aria-describedby')
    const feedback = () => wrapper.find(`[id="${feedbackId}"]`)
    const tooLongMessage = fi['tieto-on-liian-pitka-enintaan-255-merkkia']

    await input.setValue('ä'.repeat(256))
    await input.trigger('blur')
    expect(input.classes()).toContain('is-invalid')
    expect(feedback().text()).toBe(tooLongMessage)
    expect((input.element as HTMLInputElement).value).toHaveLength(256)

    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()

    await input.setValue('ä'.repeat(255))
    expect(input.classes()).not.toContain('is-invalid')
    expect(feedback().text()).not.toBe(tooLongMessage)
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toHaveLength(1)
    wrapper.destroy()
  })

  it('shows the length error on submit without requiring blur', async () => {
    const wrapper = await render()
    const input = wrapper.findAll('input[type="text"]').at(0)
    await input.setValue('a'.repeat(256))
    await wrapper.find('form').trigger('submit')
    expect(input.classes()).toContain('is-invalid')
    expect(wrapper.find(`[id="${input.attributes('aria-describedby')}"]`).text()).toBe(
      fi['tieto-on-liian-pitka-enintaan-255-merkkia']
    )
    expect(wrapper.emitted('submit')).toBeUndefined()
    wrapper.destroy()
  })

  it('preserves the required-field message for empty fields', async () => {
    const wrapper = await render()
    for (const input of wrapper.findAll('input[type="text"]').wrappers) {
      await input.setValue('')
    }
    await wrapper.find('form').trigger('submit')
    for (const input of wrapper.findAll('input[type="text"]').wrappers) {
      expect(input.classes()).toContain('is-invalid')
      expect(wrapper.find(`[id="${input.attributes('aria-describedby')}"]`).text()).toBe(
        fi['pakollinen-tieto']
      )
    }
    expect(wrapper.emitted('submit')).toBeUndefined()
    wrapper.destroy()
  })
})
