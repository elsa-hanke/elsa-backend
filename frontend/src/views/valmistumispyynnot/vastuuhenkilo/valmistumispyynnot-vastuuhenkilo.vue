<template>
  <div>
    <p class="mt-1 mb-3">{{ $t('valmistumispyynnot-ingressi-vastuuhenkilo') }}</p>
    <div v-if="$isValmistumisenVastuuhenkilo() && $isYekValmistumisenVastuuhenkilo()">
      <b-tabs v-model="tabIndex" content-class="mt-3" :no-fade="true">
        <b-tab :title="$t('erikoislaakarikoulutus')" href="#erikoislaakarikoulutus">
          <valmistumispyynnot-vastuuhenkilo-erikoistuva-laakari />
        </b-tab>
        <b-tab :title="$t('yleislaaketieteen-erikoiskoulutus')" href="#yek">
          <valmistumispyynnot-vastuuhenkilo-yek />
        </b-tab>
      </b-tabs>
    </div>
    <div v-else-if="$isYekValmistumisenVastuuhenkilo()">
      <valmistumispyynnot-vastuuhenkilo-yek />
    </div>
    <div v-else>
      <valmistumispyynnot-vastuuhenkilo-erikoistuva-laakari />
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ValmistumispyynnotVastuuhenkiloYek from '../../valmistumispyynnot-yek/vastuuhenkilo/valmistumispyynnot-vastuuhenkilo-yek.vue'

  import ValmistumispyynnotVastuuhenkiloErikoistuvaLaakari from './valmistumispyynnot-vastuuhenkilo-erikoistuva-laakari.vue'

  @Component({
    components: {
      ValmistumispyynnotVastuuhenkiloErikoistuvaLaakari,
      ValmistumispyynnotVastuuhenkiloYek
    }
  })
  export default class ValmistumispyynnotVastuuhenkilo extends Vue {
    tabIndex = 0
    tabs = ['#erikoislaakarikoulutus', '#yek']

    beforeMount() {
      this.tabIndex = this.tabs.findIndex((tab) => tab === this.$route.hash)
    }
  }
</script>
