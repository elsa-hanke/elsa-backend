<template>
  <div class="arviointityokalut">
    <arviointityokalut-arvioija-form
      v-if="!loading"
      :valitut-arviointityokalut="valitutArviointityokalut"
      :arviointityokalu-vastaukset="arviointityokaluVastaukset"
      :can-validate="canValidate"
      @submit="onSubmit"
      @delete="onDelete"
      @cancel="onCancel"
      @skipRouteExitConfirm="skipRouteExitConfirm"
    />
  </div>
</template>
<script lang="ts">
  import { Vue, Component, Prop } from 'vue-property-decorator'

  import ArviointityokalutArvioijaForm from '@/forms/arviointityokalut-arvioija-form.vue'
  import { Arviointityokalu, SuoritusarviointiArviointityokaluVastaus } from '@/types'

  @Component({
    components: { ArviointityokalutArvioijaForm }
  })
  export default class TyokertymalaskuriModalContent extends Vue {
    @Prop({ required: true, type: Array, default: () => [] })
    valitutArviointityokalut!: Arviointityokalu[]

    @Prop({ required: true, type: Array, default: () => [] })
    arviointityokaluVastaukset!: SuoritusarviointiArviointityokaluVastaus[]

    @Prop({ type: Boolean, default: false })
    canValidate!: boolean

    loading = false

    async mounted() {
      this.loading = false
    }

    onSubmit(formData: any, params: any) {
      this.$emit('submit', formData, params)
    }

    onDelete(id: number) {
      this.$emit('delete', id)
    }

    onCancel() {
      this.$emit('closeModal')
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>
