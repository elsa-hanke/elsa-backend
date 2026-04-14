<template>
  <b-modal :id="id" :title="title" centered>
    <div class="d-block">
      <b-form>
        <elsa-form-group :label="$t('syy-palautukseen')" :required="true">
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="korjausehdotus"
              :state="validateState('korjausehdotus')"
              rows="4"
              class="textarea-min-height"
            ></b-form-textarea>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-form>
    </div>
    <template #modal-footer>
      <elsa-button variant="back" @click="hideModal">
        {{ $t('peruuta') }}
      </elsa-button>
      <elsa-button variant="primary" @click="onSubmit">
        {{ $t('palauta-muokattavaksi') }}
      </elsa-button>
    </template>
  </b-modal>
</template>

<script lang="ts">
  import _get from 'lodash/get'
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup
    },
    validations: {
      korjausehdotus: {
        required
      }
    }
  })
  export default class ElsaReturnToSenderModal extends Mixins(validationMixin) {
    @Prop({ required: true, type: String })
    id!: string

    @Prop({ required: true, type: String })
    title!: string

    korjausehdotus: string | null = null

    validateState(value: string) {
      const form = this.$v
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    hideModal() {
      return this.$bvModal.hide(this.id)
    }

    // Välitä tapahtuma vanhemmalle
    onSubmit() {
      this.$v.$touch()
      if (this.$v.$anyError) {
        return
      }
      this.hideModal()
      this.$emit('submit', this.korjausehdotus)
    }
  }
</script>
