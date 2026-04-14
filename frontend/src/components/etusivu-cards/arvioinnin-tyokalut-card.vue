<template>
  <b-card-skeleton :header="$t('arvioinnin-tyokalut')" class="mb-4">
    <div class="icon-container">
      <font-awesome-icon icon="clipboard-list" fixed-width class="fa-5x text-primary icon" />
    </div>
    <div class="text-center">
      <!--<a
        href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/elsa"
        target="_blank"
        rel="noopener noreferrer"
      >-->
      <RouterLink to="/arviointityokalut/esittely">
        {{ $t('arvioinnin-tyokalut-tutustu') }}
      </RouterLink>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import BCardSkeleton from '@/components/card/card.vue'

  @Component({
    components: {
      BCardSkeleton
    }
  })
  export default class ArvioinninTyokalutCard extends Vue {}
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .icon-container {
    border: 2px solid $primary;
    border-radius: 50%;
    height: 120px;
    width: 120px;
    margin: 0 auto 15px;
    text-align: center;
    .icon {
      margin-top: 17px;
    }
  }
</style>
