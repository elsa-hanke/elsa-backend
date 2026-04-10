<template>
  <div class="seurantakeskustelut-container">
    <seurantakeskustelut-view-erikoistuva
      v-if="$isErikoistuva()"
    ></seurantakeskustelut-view-erikoistuva>
    <seurantakeskustelut-view-kouluttaja-vastuuhenkilo
      v-if="$isKouluttaja() || $isVastuuhenkilo()"
    ></seurantakeskustelut-view-kouluttaja-vastuuhenkilo>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import SeurantakeskustelutViewErikoistuva from '@/views/seurantakeskustelut/erikoistuva/seurantakeskustelut-erikoistuja.vue'
  import SeurantakeskustelutViewKouluttajaVastuuhenkilo from '@/views/seurantakeskustelut/kouluttaja-vastuuhenkilo/seurantakeskustelut-kouluttaja-vastuuhenkilo.vue'

  @Component({
    components: {
      SeurantakeskustelutViewErikoistuva,
      SeurantakeskustelutViewKouluttajaVastuuhenkilo
    }
  })
  export default class SeurantakeskustelutContainer extends Vue {}
</script>

<style lang="scss" scoped>
  .seurantakeskustelut-container {
    max-width: 1024px;
  }
</style>
