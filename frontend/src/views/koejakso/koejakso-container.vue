<template>
  <div class="koejakso-container">
    <koejakso-view-erikoistuva v-if="$isErikoistuva()"></koejakso-view-erikoistuva>
    <koejakso-view-kouluttaja v-if="$isKouluttaja()"></koejakso-view-kouluttaja>
    <koejakso-view-vastuuhenkilo v-if="$isVastuuhenkilo()"></koejakso-view-vastuuhenkilo>
    <koejakso-view-virkailija v-if="$isVirkailija()"></koejakso-view-virkailija>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import KoejaksoViewErikoistuva from '@/views/koejakso/erikoistuva/koejakso-view-erikoistuva.vue'
  import KoejaksoViewKouluttaja from '@/views/koejakso/kouluttaja/koejakso-view-kouluttaja.vue'
  import KoejaksoViewVastuuhenkilo from '@/views/koejakso/vastuuhenkilo/koejakso-view-vastuuhenkilo.vue'
  import KoejaksoViewVirkailija from '@/views/koejakso/virkailija/koejakso-view-virkailija.vue'

  @Component({
    components: {
      KoejaksoViewKouluttaja,
      KoejaksoViewErikoistuva,
      KoejaksoViewVastuuhenkilo,
      KoejaksoViewVirkailija
    }
  })
  export default class KoejaksoContainer extends Vue {
    get notFound() {
      return !(this.$isErikoistuva() || this.$isKouluttaja() || this.$isVastuuhenkilo())
    }
  }
</script>

<style lang="scss" scoped>
  .koejakso-container {
    max-width: 1024px;
  }
</style>
