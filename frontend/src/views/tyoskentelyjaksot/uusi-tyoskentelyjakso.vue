<template>
  <div class="lisaa-tyoskentelyjakso">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-tyoskentelyjakso') }}</h1>
          <hr />
          <tyoskentelyjakso-form
            v-if="!loading && tyoskentelyjaksoLomake"
            :kunnat="kunnat"
            :erikoisalat="erikoisalat"
            :reserved-asiakirja-nimet="tyoskentelyjaksoLomake.reservedAsiakirjaNimet"
            :allow-hyvaksytty-aiemmin-toiselle-erikoisalalle-option="true"
            @submit="onSubmit"
            @cancel="onCancel"
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

  import TyoskentelyjaksoForm from '@/forms/tyoskentelyjakso-form.vue'
  import { TyoskentelyjaksoLomake, ElsaError, Tyoskentelyjakso } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      TyoskentelyjaksoForm
    }
  })
  export default class UusiTyoskentelyjakso extends Vue {
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
        text: this.$t('lisaa-tyoskentelyjakso'),
        active: true
      }
    ]
    tyoskentelyjaksoLomake: null | TyoskentelyjaksoLomake = null
    tyoskentelyjakso: Tyoskentelyjakso | null = null
    loading = true

    async mounted() {
      await this.fetchLomake()
      this.loading = false
    }

    async fetchLomake() {
      try {
        this.tyoskentelyjaksoLomake = (
          await axios.get(`erikoistuva-laakari/tyoskentelyjakso-lomake`)
        ).data
      } catch {
        toastFail(this, this.$t('tyoskentelyjakson-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async onSubmit(
      value: {
        tyoskentelyjakso: Tyoskentelyjakso
        addedFiles: File[]
        deletedAsiakirjaIds: number[]
      },
      params: { saving: boolean }
    ) {
      params.saving = true
      try {
        const formData = new FormData()
        formData.append('tyoskentelyjaksoJson', JSON.stringify(value.tyoskentelyjakso))
        value.addedFiles.forEach((file: File) => formData.append('files', file, file.name))

        const tyoskentelyjakso = (
          await axios.post('erikoistuva-laakari/tyoskentelyjaksot', formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            },
            timeout: 120000
          })
        ).data

        toastSuccess(this, this.$t('uusi-tyoskentelyjakso-lisatty'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'tyoskentelyjakso',
          params: {
            tyoskentelyjaksoId: `${tyoskentelyjakso.id}`
          }
        })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('tyoskentelyjakson-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('tyoskentelyjakson-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'tyoskentelyjaksot'
      })
    }

    get kunnat() {
      if (this.tyoskentelyjaksoLomake) {
        return this.tyoskentelyjaksoLomake.kunnat
      } else {
        return []
      }
    }

    get erikoisalat() {
      if (this.tyoskentelyjaksoLomake) {
        return this.tyoskentelyjaksoLomake.erikoisalat
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
  .lisaa-tyoskentelyjakso {
    max-width: 768px;
  }
</style>
