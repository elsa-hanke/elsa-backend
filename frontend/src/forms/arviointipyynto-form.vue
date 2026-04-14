<template>
  <b-form @submit.stop.prevent="onSubmit">
    <elsa-form-group :label="$t('erikoistuja')">
      <template #default="{ uid }">
        <user-avatar :id="uid" :src-base64="avatar" src-content-type="image/jpeg" />
      </template>
    </elsa-form-group>
    <elsa-form-group
      :label="$t('tyoskentelyjakso')"
      :add-new-enabled="true"
      :add-new-label="$t('lisaa-tyoskentelyjakso')"
      :required="true"
      @submit="onTyoskentelyjaksoSubmit"
    >
      <template #modal-content="{ submit, cancel, skipRouteExitConfirm }">
        <tyoskentelyjakso-form
          :kunnat="kunnat"
          :erikoisalat="erikoisalat"
          @submit="submit"
          @cancel="cancel"
          @skipRouteExitConfirm="skipRouteExitConfirm"
        />
      </template>
      <template #default="{ uid }">
        <elsa-form-multiselect
          :id="uid"
          v-model="form.tyoskentelyjakso"
          :options="tyoskentelyjaksotFormatted"
          :state="validateState('tyoskentelyjakso')"
          label="label"
          track-by="id"
          @input="$emit('skipRouteExitConfirm', false)"
          @select="onTyoskentelyjaksoSelect"
        />
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('arvioitavat-kokonaisuudet')" :required="true">
      <template #default="{ uid }">
        <elsa-form-multiselect
          :id="uid"
          v-model="arvioitavatKokonaisuudet"
          :options="arvioitavanKokonaisuudenKategoriat"
          :multiple="true"
          :state="validateArvioitavatKokonaisuudet()"
          group-values="arvioitavatKokonaisuudet"
          group-label="nimi"
          :group-select="false"
          label="nimi"
          track-by="id"
          @input="$emit('skipRouteExitConfirm', false)"
        >
          <template #option="props">
            <span v-if="props.option.$isLabel">{{ props.option.$groupLabel }}</span>
            <span v-else class="d-inline-block ml-3">{{ props.option.nimi }}</span>
          </template>
        </elsa-form-multiselect>
        <small class="form-text text-muted">
          {{ $t('valitse-arvioitavat-kokonaisuudet-help') }}
        </small>
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <b-form-row>
      <elsa-form-group :label="$t('arvioitava-tapahtuma')" class="col-md-8">
        <template #default="{ uid }">
          <b-form-input
            :id="uid"
            v-model="form.arvioitavaTapahtuma"
            :aria-describedby="`${uid}-feedback`"
            @input="$emit('skipRouteExitConfirm', false)"
          ></b-form-input>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
      <elsa-form-group :label="$t('tapahtuman-ajankohta')" class="col-md-4" :required="true">
        <template #default="{ uid }">
          <elsa-form-datepicker
            v-if="childDataReceived"
            :id="uid"
            ref="tapahtumanAjankohta"
            :value.sync="form.tapahtumanAjankohta"
            :min="tyoskentelyjaksonAlkamispaiva"
            :max="tyoskentelyjaksonPaattymispaiva"
            :min-error-text="
              $t('tapahtuman-ajankohta-ei-voi-olla-ennen-tyoskentelyjakson-alkamista')
            "
            :max-error-text="
              $t('tapahtuman-ajankohta-ei-voi-olla-tyoskentelyjakson-paattymisen-jalkeen')
            "
            @input="$emit('skipRouteExitConfirm', false)"
          ></elsa-form-datepicker>
        </template>
      </elsa-form-group>
    </b-form-row>
    <b-form-row>
      <elsa-form-group
        :label="$t('kouluttaja-tai-vastuuhenkilo')"
        :add-new-enabled="!editing"
        :add-new-label="$t('lisaa-kouluttaja')"
        :required="true"
        class="col-md-12"
        @submit="onKouluttajaSubmit"
      >
        <template #modal-content="{ submit, cancel, skipRouteExitConfirm }">
          <kouluttaja-form
            :kouluttajat="kouluttajatAndVastuuhenkilot"
            @submit="submit"
            @cancel="cancel"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
        </template>
        <template #default="{ uid }">
          <elsa-form-multiselect
            :id="uid"
            v-model="form.arvioinninAntaja"
            :options="formattedKouluttajatAndVastuuhenkilot"
            :state="validateState('arvioinninAntaja')"
            :disabled="editing"
            label="nimi"
            track-by="nimi"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            <template #option="{ option }">
              <div v-if="option.nimi != null">{{ option.nimi }}</div>
            </template>
          </elsa-form-multiselect>
          <small class="form-text text-muted">
            {{ $t('valitse-arvioija-help') }}
            <b-link :to="{ name: 'profiili' }">{{ $t('profiilissasi') }}</b-link>
          </small>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
    </b-form-row>
    <elsa-form-group :label="$t('lisatiedot')">
      <template #default="{ uid }">
        <b-form-textarea
          :id="uid"
          v-model="form.lisatiedot"
          rows="5"
          @input="$emit('skipRouteExitConfirm', false)"
        ></b-form-textarea>
      </template>
    </elsa-form-group>
    <div class="text-right mb-2">
      <elsa-button variant="back" :to="{ name: 'arvioinnit' }">{{ $t('peruuta') }}</elsa-button>
      <elsa-button
        v-if="editing"
        :loading="params.deleting"
        variant="outline-danger"
        class="ml-2"
        @click="onArviointipyyntoDelete"
      >
        {{ $t('poista-arviointipyynto') }}
      </elsa-button>
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2">
        {{ editing ? $t('tallenna') : $t('laheta') }}
      </elsa-button>
    </div>
    <div class="row">
      <elsa-form-error :active="$v.$anyError" />
    </div>
  </b-form>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { BModal } from 'bootstrap-vue'
  import Component from 'vue-class-component'
  import { Prop, Mixins } from 'vue-property-decorator'
  import { required } from 'vuelidate/lib/validators'

  import { postLahikouluttaja } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import KouluttajaForm from '@/forms/kouluttaja-form.vue'
  import TyoskentelyjaksoForm from '@/forms/tyoskentelyjakso-form.vue'
  import TyoskentelyjaksoMixin from '@/mixins/tyoskentelyjakso'
  import store from '@/store'
  import {
    ArvioitavanKokonaisuudenKategoria,
    Erikoisala,
    Kayttaja,
    Kunta,
    Suoritusarviointi,
    ElsaError,
    ArvioitavaKokonaisuus
  } from '@/types'
  import { formatList } from '@/utils/kouluttajaAndVastuuhenkiloListFormatter'
  import { toastSuccess, toastFail } from '@/utils/toast'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      KouluttajaForm,
      TyoskentelyjaksoForm,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      UserAvatar,
      ElsaFormDatepicker,
      ElsaButton
    },
    validations: {
      form: {
        tyoskentelyjakso: {
          required
        },
        arvioinninAntaja: {
          required
        }
      },
      arvioitavatKokonaisuudet: {
        required
      }
    }
  })
  export default class ArviointipyyntoForm extends Mixins(TyoskentelyjaksoMixin) {
    $refs!: {
      tapahtumanAjankohta: ElsaFormDatepicker
    }

    @Prop({ required: false, default: () => [] })
    kunnat!: Kunta[]

    @Prop({ required: false, default: () => [] })
    erikoisalat!: Erikoisala[]

    @Prop({ required: false, default: () => [] })
    arvioitavanKokonaisuudenKategoriat!: ArvioitavanKokonaisuudenKategoria[]

    @Prop({ required: false, default: () => [] })
    kouluttajatAndVastuuhenkilot!: Kayttaja[]

    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({
      required: false,
      type: Object,
      default: () => ({
        tyoskentelyjakso: null,
        arvioitavatKokonaisuudet: [],
        arvioitavaTapahtuma: null,
        kouluttajaOrVastuuhenkilo: null,
        tapahtumanAjankohta: null,
        lisatiedot: null
      })
    })
    value?: Suoritusarviointi

    form: Partial<Suoritusarviointi> = {
      arvioitavatKokonaisuudet: [],
      arvioitavaTapahtuma: null,
      lisatiedot: null,
      arvioinninAntaja: undefined
    }

    arvioitavatKokonaisuudet: (ArvioitavaKokonaisuus | null)[] = []

    params = {
      saving: false,
      deleting: false
    }
    childDataReceived = false

    mounted() {
      this.form.tyoskentelyjakso = this.value?.tyoskentelyjakso
      if (this.form.tyoskentelyjakso) {
        this.form.tyoskentelyjakso.label = tyoskentelyjaksoLabel(this, this.value?.tyoskentelyjakso)
      }
      this.form.arvioitavatKokonaisuudet = this.value?.arvioitavatKokonaisuudet
      if (this.form.arvioitavatKokonaisuudet) {
        this.arvioitavatKokonaisuudet = this.form.arvioitavatKokonaisuudet?.map(
          (k) => k.arvioitavaKokonaisuus
        )
      }
      this.form.arvioitavaTapahtuma = this.value?.arvioitavaTapahtuma
      this.form.arvioinninAntaja = this.value?.arvioinninAntaja
      this.form.lisatiedot = this.value?.lisatiedot
      this.form.tapahtumanAjankohta = this.value?.tapahtumanAjankohta
      this.childDataReceived = true
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateArvioitavatKokonaisuudet() {
      const { $dirty, $error } = this.$v.arvioitavatKokonaisuudet as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      this.$v.arvioitavatKokonaisuudet.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      const validations = [this.validateForm(), this.$refs.tapahtumanAjankohta.validateForm()]

      if (validations.includes(false)) {
        return
      }

      this.$emit(
        'submit',
        {
          tyoskentelyjaksoId: this.form.tyoskentelyjakso?.id,
          arvioitavatKokonaisuudet: this.arvioitavatKokonaisuudet?.map((k) => {
            return { arvioitavaKokonaisuusId: k?.id }
          }),
          arvioitavaTapahtuma: this.form.arvioitavaTapahtuma,
          arvioinninAntajaId: this.form.arvioinninAntaja?.id,
          tapahtumanAjankohta: this.form.tapahtumanAjankohta,
          lisatiedot: this.form.lisatiedot
        } as Partial<Suoritusarviointi>,
        this.params
      )
    }

    onArviointipyyntoDelete() {
      this.$emit('delete', this.params)
    }

    async onKouluttajaSubmit(value: Kayttaja, params: any, modal: BModal) {
      params.saving = true
      try {
        const kouluttaja = (await postLahikouluttaja(value)).data

        if (
          !this.kouluttajatAndVastuuhenkilot.some(
            (kayttaja: Kayttaja) => kayttaja.id === kouluttaja.id
          )
        ) {
          this.kouluttajatAndVastuuhenkilot.push(kouluttaja)
        }
        this.form.arvioinninAntaja = kouluttaja
        modal.hide('confirm')
        toastSuccess(this, this.$t('uusi-kouluttaja-lisatty'))
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

    get account() {
      return store.getters['auth/account']
    }

    get avatar() {
      if (this.account) {
        return this.account.avatar
      }
      return undefined
    }

    get formattedKouluttajatAndVastuuhenkilot() {
      return formatList(this, this.kouluttajatAndVastuuhenkilot)
    }
  }
</script>
