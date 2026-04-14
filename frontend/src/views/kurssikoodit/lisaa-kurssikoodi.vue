<template>
  <div class="kurssikoodi mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-kurssikoodi') }}</h1>
          <hr />
          <kurssikoodi-form
            v-if="!loading"
            :kurssikoodi="kurssikoodi"
            :tyypit="opintosuoritusTyypit"
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

  import { getOpintosuoritusTyypit, postKurssikoodi } from '@/api/virkailija'
  import KurssikoodiForm from '@/forms/kurssikoodi-form.vue'
  import { ElsaError, Kurssikoodi, OpintosuoritusTyyppi } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      KurssikoodiForm
    }
  })
  export default class LisaaKurssikoodi extends Vue {
    kurssikoodi: Kurssikoodi = {
      id: null,
      tyyppi: null,
      tunniste: null
    }

    opintosuoritusTyypit: OpintosuoritusTyyppi[] = []

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
          text: this.$t('lisaa-kurssikoodi'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchOpintosuoritusTyypit()
      this.loading = false
    }

    async fetchOpintosuoritusTyypit() {
      try {
        this.opintosuoritusTyypit = (await getOpintosuoritusTyypit()).data
      } catch (err) {
        toastFail(this, this.$t('opintosuoritus-tyyppien-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kurssikoodit' })
      }
    }

    async onSubmit(value: Kurssikoodi, params: { saving: boolean }) {
      params.saving = true
      try {
        await postKurssikoodi(value)
        toastSuccess(this, this.$t('kurssikoodin-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'kurssikoodit'
        })
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
