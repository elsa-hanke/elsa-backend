<template>
  <div v-if="!loading">
    <component
      :is="routeComponent"
      v-if="isAllowedRoute"
      @skipRouteExitConfirm="onSkipRouteExitConfirm"
    />
    <b-container v-else class="mt-4 mt-md-6 mb-6 ml-2 ml-sm-6 ml-md-4">
      <page-not-found-content />
    </b-container>
  </div>
  <div v-else class="text-center mt-6">
    <b-spinner variant="primary" :label="$t('ladataan')" />
  </div>
</template>

<script lang="ts">
  import { Component, Mixins, Prop, Watch } from 'vue-property-decorator'

  import ConfirmRouteExit from '@/mixins/confirm-route-exit'
  import store from '@/store'
  import PageNotFoundContent from '@/views/404/page-not-found-content.vue'

  Component.registerHooks(['beforeRouteLeave'])

  @Component({
    components: {
      PageNotFoundContent
    }
  })
  export default class ErikoistuvaRoute extends Mixins(ConfirmRouteExit) {
    @Prop({ required: true })
    routeComponent!: any

    @Prop({ required: true, default: [] })
    allowedRoles!: string[]

    @Prop({ required: false, type: Boolean, default: true })
    confirmRouteExit!: boolean

    loading = true

    get isAllowedRoute() {
      const activeAuthority = store.getters['auth/account'].activeAuthority
      return this.allowedRoles.includes(activeAuthority)
    }

    onSkipRouteExitConfirm(val: boolean) {
      this.skipRouteExitConfirm = val
    }

    @Watch('$route', { immediate: true, deep: true })
    async onUrlChange() {
      this.skipRouteExitConfirm = this.confirmRouteExit
      if (!store.getters['auth/isLoggedIn']) {
        await store.dispatch('auth/authorize')
      }
      this.loading = false
    }

    // ELSAINSI-73: the account (and with it activeAuthority / which opinto-oikeus
    // is currently "kaytossa") can change on the server without this tab knowing -
    // e.g. the user switched active profile in another tab, or a nightly
    // opintotieto import reconciled the active opinto-oikeus. Without this, a tab
    // left open across such a change keeps trusting its stale cached authority,
    // renders a route it's no longer allowed on, and only fails later when the
    // underlying API call hits the server's live (and now different) state.
    // Re-checking when the tab regains visibility closes that window: the guard
    // re-evaluates against fresh data before the route component (and its data
    // fetch) ever mounts.
    boundOnVisibilityChange: (() => void) | null = null

    mounted() {
      this.boundOnVisibilityChange = () => {
        this.onVisibilityChange()
      }
      document.addEventListener('visibilitychange', this.boundOnVisibilityChange)
    }

    beforeDestroy() {
      if (this.boundOnVisibilityChange) {
        document.removeEventListener('visibilitychange', this.boundOnVisibilityChange)
      }
    }

    async onVisibilityChange() {
      if (document.visibilityState !== 'visible') {
        return
      }
      if (!store.getters['auth/isLoggedIn']) {
        return
      }
      // Refresh silently (no loading spinner) - isAllowedRoute is a computed
      // getter over store state, so once the account mutates the template
      // re-evaluates on its own and swaps to page-not-found-content if the
      // route is no longer allowed.
      await store.dispatch('auth/authorize')
    }
  }
</script>
