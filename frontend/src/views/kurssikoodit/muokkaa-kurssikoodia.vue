<template>
  <div class="kurssikoodi mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-kurssikoodia') }}</h1>
          <hr />
          <kurssikoodi-form
            v-if="!loading"
            :kurssikoodi="kurssikoodi"
            :editing="true"
            @submit="onSubmit"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { getKurssikoodi, putKurssikoodi } from '@/api/virkailija'
  import KurssikoodiForm from '@/forms/kurssikoodi-form.vue'
  import { ElsaError, Kurssikoodi } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      KurssikoodiForm
    }
  })
  export default class MuokkaaKurssikoodia extends Vue {
    kurssikoodi: Kurssikoodi | null = null

    loading = true

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
          text: this.$t('muokkaa-kurssikoodia'),
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

    async onSubmit(value: Kurssikoodi, params: { saving: boolean }) {
      params.saving = true
      try {
        await putKurssikoodi(value)
        toastSuccess(this, this.$t('kurssikoodin-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({ name: 'kurssikoodi' })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('kurssikoodin-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('kurssikoodin-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .kurssikoodi {
    max-width: 970px;
  }
</style>
