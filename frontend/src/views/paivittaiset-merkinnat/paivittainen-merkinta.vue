<template>
  <div class="paivittainen-merkinta">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <div v-if="paivakirjamerkinta">
            <h1>{{ paivakirjamerkinta.oppimistapahtumanNimi }}</h1>
            <div class="text-preline">
              {{ paivakirjamerkinta.reflektio }}
            </div>
            <hr />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="!account.impersonated"
                :to="{ name: 'muokkaa-paivittaista-merkintaa' }"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-merkintaa') }}
              </elsa-button>
              <elsa-button
                v-if="!account.impersonated"
                :loading="deleting"
                variant="outline-danger"
                class="mb-3"
                @click="onPaivakirjamerkintaDelete"
              >
                {{ $t('poista-merkinta') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'paivittaiset-merkinnat' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 paivittaiset-merkinnat-link"
              >
                {{ $t('palaa-paivittaisiin-merkintoihin') }}
              </elsa-button>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { deletePaivittainenMerkinta, getPaivakirjamerkinta } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import store from '@/store'
  import { Paivakirjamerkinta } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class PaivittainenMerkintaView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('paivittaiset-merkinnat'),
        to: { name: 'paivittaiset-merkinnat' }
      },
      {
        text: this.$t('merkinta'),
        active: true
      }
    ]
    paivakirjamerkinta: Paivakirjamerkinta | null = null
    loading = true
    deleting = false

    async mounted() {
      try {
        this.paivakirjamerkinta = (
          await getPaivakirjamerkinta(this.$route?.params?.paivakirjamerkintaId)
        ).data
      } catch (err) {
        toastFail(this, this.$t('paivittaisen-merkinnan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'paivittaiset-merkinnat' })
      }
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async onPaivakirjamerkintaDelete() {
      if (
        await confirmDelete(
          this,
          this.$t('poista-paivittainen-merkinta') as string,
          (this.$t('paivittaisen-merkinnan') as string).toLowerCase()
        )
      ) {
        this.deleting = true
        try {
          await deletePaivittainenMerkinta(this.paivakirjamerkinta?.id)
          toastSuccess(this, this.$t('paivittainen-merkinta-poistettu'))
          this.$router.push({
            name: 'paivittaiset-merkinnat'
          })
        } catch (err) {
          toastFail(this, this.$t('paivittaisen-merkinnan-poistaminen-epaonnistui'))
        }
        this.deleting = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .paivittainen-merkinta {
    max-width: 768px;
  }

  .paivittaiset-merkinnat-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
