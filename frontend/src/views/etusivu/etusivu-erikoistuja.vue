<template>
  <b-container fluid class="mt-3">
    <b-row class="mt-3">
      <b-col xxl="9">
        <avoimet-asiat-card />
        <erikoistumisen-edistyminen-card />
        <b-row class="d-flex">
          <b-col xl="6" class="mb-5">
            <koulutussuunnitelma-card v-if="!isImpersonatedVirkailija" />
          </b-col>
          <b-col xl="6" class="mb-5">
            <seurantakeskustelut-card v-if="!isImpersonatedVirkailija" />
          </b-col>
        </b-row>
        <paivittaiset-merkinnat-card v-if="!isImpersonatedVirkailija" />
      </b-col>
      <b-col xxl="3">
        <b-row>
          <b-col xxl="12" md="6" class="mt-4 mt-xxl-0">
            <henkilotiedot-card />
          </b-col>
          <b-col xxl="12" md="6" class="mt-4 mt-xxl-0">
            <arvioitavat-kokonaisuudet-card />
          </b-col>
        </b-row>
      </b-col>
    </b-row>
  </b-container>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import BCardSkeleton from '@/components/card/card.vue'
  import ArvioitavatKokonaisuudetCard from '@/components/etusivu-cards/arvioitavat-kokonaisuudet-card.vue'
  import AvoimetAsiatCard from '@/components/etusivu-cards/avoimet-asiat-card.vue'
  import ErikoistumisenEdistyminenCard from '@/components/etusivu-cards/erikoistumisen-edistyminen-card.vue'
  import HenkilotiedotCard from '@/components/etusivu-cards/henkilotiedot-card.vue'
  import KoulutussuunnitelmaCard from '@/components/etusivu-cards/koulutussuunnitelma-card.vue'
  import PaivittaisetMerkinnatCard from '@/components/etusivu-cards/paivittaiset-merkinnat-card.vue'
  import SeurantakeskustelutCard from '@/components/etusivu-cards/seurantakeskustelut-card.vue'
  import store from '@/store'
  import { ELSA_ROLE } from '@/utils/roles'

  @Component({
    components: {
      BCardSkeleton,
      AvoimetAsiatCard,
      SeurantakeskustelutCard,
      ErikoistumisenEdistyminenCard,
      HenkilotiedotCard,
      KoulutussuunnitelmaCard,
      ArvioitavatKokonaisuudetCard,
      PaivittaisetMerkinnatCard
    }
  })
  export default class EtusivuErikoistuja extends Vue {
    get isImpersonatedVirkailija() {
      const account = store.getters['auth/account']
      return (
        account.impersonated &&
        account.originalUser.authorities.includes(ELSA_ROLE.OpintohallinnonVirkailija)
      )
    }
  }
</script>
