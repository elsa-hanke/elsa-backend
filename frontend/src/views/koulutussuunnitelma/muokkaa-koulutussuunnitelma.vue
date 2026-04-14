<template>
  <div class="muokkaa-koulutussuunnitelma">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <div v-if="!loading" class="mb-4">
            <h2>{{ $t('henkilokohtainen-koulutussuunnitelma') }}</h2>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p v-html="$t('henkilokohtainen-koulutussuunnitelma-kuvaus', { linkki })" />
            <hr />
            <koulutussuunnitelma-form
              :value="koulutussuunnitelma"
              :reserved-asiakirja-nimet="reservedAsiakirjaNimet"
              @submit="onSubmit"
              @cancel="onCancel"
              @skipRouteExitConfirm="skipRouteExitConfirm"
            />
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
  import axios, { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { putKoulutussuunnitelma } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import KoulutussuunnitelmaForm from '@/forms/koulutussuunnitelma-form.vue'
  import { Koulutussuunnitelma, ElsaError } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      KoulutussuunnitelmaForm
    }
  })
  export default class MuokkaaKoulutussuunnitelma extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koulutussuunnitelma'),
        to: { name: 'koulutussuunnitelma' }
      },
      {
        text: this.$t('muokkaa-koulutussuunnitelma'),
        active: true
      }
    ]
    koulutussuunnitelma: null | Koulutussuunnitelma = null
    reservedAsiakirjaNimet: string[] = []
    loading = true

    async mounted() {
      await Promise.all([this.fetchKoulutussuunnitelma(), this.fetchReservedAsiakirjaNimet()])
      this.loading = false
    }

    async fetchKoulutussuunnitelma() {
      try {
        this.koulutussuunnitelma = (await axios.get(`erikoistuva-laakari/koulutussuunnitelma`)).data
      } catch {
        toastFail(this, this.$t('koulutussuunnitelman-hakeminen-epaonnistui'))
      }
    }

    async fetchReservedAsiakirjaNimet() {
      this.reservedAsiakirjaNimet = (await axios.get('erikoistuva-laakari/asiakirjat/nimet')).data
    }

    async onSubmit(data: Koulutussuunnitelma, params: { saving: boolean }) {
      params.saving = true
      try {
        await putKoulutussuunnitelma(data)
        toastSuccess(this, this.$t('koulutussuunnitelman-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'koulutussuunnitelma'
        })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('koulutussuunnitelman-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('koulutussuunnitelman-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'koulutussuunnitelma'
      })
    }

    get linkki() {
      return `<a
                href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/opinto-oppaat/"
                target="_blank"
                rel="noopener noreferrer"
              >${this.$t('henkilokohtainen-koulutussuunnitelma-linkki')}</a>`
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .muokkaa-koulutussuunnitelma {
    max-width: 1024px;
  }
</style>
