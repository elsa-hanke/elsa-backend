<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />

    <b-container fluid>
      <h1 class="mb-3">{{ $t('erikoisalan-vastuuhenkilon-arvio-koejaksosta') }}</h1>
      <div v-if="!loading">
        <b-row lg>
          <b-col>
            <b-alert :show="showReturned" variant="danger" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
                </em>
                <div>
                  {{ $t('vastuuhenkilon-arvio-tila-palautettu-korjattavaksi') }}
                  <span
                    v-if="
                      koejaksoData.vastuuhenkilonArvio &&
                      koejaksoData.vastuuhenkilonArvio.virkailijanKorjausehdotus != null
                    "
                  >
                    {{ $t('virkailijan-toimesta') }}
                  </span>
                  <span v-else>{{ $t('vastuuhenkilon-toimesta') }}</span>
                  <span class="d-block">
                    {{ $t('syy') }}&nbsp;
                    <span class="font-weight-500">{{ korjausehdotus }}</span>
                  </span>
                </div>
              </div>
            </b-alert>
            <b-alert
              :show="editable"
              variant="dark"
              class="mt-2 mb-2"
              :class="{ 'border border-danger': hasTyoskentelyjaksoErrors }"
            >
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                </em>
                <div>
                  {{ $t('vastuuhenkilon-arvio-ingressi-sisalto.kuvaus') }}
                  <ul>
                    <li>
                      {{ $t('vastuuhenkilon-arvio-ingressi-sisalto.lista.item1.teksti') }}
                      <elsa-button
                        :to="{
                          name: 'koejakso'
                        }"
                        variant="link"
                        class="shadow-none p-0"
                      >
                        {{ $t('vastuuhenkilon-arvio-ingressi-sisalto.lista.item1.linkki') }}
                      </elsa-button>
                    </li>
                    <li>{{ $t('vastuuhenkilon-arvio-ingressi-sisalto.lista.item2') }}</li>
                    <li>{{ $t('vastuuhenkilon-arvio-ingressi-sisalto.lista.item3') }}</li>
                  </ul>
                </div>
              </div>
            </b-alert>
            <div v-if="editable">
              <div v-if="formData.tyoskentelyjaksoLiitetty === false" class="text-error">
                {{ $t('tyoskentelyjaksoa-ei-liitetty-koejaksoon') }}
              </div>
              <div v-else>
                <div v-if="formData.tyoskentelyjaksonPituusRiittava === false" class="text-error">
                  {{ $t('tyoskentelyjakson-pituus-alle-6-kk') }}
                </div>
                <div v-if="formData.tyotodistusLiitetty === false" class="text-error">
                  {{ $t('tyotodistusta-ei-liitetty-tyoskentelyjaksoon') }}
                </div>
              </div>
            </div>
            <b-alert :show="waitingForAcceptance" variant="dark" class="mt-3 mb-2">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                </em>
                <div v-if="waitingForVirkailija">
                  {{ $t('vastuuhenkilon-arvio-tila-odottaa-hyvaksyntaa') }}
                </div>
                <div v-else>
                  {{ $t('virkailijan-arvio-tila-odottaa-hyvaksyntaa') }}
                </div>
              </div>
            </b-alert>
            <b-alert variant="success" class="mb-2" :show="accepted">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                </em>
                <span>{{ $t('vastuuhenkilon-arvio-tila-hyvaksytty') }}</span>
              </div>
            </b-alert>
          </b-col>
        </b-row>
        <hr />
        <b-row>
          <b-col>
            <erikoistuva-details
              :avatar="account.avatar"
              :name="`${account.firstName} ${account.lastName}`"
              :erikoisala="account.erikoistuvaLaakari.erikoisalaNimi"
              :opiskelijatunnus="account.erikoistuvaLaakari.opiskelijatunnus"
              :yliopisto="account.erikoistuvaLaakari.yliopisto"
              :syntymaaika="account.erikoistuvaLaakari.syntymaaika"
            ></erikoistuva-details>
          </b-col>
        </b-row>
        <hr />
        <div v-if="editable">
          <b-row>
            <b-col lg="4">
              <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
                <template #default="{ uid }">
                  <b-form-input
                    :id="uid"
                    v-model="form.erikoistuvanSahkoposti"
                    :state="validateState('erikoistuvanSahkoposti')"
                    @input="$emit('skipRouteExitConfirm', false)"
                  />
                  <b-form-invalid-feedback
                    v-if="
                      $v.form.erikoistuvanSahkoposti && !$v.form.erikoistuvanSahkoposti.required
                    "
                    :id="`${uid}-feedback`"
                  >
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                  <b-form-invalid-feedback
                    v-if="$v.form.erikoistuvanSahkoposti && !$v.form.erikoistuvanSahkoposti.email"
                    :id="`${uid}-feedback`"
                    :state="validateState('erikoistuvanSahkoposti')"
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
                    v-model="form.erikoistuvanPuhelinnumero"
                    :state="validateState('erikoistuvanPuhelinnumero')"
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
          <b-row v-if="muutOpintooikeudetEnabled">
            <b-col>
              <elsa-form-group :label="$t('useampi-opinto-oikeus')" :required="true">
                <template #default="{ uid }">
                  <p class="mb-3">
                    {{ $t('useampi-opinto-oikeus-suostumus-erikoistuja-ingressi') }}
                  </p>
                  <b-form-checkbox
                    :id="uid"
                    v-model="form.paataOpintooikeudet"
                    :state="validateState('paataOpintooikeudet')"
                    class="py-0"
                    @input="$emit('skipRouteExitConfirm', false)"
                  >
                    {{ $t('useampi-opinto-oikeus-suostumus-erikoistuja') }}
                    <label class="d-block">
                      <span v-for="(opintooikeus, index) in muutOpintooikeudet" :key="index">
                        {{ opintooikeus.erikoisalaNimi }},
                        {{ $t(`yliopisto-nimi.${opintooikeus.yliopistoNimi}`) }}
                        <br />
                      </span>
                    </label>
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
            </b-col>
            <hr />
          </b-row>
          <b-row v-if="!koulutussopimuksenHyvaksynta">
            <b-col>
              <elsa-form-group :label="$t('koulutussopimus')" :required="true">
                <template #default="{ uid }">
                  <b-form-checkbox
                    :id="uid"
                    v-model="form.koulutussopimusHyvaksytty"
                    :state="validateState('koulutussopimusHyvaksytty')"
                    class="py-0"
                    @input="$emit('skipRouteExitConfirm', false)"
                  >
                    {{ $t('vastuuhenkilon-arvio-koulutussopimus-varmistus') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <elsa-form-group :label="$t('koulutussuunnitelma')" :required="true">
                <template #default="{ uid }">
                  <b-form-checkbox
                    :id="uid"
                    v-model="koulutussuunnitelmaHyvaksytty"
                    :state="validateKoulutussuunnitelmaHyvaksytty()"
                    class="py-0"
                    @input="$emit('skipRouteExitConfirm', false)"
                  >
                    {{ $t('vastuuhenkilon-arvio-koulutussuunnitelma-varmistus') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
            </b-col>
          </b-row>
          <hr />
          <elsa-form-group :label="$t('liitetiedostot')">
            <template #label-help>
              <elsa-popover>
                {{ $t('sallitut-tiedostoformaatit-default') }}
              </elsa-popover>
            </template>
            <span>
              {{ $t('koejakson-liitetiedostot-kuvaus-1') }}
              <ul>
                <li>{{ $t('koejakson-liitetiedostot-kuvaus-2') }}</li>
                <li>
                  <a href="https://tthvyo.fi/erikoistujat/oppaat/">
                    {{ $t('tyoterveyshuollossa') }}
                  </a>
                  {{ $t('koejakson-liitetiedostot-kuvaus-3') }}
                </li>
                <li>{{ $t('koejakson-liitetiedostot-kuvaus-4') }}</li>
              </ul>
            </span>
            <asiakirjat-upload
              class="mt-3"
              :is-primary-button="false"
              :button-text="$t('lisaa-liitetiedosto')"
              :existing-file-names-in-current-view="existingFileNamesInCurrentView"
              :existing-file-names-in-other-views="existingFileNamesInOtherViews"
              :disabled="reservedAsiakirjaNimetMutable === undefined"
              @selectedFiles="onFilesAdded"
            />
            <asiakirjat-content
              :asiakirjat="asiakirjatTableItems"
              :sorting-enabled="false"
              :pagination-enabled="false"
              :enable-search="false"
              :show-info-if-empty="false"
              @deleteAsiakirja="onDeleteLiitetiedosto"
            />
          </elsa-form-group>
          <hr />
        </div>
        <div v-else>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('sahkopostiosoite') }}</h5>
              <p>{{ account.erikoistuvaLaakari.sahkoposti }}</p>
            </b-col>

            <b-col lg="4">
              <h5>{{ $t('matkapuhelinnumero') }}</h5>
              <p>{{ account.erikoistuvaLaakari.puhelinnumero }}</p>
            </b-col>
          </b-row>
          <hr />
          <div v-if="muutOpintooikeudetEnabled">
            <h3 class="mb-3">{{ $t('useampi-opinto-oikeus') }}</h3>
            <p>
              {{ $t('useampi-opinto-oikeus-suostumus-erikoistuja') }}
              <label class="d-block">
                <span v-for="(opintooikeus, index) in muutOpintooikeudet" :key="index">
                  {{ opintooikeus.erikoisalaNimi }},
                  {{ $t(`yliopisto-nimi.${opintooikeus.yliopistoNimi}`) }}
                  <br />
                </span>
              </label>
            </p>
            <hr />
          </div>
          <div v-if="!koulutussopimuksenHyvaksynta">
            <h3 class="mb-3">{{ $t('koulutussopimus') }}</h3>
            <p>
              {{ $t('vastuuhenkilon-arvio-koulutussopimus-varmistus') }}
            </p>
          </div>
          <h3 class="mb-3">{{ $t('koulutussuunnitelma') }}</h3>
          <p>
            {{ $t('vastuuhenkilon-arvio-koulutussuunnitelma-varmistus') }}
          </p>
          <hr />
          <elsa-form-group :label="$t('liitetiedostot')">
            <asiakirjat-content
              v-if="
                koejaksoData.vastuuhenkilonArvio.asiakirjat &&
                koejaksoData.vastuuhenkilonArvio.asiakirjat.length > 0
              "
              :asiakirjat="koejaksoData.vastuuhenkilonArvio.asiakirjat"
              :sorting-enabled="false"
              :pagination-enabled="false"
              :enable-search="false"
              :show-info-if-empty="false"
              :enable-delete="false"
            />
            <b-alert v-else variant="dark" show>
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span>
                {{ $t('ei-liitetiedostoja') }}
              </span>
            </b-alert>
          </elsa-form-group>
          <hr />
        </div>
        <b-row>
          <b-col v-if="accepted">
            <elsa-form-group
              class="mt-2"
              :class="{ 'mb-4': form.koejaksoHyvaksytty === false }"
              :label="$t('koejakso-on')"
            >
              {{ form.koejaksoHyvaksytty ? $t('hyvaksytty') : $t('hylatty') }}
            </elsa-form-group>
            <elsa-form-group
              v-if="form.koejaksoHyvaksytty === false"
              class="mb-4"
              :label="$t('perustelu-hylkaamiselle')"
            >
              {{ form.perusteluHylkaamiselle }}
            </elsa-form-group>
            <elsa-form-group
              v-if="form.koejaksoHyvaksytty === false"
              :label="$t('hylatyn-koejakson-arviointi-kayty-lapi-keskustellen')"
            >
              {{ $t('kylla') }}
            </elsa-form-group>
          </b-col>
          <b-col v-else-if="formData.vastuuhenkilo">
            <h3 class="mb-3">{{ $t('erikoisala-vastuuhenkilö') }}</h3>
            <elsa-form-group :label="$t('erikoisala-vastuuhenkilö-label')">
              <p>
                {{ formData.vastuuhenkilo.nimi }}
                {{ formData.vastuuhenkilo.nimike ? ', ' + formData.vastuuhenkilo.nimike : '' }}
              </p>
            </elsa-form-group>
          </b-col>
        </b-row>
        <div v-if="accepted">
          <hr />
          <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
        </div>
        <div v-if="!account.impersonated && editable">
          <hr />
          <b-row>
            <b-col class="text-right">
              <elsa-button variant="back" :to="{ name: 'koejakso' }">
                {{ $t('peruuta') }}
              </elsa-button>
              <elsa-button
                v-if="!loading"
                :disabled="hasTyoskentelyjaksoErrors"
                :loading="buttonStates.primaryButtonLoading"
                variant="primary"
                class="ml-4 px-6"
                @click="onConfirm('confirm-send')"
              >
                {{ $t('laheta') }}
              </elsa-button>
            </b-col>
          </b-row>
          <b-row>
            <elsa-form-error :active="$v.$anyError" />
          </b-row>
        </div>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>

    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('vahvista-pyyda-vastuuhenkilon-arvio')"
      :submit-text="$t('laheta')"
      @submit="onSend"
    />
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { email, required, requiredIf } from 'vuelidate/lib/validators'

  import { getVastuuhenkilonArvioLomake } from '@/api/erikoistuva'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import store from '@/store'
  import {
    Asiakirja,
    Koejakso,
    KoejaksonVaiheHyvaksynta,
    KoejaksonVaiheButtonStates,
    Opintooikeus,
    VastuuhenkilonArvioLomake,
    VastuuhenkilonArvioLomakeErikoistuva
  } from '@/types'
  import { LomakeTilat, phoneNumber } from '@/utils/constants'
  import { mapFiles } from '@/utils/fileMapper'
  import { checkCurrentRouteAndRedirect } from '@/utils/functions'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    mixins: [validationMixin],
    components: {
      AsiakirjatContent,
      AsiakirjatUpload,
      ErikoistuvaDetails,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      ElsaButton,
      ElsaConfirmationModal,
      ElsaPopover,
      KoejaksonVaiheHyvaksynnat
    }
  })
  export default class ErikoistuvaArviointilomakeVastuuhenkilonArvio extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koejakso'),
        to: { name: 'koejakso' }
      },
      {
        text: this.$t('koejakson-vastuuhenkilon-arvio'),
        active: true
      }
    ]
    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }
    koejaksonVaihe = this.$t('vastuuhenkilon-arvio')

    form: VastuuhenkilonArvioLomake = {
      erikoistuvanErikoisala: this.account.erikoistuvaLaakari.erikoisalaNimi,
      erikoistuvanNimi: `${this.account.firstName} ${this.account.lastName}`,
      erikoistuvanOpiskelijatunnus: this.account.erikoistuvaLaakari.opiskelijatunnus,
      erikoistuvanYliopisto: this.account.erikoistuvaLaakari.yliopisto,
      erikoistuvanSahkoposti: this.account.erikoistuvaLaakari.sahkoposti,
      erikoistuvanPuhelinnumero: this.account.erikoistuvaLaakari.puhelinnumero,
      muutOpintooikeudet: [],
      paataOpintooikeudet: false,
      id: null,
      muokkauspaiva: '',
      koejaksoHyvaksytty: null,
      vastuuhenkilo: null,
      hylattyArviointiKaytyLapiKeskustellen: null,
      vastuuhenkilonKuittausaika: undefined,
      virkailijanYhteenveto: null
    }
    formData: VastuuhenkilonArvioLomakeErikoistuva = {
      muutOpintooikeudet: [],
      paataOpintooikeudet: false,
      koulutussopimusHyvaksytty: true,
      vastuuhenkilo: null,
      tyoskentelyjaksoLiitetty: false,
      tyoskentelyjaksonPituusRiittava: false,
      tyotodistusLiitetty: false
    }
    loading = true
    muutOpintooikeudet: Opintooikeus[] = []
    muutOpintooikeudetEnabled = false
    koulutussopimuksenHyvaksynta = false
    koulutussuunnitelmaHyvaksytty = false
    reservedAsiakirjaNimetMutable: string[] = []
    addedFiles: File[] = []
    newAsiakirjatMapped: Asiakirja[] = []
    deletedAsiakirjat: Asiakirja[] = []

    validations() {
      return {
        form: {
          erikoistuvanSahkoposti: {
            required,
            email
          },
          erikoistuvanPuhelinnumero: {
            required,
            phoneNumber
          },
          paataOpintooikeudet: {
            checked: (value: boolean) => {
              return !this.muutOpintooikeudetEnabled || value === true
            },
            required: requiredIf(() => {
              return this.muutOpintooikeudetEnabled
            })
          },
          koulutussopimusHyvaksytty: {
            checked: (value: boolean) => {
              return this.koulutussopimuksenHyvaksynta || value === true
            },
            required: requiredIf(() => {
              return !this.koulutussopimuksenHyvaksynta
            })
          }
        },
        koulutussuunnitelmaHyvaksytty: {
          checked: (value: boolean) => value === true
        }
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    get editable() {
      if (this.account.impersonated) {
        return false
      }
      return (
        this.koejaksoData.vastuuhenkilonArvionTila === LomakeTilat.UUSI ||
        this.koejaksoData.vastuuhenkilonArvionTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
      )
    }

    get showReturned() {
      return this.koejaksoData.vastuuhenkilonArvionTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get korjausehdotus() {
      return this.koejaksoData.vastuuhenkilonArvio?.virkailijanKorjausehdotus != null
        ? this.koejaksoData.vastuuhenkilonArvio?.virkailijanKorjausehdotus
        : this.koejaksoData.vastuuhenkilonArvio?.vastuuhenkilonKorjausehdotus
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? !$error : null
    }

    validateKoulutussuunnitelmaHyvaksytty() {
      const { $dirty, $error } = this.$v.koulutussuunnitelmaHyvaksytty as any
      return $dirty ? !$error : null
    }

    get waitingForVirkailija() {
      return this.koejaksoData.vastuuhenkilonArvio?.virkailija?.sopimusHyvaksytty
    }

    get waitingForAcceptance() {
      return (
        this.koejaksoData.vastuuhenkilonArvionTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA ||
        this.koejaksoData.vastuuhenkilonArvionTila ===
          LomakeTilat.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
      )
    }

    get accepted() {
      return this.koejaksoData.vastuuhenkilonArvionTila === LomakeTilat.HYVAKSYTTY
    }

    get koejaksoData(): Koejakso {
      return store.getters['erikoistuva/koejakso']
    }

    setKoejaksoData() {
      if (this.koejaksoData.vastuuhenkilonArvio) {
        this.form = this.koejaksoData.vastuuhenkilonArvio
      }
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.form?.erikoistuvanNimi,
        this.form?.erikoistuvanKuittausaika
      )
      const hyvaksyntaVastuuhenkilo = hyvaksynnatHelper.mapHyvaksyntaVastuuhenkilo(
        this.form.vastuuhenkilo
      ) as KoejaksonVaiheHyvaksynta

      return [hyvaksyntaVastuuhenkilo, hyvaksyntaErikoistuva].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    get hasTyoskentelyjaksoErrors() {
      return (
        this.formData.tyoskentelyjaksoLiitetty === false ||
        this.formData.tyoskentelyjaksonPituusRiittava === false ||
        this.formData.tyotodistusLiitetty === false
      )
    }

    optionDisplayName(option: any) {
      return option.nimike ? option.nimi + ', ' + option.nimike : option.nimi
    }

    hideModal(id: string) {
      return this.$bvModal.hide(id)
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      this.$v.koulutussuunnitelmaHyvaksytty.$touch()
      return !this.$v.$anyError
    }

    onConfirm(modalId: string) {
      if (!this.validateForm()) {
        return
      }
      return this.$bvModal.show(modalId)
    }

    async onSend() {
      try {
        this.buttonStates.primaryButtonLoading = true
        const formData = new FormData()
        formData.append('vastuuhenkilonArvioJson', JSON.stringify(this.form))
        this.addedFiles.forEach((file: File) => formData.append('asiakirjaFiles', file, file.name))
        if (this.koejaksoData.vastuuhenkilonArvionTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI) {
          formData.append(
            'deletedAsiakirjaIdsJson',
            JSON.stringify(this.deletedAsiakirjat.map((a) => a.id))
          )
          await store.dispatch('erikoistuva/putVastuuhenkilonArvio', formData)
        } else {
          await store.dispatch('erikoistuva/postVastuuhenkilonArvio', formData)
        }
        this.buttonStates.primaryButtonLoading = false
        toastSuccess(this, this.$t('vastuuhenkilon-arvio-lahetetty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
      } catch (err) {
        toastFail(this, this.$t('vastuuhenkilon-arvio-lahetys-epaonnistui'))
      }
    }

    async mounted() {
      this.loading = true
      if (!this.koejaksoData) {
        await store.dispatch('erikoistuva/getKoejakso')
      }
      this.setKoejaksoData()
      this.formData = (await getVastuuhenkilonArvioLomake()).data
      this.muutOpintooikeudet = this.formData.muutOpintooikeudet
      this.koulutussopimuksenHyvaksynta = this.formData.koulutussopimusHyvaksytty
      this.muutOpintooikeudetEnabled =
        this.muutOpintooikeudet != null && this.muutOpintooikeudet.length > 0

      if (this.editable) {
        this.form.vastuuhenkilo = this.formData.vastuuhenkilo
        this.form.erikoistuvanSahkoposti = this.account.erikoistuvaLaakari.sahkoposti
        this.form.erikoistuvanPuhelinnumero = this.account.erikoistuvaLaakari.puhelinnumero

        this.reservedAsiakirjaNimetMutable = (
          await axios.get('erikoistuva-laakari/asiakirjat/nimet')
        ).data
      }

      this.loading = false
    }

    get existingFileNamesInCurrentView() {
      return this.asiakirjatTableItems?.map((item) => item.nimi)
    }

    get existingFileNamesInOtherViews() {
      return this.reservedAsiakirjaNimetMutable?.filter(
        (nimi) =>
          !this.existingFileNamesInCurrentView.includes(nimi) &&
          !this.deletedAsiakirjat.map((deleted) => deleted.nimi).includes(nimi)
      )
    }

    get asiakirjatTableItems() {
      return [...this.newAsiakirjatMapped, ...this.asiakirjatExcludingDeleted()]
    }

    private asiakirjatExcludingDeleted(): Asiakirja[] {
      return (this.koejaksoData.vastuuhenkilonArvio?.asiakirjat ?? []).filter(
        (asiakirja) =>
          !this.deletedAsiakirjat.map((deleted) => deleted.nimi).includes(asiakirja.nimi)
      )
    }

    onFilesAdded(files: File[]) {
      this.addedFiles = [...files, ...this.addedFiles]
      this.newAsiakirjatMapped?.push(...mapFiles(files))
      this.$emit('skipRouteExitConfirm', false)
    }

    async onDeleteLiitetiedosto(asiakirja: Asiakirja) {
      if (asiakirja.id) {
        this.deletedAsiakirjat = [asiakirja, ...this.deletedAsiakirjat]
      } else {
        this.newAsiakirjatMapped = this.newAsiakirjatMapped?.filter(
          (a) => a.nimi !== asiakirja.nimi
        )
        this.addedFiles = this.addedFiles?.filter((a) => a.name !== asiakirja.nimi)
      }
      this.$emit('skipRouteExitConfirm', false)
    }
  }
</script>
