<template>
  <b-skeleton-wrapper :loading="loading">
    <template #loading>
      <b-card v-bind="$attrs">
        <template #header>
          <slot name="header">
            <span class="font-weight-500">{{ header }}</span>
          </slot>
        </template>
        <b-skeleton width="85%"></b-skeleton>
        <b-skeleton width="55%"></b-skeleton>
        <b-skeleton width="70%"></b-skeleton>
      </b-card>
    </template>
    <b-card v-bind="$attrs">
      <template #header>
        <slot name="header">
          <router-link v-if="headerRoute" :to="headerRoute" class="text-decoration-none">
            <span class="font-weight-500 text-white">{{ header }}</span>
          </router-link>
          <span v-if="!headerRoute" class="font-weight-500">{{ header }}</span>
        </slot>
      </template>
      <template v-for="(index, name) in $slots" #[name]><slot :name="name" /></template>
    </b-card>
  </b-skeleton-wrapper>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  @Component
  export default class BCardSkeleton extends Vue {
    @Prop({ required: false, default: false })
    loading!: boolean

    @Prop({ required: false, default: '', type: String })
    header!: string

    @Prop({ required: false, default: '', type: String })
    headerRoute!: string
  }
</script>
