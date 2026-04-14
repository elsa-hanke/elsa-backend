<template>
  <div class="valmistumispyynto">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('valmistumispyynto') }}</h1>
          <div v-if="!loading">
            <div v-if="odottaaOsaamisenArviointia">
              <p class="mt-1 mb-3">
                {{ $t('valmistumispyynto-osaamisen-arviointi-ingressi-1') }}
              </p>
              <p class="mb-3">{{ $t('valmistumispyynto-osaamisen-arviointi-ingressi-2') }}</p>
              <p>{{ $t('valmistumispyynto-osaamisen-arviointi-ingressi-3') }}</p>
            </div>
            <div v-else class="mt-3">
              <b-alert :show="!hyvaksytty" variant="dark">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon
                      :icon="['fas', 'info-circle']"
                      class="text-muted text-size-md mr-2"
                    />
                  </em>
                  <div>
                    <span v-if="odottaaVirkailijanTarkastusta">
                      {{
                        $t('valmistumispyynto-osaaminen-arvioitu-odottaa-virkailijan-tarkastusta')
                      }}
                    </span>
                    <span v-else-if="odottaaHyvaksyntaa">
                      {{ $t('valmistumispyynto-osaaminen-arvioitu-odottaa-hyvaksyntaa') }}
                    </span>
                    <span v-else-if="vastuuhenkiloOsaamisenArvioijaPalauttanut">
                      {{ $t('valmistumispyynto-osaaminen-arvioitu-palautettu-erikoistujalle') }}
                    </span>
                    <span v-else-if="virkailijaPalauttanut">
                      {{
                        $t(
                          'valmistumispyynto-osaaminen-arvioitu-palautettu-erikoistujalle-virkailijan-toimesta'
                        )
                      }}
                    </span>
                    <span v-else-if="vastuuhenkiloHyvaksyjaPalauttanut">
                      {{
                        $t(
                          'valmistumispyynto-osaaminen-arvioitu-palautettu-erikoistujalle-hyvaksyjan-toimesta'
                        )
                      }}
                    </span>
                    <span
                      v-if="virkailijaPalauttanut || vastuuhenkiloHyvaksyjaPalauttanut"
                      class="d-block"
                    >
                      {{ $t('syy') }}&nbsp;
                      <span>
                        {{ korjausehdotus }}
                      </span>
                    </span>
                  </div>
                </div>
              </b-alert>
              <b-alert :show="hyvaksytty" variant="success">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                  </em>
                  <div>
                    <span>
                      {{ $t('valmistumispyynto-hyvaksytty-vastuuhenkilon-toimesta') }}
                    </span>
                  </div>
                </div>
              </b-alert>
              <elsa-button
                variant="primary"
                class="mt-2 mb-1"
                :to="{
                  name: 'valmistumispyynnot'
                }"
              >
                {{ $t('palaa-valmistumispyyntoihin') }}
              </elsa-button>
            </div>
            <hr />
            <div>
              <erikoistuva-details
                :avatar="valmistumispyynto.erikoistujanAvatar"
                :name="valmistumispyynto.erikoistujanNimi"
                :erikoisala="valmistumispyynto.erikoistujanErikoisala"
                :opiskelijatunnus="valmistumispyynto.erikoistujanOpiskelijatunnus"
                :syntymaaika="valmistumispyynto.erikoistujanSyntymaaika"
                :yliopisto="valmistumispyynto.erikoistujanYliopisto"
                :laillistamispaiva="valmistumispyynto.erikoistujanLaillistamispaiva"
                :laillistamistodistus="valmistumispyynto.erikoistujanLaillistamistodistus"
                :laillistamistodistus-nimi="valmistumispyynto.erikoistujanLaillistamistodistusNimi"
                :laillistamistodistus-tyyppi="
                  valmistumispyynto.erikoistujanLaillistamistodistusTyyppi
                "
                :opintooikeuden-myontamispaiva="valmistumispyynto.opintooikeudenMyontamispaiva"
                :asetus="valmistumispyynto.erikoistujanAsetus"
              ></erikoistuva-details>
            </div>
            <hr />
            <h2>{{ $t('osaamisen-arviointi') }}</h2>
            <div v-if="odottaaOsaamisenArviointia && valmistumispyyntoArviointienTila">
              <p class="mb-3">
                {{ $t('valmistumispyynto-osaamisen-arviointi-selite') }}
              </p>
              <div
                v-if="
                  valmistumispyyntoArviointienTila.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour ||
                  valmistumispyyntoArviointienTila.hasArvioitaviaKokonaisuuksiaWithoutArviointi
                "
                class="d-flex flex-row"
              >
                <em class="align-middle">
                  <font-awesome-icon
                    :icon="['fas', 'info-circle']"
                    class="mr-2 text-danger text-size-md"
                  />
                </em>
                <div class="mb-4">
                  <p
                    v-if="
                      valmistumispyyntoArviointienTila.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour
                    "
                    class="m-0"
                  >
                    {{ $t('valmistumispyynto-arviointeja-ei-yhtaan-vahintaan-tasolla-nelja') }}
                  </p>
                  <p
                    v-if="
                      valmistumispyyntoArviointienTila.hasArvioitaviaKokonaisuuksiaWithoutArviointi
                    "
                    class="m-0"
                  >
                    {{ $t('valmistumispyynto-arvioitavia-kokonaisuuksia-ilman-arviointia') }}
                  </p>
                </div>
              </div>
            </div>
            <b-form @submit.stop.prevent="onSubmit">
              <elsa-button
                variant="outline-primary"
                class="mt-2 mb-4"
                @click="vaihdaRooli(valmistumispyynto.opintooikeusId)"
              >
                {{ $t('nayta-erikoistujan-suoritustiedot') }}
              </elsa-button>
              <elsa-form-group
                class="mb-2"
                :label="$t('erikoistujan-osaaminen-riittavalla-tasolla-valmistumiseen')"
                :required="odottaaOsaamisenArviointia"
              >
                <template #default="{ uid }">
                  <div v-if="odottaaOsaamisenArviointia">
                    <b-form-radio-group
                      :id="uid"
                      v-model="form.osaaminenRiittavaValmistumiseen"
                      :state="validateState('osaaminenRiittavaValmistumiseen')"
                      stacked
                    >
                      <b-form-radio :value="true" @input="onSkipRouteExitConfirm">
                        <span>{{ $t('kylla') }}</span>
                      </b-form-radio>
                      <b-form-radio :value="false" @input="onSkipRouteExitConfirm">
                        <span>{{ $t('ei-osaaminen-ei-riita-valmistumiseen') }}</span>
                      </b-form-radio>
                    </b-form-radio-group>
                    <b-form-invalid-feedback
                      :id="`${uid}-feedback`"
                      :state="validateState('osaaminenRiittavaValmistumiseen')"
                    >
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </div>
                  <div v-else>
                    <span>
                      {{
                        !vastuuhenkiloOsaamisenArvioijaPalauttanut
                          ? $t('kylla')
                          : $t('ei-osaaminen-ei-riita-valmistumiseen')
                      }}
                    </span>
                  </div>
                </template>
              </elsa-form-group>
              <div
                v-if="odottaaOsaamisenArviointia && form.osaaminenRiittavaValmistumiseen === false"
              >
                <elsa-form-group
                  class="ml-4"
                  :label="$t('lisatiedot-erikoistujalle')"
                  :required="true"
                >
                  <template #default="{ uid }">
                    <b-form-textarea
                      :id="uid"
                      v-model="form.korjausehdotus"
                      :state="validateState('korjausehdotus')"
                      rows="5"
                    ></b-form-textarea>
                    <b-form-invalid-feedback
                      :id="`${uid}-feedback`"
                      :state="validateState('korjausehdotus')"
                    >
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </template>
                </elsa-form-group>
              </div>
              <elsa-form-group
                v-if="vastuuhenkiloOsaamisenArvioijaPalauttanut"
                class="mt-3"
                :label="$t('lisatiedot-erikoistujalle')"
              >
                <span>{{ valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus }}</span>
              </elsa-form-group>
              <div
                v-if="
                  !odottaaOsaamisenArviointia &&
                  vastuuhenkiloOsaamisenArvioijaKuittausOrPalautusaika
                "
                class="mt-5"
              >
                <h2 class="mb-3">{{ $t('erikoisalan-vastuuhenkilo') }}</h2>
                <b-row>
                  <b-col class="hyvaksynta-pvm col-xxl-1" lg="2">
                    <h5>{{ $t('paivays') }}</h5>
                    <p>
                      {{ vastuuhenkiloOsaamisenArvioijaKuittausOrPalautusaika }}
                    </p>
                  </b-col>
                  <b-col>
                    <h5>{{ $t('vastuuhenkilon-nimi-ja-nimike') }}</h5>
                    <p>
                      {{ vastuuhenkiloOsaamisenArvioija }}
                    </p>
                  </b-col>
                </b-row>
              </div>
              <hr v-if="odottaaOsaamisenArviointia" />
              <div v-if="odottaaOsaamisenArviointia" class="text-right">
                <elsa-button
                  variant="back"
                  :to="{
                    name: 'valmistumispyynnot'
                  }"
                >
                  {{ $t('peruuta') }}
                </elsa-button>
                <elsa-button :loading="sending" type="submit" variant="primary" class="ml-2">
                  {{ $t('tallenna-ja-laheta') }}
                </elsa-button>
              </div>
            </b-form>
          </div>
          <div v-else class="text-center mt-3">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
      <b-row>
        <elsa-form-error :active="$v.$anyError" />
      </b-row>
    </b-container>
    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('valmistumispyynto-osaamisen-arviointi-vahvistus')"
      :submit-text="$t('tallenna-ja-laheta')"
      @submit="onSend"
    />
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required, requiredIf } from 'vuelidate/lib/validators'

  import { ELSA_API_LOCATION } from '@/api'
  import {
    getValmistumispyyntoOsaamisenArviointi,
    getValmistumispyyntoArviointienTila,
    putValmistumispyynto
  } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ValmistumispyyntoMixin from '@/mixins/valmistumispyynto'
  import {
    ElsaError,
    ValmistumispyyntoArviointienTila,
    ValmistumispyyntoLomakeOsaamisenArviointi
  } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { toastSuccess, toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError,
      ErikoistuvaDetails,
      ElsaConfirmationModal
    }
  })
  export default class ValmistumispyynnonArviointi extends Mixins<ValmistumispyyntoMixin>(
    validationMixin,
    ValmistumispyyntoMixin
  ) {
    validations() {
      return {
        form: {
          osaaminenRiittavaValmistumiseen: {
            required
          },
          korjausehdotus: {
            required: requiredIf((value) => {
              return value.osaaminenRiittavaValmistumiseen === false
            })
          }
        }
      }
    }

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('valmistumispyynnot'),
        to: { name: 'valmistumispyynnot' }
      },
      {
        text: this.$t('valmistumispyynto'),
        active: true
      }
    ]

    form: ValmistumispyyntoLomakeOsaamisenArviointi = {
      id: null,
      osaaminenRiittavaValmistumiseen: null,
      korjausehdotus: null
    }

    valmistumispyyntoArviointienTila: ValmistumispyyntoArviointienTila | null = null
    loading = true
    sending = false
    skipRouteExitConfirm = true

    async mounted() {
      const valmistumispyyntoId = this.$route?.params?.valmistumispyyntoId
      if (valmistumispyyntoId) {
        try {
          await getValmistumispyyntoOsaamisenArviointi(parseInt(valmistumispyyntoId)).then(
            (response) => {
              this.valmistumispyynto = response.data
            }
          )
          if (this.odottaaOsaamisenArviointia) {
            await getValmistumispyyntoArviointienTila(parseInt(valmistumispyyntoId)).then(
              (response) => {
                this.valmistumispyyntoArviointienTila = response.data
              }
            )
          }
          this.loading = false
        } catch {
          toastFail(this, this.$t('valmistumispyynnon-hakeminen-epaonnistui'))
          this.$router.replace({ name: 'valmistumispyynnot' })
        }
      }
    }

    async vaihdaRooli(id: number | undefined) {
      if (this.odottaaOsaamisenArviointia && !this.skipRouteExitConfirm) {
        if (!(await confirmExit(this))) {
          return
        }
      }

      this.$emit('skipRouteExitConfirm', true)
      window.location.href = `${ELSA_API_LOCATION}/api/login/impersonate?opintooikeusId=${id}&originalUrl=${window.location.href}`
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      if (!this.validateForm()) return
      return this.$bvModal.show('confirm-send')
    }

    async onSend() {
      try {
        this.sending = true
        if (this.valmistumispyynto.id) {
          this.form.id = this.valmistumispyynto.id
        }
        this.valmistumispyynto = (await putValmistumispyynto(this.form)).data
        this.$emit('skipRouteExitConfirm', true)
        toastSuccess(this, this.$t('osaamisen-arviointi-tallennettu'))
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('osaamisen-arvioinnin-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('osaamisen-arvioinnin-tallentaminen-epaonnistui')
        )
      }
      this.sending = false
    }

    onSkipRouteExitConfirm() {
      this.skipRouteExitConfirm = false
      this.$emit('skipRouteExitConfirm', false)
    }
  }
</script>

<style lang="scss" scoped>
  .valmistumispyynto {
    max-width: 970px;
  }

  .hyvaksynta-pvm {
    min-width: 7rem;
  }
</style>
