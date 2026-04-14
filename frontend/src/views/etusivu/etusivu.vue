<template>
  <div class="etusivu">
    <EtusivuErikoistuja v-if="$isErikoistuva()" />
    <EtusivuYekErikoistuja v-if="$isYekKoulutettava()" />
    <EtusivuKouluttaja v-else-if="$isKouluttaja()" />
    <EtusivuVastuuhenkilo v-else-if="$isVastuuhenkilo()" />
    <EtusivuVirkailija v-else-if="$isVirkailija()" />
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import EtusivuErikoistuja from '@/views/etusivu/etusivu-erikoistuja.vue'
  import EtusivuKouluttaja from '@/views/etusivu/etusivu-kouluttaja.vue'
  import EtusivuVastuuhenkilo from '@/views/etusivu/etusivu-vastuuhenkilo.vue'
  import EtusivuVirkailija from '@/views/etusivu/etusivu-virkailija.vue'
  import EtusivuYekErikoistuja from '@/views/etusivu/etusivu-yek-erikoistuja.vue'

  @Component({
    components: {
      EtusivuErikoistuja,
      EtusivuYekErikoistuja,
      EtusivuKouluttaja,
      EtusivuVastuuhenkilo,
      EtusivuVirkailija
    }
  })
  export default class Etusivu extends Vue {}
</script>

<style lang="scss" scoped>
  .etusivu {
    max-width: 1420px;
  }
</style>
