import { shallowMount } from '@vue/test-utils'
import { BootstrapVueNext } from 'bootstrap-vue-next'

import Card from '@/components/card/card.vue'

describe('Card.vue', () => {
  it('renders', () => {
    shallowMount(Card, {
      global: {
        plugins: [BootstrapVueNext]
      }
    })
  })
})
