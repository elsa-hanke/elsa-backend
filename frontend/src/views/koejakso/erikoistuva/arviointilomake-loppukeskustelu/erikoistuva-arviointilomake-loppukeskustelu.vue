<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />

    <b-container fluid>
      <h1 class="mb-3">{{ $t('koejakson-loppukeskustelu') }}</h1>
      <div v-if="!loading">
        <b-row lg>
          <b-col>
            <div v-if="editable">
              <p>{{ $t('koejakson-loppukeskustelu-ingressi') }}</p>
            </div>
            <div v-else>
              <b-alert :show="waitingForAcceptance" variant="dark" class="mt-3">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                  </em>
                  <div>
                    {{ $t('loppukeskustelu-tila-odottaa-hyvaksyntaa') }}
                  </div>
                </div>
              </b-alert>
              <b-alert variant="success" :show="acceptedByEveryone">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                  </em>
                  <span>{{ $t('loppukeskustelu-tila-hyvaksytty') }}</span>
                </div>
              </b-alert>
            </div>
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
              :syntymaaika="account.erikoistuvaLaakari.syntymaaika"
              :yliopisto="account.erikoistuvaLaakari.yliopisto"
            ></erikoistuva-details>
          </b-col>
        </b-row>

        <hr />

        <div v-if="acceptedByEveryone">
          <h3 class="mb-3">{{ $t('koejakson-tavoitteet-on-kasitelty-loppukeskustelussa') }}</h3>
          <p v-if="loppukeskusteluLomake.esitetaanKoejaksonHyvaksymista">
            {{ $t('loppukeskustelu-kayty-hyvaksytty') }}
          </p>
          <p v-else>{{ $t('loppukeskustelu-kayty-jatkotoimenpiteet') }}</p>
          <b-row v-if="!loppukeskusteluLomake.esitetaanKoejaksonHyvaksymista">
            <b-col lg="8">
              <h5>{{ $t('selvitys-jatkotoimista') }}</h5>
              <p>{{ loppukeskusteluLomake.jatkotoimenpiteet }}</p>
            </b-col>
          </b-row>
          <hr />
        </div>

        <koulutuspaikan-arvioijat
          ref="koulutuspaikanArvioijat"
          :lahikouluttaja="loppukeskusteluLomake.lahikouluttaja"
          :lahiesimies="loppukeskusteluLomake.lahiesimies"
          :is-readonly="!editable"
          :allow-duplicates="true"
          :kouluttajat="kouluttajat"
          @lahikouluttajaSelect="onLahikouluttajaSelect"
          @lahiesimiesSelect="onLahiesimiesSelect"
        />

        <hr />

        <b-row>
          <b-col lg="6">
            <h5>{{ $t('koejakson-alkamispäivä') }}</h5>
            <p>
              {{
                koejaksoData.aloituskeskustelu.koejaksonAlkamispaiva
                  ? $date(koejaksoData.aloituskeskustelu.koejaksonAlkamispaiva)
                  : ''
              }}
            </p>
          </b-col>
        </b-row>
        <b-row>
          <b-col v-if="editable" lg="6">
            <elsa-form-group :label="$t('koejakson-päättymispäivä')" :required="true">
              <template #default="{ uid }">
                <elsa-form-datepicker
                  v-if="!loading"
                  :id="uid"
                  ref="koejaksonPaattymispaiva"
                  :value.sync="loppukeskusteluLomake.koejaksonPaattymispaiva"
                  :min="minKoejaksonPaattymispaiva"
                  :min-error-text="$t('koejakso-voi-paattya-aikaisintaan-6kk-alkamispaivasta')"
                  :max="maxKoejaksonPaattymispaiva"
                  :max-error-text="$t('koejakson-maksimi-paattymispaiva-kuvaus')"
                  @input="$emit('skipRouteExitConfirm', false)"
                ></elsa-form-datepicker>
              </template>
            </elsa-form-group>
          </b-col>
          <b-col v-else lg="6">
            <h5>{{ $t('koejakson-päättymispäivä') }}</h5>
            <p>
              {{
                loppukeskusteluLomake.koejaksonPaattymispaiva
                  ? $date(loppukeskusteluLomake.koejaksonPaattymispaiva)
                  : ''
              }}
            </p>
          </b-col>
        </b-row>

        <hr />

        <div v-if="acceptedByEveryone">
          <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
        </div>

        <div v-if="deletable">
          <b-row>
            <b-col class="text-right">
              <elsa-button
                v-if="!loading"
                :loading="buttonStates.primaryButtonLoading"
                variant="outline-danger"
                class="ml-4 px-6"
                @click="onValidateAndConfirm('confirm-form-delete')"
              >
                {{ $t('tyhjenna-lomake') }}
              </elsa-button>
            </b-col>
          </b-row>
        </div>

        <div v-if="!account.impersonated && editable">
          <hr v-if="hyvaksynnat.length > 0" />

          <b-row>
            <b-col class="text-right">
              <elsa-button variant="back" :to="{ name: 'koejakso' }">
                {{ $t('peruuta') }}
              </elsa-button>
              <elsa-button
                v-if="!loading"
                :loading="buttonStates.primaryButtonLoading"
                variant="primary"
                class="ml-4 px-6"
                @click="onValidateAndConfirm('confirm-send')"
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
      :text="$t('vahvista-koejakson-vaihe-lahetys')"
      :submit-text="$t('laheta')"
      @submit="onSend"
    />

    <elsa-confirmation-modal
      id="confirm-form-delete"
      :title="$t('vahvista-lomakkeen-tyhjennys')"
      :text="$t('vahvista-lomakkeen-tyhjennys-selite')"
      :submit-text="$t('tyhjenna-lomake')"
      @submit="onFormDelete"
    />
  </div>
</template>

<script lang="ts">
  import { format } from 'date-fns'
  import _get from 'lodash/get'
  import Component from 'vue-class-component'
  import { Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import store from '@/store'
  import {
    LoppukeskusteluLomake,
    KoejaksonVaiheHyvaksyja,
    Koejakso,
    KoejaksonVaiheButtonStates,
    KoejaksonVaiheHyvaksynta,
    Opintooikeus
  } from '@/types'
  import { LomakeTilat } from '@/utils/constants'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'
  import { resolveOpintooikeusKaytossa } from '@/utils/opintooikeus'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaPopover,
      ElsaFormDatepicker,
      ErikoistuvaDetails,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      ElsaButton,
      ElsaConfirmationModal,
      KoejaksonVaiheHyvaksynnat,
      KoulutuspaikanArvioijat
    }
  })
  export default class ErikoistuvaArviointilomakeLoppukeskustelu extends Mixins(validationMixin) {
    $refs!: {
      koulutuspaikanArvioijat: KoulutuspaikanArvioijat
      koejaksonPaattymispaiva: ElsaFormDatepicker
    }
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
        text: this.$t('koejakson-loppukeskustelu'),
        active: true
      }
    ]
    validations() {
      return {
        loppukeskusteluLomake: {
          lahikouluttaja: {
            nimi: {
              required
            }
          },
          lahiesimies: {
            nimi: {
              required
            }
          },
          koejaksonPaattymispaiva: required
        }
      }
    }

    loading = true

    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }

    koejaksonVaihe = 'väliarviointi'

    loppukeskusteluLomake: Partial<LoppukeskusteluLomake> = {
      erikoistuvanErikoisala: this.account.erikoistuvaLaakari.erikoisalaNimi,
      erikoistuvanNimi: `${this.account.firstName} ${this.account.lastName}`,
      erikoistuvanOpiskelijatunnus: this.account.erikoistuvaLaakari.opiskelijatunnus,
      erikoistuvanYliopisto: this.account.erikoistuvaLaakari.yliopisto,
      esitetaanKoejaksonHyvaksymista: null,
      id: null,
      jatkotoimenpiteet: null,
      korjausehdotus: '',
      lahiesimies: {
        id: null,
        kayttajaUserId: null,
        kuittausaika: '',
        nimi: '',
        nimike: null,
        sopimusHyvaksytty: false
      },
      lahikouluttaja: {
        id: 0,
        kayttajaUserId: null,
        kuittausaika: '',
        nimi: '',
        nimike: null,
        sopimusHyvaksytty: false
      },
      muokkauspaiva: '',
      koejaksonPaattymispaiva: ''
    }

    validateState(value: string) {
      const form = this.$v.loppukeskusteluLomake
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    get account() {
      return store.getters['auth/account']
    }

    get editable() {
      if (this.account.impersonated) {
        return false
      }
      return this.koejaksoData.loppukeskustelunTila === LomakeTilat.UUSI
    }

    get deletable() {
      return this.koejaksoData.loppukeskustelunTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get waitingForAcceptance() {
      return this.koejaksoData.loppukeskustelunTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get acceptedByEveryone() {
      return this.koejaksoData.loppukeskustelunTila === LomakeTilat.HYVAKSYTTY
    }

    get kouluttajat() {
      return store.getters['erikoistuva/kouluttajatJaVastuuhenkilot'] || []
    }

    get koejaksoData(): Koejakso {
      return store.getters['erikoistuva/koejakso']
    }

    setKoejaksoData() {
      if (this.koejaksoData.loppukeskustelu) {
        this.loppukeskusteluLomake = this.koejaksoData.loppukeskustelu
      }
    }

    onLahikouluttajaSelect(lahikouluttaja: KoejaksonVaiheHyvaksyja) {
      this.loppukeskusteluLomake.lahikouluttaja = lahikouluttaja
    }

    onLahiesimiesSelect(lahiesimies: KoejaksonVaiheHyvaksyja) {
      this.loppukeskusteluLomake.lahiesimies = lahiesimies
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.loppukeskusteluLomake?.erikoistuvanNimi,
        this.loppukeskusteluLomake?.erikoistuvanKuittausaika
      )
      const hyvaksyntaLahikouluttaja = hyvaksynnatHelper.mapHyvaksyntaLahikouluttaja(
        this,
        this.loppukeskusteluLomake?.lahikouluttaja
      )
      const hyvaksyntaLahiesimies = hyvaksynnatHelper.mapHyvaksyntaLahiesimies(
        this,
        this.loppukeskusteluLomake?.lahiesimies
      )

      return [hyvaksyntaLahikouluttaja, hyvaksyntaLahiesimies, hyvaksyntaErikoistuva].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    optionDisplayName(option: any) {
      return option.nimike ? option.nimi + ', ' + option.nimike : option.nimi
    }

    hideModal(id: string) {
      return this.$bvModal.hide(id)
    }

    onValidateAndConfirm(modalId: string) {
      if (
        this.$refs.koulutuspaikanArvioijat &&
        !this.$refs.koulutuspaikanArvioijat.validateForm()
      ) {
        return
      }
      if (
        this.$refs.koejaksonPaattymispaiva &&
        !this.$refs.koejaksonPaattymispaiva.validateForm()
      ) {
        return
      }
      return this.$bvModal.show(modalId)
    }

    async onSend() {
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch('erikoistuva/postLoppukeskustelu', this.loppukeskusteluLomake)
        this.buttonStates.primaryButtonLoading = false
        toastSuccess(this, this.$t('loppukeskustelu-lahetetty-onnistuneesti'))
        this.setKoejaksoData()
      } catch {
        toastFail(this, this.$t('loppukeskustelu-tallennus-epaonnistui'))
      }
    }

    async onFormDelete() {
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch('erikoistuva/deleteLoppukeskustelu', this.loppukeskusteluLomake)
        this.buttonStates.primaryButtonLoading = false
        toastSuccess(this, this.$t('lomake-tyhjennetty-onnistuneesti'))
        this.loppukeskusteluLomake.id = null
      } catch {
        toastFail(this, this.$t('lomakkeen-tyhjennys-epaonnistui'))
      }
    }

    async mounted() {
      this.loading = true
      if (!this.koejaksoData) {
        await store.dispatch('erikoistuva/getKoejakso')
      }
      await store.dispatch('erikoistuva/getKouluttajatJaVastuuhenkilot')
      this.setKoejaksoData()
      this.loading = false
    }

    get opintooikeusKaytossa(): Opintooikeus | undefined {
      return resolveOpintooikeusKaytossa(this.account.erikoistuvaLaakari)
    }

    get minKoejaksonPaattymispaiva() {
      const dateFormat = 'yyyy-MM-dd'
      const koejaksonAlkamispaiva = this.koejaksoData.aloituskeskustelu.koejaksonAlkamispaiva
      if (!koejaksonAlkamispaiva) {
        return null
      }

      const koejaksonAlkamispaivaDate = new Date(koejaksonAlkamispaiva)
      // Koejakson kesto on vähintään 6kk.
      koejaksonAlkamispaivaDate.setMonth(koejaksonAlkamispaivaDate.getMonth() + 6)
      return format(koejaksonAlkamispaivaDate, dateFormat)
    }

    get maxKoejaksonPaattymispaiva() {
      const dateFormat = 'yyyy-MM-dd'
      const koejaksonAlkamispaiva = this.koejaksoData.aloituskeskustelu.koejaksonAlkamispaiva
      if (!this.opintooikeusKaytossa?.opintooikeudenPaattymispaiva || !koejaksonAlkamispaiva) {
        return null
      }

      const koejaksonAlkamispaivaMaxDate = new Date(
        this.koejaksoData.aloituskeskustelu.koejaksonAlkamispaiva
      )
      // Koejakson kesto on maksimissaan 2 vuotta.
      koejaksonAlkamispaivaMaxDate.setFullYear(koejaksonAlkamispaivaMaxDate.getFullYear() + 2)
      const opintooikeudenPaattymispaivaDate = new Date(
        this.opintooikeusKaytossa.opintooikeudenPaattymispaiva
      )
      // Mikäli maksimikesto 2 vuotta ylittää opinto-oikeuden päättymispäivän,
      // on maksimi päättymispäivä opinto-oikeuden päättymispäivä.
      if (koejaksonAlkamispaivaMaxDate > opintooikeudenPaattymispaivaDate) {
        return format(opintooikeudenPaattymispaivaDate, dateFormat)
      }

      return format(koejaksonAlkamispaivaMaxDate, dateFormat)
    }
  }
</script>
