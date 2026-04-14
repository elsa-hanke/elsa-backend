<template>
  <div>
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('valmistumispyynnot') }}</h1>
        </b-col>
      </b-row>
      <valmistumispyynnot-vastuuhenkilo v-if="$isVastuuhenkilo()" />
      <valmistumispyynnot-virkailija v-if="$isVirkailija()" />
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ValmistumispyynnotVastuuhenkilo from '@/views/valmistumispyynnot/vastuuhenkilo/valmistumispyynnot-vastuuhenkilo.vue'
  import ValmistumispyynnotVirkailija from '@/views/valmistumispyynnot/virkailija/valmistumispyynnot-virkailija.vue'

  @Component({
    components: {
      ValmistumispyynnotVastuuhenkilo,
      ValmistumispyynnotVirkailija
    }
  })
  export default class Valmistumispyynnot extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('valmistumispyynnot'),
        active: true
      }
    ]
  }
</script>
