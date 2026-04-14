<template>
  <div>
    <b-form @submit.stop.prevent="onSubmit">
      <elsa-form-group :label="$t('sisalto')" class="col-12 pr-md-3 pl-0 kuvaus mb-1 mt-4">
        <template #default="{ uid }">
          <b-input-group :id="uid" class="mb-2">
            <b-col class="pl-0 pr-0">
              <elsa-text-editor v-model="form.teksti" />
            </b-col>
          </b-input-group>
          <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateState('teksti')">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
      <hr />
      <div class="text-right">
        <elsa-button variant="back" :to="{ name: 'ilmoitukset' }">
          {{ $t('peruuta') }}
        </elsa-button>
        <elsa-button v-if="form.id" variant="outline-danger" @click="onDelete()">
          {{ $t('poista-ilmoitus') }}
        </elsa-button>
        <elsa-button type="submit" :loading="params.saving" variant="primary" class="ml-2">
          {{ $t('tallenna-ilmoitus') }}
        </elsa-button>
      </div>
    </b-form>
  </div>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaTextEditor from '@/components/text-editor/text-editor.vue'
  import { Ilmoitus } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaTextEditor
    },
    validations: {
      form: {
        teksti: {
          required
        }
      }
    }
  })
  export default class IlmoitusForm extends Mixins(validationMixin) {
    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: false, type: Object })
    value?: Ilmoitus

    form: Ilmoitus = {
      teksti: null
    }

    params = {
      saving: false
    }

    async mounted() {
      if (this.value) {
        this.form = this.value
      }
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    onSubmit() {
      this.$v.form.$touch()
      if (this.$v.form.$anyError) {
        return
      }
      this.$emit('submit', this.form, this.params)
    }

    onDelete() {
      this.$emit('delete', this.params)
    }
  }
</script>

<style lang="scss" scoped></style>
