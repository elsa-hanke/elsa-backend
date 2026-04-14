<template>
  <div class="valmistumispyynnot">
    <p class="mt-1 mb-3">{{ $t('valmistumispyynnot-ingressi-vastuuhenkilo') }}</p>
    <div v-if="!initializing">
      <b-tabs v-model="tabIndex" content-class="mt-3" :no-fade="true">
        <b-tab :title="$t('erikoislaakarikoulutus')" href="#erikoislaakarikoulutus">
          <valmistumispyynnot-virkailija-erikoistuva-laakari :erikoisalat="erikoisalat" />
        </b-tab>
        <b-tab :title="$t('yleislaaketieteen-erikoiskoulutus')" href="#yek">
          <valmistumispyynnot-virkailija-yek />
        </b-tab>
      </b-tabs>
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ValmistumispyynnotVirkailijaYek from '../../valmistumispyynnot-yek/virkailija/valmistumispyynnot-virkailija-yek.vue'

  import ValmistumispyynnotVirkailijaErikoistuvaLaakari from './valmistumispyynnot-virkailija-erikoistuva-laakari.vue'

  import { getErikoisalat } from '@/api/virkailija'
  import { Erikoisala } from '@/types'

  @Component({
    components: {
      ValmistumispyynnotVirkailijaErikoistuvaLaakari,
      ValmistumispyynnotVirkailijaYek
    }
  })
  export default class ValmistumispyynnotVirkailija extends Vue {
    initializing = true
    erikoisalat: Erikoisala[] | null = null

    tabIndex = 0
    tabs = ['#erikoislaakarikoulutus', '#yek']

    beforeMount() {
      this.tabIndex = this.tabs.findIndex((tab) => tab === this.$route.hash)
    }

    async mounted() {
      this.erikoisalat = this.$isVirkailija() ? (await getErikoisalat()).data : []
      this.initializing = false
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .valmistumispyynnot {
    max-width: 1024px;
  }
  .task-type {
    text-transform: capitalize;
  }
</style>
