<template>
  <koejakson-vaiheet-list
    :loading="loading"
    :koejaksot="koejaksot"
    :component-links="componentLinks"
    :vastuuhenkilo="true"
  />
</template>

<script lang="ts">
  import { Vue, Component } from 'vue-property-decorator'

  import KoejaksonVaiheetList from '@/components/koejakson-vaiheet/koejakson-vaiheet-list.vue'
  import store from '@/store'
  import { LomakeTyypit } from '@/utils/constants'

  @Component({
    components: {
      KoejaksonVaiheetList
    }
  })
  export default class KoejaksoViewVastuuhenkilo extends Vue {
    loading = true
    componentLinks = new Map([
      [LomakeTyypit.KOULUTUSSOPIMUS, 'koulutussopimus'],
      [LomakeTyypit.ALOITUSKESKUSTELU, 'aloituskeskustelu-kouluttaja'],
      [LomakeTyypit.VALIARVIOINTI, 'valiarviointi-kouluttaja'],
      [LomakeTyypit.KEHITTAMISTOIMENPITEET, 'kehittamistoimenpiteet-kouluttaja'],
      [LomakeTyypit.LOPPUKESKUSTELU, 'loppukeskustelu-kouluttaja'],
      [LomakeTyypit.VASTUUHENKILON_ARVIO, 'vastuuhenkilon-arvio-vastuuhenkilo']
    ])

    async mounted() {
      await store.dispatch('vastuuhenkilo/getKoejaksot')
      this.loading = false
    }

    get koejaksot() {
      return store.getters['vastuuhenkilo/koejaksot']
    }
  }
</script>
