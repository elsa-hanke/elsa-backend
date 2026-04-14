<template>
  <div v-if="active" class="text-danger text-right w-100 my-2">
    <font-awesome-icon icon="info-circle" fixed-width class="text-size-lg mr-1" />
    {{ $t('lomake-sisaltaa-virheita') }}
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  @Component({})
  export default class ElsaFormError extends Vue {
    @Prop({ required: true, type: Boolean })
    active!: boolean
  }
</script>
