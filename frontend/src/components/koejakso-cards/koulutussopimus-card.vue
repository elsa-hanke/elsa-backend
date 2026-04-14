<template>
  <div class="d-flex justify-content-center border rounded pt-3 mb-4">
    <div class="container-fluid">
      <h2>{{ $t('koulutussopimus') }}</h2>
      <koejakso-card-content v-if="tila === lomaketilat.UUSI">
        <template #content>
          <span class="pr-6">{{ $t('koulutussopimus-tila-uusi') }}</span>
        </template>
        <template #button>
          <elsa-button
            v-if="!account.impersonated"
            variant="primary"
            class="mb-4"
            :to="{ name: url }"
          >
            {{ $t('tayta-koulutussopimus') }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.TALLENNETTU_KESKENERAISENA">
        <template #content>
          <div>
            <font-awesome-icon :icon="['far', 'clock']" class="text-warning mr-2" />
          </div>
          <div>
            <span class="pr-6 mb-2">
              {{ $t('koulutussopimus-tila-tallennettu-keskeneraisena') }}
            </span>
          </div>
        </template>
        <template #button>
          <elsa-button variant="primary" class="mb-4" :to="{ name: url }">
            {{
              account.impersonated ? $t('nayta-koulutussopimus') : $t('viimeistele-koulutussopimus')
            }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content
        v-if="
          tila === lomaketilat.ODOTTAA_HYVAKSYNTAA ||
          tila === lomaketilat.ODOTTAA_TOISEN_KOULUTTAJAN_HYVAKSYNTAA ||
          tila === lomaketilat.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
        "
      >
        <template #content>
          <p class="pr-6 mb-2">{{ $t('koulutussopimus-tila-odottaa-hyvaksyntaa') }}</p>
        </template>
        <template #button>
          <elsa-button variant="outline-primary" class="mb-4" :to="{ name: url }">
            {{ $t('nayta-koulutussopimus') }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.PALAUTETTU_KORJATTAVAKSI">
        <template #content>
          <div>
            <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="text-danger mr-2" />
          </div>
          <div>
            <div class="pr-6">
              <p class="mb-1">{{ $t('koulutussopimus-tila-palautettu-korjattavaksi') }}</p>
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
              account.impersonated ? $t('nayta-koulutussopimus') : $t('viimeistele-koulutussopimus')
            }}
          </elsa-button>
        </template>
      </koejakso-card-content>

      <koejakso-card-content v-if="tila === lomaketilat.HYVAKSYTTY">
        <template #content>
          <div>
            <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
          </div>
          <div>
            <span class="pr-6">
              {{ $t('koulutussopimus-tila-hyvaksytty') }}
            </span>
          </div>
        </template>
        <template #button>
          <elsa-button variant="primary" class="mb-4" :to="{ name: url }">
            {{ $t('nayta-koulutussopimus') }}
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
  export default class KoulutussopimusCard extends Vue {
    get account() {
      return store.getters['auth/account']
    }

    get koejakso() {
      return store.getters['erikoistuva/koejakso']
    }

    get tila() {
      return this.koejakso.koulutusSopimuksenTila
    }

    get korjausehdotus() {
      if (this.koejakso.koulutussopimus) {
        return (
          this.koejakso.koulutussopimus.korjausehdotus ||
          this.koejakso.koulutussopimus.vastuuhenkilonKorjausehdotus
        )
      }
      return null
    }

    get lomaketilat() {
      return LomakeTilat
    }

    get url() {
      return 'koulutussopimus-erikoistuva'
    }
  }
</script>
