<template>
  <div>
    <b-row lg>
      <b-col cols="12" lg="5">
        <elsa-search-input
          class="mb-4 hakutermi"
          :hakutermi.sync="hakutermi"
          :placeholder="$t('hae-koulutettavan-nimella')"
        />
      </b-col>
    </b-row>
    <b-row class="mb-5">
      <b-col>
        <h3>{{ $t('avoimet') }}</h3>
        <yek-valmistumispyynnot-list
          :valmistumispyynnot="valmistumispyynnotAvoimet"
          :valmistumispyynnon-hyvaksyja-role="valmistumispyynnonHyvaksyjaRole"
          :current-page="currentAvoinPage"
          :per-page="perPage"
          :loading="loadingAvoimet"
          :hakutermi-exists="hakutermi.length > 0"
          @update:currentPage="onAvoinPageInput"
        />
      </b-col>
    </b-row>
    <b-row>
      <b-col>
        <h3>{{ $t('valmiit-hyvaksytyt-palautetut') }}</h3>
        <yek-valmistumispyynnot-list
          :valmistumispyynnot="valmistumispyynnotMuut"
          :valmistumispyynnon-hyvaksyja-role="valmistumispyynnonHyvaksyjaRole"
          :current-page="currentMuutPage"
          :per-page="perPage"
          :loading="loadingMuut"
          :hakutermi-exists="hakutermi.length > 0"
          @update:currentPage="onMuutPageInput"
        />
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Vue, Watch } from 'vue-property-decorator'

  import { getValmistumispyynnot } from '@/api/virkailija'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import YekValmistumispyynnotList from '@/components/valmistumispyynnot-list/yek-valmistumispyynnot-list.vue'
  import { Erikoisala, ValmistumispyyntoListItem, Page } from '@/types'
  import { ERIKOISALA_YEK_ID } from '@/utils/constants'
  import { ValmistumispyynnonHyvaksyjaRole } from '@/utils/roles'

  @Component({
    components: {
      ElsaSearchInput,
      YekValmistumispyynnotList
    }
  })
  export default class ValmistumispyynnotVirkailijaYek extends Vue {
    hakutermi = ''
    currentAvoinPage = 1
    currentMuutPage = 1
    perPage = 10
    loading = true
    loadingAvoimet = true
    loadingMuut = true
    valmistumispyynnotAvoimet: Page<ValmistumispyyntoListItem> | null = null
    valmistumispyynnotMuut: Page<ValmistumispyyntoListItem> | null = null
    valmistumispyynnonHyvaksyjaRole: ValmistumispyynnonHyvaksyjaRole | null =
      ValmistumispyynnonHyvaksyjaRole.VIRKAILIJA
    debounce?: number

    filtered: {
      nimi: string | null
      erikoisala: Erikoisala | null
      sortBy: string | null
    } = {
      nimi: null,
      erikoisala: null,
      sortBy: null
    }

    @Watch('hakutermi')
    onPropertyChanged(value: string) {
      this.debounceSearch(value)
    }

    debounceSearch(value: string) {
      this.loadingAvoimet = true
      this.loadingMuut = true
      clearTimeout(this.debounce)
      this.debounce = setTimeout(() => {
        this.filtered.nimi = value
        this.filterResults()
      }, 400)
    }

    async mounted() {
      this.fetchAll()
    }

    async filterResults() {
      this.loadingAvoimet = true
      this.loadingMuut = true
      this.currentAvoinPage = 1
      this.currentMuutPage = 1
      this.fetchAll()
    }

    async fetchAll() {
      this.fetchAvoimet()
      this.fetchMuut()
    }

    async fetchAvoimet() {
      getValmistumispyynnot({
        page: this.currentAvoinPage - 1,
        size: this.perPage,
        sort: 'muokkauspaiva,asc',
        ...{ avoin: true },
        ...(this.filtered.nimi ? { 'erikoistujanNimi.contains': this.filtered.nimi } : {}),
        ...{ 'erikoisalaId.equals': ERIKOISALA_YEK_ID }
      }).then((response) => {
        this.valmistumispyynnotAvoimet = response.data
        this.loadingAvoimet = false
      })
    }

    async fetchMuut() {
      getValmistumispyynnot({
        page: this.currentMuutPage - 1,
        size: this.perPage,
        sort: 'muokkauspaiva,desc',
        ...{ avoin: false },
        ...(this.filtered.nimi ? { 'erikoistujanNimi.contains': this.filtered.nimi } : {}),
        ...{ 'erikoisalaId.equals': ERIKOISALA_YEK_ID }
      }).then((response) => {
        this.valmistumispyynnotMuut = response.data
        this.loadingMuut = false
      })
    }

    onAvoinPageInput(value: number) {
      this.loadingAvoimet = true
      this.currentAvoinPage = value
      this.fetchAvoimet()
    }

    onMuutPageInput(value: number) {
      this.loadingMuut = true
      this.currentMuutPage = value
      this.fetchMuut()
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .hakutermi {
    margin-top: 1.2rem;
  }

  .hakutermi::v-deep .search-input {
    margin-top: 0 !important;
  }

  .filter::v-deep .form-group {
    label {
      font-weight: 300;
      text-transform: uppercase;
      font-size: $font-size-sm;
      margin-bottom: 0;
    }
  }
</style>
