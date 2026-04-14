<template>
  <div class="d-flex justify-content-center border rounded pt-3 mb-4">
    <div class="container-fluid">
      <h2>
        <span class="form-order">C</span>
        {{ $t('kehittamistoimenpiteet-otsikko') }}
      </h2>

      <koejakso-card-content v-if="tila === lomaketilat.EI_AKTIIVINEN">
        <template #content>
          <p class="mb-2">{{ $t('kehittamistoimenpiteet-arviointi-ei-viela-pyydetty') }}</p>
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
          <p class="mb-2">{{ $t('kehittamistoimenpiteet-arviointi-ei-viela-pyydetty') }}</p>
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

      <koejakso-card-content v-if="tila === lomaketilat.ODOTTAA_HYVAKSYNTAA">
        <template #content>
          <p class="pr-6 mb-2">{{ $t('kehittamistoimenpiteet-tila-odottaa-hyvaksyntaa') }}</p>
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
            <span class="pr-6 mb-2">{{ $t('kehittamistoimenpiteet-tila-hyvaksytty') }}</span>
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
  export default class KehittamistoimenpiteetCard extends Vue {
    get account() {
      return store.getters['auth/account']
    }

    get koejakso() {
      return store.getters['erikoistuva/koejakso']
    }

    get tila() {
      return this.koejakso.kehittamistoimenpiteidenTila
    }

    get lomaketilat() {
      return LomakeTilat
    }

    get url() {
      return 'koejakson-kehittamistoimenpiteet'
    }
  }
</script>
