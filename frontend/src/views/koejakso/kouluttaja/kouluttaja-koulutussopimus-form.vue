<template>
  <div>
    <b-row class="mb-3">
      <b-col>
        <h5>{{ $t('lahikouluttajan-nimi') }}</h5>
        <p>{{ kouluttaja.nimi }}</p>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="4">
        <elsa-form-group :label="$t('lahikouluttajan-nimike')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.nimike"
              :state="validateState('nimike')"
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
      <b-col lg="4">
        <elsa-form-group :label="$t('toimipaikka')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.toimipaikka"
              :state="validateState('toimipaikka')"
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
      <b-col lg="4">
        <elsa-form-group :label="$t('lahiosoite')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.lahiosoite"
              :state="validateState('lahiosoite')"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-col>

      <b-col lg="4">
        <elsa-form-group :label="$t('postitoimipaikka')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.postitoimipaikka"
              :state="validateState('postitoimipaikka')"
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
      <b-col lg="4">
        <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.sahkoposti"
              :state="validateState('sahkoposti')"
              :value="account.email"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback
              v-if="$v.form.sahkoposti && !$v.form.sahkoposti.required"
              :id="`${uid}-feedback`"
            >
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
            <b-form-invalid-feedback
              v-if="$v.form.sahkoposti && !$v.form.sahkoposti.email"
              :id="`${uid}-feedback`"
              :state="validateState('sahkoposti')"
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
              v-model="form.puhelin"
              :state="validateState('puhelin')"
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
  </div>
</template>

<script lang="ts">
  import _get from 'lodash/get'
  import Component from 'vue-class-component'
  import { Prop, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { email, required } from 'vuelidate/lib/validators'

  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import store from '@/store'
  import { Kouluttaja } from '@/types'
  import { defaultKouluttaja, phoneNumber } from '@/utils/constants'

  @Component({
    components: {
      ElsaFormGroup
    },
    validations: {
      form: {
        nimike: {
          required
        },
        toimipaikka: {
          required
        },
        lahiosoite: {
          required
        },
        postitoimipaikka: {
          required
        },
        sahkoposti: {
          required,
          email
        },
        puhelin: {
          required,
          phoneNumber
        }
      }
    }
  })
  export default class KouluttajaKoulutussopimusForm extends Mixins(validationMixin) {
    @Prop({ required: true, default: {} })
    kouluttaja!: Kouluttaja

    @Prop({ required: true })
    index!: number

    form: Kouluttaja = defaultKouluttaja

    get kouluttajaIndex() {
      return this.index
    }

    get account() {
      return store.getters['auth/account']
    }

    validateState(value: string) {
      const form = this.$v.form
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm() {
      this.$v.$touch()
      if (this.$v.$anyError) {
        return
      }

      this.$emit('ready', this.kouluttajaIndex, this.form)
    }

    mounted() {
      this.form = this.kouluttaja
    }
  }
</script>
