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
  }
</script>
