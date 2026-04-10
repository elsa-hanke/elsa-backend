<template>
  <div class="valmistumispyynto">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <h1>{{ $t('valmistumispyynto') }}</h1>
      <div v-if="!loading">
        <div v-if="showValmistumispyyntoForm">
          <b-row lg>
            <b-col>
              <b-alert :show="valmistumispyyntoPalautettu" variant="danger" class="mt-2">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
                  </em>
                  <div>
                    <span v-if="vastuuhenkiloOsaamisenArvioijaPalauttanut">
                      {{ $t('valmistumispyynto-osaamisen-arvioija-palauttanut') }}
                    </span>
                    <span v-else-if="virkailijaPalauttanut">
                      {{ $t('valmistumispyynto-virkailija-palauttanut') }}
                    </span>
                    <span v-else>
                      {{ $t('valmistumispyynto-hyvaksyja-palauttanut') }}
                    </span>
                    <span v-if="!vastuuhenkiloOsaamisenArvioijaPalauttanut" class="d-block">
                      {{ $t('syy') }}&nbsp;
                      <span class="font-weight-500">{{ korjausehdotus }}</span>
                    </span>
                    <span v-else class="d-block">
                      <div class="font-weight-700 mt-1">
                        {{ $t('osaaminen-ei-riita-valmistumiseen') }}
                      </div>
                      <span class="font-weight-500">{{ korjausehdotus }}</span>
                    </span>
                  </div>
                </div>
              </b-alert>
              <p class="mt-3 mb-3">
                {{ $t('yek.valmistumispyynto-ingressi-hae-koulutustodistusta-kappale') }}
              </p>
              <p class="mb-3">
                {{ $t('yek.valmistumispyynto-ingressi-opintohallinto-tarkastaa-kappale') }}
              </p>
              <p class="mb-3">
                {{ $t('yek.valmistumispyynto-ingressi-kun-koulutuskokonaisuus-arvioitu-kappale') }}
              </p>
              <elsa-form-group :label="$t('valmistumispyynto-ennen-lahettamista')" :required="true">
                <template #label-right>
                  <b-form-checkbox
                    v-model="valmistumispyyntoVaatimuksetLomake.tyoskentelyjaksot"
                    :state="validateVaatimuksetState('tyoskentelyjaksot')"
                    :disabled="vaatimuksetHyvaksytty"
                    class="py-0 mt-3"
                    @input="$emit('skipRouteExitConfirm', false)"
                  >
                    <span>
                      {{ $t('valmistumispyynto-tyoskentelyjaksot-suoritettu') }}
                      <a
                        href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/opinto-oppaat/"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {{ $t('opinto-oppaasta') }}.
                      </a>
                    </span>
                  </b-form-checkbox>
                  <b-form-checkbox
                    v-model="valmistumispyyntoVaatimuksetLomake.tyotodistukset"
                    :state="validateVaatimuksetState('tyotodistukset')"
                    :disabled="vaatimuksetHyvaksytty"
                    class="py-0 mt-3"
                    @input="$emit('skipRouteExitConfirm', false)"
                  >
                    {{ $t('valmistumispyynto-tyotodistukset') }}
                  </b-form-checkbox>
                  <b-form-checkbox
                    v-model="valmistumispyyntoVaatimuksetLomake.teoriakoulutus"
                    :state="validateVaatimuksetState('teoriakoulutus')"
                    :disabled="vaatimuksetHyvaksytty"
                    class="py-0 mt-3"
                    @input="$emit('skipRouteExitConfirm', false)"
                  >
                    {{ $t('yek.valmistumispyynto-teoriakoulutusta-riittavasti') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
              <hr />
              <div v-if="!vaatimuksetHyvaksytty" class="text-right mr-2">
                <elsa-button
                  :disabled="!getValmistumispyyntoVaatimuksetChecked()"
                  variant="primary"
                  @click="confirmVaatimukset"
                >
                  {{ $t('yek.laheta-valmistumispyynto') }}
                </elsa-button>
              </div>
              <div v-else-if="valmistumispyyntoSuoritustenTila">
                <erikoistuva-details
                  :avatar="account.avatar"
                  :name="`${account.firstName} ${account.lastName}`"
                  :erikoisala="account.erikoistuvaLaakari.erikoisalaNimi"
                  :syntymaaika="account.erikoistuvaLaakari.syntymaaika"
                  :opiskelijatunnus="opiskelijatunnus"
                  :yliopisto="account.erikoistuvaLaakari.yliopisto"
                  :laillistamispaiva="valmistumispyynto.erikoistujanLaillistamispaiva"
                  :laillistamistodistus="valmistumispyynto.erikoistujanLaillistamistodistus"
                  :laillistamistodistus-nimi="
                    valmistumispyynto.erikoistujanLaillistamistodistusNimi
                  "
                  :laillistamistodistus-tyyppi="
                    valmistumispyynto.erikoistujanLaillistamistodistusTyyppi
                  "
                  :laillistamisen-muokkaus-sallittu="true"
                  :yek="true"
                  @muokkaaLaillistamista="muokkaaLaillistamista"
                />
                <div v-if="laillistaminenMuokattavissa">
                  <elsa-form-group
                    class="col-xs-12 col-sm-3 pl-0"
                    :label="$t('laillistamispaiva')"
                    :required="true"
                  >
                    <template #default="{ uid }">
                      <elsa-form-datepicker
                        :id="uid"
                        ref="laillistamispaiva"
                        :value.sync="valmistumispyyntoLomake.laillistamispaiva"
                        @input="$emit('skipRouteExitConfirm', false)"
                      ></elsa-form-datepicker>
                      <b-form-invalid-feedback>
                        {{ $t('pakollinen-tieto') }}
                      </b-form-invalid-feedback>
                    </template>
                  </elsa-form-group>
                  <elsa-form-group :label="$t('laillistamispaivan-liitetiedosto')" :required="true">
                    <span>
                      {{ $t('lisaa-liite-joka-todistaa-laillistamispaivan') }}
                    </span>
                    <asiakirjat-upload
                      class="mt-3"
                      :is-primary-button="false"
                      :allow-multiples-files="false"
                      :button-text="$t('lisaa-liitetiedosto')"
                      :disabled="laillistamispaivaAsiakirjat.length > 0"
                      @selectedFiles="onLaillistamistodistusFilesAdded"
                    />
                    <asiakirjat-content
                      :asiakirjat="laillistamispaivaAsiakirjat"
                      :sorting-enabled="false"
                      :pagination-enabled="false"
                      :enable-search="false"
                      :enable-delete="true"
                      :no-results-info-text="$t('ei-liitetiedostoja')"
                      :state="validateValmistumispyyntoState('laillistamistodistus')"
                      @deleteAsiakirja="onDeletelaillistamistodistus"
                    />
                    <b-form-invalid-feedback
                      :state="validateValmistumispyyntoState('laillistamistodistus')"
                    >
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </elsa-form-group>
                  <hr />
                </div>
                <hr />
                <div>
                  <b-row>
                    <b-col lg="4">
                      <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
                        <template #default="{ uid }">
                          <b-form-input
                            :id="uid"
                            v-model="valmistumispyyntoLomake.erikoistujanSahkoposti"
                            :state="validateValmistumispyyntoState('erikoistujanSahkoposti')"
                            @input="$emit('skipRouteExitConfirm', false)"
                          />
                          <b-form-invalid-feedback
                            v-if="
                              $v.valmistumispyyntoLomake.erikoistujanSahkoposti &&
                              !$v.valmistumispyyntoLomake.erikoistujanSahkoposti.required
                            "
                            :id="`${uid}-feedback`"
                          >
                            {{ $t('pakollinen-tieto') }}
                          </b-form-invalid-feedback>
                          <b-form-invalid-feedback
                            v-if="
                              $v.valmistumispyyntoLomake.erikoistujanSahkoposti &&
                              !$v.valmistumispyyntoLomake.erikoistujanSahkoposti.email
                            "
                            :id="`${uid}-feedback`"
                            :state="validateValmistumispyyntoState('erikoistujanSahkoposti')"
                          >
                            {{ $t('sahkopostiosoite-ei-kelvollinen') }}
                          </b-form-invalid-feedback>
                        </template>
                      </elsa-form-group>
                    </b-col>

                    <b-col lg="4">
                      <elsa-form-group :label="$t('matkapuhelinnumero')" :required="true">
                        <template #default="{ uid }">
                          <b-form-input
                            :id="uid"
                            v-model="valmistumispyyntoLomake.erikoistujanPuhelinnumero"
                            :state="validateValmistumispyyntoState('erikoistujanPuhelinnumero')"
                            @input="$emit('skipRouteExitConfirm', false)"
                          />
                          <small class="form-text text-muted">
                            {{ $t('syota-puhelinnumero-muodossa') }}
                          </small>
                          <b-form-invalid-feedback :id="`${uid}-feedback`">
                            {{ $t('tarkista-puhelinnumeron-muoto') }}
                          </b-form-invalid-feedback>
                        </template>
                      </elsa-form-group>
                    </b-col>
                  </b-row>
                  <hr />
                </div>
                <div
                  v-if="
                    !valmistumispyynto.erikoistujanLaillistamispaiva ||
                    !valmistumispyynto.erikoistujanLaillistamistodistus
                  "
                >
                  <h3>
                    {{ $t('laillistaminen') }}
                  </h3>
                  <elsa-form-group
                    class="col-xs-12 col-sm-3 pl-0"
                    :label="$t('laillistamispaiva')"
                    :required="true"
                  >
                    <template #default="{ uid }">
                      <elsa-form-datepicker
                        :id="uid"
                        ref="laillistamispaiva"
                        :value.sync="valmistumispyyntoLomake.laillistamispaiva"
                        @input="$emit('skipRouteExitConfirm', false)"
                      ></elsa-form-datepicker>
                      <b-form-invalid-feedback>
                        {{ $t('pakollinen-tieto') }}
                      </b-form-invalid-feedback>
                    </template>
                  </elsa-form-group>
                  <elsa-form-group :label="$t('laillistamispaivan-liitetiedosto')" :required="true">
                    <span>
                      {{ $t('lisaa-liite-joka-todistaa-laillistamispaivan') }}
                    </span>
                    <asiakirjat-upload
                      class="mt-3"
                      :is-primary-button="false"
                      :allow-multiples-files="false"
                      :button-text="$t('lisaa-liitetiedosto')"
                      :disabled="laillistamispaivaAsiakirjat.length > 0"
                      @selectedFiles="onLaillistamistodistusFilesAdded"
                    />
                    <asiakirjat-content
                      :asiakirjat="laillistamispaivaAsiakirjat"
                      :sorting-enabled="false"
                      :pagination-enabled="false"
                      :enable-search="false"
                      :enable-delete="true"
                      :no-results-info-text="$t('ei-liitetiedostoja')"
                      :state="validateValmistumispyyntoState('laillistamistodistus')"
                      @deleteAsiakirja="onDeletelaillistamistodistus"
                    />
                    <b-form-invalid-feedback
                      :state="validateValmistumispyyntoState('laillistamistodistus')"
                    >
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </elsa-form-group>
                  <hr />
                </div>
                <div v-if="hasVanhentuneitaSuorituksia">
                  <h3>{{ $t('vanhat-suoritukset') }}</h3>
                  <span
                    v-if="
                      valmistumispyyntoSuoritustenTila.vanhojaTyoskentelyjaksojaOrSuorituksiaExists
                    "
                  >
                    {{
                      $t('yek.valmistumispyynto-vanhoja-tyoskentelyjaksoja', {
                        vanhentunutSuoritusVuotta
                      })
                    }}
                  </span>
                  <span v-if="valmistumispyyntoSuoritustenTila.kuulusteluVanhentunut">
                    {{ $t('valmistumispyynto-valtakunnallinen-kuulustelu-yli-4-vuotta') }}
                  </span>
                  <elsa-form-group
                    :label="$t('selvitys-vanhentuneista-suorituksista-otsikko')"
                    :required="true"
                    class="mt-4"
                  >
                    <template #default="{ uid }">
                      <b-form-textarea
                        :id="uid"
                        v-model="valmistumispyyntoLomake.selvitysVanhentuneistaSuorituksista"
                        :state="
                          validateValmistumispyyntoState('selvitysVanhentuneistaSuorituksista')
                        "
                        rows="5"
                        @input="$emit('skipRouteExitConfirm', false)"
                      ></b-form-textarea>
                      <b-form-invalid-feedback :id="`${uid}-feedback`">
                        {{ $t('pakollinen-tieto') }}
                      </b-form-invalid-feedback>
                    </template>
                  </elsa-form-group>
                  <hr />
                </div>
                <b-row>
                  <b-col lg="8">
                    <h3>{{ $t('yek.yek-koulutuksen-vastuuhenkilo') }}</h3>
                    <h5>{{ $t('erikoisala-vastuuhenkilö-label') }}</h5>
                    {{ vastuuhenkiloHyvaksyja }}
                  </b-col>
                </b-row>
                <hr />
                <b-row>
                  <b-col class="text-right">
                    <elsa-button variant="back" @click="onCancel">
                      {{ $t('peruuta') }}
                    </elsa-button>
                    <elsa-button
                      v-if="!loading"
                      :loading="sending"
                      variant="primary"
                      class="ml-2"
                      style="min-width: 14rem"
                      @click="onValidateAndConfirmSend('confirm-send')"
                    >
                      {{ $t('yek.laheta-valmistumispyynto') }}
                    </elsa-button>
                  </b-col>
                </b-row>
              </div>
            </b-col>
          </b-row>
          <b-row>
            <elsa-form-error :active="$v.$anyError" />
          </b-row>
        </div>
        <div v-else>
          <valmistumispyynnon-tila-koulutettava :valmistumispyynto="valmistumispyynto" />
        </div>
      </div>
      <div v-else class="text-center mt-3">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>

    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('yek.valmistumispyynto-vahvistus')"
      :submit-text="$t('yek.laheta-valmistumispyynto')"
      @submit="onSend"
    />
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { requiredIf, required, email } from 'vuelidate/lib/validators'

  import {
    getYekValmistumispyynto,
    getYekValmistumispyyntoSuoritustenTila,
    postYekValmistumispyynto,
    putYekValmistumispyynto
  } from '@/api/yek-koulutettava'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import ValmistumispyyntoMixin from '@/mixins/valmistumispyynto'
  import store from '@/store'
  import {
    ValmistumispyyntoLomakeErikoistuja,
    ValmistumispyyntoSuoritustenTila,
    ElsaError,
    Asiakirja,
    ValmistumispyyntoVaatimuksetLomakeYek
  } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { ErikoisalaTyyppi, ValmistumispyynnonTila, phoneNumber } from '@/utils/constants'
  import { mapFile, mapFiles } from '@/utils/fileMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import ValmistumispyynnonTilaKoulutettava from '@/views/valmistumispyynnot-yek/koulutettava/valmistumispyynnon-tila-koulutettava.vue'

  @Component({
    components: {
      ValmistumispyynnonTilaKoulutettava,
      AsiakirjatContent,
      AsiakirjatUpload,
      ElsaButton,
      ErikoistuvaDetails,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormDatepicker,
      ElsaPopover,
      ElsaConfirmationModal
    }
  })
  export default class ValmistumispyyntoYekKoulutettava extends Mixins<ValmistumispyyntoMixin>(
    validationMixin,
    ValmistumispyyntoMixin
  ) {
    $refs!: {
      laillistamispaiva: ElsaFormDatepicker
    }

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('valmistumispyynto'),
        active: true
      }
    ]

    valmistumispyyntoSuoritustenTila: Partial<ValmistumispyyntoSuoritustenTila> = {}
    valmistumispyyntoVaatimuksetLomake: Partial<ValmistumispyyntoVaatimuksetLomakeYek> = {}
    valmistumispyyntoLomake: ValmistumispyyntoLomakeErikoistuja = {
      selvitysVanhentuneistaSuorituksista: null,
      laillistamispaiva: null,
      laillistamistodistus: null,
      erikoistujanPuhelinnumero: null,
      erikoistujanSahkoposti: null
    }
    vaatimuksetHyvaksytty = false
    loading = true
    sending = false
    laillistamispaivaAsiakirjat: Asiakirja[] = []
    laillistaminenMuokattavissa = false

    validations() {
      return {
        valmistumispyyntoVaatimuksetLomake: {
          tyoskentelyjaksot: { checked: (value: boolean) => value },
          tyotodistukset: { checked: (value: boolean) => value },
          teoriakoulutus: { checked: (value: boolean) => value }
        },
        valmistumispyyntoLomake: {
          erikoistujanSahkoposti: {
            required,
            email
          },
          erikoistujanPuhelinnumero: {
            required,
            phoneNumber
          },
          selvitysVanhentuneistaSuorituksista: {
            required: requiredIf(() => this.hasVanhentuneitaSuorituksia)
          },
          laillistamispaiva: {
            required
          },
          laillistamistodistus: {
            required: requiredIf(() => {
              return this.laillistamispaivaAsiakirjat.length === 0
            })
          }
        }
      }
    }

    async mounted() {
      try {
        this.valmistumispyynto = (await getYekValmistumispyynto()).data
        if (this.showValmistumispyyntoForm) {
          this.valmistumispyyntoSuoritustenTila = (
            await getYekValmistumispyyntoSuoritustenTila()
          ).data
          this.initLaillistamispaivaAndTodistus()
          this.valmistumispyyntoLomake.erikoistujanSahkoposti =
            this.account.erikoistuvaLaakari.sahkoposti
          this.valmistumispyyntoLomake.erikoistujanPuhelinnumero =
            this.account.erikoistuvaLaakari.puhelinnumero
          this.valmistumispyyntoLomake.selvitysVanhentuneistaSuorituksista =
            this.valmistumispyynto.selvitysVanhentuneistaSuorituksista || ''
        }
        this.loading = false
      } catch (err) {
        toastFail(this, this.$t('valmistumispyynnon-hakeminen-epaonnistui'))
      }
    }

    getValmistumispyyntoVaatimuksetChecked() {
      return (
        this.valmistumispyyntoVaatimuksetLomake.teoriakoulutus &&
        this.valmistumispyyntoVaatimuksetLomake.tyoskentelyjaksot &&
        this.valmistumispyyntoVaatimuksetLomake.tyotodistukset
      )
    }

    get account() {
      return store.getters['auth/account']
    }

    get vanhentunutSuoritusVuotta() {
      return this.valmistumispyyntoSuoritustenTila.erikoisalaTyyppi === ErikoisalaTyyppi.LAAKETIEDE
        ? 10
        : 6
    }

    get hasVanhentuneitaSuorituksia() {
      return (
        this.valmistumispyyntoSuoritustenTila.vanhojaTyoskentelyjaksojaOrSuorituksiaExists ||
        this.valmistumispyyntoSuoritustenTila.kuulusteluVanhentunut
      )
    }

    get opiskelijatunnus() {
      return this.account.erikoistuvaLaakari.opiskelijatunnus ?? ''
    }

    get showValmistumispyyntoForm() {
      return (
        this.valmistumispyynto?.tila === ValmistumispyynnonTila.UUSI ||
        this.valmistumispyynto?.tila ===
          ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU ||
        this.valmistumispyynto?.tila === ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU ||
        this.valmistumispyynto?.tila === ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
      )
    }

    validateVaatimuksetState(name: string) {
      const { $dirty, $error } = this.$v.valmistumispyyntoVaatimuksetLomake[name] as any
      return $dirty ? !$error : null
    }

    validateValmistumispyyntoState(name: string) {
      const { $dirty, $error } = this.$v.valmistumispyyntoLomake[name] as any
      return $dirty ? !$error : null
    }

    confirmVaatimukset() {
      this.$v.valmistumispyyntoVaatimuksetLomake.$touch()
      this.vaatimuksetHyvaksytty = !this.$v.$anyError
    }

    onValidateAndConfirmSend(modalId: string) {
      const validations = [
        this.validateForm(),
        this.$refs.laillistamispaiva ? this.$refs.laillistamispaiva.validateForm() : true
      ]
      if (validations.includes(false)) return

      return this.$bvModal.show(modalId)
    }

    validateForm() {
      this.$v.$touch()
      return !this.$v.$anyError
    }

    initLaillistamispaivaAndTodistus() {
      if (this.valmistumispyynto?.erikoistujanLaillistamistodistus) {
        const data = Uint8Array.from(
          atob(this.valmistumispyynto?.erikoistujanLaillistamistodistus),
          (c) => c.charCodeAt(0)
        )
        this.laillistamispaivaAsiakirjat.push(
          mapFile(
            new File([data], this.valmistumispyynto?.erikoistujanLaillistamistodistusNimi || '', {
              type: this.valmistumispyynto?.erikoistujanLaillistamistodistusTyyppi
            })
          )
        )
      }
      if (this.valmistumispyynto?.erikoistujanLaillistamispaiva) {
        this.valmistumispyyntoLomake.laillistamispaiva =
          this.valmistumispyynto.erikoistujanLaillistamispaiva
      }
    }

    async onSend() {
      try {
        this.sending = true
        this.valmistumispyynto = this.valmistumispyyntoPalautettu
          ? (await putYekValmistumispyynto(this.valmistumispyyntoLomake)).data
          : (await postYekValmistumispyynto(this.valmistumispyyntoLomake)).data
        const account = store.getters['auth/account']
        account.erikoistuvaLaakari.sahkoposti = this.valmistumispyyntoLomake.erikoistujanSahkoposti
        account.email = this.valmistumispyyntoLomake.erikoistujanSahkoposti
        account.erikoistuvaLaakari.puhelinnumero =
          this.valmistumispyyntoLomake.erikoistujanPuhelinnumero
        account.phoneNumber = this.valmistumispyyntoLomake.erikoistujanPuhelinnumero
        this.$emit('skipRouteExitConfirm', true)
        toastSuccess(this, this.$t('valmistumispyynto-lahetetty-onnistuneesti'))
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('valmistumispyynnon-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('valmistumispyynnon-tallentaminen-epaonnistui')
        )
      }
      this.sending = false
    }

    async onCancel() {
      if (await confirmExit(this)) {
        this.initForm()
        this.$v.valmistumispyyntoVaatimuksetLomake.$reset()
        this.$v.valmistumispyyntoLomake.$reset()
        this.$nextTick(() => {
          this.$emit('skipRouteExitConfirm', true)
        })
      }
    }

    onLaillistamistodistusFilesAdded(files: File[]) {
      this.valmistumispyyntoLomake.laillistamistodistus = files[0]
      this.laillistamispaivaAsiakirjat.push(...mapFiles(files))
    }

    async onDeletelaillistamistodistus() {
      this.valmistumispyyntoLomake.laillistamistodistus = null
      this.laillistamispaivaAsiakirjat = []
      this.$emit('skipRouteExitConfirm', false)
    }

    initForm() {
      this.valmistumispyyntoVaatimuksetLomake.tyoskentelyjaksot = false
      this.valmistumispyyntoVaatimuksetLomake.tyotodistukset = false
      this.valmistumispyyntoVaatimuksetLomake.teoriakoulutus = false
      this.valmistumispyyntoLomake.selvitysVanhentuneistaSuorituksista = null
      this.valmistumispyyntoLomake.erikoistujanSahkoposti =
        this.account.erikoistuvaLaakari.sahkoposti
      this.valmistumispyyntoLomake.erikoistujanPuhelinnumero =
        this.account.erikoistuvaLaakari.puhelinnumero
      this.vaatimuksetHyvaksytty = false
      this.initLaillistamispaivaAndTodistus()
    }

    muokkaaLaillistamista() {
      this.laillistaminenMuokattavissa = true
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .valmistumispyynto {
    max-width: 1024px;
  }

  ::v-deep {
    .custom-control-label {
      font-size: 0.9375rem;
    }

    .custom-control-input.is-valid ~ .custom-control-label {
      color: $black;
    }

    .custom-checkbox > [type='checkbox']:checked + label:after {
      background-color: $primary;
    }
  }
</style>
