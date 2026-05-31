<template>
  <span>
    <elsa-button
      :id="uid"
      variant="link"
      class="shadow-none p-0 border-0 btn-normal-line-height"
      :aria-label="buttonAriaLabel"
      :aria-expanded="String(popoverShow)"
      :aria-controls="`${uid}-popover`"
    >
      <font-awesome-icon class="text-primary" icon="info-circle" fixed-width :aria-hidden="true" />
    </elsa-button>
    <b-popover
      :id="`${uid}-popover`"
      :target="uid"
      triggers="click blur"
      placement="auto"
      :show.sync="popoverShow"
    >
      <template v-for="(_, name) in $slots" #[name]>
        <slot :name="name" />
      </template>
      <template #title>
        <div v-if="title">
          <b-button
            variant="outline-primary"
            class="close popover-close"
            :aria-label="$t('sulje')"
            @click="onClose"
          >
            <font-awesome-icon
              icon="times"
              class="times"
              transform="shrink-4"
              :aria-hidden="true"
            />
          </b-button>
          <h3 class="mb-0">{{ title }}</h3>
        </div>
      </template>
    </b-popover>
  </span>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class ElsaPopover extends Vue {
    @Prop({ required: false, type: String })
    title?: string

    @Prop({ required: false, type: String })
    ariaLabel?: string

    popoverShow = false

    get uid() {
      return `elsa-popover-${(this as any)._uid}`
    }

    get buttonAriaLabel() {
      return this.ariaLabel || this.title || (this.$t('lisatietoja') as string)
    }

    onClose() {
      this.popoverShow = false
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  .btn-normal-line-height {
    line-height: normal !important;
  }

  ::v-deep {
    .popover-body {
      max-height: 75vh;
      overflow-y: scroll;
    }
  }

  .popover-close {
    color: $gray-600;
  }
</style>
