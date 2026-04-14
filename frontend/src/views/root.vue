<template>
  <div id="root" class="min-height-800" :class="{ 'gray-backdrop': hasGrayBackdrop }">
    <navbar />
    <navbar-impersonated />
    <mobile-nav v-if="!$screen.lg" />
    <sidebar-menu ref="sidebar" />
    <main role="main">
      <router-view />
    </main>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import MobileNav from '@/components/mobile-nav/mobile-nav.vue'
  import NavbarImpersonated from '@/components/navbar/navbar-impersonate.vue'
  import Navbar from '@/components/navbar/navbar.vue'
  import SidebarMenu from '@/components/sidebar-menu/sidebar-menu.vue'

  @Component({
    components: {
      Navbar,
      NavbarImpersonated,
      SidebarMenu,
      MobileNav
    }
  })
  export default class Root extends Vue {
    get hasGrayBackdrop() {
      return this.$route.meta.grayBackdrop
    }
  }
</script>

<style lang="scss" scoped>
  @import '~bootstrap/scss/mixins/breakpoints';
  @import '~@/styles/variables';

  .gray-backdrop {
    background-color: $backdrop-background-color;
  }

  .min-height-800 {
    min-height: 800px;
  }

  @include media-breakpoint-up(lg) {
    main {
      padding-left: $sidebar-width;
    }
  }
</style>
