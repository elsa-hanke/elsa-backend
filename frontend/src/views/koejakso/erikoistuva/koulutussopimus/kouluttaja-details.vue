<template>
  <elsa-form-group
    :label="$t('lahikouluttaja')"
    :add-new-enabled="true"
    :add-new-label="$t('lisaa-kouluttaja')"
    :required="true"
    @submit="onKouluttajaSubmit"
  >
    <template #modal-content="{ submit, cancel }">
      <kouluttaja-form :kouluttajat="kouluttajat" @submit="submit" @cancel="cancel" />
    </template>
    <template #default="{ uid }">
      <elsa-form-multiselect
        :id="uid"
        v-model="form.kouluttaja"
        :options="kouluttajat"
        :state="validateState('kouluttaja')"
        label="nimi"
        track-by="id"
        @select="onKouluttajaSelect"
        @valueToBeCleared="clearKouluttaja"
      >
        <template #option="{ option }">
          <div v-if="option.nimi">{{ optionDisplayName(option) }}</div>
        </template>
      </elsa-form-multiselect>
      <b-form-invalid-feedback :id="`${uid}-feedback`">
        {{ $t('pakollinen-tieto') }}
      </b-form-invalid-feedback>
    </template>
  </elsa-form-group>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { BModal } from 'bootstrap-vue'
  import Avatar from 'vue-avatar'
  import Component from 'vue-class-component'
  import { Prop, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import { postLahikouluttaja } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import KouluttajaForm from '@/forms/kouluttaja-form.vue'
  import store from '@/store'
  import { Kayttaja, Kouluttaja, ElsaError } from '@/types'
  import { defaultKouluttaja } from '@/utils/constants'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaButton,
      KouluttajaForm,
      ElsaFormMultiselect,
      Avatar
    },
    validations: {
      form: {
        kouluttaja: {
          id: {
            required
          }
        }
      }
    }
  })
  export default class KouluttajaDetails extends Mixins(validationMixin) {
    @Prop({ required: true, default: null })
    kouluttaja!: Kouluttaja

    @Prop({ required: false, default: () => [] })
    kouluttajat!: Kouluttaja[]

    @Prop({ required: true, default: null })
    index!: number

    async onKouluttajaSubmit(value: Kayttaja, params: { saving: boolean }, modal: BModal) {
      params.saving = true
      try {
        await postLahikouluttaja(value)
        modal.hide('confirm')
        toastSuccess(this, this.$t('uusi-kouluttaja-lisatty'))
        await store.dispatch('erikoistuva/getKouluttajat')
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('uuden-kouluttajan-lisaaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('uuden-kouluttajan-lisaaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    form = {
      kouluttaja: null
    } as any

    mounted(): void {
      if (this.kouluttaja.kayttajaId) {
        this.form.kouluttaja = this.kouluttaja
      }
    }

    clearKouluttaja(value: Kouluttaja): void {
      this.$emit('clearKouluttaja', value)
    }

    onKouluttajaSelect(kouluttaja: any) {
      const value = {
        ...defaultKouluttaja,
        kayttajaId: kouluttaja.id,
        nimi: kouluttaja.nimi
      }
      this.form.kouluttaja = value
      this.$emit('kouluttajaSelected', value, this.index)
    }

    optionDisplayName(option: any) {
      return option.nimike ? option.nimi + ', ' + option.nimike : option.nimi
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? !$error : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      if (this.$v.$anyError) {
        return false
      }
      return true
    }
  }
</script>
