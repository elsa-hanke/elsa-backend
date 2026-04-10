<template>
  <arvioinnit-list :arvioinnit="arvioinnit" />
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

    async mounted() {
      await this.fetch()
    }

    async fetch() {
      try {
        this.arvioinnit = (await axios.get('vastuuhenkilo/suoritusarvioinnit')).data
      } catch {
        this.arvioinnit = []
      }
    }
  }
</script>
