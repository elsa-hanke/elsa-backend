<template>
  <elsa-badge :value="value" :help="tasonNimi" />
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import ElsaBadge from '@/components/badge/badge.vue'
  import { ArviointiasteikonTaso } from '@/types'

  @Component({
    components: {
      ElsaBadge
    }
  })
  export default class ElsaArviointiasteikonTaso extends Vue {
    @Prop({ required: true })
    value!: number

    @Prop({ required: true })
    tasot!: ArviointiasteikonTaso[]

    get tasonNimi() {
      const taso = this.tasot.find((asteikonTaso) => asteikonTaso.taso === this.value)
      if (taso) {
        return this.$t('arviointiasteikon-taso-' + taso.nimi)
      }
      return undefined
    }
  }
</script>
