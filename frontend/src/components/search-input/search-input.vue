<template>
  <div>
    <div v-if="visible" class="search-input form-group col-xs-12 col-sm-6 col-xl-4 p-0">
      <b-form-input
        v-model="hakutermiMutable"
        :change="$emit('hakutermi', hakutermi)"
        :placeholder="placeholder"
        class="rounded-pill"
        responsive
      ></b-form-input>
      <font-awesome-icon
        :icon="['fas', 'search']"
        class="text-primary position-absolute search-icon"
      />
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Vue, Prop } from 'vue-property-decorator'

  @Component({})
  export default class ElsaSearchInput extends Vue {
    @Prop({ required: true, type: String, default: undefined })
    hakutermi!: string

    @Prop({ required: false, type: Boolean, default: true })
    visible!: boolean

    @Prop({ required: true, type: String })
    placeholder?: string

    get hakutermiMutable() {
      return this.hakutermi
    }

    set hakutermiMutable(val) {
      this.$emit('update:hakutermi', val)
    }
  }
</script>

<style lang="scss" scoped>
  @import '~bootstrap/scss/mixins/breakpoints';
  @import '~@/styles/variables';

  .search-input {
    margin: 0.625rem 0 0.25rem 0 !important;
    max-width: 25rem;

    @include media-breakpoint-down(xs) {
      margin: 0.625rem 0 1rem 0 !important;
    }
  }
</style>
