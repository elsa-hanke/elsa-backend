<template>
  <b-row>
    <b-col v-if="!isReadonly" lg="10">
      <h3>{{ $t('koulutuspaikan-arvioijat') }}</h3>
      <p>
        {{ $t('valitse-arvioijat-help') }}
        <b-link :to="{ name: 'profiili' }">{{ $t('profiilissasi') }}</b-link>
      </p>
      <div v-if="!loading">
        <elsa-form-group
          :label="$t('lahikouluttaja')"
          :add-new-enabled="true"
          :add-new-label="$t('lisaa-kouluttaja')"
          :required="true"
          @submit="onLahikouluttajaSubmit"
        >
          <template #modal-content="{ submit, cancel }">
            <kouluttaja-form :kouluttajat="kouluttajat" @submit="submit" @cancel="cancel" />
          </template>
          <template #default="{ uid }">
            <elsa-form-multiselect
              :id="uid"
              v-model="form.lahikouluttaja"
              :options="lahikouluttajatList"
              :state="validateState('lahikouluttaja')"
              label="nimi"
              track-by="nimi"
              @select="onLahikouluttajaSelect"
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

        <elsa-form-group
          :label="$t('lahiesimies-tai-muu')"
          :add-new-enabled="true"
          :add-new-label="$t('lisaa-henkilo')"
          :required="true"
          @submit="onLahiesimiesSubmit"
        >
          <template #modal-content="{ submit, cancel }">
            <kouluttaja-form :kouluttajat="kouluttajat" @submit="submit" @cancel="cancel" />
          </template>
          <template #default="{ uid }">
            <elsa-form-multiselect
              :id="uid"
              v-model="form.lahiesimies"
              :options="lahiesimiesList"
              :state="validateState('lahiesimies')"
              label="nimi"
              track-by="nimi"
              @select="onLahiesimiesSelect"
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
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-col>
    <b-col v-else lg="10">
      <h3>{{ $t('koulutuspaikan-arvioijat') }}</h3>
      <h5>{{ $t('lahikouluttaja') }}</h5>
      <p>
        {{
          lahikouluttaja.nimike
            ? lahikouluttaja.nimi + ', ' + lahikouluttaja.nimike
            : lahikouluttaja.nimi
        }}
      </p>
      <h5>{{ $t('lahiesimies-tai-muu') }}</h5>
      <p>
        {{ lahiesimies.nimike ? lahiesimies.nimi + ', ' + lahiesimies.nimike : lahiesimies.nimi }}
      </p>
    </b-col>
  </b-row>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { BModal } from 'bootstrap-vue'
  import { Component, Prop, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import { postLahikouluttaja } from '@/api/erikoistuva'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import KouluttajaForm from '@/forms/kouluttaja-form.vue'
  import store from '@/store'
  import { Kayttaja, KoejaksonVaiheHyvaksyja, ElsaError, Kouluttaja } from '@/types'
  import { formatList } from '@/utils/kouluttajaAndVastuuhenkiloListFormatter'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaFormMultiselect,
      KouluttajaForm
    }
  })
  export default class KoulutuspaikanArvioijat extends Mixins(validationMixin) {
    validations() {
      return {
        form: {
          lahikouluttaja: {
            nimi: {
              required
            }
          },
          lahiesimies: {
            nimi: {
              required
            }
          }
        }
      }
    }

    @Prop({ required: true })
    lahikouluttaja!: KoejaksonVaiheHyvaksyja

    @Prop({ required: true })
    lahiesimies!: KoejaksonVaiheHyvaksyja

    @Prop({ required: false, default: false })
    isReadonly!: boolean

    @Prop({ required: false, default: false })
    allowDuplicates!: boolean

    @Prop({ required: false, default: () => [] })
    kouluttajat!: Kouluttaja[]

    form = {
      lahikouluttaja: null,
      lahiesimies: null
    } as any

    loading = true

    get lahikouluttajatList() {
      const lahikouluttajat = this.kouluttajat?.map((k: any) => {
        if (this.lahiesimies?.id === k.id && !this.allowDuplicates) {
          return {
            ...k,
            $isDisabled: true
          }
        }
        return k
      })
      return formatList(this, lahikouluttajat)
    }

    get lahiesimiesList() {
      const lahiesimiehet = this.kouluttajat?.map((k: any) => {
        if (this.lahikouluttaja?.id === k.id && !this.allowDuplicates) {
          return {
            ...k,
            $isDisabled: true
          }
        }
        return k
      })
      return formatList(this, lahiesimiehet)
    }

    onLahikouluttajaSelect(lahikouluttaja: KoejaksonVaiheHyvaksyja) {
      this.form.lahikouluttaja = lahikouluttaja
      this.$emit('lahikouluttajaSelect', this.form.lahikouluttaja)
    }

    onLahiesimiesSelect(lahiesimies: KoejaksonVaiheHyvaksyja) {
      this.form.lahiesimies = lahiesimies
      this.$emit('lahiesimiesSelect', this.form.lahiesimies)
    }

    async onLahikouluttajaSubmit(value: Kayttaja, params: { saving: boolean }, modal: BModal) {
      params.saving = true
      await this.onKouluttajaSubmit(value, modal, false)
      params.saving = false
    }

    async onLahiesimiesSubmit(value: Kayttaja, params: { saving: boolean }, modal: BModal) {
      params.saving = true
      await this.onKouluttajaSubmit(value, modal, true)
      params.saving = false
    }

    private async onKouluttajaSubmit(value: Kayttaja, modal: BModal, isLahiesimies: boolean) {
      try {
        const kouluttaja = (await postLahikouluttaja(value)).data
        const koejaksonVaiheHyvaksyja = {
          id: kouluttaja.id,
          kayttajaUserId: kouluttaja.userId,
          nimi: kouluttaja.nimi
        } as KoejaksonVaiheHyvaksyja
        modal.hide('confirm')
        toastSuccess(this, this.$t('uusi-kouluttaja-lisatty'))
        await store.dispatch('erikoistuva/getKouluttajatJaVastuuhenkilot')
        if (isLahiesimies) {
          this.onLahiesimiesSelect(koejaksonVaiheHyvaksyja)
        } else {
          this.onLahikouluttajaSelect(koejaksonVaiheHyvaksyja)
        }
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
      return !this.$v.$anyError
    }

    async mounted() {
      this.form.lahikouluttaja = this.lahikouluttaja?.id ? this.lahikouluttaja : null
      this.form.lahiesimies = this.lahiesimies?.id ? this.lahiesimies : null
      this.loading = false
    }
  }
</script>
