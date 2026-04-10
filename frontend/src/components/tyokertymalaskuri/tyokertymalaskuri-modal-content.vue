<template>
  <div class="lisaa-tyoskentelyjakso">
    <b-container fluid>
      <b-row lg>
        <b-col>
          <tyokertymalaskuri-tyoskentelyjakso-form
            v-if="!loading"
            :tyoskentelyjakso="tyoskentelyjakso"
            @submit="onSubmit"
            @delete="onDelete"
            @cancel="onCancel"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>
<script lang="ts">
  import { Vue, Component, Prop } from 'vue-property-decorator'

  import TyokertymalaskuriTyoskentelyjaksoForm from '@/forms/tyokertymalaskuri-tyoskentelyjakso-form.vue'
  import YekTyoskentelyjaksoForm from '@/forms/yek-tyoskentelyjakso-form.vue'
  import { TyokertymaLaskuriTyoskentelyjakso } from '@/types'

  @Component({
    components: { TyokertymalaskuriTyoskentelyjaksoForm, YekTyoskentelyjaksoForm }
  })
  export default class TyokertymalaskuriModalContent extends Vue {
    @Prop({ required: false })
    tyoskentelyjakso!: TyokertymaLaskuriTyoskentelyjakso

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
