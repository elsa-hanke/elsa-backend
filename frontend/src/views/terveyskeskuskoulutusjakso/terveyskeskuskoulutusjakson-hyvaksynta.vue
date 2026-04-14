<template>
  <div class="terveyskeskuskoulutusjakso">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>
            {{
              onkoYek
                ? $t('yek.terveyskeskuskoulutusjakson-hyvaksynta')
                : $t('terveyskeskuskoulutusjakson-hyvaksynta')
            }}
          </h1>
          <div v-if="hyvaksynta != null">
            <b-alert :show="showReturned" variant="dark">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-2" />
                </em>
                <div>
                  <span v-if="onkoYek">
                    {{
                      $t(
                        'yek.terveyskeskuskoulutusjakso-on-palautettu-koulutettavalle-muokattavaksi'
                      )
                    }}
                  </span>
                  <span v-else>
                    {{
                      $t('terveyskeskuskoulutusjakso-on-palautettu-erikoistujalle-muokattavaksi')
                    }}
                  </span>
                  <span class="d-block">
                    {{ $t('syy') }}&nbsp;
                    <span class="font-weight-500">
                      {{ hyvaksynta.vastuuhenkilonKorjausehdotus }}
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
            <div v-if="editable">
              <span v-if="onkoYek">
                {{ $t('yek.terveyskeskuskoulutusjakson-hyvaksynta-vastuuhenkilo-kuvaus') }}
              </span>
              <span v-else>
                {{ $t('terveyskeskuskoulutusjakson-hyvaksynta-vastuuhenkilo-kuvaus') }}
              </span>

              <div v-show="hyvaksynta.lisatiedotVirkailijalta">
                <p class="mb-2">
                  <strong>{{ $t('lisatiedot-virkailijalta') }}:</strong>
                  {{ hyvaksynta.lisatiedotVirkailijalta }}
                </p>
              </div>
            </div>
            <elsa-button
              v-if="!editable"
              :to="{ name: 'terveyskeskuskoulutusjaksot', hash: hash }"
              variant="primary"
            >
              {{ $t('palaa-terveyskeskuskoulutusjaksoihin') }}
            </elsa-button>
            <hr />
            <terveyskeskuskoulutusjakso-form
              :hyvaksynta="hyvaksynta"
              :editable="editable"
              :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
              :yek="onkoYek"
              @submit="onSubmit"
              @cancel="onCancel"
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
  import { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { getTerveyskeskuskoulutusjakso, putTerveyskeskuskoulutusjakso } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import TerveyskeskuskoulutusjaksoForm from '@/forms/terveyskeskuskoulutusjakso-form.vue'
  import { ElsaError, TerveyskeskuskoulutusjaksonHyvaksyminen } from '@/types'
  import { ERIKOISALA_YEK_ID, TerveyskeskuskoulutusjaksonTila } from '@/utils/constants'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      TerveyskeskuskoulutusjaksoForm
    }
  })
  export default class TerveyskeskuskoulutusjaksonHyvaksynta extends Vue {
    get items() {
      return [
        {
          text: this.$t('etusivu'),
          to: { name: 'etusivu' }
        },
        {
          text: this.$t('terveyskeskuskoulutusjaksot'),
          to: { name: 'terveyskeskuskoulutusjaksot' }
        },
        {
          text: this.onkoYek
            ? this.$t('yek.terveyskeskuskoulutusjakson-hyvaksynta')
            : this.$t('terveyskeskuskoulutusjakson-hyvaksynta'),
          active: true
        }
      ]
    }

    params = {
      saving: false
    }

    hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksyminen | null = null

    async mounted() {
      try {
        this.hyvaksynta = (
          await getTerveyskeskuskoulutusjakso(this.$route.params.terveyskeskuskoulutusjaksoId)
        ).data
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
        this.$router.replace({ name: 'terveyskeskuskoulutusjaksot', hash: this.hash })
      }
    }

    get editable() {
      return (
        this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
      )
    }

    get showReturned() {
      return this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.PALAUTETTU_KORJATTAVAKSI
    }

    get showAcceptedByEveryone() {
      return this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.HYVAKSYTTY
    }

    get asiakirjaDataEndpointUrl() {
      return 'vastuuhenkilo/terveyskeskuskoulutusjakso/tyoskentelyjakso-liite'
    }

    async onSubmit(formData: { korjausehdotus: string }) {
      this.params.saving = true

      try {
        await putTerveyskeskuskoulutusjakso(
          this.$route.params.terveyskeskuskoulutusjaksoId,
          formData.korjausehdotus
        )

        toastSuccess(
          this,
          formData.korjausehdotus != null
            ? this.$t('terveyskeskuskoulutusjakso-palautettu-muokattavaksi')
            : this.$t('terveyskeskuskoulutusjakso-hyvaksytty')
        )

        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({ name: 'terveyskeskuskoulutusjaksot', hash: this.hash })
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
      this.$router.push({ name: 'terveyskeskuskoulutusjaksot', hash: this.hash })
    }

    get onkoYek() {
      return this.hyvaksynta?.erikoisalaId === ERIKOISALA_YEK_ID
    }

    get hash() {
      return this.onkoYek ? '#yek' : '#erikoislaakarikoulutus'
    }
  }
</script>

<style lang="scss" scoped>
  .terveyskeskuskoulutusjakso {
    max-width: 1024px;
  }
</style>
