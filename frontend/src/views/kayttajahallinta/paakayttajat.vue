<template>
  <div>
    <b-row align-v="center" lg>
      <b-col cols="12" lg="4">
        <elsa-search-input
          class="mb-3"
          :hakutermi.sync="hakutermi"
          :placeholder="$t('hae-paakayttajan-nimella')"
        />
      </b-col>
      <b-col cols="12" lg="4">
        <div class="drop-down-filter">
          <elsa-form-group :label="$t('jarjestys')" class="mb-4">
            <template #default="{ uid }">
              <elsa-form-multiselect
                :id="uid"
                v-model="sortBy"
                :options="sortFields"
                label="name"
                :taggable="true"
                @select="onSortBySelect"
              >
                <template #option="{ option }">
                  <div v-if="option.name">{{ option.name }}</div>
                </template>
              </elsa-form-multiselect>
            </template>
          </elsa-form-group>
        </div>
      </b-col>
    </b-row>
    <div v-if="!loading">
      <b-alert v-if="rows === 0" variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        <span v-if="hakutermi.length > 0">
          {{ $t('ei-hakutuloksia') }}
        </span>
        <span v-else>
          {{ $t('ei-kayttajia') }}
        </span>
      </b-alert>
    </div>
    <div v-else class="text-center">
      <b-spinner variant="primary" :label="$t('ladataan')" />
    </div>

    <b-table
      v-if="!loading && kayttajat && rows > 0"
      class="kayttajat-table"
      :items="kayttajat.content"
      :fields="fields"
      stacked="md"
      responsive
    >
      <template #cell(nimi)="row">
        <elsa-button
          :to="{
            name: 'paakayttaja',
            params: { kayttajaId: row.item.kayttajaId }
          }"
          variant="link"
          class="p-0 border-0 shadow-none"
        >
          <span>{{ row.item.sukunimi }}&nbsp;{{ row.item.etunimi }}</span>
        </elsa-button>
      </template>
      <template #cell(tila)="row">
        <span :class="getTilaColor(row.item.kayttajatilinTila)">
          {{ $t(`tilin-tila-${row.item.kayttajatilinTila}`) }}
        </span>
      </template>
    </b-table>
    <elsa-pagination
      v-if="!loading"
      :current-page="currentPage"
      :per-page="perPage"
      :rows="rows"
      @update:currentPage="onPageInput"
    />
  </div>
</template>

<script lang="ts">
  import { Component, Mixins, Watch } from 'vue-property-decorator'

  import { getPaakayttajat } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import KayttajahallintaListMixin from '@/mixins/kayttajahallinta-list'
  import { SortByEnum } from '@/types'
  import { KayttajaJarjestys } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaPagination,
      ElsaSearchInput
    }
  })
  export default class Paakayttajat extends Mixins(KayttajahallintaListMixin) {
    fields = [
      {
        key: 'nimi',
        label: this.$t('nimi'),
        sortable: false
      },
      {
        key: 'tila',
        label: this.$t('tilin-tila'),
        sortable: false
      }
    ]

    async mounted() {
      this.loading = true
      try {
        await this.fetch()
      } catch {
        toastFail(this, this.$t('kayttajien-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    async fetch() {
      this.kayttajat = (
        await getPaakayttajat({
          page: this.currentPage - 1,
          size: this.perPage,
          sort: this.filtered.sortBy ?? 'user.lastName,asc',
          ...(this.filtered.nimi ? { 'nimi.contains': this.filtered.nimi } : {})
        })
      ).data
    }

    @Watch('hakutermi')
    onPropertyChanged(value: string) {
      this.debounceSearch(value)
    }

    onPageInput(value: number) {
      this.currentPage = value
      this.fetch()
    }

    onSortBySelect(sortByEnum: SortByEnum) {
      switch (sortByEnum.value) {
        case KayttajaJarjestys.SUKUNIMI_ASC:
          this.filtered.sortBy = 'user.lastName,asc'
          break
        case KayttajaJarjestys.SUKUNIMI_DESC:
          this.filtered.sortBy = 'user.lastName,desc'
          break
      }
      this.filterResults()
    }

    debounceSearch(value: string) {
      clearTimeout(this.debounce)
      this.debounce = setTimeout(() => {
        this.filtered.nimi = value
        this.filterResults()
      }, 400)
    }

    async filterResults() {
      this.loading = true
      this.currentPage = 1
      await this.fetch()
      this.loading = false
    }

    get rows() {
      return this.kayttajat?.page.totalElements ?? 0
    }
  }
</script>
