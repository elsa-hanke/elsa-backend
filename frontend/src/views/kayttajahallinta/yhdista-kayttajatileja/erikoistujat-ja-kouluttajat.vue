<template>
  <div>
    <b-row lg>
      <b-col>
        <h1>{{ $t('hae-erikoistujaa-kouluttajaa') }}</h1>
      </b-col>
    </b-row>
    <b-row align-v="center" lg>
      <b-col cols="12" lg="8">
        <elsa-search-input
          class="mb-3"
          :hakutermi.sync="hakutermi"
          :placeholder="$t('hae-erikoistujaa-kouluttajaa')"
        />
      </b-col>
    </b-row>
    <div v-if="!loading">
      <b-alert v-if="hakutermi.length > 0 && rows === 0" variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        <span>
          {{ $t('ei-hakutuloksia') }}
        </span>
      </b-alert>
    </div>
    <div v-else class="text-center">
      <b-spinner variant="primary" :label="$t('ladataan')" />
    </div>
    <b-row v-if="!loading && kayttajat && rows > 0" lg class="mt-4">
      <b-col>
        <h1>{{ $t('erikoistuja-jonka-tili-yhdistetaan') }}</h1>
      </b-col>
    </b-row>
    <b-table
      v-if="!loading && kayttajat && rows > 0"
      class="kayttajat-table"
      :items="erikoistujat"
      :fields="fields"
      stacked="md"
      responsive
    >
      <template #cell(nimi)="row">
        <b-form-radio
          v-model="form.erikoistujaKayttajaId"
          :name="`kayttaja-${row.item.kayttajaId}`"
          :state="null"
          :value="row.item.kayttajaId"
        >
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
        </b-form-radio>
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
    <b-row v-if="!loading && kayttajat && rows > 0" lg class="mt-4">
      <b-col>
        <h1>{{ $t('kouluttaja-jonka-tili-yhdistetaan') }}</h1>
      </b-col>
    </b-row>
    <b-table
      v-if="!loading && kayttajat && rows > 0"
      class="kayttajat-table"
      :items="kouluttajat"
      :fields="fields"
      stacked="md"
      responsive
    >
      <template #cell(nimi)="row">
        <b-form-radio
          v-model="form.kouluttajaKayttajaId"
          :name="`kayttaja-${row.item.kayttajaId}`"
          :state="null"
          :value="row.item.kayttajaId"
        >
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
        </b-form-radio>
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
  </div>
</template>

<script lang="ts">
  import { Component, Mixins, Prop, Watch } from 'vue-property-decorator'

  import { getErikoistujatJaKouluttajat } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import KayttajahallintaMixin from '@/mixins/kayttajahallinta'
  import {
    Erikoisala,
    KayttajahallintaYhdistaKayttajatilejaListItem,
    Page,
    SortByEnum,
    YhdistaKayttajatilejaForm
  } from '@/types'
  import { KayttajaJarjestys } from '@/utils/constants'
  import { ELSA_ROLE } from '@/utils/roles'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaPagination,
      ElsaSearchInput
    }
  })
  export default class ErikoistujatJaKouluttajat extends Mixins(KayttajahallintaMixin) {
    @Prop({ required: true })
    form!: YhdistaKayttajatilejaForm
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
      },
      {
        key: 'sahkoposti',
        label: this.$t('sahkoposti'),
        sortable: false
      },
      {
        key: 'button',
        label: '',
        sortable: false
      }
    ]

    loading = false
    currentPage = 1
    perPage = 20
    debounce?: number
    hakutermi = ''

    kayttajat: Page<KayttajahallintaYhdistaKayttajatilejaListItem> | null = null

    filtered: {
      nimi: string | null
      erikoisala: Erikoisala | null
      useaOpintooikeus: boolean
      sortBy: string | null
    } = {
      nimi: null,
      erikoisala: null,
      useaOpintooikeus: false,
      sortBy: null
    }

    async fetch() {
      if (!this.filtered.nimi) {
        return
      }
      this.kayttajat = (
        await getErikoistujatJaKouluttajat({
          page: this.currentPage - 1,
          size: this.perPage,
          ...(this.filtered.nimi ? { 'nimi.contains': this.filtered.nimi } : {})
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

    get erikoistujat() {
      return this.kayttajat?.content.filter(
        (k) =>
          k.authorities === null ||
          k.authorities.some(
            (authority: string) =>
              authority === ELSA_ROLE.ErikoistuvaLaakari || authority === ELSA_ROLE.YEKKoulutettava
          )
      )
    }

    get kouluttajat() {
      return this.kayttajat?.content.filter((k) =>
        k.authorities?.some((authority: string) => authority === ELSA_ROLE.Kouluttaja)
      )
    }
  }
</script>
