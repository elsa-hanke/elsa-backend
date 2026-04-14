<template>
  <b-form @submit.stop.prevent="onSubmit">
    <p>{{ $t('kayton-aloitus-ingressi') }}</p>
    <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
      <template #default="{ uid }">
        <b-form-input
          :id="uid"
          v-model="form.sahkoposti"
          :state="validateState('sahkoposti')"
        ></b-form-input>
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
    <elsa-form-group :label="$t('vahvista-sahkopostiosoite')" :required="true">
      <template #default="{ uid }">
        <b-form-input
          :id="uid"
          v-model="form.sahkopostiUudelleen"
          :state="validateState('sahkopostiUudelleen')"
        ></b-form-input>
        <b-form-invalid-feedback
          v-if="$v.form.sahkopostiUudelleen && !$v.form.sahkopostiUudelleen.required"
          :id="`${uid}-feedback`"
        >
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
        <b-form-invalid-feedback
          v-if="$v.form.sahkopostiUudelleen && !$v.form.sahkopostiUudelleen.email"
          :id="`${uid}-feedback`"
        >
          {{ $t('sahkopostiosoite-ei-kelvollinen') }}
        </b-form-invalid-feedback>
        <b-form-invalid-feedback
          v-if="
            $v.form.sahkopostiUudelleen &&
            $v.form.sahkopostiUudelleen.required &&
            $v.form.sahkopostiUudelleen.email &&
            !$v.form.sahkopostiUudelleen.sameAsSahkoposti
          "
          :id="`${uid}-feedback`"
        >
          {{ $t('sahkopostiosoitteet-eivat-tasmaa') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>

    <elsa-form-group
      v-if="hasMultipleOpintooikeus"
      :label="$t('useampi-opinto-oikeus')"
      :required="true"
    >
      <template #default="{ uid }">
        <p class="mb-4">{{ $t('useampi-opinto-oikeus-ingressi') }}</p>
        <b-form-radio-group
          :id="uid"
          v-model="form.opintooikeusId"
          :options="opintooikeudetOptions"
          :state="validateState('opintooikeusId')"
          stacked
        ></b-form-radio-group>
        <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateState('opintooikeusId')">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <div class="text-right">
      <b-form ref="logoutForm" :action="logoutUrl" method="POST" />
      <elsa-button variant="back" @click="logout()">
        {{ $t('keskeyta-ja-kirjaudu-ulos') }}
      </elsa-button>
      <elsa-button type="submit" variant="primary" class="ml-2">
        {{ $t('aloita-palvelun-kaytto') }}
      </elsa-button>
    </div>
  </b-form>
</template>
<script lang="ts">
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required, requiredIf, email, sameAs } from 'vuelidate/lib/validators'

  import { ELSA_API_LOCATION } from '@/api'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import store from '@/store'
  import { Opintooikeus, KaytonAloitusModel } from '@/types'
  @Component({
    components: {
      ElsaFormGroup,
      ElsaFormMultiselect,
      UserAvatar,
      ElsaButton
    }
  })
  export default class KaytonAloitusForm extends Mixins(validationMixin) {
    validations() {
      return {
        form: {
          sahkoposti: {
            required,
            email
          },
          sahkopostiUudelleen: {
            required,
            email,
            sameAsSahkoposti: sameAs('sahkoposti')
          },
          opintooikeusId: {
            required: requiredIf(() => {
              return this.hasMultipleOpintooikeus
            })
          }
        }
      }
    }

    @Prop({ required: true, default: [] })
    opintooikeudet!: Opintooikeus[]

    form: KaytonAloitusModel = {
      sahkoposti: null,
      sahkopostiUudelleen: null,
      opintooikeusId: null
    }

    get logoutUrl() {
      return ELSA_API_LOCATION + '/api/logout'
    }

    async logout() {
      await store.dispatch('auth/logout')

      if (store.getters['auth/sloEnabled'] === true) {
        const logoutForm = this.$refs.logoutForm as HTMLFormElement
        logoutForm.submit()
      }
    }

    get hasMultipleOpintooikeus() {
      return this.opintooikeudet?.length > 1
    }

    get opintooikeudetOptions() {
      return this.opintooikeudet.map((o: Opintooikeus) => ({
        text: `${o.erikoisalaNimi}, ${this.$t(`yliopisto-nimi.${o.yliopistoNimi}`)}`,
        value: o.id
      }))
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    onSubmit() {
      this.$v.form.$touch()
      if (!this.$v.$anyError) {
        this.$emit('submit', this.form)
      }
    }
  }
</script>
