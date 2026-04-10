<template>
  <div class="suoritemerkinta">
    <b-breadcrumb :items="items" class="mb-0"></b-breadcrumb>
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('suoritemerkinta') }}</h1>
          <hr />
          <div v-if="suoritemerkinta != null">
            <suoritemerkinta-details :value="suoritemerkinta" />
            <div class="d-flex flex-row-reverse flex-wrap">
              <hr />

              <elsa-button
                v-if="!account.impersonated"
                :disabled="suoritemerkinta.lukittu"
                :to="{ name: 'muokkaa-suoritemerkintaa' }"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-merkintaa') }}
              </elsa-button>
              <elsa-button
                v-if="!account.impersonated"
                :disabled="suoritemerkinta.lukittu"
                :loading="deleting"
                variant="outline-danger"
                class="mb-3"
                @click="onSuoritemerkintaDelete"
              >
                {{ $t('poista-merkinta') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'suoritemerkinnat' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 suoritemerkinta-link"
              >
                {{ $t('palaa-suoritemerkintoihin') }}
              </elsa-button>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
      <b-row v-if="suoritemerkinta != null && suoritemerkinta.lukittu">
        <b-col>
          <div class="d-flex flex-row mb-4">
            <em class="align-middle">
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-1" />
            </em>
            <div>
              <span class="text-size-sm">{{ $t('suoritemerkinta-on-lukittu') }}</span>
            </div>
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import Vue from 'vue'
  import { Component } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import store from '@/store'
  import { Suoritemerkinta } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { vaativuustasot } from '@/utils/constants'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import SuoritemerkintaDetails from '@/views/suoritemerkinnat/suoritemerkinta-details.vue'

  @Component({
    components: {
      ElsaButton,
      SuoritemerkintaDetails
    }
  })
  export default class SuoritemerkintaView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('suoritemerkinnat'),
        to: { name: 'suoritemerkinnat' }
      },
      {
        text: this.$t('suoritemerkinta'),
        active: true
      }
    ]
    suoritemerkinta: Suoritemerkinta | null = null
    vaativuustasot = vaativuustasot
    deleting = false

    async mounted() {
      const suoritemerkintaId = this.$route?.params?.suoritemerkintaId
      if (suoritemerkintaId) {
        try {
          this.suoritemerkinta = (
            await axios.get(`erikoistuva-laakari/suoritemerkinnat/${suoritemerkintaId}`)
          ).data
        } catch {
          toastFail(this, this.$t('suoritemerkinnan-hakeminen-epaonnistui'))
          this.$router.replace({ name: 'suoritemerkinnat' })
        }
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    async onSuoritemerkintaDelete() {
      if (
        await confirmDelete(
          this,
          this.$t('poista-suoritemerkinta') as string,
          (this.$t('suoritemerkinnan') as string).toLowerCase()
        )
      ) {
        this.deleting = true
        try {
          await axios.delete(`erikoistuva-laakari/suoritemerkinnat/${this.suoritemerkinta?.id}`)
          toastSuccess(this, this.$t('suoritemerkinta-poistettu-onnistuneesti'))
          this.$router.push({
            name: 'suoritemerkinnat'
          })
        } catch {
          toastFail(this, this.$t('suoritemerkinnan-poistaminen-epaonnistui'))
        }
        this.deleting = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .suoritemerkinta {
    max-width: 970px;
  }

  .suoritemerkinta-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
