<template>
  <div class="d-flex flex-column">
    <navbar-yleinen-laskuri />
    <router-view></router-view>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import NavbarYleinenLaskuri from '@/components/navbar/navbar-yleinen-laskuri.vue'

  @Component({
    components: {
      NavbarYleinenLaskuri,
      ElsaButton
    }
  })
  export default class TyokertymalaskuriView extends Vue {
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
