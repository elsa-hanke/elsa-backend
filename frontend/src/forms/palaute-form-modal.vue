<template>
  <b-modal
    id="palauteFormModal"
    centered
    :title="$t('palaute-otsikko')"
    size="lg"
    :hide-footer="true"
    @hide="hideAndReset"
  >
    <div v-if="showFormSent">
      <div class="text-center mb-6">
        <font-awesome-icon :icon="['fas', 'check-circle']" class="mt-6 mb-3 text-success icon-xl" />
        <p class="font-weight-500 text-center">{{ $t('palautteen-lahettaminen-onnistui') }}</p>
      </div>
      <div class="text-right mr-2">
        <elsa-button variant="primary" class="px-6 mb-2" :loading="saving" @click="hideAndReset">
          {{ $t('sulje') }}
        </elsa-button>
      </div>
    </div>
    <div v-else-if="showError">
      <div class="text-center mb-6">
        <font-awesome-icon
          :icon="['fas', 'exclamation-circle']"
          class="mt-6 mb-3 text-danger icon-xl"
        />
        <p class="font-weight-500 text-center">
          {{ $t('palautteen-lahettaminen-epaonnistui') }}
        </p>
      </div>
      <div class="text-right mr-2">
        <elsa-button variant="primary" class="px-6 mb-2" :loading="saving" @click="hideAndReset">
          {{ $t('sulje') }}
        </elsa-button>
      </div>
    </div>
    <div v-else>
      <p>{{ $t('palaute-ingressi') }}</p>
      <p class="mb-2 mt-0">{{ $t('palaute-valiaikainen-ohje') }}</p>
      <p class="mb-0 mt-0">
        <b class="mr-1">Helsinki:</b>
        <a href="mailto:meilahti-specialist@helsinki.fi">meilahti-specialist@helsinki.fi</a>
      </p>
      <p class="mb-0 mt-0">
        <b class="mr-1">Itä-Suomi:</b>
        <a href="mailto:erikoislaakarikoulutus@uef.fi">erikoislaakarikoulutus@uef.fi</a>
      </p>
      <p class="mb-0 mt-0">
        <b class="mr-1">Oulu:</b>
        <a href="mailto:study.medicine@oulu.fi">study.medicine@oulu.fi</a>
      </p>
      <p class="mb-0 mt-0">
        <b class="mr-1">Tampere:</b>
        <a href="mailto:met.ammatillinen.jatkokoulutus.tau@tuni.fi">
          met.ammatillinen.jatkokoulutus.tau@tuni.fi
        </a>
      </p>
      <p class="mb-4 mt-0">
        <b class="mr-1">Turku:</b>
        <a href="mailto:laak-ammatillinen@utu.fi">laak-ammatillinen@utu.fi</a>
      </p>
      <p>{{ $t('palaute-ohje') }}</p>
      <elsa-form-group :label="$t('palautteen-aihe')" :required="true">
        <template #default="{ uid }">
          <b-form-radio
            v-model="form.palautteenAihe"
            name="palautteen-aihe"
            :state="validateState('palautteenAihe')"
            :value="$t('palautteen-aihe-kehitysidea')"
          >
            {{ $t('palautteen-aihe-kehitysidea') }}
          </b-form-radio>
          <b-form-radio
            v-model="form.palautteenAihe"
            name="palautteen-aihe"
            :state="validateState('palautteenAihe')"
            :value="$t('palautteen-aihe-virhe')"
          >
            {{ $t('palautteen-aihe-virhe') }}
          </b-form-radio>
          <b-form-radio
            v-model="form.palautteenAihe"
            name="palautteen-aihe"
            :state="validateState('palautteenAihe')"
            :value="$t('palautteen-aihe-muu')"
          >
            {{ $t('palautteen-aihe-muu') }}
          </b-form-radio>
          <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateState('palautteenAihe')">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
      <elsa-form-group :label="$t('palaute-yliopisto')">
        <b-form-select
          v-model="form.palauteYliopisto"
          :options="[
            { text: 'Valitse yliopisto', value: 'Tyhjä' },
            { text: $t('palaute-yliopisto-helsinki'), value: 'Helsinki' },
            { text: $t('palaute-yliopisto-ita-suomi'), value: 'Itä-Suomi' },
            { text: $t('palaute-yliopisto-oulu'), value: 'Oulu' },
            { text: $t('palaute-yliopisto-tampere'), value: 'Tampere' },
            { text: $t('palaute-yliopisto-turku'), value: 'Turku' }
          ]"
          :state="validateState('palauteYliopisto')"
          first-option-as-reset
        ></b-form-select>
      </elsa-form-group>
      <elsa-form-group :label="$t('palaute-ja-tuki')" :required="true">
        <template #default="{ uid }">
          <b-form-textarea
            :id="uid"
            v-model="form.palaute"
            :state="validateState('palaute')"
            rows="7"
            class="textarea-min-height"
          ></b-form-textarea>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
      <b-form-checkbox v-model="form.anonyymiPalaute">
        {{ $t('laheta-palaute-anonyymisti') }}
      </b-form-checkbox>
      <div class="text-right mt-4">
        <elsa-button variant="back" :disabled="saving" @click="hideAndReset">
          {{ $t('peruuta') }}
        </elsa-button>
        <elsa-button variant="primary" class="ml-4 px-6" :loading="saving" @click="onSubmit">
          {{ $t('palaute-laheta') }}
        </elsa-button>
      </div>
      <div class="row">
        <elsa-form-error :active="$v.$anyError" />
      </div>
    </div>
  </b-modal>
</template>

<script lang="ts">
  import axios from 'axios'
  import Component from 'vue-class-component'
  import { Mixins, Prop, Watch } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import { Palaute } from '@/types'

  @Component({
    components: {
      ElsaFormError,
      ElsaFormGroup,
      ElsaButton
    },
    validations: {
      form: {
        palautteenAihe: {
          required
        },
        palauteYliopisto: {},
        palaute: {
          required
        },
        anonyymiPalaute: {
          required
        }
      }
    }
  })
  export default class PalauteFormModal extends Mixins(validationMixin) {
    @Prop({ required: true, type: Boolean, default: false })
    show!: boolean

    saving = false
    showFormSent = false
    showError = false

    form: Palaute = {
      palautteenAihe: null,
      palauteYliopisto: 'Tyhjä',
      palaute: null,
      anonyymiPalaute: false
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    @Watch('show')
    onPropertyChanged(value: boolean) {
      if (value) {
        this.$bvModal.show('palauteFormModal')
      } else {
        this.$bvModal.hide('palauteFormModal')
      }
    }

    hideAndReset() {
      this.$emit('hide')
      this.$v.form.$reset()
      this.form = {
        palautteenAihe: null,
        palauteYliopisto: 'Tyhjä',
        palaute: null,
        anonyymiPalaute: false
      }
      this.showFormSent = false
      this.showError = false
    }

    async onSubmit() {
      this.$v.form.$touch()
      if (this.$v.form.$anyError) {
        return
      }

      this.saving = true
      try {
        await axios.post('/palaute', this.form)
        this.showFormSent = true
      } catch {
        this.showError = true
      }
      this.saving = false
    }
  }
</script>
