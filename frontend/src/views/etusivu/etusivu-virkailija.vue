<template>
  <b-container fluid class="mt-3">
    <div>
      <h2 class="mb-3">{{ $t('avoimet-asiat') }}</h2>
      <b-row>
        <b-col xxl="9">
          <div v-if="!loading">
            <terveyskeskuskoulutusjaksot-card />
            <yek-terveyskeskuskoulutusjaksot-card />
            <koejaksot-card :show-vaihe="false" />
            <valmistumispyynnot-card />
            <yekvalmistumispyynnot-card />
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
          <h2 class="mt-5 mb-3">{{ $t('erikoistujien-seuranta') }}</h2>
          <b-tabs>
            <b-tab :title="$t('virkailija-etusivu-seuranta-otsikko')">
              <div v-if="!loading">
                <erikoistujien-seuranta-virkailija-card :yliopisto="yliopisto" />
              </div>
              <div v-else class="text-center">
                <b-spinner variant="primary" :label="$t('ladataan')" />
              </div>
            </b-tab>
            <b-tab :title="$t('yek.virkailija-etusivu-seuranta-yek-otsikko')">
              <div v-if="!loading">
                <yek-koulutettavien-seuranta-card :yliopisto="yliopisto" />
              </div>
              <div v-else class="text-center">
                <b-spinner variant="primary" :label="$t('ladataan')" />
              </div>
            </b-tab>
          </b-tabs>
        </b-col>
      </b-row>
    </div>
  </b-container>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getYliopisto } from '@/api/virkailija'
  import BCardSkeleton from '@/components/card/card.vue'
  import ErikoistujienSeurantaVirkailijaCard from '@/components/etusivu-cards/erikoistujien-seuranta-virkailija-card.vue'
  import HenkilotiedotCard from '@/components/etusivu-cards/henkilotiedot-card.vue'
  import KoejaksotCard from '@/components/etusivu-cards/koejaksot-card.vue'
  import TerveyskeskuskoulutusjaksotCard from '@/components/etusivu-cards/terveyskeskuskoulutusjaksot-card.vue'
  import ValmistumispyynnotCard from '@/components/etusivu-cards/valmistumispyynnot-card.vue'
  import YekKoulutettavienSeurantaCard from '@/components/etusivu-cards/yek-koulutettavien-seuranta-card.vue'
  import YekTerveyskeskuskoulutusjaksotCard from '@/components/etusivu-cards/yek-terveyskeskuskoulutusjaksot-card.vue'
  import YekvalmistumispyynnotCard from '@/components/etusivu-cards/yek-valmistumispyynnot-card.vue'
  import ErikoistuvatLaakarit from '@/views/kayttajahallinta/erikoistuvat-laakarit.vue'
  import Kouluttajat from '@/views/kayttajahallinta/kouluttajat.vue'
  import Paakayttajat from '@/views/kayttajahallinta/paakayttajat.vue'
  import Vastuuhenkilot from '@/views/kayttajahallinta/vastuuhenkilot.vue'
  import Virkailijat from '@/views/kayttajahallinta/virkailijat.vue'

  @Component({
    components: {
      Paakayttajat,
      ErikoistuvatLaakarit,
      Kouluttajat,
      Virkailijat,
      Vastuuhenkilot,
      BCardSkeleton,
      ErikoistujienSeurantaVirkailijaCard,
      HenkilotiedotCard,
      KoejaksotCard,
      TerveyskeskuskoulutusjaksotCard,
      ValmistumispyynnotCard,
      YekTerveyskeskuskoulutusjaksotCard,
      YekvalmistumispyynnotCard,
      YekKoulutettavienSeurantaCard
    }
  })
  export default class EtusivuVirkailija extends Vue {
    initializing = true
    loading = true
    yliopisto: string | null = null

    tabIndex = 0
    tabs = ['#erikoislaakarikoulutus', '#yekkoulutus']
    beforeMount() {
      this.tabIndex = this.tabs.findIndex((tab) => tab === this.$route.hash)
    }
    async mounted() {
      await this.fetchYliopisto()
      this.loading = false
    }

    async fetchYliopisto() {
      this.yliopisto = (await getYliopisto()).data
      this.loading = false
    }
  }
</script>
