<template>
  <div class="ilmoitukset mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('julkiset-ilmoitukset') }}</h1>
          <p>{{ $t('julkiset-ilmoitukset-kuvaus') }}</p>
          <hr />
          <elsa-button variant="primary" :to="{ name: 'lisaa-ilmoitus' }" class="mb-3 ml-3">
            {{ $t('lisaa-ilmoitus') }}
          </elsa-button>
          <div v-if="!loading">
            <b-alert v-if="ilmoitukset != null && ilmoitukset.length === 0" variant="dark" show>
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span>
                {{ $t('ei-julkisia-ilmoituksia') }}
              </span>
            </b-alert>
            <b-row v-for="ilmoitus in ilmoitukset" :key="ilmoitus.id" lg>
              <b-col>
                <div class="d-flex justify-content-center border rounded pt-1 pb-1 mb-4">
                  <div class="container-fluid">
                    <!-- eslint-disable-next-line vue/no-v-html -->
                    <p class="mb-2" v-html="ilmoitus.teksti"></p>
                    <div class="d-flex flex-row-reverse flex-wrap">
                      <elsa-button
                        variant="primary"
                        :to="{ name: 'muokkaa-ilmoitusta', params: { ilmoitusId: ilmoitus.id } }"
                        class="mb-3 ml-3"
                      >
                        {{ $t('muokkaa-ilmoitusta') }}
                      </elsa-button>
                      <elsa-button
                        :loading="deleting"
                        variant="outline-danger"
                        class="ml-2 mb-3"
                        @click="poistaIlmoitus(ilmoitus.id)"
                      >
                        {{ $t('poista-ilmoitus') }}
                      </elsa-button>
                    </div>
                  </div>
                </div>
              </b-col>
            </b-row>
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

  import { getIlmoitukset } from '@/api/julkinen'
  import { deleteIlmoitus } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaTextEditor from '@/components/text-editor/text-editor.vue'
  import { Ilmoitus } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaTextEditor
    }
  })
  export default class MuokkaaIlmoituksia extends Vue {
    ilmoitukset: Ilmoitus[] | null = null

    loading = true
    deleting = false

    get items() {
      return [
        {
          text: this.$t('etusivu'),
          to: { name: 'etusivu' }
        },
        {
          text: this.$t('julkiset-ilmoitukset'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchIlmoitukset()
      this.loading = false
    }

    async fetchIlmoitukset() {
      try {
        this.ilmoitukset = (await getIlmoitukset()).data
      } catch (err) {
        toastFail(this, this.$t('ilmoituksien-hakeminen-epaonnistui'))
      }
    }

    async poistaIlmoitus(id?: number) {
      if (
        id &&
        (await confirmDelete(
          this,
          this.$t('poista-ilmoitus') as string,
          (this.$t('ilmoituksen') as string).toLowerCase()
        ))
      ) {
        try {
          this.deleting = true
          await deleteIlmoitus(id)
          toastSuccess(this, this.$t('ilmoitus-poistettu-onnistuneesti'))
          await this.fetchIlmoitukset()
        } catch {
          toastFail(this, this.$t('ilmoituksen-poistaminen-epaonnistui'))
        }
        this.deleting = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .ilmoitukset {
    max-width: 970px;
  }
</style>
