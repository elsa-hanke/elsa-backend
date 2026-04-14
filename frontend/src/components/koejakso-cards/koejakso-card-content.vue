<template>
  <div class="d-flex-column sopimus-card-container">
    <b-row class="mb-3">
      <b-col class="d-flex">
        <slot name="content"></slot>
      </b-col>
    </b-row>
    <b-row>
      <b-col>
        <slot name="button"></slot>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  @Component
  export default class KoejaksoCardContent extends Vue {}
</script>

<style lang="scss" scoped>
  .sopimus-card-container {
    &:hover {
      cursor: pointer;
    }
  }
</style>
