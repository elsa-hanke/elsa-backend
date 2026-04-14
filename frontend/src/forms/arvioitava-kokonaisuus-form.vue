<template>
  <div>
    <b-row lg>
      <b-col v-if="!loading">
        <b-form v-if="editing" @submit.stop.prevent="onSubmit">
          <elsa-form-group
            :label="$t('kategorian-nimi')"
            :required="true"
            class="col-12 pr-md-3 pl-0"
          >
            <template #default="{ uid }">
              <elsa-form-multiselect
                v-if="kategoriaEditable"
                :id="uid"
                v-model="form.kategoria"
                :options="sortedKategoriat"
                label="nimi"
                track-by="nimi"
                :state="validateState('kategoria')"
              ></elsa-form-multiselect>
              <p v-else>{{ kokonaisuus.kategoria.nimi }}</p>
            </template>
          </elsa-form-group>
          <elsa-form-group
            :label="$t('arvioitavan-kokonaisuuden-nimi')"
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
                  v-model="form.nimi"
                  :state="validateState('nimi')"
                ></b-form-input>
                <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateState('nimi')">
                  {{ $t('pakollinen-tieto') }}
                </b-form-invalid-feedback>
              </b-input-group>
              <b-input-group prepend="SV">
                <b-form-input :id="uid" v-model="form.nimiSv"></b-form-input>
              </b-input-group>
            </template>
          </elsa-form-group>
          <b-form-row>
            <elsa-form-group
              :label="$t('voimassaolon-alkupaiva')"
              class="col-xs-12 col-sm-6 col-md-4 pr-sm-3"
              :required="true"
            >
              <template #default="{ uid }">
                <elsa-form-datepicker
                  :id="uid"
                  ref="voimassaoloAlkaa"
                  v-model="form.voimassaoloAlkaa"
                  :state="validateState('voimassaoloAlkaa')"
                  @input="$emit('skipRouteExitConfirm', false)"
                ></elsa-form-datepicker>
              </template>
            </elsa-form-group>
            <elsa-form-group
              :label="$t('voimassaolon-paattymispaiva')"
              class="col-xs-12 col-sm-6 col-md-4 pl-sm-3"
            >
              <template #default="{ uid }">
                <elsa-form-datepicker
                  :id="uid"
                  ref="voimassaoloPaattyy"
                  v-model="form.voimassaoloLoppuu"
                  :required="false"
                  class="datepicker-range"
                  @input="$emit('skipRouteExitConfirm', false)"
                ></elsa-form-datepicker>
              </template>
            </elsa-form-group>
          </b-form-row>
          <elsa-form-group :label="$t('kuvaus')" class="col-12 pr-md-3 pl-0 kuvaus">
            <template #default="{ uid }">
              <b-input-group :id="uid" class="mb-2">
                <b-input-group-prepend>
                  <b-input-group-text class="input-group-fi">{{ 'FI' }}</b-input-group-text>
                </b-input-group-prepend>
                <b-col class="pl-0 pr-0">
                  <elsa-text-editor v-model="form.kuvaus" />
                </b-col>
              </b-input-group>
              <b-input-group prepend="SV">
                <b-col class="pl-0 pr-0">
                  <elsa-text-editor v-model="form.kuvausSv" />
                </b-col>
              </b-input-group>
            </template>
          </elsa-form-group>
          <hr class="mt-6" />
          <div class="text-right">
            <elsa-button
              variant="back"
              :to="{
                name: kokonaisuus.id == null ? 'erikoisala' : 'arvioitava-kokonaisuus',
                hash: kokonaisuus.id == null ? '#arvioitavat-kokonaisuudet' : ''
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
          <p>{{ kokonaisuus.kategoria.nimi }}</p>
          <hr />
          <h5>{{ $t('arvioitavan-kokonaisuuden-nimi') }}</h5>
          <p>{{ kokonaisuus.nimi }}</p>
          <h5>{{ $t('voimassaolo') }}</h5>
          <p>
            {{ $date(kokonaisuus.voimassaoloAlkaa) }} -
            {{ kokonaisuus.voimassaoloLoppuu != null ? $date(kokonaisuus.voimassaoloLoppuu) : '' }}
          </p>
          <h5>{{ $t('kuvaus') }}</h5>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <p v-html="kokonaisuus.kuvaus"></p>
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
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaTextEditor from '@/components/text-editor/text-editor.vue'
  import { ArvioitavaKokonaisuus, ArvioitavanKokonaisuudenKategoria } from '@/types'
  import { sortByAsc } from '@/utils/sort'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormDatepicker,
      ElsaFormMultiselect,
      ElsaTextEditor
    }
  })
  export default class ArvioitavaKokonaisuusForm extends Mixins(validationMixin) {
    $refs!: {
      voimassaoloAlkaa: ElsaFormDatepicker
      voimassaoloPaattyy: ElsaFormDatepicker
    }

    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: false, default: true })
    kategoriaEditable!: boolean

    @Prop({ required: true, type: Object })
    kokonaisuus!: ArvioitavaKokonaisuus

    @Prop({ required: false, type: Array, default: () => [] })
    kategoriat!: ArvioitavanKokonaisuudenKategoria[]

    loading = true

    params = {
      saving: false
    }

    form: Partial<ArvioitavaKokonaisuus> = {}

    mounted() {
      this.form = { ...this.kokonaisuus }
      this.loading = false
    }

    get sortedKategoriat() {
      return this.kategoriat.sort((a, b) => sortByAsc(a.nimi, b.nimi))
    }

    validations() {
      return {
        form: {
          kategoria: {
            required
          },
          nimi: {
            required
          },
          voimassaoloAlkaa: {
            required
          }
        }
      }
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      const validations = [this.validateForm(), this.$refs.voimassaoloAlkaa.validateForm()]

      if (validations.includes(false)) {
        return
      }

      this.$emit(
        'submit',
        {
          ...this.form
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

  .kuvaus::v-deep {
    .tox-tinymce {
      border-top-left-radius: 0;
      border-bottom-left-radius: 0;
    }
  }
</style>
