<template>
  <div>
    <p v-if="selite" class="mb-3">{{ selite }}</p>
    <div v-for="(asteikonTaso, index) in arviointiasteikonTasot" :key="index">
      <h4 class="mb-1">
        {{ asteikonTaso.taso }}
        {{ $t('arviointiasteikon-taso-' + asteikonTaso.nimi) }}
      </h4>
      <p class="text-size-sm mb-3">
        {{ $t('arviointiasteikon-tason-kuvaus-' + asteikonTaso.nimi) }}
      </p>
    </div>
  </div>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Vue, Prop } from 'vue-property-decorator'

  import { ArviointiasteikonTaso } from '@/types'

  @Component({})
  export default class ElsaArviointiasteikonTasoTooltipContent extends Vue {
    @Prop({ required: false, type: String, default: undefined })
    selite?: string

    @Prop({ required: true })
    arviointiasteikonTasot!: ArviointiasteikonTaso[]
  }
</script>
