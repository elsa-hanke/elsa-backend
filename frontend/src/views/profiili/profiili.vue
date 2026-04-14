<template>
  <div class="profiili">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('oma-profiili') }}</h1>
          <p v-if="$isErikoistuva() || $isYekKoulutettava() || $isYekKoulutettava()">
            {{ $t('oma-profiili-kuvaus-erikoistuja') }}
          </p>
          <p v-else>{{ $t('oma-profiili-kuvaus') }}</p>
          <b-tabs v-model="tabIndex" content-class="mt-3" :no-fade="true">
            <b-tab
              v-if="$isErikoistuva() || $isYekKoulutettava()"
              :title="$t('omat-tiedot')"
              href="#omat-tiedot"
            >
              <omat-tiedot-erikoistuja :editing="editing" @change="changeEditing" />
            </b-tab>
            <b-tab v-else :title="$t('omat-tiedot')" href="#omat-tiedot">
              <omat-tiedot :editing="editing" @change="changeEditing" />
            </b-tab>
            <b-tab
              v-if="$isErikoistuva()"
              :title="$t('katseluoikeudet')"
              href="#katseluoikeudet"
              lazy
            >
              <katseluoikeudet />
            </b-tab>
            <b-tab
              v-if="$isErikoistuva() || $isYekKoulutettava()"
              :title="$t('muokkausoikeudet')"
              href="#muokkausoikeudet"
              lazy
            >
              <muokkausoikeudet />
            </b-tab>
          </b-tabs>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Mixins } from 'vue-property-decorator'

  import OmatTiedotErikoistuja from './omat-tiedot-erikoistuja.vue'
  import OmatTiedot from './omat-tiedot.vue'

  import ConfirmRouteExit from '@/mixins/confirm-route-exit'
  import Katseluoikeudet from '@/views/profiili/katseluoikeudet.vue'
  import Muokkausoikeudet from '@/views/profiili/muokkausoikeudet.vue'

  @Component({
    components: {
      OmatTiedot,
      OmatTiedotErikoistuja,
      Katseluoikeudet,
      Muokkausoikeudet
    }
  })
  export default class Profiili extends Mixins(ConfirmRouteExit) {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('oma-profiili'),
        active: true
      }
    ]
    editing = false
    skipRouteExitConfirm = true

    tabIndex = 0
    tabs = ['#omat-tiedot', '#katseluoikeudet']

    beforeMount() {
      this.tabIndex = this.tabs.findIndex((tab) => tab === this.$route.hash)
    }

    changeEditing(event: boolean) {
      this.skipRouteExitConfirm = !event
      this.editing = event
    }
  }
</script>

<style lang="scss" scoped>
  .profiili {
    max-width: 768px;
  }
</style>
