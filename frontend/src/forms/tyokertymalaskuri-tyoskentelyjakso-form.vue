<template>
  <b-form @submit.stop.prevent="onSubmit">
    <elsa-form-group :label="$t('tyoskentelypaikka')" :required="!value.tapahtumia">
      <template #default="{ uid }">
        <div>
          <b-form-input
            :id="uid"
            v-model="form.tyoskentelypaikka.nimi"
            :state="validateState('tyoskentelypaikka.nimi')"
            @input="$emit('skipRouteExitConfirm', false)"
          ></b-form-input>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('kaytannon-koulutus')" :required="true">
      <template #default="{ uid }">
        <div>
          <b-form-radio
            v-model="form.kaytannonKoulutus"
            name="kaytannon-koulutus-tyyppi"
            :state="validateState('kaytannonKoulutus')"
            :value="omanErikoisalanKoulutus"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            {{ $t('oman-erikoisalan-koulutus') }}
          </b-form-radio>
          <b-form-radio
            v-model="form.kaytannonKoulutus"
            name="kaytannon-koulutus-tyyppi"
            :state="validateState('kaytannonKoulutus')"
            :value="muuErikoisala"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            {{ $t('muu-erikoisala') }}
          </b-form-radio>
          <b-form-radio
            v-model="form.kaytannonKoulutus"
            name="kaytannon-koulutus-tyyppi"
            :state="validateState('kaytannonKoulutus')"
            :value="kahdenVuodenKliininenTyokokemus"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            {{ $t('kahden-vuoden-kliininen-tyokokemus') }}
          </b-form-radio>
          <b-form-radio
            v-model="form.kaytannonKoulutus"
            name="kaytannon-koulutus-tyyppi"
            :state="validateState('kaytannonKoulutus')"
            :value="terveyskeskustyo"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            {{ $t('pakollinen-terveyskeskuskoulutusjakso') }}
          </b-form-radio>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <b-form-row>
      <elsa-form-group
        :label="$t('alkamispaiva')"
        class="col-xs-12 col-sm-6 pr-sm-3"
        :required="!value.tapahtumia"
      >
        <template #default="{ uid }">
          <div>
            <elsa-form-datepicker
              v-if="childDataReceived"
              :id="uid"
              ref="alkamispaiva"
              :value.sync="form.alkamispaiva"
              :max="maxAlkamispaiva"
              :max-error-text="
                value.tapahtumia
                  ? $t('tyoskentelyjakso-datepicker-help')
                  : $t('alkamispaiva-ei-voi-olla-paattymispaivan-jalkeen')
              "
              @input="$emit('skipRouteExitConfirm', false)"
            ></elsa-form-datepicker>
          </div>
        </template>
      </elsa-form-group>
      <elsa-form-group :label="$t('paattymispaiva')" class="col-xs-12 col-sm-6 pl-sm-3">
        <template #default="{ uid }">
          <elsa-form-datepicker
            v-if="childDataReceived"
            :id="uid"
            ref="paattymispaiva"
            :value.sync="form.paattymispaiva"
            :min="minPaattymispaiva"
            :max="maxPaattymispaiva"
            :min-error-text="
              value.tapahtumia
                ? $t('tyoskentelyjakso-datepicker-help')
                : $t('paattymispaiva-ei-voi-olla-ennen-alkamispaivaa')
            "
            :required="false"
            :aria-describedby="`${uid}-help`"
            class="datepicker-range"
            @input="$emit('skipRouteExitConfirm', false)"
          />
          <small v-if="value.tapahtumia" class="form-text text-muted">
            {{ $t('tyoskentelyjakso-paattymispaiva-help') }}
          </small>
        </template>
      </elsa-form-group>
    </b-form-row>
    <elsa-form-group
      v-if="String(form.kaytannonKoulutus) !== String(kahdenVuodenKliininenTyokokemus)"
      :label="$t('tyoaika-taydesta-tyopaivasta') + ' (50–100 %)'"
      :required="!value.tapahtumia"
    >
      <template #default="{ uid }">
        <div>
          <div class="d-flex align-items-center">
            <b-form-input
              :id="uid"
              :value="form.osaaikaprosentti"
              :state="validateState('osaaikaprosentti')"
              type="number"
              step="any"
              class="col-sm-3"
              @input="onOsaaikaprosenttiInput"
            />
            <span class="mx-3">%</span>
          </div>
          <b-form-invalid-feedback
            :id="`${uid}-feedback`"
            :style="{
              display: validateState('osaaikaprosentti') === false ? 'block' : 'none'
            }"
          >
            {{ $t('osaaikaprosentti-validointivirhe') }} 50–100 %
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <elsa-form-group v-else :label="$t('tyoaikaprosentti')" :required="!value.tapahtumia">
      <template #default="{ uid }">
        <div>
          <div class="d-flex align-items-center">
            <b-form-input
              :id="uid"
              :value="form.kahdenvuodenosaaikaprosentti"
              :state="validateState('kahdenvuodenosaaikaprosentti')"
              type="number"
              step="any"
              class="col-sm-3"
              @input="onOsaaikaprosenttiInput2"
            />
            <span class="mx-3">%</span>
          </div>
          <b-form-invalid-feedback
            :id="`${uid}-feedback`"
            :style="{
              display: validateState('kahdenvuodenosaaikaprosentti') === false ? 'block' : 'none'
            }"
          >
            {{ $t('osaaikaprosentti-validointivirhe') }} 1–100 %
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <hr />
    <div>
      <div v-for="(poissaolo, index) in form.poissaolot" :key="index">
        <tyokertymalaskuri-tyoskentelyjakso-poissaolo-form
          :poissaolo="poissaolo"
          :poissaolon-syyt-sorted="poissaolonSyytSorted"
          :child-data-received="childDataReceived"
          :tyojakso-alkamispaiva="form.alkamispaiva"
          :tyojakso-paattymispaiva="form.paattymispaiva || getISODateNow()"
          @input="onPoissaoloInput($event, index)"
        />
        <elsa-button variant="link" class="shadow-none p-0 mt-2" @click="removePoissaolo(index)">
          <font-awesome-icon :icon="['far', 'trash-alt']" />
          {{ $t('poista-poissaolo') }}
        </elsa-button>
        <hr />
      </div>
      <b-button variant="outline-primary" :disabled="!form.alkamispaiva" @click="addPoissaolo">
        {{ $t('lisaa-jaksolle-poissaolo') }}
      </b-button>
    </div>
    <hr />
    <div class="d-flex flex-row-reverse flex-wrap">
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2 mb-2">
        {{ editing ? $t('tallenna') : $t('tallenna-jakso') }}
      </elsa-button>
      <elsa-button
        v-if="tyoskentelyjakso"
        variant="outline-danger"
        class="mb-2"
        @click.stop.prevent="onDelete(tyoskentelyjakso.id)"
      >
        {{ $t('poista-jakso') }}
      </elsa-button>
      <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
        {{ $t('peruuta') }}
      </elsa-button>
    </div>
    <div class="row">
      <elsa-form-error :active="$v.$anyError" />
    </div>
  </b-form>
</template>

<script lang="ts">
  import axios from 'axios'
  import Component from 'vue-class-component'
  import { Prop, Vue } from 'vue-property-decorator'
  import { between, integer, required } from 'vuelidate/lib/validators'

  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPoissaolonSyyt from '@/components/poissaolon-syyt/poissaolon-syyt.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import TyokertymalaskuriTyoskentelyjaksoPoissaoloForm from '@/forms/tyokertymalaskuri-tyoskentelyjakso-poissaolo-form.vue'
  import {
    Poissaolo,
    PoissaolonSyy,
    TyokertymaLaskuriTyoskentelyjakso,
    TyokertymaLaskuriTyoskentelyjaksoForm,
    Tyoskentelyjakso
  } from '@/types'
  import { KaytannonKoulutusTyyppi, PoissaolonSyyTyyppi } from '@/utils/constants'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'
  import { tyoskentelypaikkaTyyppiLabel } from '@/utils/tyoskentelyjakso'
  import KouluttajaKoulutussopimusForm from '@/views/koejakso/kouluttaja/kouluttaja-koulutussopimus-form.vue'

  @Component({
    components: {
      TyokertymalaskuriTyoskentelyjaksoPoissaoloForm,
      ElsaPoissaolonSyyt,
      KouluttajaKoulutussopimusForm,
      AsiakirjatContent,
      AsiakirjatUpload,
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      ElsaFormDatepicker,
      ElsaPopover
    },
    validations: {
      form: {
        tyoskentelypaikka: {
          required,
          nimi: {
            required
          }
        },
        osaaikaprosentti: {
          required,
          integer,
          between: between(50, 100)
        },
        kaytannonKoulutus: {
          required
        },
        kahdenvuodenosaaikaprosentti: {
          required,
          integer,
          between: between(1, 100)
        }
      }
    }
  })
  export default class TyokertymalaskuriTyoskentelyjaksoForm extends Vue {
    $refs!: {
      alkamispaiva: ElsaFormDatepicker
      paattymispaiva: ElsaFormDatepicker
    }

    @Prop({ required: false })
    tyoskentelyjakso!: TyokertymaLaskuriTyoskentelyjakso

    @Prop({
      required: false,
      type: Object,
      default: () => ({
        alkamispaiva: null,
        paattymispaiva: null,
        osaaikaprosentti: null,
        tyoskentelypaikka: {
          nimi: null,
          tyyppi: null,
          muuTyyppi: null
        }
      })
    })
    value!: Tyoskentelyjakso

    form: TyokertymaLaskuriTyoskentelyjaksoForm = {
      alkamispaiva: null,
      paattymispaiva: null,
      minPaattymispaiva: null,
      maxAlkamispaiva: null,
      osaaikaprosentti: 100,
      kahdenvuodenosaaikaprosentti: 100,
      tyoskentelypaikka: {
        nimi: null,
        tyyppi: null,
        muuTyyppi: null
      },
      kaytannonKoulutus: null,
      poissaolot: []
    }
    params = {
      saving: false,
      deleting: false
    }
    childDataReceived = false
    editing = false

    poissaolonSyyt: PoissaolonSyy[] = []

    async mounted() {
      await this.fetchPoissaolonSyyt()
      this.childDataReceived = true
      if (this.tyoskentelyjakso) {
        this.editing = true
        this.form.alkamispaiva = this.tyoskentelyjakso.alkamispaiva
        this.form.paattymispaiva = this.tyoskentelyjakso.paattymispaiva
        this.form.tyoskentelypaikka = this.tyoskentelyjakso.tyoskentelypaikka
        this.form.osaaikaprosentti = this.tyoskentelyjakso.osaaikaprosentti
        this.form.kahdenvuodenosaaikaprosentti = this.tyoskentelyjakso.kahdenvuodenosaaikaprosentti
        this.form.kaytannonKoulutus = this.tyoskentelyjakso.kaytannonKoulutus
        this.form.poissaolot = this.tyoskentelyjakso.poissaolot
      }
    }

    async fetchPoissaolonSyyt() {
      try {
        this.poissaolonSyyt = (await axios.get(`/julkinen/poissaolon-syyt`)).data
      } catch {
        toastFail(this, this.$t('poissaolon-syiden-hakeminen-epaonnistui'))
      }
    }

    validateState(name: string) {
      const get = (obj: any, path: any, defaultValue = undefined) => {
        const travel = (regexp: any) =>
          String.prototype.split
            .call(path, regexp)
            .filter(Boolean)
            .reduce((res, key) => (res !== null && res !== undefined ? res[key] : res), obj)
        const result = travel(/[,[\]]+?/) || travel(/[,[\].]+?/)
        return result === undefined || result === obj ? defaultValue : result
      }
      const { $dirty, $error } = get(this.$v.form, name)
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    validatePoissaolot() {
      let isValid = true
      this.$emit('validate-all-poissaolot')
      this.form.poissaolot.forEach((poissaolo, index) => {
        if (!poissaolo.poissaolonSyy.id || !poissaolo.alkamispaiva || !poissaolo.paattymispaiva) {
          isValid = false
          this.$set(poissaolo, 'invalid', true)
          console.log(`Invalid Poissaolo at index ${index}:`, poissaolo)
        } else {
          this.$set(poissaolo, 'invalid', false)
        }
      })
      return isValid
    }

    async onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.alkamispaiva ? this.$refs.alkamispaiva.validateForm() : true,
        this.$refs.paattymispaiva.validateForm(),
        this.validatePoissaolot()
      ]

      if (validations.includes(false)) {
        return
      }

      const submitData: TyokertymaLaskuriTyoskentelyjakso = {
        id: -1,
        tyoskentelypaikka: this.form.tyoskentelypaikka,
        alkamispaiva: this.form.alkamispaiva as string,
        paattymispaiva: this.form.paattymispaiva as string,
        kaytannonKoulutus: this.form.kaytannonKoulutus as KaytannonKoulutusTyyppi,
        osaaikaprosentti: this.form.osaaikaprosentti || 100,
        kahdenvuodenosaaikaprosentti: this.form.kahdenvuodenosaaikaprosentti || 100,
        poissaolot: this.form.poissaolot
      }
      if (this.editing) {
        submitData.id = this.tyoskentelyjakso.id
      }
      this.$emit('skipRouteExitConfirm', true)
      this.$emit('submit', submitData, this.params)
    }

    onOsaaikaprosenttiInput(value: string) {
      if (value === '') {
        this.form.osaaikaprosentti = null
      } else {
        this.form.osaaikaprosentti = parseFloat(value)
      }
      this.$emit('skipRouteExitConfirm', false)
    }

    onOsaaikaprosenttiInput2(value: string) {
      if (value === '') {
        this.form.kahdenvuodenosaaikaprosentti = null
      } else {
        this.form.kahdenvuodenosaaikaprosentti = parseFloat(value)
      }
      this.$emit('skipRouteExitConfirm', false)
    }

    onCancel() {
      this.$emit('skipRouteExitConfirm', true)
      this.$emit('cancel')
    }

    onDelete(id: number | undefined) {
      if (id) {
        this.$emit('skipRouteExitConfirm', true)
        this.$emit('delete', id)
      }
    }

    get maxAlkamispaiva() {
      if (this.value.tapahtumia) {
        return this.form.maxAlkamispaiva
      } else {
        return this.form.paattymispaiva === null ? this.getISODateNow() : this.form.paattymispaiva
      }
    }

    get minPaattymispaiva() {
      if (this.value.tapahtumia) {
        return this.form.minPaattymispaiva
      } else {
        return this.form.alkamispaiva
      }
    }

    get maxPaattymispaiva() {
      return this.getISODateNow()
    }

    get tyyppiLabel() {
      if (this.form?.tyoskentelypaikka?.tyyppi) {
        return tyoskentelypaikkaTyyppiLabel(this, this.form?.tyoskentelypaikka?.tyyppi)
      }
      return undefined
    }

    get omanErikoisalanKoulutus() {
      return KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS
    }

    get muuErikoisala() {
      return KaytannonKoulutusTyyppi.MUU_ERIKOISALA
    }

    get kahdenVuodenKliininenTyokokemus() {
      return KaytannonKoulutusTyyppi.KAHDEN_VUODEN_KLIININEN_TYOKOKEMUS
    }

    get terveyskeskustyo() {
      return KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
    }

    get poissaolonSyytSorted() {
      return [...this.poissaolonSyyt.sort((a, b) => sortByAsc(a.nimi, b.nimi))]
    }

    addPoissaolo() {
      this.form.poissaolot.push({
        alkamispaiva: '',
        paattymispaiva: '',
        tyoskentelyjakso: {
          id: -1,
          osaaikaprosentti: 100,
          kahdenvuodenosaaikaprosentti: 100,
          paattymispaiva: this.form.paattymispaiva || this.getISODateNow()
        },
        poissaolonSyyId: 0,
        poissaolonSyy: {
          id: 0,
          nimi: '',
          vahennystyyppi: PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN,
          vahennetaanKerran: false,
          voimassaolonAlkamispaiva: '',
          voimassaolonPaattymispaiva: null
        },
        tyoskentelyjaksoId: 0,
        kokoTyoajanPoissaolo: true,
        poissaoloprosentti: 100
      })
    }

    removePoissaolo(index: number) {
      this.form.poissaolot.splice(index, 1)
    }

    onPoissaoloInput(updatedPoissaolo: Poissaolo, index: number) {
      updatedPoissaolo.poissaoloprosentti = updatedPoissaolo.kokoTyoajanPoissaolo ? 100 : 0
      this.$set(this.form.poissaolot, index, updatedPoissaolo)
    }

    getISODateNow() {
      const date = new Date()
      return date.toISOString().split('T')[0]
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .datepicker-range::before {
    content: '–';
    position: absolute;
    left: -1.5rem;
    padding: 0.375rem 0.75rem;
    @include media-breakpoint-down(xs) {
      display: none;
    }
  }
</style>
