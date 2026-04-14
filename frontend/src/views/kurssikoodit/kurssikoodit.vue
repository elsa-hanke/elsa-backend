<template>
  <div class="kurssikoodit mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row>
        <b-col>
          <h1>{{ $t('kurssikoodien-yllapito') }}</h1>
          <p class="mb-2">{{ $t('kurssikoodien-yllapito-kuvaus') }}</p>
          <elsa-button
            :to="{
              name: 'lisaa-kurssikoodi'
            }"
            variant="primary"
            class="mb-2 mt-2"
          >
            {{ $t('lisaa-kurssikoodi') }}
          </elsa-button>
        </b-col>
      </b-row>
      <b-row lg>
        <b-col>
          <search-input
            class="mb-4"
            :hakutermi.sync="hakutermi"
            :placeholder="$t('hae-kursseja-tunnisteella')"
          />
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <div v-if="loading" class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
          <div v-else-if="rows === 0">
            <b-alert variant="dark" show>
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span v-if="hakutermi.length > 0">
                {{ $t('ei-hakutuloksia') }}
              </span>
              <span v-else>
                {{ $t('ei-kurssikoodeja') }}
              </span>
            </b-alert>
          </div>
          <b-table
            v-else
            :items="kurssikooditSorted"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            class="kurssikoodit-table"
            stacked="md"
            responsive
          >
            <template #cell(tunniste)="data">
              <b-link
                :to="{
                  name: 'kurssikoodi',
                  params: { kurssikoodiId: data.item.id }
                }"
                class="task-type"
              >
                {{ data.item.tunniste }}
              </b-link>
            </template>

            <template #cell(tyyppi)="data">
              {{ $t('kurssikoodi-tyyppi-' + data.item.tyyppi.nimi) }}
            </template>
          </b-table>
          <pagination
            :current-page.sync="currentPage"
            :per-page="perPage"
            :rows="rows"
            :loading="loading"
          />
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getKurssikoodit } from '@/api/virkailija'
  import ElsaButton from '@/components/button/button.vue'
  import Pagination from '@/components/pagination/pagination.vue'
  import SearchInput from '@/components/search-input/search-input.vue'
  import { Kurssikoodi } from '@/types'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      Pagination,
      SearchInput
    }
  })
  export default class Kurssikoodit extends Vue {
    kurssikoodit: Kurssikoodi[] = []

    loading = true
    perPage = 20
    currentPage = 1
    hakutermi = ''

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('kurssikoodien-yllapito'),
        active: true
      }
    ]

    fields = [
      {
        key: 'tunniste',
        label: this.$t('tunniste'),
        class: 'tunniste',
        sortable: true
      },
      {
        key: 'tyyppi',
        label: this.$t('tyyppi'),
        class: 'tyyppi',
        sortable: true
      }
    ]

    async mounted() {
      await this.fetchKurssikoodit()
      this.loading = false
    }

    async fetchKurssikoodit() {
      try {
        this.kurssikoodit = (await getKurssikoodit()).data
      } catch (err) {
        toastFail(this, this.$t('kurssikoodien-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'etusivu' })
      }
    }

    get kurssikooditSorted() {
      if (this.hakutermi) {
        this.currentPage = 1
        return this.sortKurssikoodit(
          this.kurssikoodit?.filter((item: Kurssikoodi) =>
            item.tunniste?.toLowerCase().includes(this.hakutermi.toLowerCase())
          )
        )
      }
      return this.sortKurssikoodit(this.kurssikoodit)
    }

    sortKurssikoodit(kurssikoodit: Kurssikoodi[]) {
      return kurssikoodit.sort((a, b) =>
        a.tyyppi?.nimi === b.tyyppi?.nimi
          ? sortByAsc(a.tunniste, b.tunniste)
          : sortByAsc(a.tyyppi?.nimi, b.tyyppi?.nimi)
      )
    }

    get rows() {
      return this.kurssikoodit?.length ?? 0
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .kurssikoodit {
    max-width: 1420px;
  }
  ::v-deep .kurssikoodit-table {
    .row-details {
      padding: 0;
      table {
        margin: 0;

        border: none;
        thead,
        &tr {
          display: none;
        }
        td {
          word-wrap: break-all;
        }
      }
    }

    @include media-breakpoint-down(sm) {
      border-bottom: 0;

      tr {
        border: $table-border-width solid $table-border-color;
        border-radius: $border-radius;
        margin-top: 0.5rem;
        padding-top: $table-cell-padding;
        padding-bottom: $table-cell-padding;
      }

      td {
        border: none;
        padding: 0 0.5rem;

        & > div {
          width: 100% !important;
          padding: 0 0 0.5rem 0 !important;
        }

        &::before {
          text-align: left !important;
          font-weight: 500 !important;
          width: 100% !important;
          padding-right: 0 !important;
        }
        &:last-child > div {
          padding-bottom: 0 !important;
        }

        &.last > div {
          padding-bottom: 0 !important;
        }
      }
    }
  }
</style>
