<template>
  <div class="d-flex flex-column">
    <navbar-not-logged-in />
    <b-container class="mt-6 mb-6">
      <b-row>
        <b-col lg>
          <h1 class="text-primary mb-lg-3">{{ $t('uloskirjaus') }}</h1>
          <p style="font-size: 1.25rem">{{ $t('uloskirjaus-kuvaus') }}</p>
        </b-col>
        <b-col lg="5">
          <img src="@/assets/elsa-kirjautuminen.svg" :alt="$t('elsa-palvelu')" class="img-fluid" />
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import NavbarNotLoggedIn from '@/components/navbar/navbar-not-logged-in.vue'

  @Component({
    components: {
      NavbarNotLoggedIn
    }
  })
  export default class Logout extends Vue {}
</script>

<style lang="scss" scoped>
  .text-primary {
    font-size: 1.75rem;
  }
</style>
