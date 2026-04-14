<template>
  <div class="seurantajakso-container">
    <seurantajakso-view-erikoistuja v-if="$isErikoistuva()"></seurantajakso-view-erikoistuja>
    <seurantajakso-view-kouluttaja-vastuuhenkilo
      v-if="$isKouluttaja() || $isVastuuhenkilo()"
    ></seurantajakso-view-kouluttaja-vastuuhenkilo>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import SeurantajaksoViewErikoistuja from '@/views/seurantakeskustelut/erikoistuva/seurantajakso-erikoistuja.vue'
  import SeurantajaksoViewKouluttajaVastuuhenkilo from '@/views/seurantakeskustelut/kouluttaja-vastuuhenkilo/seurantajakso-kouluttaja-vastuuhenkilo.vue'

  @Component({
    components: {
      SeurantajaksoViewErikoistuja,
      SeurantajaksoViewKouluttajaVastuuhenkilo
    }
  })
  export default class SeurantajaksoContainer extends Vue {}
</script>

<style lang="scss" scoped>
  .seurantajakso-container {
    max-width: 1024px;
  }
</style>
