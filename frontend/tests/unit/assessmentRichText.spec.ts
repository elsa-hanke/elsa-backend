import { createLocalVue, mount } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'
import Vuelidate from 'vuelidate'

import RichTextContent from '@/components/rich-text-content/rich-text-content.vue'
import ReviewerForm from '@/forms/arviointityokalut-arvioija-form.vue'
import AssessmentList from '@/views/arvioitavat-kokonaisuudet/arvioitavat-kokonaisuudet-lista.vue'

const localVue = createLocalVue()
localVue.use(BootstrapVue)
localVue.use(Vuelidate)
const fi = '<p onclick="alert(1)"><strong>Suomi</strong></p><img src=x onerror=alert(1)>'
const sv = '<h2>Svenska</h2><a href="javascript:alert(1)">link</a><svg onload=alert(1)></svg>'
const mocks = { $t: (key: string) => key }

it('renders historical assessment descriptions safely in both languages', async () => {
  const wrapper = mount(AssessmentList, {
    localVue,
    mocks,
    propsData: {
      locale: 'fi',
      arvioitavatKokonaisuudet: [{ nimi: 'Block', kuvaus: fi, kuvausSv: sv }]
    },
    stubs: { ElsaAccordian: { template: '<div><slot /></div>' } }
  })
  const content = () => wrapper.findComponent(RichTextContent)
  expect(content().find('strong').text()).toBe('Suomi')
  expect(content().findAll('[onclick], img')).toHaveLength(0)
  await wrapper.setProps({ locale: 'sv' })
  expect(content().find('h2').text()).toBe('Svenska')
  expect(content().findAll('svg, [onload], a[href]')).toHaveLength(0)
  wrapper.destroy()
})

it('renders historical assessment-tool instructions safely for the reviewer', () => {
  const wrapper = mount(ReviewerForm, {
    localVue,
    mocks,
    propsData: {
      valitutArviointityokalut: [{ id: 42, nimi: 'Tool', ohjeteksti: fi, kysymykset: [] }],
      arviointityokaluVastaukset: []
    },
    stubs: {
      ElsaFormGroup: { template: '<div><slot /></div>' },
      ElsaButton: true,
      'font-awesome-icon': true
    }
  })
  const content = wrapper.findComponent(RichTextContent)
  expect(content.find('strong').text()).toBe('Suomi')
  expect(content.findAll('[onclick], img')).toHaveLength(0)
  wrapper.destroy()
})
