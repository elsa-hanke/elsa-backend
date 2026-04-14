<template>
  <div class="terveyskeskuskoulutusjakso">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('terveyskeskuskoulutusjakson-hyvaksynta') }}</h1>
          <div v-if="hyvaksynta != null">
            <b-alert :show="showSent" variant="dark">
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span>
                {{ $t('terveyskeskuskoulutusjakso-on-lahetetty-hyvaksyttavaksi') }}
              </span>
            </b-alert>
            <b-alert :show="showReturned" variant="danger" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
                </em>
                <div>
                  {{ $t('terveyskeskuskoulutusjakso-on-palautettu-muokattavaksi') }}
                  <span class="d-block">
                    {{ $t('syy') }}&nbsp;
                    <span class="font-weight-500">
                      {{
                        hyvaksynta.virkailijanKorjausehdotus != null
                          ? hyvaksynta.virkailijanKorjausehdotus
                          : hyvaksynta.vastuuhenkilonKorjausehdotus
                      }}
                    </span>
                  </span>
                </div>
              </div>
            </b-alert>
            <b-alert variant="success" :show="showAcceptedByEveryone">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                </em>
                <span>{{ $t('terveyskeskuskoulutusjakso-on-hyvaksytty') }}</span>
              </div>
            </b-alert>
            <p v-if="editable">{{ $t('terveyskeskuskoulutusjakson-hyvaksynta-kuvaus') }}</p>
            <hr />
            <terveyskeskuskoulutusjakso-form
              :hyvaksynta="hyvaksynta"
              :reserved-asiakirja-nimet-mutable="reservedAsiakirjaNimetMutable"
              :editable="editable"
              :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
              @submit="onSubmit"
              @cancel="onCancel"
            />
            <elsa-button
              v-if="!editable"
              :to="{ name: 'tyoskentelyjaksot' }"
              variant="link"
              :class="'mb-3 mr-auto font-weight-500 tyoskentelyjaksot-link'"
            >
              {{ $t('palaa-tyoskentelyjaksoihin') }}
            </elsa-button>
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

  import { getTerveyskeskuskoulutusjakso } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import TerveyskeskuskoulutusjaksoForm from '@/forms/terveyskeskuskoulutusjakso-form.vue'
  import store from '@/store'
  import {
    ElsaError,
    TerveyskeskuskoulutusjaksonHyvaksyminen,
    TerveyskeskuskoulutusjaksonHyvaksyntaForm
  } from '@/types'
  import { TerveyskeskuskoulutusjaksonTila } from '@/utils/constants'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      TerveyskeskuskoulutusjaksoForm
    }
  })
  export default class TerveyskeskuskoulutusjaksonHyvaksyntapyynto extends Vue {
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
        text: this.$t('terveyskeskuskoulutusjakson-hyvaksynta'),
        active: true
      }
    ]

    params = {
      saving: false
    }

    hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksyminen | null = null
    reservedAsiakirjaNimetMutable: string[] | undefined = []

    async mounted() {
      try {
        this.hyvaksynta = (await getTerveyskeskuskoulutusjakso()).data
        const existingFileNamesInCurrentView = this.hyvaksynta
          ? this.hyvaksynta.tyoskentelyjaksot?.flatMap((item) =>
              item.asiakirjat?.map((asiakirja) => asiakirja.nimi)
            )
          : []
        this.reservedAsiakirjaNimetMutable = (
          await axios.get('erikoistuva-laakari/asiakirjat/nimet')
        ).data?.filter((nimi: string) => !existingFileNamesInCurrentView.includes(nimi))
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('terveyskeskuskoulutusjakson-tietojen-hakeminen-epaonnistui')}: ${this.$t(
                message
              )}`
            : this.$t('terveyskeskuskoulutusjakson-tietojen-hakeminen-epaonnistui')
        )
        this.$router.replace({ name: 'tyoskentelyjaksot' })
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    get editable() {
      return (
        (this.hyvaksynta?.id == null || this.hyvaksynta?.virkailijanKuittausaika == null) &&
        !this.account.impersonated
      )
    }

    get showReturned() {
      return this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.PALAUTETTU_KORJATTAVAKSI
    }

    get showSent() {
      return (
        this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA ||
        this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
      )
    }

    get showAcceptedByEveryone() {
      return this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.HYVAKSYTTY
    }

    get asiakirjaDataEndpointUrl() {
      return 'erikoistuva-laakari/asiakirjat/'
    }
    successfulUploads: Record<number, string[]> = {}

    async onSubmit(
      submitData: {
        hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksyminen
        form: TerveyskeskuskoulutusjaksonHyvaksyntaForm
      },
      params: { saving: boolean }
    ) {
      params.saving = true
      const uploadPromises: Promise<void>[] = []

      for (const asiakirjat of submitData.form.tyoskentelyjaksoAsiakirjat) {
        for (const file of asiakirjat.addedFiles) {
          if (asiakirjat.id !== null && asiakirjat.id !== undefined) {
            const alreadyUploaded = this.successfulUploads[asiakirjat.id]?.includes(file.name)
            if (alreadyUploaded) continue // dont send again
          }
          const formData = new FormData()
          formData.append('addedFiles', file, file.name)
          asiakirjat.deletedFiles.forEach((fileId: number) =>
            formData.append('deletedFiles', String(fileId))
          )

          const uploadPromise = axios
            .put(`erikoistuva-laakari/tyoskentelyjaksot/${asiakirjat.id}/asiakirjat`, formData, {
              headers: { 'Content-Type': 'multipart/form-data' },
              timeout: 120000
            })
            .then(() => {
              if (asiakirjat.id !== undefined && asiakirjat.id !== null) {
                if (!this.successfulUploads[asiakirjat.id]) {
                  this.successfulUploads[asiakirjat.id] = []
                }
                this.successfulUploads[asiakirjat.id].push(file.name)
              }
            })
            .catch((err) => {
              const axiosError = err as AxiosError<ElsaError>
              console.error('Asiakirjan lähetysvirhe:', axiosError)

              const status = axiosError?.response?.status
              const message = axiosError?.response?.data?.message?.toLowerCase() || ''

              const isFileMissingLikely = status === 404 || status === 500

              const errorMessage = isFileMissingLikely
                ? `${this.$t('asiakirja-ei-loydy-uudelleennimeaminen-virhe')}: ${file.name}`
                : message
                ? `${this.$t('terveyskeskuskoulutusjakson-lahetys-epaonnistui')}: ${this.$t(
                    message
                  )}`
                : this.$t('terveyskeskuskoulutusjakson-lahetys-epaonnistui')

              toastFail(this, errorMessage)
              throw err
            })

          uploadPromises.push(uploadPromise)
        }
      }

      try {
        await Promise.all(uploadPromises)
      } catch {
        params.saving = false
        return
      }

      try {
        const formData = new FormData()
        if (submitData.form.laillistamispaiva != null)
          formData.append('laillistamispaiva', submitData.form.laillistamispaiva)
        if (submitData.form.laillistamispaivanLiite != null)
          formData.append(
            'laillistamispaivanLiite',
            submitData.form.laillistamispaivanLiite,
            submitData.form.laillistamispaivanLiite?.name
          )
        if (this.hyvaksynta?.id === null) {
          await axios.post(
            'erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta',
            formData,
            {
              headers: {
                'Content-Type': 'multipart/form-data'
              },
              timeout: 120000
            }
          )
        } else {
          await axios.put(
            'erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta',
            formData,
            {
              headers: {
                'Content-Type': 'multipart/form-data'
              },
              timeout: 120000
            }
          )
        }
        toastSuccess(this, this.$t('terveyskeskuskoulutusjakson-lahetys-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({ name: 'tyoskentelyjaksot' })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('terveyskeskuskoulutusjakson-lahetys-epaonnistui')}: ${this.$t(message)}`
            : this.$t('terveyskeskuskoulutusjakson-lahetys-epaonnistui')
        )
      }
      this.params.saving = false
    }

    async onCancel() {
      this.$router.push({ name: 'tyoskentelyjaksot' })
    }
  }
</script>

<style lang="scss" scoped>
  .terveyskeskuskoulutusjakso {
    max-width: 1024px;
  }

  .tyoskentelyjaksot-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
