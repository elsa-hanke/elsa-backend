<template>
  <div>
    <b-row lg>
      <b-col>
        <b-form v-if="editing" @submit.stop.prevent="onSubmit">
          <elsa-form-group
            :label="$t('kategorian-nimi')"
            :required="true"
            class="col-12 pr-md-3 pl-0"
          >
            <template #default="{ uid }">
              <b-input-group class="mb-2">
                <b-input-group-prepend>
                  <b-input-group-text class="input-group-fi">{{ 'FI' }}</b-input-group-text>
                </b-input-group-prepend>
                <b-form-input
                  :id="uid"
                  v-model="kategoria.nimi"
                  :state="validateState('nimi')"
                ></b-form-input>
                <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateState('nimi')">
                  {{ $t('pakollinen-tieto') }}
                </b-form-invalid-feedback>
              </b-input-group>
              <b-input-group prepend="SV">
                <b-form-input :id="uid" v-model="kategoria.nimiSv"></b-form-input>
              </b-input-group>
            </template>
          </elsa-form-group>
          <elsa-form-group :label="$t('jarjestysnumero')" class="col-12 pr-md-3 pl-0">
            <template #default="{ uid }">
              <b-form-input :id="uid" v-model="kategoria.jarjestysnumero"></b-form-input>
            </template>
          </elsa-form-group>
          <hr class="mt-6" />
          <div class="text-right">
            <elsa-button
              variant="back"
              :to="{
                name: kategoria.id == null ? 'erikoisala' : 'arvioitavan-kokonaisuuden-kategoria',
                hash: kategoria.id == null ? '#arvioitavat-kokonaisuudet' : ''
              }"
            >
              {{ $t('peruuta') }}
            </elsa-button>
            <elsa-button type="submit" variant="primary" class="ml-2">
              {{ $t('tallenna') }}
            </elsa-button>
          </div>
          <div class="row">
            <elsa-form-error :active="$v.$anyError" />
          </div>
        </b-form>
        <div v-else>
          <h5>{{ $t('kategorian-nimi') }}</h5>
          <p>{{ kategoria.nimi }}</p>
          <h5>{{ $t('jarjestysnumero') }}</h5>
          <p>{{ kategoria.jarjestysnumero != null ? kategoria.jarjestysnumero : '-' }}</p>
          <hr />
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Mixins, Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import { ArvioitavanKokonaisuudenKategoria } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ElsaFormError,
      ElsaFormGroup
    }
  })
  export default class ArvioitavanKokonaisuudenKategoriaForm extends Mixins(validationMixin) {
    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: true, type: Object })
    kategoria!: ArvioitavanKokonaisuudenKategoria

    params = {
      saving: false
    }

    validations() {
      return {
        kategoria: {
          nimi: {
            required
          }
        }
      }
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.kategoria[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.kategoria.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      if (!this.validateForm()) {
        return
      }
      this.$emit(
        'submit',
        {
          ...this.kategoria
        },
        this.params
      )
    }
  }
</script>

<style lang="scss" scoped>
  .input-group-fi {
    padding-left: 0.95rem;
    padding-right: 0.95rem;
  }
</style>
