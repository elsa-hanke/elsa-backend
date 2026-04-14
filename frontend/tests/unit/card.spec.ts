import { shallowMount, createLocalVue } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'

import Card from '@/components/card/card.vue'

const localVue = createLocalVue()
localVue.use(BootstrapVue)

describe('Card.vue', () => {
  it('renders', () => {
    shallowMount(Card, {
      localVue
    })
  })
})
