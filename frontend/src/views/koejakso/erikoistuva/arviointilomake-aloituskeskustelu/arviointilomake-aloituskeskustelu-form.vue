<template>
  <b-form @submit.stop.prevent="onSubmit">
    <b-row>
      <b-col lg="8">
        <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.erikoistuvanSahkoposti"
              :state="validateState('erikoistuvanSahkoposti')"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-col>
    </b-row>

    <b-row>
      <b-col lg="8">
        <elsa-form-group :label="$t('koejakson-suorituspaikka')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.koejaksonSuorituspaikka"
              :state="validateState('koejaksonSuorituspaikka')"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-button
          v-if="!local.toinenSuorituspaikka"
          variant="outline-primary"
          class="border-0 p-0"
          @click="local.toinenSuorituspaikka = true"
        >
          + {{ $t('suorituspaikka-toinen') }}
        </elsa-button>
      </b-col>
    </b-row>

    <b-row v-if="local.toinenSuorituspaikka">
      <b-col lg="8">
        <elsa-form-group :label="$t('koejakson-toinen-suorituspaikka')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.koejaksonToinenSuorituspaikka"
              :state="validateState('koejaksonToinenSuorituspaikka')"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-button
          variant="outline-primary"
          class="border-0 p-0"
          @click="deleteToinenSuorituspaikka"
        >
          <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="lg" />
          {{ $t('suorituspaikka-poista') }}
        </elsa-button>
      </b-col>
    </b-row>

    <b-row class="mt-4">
      <b-col xl="4" lg="8">
        <elsa-form-group :label="$t('koejakson-alkamispäivä')" :required="true">
          <template #label-help>
            <elsa-popover>
              {{ $t('koejakson-alkamis-tooltip') }}
            </elsa-popover>
          </template>
          <template #default="{ uid }">
            <elsa-form-datepicker
              v-if="childDataReceived"
              :id="uid"
              ref="koejaksonAlkamispaiva"
              :value.sync="form.koejaksonAlkamispaiva"
              :min="opintooikeudenMyontamispaiva"
              :min-error-text="$t('koejakso-ei-voi-alkaa-ennen-opinto-oikeuden-myontamispaivaa')"
              :max="maxKoejaksonAlkamispaiva"
              :max-error-text="
                $t(
                  'koejakson-voi-aloittaa-viimeistaan-puoli-vuotta-ennen-opinto-oikeuden-paattymista'
                )
              "
              @input="$emit('skipRouteExitConfirm', false)"
            ></elsa-form-datepicker>
          </template>
        </elsa-form-group>
      </b-col>

      <b-col xl="4" lg="8">
        <elsa-form-group :label="$t('koejakson-päättymispäivä')" :required="true">
          <template #default="{ uid }">
            <elsa-form-datepicker
              v-if="childDataReceived"
              :id="uid"
              ref="koejaksonPaattymispaiva"
              :value.sync="form.koejaksonPaattymispaiva"
              :min="minKoejaksonPaattymispaiva"
              :min-error-text="$t('koejakso-voi-paattya-aikaisintaan-6kk-alkamispaivasta')"
              :max="maxKoejaksonPaattymispaiva"
              :max-error-text="$t('koejakson-maksimi-paattymispaiva-kuvaus')"
              @input="$emit('skipRouteExitConfirm', false)"
            ></elsa-form-datepicker>
          </template>
        </elsa-form-group>
      </b-col>
    </b-row>

    <b-row>
      <b-col lg="8">
        <elsa-form-group
          :label="$t('koejakso-suoritettu-kokoaikatyössä')"
          :required="true"
          class="mb-1"
        >
          <template #label-help>
            <elsa-popover>
              {{ $t('koejakso-suoritettu-kokoaikatyössä-tooltip-content') }}
            </elsa-popover>
          </template>
          <template #default="{ uid }">
            <b-form-radio-group
              :id="uid"
              v-model="form.suoritettuKokoaikatyossa"
              :options="suorittettuKokoaikatyossaOptions"
              :state="validateState('suoritettuKokoaikatyossa')"
              name="suoritettu-kokoaikatyossa"
              stacked
              @input="$emit('skipRouteExitConfirm', false)"
            ></b-form-radio-group>
            <b-form-invalid-feedback
              :id="`${uid}-feedback`"
              :state="validateState('suoritettuKokoaikatyossa')"
            >
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-col>
    </b-row>
    <b-row v-if="form.suoritettuKokoaikatyossa === false" class="mb-3">
      <b-col lg="3">
        <elsa-form-group :label="$t('suoritetaan-osa-aikatyossa')" :required="true" class="ml-4">
          <template #default="{ uid }">
            <div class="d-inline-flex align-items-center">
              <b-form-input
                :id="uid"
                v-model="local.tyotunnitViikossa"
                class="tyotunnit-viikossa-input"
                :state="validateState('tyotunnitViikossa')"
                @input="$emit('skipRouteExitConfirm', false)"
                @change="onTyotunnitViikossaChange"
              />
              <span class="ml-2">({{ $t('tuntia-viikossa') }})</span>
            </div>
            <b-form-invalid-feedback
              v-if="$v.form.tyotunnitViikossa && !$v.form.tyotunnitViikossa.required"
              :id="`${uid}-feedback`"
              :state="validateState('tyotunnitViikossa')"
            >
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
            <b-form-invalid-feedback
              v-if="$v.form.tyotunnitViikossa && !$v.form.tyotunnitViikossa.between"
              :id="`${uid}-feedback`"
              :state="validateState('tyotunnitViikossa')"
            >
              {{ $t('koejakso-suoritettu-osaaikatyossa.invalid') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-col>
    </b-row>
    <hr />
    <koulutuspaikan-arvioijat
      ref="koulutuspaikanArvioijat"
      :lahikouluttaja="form.lahikouluttaja"
      :lahiesimies="form.lahiesimies"
      :button-states="buttonStates"
      :allow-duplicates="true"
      :kouluttajat="kouluttajat"
      @lahikouluttajaSelect="onLahikouluttajaSelect"
      @lahiesimiesSelect="onLahiesimiesSelect"
    />
    <hr />

    <b-row>
      <b-col>
        <h3>{{ $t('aloituskeskustelu-tavoitteet') }}</h3>
        <elsa-form-group :label="$t('koejakso-osaamistavoitteet')" :required="true">
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="form.koejaksonOsaamistavoitteet"
              :state="validateState('koejaksonOsaamistavoitteet')"
              rows="7"
              class="textarea-min-height"
              @input="$emit('skipRouteExitConfirm', false)"
            ></b-form-textarea>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-col>
    </b-row>

    <hr />

    <b-row>
      <b-col class="text-right">
        <elsa-button
          class="ml-1 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block text-left"
          style="max-width: 14rem"
          variant="back"
          :to="{ name: 'koejakso' }"
        >
          {{ $t('peruuta') }}
        </elsa-button>
        <elsa-button
          v-b-modal.confirm-save
          class="my-2 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block"
          style="min-width: 14rem"
          variant="outline-primary"
          :disabled="buttonStates.primaryButtonLoading"
          :loading="buttonStates.secondaryButtonLoading"
        >
          {{ $t('tallenna-keskeneraisena') }}
        </elsa-button>
        <elsa-button
          class="my-2 d-block d-md-inline-block d-lg-block d-xl-inline-block"
          style="min-width: 14rem"
          :disabled="buttonStates.secondaryButtonLoading"
          :loading="buttonStates.primaryButtonLoading"
          variant="primary"
          @click="validateAndConfirmSend"
        >
          {{ $t('laheta') }}
        </elsa-button>
      </b-col>
    </b-row>
    <b-row>
      <elsa-form-error :active="$v.$anyError" />
    </b-row>
    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('vahvista-koejakson-vaihe-lahetys')"
      :submit-text="$t('laheta')"
      @submit="onSubmit"
    />
    <elsa-confirmation-modal
      id="confirm-save"
      :title="$t('vahvista-tallennus-keskeneraisena-title')"
      :text="$t('vahvista-tallennus-keskeneraisena-body')"
      :submit-text="$t('tallenna-keskeneraisena')"
      @submit="saveAndExit"
    />
  </b-form>
</template>

<script lang="ts">
  import { format } from 'date-fns'
  import _get from 'lodash/get'
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required, requiredIf, email, between } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import {
    AloituskeskusteluLomake,
    Kouluttaja,
    UserAccount,
    KoejaksonVaiheHyvaksyja,
    KoejaksonVaiheButtonStates,
    Opintooikeus
  } from '@/types'
  import { resolveOpintooikeusKaytossa } from '@/utils/opintooikeus'

  @Component({
    mixins: [validationMixin],
    components: {
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormDatepicker,
      ElsaButton,
      ElsaPopover,
      ElsaFormMultiselect,
      ElsaConfirmationModal,
      KoulutuspaikanArvioijat
    }
  })
  export default class ArviointilomakeAloituskeskusteluForm extends Vue {
    $refs!: {
      koulutuspaikanArvioijat: KoulutuspaikanArvioijat
      koejaksonAlkamispaiva: ElsaFormDatepicker
      koejaksonPaattymispaiva: ElsaFormDatepicker
    }

    @Prop({ required: true, default: {} })
    data!: AloituskeskusteluLomake

    @Prop({ required: true, default: {} })
    account!: UserAccount

    @Prop({ required: false, default: () => [] })
    kouluttajat!: Kouluttaja[]

    validations() {
      return {
        form: {
          erikoistuvanSahkoposti: {
            required,
            email
          },
          koejaksonSuorituspaikka: {
            required
          },
          koejaksonToinenSuorituspaikka: {
            required: requiredIf(() => {
              return this.local.toinenSuorituspaikka
            })
          },
          suoritettuKokoaikatyossa: {
            required
          },
          tyotunnitViikossa: {
            required: requiredIf((value) => {
              return value.suoritettuKokoaikatyossa == false
            }),
            between: between(19.125, 38.25)
          },
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
          koejaksonOsaamistavoitteet: {
            required
          }
        }
      }
    }

    suorittettuKokoaikatyossaOptions = [
      { value: true, text: this.$t('kylla') },
      { value: false, text: this.$t('ei') }
    ]

    form: AloituskeskusteluLomake = {
      erikoistuvanErikoisala: '',
      erikoistuvanNimi: '',
      erikoistuvanOpiskelijatunnus: '',
      erikoistuvanSahkoposti: '',
      erikoistuvanYliopisto: '',
      id: null,
      koejaksonAlkamispaiva: '',
      koejaksonOsaamistavoitteet: '',
      koejaksonPaattymispaiva: '',
      koejaksonSuorituspaikka: '',
      koejaksonToinenSuorituspaikka: '',
      korjausehdotus: '',
      lahetetty: false,
      lahiesimies: {
        id: null,
        kayttajaUserId: null,
        kuittausaika: '',
        nimi: '',
        nimike: null,
        sopimusHyvaksytty: false
      },
      lahikouluttaja: {
        id: null,
        kayttajaUserId: null,
        kuittausaika: '',
        nimi: '',
        nimike: null,
        sopimusHyvaksytty: false
      },
      muokkauspaiva: '',
      suoritettuKokoaikatyossa: null,
      tyotunnitViikossa: null
    }

    local: any = {
      tyotunnitViikossa: String,
      toinenSuorituspaikka: false
    }

    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }
    childDataReceived = false

    get hasErrors() {
      return this.$v.$anyError
    }

    validateState(value: string) {
      const form = this.$v.form
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    deleteToinenSuorituspaikka() {
      this.form.koejaksonToinenSuorituspaikka = ''
      this.local.toinenSuorituspaikka = false
    }

    optionDisplayName(option: any) {
      return option.nimike ? option.nimi + ', ' + option.nimike : option.nimi
    }

    onTyotunnitViikossaChange(val: string) {
      this.form.tyotunnitViikossa = parseFloat(val.replace(',', '.'))
    }

    get opintooikeusKaytossa(): Opintooikeus | undefined {
      return resolveOpintooikeusKaytossa(this.account.erikoistuvaLaakari)
    }

    get maxKoejaksonAlkamispaiva() {
      const dateFormat = 'yyyy-MM-dd'
      if (!this.opintooikeusKaytossa?.opintooikeudenPaattymispaiva) {
        return null
      }

      const opintooikeudenPaattymispaivaDate = new Date(
        this.opintooikeusKaytossa.opintooikeudenPaattymispaiva
      )
      // Koejakson voi aloittaa viimeistään 6kk ennen määrä-aikaisen
      // opinto-oikeuden päättymispäivää, koska koejakson kesto on 6kk.
      opintooikeudenPaattymispaivaDate.setMonth(opintooikeudenPaattymispaivaDate.getMonth() - 6)
      return format(opintooikeudenPaattymispaivaDate, dateFormat)
    }

    get minKoejaksonPaattymispaiva() {
      const dateFormat = 'yyyy-MM-dd'
      const koejaksonAlkamispaiva = this.form.koejaksonAlkamispaiva
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
      const koejaksonAlkamispaiva = this.form.koejaksonAlkamispaiva
      if (!this.opintooikeusKaytossa?.opintooikeudenPaattymispaiva || !koejaksonAlkamispaiva) {
        return null
      }

      const koejaksonAlkamispaivaMaxDate = new Date(this.form.koejaksonAlkamispaiva)
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

    get opintooikeudenMyontamispaiva() {
      return this.opintooikeusKaytossa?.opintooikeudenMyontamispaiva
    }

    onLahikouluttajaSelect(lahikouluttaja: KoejaksonVaiheHyvaksyja) {
      this.form.lahikouluttaja = lahikouluttaja
    }

    onLahiesimiesSelect(lahiesimies: KoejaksonVaiheHyvaksyja) {
      this.form.lahiesimies = lahiesimies
    }

    saveAndExit() {
      this.$emit('saveAndExit', this.form, this.buttonStates)
    }

    onSubmit() {
      this.$emit('submit', this.form, this.buttonStates)
    }

    resetValidationsAndConfirmSave() {
      this.$v.$reset()
      this.$refs.koulutuspaikanArvioijat.$v.$reset()
      this.$refs.koejaksonAlkamispaiva.$v.$reset()
      this.$refs.koejaksonPaattymispaiva.$v.$reset()

      return this.$bvModal.show('confirm-send')
    }

    validateAndConfirmSend() {
      const validations = [
        this.validateForm(),
        this.$refs.koulutuspaikanArvioijat.validateForm(),
        this.$refs.koejaksonAlkamispaiva.validateForm(),
        this.$refs.koejaksonPaattymispaiva.validateForm()
      ]

      if (validations.includes(false)) {
        return
      }

      return this.$bvModal.show('confirm-send')
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    hideModal(id: string) {
      return this.$bvModal.hide(id)
    }

    mounted(): void {
      if (this.data !== null) {
        this.form = this.data
      }

      if (!this.form.erikoistuvanSahkoposti) {
        this.form.erikoistuvanSahkoposti = this.account.email
      }

      // Asetetaan ei-muokattavien kenttien arvot
      this.form.erikoistuvanNimi = `${this.account.firstName} ${this.account.lastName}`
      this.form.erikoistuvanOpiskelijatunnus = this.opintooikeusKaytossa?.opiskelijatunnus
      this.form.erikoistuvanErikoisala = this.opintooikeusKaytossa?.erikoisalaNimi
      this.form.erikoistuvanYliopisto = this.account.erikoistuvaLaakari.yliopisto

      if (!this.form.koejaksonAlkamispaiva) {
        this.form.koejaksonAlkamispaiva = this.data?.koejaksonAlkamispaiva
      }

      this.local.tyotunnitViikossa = this.form.tyotunnitViikossa?.toString().replace('.', ',')
      this.childDataReceived = true
    }
  }
</script>

<style lang="scss" scoped>
  ::v-deep {
    .textarea-min-height {
      min-height: 200px;
    }
  }

  .tyotunnit-viikossa-input {
    width: 5rem;
  }
</style>
