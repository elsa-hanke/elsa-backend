<template>
  <div v-if="showHyvaksynnat">
    <b-row class="mb-3">
      <b-col>
        <h3>{{ $t(title) }}</h3>
      </b-col>
    </b-row>
    <b-row v-for="(hyvaksynta, index) in hyvaksynnat" :key="index">
      <b-col class="hyvaksynta-pvm col-xxl-1" lg="2">
        <h5>{{ $t('paivays') }}</h5>
        <p>
          {{ hyvaksynta.pvm ? $date(hyvaksynta.pvm) : '' }}
        </p>
      </b-col>
      <b-col>
        <h5>{{ $t('nimi-ja-nimike') }}</h5>
        <p>
          {{ hyvaksynta.nimiAndNimike }}
        </p>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import { KoejaksonVaiheHyvaksynta } from '@/types'

  @Component({})
  export default class KoejaksonVaiheHyvaksynnat extends Vue {
    @Prop({ required: true, default: [] })
    hyvaksynnat!: KoejaksonVaiheHyvaksynta[] | null

    @Prop({ required: false, default: 'muokkauspaivamaarat' })
    title!: string

    get showHyvaksynnat() {
      return this.hyvaksynnat ? this.hyvaksynnat.length > 0 : false
    }
  }
</script>

<style lang="scss" scoped>
  .hyvaksynta-pvm {
    min-width: 7rem;
  }
</style>
