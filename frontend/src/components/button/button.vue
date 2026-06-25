<template>
  <b-button v-bind="$attrs" :disabled="disabled || loading" v-on="$listeners">
    <slot v-if="!loadingEnabled" />
    <span v-else>
      <slot />
      <!-- Toinen vaihtoehto -->
      <!--<span class="ml-4" :class="{ 'mr-4': !loading }"><slot /></span>-->
      <b-spinner v-if="loading" small class="ml-2"></b-spinner>
    </span>
  </b-button>
</template>

<script lang="ts">
  import { Vue, Component, Prop } from 'vue-property-decorator'

  @Component({})
  export default class ElsaButton extends Vue {
    @Prop({ required: false })
    loading?: boolean

    @Prop({ required: false })
    disabled?: boolean

    get loadingEnabled() {
      return this.loading !== undefined
    }
  }
</script>
