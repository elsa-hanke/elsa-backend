<template>
  <b-form @submit.stop.prevent="onSubmit">
    <b-row>
      <b-col lg="8">
        <h5>{{ $t('opinto-oikeuden-alkamispäivä') }}</h5>
        <p v-if="form.opintooikeudenMyontamispaiva">
          {{ $date(form.opintooikeudenMyontamispaiva) }}
        </p>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="4">
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
              :min="form.opintooikeudenMyontamispaiva"
              :min-error-text="$t('koejakso-ei-voi-alkaa-ennen-opinto-oikeuden-myontamispaivaa')"
              :max="maxKoejaksonAlkamispaiva"
              :max-error-text="
                $t(
                  'koejakson-voi-aloittaa-viimeistaan-puoli-vuotta-ennen-opinto-oikeuden-paattymista'
                )
              "
              :disabled="!form.opintooikeudenMyontamispaiva"
              @input="$emit('skipRouteExitConfirm', false)"
            ></elsa-form-datepicker>
          </template>
        </elsa-form-group>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="4">
        <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.erikoistuvanSahkoposti"
              :state="validateState('erikoistuvanSahkoposti')"
              :value="account.email"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback
              v-if="$v.form.erikoistuvanSahkoposti && !$v.form.erikoistuvanSahkoposti.required"
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
              :value="account.phoneNumber"
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
    <b-row>
      <b-col>
        <h3>{{ $t('koulutuspaikan-tiedot') }}</h3>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="8">
        <koulutuspaikka-details
          v-for="(koulutuspaikka, index) in form.koulutuspaikat"
          ref="koulutuspaikkaDetails"
          :key="index"
          :koulutuspaikka="koulutuspaikka"
          :yliopistot="yliopistot"
        ></koulutuspaikka-details>
        <elsa-button
          v-if="form.koulutuspaikat.length >= 2"
          variant="outline-primary"
          class="border-0 p-0"
          @click="deleteKoulutuspaikka"
        >
          <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="lg" />
          {{ $t('poista-toimipaikka') }}
        </elsa-button>
        <elsa-button
          v-else
          variant="outline-primary"
          class="border-0 p-0"
          @click="addKoulutuspaikka"
        >
          + {{ $t('toinen-toimipaikka') }}
        </elsa-button>
      </b-col>
    </b-row>
    <hr />
    <b-row>
      <b-col lg="8">
        <h3>{{ $t('koulutuspaikan-lahikouluttaja') }}</h3>
        <p class="mb-4">
          {{ $t('valitse-kouluttaja-help') }}
          <b-link :to="{ name: 'profiili' }">{{ $t('profiilissasi') }}</b-link>
        </p>
        <p>
          {{ $t('valitse-kouluttaja-help2') }}
        </p>
        <kouluttaja-details
          v-for="(k, index) in form.kouluttajat"
          ref="kouluttajaDetails"
          :key="index"
          :index="index"
          :kouluttaja="k"
          :kouluttajat="kouluttajatList"
          @kouluttajaSelected="selectKouluttaja"
          @clearKouluttaja="clearKouluttaja"
        ></kouluttaja-details>
        <elsa-button
          v-if="form.kouluttajat.length >= 2"
          variant="outline-primary"
          class="border-0 p-0"
          @click="deleteKouluttaja"
        >
          <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="lg" />
          {{ $t('lahikouluttaja-poista') }}
        </elsa-button>
        <elsa-button v-else variant="outline-primary" class="border-0 p-0" @click="addKouluttaja">
          + {{ $t('lahikouluttaja-toinen') }}
        </elsa-button>
      </b-col>
    </b-row>
    <hr />
    <b-row>
      <b-col lg="8">
        <h3>{{ $t('erikoisala-vastuuhenkilö') }}</h3>
        <h5>{{ $t('erikoisala-vastuuhenkilö-label') }}</h5>
        <p>
          {{ vastuuhenkilo.nimi }}
          {{ vastuuhenkilo.nimike ? ', ' + vastuuhenkilo.nimike : '' }}
        </p>
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
          class="my-2 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block"
          style="min-width: 14rem"
          variant="outline-primary"
          :disabled="buttonStates.primaryButtonLoading"
          :loading="buttonStates.secondaryButtonLoading"
          @click="resetValidationsAndConfirm"
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
          {{ $t('hyvaksy-laheta') }}
        </elsa-button>
      </b-col>
    </b-row>
    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('vahvista-koulutussopimus-lahetys')"
      :submit-text="$t('hyvaksy-laheta')"
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
  import Component from 'vue-class-component'
  import { Vue, Prop, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required, email } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import {
    KoejaksonVaiheButtonStates,
    Kouluttaja,
    KoulutussopimusLomake,
    UserAccount,
    Opintooikeus,
    Vastuuhenkilo
  } from '@/types'
  import { defaultKouluttaja, defaultKoulutuspaikka, phoneNumber } from '@/utils/constants'
  import { formatList } from '@/utils/kouluttajaAndVastuuhenkiloListFormatter'
  import { resolveOpintooikeusKaytossa } from '@/utils/opintooikeus'
  import KouluttajaDetails from '@/views/koejakso/erikoistuva/koulutussopimus/kouluttaja-details.vue'
  import KoulutuspaikkaDetails from '@/views/koejakso/erikoistuva/koulutussopimus/koulutuspaikka-details.vue'

  @Component({
    components: {
      KoulutuspaikkaDetails,
      KouluttajaDetails,
      ElsaFormGroup,
      ElsaFormDatepicker,
      ElsaButton,
      ElsaPopover,
      ElsaConfirmationModal
    },
    validations: {
      form: {
        erikoistuvanSahkoposti: {
          required,
          email
        },
        erikoistuvanPuhelinnumero: {
          required,
          phoneNumber
        }
      }
    }
  })
  export default class KoulutussopimusForm extends Mixins(validationMixin) {
    $refs!: {
      kouluttajaDetails: any
      koulutuspaikkaDetails: any
      koejaksonAlkamispaiva: ElsaFormDatepicker
    }

    @Prop({ required: true, default: {} })
    data!: KoulutussopimusLomake

    @Prop({ required: true, default: {} })
    account!: UserAccount

    @Prop({ required: true, default: () => [] })
    kouluttajat!: Kouluttaja[]

    @Prop({ required: true, default: () => [] })
    yliopistot!: []

    @Prop({ required: true, default: () => [] })
    vastuuhenkilo!: Vastuuhenkilo

    form: KoulutussopimusLomake = {
      id: null,
      erikoistuvanNimi: '',
      erikoistuvanErikoisala: '',
      erikoistuvanOpiskelijatunnus: '',
      erikoistuvanPuhelinnumero: '',
      erikoistuvanSahkoposti: '',
      erikoistuvanSyntymaaika: '',
      koejaksonAlkamispaiva: '',
      korjausehdotus: '',
      vastuuhenkilonKorjausehdotus: '',
      kouluttajat: [],
      koulutuspaikat: [],
      lahetetty: false,
      muokkauspaiva: '',
      opintooikeudenMyontamispaiva: '',
      opintooikeudenPaattymispaiva: '',
      vastuuhenkilo: null,
      yliopistot: []
    }
    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }
    childDataReceived = false

    validateState(value: string) {
      const form = this.$v.form
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    get koejaksonPaattymispaiva() {
      return ''
    }

    addKoulutuspaikka() {
      this.form.koulutuspaikat.push(Object.assign({}, defaultKoulutuspaikka))
      this.$emit('skipRouteExitConfirm', false)
    }

    deleteKoulutuspaikka() {
      this.form.koulutuspaikat.pop()
      this.$emit('skipRouteExitConfirm', false)
    }

    selectKouluttaja(kouluttaja: Kouluttaja, index: number) {
      Vue.set(this.form.kouluttajat, index, kouluttaja)
      this.$emit('skipRouteExitConfirm', false)
    }

    addKouluttaja() {
      this.form.kouluttajat.push(defaultKouluttaja)
      this.$emit('skipRouteExitConfirm', false)
    }

    deleteKouluttaja() {
      this.form.kouluttajat.pop()
      this.$emit('skipRouteExitConfirm', false)
    }

    clearKouluttaja(value: Kouluttaja) {
      const kouluttajat = this.form.kouluttajat.map((k: any) => {
        if (k.kayttajaId && k.kayttajaId !== value.id) {
          return {
            ...k,
            $isDisabled: false
          }
        }
      })
      this.form.kouluttajat = kouluttajat
    }

    get kouluttajatList() {
      const selectedKouluttajat =
        this.form.kouluttajat[0]?.kayttajaId || this.form.kouluttajat[1]?.kayttajaId
          ? this.form.kouluttajat.map((k) => {
              return k ? k.kayttajaId : null
            })
          : null

      const kouluttajat = this.kouluttajat.map((k: any) => {
        if (selectedKouluttajat?.includes(k.id)) {
          return {
            ...k,
            $isDisabled: true
          }
        } else {
          return {
            ...k,
            $isDisabled: false
          }
        }
      })
      return formatList(this, kouluttajat)
    }

    get opintooikeusKaytossa(): Opintooikeus | undefined {
      return resolveOpintooikeusKaytossa(this.account.erikoistuvaLaakari)
    }

    get maxKoejaksonAlkamispaiva() {
      const dateFormat = 'yyyy-MM-dd'
      if (!this.opintooikeusKaytossa?.opintooikeudenPaattymispaiva) {
        return null
      }

      const d = new Date(this.opintooikeusKaytossa.opintooikeudenPaattymispaiva)
      // Koejakson voi aloittaa viimeistään 6kk ennen määrä-aikaisen
      // opinto-oikeuden päättymispäivää, koska koejakson kesto on 6kk.
      d.setMonth(d.getMonth() - 6)
      return format(d, dateFormat)
    }

    nimikeLabel(nimike: string) {
      if (nimike) {
        return ', ' + nimike
      }
      return ''
    }

    saveAndExit() {
      this.$emit('saveAndExit', this.form, this.buttonStates)
    }

    resetValidationsAndConfirm() {
      this.$v.$reset()
      this.$refs.koejaksonAlkamispaiva.$v.$reset()
      this.$refs.kouluttajaDetails.forEach((k: any) => {
        k.$v.$reset()
      })
      this.$refs.koulutuspaikkaDetails.forEach((k: any) => {
        k.$v.$reset()
      })
      return this.$bvModal.show('confirm-save')
    }

    validateAndConfirmSend() {
      let childFormsValid = true
      this.$v.form.$touch()
      this.$refs.kouluttajaDetails.forEach((k: any) => {
        if (!k.validateForm()) {
          childFormsValid = false
        }
      })

      this.$refs.koulutuspaikkaDetails.forEach((k: any) => {
        if (!k.validateForm()) {
          childFormsValid = false
        }
      })
      childFormsValid = !this.$refs.koejaksonAlkamispaiva.validateForm() ? false : childFormsValid

      if (this.$v.form.$anyError || !childFormsValid) {
        return
      }

      return this.$bvModal.show('confirm-send')
    }

    onSubmit() {
      this.$emit('submit', this.form, this.buttonStates)
    }

    mounted(): void {
      if (this.data !== null) {
        this.form = this.data
      } else {
        this.form.kouluttajat.push(defaultKouluttaja)
        // Luo kopio defaultKoulutuspaikasta, koska muutoin mahdollinen toinen koulutuspaikka mäppäytyy samaan objektiin.
        this.form.koulutuspaikat.push(Object.assign({}, defaultKoulutuspaikka))
      }

      // Asetetaan ei-muokattavien kenttien arvot
      this.form.erikoistuvanNimi = `${this.account.firstName} ${this.account.lastName}`
      this.form.erikoistuvanOpiskelijatunnus = this.opintooikeusKaytossa?.opiskelijatunnus
      this.form.erikoistuvanErikoisala = this.opintooikeusKaytossa?.erikoisalaNimi
      this.form.erikoistuvanSyntymaaika = this.account.erikoistuvaLaakari.syntymaaika
      this.form.opintooikeudenMyontamispaiva =
        this.opintooikeusKaytossa?.opintooikeudenMyontamispaiva

      // Asetetaan arvot kentille, jotka saatavissa erikoistuvan lääkärin tiedoista, mutta jotka
      // käyttäjä on saattanut yliajaa lomakkeen välitallennuksen yhteydessä. Kuitenkaan opinto-oikeuden
      // alkamispäivää käyttäjä ei voi yliajaa, mikäli se on saatu opintotietojärjestelmästä.
      if (!this.form.erikoistuvanPuhelinnumero) {
        this.form.erikoistuvanPuhelinnumero =
          this.account.erikoistuvaLaakari.puhelinnumero ?? this.account.phoneNumber
      }
      if (!this.form.erikoistuvanSahkoposti) {
        this.form.erikoistuvanSahkoposti = this.account.email
      }
      this.form.vastuuhenkilo = this.vastuuhenkilo
      this.childDataReceived = true
    }
  }
</script>
