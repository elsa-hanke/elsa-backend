<template>
  <div>
    <div v-if="loading" class="text-center">
      <b-spinner variant="primary" :label="$t('ladataan')" />
    </div>
    <b-table
      :items="flatList"
      :fields="fields"
      class="arviointityokalut-table"
      stacked="md"
      responsive
    >
      <template #cell(nimi)="data">
        <template v-if="data.item.isCategory">
          <b-link
            :to="{
              name: 'kategoria',
              params: { kategoriaId: data.item.kategoriaId }
            }"
            class="font-weight-bold"
          >
            {{ data.item.nimi }}
          </b-link>
        </template>
        <template v-else>
          <div class="pl-4">
            <b-link
              :to="{
                name: 'arviointityokalu',
                params: { arviointityokaluId: data.item.arviointityokaluId }
              }"
            >
              {{ data.item.nimi }}
            </b-link>
          </div>
        </template>
      </template>

      <template #cell(tila)="data">
        <template v-if="!data.item.isCategory">
          <template v-if="poistetut">
            <span class="text-danger">
              {{ $t('poistettu') }}
            </span>
          </template>
          <template v-else-if="data.item.tila.toLowerCase() === 'julkaistu'">
            <span class="text-success">
              {{ $t('arviointityokalu-tila-' + data.item.tila.toLowerCase()) }}
            </span>
          </template>
          <template v-else>
            {{ $t('arviointityokalu-tila-' + data.item.tila.toLowerCase()) }}
          </template>
        </template>
      </template>
    </b-table>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import {
    getArviointityokalut,
    getArviointityokalutKategoriat,
    getPoistetutArviointityokalut
  } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import Pagination from '@/components/pagination/pagination.vue'
  import SearchInput from '@/components/search-input/search-input.vue'
  import { Arviointityokalu, ArviointityokaluKategoria } from '@/types'
  import { MUU_ARVIOINTITYOKALU_ID } from '@/utils/constants'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'
  import ErikoistuvatLaakarit from '@/views/kayttajahallinta/erikoistuvat-laakarit.vue'
  import Kouluttajat from '@/views/kayttajahallinta/kouluttajat.vue'
  import Paakayttajat from '@/views/kayttajahallinta/paakayttajat.vue'
  import Vastuuhenkilot from '@/views/kayttajahallinta/vastuuhenkilot.vue'
  import Virkailijat from '@/views/kayttajahallinta/virkailijat.vue'

  @Component({
    components: {
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
  export default class ArviointityokalutListaus extends Vue {
    @Prop({ required: false, default: false })
    poistetut!: boolean

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

    async mounted() {
      this.loading = true
      try {
        this.arviointityokaluKategoriat = (await getArviointityokalutKategoriat()).data.sort(
          (a, b) => sortByAsc(a.nimi, b.nimi)
        )
        this.arviointityokalut = (
          this.poistetut ? await getPoistetutArviointityokalut() : await getArviointityokalut()
        ).data
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
