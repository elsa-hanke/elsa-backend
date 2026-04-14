<template>
  <div class="muokkaa-tyoskentelyjaksoa">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-tyoskentelyjaksoa') }}</h1>
          <hr />
          <yek-tyoskentelyjakso-form
            v-if="!loading && tyoskentelyjakso && tyoskentelyjaksoLomake"
            :value="tyoskentelyjakso"
            :editing="true"
            :kunnat="tyoskentelyjaksoLomake.kunnat"
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
    putKoulutettavaLaillistamispaiva,
    putTyoskentelyjakso
  } from '@/api/yek-koulutettava'
  import YekTyoskentelyjaksoForm from '@/forms/yek-tyoskentelyjakso-form.vue'
  import {
    Tyoskentelyjakso,
    TyoskentelyjaksoLomake,
    ElsaError,
    LaillistamistiedotLomakeKoulutettava
  } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      YekTyoskentelyjaksoForm
    }
  })
  export default class MuokkaaYekTyoskentelyjaksoa extends Vue {
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
        text: this.$t('muokkaa-yek-tyoskentelyjaksoa'),
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
          this.$router.replace({ name: 'yektyoskentelyjaksot' })
        }
      }
    }

    async onSubmit(
      value: {
        tyoskentelyjakso: Tyoskentelyjakso
        addedFiles: File[]
        deletedAsiakirjaIds: number[]
        laillistamistiedot: LaillistamistiedotLomakeKoulutettava
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
          name: 'yektyoskentelyjakso',
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

      if (value.laillistamistiedot.ensimmainenTyoskentelyjakso) {
        try {
          await putKoulutettavaLaillistamispaiva(value.laillistamistiedot)
        } catch (err) {
          const axiosError = err as AxiosError<ElsaError>
          const message = axiosError?.response?.data?.message
          toastFail(
            this,
            message
              ? `${this.$t('yek.laillistamistietojen-tallennus-epaonnistui')}: ${this.$t(message)}`
              : this.$t('yek.laillistamistietojen-tallennus-epaonnistui')
          )
        }
      }

      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'yektyoskentelyjaksot'
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
