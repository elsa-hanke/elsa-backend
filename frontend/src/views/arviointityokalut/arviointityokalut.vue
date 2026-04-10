<template>
  <div class="arviointityokalut">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('arviointityokalut') }}</h1>
          <p>{{ $t('arviointityokalut-kuvaus') }}</p>
          <elsa-button variant="primary" :to="{ name: 'lisaa-arviointityokalu' }" class="mb-4 mr-2">
            {{ $t('lisaa-arviointityokalu') }}
          </elsa-button>
          <elsa-button variant="primary" :to="{ name: 'uusi-kategoria' }" class="mb-4">
            {{ $t('lisaa-kategoria') }}
          </elsa-button>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <div v-if="loading" class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
          <b-tabs v-model="tabIndex" content-class="mt-3" :no-fade="true">
            <b-tab :title="$t('julkaistut-ja-luonnokset')" href="#arviointityokalut">
              <ArviointityokalutListaus :poistetut="false"></ArviointityokalutListaus>
            </b-tab>
            <b-tab :title="$t('poistetut')" lazy href="#poistetut-arviointityokalut">
              <ArviointityokalutListaus :poistetut="true"></ArviointityokalutListaus>
            </b-tab>
          </b-tabs>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getArviointityokalut, getArviointityokalutKategoriat } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import Pagination from '@/components/pagination/pagination.vue'
  import SearchInput from '@/components/search-input/search-input.vue'
  import { Arviointityokalu, ArviointityokaluKategoria } from '@/types'
  import { MUU_ARVIOINTITYOKALU_ID } from '@/utils/constants'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'
  import ArviointityokalutListaus from '@/views/arviointityokalut/arviointityokalut-listaus.vue'
  import ErikoistuvatLaakarit from '@/views/kayttajahallinta/erikoistuvat-laakarit.vue'
  import Kouluttajat from '@/views/kayttajahallinta/kouluttajat.vue'
  import Paakayttajat from '@/views/kayttajahallinta/paakayttajat.vue'
  import Vastuuhenkilot from '@/views/kayttajahallinta/vastuuhenkilot.vue'
  import Virkailijat from '@/views/kayttajahallinta/virkailijat.vue'

  @Component({
    components: {
      ArviointityokalutListaus,
      Vastuuhenkilot,
      ErikoistuvatLaakarit,
      Kouluttajat,
      Virkailijat,
      Paakayttajat,
      ElsaButton,
      SearchInput,
      Pagination
    }
  })
  export default class Arviointityokalut extends Vue {
    arviointityokaluKategoriat: ArviointityokaluKategoria[] = []
    arviointityokalut: Arviointityokalu[] = []

    loading = true

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arviointityokalut'),
        active: true
      }
    ]

    fields = [
      {
        key: 'nimi',
        label: this.$t('nimi'),
        class: 'nimi',
        sortable: false
      },
      {
        key: 'tila',
        label: this.$t('tila'),
        class: 'tila',
        sortable: false
      }
    ]
    perPage = 20
    currentPage = 1

    tabIndex = 0
    tabs = ['#arviointityokalut', '#poistetut-arviointityokalut']

    beforeMount() {
      this.tabIndex = this.tabs.findIndex((tab) => tab === this.$route.hash)
    }

    async mounted() {
      this.loading = true
      try {
        this.arviointityokaluKategoriat = (await getArviointityokalutKategoriat()).data.sort(
          (a, b) => sortByAsc(a.nimi, b.nimi)
        )
        this.arviointityokalut = (await getArviointityokalut()).data
          .sort((a, b) => sortByAsc(a.nimi, b.nimi))
          .filter((at) => at.id !== MUU_ARVIOINTITYOKALU_ID)
      } catch {
        toastFail(this, this.$t('arviointityokalujen-kategorioiden-hakeminen-epaonnistui'))
        this.arviointityokaluKategoriat = []
        this.arviointityokalut = []
      }
      this.loading = false
    }

    get flatList() {
      let list = []
      list.push(
        ...this.arviointityokalut
          .filter((tool) => tool.kategoria === null)
          .map((tool) => ({ ...tool, isCategory: false, arviointityokaluId: tool.id }))
      )
      this.arviointityokaluKategoriat.forEach((category) => {
        list.push({ nimi: category.nimi, isCategory: true, kategoriaId: category.id })
        list.push(
          ...this.arviointityokalut
            .filter((tool) => tool.kategoria?.id === category.id)
            .map((tool) => ({ ...tool, isCategory: false, arviointityokaluId: tool.id }))
        )
      })
      return list
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .arviointityokalut {
    max-width: 1024px;
  }
</style>
