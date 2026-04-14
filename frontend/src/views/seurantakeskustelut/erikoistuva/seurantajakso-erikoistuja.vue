<template>
  <div class="seurantajakso">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row v-if="!loading && seurantajakso" lg>
        <b-col>
          <h1>{{ $t('seurantajakso') }}</h1>
          <b-alert
            :show="showOdottaaKeskusteluaJaArviointia || showOdottaaArviointia"
            variant="dark"
            class="mt-3"
          >
            <div class="d-flex flex-row">
              <em class="align-middle">
                <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
              </em>
              <div>
                <p class="mb-0">
                  {{ $t('seurantajakso-tila-lahetetty-kouluttajalle') }}
                  <span v-if="showOdottaaArviointia">
                    {{ $t('seurantajakso-tila-odottaa-arviointia') }}
                  </span>
                </p>
                <div v-if="showOdottaaKeskusteluaJaArviointia">
                  <p class="mt-2 mb-1">
                    {{ $t('seurantajakso-tila-taydenna-tiedot-1') }}
                  </p>
                  <ul>
                    <li>{{ $t('seurantajakso-tila-taydenna-tiedot-2') }}</li>
                    <li>{{ $t('seurantajakso-tila-taydenna-tiedot-3') }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </b-alert>
          <b-alert
            :show="showOdottaaKeskustelua || showOdottaaHyvaksyntaa"
            variant="dark"
            class="mt-3"
          >
            <div class="d-flex flex-row">
              <em class="align-middle">
                <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
              </em>
              <div>
                <p class="mb-0">
                  {{ $t('seurantajakso-tila-kouluttaja-arvioinut') }}
                  <span v-if="showHuolenaiheet">
                    {{ $t('seurantajakso-sisaltaa-huolia') }}
                  </span>
                  <span v-if="showOdottaaHyvaksyntaa">
                    {{ $t('seurantajakso-tila-merkinnat-lahetetty') }}
                  </span>
                </p>
                <div v-if="showOdottaaKeskustelua">
                  <p class="mt-2 mb-1">
                    {{ $t('seurantajakso-tila-taydenna-tiedot-1') }}
                  </p>
                  <ul>
                    <li>{{ $t('seurantajakso-tila-taydenna-tiedot-2') }}</li>
                    <li>{{ $t('seurantajakso-tila-taydenna-tiedot-3') }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </b-alert>
          <b-alert :show="showPalautettuKorjattavaksi" variant="danger" class="mt-3">
            <div class="d-flex flex-row">
              <em class="align-middle">
                <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
              </em>
              <div>
                {{ $t('seurantajakso-palautettu-korjattavaksi') }}
                <span class="d-block">
                  {{ $t('syy') }}
                  <span class="font-weight-500">{{ seurantajakso.korjausehdotus }}</span>
                </span>
              </div>
            </div>
          </b-alert>
          <b-alert :show="showHyvaksytty" variant="success" class="mt-3">
            <div class="d-flex flex-row">
              <em class="align-middle">
                <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
              </em>
              {{ $t('seurantajakso-tila-hyvaksytty') }}
              <span v-if="showHuolenaiheet" class="ml-1">
                {{ $t('seurantajakso-sisaltaa-huolia') }}
              </span>
            </div>
          </b-alert>
        </b-col>
      </b-row>
      <div v-if="!loading">
        <div class="d-flex flex-wrap">
          <elsa-button class="mb-2 mt-2" :to="{ name: 'seurantakeskustelut' }" variant="primary">
            {{ $t('palaa-seurantajaksoihin') }}
          </elsa-button>
          <elsa-button
            v-if="canEdit && !account.impersonated"
            :to="{ name: 'muokkaa-seurantajaksoa' }"
            variant="outline-primary"
            class="ml-md-3 mb-2 mt-2"
          >
            {{ $t('muokkaa-tietoja') }}
          </elsa-button>
        </div>
        <hr />
        <div v-if="seurantajaksonTiedot != null">
          <seurantajakso-form
            :editing="false"
            :seurantajakso="seurantajakso"
            :seurantajakson-tiedot="seurantajaksonTiedot"
          />
          <div class="d-flex flex-row-reverse flex-wrap">
            <elsa-button
              v-if="canEdit && !account.impersonated"
              :to="{ name: 'muokkaa-seurantajaksoa' }"
              variant="primary"
              class="ml-3"
            >
              {{ $t('muokkaa-tietoja') }}
            </elsa-button>
          </div>
        </div>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import { getSeurantajakso, getSeurantajaksonTiedot } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import SeurantajaksoForm from '@/forms/seurantajakso-form.vue'
  import store from '@/store'
  import { Seurantajakso, SeurantajaksonTiedot } from '@/types'
  import { SeurantajaksoTila } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SeurantajaksoForm
    }
  })
  export default class SeurantajaksoViewErikoistuja extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('seurantakeskustelut'),
        to: { name: 'seurantakeskustelut' }
      },
      {
        text: this.$t('seurantajakso'),
        active: true
      }
    ]
    loading = true

    seurantajakso: Seurantajakso | null = null
    seurantajaksonTiedot: SeurantajaksonTiedot | null = null

    params = {
      saving: false
    }

    async mounted() {
      this.loading = true
      try {
        this.seurantajakso = (await getSeurantajakso(this.$route?.params?.seurantajaksoId)).data
        this.seurantajaksonTiedot = (
          await getSeurantajaksonTiedot(
            this.seurantajakso.alkamispaiva || '',
            this.seurantajakso.paattymispaiva || '',
            this.seurantajakso.koulutusjaksot
              .map((k) => k.id)
              .filter((k): k is number => k !== null)
          )
        ).data
      } catch {
        toastFail(this, this.$t('seurantajakson-tietojen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'seurantakeskustelut' })
      }
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    get showOdottaaKeskusteluaJaArviointia() {
      return (
        this.seurantajakso?.tila === SeurantajaksoTila.ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA
      )
    }

    get showOdottaaKeskustelua() {
      return this.seurantajakso?.tila === SeurantajaksoTila.ODOTTAA_YHTEISIA_MERKINTOJA
    }

    get showOdottaaArviointia() {
      return this.seurantajakso?.tila === SeurantajaksoTila.ODOTTAA_ARVIOINTIA
    }

    get showHyvaksytty() {
      return this.seurantajakso?.tila === SeurantajaksoTila.HYVAKSYTTY
    }

    get showPalautettuKorjattavaksi() {
      return this.seurantajakso?.tila === SeurantajaksoTila.PALAUTETTU_KORJATTAVAKSI
    }

    get showKouluttajaArvioinut() {
      return this.seurantajakso?.tila === SeurantajaksoTila.ODOTTAA_YHTEISIA_MERKINTOJA
    }

    get showOdottaaHyvaksyntaa() {
      return this.seurantajakso?.tila === SeurantajaksoTila.ODOTTAA_HYVAKSYNTAA
    }

    get showHuolenaiheet() {
      return this.seurantajakso?.huolenaiheet != null
    }

    get canEdit() {
      return (
        this.seurantajakso?.seurantakeskustelunYhteisetMerkinnat === null ||
        this.seurantajakso?.korjausehdotus !== null
      )
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .seurantajakso {
    max-width: 970px;
  }
</style>
