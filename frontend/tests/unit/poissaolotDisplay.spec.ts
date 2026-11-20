import { shallowMount, createLocalVue } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'

import ElsaPoissaolotDisplay from '@/components/poissaolot-display/poissaolot-display.vue'
import { Keskeytysaika } from '@/types'

const localVue = createLocalVue()
localVue.use(BootstrapVue)

describe('ElsaPoissaolotDisplay.vue', () => {
  const poissaolot = [
    {
      id: 1,
      alkamispaiva: '2023-05-01',
      paattymispaiva: '2023-05-10',
      poissaoloprosentti: 100,
      poissaolonSyy: { id: 1, nimi: 'Muu syy' }
    },
    {
      id: 2,
      alkamispaiva: '2021-02-01',
      paattymispaiva: '2021-02-15',
      poissaoloprosentti: 50,
      poissaolonSyy: { id: 2, nimi: 'Vanhempainvapaa' }
    },
    {
      id: 3,
      alkamispaiva: '2022-10-01',
      paattymispaiva: '2022-10-05',
      poissaoloprosentti: 100,
      poissaolonSyy: { id: 3, nimi: 'Sairausloma' }
    }
  ] as unknown as Keskeytysaika[]

  it('sorts poissaolot chronologically by alkamispaiva', () => {
    const wrapper = shallowMount(ElsaPoissaolotDisplay, {
      localVue,
      stubs: ['font-awesome-icon'],
      propsData: {
        poissaolot
      },
      mocks: {
        $t: (key: string) => key,
        $date: (val: string) => val
      }
    })

    const vm = wrapper.vm as unknown as { sortedPoissaolot: Keskeytysaika[] }
    expect(vm.sortedPoissaolot.map((p) => p.alkamispaiva)).toEqual([
      '2021-02-01',
      '2022-10-01',
      '2023-05-01'
    ])
  })
})
