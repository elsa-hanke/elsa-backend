<template>
  <div class="d-flex justify-content-center border rounded pt-3 mb-4">
    <div class="container-fluid">
      <h2>
        <span class="form-order">E</span>
        {{ $t('vastuuhenkilon-arvio-otsikko') }}
      </h2>

      <koejakso-card-content v-if="tila === lomaketilat.EI_AKTIIVINEN">
        <template #content>
          <p class="mb-2">{{ $t('arviointi-ei-viela-pyydetty') }}</p>
        </template>
        <template #button>
          <elsa-button
            v-if="!account.impersonated"
            variant="primary"
            class="mb-4"
            :disabled="true"
            :to="{ name: url }"
          >
            {{ $t('pyyda-arviointia') }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.UUSI">
        <template #content>
          <p class="mb-2">{{ $t('arviointi-ei-viela-pyydetty') }}</p>
        </template>
        <template #button>
          <elsa-button
            v-if="!account.impersonated"
            variant="primary"
            class="mb-4"
            :to="{ name: url }"
          >
            {{ $t('pyyda-arviointia') }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.PALAUTETTU_KORJATTAVAKSI">
        <template #content>
          <div>
            <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="text-danger mr-2" />
          </div>
          <div class="d-inline-flex">
            <div class="pr-6">
              <p class="mb-1">
                {{ $t('vastuuhenkilon-arvio-tila-palautettu-korjattavaksi') }}
                <span v-if="koejakso.vastuuhenkilonArvio.virkailijanKorjausehdotus != null">
                  {{ $t('virkailijan-toimesta') }}
                </span>
                <span v-else>{{ $t('vastuuhenkilon-toimesta') }}</span>
              </p>
              <p class="mb-2">
                <span>{{ $t('syy') }}</span>
                <span>&nbsp;{{ korjausehdotus }}</span>
              </p>
            </div>
          </div>
        </template>
        <template #button>
          <elsa-button variant="primary" class="mb-4" :to="{ name: url }">
            {{
              account.impersonated ? $t('nayta-arviointipyynto') : $t('viimeistele-arviointipyynto')
            }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content
        v-if="
          tila === lomaketilat.ODOTTAA_HYVAKSYNTAA ||
          tila === lomaketilat.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
        "
      >
        <template #content>
          <div v-if="waitingForVirkailija">
            <p class="pr-6 mb-2">
              {{ $t('vastuuhenkilon-arvio-tila-odottaa-hyvaksyntaa') }}
            </p>
          </div>
          <div v-else>
            <p class="pr-6 mb-2">
              {{ $t('virkailijan-arvio-tila-odottaa-hyvaksyntaa') }}
            </p>
          </div>
        </template>
        <template #button>
          <elsa-button variant="outline-primary" class="mb-4" :to="{ name: url }">
            {{ $t('nayta-arviointipyynto') }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.HYVAKSYTTY">
        <template #content>
          <div>
            <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
          </div>
          <div class="d-inline-flex">
            <span class="pr-6 mb-2">
              {{ $t('vastuuhenkilon-arvio-tila-hyvaksytty') }}
            </span>
          </div>
        </template>
        <template #button>
          <elsa-button variant="outline-primary" class="mb-4" :to="{ name: url }">
            {{ $t('nayta-arviointi') }}
          </elsa-button>
        </template>
      </koejakso-card-content>
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import KoejaksoCardContent from './koejakso-card-content.vue'

  import ElsaButton from '@/components/button/button.vue'
  import store from '@/store'
  import { LomakeTilat } from '@/utils/constants'

  @Component({
    components: {
      KoejaksoCardContent,
      ElsaButton
    }
  })
  export default class VastuuhenkilonArvioCard extends Vue {
    get account() {
      return store.getters['auth/account']
    }

    get koejakso() {
      return store.getters['erikoistuva/koejakso']
    }

    get korjausehdotus() {
      if (this.koejakso.vastuuhenkilonArvio) {
        return this.koejakso.vastuuhenkilonArvio.virkailijanKorjausehdotus != null
          ? this.koejakso.vastuuhenkilonArvio.virkailijanKorjausehdotus
          : this.koejakso.vastuuhenkilonArvio.vastuuhenkilonKorjausehdotus
      }
      return null
    }

    get tila() {
      return this.koejakso.vastuuhenkilonArvionTila
    }

    get lomaketilat() {
      return LomakeTilat
    }

    get url() {
      return 'koejakson-vastuuhenkilon-arvio'
    }

    get waitingForVirkailija() {
      return this.koejakso.vastuuhenkilonArvio?.virkailija?.sopimusHyvaksytty
    }
  }
</script>
