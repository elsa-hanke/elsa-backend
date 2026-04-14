<template>
  <div class="d-flex justify-content-center border rounded pt-3 mb-4">
    <div class="container-fluid">
      <h2>
        <span class="form-order">A</span>
        {{ $t('aloituskeskustelu-otsikko') }}
      </h2>

      <koejakso-card-content v-if="tila === lomaketilat.UUSI">
        <template #content>
          <p class="mb-2">{{ $t('lomake-ei-taytetty') }}</p>
        </template>
        <template #button>
          <elsa-button
            v-if="!account.impersonated"
            variant="primary"
            class="mb-4"
            :to="{ name: url }"
          >
            {{ $t('tayta-aloituskeskustelu') }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.TALLENNETTU_KESKENERAISENA">
        <template #content>
          <div>
            <font-awesome-icon :icon="['far', 'clock']" class="text-warning mr-2" />
          </div>
          <div class="d-inline-flex">
            <span class="pr-6 mb-2">
              {{ $t('aloituskeskustelu-tila-tallennettu-keskeneraisena') }}
            </span>
          </div>
        </template>
        <template #button>
          <elsa-button variant="primary" class="mb-4" :to="{ name: url }">
            {{
              account.impersonated
                ? $t('nayta-aloituskeskustelu')
                : $t('viimeistele-aloituskeskustelu')
            }}
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
              <p class="mb-1">{{ $t('aloituskeskustelu-tila-palautettu-korjattavaksi') }}</p>
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
              account.impersonated
                ? $t('nayta-aloituskeskustelu')
                : $t('viimeistele-aloituskeskustelu')
            }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content
        v-if="
          tila === lomaketilat.ODOTTAA_HYVAKSYNTAA ||
          tila === lomaketilat.ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
        "
      >
        <template #content>
          <p class="pr-6 mb-2">{{ $t('aloituskeskustelu-tila-odottaa-hyvaksyntaa') }}</p>
        </template>
        <template #button>
          <elsa-button variant="outline-primary" class="mb-4" :to="{ name: url }">
            {{ $t('nayta-aloituskeskustelu') }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.HYVAKSYTTY">
        <template #content>
          <div>
            <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
          </div>
          <div class="d-inline-flex">
            <span class="pr-6 mb-2">{{ $t('aloituskeskustelu-tila-hyvaksytty') }}</span>
          </div>
        </template>
        <template #button>
          <elsa-button variant="outline-primary" class="mb-4" :to="{ name: url }">
            {{ $t('nayta-aloituskeskustelu') }}
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
  export default class AloituskeskusteluCard extends Vue {
    get account() {
      return store.getters['auth/account']
    }

    get koejakso() {
      return store.getters['erikoistuva/koejakso']
    }

    get tila() {
      return this.koejakso.aloituskeskustelunTila
    }

    get korjausehdotus() {
      if (this.koejakso.aloituskeskustelu) {
        return this.koejakso.aloituskeskustelu.korjausehdotus
      }
      return null
    }

    get lomaketilat() {
      return LomakeTilat
    }

    get url() {
      return 'koejakson-aloituskeskustelu'
    }
  }
</script>
