<template>
  <b-modal :id="id" :title="title" centered>
    <div v-if="text != null" class="d-block">
      <p class="mb-2">{{ text }}</p>
    </div>

    <template #modal-footer>
      <elsa-button variant="back" @click="onCancel">
        {{ $t('peruuta') }}
      </elsa-button>

      <elsa-button :variant="submitVariant" @click="onSubmit">
        {{ submitText }}
      </elsa-button>
    </template>
    <slot name="modal-content" />
  </b-modal>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'

  @Component({
    components: {
      ElsaButton
    },
    validations: {
      korjausehdotus: {
        required
      }
    }
  })
  export default class ElsaConfirmationModal extends Mixins(validationMixin) {
    @Prop({ required: true, type: String })
    id!: string

    @Prop({ required: true, type: String })
    title!: string

    @Prop({ required: false, type: String })
    text?: string

    @Prop({ required: true, type: String })
    submitText!: string

    @Prop({ required: false, type: String, default: 'primary' })
    submitVariant?: string

    @Prop({ required: false, type: Boolean, default: true })
    hideOnSubmit?: boolean

    hideModal() {
      return this.$bvModal.hide(this.id)
    }

    // Välitä tapahtuma vanhemmalle
    onSubmit(value: any, params: any) {
      if (this.hideOnSubmit) {
        this.hideModal()
      }
      this.$emit('submit', value, params)
    }

    onCancel(value: any, params: any) {
      this.hideModal()
      this.$emit('cancel', value, params)
    }
  }
</script>
