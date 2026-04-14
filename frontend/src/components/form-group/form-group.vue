<template>
  <b-form-group :label-for="uid" v-bind="$attrs">
    <template #label>
      <div class="d-flex justify-content-between align-items-center flex-wrap">
        <div :class="{ 'mr-3': addNewEnabled }">
          {{ label }}
          <span class="text-primary">
            {{ required ? '*' : '' }}
            <font-awesome-icon
              v-if="help"
              v-b-popover.hover.top="help"
              icon="info-circle"
              fixed-width
            />
          </span>
          <slot name="label-help" />
        </div>
        <div v-if="$slots['help']" class="form-group-help font-weight-400">
          <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
          <slot name="help" />
        </div>
        <div v-if="$slots['label-right']" class="form-group-help font-weight-400">
          <slot name="label-right" />
        </div>
        <div v-if="addNewEnabled" class="form-group-help font-weight-400">
          <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
          <b-link @click="showModal">{{ addNewLabelText }}</b-link>
          <span>{{ $t('tai-valitse-alta') }}</span>
          <b-modal
            :id="modalRef"
            :ref="modalRef"
            centered
            size="lg"
            :hide-footer="true"
            @hide="onHide"
            @show="onShow"
          >
            <template #modal-title>
              {{ addNewLabelText }}
            </template>
            <slot
              name="modal-content"
              :submit="onSubmit"
              :cancel="onCancel"
              :skipRouteExitConfirm="onSkipRouteExitConfirm"
            />
          </b-modal>
        </div>
      </div>
    </template>
    <slot :uid="uid" />
  </b-form-group>
</template>

<script lang="ts">
  import { BvModalEvent } from 'bootstrap-vue'
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import { confirmExit } from '@/utils/confirm'

  @Component({})
  export default class ElsaFormGroup extends Vue {
    @Prop({ required: true, type: String })
    label!: string

    @Prop({ required: false, type: String })
    addNewLabel?: string

    @Prop({ required: false, default: false })
    addNewEnabled!: boolean

    @Prop({ required: false, type: Boolean, default: false })
    required!: boolean

    @Prop({ required: false, type: String })
    help?: string

    skipConfirm = true

    async onShow() {
      this.skipConfirm = true
    }

    async onHide(event: BvModalEvent) {
      // Tarkista, onko poistuminen jo vahvistettu
      if (!this.skipConfirm) {
        event.preventDefault()
      } else {
        return
      }

      // Pyydä poistumisen vahvistus
      if (await confirmExit(this)) {
        this.skipConfirm = true
        this.$bvModal.hide(this.modalRef)
      }
    }

    // Välitä tapahtuma vanhemmalle
    onSubmit(value: any, params: any) {
      this.skipConfirm = true
      this.$emit('submit', value, params, this.$refs[this.modalRef])
    }

    async onSkipRouteExitConfirm(value: boolean) {
      this.skipConfirm = value
    }

    async onCancel() {
      if (this.skipConfirm) {
        this.$bvModal.hide(this.modalRef)
      } else {
        if (await confirmExit(this)) {
          this.skipConfirm = true
          this.$bvModal.hide(this.modalRef)
        }
      }
    }

    get addNewLabelText() {
      return this.addNewLabel ? this.addNewLabel : this.$t('lisaa-uusi')
    }

    get modalRef() {
      return `elsa-form-group-modal-${(this as any)._uid}`
    }

    get uid() {
      return `elsa-form-group-${(this as any)._uid}`
    }

    showModal() {
      this.$bvModal.show(this.modalRef)
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .form-group-help {
    font-size: $font-size-sm;
  }
</style>
