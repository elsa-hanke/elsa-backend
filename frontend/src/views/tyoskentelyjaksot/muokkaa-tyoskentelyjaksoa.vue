<template>
  <div class="muokkaa-tyoskentelyjaksoa">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-tyoskentelyjaksoa') }}</h1>
          <hr />
          <tyoskentelyjakso-form
            v-if="!loading && tyoskentelyjakso && tyoskentelyjaksoLomake"
            :value="tyoskentelyjakso"
            :editing="true"
            :kunnat="tyoskentelyjaksoLomake.kunnat"
            :erikoisalat="tyoskentelyjaksoLomake.erikoisalat"
            :asiakirjat="tyoskentelyjakso.asiakirjat"
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
  import { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import {
    getTyoskentelyjakso,
    getTyoskentelyjaksoLomake,
    putTyoskentelyjakso
  } from '@/api/erikoistuva'
  import TyoskentelyjaksoForm from '@/forms/tyoskentelyjakso-form.vue'
  import { Tyoskentelyjakso, TyoskentelyjaksoLomake, ElsaError } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      TyoskentelyjaksoForm
    }
  })
  export default class MuokkaaTyoskentelyjaksoa extends Vue {
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
        text: this.$t('muokkaa-tyoskentelyjaksoa'),
        active: true
      }
    ]
    tyoskentelyjaksoLomake: null | TyoskentelyjaksoLomake = null
    tyoskentelyjakso: null | Tyoskentelyjakso = null
    loading = true

    async mounted() {
      await this.fetchTyoskentelyjakso()
      await this.fetchLomake()
      this.loading = false
    }

    async fetchLomake() {
      try {
        this.tyoskentelyjaksoLomake = (await getTyoskentelyjaksoLomake()).data
      } catch {
        toastFail(this, this.$t('tyoskentelyjakson-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async fetchTyoskentelyjakso() {
      const tyoskentelyjaksoId = this.$route?.params?.tyoskentelyjaksoId
      if (tyoskentelyjaksoId) {
        try {
          this.tyoskentelyjakso = (await getTyoskentelyjakso(tyoskentelyjaksoId)).data
        } catch {
          toastFail(this, this.$t('tyoskentelyjakson-hakeminen-epaonnistui'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.replace({ name: 'tyoskentelyjaksot' })
        }
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
        formData.append('deletedAsiakirjaIdsJson', JSON.stringify(value.deletedAsiakirjaIds))

        await putTyoskentelyjakso(formData)

        toastSuccess(this, this.$t('tyoskentelyjakson-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'tyoskentelyjakso',
          params: {
            tyoskentelyjaksoId: `${this.tyoskentelyjakso?.id}`
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

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .muokkaa-tyoskentelyjaksoa {
    max-width: 768px;
  }
</style>
