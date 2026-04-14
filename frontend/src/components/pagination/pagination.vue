<template>
  <b-pagination
    v-if="rows > perPage"
    v-model="currentPageMutable"
    class="mt-4 user-select-none"
    :total-rows="rows"
    :per-page="perPage"
    :first-number="true"
    :last-number="true"
    align="center"
    pills
  >
    <template slot="prev-text">
      <font-awesome-icon icon="chevron-left" fixed-width size="lg" />
      {{ $t('edellinen') }}
    </template>
    <template slot="next-text">
      {{ $t('seuraava') }}
      <font-awesome-icon icon="chevron-right" fixed-width size="lg" />
    </template>
  </b-pagination>
</template>

<script lang="ts">
  import { Component, Vue, Prop } from 'vue-property-decorator'

  @Component({})
  export default class ElsaPagination extends Vue {
    @Prop({ required: true, default: null })
    currentPage!: any | null

    @Prop({ required: true, default: 0 })
    rows!: number

    @Prop({ required: true, default: 20 })
    perPage!: number

    get currentPageMutable() {
      return this.currentPage
    }

    set currentPageMutable(val) {
      this.$emit('update:currentPage', val)
    }
  }
</script>
