<template>
  <p v-if="vanhanAsetuksenMukainen" class="d-flex">
    <font-awesome-icon icon="info-circle" class="text-muted mr-2 mt-1" />
    <!-- eslint-disable-next-line vue/no-v-html -->
    <span v-html="$t('vanhan-asetuksen-mukaisesti', { opintooppaastasiLinkki })" />
  </p>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import ElsaBadge from '@/components/badge/badge.vue'
  import store from '@/store'
  import { Asetus, ErikoistuvaLaakari } from '@/types'
  import { vanhatAsetukset } from '@/utils/constants'
  import { resolveOpintooikeusKaytossa } from '@/utils/opintooikeus'

  @Component({
    components: {
      ElsaBadge
    }
  })
  export default class ElsaVanhaAsetusVaroitus extends Vue {
    get opintooppaastasiLinkki() {
      return `<a href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/opinto-oppaat/" target="_blank" rel="noopener noreferrer">${(
        this.$t('opintooppaastasi') as string
      ).toLowerCase()}</a>`
    }

    get asetus(): Asetus | undefined {
      const erikoistuvaLaakari = store.getters['auth/account']
        ?.erikoistuvaLaakari as ErikoistuvaLaakari
      const opintooikeusKaytossa = resolveOpintooikeusKaytossa(erikoistuvaLaakari)
      return opintooikeusKaytossa?.asetus
    }

    get vanhanAsetuksenMukainen(): boolean {
      return this.asetus?.nimi ? vanhatAsetukset.includes(this.asetus?.nimi) : false
    }
  }
</script>
