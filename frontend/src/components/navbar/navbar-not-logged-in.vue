<template>
  <b-navbar id="navbar-top" type="dark" variant="primary" sticky class="px-0 py-0">
    <b-container>
      <router-link to="/etusivu">
        <b-navbar-brand class="col text-nowrap user-select-none">
          <div class="brand-logo d-inline-block font-weight-bold text-uppercase">
            {{ $t('elsa') }}
          </div>
          <div class="brand-text d-inline-block text-size-sm align-top pt-2">
            -
            <span class="text-lowercase">{{ $t('palvelu') }}</span>
          </div>
        </b-navbar-brand>
      </router-link>
      <!-- Piilotetaan pilotista -->
      <!-- <b-navbar-nav class="ml-auto pr-3 font-weight-500">
        <b-nav-item-dropdown :text="$t(currentLocale)" class="align-self-center px-3" right>
          <b-dropdown-item
            v-for="locale in locales"
            :key="locale"
            :disabled="currentLocale === locale"
            @click="changeLocale(locale)"
          >
            {{ $t(locale) }}
          </b-dropdown-item>
        </b-nav-item-dropdown>
      </b-navbar-nav> -->
    </b-container>
  </b-navbar>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  @Component({
    components: {}
  })
  export default class NavbarNotLoggedIn extends Vue {
    get currentLocale() {
      return this.$i18n.locale
    }

    get locales() {
      return Object.keys(this.$i18n.messages)
    }

    changeLocale(lang: string) {
      this.$i18n.locale = lang
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .brand-logo {
    font-size: 2.1875rem;
    line-height: 2.375rem;
    @include media-breakpoint-down(sm) {
      font-size: 1.875rem;
    }
  }

  .brand-text {
    font-size: 0.9375rem;
    position: relative;
    top: -10px;
    @include media-breakpoint-down(sm) {
      font-size: 0.8125rem;
      top: -7px;
    }
  }
</style>
