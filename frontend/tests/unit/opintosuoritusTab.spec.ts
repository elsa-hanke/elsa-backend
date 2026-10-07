import { createLocalVue, mount } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'
import VueI18n from 'vue-i18n'

import OpintosuoritusTab from '@/views/opintosuoritukset/opintosuoritus-tab.vue'
import { Opintosuoritus, OpintosuoritusOsakokonaisuus } from '@/types'

const localVue = createLocalVue()
localVue.use(BootstrapVue)
localVue.use(VueI18n)

function part(
  id: number,
  name: string,
  date: string,
  passed: boolean,
  swedish = ''
): OpintosuoritusOsakokonaisuus {
  return {
    id,
    nimi_fi: name,
    nimi_sv: swedish,
    suorituspaiva: date,
    hyvaksytty: passed,
    kurssikoodi: `part-${id}`,
    opintopisteet: 0,
    arvio_fi: null,
    arvio_sv: null,
    vanhenemispaiva: null
  }
}

function render(
  children: OpintosuoritusOsakokonaisuus[] | null,
  locale = 'fi',
  variant = 'kuulustelu'
) {
  const exam: Opintosuoritus = {
    id: 1,
    nimi_fi: 'Valtakunnallinen kuulustelu',
    nimi_sv: 'Nationellt prov',
    kurssikoodi: 'ELOP0001',
    suorituspaiva: '2026-05-07',
    hyvaksytty: true,
    opintopisteet: 0,
    tyyppi: null,
    arvio_fi: null,
    arvio_sv: null,
    vanhenemispaiva: null,
    yliopistoOpintooikeusId: 'fixture-right',
    osakokonaisuudet: children
  }
  return mount(OpintosuoritusTab, {
    localVue,
    i18n: new VueI18n({
      locale,
      messages: {
        fi: { hyvaksytty: 'Hyväksytty', hylatty: 'Hylätty' },
        sv: { hyvaksytty: 'Godkänd', hylatty: 'Underkänd' }
      },
      silentTranslationWarn: true
    }),
    propsData: { variant, suoritukset: [exam] },
    mocks: { $date: (date: string) => date },
    stubs: { 'font-awesome-icon': true }
  })
}

describe('Exam subparts in the shared study accomplishment view', () => {
  it('renders name, date and pass/fail independently of the main exam', () => {
    const wrapper = render([
      part(2, 'Patologia, esseet', '2026-05-07', false),
      part(3, 'Patologia, preparaatit', '2026-05-08', true)
    ])
    const rows = wrapper.findAll('.exam-subpart-row')
    expect(rows).toHaveLength(2)
    expect(rows.at(0).text()).toContain('Patologia, esseet')
    expect(rows.at(0).text()).toContain('2026-05-07')
    expect(rows.at(0).text()).toContain('Hylätty')
    expect(rows.at(1).text()).toContain('Hyväksytty')
    wrapper.destroy()
  })

  it('uses Finnish for empty Swedish names and keeps nonempty Swedish names', () => {
    const wrapper = render(
      [
        part(2, 'Patologia, esseet', '2026-05-07', true, '  '),
        part(3, 'Patologia, preparaatit', '2026-05-08', false, 'Patologi, preparat')
      ],
      'sv'
    )
    expect(wrapper.text()).toContain('Nationellt prov')
    const rows = wrapper.findAll('.exam-subpart-row')
    expect(rows.at(0).text()).toContain('Patologia, esseet')
    expect(rows.at(0).text()).toContain('Godkänd')
    expect(rows.at(1).text()).toContain('Patologi, preparat')
    expect(rows.at(1).text()).toContain('Underkänd')
    wrapper.destroy()
  })

  it('sorts by date then name regardless of incoming order without mutating the prop', () => {
    const children = [
      part(4, 'C', '2026-05-08', true),
      part(3, 'B', '2026-05-07', true),
      part(2, 'A', '2026-05-07', true)
    ]
    const first = render(children)
    const second = render([...children].reverse())
    const rowNames = (wrapper: typeof first) =>
      wrapper.findAll('.exam-subpart-row').wrappers.map((row) => row.find('td').text())
    expect(rowNames(first)).toEqual(['A', 'B', 'C'])
    expect(rowNames(second)).toEqual(rowNames(first))
    expect(children.map((child) => child.id)).toEqual([4, 3, 2])
    first.destroy()
    second.destroy()
  })

  it('renders only the main exam for null or empty children', () => {
    for (const children of [null, [] as OpintosuoritusOsakokonaisuus[]]) {
      const wrapper = render(children)
      expect(wrapper.findAll('.exam-subpart-row')).toHaveLength(0)
      expect(wrapper.text()).toContain('Valtakunnallinen kuulustelu')
      wrapper.destroy()
    }
  })

  it('keeps the muu variant without exam child rows', () => {
    const wrapper = render([part(2, 'A', '2026-05-07', true)], 'fi', 'muu')
    expect(wrapper.findAll('.exam-subpart-row')).toHaveLength(0)
    wrapper.destroy()
  })
})
