<template>
  <div class="seurantajakso-container">
    <muokkaa-seurantajaksoa-erikoistuja
      v-if="$isErikoistuva()"
      @skipRouteExitConfirm="skipRouteExitConfirm()"
    ></muokkaa-seurantajaksoa-erikoistuja>
    <muokkaa-seurantajaksoa-kouluttaja-vastuuhenkilo
      v-if="$isKouluttaja() || $isVastuuhenkilo()"
      @skipRouteExitConfirm="skipRouteExitConfirm()"
    ></muokkaa-seurantajaksoa-kouluttaja-vastuuhenkilo>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import MuokkaaSeurantajaksoaErikoistuja from '@/views/seurantakeskustelut/erikoistuva/muokkaa-seurantajaksoa-erikoistuja.vue'
  import MuokkaaSeurantajaksoaKouluttajaVastuuhenkilo from '@/views/seurantakeskustelut/kouluttaja-vastuuhenkilo/muokkaa-seurantajaksoa-kouluttaja-vastuuhenkilo.vue'

  @Component({
    components: {
      MuokkaaSeurantajaksoaErikoistuja,
      MuokkaaSeurantajaksoaKouluttajaVastuuhenkilo
    }
  })
  export default class SeurantajaksoContainer extends Vue {
    get notFound() {
      return !(this.$isErikoistuva() || this.$isKouluttaja())
    }

    skipRouteExitConfirm() {
      this.$emit('skipRouteExitConfirm', true)
    }
  }
</script>

<style lang="scss" scoped>
  .seurantajakso-container {
    max-width: 1024px;
  }
</style>
