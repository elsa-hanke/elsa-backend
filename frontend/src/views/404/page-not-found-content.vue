<template>
  <b-row>
    <b-col md="5">
      <h1 class="text-primary mt-2 mt-md-6 mb-4">{{ $t('sivua-ei-loytynyt-otsikko') }}</h1>
      <p class="mr-4 mb-4">{{ $t('sivua-ei-loytynyt-kuvaus') }}</p>
      <div class="d-flex flex-wrap mb-4">
        <elsa-button variant="primary" class="mr-3 mb-4" :to="{ name: 'etusivu' }">
          {{ $t('siirry-etusivulle') }}
        </elsa-button>
      </div>
    </b-col>
    <b-col class="mr-4">
      <img
        src="@/assets/elsa-404.svg"
        :alt="`404 ${$t('sivua-ei-loytynyt-otsikko')}`"
        class="img-fluid"
      />
    </b-col>
  </b-row>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class PageNotFoundContent extends Vue {}
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .text-primary {
    font-size: 1.75rem;
    font-weight: 400;
    color: $black !important;
  }

  p {
    font-size: $h4-font-size;
  }
</style>
