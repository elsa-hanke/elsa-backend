<template>
  <div class="muokkaa-poissaoloa">
    <b-breadcrumb :items="items" class="mb-0"></b-breadcrumb>
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-poissaoloa') }}</h1>
          <hr />
          <poissaolo-form
            v-if="!loading"
            :poissaolo="poissaolo"
            :tyoskentelyjaksot="tyoskentelyjaksot"
            :poissaolon-syyt="poissaolonSyyt"
            @submit="onSubmit"
            @delete="onDelete"
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

  import PoissaoloForm from '@/forms/poissaolo-form.vue'
  import { ElsaError, Poissaolo, PoissaoloLomake } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      PoissaoloForm
    }
  })
  export default class MuokkaaPoissaoloa extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('tyoskentelyjaksot'),
        to: { name: 'tyoskentelyjaksot' }
      },
      {
        text: this.$t('muokkaa-poissaoloa'),
        active: true
      }
    ]
    poissaoloLomake?: null | PoissaoloLomake = null
    poissaolo?: null | Poissaolo = null
    loading = true

    async mounted() {
      await Promise.all([this.fetchLomake(), this.fetchPoissaolo()])
      this.loading = false
    }

    async fetchPoissaolo() {
      const poissaoloId = this.$route?.params?.poissaoloId
      if (poissaoloId) {
        try {
          const poissaoloData = (
            await axios.get(`erikoistuva-laakari/tyoskentelyjaksot/poissaolot/${poissaoloId}`)
          ).data
          this.poissaolo = {
            ...poissaoloData,
            kokoTyoajanPoissaolo: poissaoloData.poissaoloprosentti === 100,
            tyoskentelyjakso: {
              ...poissaoloData.tyoskentelyjakso,
              label: tyoskentelyjaksoLabel(this, poissaoloData.tyoskentelyjakso)
            }
          }
        } catch {
          toastFail(this, this.$t('poissaolon-hakeminen-epaonnistui'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.replace({ name: 'tyoskentelyjaksot' })
        }
      }
    }

    async fetchLomake() {
      try {
        this.poissaoloLomake = (await axios.get(`erikoistuva-laakari/poissaolo-lomake`)).data
      } catch {
        toastFail(this, this.$t('poissaolon-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async onSubmit(poissaolo: Poissaolo, params: any) {
      params.saving = true
      try {
        this.poissaolo = (
          await axios.put('erikoistuva-laakari/tyoskentelyjaksot/poissaolot', poissaolo)
        ).data
        toastSuccess(this, this.$t('poissaolon-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'poissaolo',
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
            ? `${this.$t('poissaolon-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('poissaolon-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    async onDelete(params: any) {
      if (
        await confirmDelete(
          this,
          this.$t('poista-poissaolo') as string,
          (this.$t('poissaolon') as string).toLowerCase()
        )
      ) {
        params.deleting = true
        try {
          await axios.delete(
            `erikoistuva-laakari/tyoskentelyjaksot/poissaolot/${this.poissaolo?.id}`
          )
          toastSuccess(this, this.$t('poissaolo-poistettu-onnistuneesti'))
          this.$router.push({
            name: 'tyoskentelyjaksot'
          })
        } catch (err) {
          const axiosError = err as AxiosError<ElsaError>
          const message = axiosError?.response?.data?.message
          toastFail(
            this,
            message
              ? `${this.$t('poissaolon-poistaminen-epaonnistui')}: ${this.$t(message)}`
              : this.$t('poissaolon-poistaminen-epaonnistui')
          )
        }
        params.deleting = false
      }
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
  .muokkaa-poissaoloa {
    max-width: 768px;
  }
</style>
