<template>
  <div>
    <p>{{ $t('vaativuustaso-tooltip.ingressi') }}</p>
    <div v-for="(taso, index) in vaativuustasot" :key="index">
      <h4>{{ taso.arvo }} {{ $t(taso.nimi) }}</h4>
      <p>{{ $t(taso.kuvaus) }}</p>
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import { Vaativuustaso } from '@/types'
  import { vaativuustasot } from '@/utils/constants'

  @Component({})
  export default class ElsaVaativuustasoTooltipContent extends Vue {
    vaativuustasot: Vaativuustaso[] = vaativuustasot
  }
</script>
