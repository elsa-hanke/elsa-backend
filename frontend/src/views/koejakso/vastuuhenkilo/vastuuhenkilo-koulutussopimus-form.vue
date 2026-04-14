<template>
  <b-row>
    <b-col lg="4">
      <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
        <template #default="{ uid }">
          <b-form-input
            :id="uid"
            v-model="form.sahkoposti"
            :state="validateState('sahkoposti')"
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
</template>

<script lang="ts">
  import _get from 'lodash/get'
  import Component from 'vue-class-component'
  import { Prop, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { email, required } from 'vuelidate/lib/validators'

  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import store from '@/store'
  import { Vastuuhenkilo } from '@/types'
  import { defaultVastuuhenkilo, phoneNumber } from '@/utils/constants'

  @Component({
    components: {
      ElsaFormGroup
    },
    validations: {
      form: {
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
  export default class VastuuhenkiloKoulutussopimusForm extends Mixins(validationMixin) {
    @Prop({ required: true, default: {} })
    vastuuhenkilo!: Vastuuhenkilo

    form: Vastuuhenkilo = defaultVastuuhenkilo

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

      this.$emit('ready', this.form)
    }

    mounted() {
      this.form = this.vastuuhenkilo
    }
  }
</script>
