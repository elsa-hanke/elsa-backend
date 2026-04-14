<template>
  <div class="seurantakeskustelut">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('seurantakeskustelut') }}</h1>
          <p class="mt-3 mb-1">
            {{ $t('seurantakeskustelut-kuvaus-1') }}
          </p>
          <p v-if="naytaKuvaus" class="mt-4 mb-4">
            {{ $t('seurantakeskustelut-kuvaus-2') }}
          </p>
          <p v-if="naytaKuvaus" class="mb-1 mt-1">
            {{ $t('seurantakeskustelut-kuvaus-3') }}
          </p>
        </b-col>
      </b-row>
      <b-row class="justify-content-md-center">
        <b-button variant="link" @click="toggleKuvaus">
          {{ naytaKuvaus ? $t('nayta-vahemman') : $t('nayta-lisaa') }}
        </b-button>
      </b-row>
      <hr />
      <elsa-button
        v-if="!account.impersonated"
        :to="{ name: 'lisaa-seurantajakso' }"
        variant="primary"
        class="mb-4"
      >
        {{ $t('lisaa-seurantajakso') }}
      </elsa-button>
      <div v-if="!loading">
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
                  <p v-if="seurantajakso.koulutusjaksot.length > 0" class="mb-2">
                    <span class="font-weight-bold">{{ $t('koulutusjaksot') }}:</span>
                    <span>
                      {{ koulutusjaksotLabel(seurantajakso) }}
                    </span>
                  </p>
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
                      <elsa-button
                        :to="{
                          name: 'muokkaa-seurantajaksoa',
                          params: { seurantajaksoId: seurantajakso.id }
                        }"
                        variant="primary"
                        class="mb-2 mt-3"
                      >
                        {{ $t('muokkaa-yhteisia-merkintoja') }}
                      </elsa-button>
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
                    <elsa-button
                      v-if="!account.impersonated"
                      :to="{
                        name: 'muokkaa-seurantajaksoa',
                        params: { seurantajaksoId: seurantajakso.id }
                      }"
                      variant="primary"
                      class="mb-3"
                    >
                      {{ $t('lisaa-yhteiset-merkinnat') }}
                    </elsa-button>
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
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getSeurantajaksot } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import store from '@/store'
  import { Seurantajakso } from '@/types'
  import { SeurantajaksoTila } from '@/utils/constants'
  import { sortByDateDesc } from '@/utils/date'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class SeurantakeskustelutViewErikoistuva extends Vue {
    seurantajaksot: Seurantajakso[] = []
    naytaKuvaus = false
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('seurantakeskustelut'),
        active: true
      }
    ]
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

    toggleKuvaus() {
      this.naytaKuvaus = !this.naytaKuvaus
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

    koulutusjaksotLabel(seurantajakso: Seurantajakso) {
      return seurantajakso.koulutusjaksot.map((k) => k.nimi).join(', ')
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .seurantakeskustelut {
    max-width: 1024px;
  }
</style>
