<template>
  <arvioinnit-list :arvioinnit="arvioinnit" :loading="loading" />
</template>

<script lang="ts">
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import ArvioinnitList from '@/components/arvioinnit-list/arvioinnit-list.vue'
  import ElsaButton from '@/components/button/button.vue'
  import { Suoritusarviointi } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ArvioinnitList
    }
  })
  export default class Arvioinnit extends Vue {
    arvioinnit: null | Suoritusarviointi[] = null
    loading = true

    async mounted() {
      await this.fetch()
      this.loading = false
    }

    async fetch() {
      try {
        this.arvioinnit = (await axios.get('kouluttaja/suoritusarvioinnit')).data
      } catch {
        this.arvioinnit = []
      }
    }
  }
</script>
