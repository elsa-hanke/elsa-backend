<template>
  <div id="app" class="d-flex position-relative flex-column justify-content-between min-vh-100">
    <!-- Skip to main content link for keyboard users -->
    <a href="#main-content" class="skip-to-main">{{ $t('hyppaa-paasisaltoon') }}</a>
    <!-- Aria-live region for SPA route change announcements -->
    <div id="route-announcer" aria-live="polite" aria-atomic="true" class="sr-only">
      {{ routeAnnouncement }}
    </div>
    <div id="main-content">
      <router-view class="router-view" />
    </div>
    <b-link v-if="isLoggedIn" class="d-print-none" @click="openPalauteFormModal">
      <div class="feedback-link">
        <font-awesome-icon class="feedback-icon" :icon="['far', 'envelope']" fixed-width />
        <span class="feedback-text">{{ $t('palaute') }}</span>
      </div>
    </b-link>
    <footer class="pb-4 bg-white d-print-none">
      <div class="d-flex justify-content-center align-items-center">
        <b-link class="my-4 mx-3 mx-lg-4" @click="openTietosuojaselosteModal">
          {{ $t('tietosuojaseloste') }}
        </b-link>
        <tietosuojaseloste-modal v-model="showTietosuojaselosteFormModal" />
        <b-link v-if="isLoggedIn" class="my-4 mx-3 mx-lg-4" @click="openPalauteFormModal">
          {{ $t('palaute') }}
        </b-link>
        <palaute-form-modal :show="showPalauteFormModal" @hide="hidePalauteFormModal" />
        <span v-if="appVersion" class="app-version my-4 mx-3 mx-lg-4">{{ appVersion }}</span>
      </div>
      <div class="d-flex flex-wrap justify-content-center">
        <a
          href="https://www.oulu.fi/fi"
          target="_blank"
          rel="noopener noreferrer"
          class="m-2 m-lg-3"
        >
          <img src="@/assets/elsa-oulun-yliopisto.svg" alt="Oulun Yliopisto" />
        </a>
        <a
          href="https://www.tuni.fi/fi"
          target="_blank"
          rel="noopener noreferrer"
          class="m-2 m-lg-3"
        >
          <img src="@/assets/elsa-tampereen-yliopisto.svg" alt="Tampereen Yliopisto" />
        </a>
        <a
          href="https://www.utu.fi/fi"
          target="_blank"
          rel="noopener noreferrer"
          class="m-2 m-lg-3"
        >
          <img src="@/assets/elsa-turun-yliopisto.svg" alt="Turun Yliopisto" />
        </a>
        <a
          href="https://www.uef.fi/fi"
          target="_blank"
          rel="noopener noreferrer"
          class="m-2 m-lg-3"
        >
          <img src="@/assets/elsa-itasuomen-yliopisto.svg" alt="Itä-Suomen Yliopisto" />
        </a>
        <a
          href="https://www.helsinki.fi/fi"
          target="_blank"
          rel="noopener noreferrer"
          class="m-2 m-lg-3"
        >
          <img src="@/assets/elsa-helsingin-yliopisto.svg" alt="Helsingin Yliopisto" />
        </a>
      </div>
    </footer>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Watch } from 'vue-property-decorator'

  import TietosuojaselosteModal from '@/components/tietosuojaseloste/tietosuojaseloste-modal.vue'
  import PalauteFormModal from '@/forms/palaute-form-modal.vue'
  import store from '@/store'
  import { Meta } from '@/utils/decorators'

  @Component({
    components: {
      PalauteFormModal,
      TietosuojaselosteModal
    }
  })
  export default class App extends Vue {
    showTietosuojaselosteFormModal = false
    showPalauteFormModal = false
    routeAnnouncement = ''

    openTietosuojaselosteModal() {
      this.showTietosuojaselosteFormModal = true
    }

    openPalauteFormModal() {
      this.showPalauteFormModal = true
    }

    hidePalauteFormModal() {
      this.showPalauteFormModal = false
    }

    @Watch('$route')
    onRouteChange() {
      // Announce the new page to screen readers after SPA navigation
      this.$nextTick(() => {
        this.routeAnnouncement = document.title
        // Move focus to the main content area
        const mainEl = document.getElementById('main-content')
        if (mainEl) {
          mainEl.setAttribute('tabindex', '-1')
          mainEl.focus()
          mainEl.addEventListener('blur', () => mainEl.removeAttribute('tabindex'), { once: true })
        }
      })
    }

    @Meta
    getMetaInfo() {
      if (this.$route && this.$route.name) {
        return {
          title: this.$t(this.$route.name),
          titleTemplate: `%s - ${this.$t('elsa-palvelu')}`
        }
      }
      return {
        title: this.$t('elsa-palvelu'),
        titleTemplate: null
      }
    }

    get isLoggedIn() {
      return store.getters['auth/isLoggedIn']
    }

    get appVersion() {
      return process.env.VUE_APP_VERSION || ''
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .skip-to-main {
    position: absolute;
    top: -9999px;
    left: 0;
    z-index: 9999;
    background: $primary;
    color: #fff;
    padding: 0.5rem 1rem;
    font-weight: 600;

    &:focus {
      top: 0;
    }
  }

  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  footer {
    border-top: 1px solid $border-color;
    @include media-breakpoint-down(sm) {
      border-top: 5px solid #f5f5f6;
    }
    z-index: 990;
  }

  .feedback-link {
    background-color: $primary;
    color: $white;
    font-weight: 500;
    position: fixed;
    bottom: 0;
    right: 0;
    border-radius: 0.25rem 0 0 0;
    padding: 0.375rem 0.625rem 0.25rem 0.5rem;
    z-index: 1005;
  }

  .feedback-icon {
    font-size: 1.5rem;
    vertical-align: middle;
  }

  .feedback-text {
    margin-left: 0.375rem;
    vertical-align: middle;
  }

  .router-view {
    padding-bottom: 5rem;
  }

  #main-content {
    flex: 1;
  }

  .app-version {
    font-size: 0.75rem;
    color: $gray-600;
  }
</style>
