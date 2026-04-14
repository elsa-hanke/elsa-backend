<template>
  <b-card-skeleton
    :header-route="'seurantakeskustelut'"
    :header="$t('seurantakeskustelut')"
    :loading="loading"
    class="h-100"
  >
    <div v-if="!loading">
      <div v-if="!account.impersonated">
        <b-row>
          <b-col>
            <elsa-button
              :to="{ name: 'lisaa-seurantajakso' }"
              variant="outline-primary"
              class="mb-3 mt-1"
            >
              {{ $t('lisaa-seurantajakso') }}
            </elsa-button>
          </b-col>
        </b-row>
      </div>
      <div v-if="seurantajaksot.length > 0">
        <b-row v-for="(seurantajakso, index) in seurantajaksot" :key="index" lg>
          <b-col>
            <div class="d-flex justify-content-center border rounded pt-1 pb-1 mb-4">
              <div class="container-fluid">
                <elsa-button
                  :to="{
                    name: 'seurantajakso',
                    params: { seurantajaksoId: seurantajakso.id }
                  }"
                  variant="link"
                  class="pl-0"
                >
                  <h3 class="mb-0">
                    {{ $t('seurantajakso') }} {{ $date(seurantajakso.alkamispaiva) }}
                    <span v-if="seurantajakso.paattymispaiva != null">
                      - {{ $date(seurantajakso.paattymispaiva) }}
                    </span>
                  </h3>
                </elsa-button>
                <p class="mb-2">
                  <font-awesome-icon
                    v-if="showHyvaksytty(seurantajakso)"
                    :icon="['fas', 'check-circle']"
                    class="text-success"
                  />
                  <font-awesome-icon
                    v-if="showKorjattavaa(seurantajakso)"
                    :icon="['fas', 'exclamation-circle']"
                    class="text-error"
                  />
                  <span v-if="showHyvaksytty(seurantajakso)">
                    {{ $t('seurantajakso-tila-hyvaksytty') }}
                    <span v-if="seurantajakso.huolenaiheet != null">
                      {{ $t('seurantajakso-sisaltaa-huolia') }}
                    </span>
                  </span>
                  <span v-else-if="showKorjattavaa(seurantajakso)">
                    {{ $t('seurantajakso-tila-palautettu-korjattavaksi') }}
                    <span class="d-block">
                      {{ $t('syy') }}&nbsp;
                      <span class="font-weight-500">{{ seurantajakso.korjausehdotus }}</span>
                    </span>
                  </span>
                  <span
                    v-else-if="
                      showKouluttajaArvioinut(seurantajakso) ||
                      showOdottaaHyvaksyntaa(seurantajakso)
                    "
                  >
                    {{ $t('seurantajakso-tila-kouluttaja-arvioinut') }}
                    <span v-if="seurantajakso.huolenaiheet != null">
                      {{ $t('seurantajakso-sisaltaa-huolia') }}
                    </span>
                    <span v-if="showOdottaaHyvaksyntaa(seurantajakso)">
                      {{ $t('seurantajakso-tila-merkinnat-lahetetty') }}
                    </span>
                  </span>
                  <span v-else>
                    {{ $t('seurantajakso-tila-lahetetty-kouluttajalle') }}
                    <span v-if="seurantajakso.seurantakeskustelunYhteisetMerkinnat != null">
                      {{ $t('seurantajakso-tila-merkinnat-lahetetty') }}
                    </span>
                  </span>
                </p>
                <div v-if="seurantajakso.seurantakeskustelunYhteisetMerkinnat == null">
                  <p class="mb-1">
                    {{ $t('seurantajakso-tila-taydenna-tiedot-1') }}
                  </p>
                  <ul>
                    <li>{{ $t('seurantajakso-tila-taydenna-tiedot-2') }}</li>
                    <li>{{ $t('seurantajakso-tila-taydenna-tiedot-3') }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </b-col>
        </b-row>
      </div>
      <div v-else>
        <b-alert :show="true" variant="dark">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
            </em>
            <div>{{ $t('seurantakeskusteluja-ei-viela-kayty') }}</div>
          </div>
        </b-alert>
      </div>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getSeurantajaksot } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import store from '@/store'
  import { Seurantajakso } from '@/types'
  import { SeurantajaksoTila } from '@/utils/constants'
  import { sortByDateDesc } from '@/utils/date'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class SeurantakeskustelutCard extends Vue {
    seurantajaksot: Seurantajakso[] = []
    naytaKuvaus = false
    loading = true

    async mounted() {
      this.loading = true
      try {
        this.seurantajaksot = (await getSeurantajaksot()).data.sort(
          (s1: Seurantajakso, s2: Seurantajakso) => {
            return sortByDateDesc(s1.alkamispaiva, s2.alkamispaiva)
          }
        )
      } catch {
        toastFail(this, this.$t('seurantakeskustelujen-hakeminen-epaonnistui'))
        this.seurantajaksot = []
      }
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    showHyvaksytty(seurantajakso: Seurantajakso) {
      return seurantajakso.tila === SeurantajaksoTila.HYVAKSYTTY
    }

    showKorjattavaa(seurantajakso: Seurantajakso) {
      return seurantajakso.tila === SeurantajaksoTila.PALAUTETTU_KORJATTAVAKSI
    }

    showKouluttajaArvioinut(seurantajakso: Seurantajakso) {
      return seurantajakso.tila === SeurantajaksoTila.ODOTTAA_YHTEISIA_MERKINTOJA
    }

    showOdottaaHyvaksyntaa(seurantajakso: Seurantajakso) {
      return seurantajakso.tila === SeurantajaksoTila.ODOTTAA_HYVAKSYNTAA
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep {
    .card-body {
      padding-top: 0.75rem;
      padding-bottom: 0.5rem;
    }
  }
</style>
