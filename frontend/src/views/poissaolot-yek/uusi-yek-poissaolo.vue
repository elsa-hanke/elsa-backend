<template>
  <div class="uusi-poissaolo">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-poissaolo') }}</h1>
          <p class="mb-2">{{ $t('uusi-poissaolo-ohjeteksti') }}</p>
          <hr />
          <yek-poissaolo-form
            v-if="!loading"
            :tyoskentelyjaksot="tyoskentelyjaksot"
            :poissaolon-syyt="poissaolonSyyt"
            :tyoskentelyjakso-id="tyoskentelyjaksoId"
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
  import axios, { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import YekPoissaoloForm from '@/forms/yek-poissaolo-form.vue'
  import { Poissaolo, PoissaoloLomake, ElsaError } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      YekPoissaoloForm
    }
  })
  export default class UusiYekPoissaolo extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('tyoskentelyjaksot'),
        to: { name: 'yektyoskentelyjaksot' }
      },
      {
        text: this.$t('lisaa-poissaolo'),
        active: true
      }
    ]
    poissaoloLomake: null | PoissaoloLomake = null
    poissaolo: null | Poissaolo = null
    tyoskentelyjaksoId: number | null = null
    loading = true

    async mounted() {
      await this.fetchLomake()

      const tyoskentelyjaksoId = this.$route?.params?.tyoskentelyjaksoId
      const tyoskentelyjaksoIdParsed = parseInt(tyoskentelyjaksoId)
      if (tyoskentelyjaksoIdParsed) {
        this.tyoskentelyjaksoId = tyoskentelyjaksoIdParsed
      }

      this.loading = false
    }

    async fetchLomake() {
      try {
        this.poissaoloLomake = (await axios.get(`yek-koulutettava/poissaolo-lomake`)).data
      } catch {
        toastFail(this, this.$t('poissaolon-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async onSubmit(poissaolo: Poissaolo, params: any) {
      params.saving = true
      try {
        this.poissaolo = (
          await axios.post('yek-koulutettava/tyoskentelyjaksot/poissaolot', poissaolo)
        ).data
        toastSuccess(this, this.$t('poissaolo-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'yekpoissaolo',
          params: {
            poissaoloId: `${this.poissaolo?.id}`
          }
        })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('uuden-poissaolon-lisaaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('uuden-poissaolon-lisaaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    get tyoskentelyjaksot() {
      if (this.poissaoloLomake) {
        return this.poissaoloLomake.tyoskentelyjaksot
      } else {
        return []
      }
    }

    get poissaolonSyyt() {
      if (this.poissaoloLomake) {
        return this.poissaoloLomake.poissaolonSyyt
      } else {
        return []
      }
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .uusi-poissaolo {
    max-width: 768px;
  }
</style>
