<template>
  <div>
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('arvioinnit') }}</h1>
          <p>{{ $t('arvioinnit-kouluttaja-kuvaus') }}</p>
          <elsa-search-input
            class="mb-4"
            :hakutermi.sync="hakutermi"
            :placeholder="$t('hae-erikoistujan-nimella')"
          />
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <div v-if="!loading">
            <b-alert v-if="rows === 0" variant="dark" show>
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span v-if="hakutermi.length > 0">
                {{ $t('ei-hakutuloksia') }}
              </span>
              <span v-else>
                {{ $t('ei-suoritusarviointeja') }}
              </span>
            </b-alert>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
          <b-table
            v-if="!loading && rows > 0"
            class="arvioinnit-table"
            :items="tulokset"
            :fields="fields"
            :per-page="perPage"
            :sort-compare="sortCompare"
            :current-page="currentPage"
            fixed
            responsive
            stacked="xl"
          >
            <template #table-colgroup>
              <col span="1" style="width: 15%" />
              <col span="1" style="width: 6rem" />
              <col span="1" style="width: 7rem" />
              <col span="1" style="width: 15%" />
              <col span="1" style="width: 12rem" />
              <col span="1" style="width: 9rem" />
            </template>
            <!-- eslint-disable-next-line -->
            <template #cell(arvioinninSaaja.nimi)="row">
              <elsa-button
                :to="{
                  name: 'arviointi',
                  params: { arviointiId: row.item.id }
                }"
                variant="link"
                class="shadow-none px-0 text-truncate text-left w-100"
              >
                {{ row.item.arvioinninSaaja.nimi }}
              </elsa-button>
            </template>
            <template #cell(tapahtumanAjankohta)="row">
              <span class="text-nowrap">
                {{ $date(row.item.arviointiAika ? row.item.arviointiAika : row.item.pyynnonAika) }}
              </span>
            </template>
            <template #cell(tila)="row">
              <span v-if="row.item.lukittu" class="text-nowrap">
                <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success" />
                {{ $t('hyvaksytty') }}
              </span>
              <span v-else-if="row.item.arviointiAika" class="text-nowrap">
                <font-awesome-icon :icon="['far', 'check-circle']" class="text-success" />
                {{ $t('valmis') }}
              </span>
              <span v-else-if="row.item.keskenerainen">
                <font-awesome-icon :icon="['far', 'clock']" class="text-warning" />
                {{ $t('avoin-tallennettu-keskeneraisena') }}
              </span>
              <span v-else class="text-nowrap">
                <font-awesome-icon :icon="['far', 'clock']" class="text-warning" />
                {{ $t('avoin') }}
              </span>
            </template>
            <template #cell(arvioitavaTapahtuma)="row">
              {{ row.item.arvioitavaTapahtuma }}
            </template>

            <!-- eslint-disable-next-line -->
            <template #cell(tyoskentelyjakso.tyoskentelypaikka.nimi)="row">
              <div class="text-truncate w-100">
                {{ row.item.tyoskentelyjakso.tyoskentelypaikka.nimi }}
              </div>
            </template>
            <template #cell(teeArviointi)="row">
              <elsa-button
                v-if="!row.item.arviointiAika"
                variant="primary"
                :to="{
                  name: 'muokkaa-arviointia',
                  params: { arviointiId: row.item.id }
                }"
                class="pt-1 pb-1"
              >
                {{ $t('tee-arviointi') }}
              </elsa-button>
            </template>
          </b-table>
          <elsa-pagination
            v-if="!loading"
            :current-page.sync="currentPage"
            :per-page="perPage"
            :rows="rows"
            :style="{ 'max-width': '1420px' }"
          />
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import { Suoritusarviointi } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ElsaSearchInput,
      ElsaPagination
    }
  })
  export default class ArvioinnitList extends Vue {
    @Prop({ required: true, default: undefined })
    arvioinnit!: null | Suoritusarviointi[]

    @Prop({ required: false, type: Boolean, default: false })
    loading!: boolean

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arvioinnit'),
        active: true
      }
    ]
    fields = [
      {
        key: 'arvioinninSaaja.nimi',
        label: this.$t('erikoistuja'),
        sortable: true
      },
      {
        key: 'tapahtumanAjankohta',
        label: this.$t('pvm'),
        sortable: true,
        thClass: 'pvm-field'
      },
      {
        key: 'tila',
        label: this.$t('tila'),
        sortable: true,
        thClass: 'tila-field'
      },
      {
        key: 'arvioitavaTapahtuma',
        label: this.$t('tapahtuma'),
        sortable: true
      },
      {
        key: 'tyoskentelyjakso.tyoskentelypaikka.nimi',
        label: this.$t('tyoskentelypaikka'),
        sortable: true
      },
      {
        key: 'teeArviointi',
        label: '',
        class: 'tee-arviointi'
      }
    ]

    hakutermi = ''
    perPage = 20
    currentPage = 1

    get tulokset() {
      return this.hakutermi
        ? this.arvioinnit?.filter(
            (item) =>
              item.arvioinninSaaja.nimi &&
              item.arvioinninSaaja.nimi.toLowerCase().includes(this.hakutermi.toLowerCase())
          ) || []
        : this.arvioinnit || []
    }

    get rows() {
      return this.tulokset?.length || 0
    }

    sortCompare(a: Suoritusarviointi, b: Suoritusarviointi, key: string): number {
      if (key == 'tila') {
        if (a.lukittu) {
          return 1
        }
        if (b.lukittu) {
          return -1
        }
        if (a.arviointiAika) {
          return 1
        }
        if (b.arviointiAika) {
          return -1
        }
        return 0
      }
      return -1
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .arvioinnit-table {
    max-width: 1420px;
    ::v-deep table {
      td {
        padding-top: 0.25rem;
        padding-bottom: 0.25rem;
        vertical-align: middle;
      }
      @include media-breakpoint-down(lg) {
        border-bottom: none;

        tr {
          padding: 0.375rem 0 0.375rem 0;
          border: $table-border-width solid $table-border-color;
          border-radius: 0.25rem;
          margin-bottom: 0.75rem;
        }

        td {
          > div {
            width: 100% !important;
          }
          &.tee-arviointi {
            > div {
              &:empty {
                display: none !important;
              }
              a.btn {
                padding: 0.25rem 2rem !important;
                margin: 0.5rem 0 0.3275rem 0;
                display: inline-block !important;
              }
            }
          }

          padding: 0.25rem 0 0.25rem 0.25rem;
          border: none;
          &::before {
            text-align: left !important;
            padding-left: 0.5rem !important;
            font-weight: 500 !important;
            width: 100% !important;
          }
        }
      }
    }
  }
</style>
