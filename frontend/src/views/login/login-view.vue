<template>
  <div class="d-flex flex-column">
    <navbar-not-logged-in />
    <router-view></router-view>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import NavbarNotLoggedIn from '@/components/navbar/navbar-not-logged-in.vue'

  @Component({
    components: {
      NavbarNotLoggedIn,
      ElsaButton
    }
  })
  export default class LoginView extends Vue {
    changeLocale(lang: string) {
      this.$i18n.locale = lang
    }

    get currentLocale() {
      return this.$i18n.locale
    }

    get locales() {
      return Object.keys(this.$i18n.messages)
    }
  }
</script>

<style lang="scss" scoped>
  .brand-logo {
    font-size: 2rem;
  }
</style>
