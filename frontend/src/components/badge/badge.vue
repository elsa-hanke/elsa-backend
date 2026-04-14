<template>
  <span>
    <span class="text-size-lg">
      <b-badge pill :variant="variant">{{ value }}</b-badge>
    </span>
    <span v-if="help">
      {{ help }}
    </span>
  </span>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  @Component({})
  export default class ElsaBadge extends Vue {
    @Prop({ required: true, type: [String, Number] })
    value!: number | string

    @Prop({ required: false, type: String })
    help?: string

    @Prop({ required: false, type: String, default: 'light' })
    variant!: string
  }
</script>
