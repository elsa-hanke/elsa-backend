<template>
  <div class="seurantajakso">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row v-if="!loading && seurantajakso" lg>
        <b-col>
          <h1>{{ $t('seurantajakso') }}</h1>
          <b-alert
            :show="showOdottaaKeskustelua && !showOdottaaArviointia"
            variant="dark"
            class="mt-3"
          >
            <div class="d-flex flex-row">
              <em class="align-middle">
                <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
              </em>
              <div>
                <p class="mb-0">
                  {{ $t('seurantajakso-tila-arvioitu-1') }}
                </p>
                <p class="mt-2 mb-1">
                  {{ $t('seurantajakso-tila-arvioitu-2') }}
                </p>
                <ul>
                  <li>{{ $t('seurantajakso-tila-taydenna-tiedot-2') }}</li>
                  <li>{{ $t('seurantajakso-tila-taydenna-tiedot-3') }}</li>
                </ul>
              </div>
            </div>
          </b-alert>
          <b-alert :show="showPalautettuKorjattavaksi" variant="dark" class="mt-3">
            <div class="d-flex flex-row">
              <em class="align-middle">
                <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
              </em>
              <div>
                {{ $t('seurantajakso-palautettu-korjattavaksi') }}
                <span class="d-block">
                  {{ $t('syy') }}&nbsp;
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
            v-if="canEdit"
            :to="{ name: 'muokkaa-seurantajaksoa' }"
            variant="outline-primary"
            class="ml-md-3 mb-2 mt-2"
          >
            {{ $t('muokkaa-arviointia') }}
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
              v-if="canEdit"
              :to="{ name: 'muokkaa-seurantajaksoa' }"
              variant="primary"
              class="ml-3"
            >
              {{ $t('muokkaa-arviointia') }}
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

  import { getSeurantajakso, getSeurantajaksonTiedot } from '@/api/kouluttaja'
  import ElsaButton from '@/components/button/button.vue'
  import SeurantajaksoForm from '@/forms/seurantajakso-form.vue'
  import store from '@/store'
  import { Seurantajakso, SeurantajaksonTiedot } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SeurantajaksoForm
    }
  })
  export default class SeurantajaksoViewKouluttaja extends Vue {
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
        if (this.seurantajakso.id != null) {
          this.seurantajaksonTiedot = (await getSeurantajaksonTiedot(this.seurantajakso.id)).data
        }
      } catch {
        toastFail(this, this.$t('seurantajakson-tietojen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'seurantakeskustelut' })
      }
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    get showOdottaaKeskustelua() {
      return this.seurantajakso?.seurantakeskustelunYhteisetMerkinnat == null
    }

    get showOdottaaArviointia() {
      return this.seurantajakso?.kouluttajanArvio == null
    }

    get showPalautettuKorjattavaksi() {
      return this.seurantajakso?.korjausehdotus != null
    }

    get showHyvaksytty() {
      return this.seurantajakso?.hyvaksytty === true
    }

    get canEdit() {
      return this.seurantajakso?.hyvaksytty !== true && this.seurantajakso?.korjausehdotus == null
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
