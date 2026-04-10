<template>
  <b-navbar
    v-if="account.impersonated"
    id="navbar-top"
    toggleable="lg"
    type="dark"
    variant="secondary"
    class="px-0 py-lg-0 navbar-impersonated"
  >
    <b-col cols="12" class="p-sm-2 p-lg-3 bg-secondary">
      <div class="text-white">
        {{ $t('katselet-erikoistujaa') }}: {{ account.firstName }} {{ account.lastName }},
        {{ account.erikoistuvaLaakari.erikoisalaNimi }},
        {{ $t(`yliopisto-nimi.${account.erikoistuvaLaakari.yliopisto}`) }}
        <elsa-button
          size="sm"
          variant="primary"
          class="rounded-pill ml-md-3 mt-2 mt-md-0 d-block d-md-inline-block"
          @click="vaihdaRooli()"
        >
          {{ $t('palaa-omaan-profiiliisi') }}
        </elsa-button>
      </div>
    </b-col>
  </b-navbar>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Avatar from 'vue-avatar'
  import Component from 'vue-class-component'

  import { ELSA_API_LOCATION } from '@/api'
  import ElsaButton from '@/components/button/button.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import store from '@/store'

  @Component({
    components: {
      Avatar,
      ElsaButton,
      UserAvatar
    }
  })
  export default class NavbarImpersonated extends Vue {
    get account() {
      return store.getters['auth/account']
    }

    vaihdaRooli() {
      window.location.href = `${ELSA_API_LOCATION}/api/logout/impersonate`
    }
  }
</script>
<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .navbar-impersonated {
    position: sticky;
    top: 64px;
    z-index: 1000;
    @include media-breakpoint-down(md) {
      top: 54px;
    }
  }
</style>
