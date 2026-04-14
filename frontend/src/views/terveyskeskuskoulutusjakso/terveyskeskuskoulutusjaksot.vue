<template>
  <div class="terveyskeskuskoulutusjaksot">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <h1>{{ $t('terveyskeskuskoulutusjaksot') }}</h1>
      <p class="mb-2">
        {{
          $isVirkailija()
            ? $t('terveyskeskuskoulutusjaksot-ingressi-virkailija')
            : $t('terveyskeskuskoulutusjaksot-ingressi-vastuuhenkilo')
        }}
      </p>
      <div v-if="!loading">
        <div
          v-if="
            $isVirkailija() ||
            ($isTerveyskeskuskoulutusjaksoVastuuhenkilo() &&
              $isYekTerveyskeskuskoulutusjaksoVastuuhenkilo())
          "
        >
          <b-tabs v-model="tabIndex" content-class="mt-3" :no-fade="true">
            <b-tab :title="$t('erikoislaakarikoulutus')" href="#erikoislaakarikoulutus">
              <terveyskeskuskoulutusjaksot-el :erikoisalat="erikoisalat" />
            </b-tab>
            <b-tab :title="$t('yleislaaketieteen-erikoiskoulutus')" href="#yek">
              <terveyskeskuskoulutusjaksot-yek />
            </b-tab>
          </b-tabs>
        </div>
        <div v-else-if="$isYekTerveyskeskuskoulutusjaksoVastuuhenkilo()">
          <terveyskeskuskoulutusjaksot-yek />
        </div>
        <div v-else>
          <terveyskeskuskoulutusjaksot-el :erikoisalat="erikoisalat" />
        </div>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import TerveyskeskuskoulutusjaksotEl from './terveyskeskuskoulutusjaksot-el.vue'
  import TerveyskeskuskoulutusjaksotYek from './terveyskeskuskoulutusjaksot-yek.vue'

  import { getErikoisalat } from '@/api/virkailija'
  import { Erikoisala } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      TerveyskeskuskoulutusjaksotEl,
      TerveyskeskuskoulutusjaksotYek
    }
  })
  export default class Terveyskeskuskoulutusjaksot extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('terveyskeskuskoulutusjaksot'),
        active: true
      }
    ]

    erikoisalat: Erikoisala[] | null = null
    loading = true

    tabIndex = 0
    tabs = ['#erikoislaakarikoulutus', '#yek']

    beforeMount() {
      this.tabIndex = this.tabs.findIndex((tab) => tab === this.$route.hash)
    }

    async mounted() {
      try {
        await this.fetchRajaimet()
      } catch {
        toastFail(this, this.$t('terveyskeskuskoulutusjaksojen-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    async fetchRajaimet() {
      this.erikoisalat = this.$isVirkailija() ? (await getErikoisalat()).data : []
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .terveyskeskuskoulutusjaksot {
    max-width: 1024px;
  }
</style>
