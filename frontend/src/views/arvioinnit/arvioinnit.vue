<template>
  <ArvioinnitErikoistuva v-if="$isErikoistuva()" />
  <ArvioinnitVastuuhenkilo v-else-if="$isVastuuhenkilo()" />
  <ArvioinnitKouluttaja v-else />
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ArvioinnitErikoistuva from '@/views/arvioinnit/arvioinnit-erikoistuva.vue'
  import ArvioinnitKouluttaja from '@/views/arvioinnit/arvioinnit-kouluttaja.vue'
  import ArvioinnitVastuuhenkilo from '@/views/arvioinnit/arvioinnit-vastuuhenkilo.vue'

  @Component({
    components: {
      ArvioinnitErikoistuva,
      ArvioinnitKouluttaja,
      ArvioinnitVastuuhenkilo
    }
  })
  export default class Arvioinnit extends Vue {}
</script>
