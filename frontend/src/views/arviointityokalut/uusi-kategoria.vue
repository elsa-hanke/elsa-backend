<template>
  <div class="uusi-kategoria">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-form @submit.stop.prevent="onSubmit">
        <b-row lg>
          <b-col>
            <h1>{{ $t('lisaa-uusi-kategoria') }}</h1>
            <hr />
            <b-form-row>
              <elsa-form-group
                :label="$t('nimi')"
                class="col-sm-12 col-md-12 pr-md-3"
                :required="true"
              >
                <template #default="{ uid }">
                  <b-form-input
                    :id="uid"
                    v-model="form.nimi"
                    :state="validateState('nimi')"
                    @input="$emit('skipRouteExitConfirm', false)"
                  ></b-form-input>
                  <b-form-invalid-feedback :id="`${uid}-feedback`">
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                </template>
              </elsa-form-group>
            </b-form-row>
          </b-col>
        </b-row>
        <div class="d-flex flex-row-reverse flex-wrap">
          <elsa-button :loading="saving" type="submit" variant="primary" class="ml-2 mb-2">
            {{ $t('tallenna') }}
          </elsa-button>
          <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
            {{ $t('peruuta') }}
          </elsa-button>
        </div>
        <div class="row">
          <elsa-form-error :active="$v.$anyError" />
        </div>
      </b-form>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import { postArviointityokalutKategoria } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ErikoistuvaLaakariForm from '@/forms/uusi-erikoistuva-laakari-form.vue'
  import PaakayttajaForm from '@/forms/uusi-paakayttaja-form.vue'
  import VastuuhenkiloForm from '@/forms/uusi-vastuuhenkilo-form.vue'
  import VirkailijaForm from '@/forms/uusi-virkailija-form.vue'
  import { ArviointityokaluKategoria, ElsaError } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaFormError,
      ElsaFormGroup,
      ErikoistuvaLaakariForm,
      VastuuhenkiloForm,
      VirkailijaForm,
      PaakayttajaForm,
      ElsaButton
    },
    validations: {
      form: {
        nimi: {
          required
        }
      }
    }
  })
  export default class UusiKategoria extends Mixins(validationMixin) {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arviointityokalut'),
        to: { name: 'arviointityokalut' }
      },
      {
        text: this.$t('lisaa-uusi-kategoria'),
        active: true
      }
    ]

    form: ArviointityokaluKategoria = {
      nimi: null
    }

    saving = false

    async onSubmit() {
      const validations = [this.validateForm()]
      if (validations.includes(false)) {
        return
      }

      try {
        await postArviointityokalutKategoria({
          ...this.form
        })
        toastSuccess(this, this.$t('arviointityokalu-kategoria-lisatty'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'arviointityokalut'
        })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('arviointityokalu-kategoria-lisaaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('arviointityokalu-kategoria-lisaaminen-epaonnistui')
        )
      }
      this.saving = false
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form?.[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    mounted() {
      if (this.$isTekninenPaakayttaja()) {
        /* */
      }
    }

    onCancel() {
      this.$router.push({
        name: 'arviointityokalut'
      })
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .uusi-kategoria {
    max-width: 1024px;
  }
</style>
