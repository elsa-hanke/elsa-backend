<template>
  <koejakson-vaiheet-list
    :loading="loading"
    :koejaksot="koejaksot"
    :component-links="componentLinks"
  />
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import KoejaksonVaiheetList from '@/components/koejakson-vaiheet/koejakson-vaiheet-list.vue'
  import store from '@/store'
  import { LomakeTyypit } from '@/utils/constants'

  @Component({
    components: {
      KoejaksonVaiheetList
    }
  })
  export default class KoejaksoViewKouluttaja extends Vue {
    loading = true
    componentLinks = new Map([
      [LomakeTyypit.KOULUTUSSOPIMUS, 'koulutussopimus'],
      [LomakeTyypit.ALOITUSKESKUSTELU, 'aloituskeskustelu-kouluttaja'],
      [LomakeTyypit.VALIARVIOINTI, 'valiarviointi-kouluttaja'],
      [LomakeTyypit.KEHITTAMISTOIMENPITEET, 'kehittamistoimenpiteet-kouluttaja'],
      [LomakeTyypit.LOPPUKESKUSTELU, 'loppukeskustelu-kouluttaja']
    ])

    async mounted() {
      await store.dispatch('kouluttaja/getKoejaksot')
      this.loading = false
    }

    get koejaksot() {
      return store.getters['kouluttaja/koejaksot']
    }
  }
</script>
