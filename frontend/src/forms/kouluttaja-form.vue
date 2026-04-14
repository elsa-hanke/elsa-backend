<template>
  <b-form @submit.stop.prevent="onSubmit">
    <p class="mb-1">{{ $t('lisaa-kouluttaja-info-1') }}</p>
    <ul>
      <li class="font-weight-bold">{{ $t('lisaa-kouluttaja-info-2') }}</li>
      <li>{{ $t('lisaa-kouluttaja-info-3') }}</li>
      <li>{{ $t('lisaa-kouluttaja-info-4') }}</li>
      <li>{{ $t('lisaa-kouluttaja-info-5') }}</li>
    </ul>
    <b-form-row>
      <elsa-form-group
        :label="$t('kouluttajan-etunimi')"
        class="col-sm-12 col-md-5 pr-md-3"
        :required="true"
      >
        <template #default="{ uid }">
          <b-form-input
            :id="uid"
            v-model="form.etunimi"
            :state="validateState('etunimi')"
            trim
            @input="$emit('skipRouteExitConfirm', false)"
          ></b-form-input>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
      <elsa-form-group
        :label="$t('kouluttajan-sukunimi')"
        class="col-sm-12 col-md-7 pl-md-3"
        :required="true"
      >
        <template #default="{ uid }">
          <b-form-input
            :id="uid"
            v-model="form.sukunimi"
            :state="validateState('sukunimi')"
            trim
            @input="$emit('skipRouteExitConfirm', false)"
          ></b-form-input>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
    </b-form-row>
    <elsa-form-group :label="$t('kouluttajan-sahkoposti')" :required="true">
      <template #default="{ uid }">
        <b-form-input
          :id="uid"
          v-model="form.sahkoposti"
          :state="validateState('sahkoposti')"
          @input="$emit('skipRouteExitConfirm', false)"
        />
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <b-alert variant="danger" :show="samaSahkoposti">
      <div class="d-flex flex-row">
        <em class="align-middle">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
        </em>
        <div>{{ $t('sama-sahkoposti-loytyy') }}</div>
      </div>
    </b-alert>
    <b-alert variant="dark" :show="samaNimi">
      <div class="d-flex flex-row">
        <em class="align-middle">
          <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
        </em>
        <div class="font-weight-bold">{{ $t('samanniminen-kouluttaja-loytyy') }}:</div>
      </div>
      <ul class="ml-4">
        <li v-for="kouluttaja in samanNimiset" :key="kouluttaja.id">
          {{ kouluttaja.nimi }} ({{ kouluttaja.sahkoposti }})
        </li>
        <li class="font-weight-bold">{{ $t('lisaa-kouluttaja-info-2') }}</li>
        <li>{{ $t('lisaa-kouluttaja-info-6') }}</li>
        <li>{{ $t('lisaa-kouluttaja-info-7') }}</li>
      </ul>
    </b-alert>
    <div class="text-right">
      <elsa-button variant="back" @click.stop.prevent="onCancel">{{ $t('peruuta') }}</elsa-button>
      <elsa-button
        :loading="params.saving"
        type="submit"
        variant="primary"
        :disabled="samaSahkoposti"
        class="ml-2 pr-5 pl-5"
      >
        {{ $t('lisaa-kouluttaja') }}
      </elsa-button>
    </div>
  </b-form>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { Validation, validationMixin } from 'vuelidate'
  import { required, email } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import { Kouluttaja, UusiKouluttaja } from '@/types'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaButton
    },
    validations: {
      form: {
        etunimi: {
          required
        },
        sukunimi: {
          required
        },
        sahkoposti: {
          required,
          email
        }
      }
    }
  })
  export default class KouluttajaForm extends Mixins(validationMixin) {
    form: Partial<UusiKouluttaja> = {}
    params = {
      saving: false
    }

    @Prop({ required: true })
    kouluttajat!: Kouluttaja[]

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as Validation
      return $dirty ? ($error ? false : null) : null
    }

    get samanNimiset() {
      return this.kouluttajat.filter(
        (k) =>
          k.nimi.toLowerCase() ===
            `${this.form.etunimi?.toLowerCase()} ${this.form.sukunimi?.toLowerCase()}` &&
          k.sahkoposti !== this.form.sahkoposti
      )
    }

    get samaNimi() {
      return this.samanNimiset.length > 0
    }

    get samaSahkoposti() {
      return this.kouluttajat.filter((k) => k.sahkoposti === this.form.sahkoposti).length > 0
    }

    onSubmit() {
      this.$v.form.$touch()
      if (this.$v.form.$anyError || this.samaSahkoposti) {
        return
      }
      this.$emit('submit', this.form, this.params)
    }

    onCancel() {
      this.$emit('cancel')
    }
  }
</script>
