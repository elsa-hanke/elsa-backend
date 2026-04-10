<template>
  <div>
    <b-row align-v="center" lg>
      <b-col cols="12" lg="4">
        <elsa-search-input
          class="mb-3"
          :hakutermi.sync="hakutermi"
          :placeholder="$t('hae-erikoistujan-nimella')"
        />
      </b-col>
      <b-col cols="12" lg="4">
        <div v-if="rajaimet.erikoisalat.length > 1" class="drop-down-filter">
          <elsa-form-group :label="$t('erikoisala')">
            <template #default="{ uid }">
              <elsa-form-multiselect
                :id="uid"
                v-model="filtered.erikoisala"
                :options="rajaimet.erikoisalat"
                label="nimi"
                @select="onErikoisalaSelect"
                @clearMultiselect="onErikoisalaReset"
              ></elsa-form-multiselect>
            </template>
          </elsa-form-group>
        </div>
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
    <b-row lg>
      <b-col cols="12">
        <b-form-checkbox
          v-model="filtered.useaOpintooikeus"
          class="mb-4"
          @input="onUseaOpintooikeusInput"
        >
          {{ $t('nayta-erikoistujat-useampi-opintooikeus') }}
        </b-form-checkbox>
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
            name: 'erikoistuva-laakari',
            params: { kayttajaId: row.item.kayttajaId }
          }"
          variant="link"
          class="p-0 border-0 shadow-none"
        >
          <span>{{ row.item.sukunimi }}&nbsp;{{ row.item.etunimi }}</span>
          <span v-if="row.item.syntymaaika">&nbsp;({{ $date(row.item.syntymaaika) }})</span>
        </elsa-button>
      </template>
      <template #cell(opintooikeus)="row">
        <div v-for="(item, index) in row.item.yliopistotAndErikoisalat" :key="index">
          {{ `${$t(`yliopisto-nimi.${item.yliopisto}`)}: ${item.erikoisala}` }}
        </div>
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

  import { getErikoistuvatLaakarit } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import KayttajahallintaListMixin from '@/mixins/kayttajahallinta-list'
  import { Erikoisala, SortByEnum } from '@/types'
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
  export default class ErikoistuvatLaakarit extends Mixins(KayttajahallintaListMixin) {
    fields = [
      {
        key: 'nimi',
        label: this.$t('nimi'),
        sortable: false
      },
      {
        key: 'opintooikeus',
        label: this.$t('opintooikeus'),
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
        await getErikoistuvatLaakarit({
          page: this.currentPage - 1,
          size: this.perPage,
          sort: this.filtered.sortBy ?? 'kayttaja.user.lastName,asc',
          ...(this.filtered.nimi ? { 'nimi.contains': this.filtered.nimi } : {}),
          ...(this.filtered.erikoisala?.id
            ? { 'erikoisalaId.equals': this.filtered.erikoisala.id }
            : {}),
          ...{ 'useaOpintooikeus.equals': this.filtered.useaOpintooikeus }
        })
      ).data
    }

    @Watch('hakutermi')
    onPropertyChanged(value: string) {
      this.debounceSearch(value)
    }

    onErikoisalaSelect(erikoisala: Erikoisala) {
      this.filtered.erikoisala = erikoisala
      this.filterResults()
    }

    onErikoisalaReset() {
      this.filtered.erikoisala = null
      this.filterResults()
    }

    onUseaOpintooikeusInput(checked: boolean) {
      this.filtered.useaOpintooikeus = checked
      this.filterResults()
    }

    onPageInput(value: number) {
      this.currentPage = value
      this.fetch()
    }

    onSortBySelect(sortByEnum: SortByEnum) {
      switch (sortByEnum.value) {
        case KayttajaJarjestys.SUKUNIMI_ASC:
          this.filtered.sortBy = 'kayttaja.user.lastName,asc'
          break
        case KayttajaJarjestys.SUKUNIMI_DESC:
          this.filtered.sortBy = 'kayttaja.user.lastName,desc'
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
