<template>
  <div class="d-flex flex-column">
    <div v-if="!loading">
      <navbar v-if="isLoggedIn" />
      <navbar-not-logged-in v-else />
      <b-container v-if="isLoggedIn" fluid>
        <mobile-nav v-if="!$screen.lg" />
        <b-row class="position-relative">
          <sidebar-menu />
          <main role="main">
            <b-container class="mt-4 mt-md-6 mb-6 ml-2 ml-sm-6 ml-md-4">
              <page-not-found-content />
            </b-container>
          </main>
        </b-row>
      </b-container>
      <b-container v-else class="mt-4 mt-md-6 mb-6">
        <page-not-found-content />
      </b-container>
    </div>
    <div v-else class="text-center mt-6">
      <b-spinner variant="primary" :label="$t('ladataan')" />
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import MobileNav from '@/components/mobile-nav/mobile-nav.vue'
  import NavbarNotLoggedIn from '@/components/navbar/navbar-not-logged-in.vue'
  import Navbar from '@/components/navbar/navbar.vue'
  import SidebarMenu from '@/components/sidebar-menu/sidebar-menu.vue'
  import store from '@/store'
  import PageNotFoundContent from '@/views/404/page-not-found-content.vue'

  @Component({
    components: {
      ElsaButton,
      Navbar,
      NavbarNotLoggedIn,
      SidebarMenu,
      MobileNav,
      PageNotFoundContent
    }
  })
  export default class PageNotFound extends Vue {
    loading = true

    get isLoggedIn() {
      return store.getters['auth/isLoggedIn']
    }

    changeLocale(lang: string) {
      this.$i18n.locale = lang
    }

    get currentLocale() {
      return this.$i18n.locale
    }

    get locales() {
      return Object.keys(this.$i18n.messages)
    }

    async mounted() {
      await store.dispatch('auth/authorize')
      this.loading = false
    }
  }
</script>

<style lang="scss" scoped>
  @import '~bootstrap/scss/mixins/breakpoints';
  @import '~@/styles/variables';

  @include media-breakpoint-up(lg) {
    main {
      padding-left: $sidebar-width;
    }
  }
</style>
