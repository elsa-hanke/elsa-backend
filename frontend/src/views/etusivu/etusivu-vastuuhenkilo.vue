<template>
  <b-container fluid class="mt-3">
    <div>
      <h2 class="mb-3">{{ $t('avoimet-asiat') }}</h2>
      <b-row>
        <b-col xxl="9">
          <arviointipyynnot-card />
          <seurantajaksot-card />
          <koejaksot-card />
          <terveyskeskuskoulutusjaksot-card v-if="$isTerveyskeskuskoulutusjaksoVastuuhenkilo()" />
          <yek-terveyskeskuskoulutusjaksot-card
            v-if="$isYekTerveyskeskuskoulutusjaksoVastuuhenkilo()"
          />
          <valmistumispyynnot-card />
          <yekvalmistumispyynnot-card v-if="$isYekValmistumisenVastuuhenkilo()" />
          <h2 class="mt-5 mb-3">{{ $t('erikoistujien-seuranta') }}</h2>
          <b-tabs
            v-if="
              $isYekValmistumisenVastuuhenkilo() || $isYekTerveyskeskuskoulutusjaksoVastuuhenkilo()
            "
          >
            <b-tab :title="$t('vastuuhenkilo-etusivu-seuranta-otsikko')">
              <erikoistujien-seuranta-card />
            </b-tab>
            <b-tab v-if="!loading" :title="$t('yek.virkailija-etusivu-seuranta-yek-otsikko')">
              <yek-koulutettavien-seuranta-card :yliopisto="yliopisto" />
            </b-tab>
          </b-tabs>
          <erikoistujien-seuranta-card v-else />
        </b-col>
        <b-col xxl="3">
          <b-row>
            <b-col xxl="12" lg="6">
              <henkilotiedot-card />
            </b-col>
            <b-col xxl="12" lg="6">
              <arvioinnin-tyokalut-card />
            </b-col>
            <b-col xxl="12" md="6" class="mt-4 mt-xxl-0">
              <arvioitavat-kokonaisuudet-card />
            </b-col>
          </b-row>
        </b-col>
      </b-row>
    </div>
  </b-container>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getYliopisto } from '@/api/vastuuhenkilo'
  import BCardSkeleton from '@/components/card/card.vue'
  import ArvioinninTyokalutCard from '@/components/etusivu-cards/arvioinnin-tyokalut-card.vue'
  import ArviointipyynnotCard from '@/components/etusivu-cards/arviointipyynnot-card.vue'
  import ArvioitavatKokonaisuudetCard from '@/components/etusivu-cards/arvioitavat-kokonaisuudet-card.vue'
  import ErikoistujienSeurantaCard from '@/components/etusivu-cards/erikoistujien-seuranta-card.vue'
  import HenkilotiedotCard from '@/components/etusivu-cards/henkilotiedot-card.vue'
  import KoejaksotCard from '@/components/etusivu-cards/koejaksot-card.vue'
  import SeurantajaksotCard from '@/components/etusivu-cards/seurantajaksot-card.vue'
  import TerveyskeskuskoulutusjaksotCard from '@/components/etusivu-cards/terveyskeskuskoulutusjaksot-card.vue'
  import ValmistumispyynnotCard from '@/components/etusivu-cards/valmistumispyynnot-card.vue'
  import YekKoulutettavienSeurantaCard from '@/components/etusivu-cards/yek-koulutettavien-seuranta-card.vue'
  import YekTerveyskeskuskoulutusjaksotCard from '@/components/etusivu-cards/yek-terveyskeskuskoulutusjaksot-card.vue'
  import YekvalmistumispyynnotCard from '@/components/etusivu-cards/yek-valmistumispyynnot-card.vue'

  @Component({
    components: {
      ArvioinninTyokalutCard,
      ArviointipyynnotCard,
      ArvioitavatKokonaisuudetCard,
      BCardSkeleton,
      ErikoistujienSeurantaCard,
      HenkilotiedotCard,
      KoejaksotCard,
      SeurantajaksotCard,
      TerveyskeskuskoulutusjaksotCard,
      YekTerveyskeskuskoulutusjaksotCard,
      YekvalmistumispyynnotCard,
      YekKoulutettavienSeurantaCard,
      ValmistumispyynnotCard
    }
  })
  export default class EtusivuVastuuhenkilo extends Vue {
    tabIndex = 0
    loading = true
    yliopisto: string | null = null
    tabs = ['#erikoislaakarikoulutus', '#yekkoulutus']

    beforeMount() {
      this.tabIndex = this.tabs.findIndex((tab) => tab === this.$route.hash)
    }

    async mounted() {
      this.yliopisto = (await getYliopisto()).data
      this.loading = false
    }
  }
</script>
