<template>
  <div class="kurssikoodi mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('kurssikoodi') }}</h1>
          <hr />
          <div v-if="!loading">
            <kurssikoodi-form :kurssikoodi="kurssikoodi" />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button :to="{ name: 'muokkaa-kurssikoodia' }" variant="primary" class="ml-2">
                {{ $t('muokkaa-kurssikoodia') }}
              </elsa-button>
              <elsa-button
                :loading="deleting"
                variant="outline-danger"
                class="ml-2"
                @click="onKurssikoodiDelete"
              >
                {{ $t('poista-kurssikoodi') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'kurssikoodit' }"
                variant="link"
                class="mr-auto font-weight-500 kurssikoodi-link"
              >
                {{ $t('palaa-kurssikoodien-yllapitoon') }}
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

  import { deleteKurssikoodi, getKurssikoodi } from '@/api/virkailija'
  import ElsaButton from '@/components/button/button.vue'
  import KurssikoodiForm from '@/forms/kurssikoodi-form.vue'
  import { Kurssikoodi } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      KurssikoodiForm
    }
  })
  export default class KurssikoodiView extends Vue {
    kurssikoodi: Kurssikoodi | null = null

    loading = true
    deleting = false

    get items() {
      return [
        {
          text: this.$t('etusivu'),
          to: { name: 'etusivu' }
        },
        {
          text: this.$t('kurssikoodien-yllapito'),
          to: { name: 'kurssikoodit' }
        },
        {
          text: this.$t('kurssikoodi'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchKurssikoodi()
      this.loading = false
    }

    async fetchKurssikoodi() {
      try {
        this.kurssikoodi = (await getKurssikoodi(this.$route?.params?.kurssikoodiId)).data
      } catch (err) {
        toastFail(this, this.$t('kurssikoodin-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kurssikoodit' })
      }
    }

    async onKurssikoodiDelete() {
      if (
        await confirmDelete(
          this,
          this.$t('poista-kurssikoodi') as string,
          (this.$t('kurssikoodin') as string).toLowerCase()
        )
      ) {
        this.deleting = true
        try {
          await deleteKurssikoodi(this.$route?.params?.kurssikoodiId)
          toastSuccess(this, this.$t('kurssikoodi-poistettu-onnistuneesti'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.push({
            name: 'kurssikoodit'
          })
        } catch {
          toastFail(this, this.$t('kurssikoodin-poistaminen-epaonnistui'))
        }
        this.deleting = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .kurssikoodi {
    max-width: 970px;
  }

  .kurssikoodi-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
