<template>
  <b-container class="mt-4 mb-5">
    <b-row>
      <b-col lg="5">
        <h1 class="text-primary">{{ $t('valitse-yliopisto') }}</h1>
        <haka-yliopisto-form class="mb-3" @submit="onSubmit" />
      </b-col>
      <b-col>
        <img src="@/assets/elsa-kirjautuminen.svg" :alt="$t('elsa-palvelu')" class="img-fluid" />
      </b-col>
    </b-row>
  </b-container>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { ELSA_API_LOCATION } from '@/api'
  import HakaYliopistoForm from '@/forms/haka-yliopisto-form.vue'

  @Component({
    components: {
      HakaYliopistoForm
    }
  })
  export default class HakaYliopisto extends Vue {
    loading = false

    async onSubmit(value: string) {
      return (window.location.href = `${ELSA_API_LOCATION}/saml2/authenticate/${value}`)
    }
  }
</script>

<style lang="scss" scoped>
  .text-primary {
    font-size: 1.75rem;
  }
</style>
