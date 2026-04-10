<template>
  <div class="suoritteet mb-4">
    <b-row align-h="between" align-v="center">
      <b-col sm="6" cols="auto">
        <elsa-button
          :to="{
            name: 'lisaa-suoritteen-kategoria'
          }"
          variant="primary"
          class="mb-2 mt-2 mr-3"
        >
          {{ $t('lisaa-kategoria') }}
        </elsa-button>
        <elsa-button
          :to="{
            name: 'lisaa-suorite'
          }"
          variant="primary"
          class="mb-2 mt-2"
        >
          {{ $t('lisaa-suorite') }}
        </elsa-button>
      </b-col>
      <b-col cols="5" xl="auto">
        <b-row align-v="center" class="voimassaolo">
          <b-col cols="auto">
            <span class="text-uppercase text-size-sm">{{ $t('voimassaolopvm') }}:</span>
          </b-col>
          <b-col cols="auto" md="5">
            <elsa-form-datepicker id="voimassaolo" v-model="voimassaolo"></elsa-form-datepicker>
          </b-col>
        </b-row>
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
            <span v-if="voimassaolo != null">
              {{ $t('ei-hakutuloksia') }}
            </span>
            <span v-else>
              {{ $t('ei-suoritteita') }}
            </span>
          </b-alert>
        </div>
        <b-table
          v-else
          :items="kategoriatSorted"
          :fields="fields"
          class="suoritteet-table"
          details-td-class="row-details"
          stacked="md"
          responsive
        >
          <template #table-colgroup>
            <col span="1" width="10%" />
            <col span="1" width="45%" />
            <col span="1" width="27%" />
          </template>

          <template #cell(jarjestys)="data">
            {{ data.item.jarjestysnumero }}
          </template>

          <template #cell(nimi)="data">
            <b-link
              :to="{
                name: 'suoritteen-kategoria',
                params: { kategoriaId: data.item.id }
              }"
            >
              {{ data.item.nimi }}
            </b-link>
          </template>

          <template #row-details="row">
            <b-table :items="filteredSuoritteet(row.item.suoritteet)" :fields="fields">
              <template #table-colgroup>
                <col span="1" width="12%" />
                <col span="1" width="45%" />
                <col span="1" width="27%" />
              </template>

              <template #cell(nimi)="data">
                <b-link
                  :to="{
                    name: 'suorite',
                    params: { suoriteId: data.item.id }
                  }"
                >
                  {{ data.item.nimi }}
                </b-link>
              </template>

              <template #cell(voimassaolo)="data">
                {{ $date(data.item.voimassaolonAlkamispaiva) }} -
                {{
                  data.item.voimassaolonPaattymispaiva != null
                    ? $date(data.item.voimassaolonPaattymispaiva)
                    : ''
                }}
              </template>
            </b-table>
          </template>
        </b-table>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getSuoritteet } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import { Suorite, SuoritteenKategoria } from '@/types'
  import { dateBetween } from '@/utils/date'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormDatepicker,
      ElsaFormGroup
    }
  })
  export default class Suoritteet extends Vue {
    kategoriat: SuoritteenKategoria[] = []

    voimassaolo: string | null = null

    loading = true

    fields = [
      {
        key: 'jarjestys',
        label: this.$t('jarjestys'),
        class: 'jarjestys'
      },
      {
        key: 'nimi',
        label: this.$t('nimi'),
        class: 'nimi'
      },
      {
        key: 'voimassaolo',
        label: this.$t('voimassaolo'),
        class: 'voimassaolo'
      },
      {
        key: 'vaadittulkm',
        label: this.$t('vaadittulkm'),
        class: 'vaadittulkm'
      }
    ]

    async mounted() {
      await this.fetchKategoriat()
      this.loading = false
    }

    async fetchKategoriat() {
      try {
        this.kategoriat = (await getSuoritteet(this.$route?.params?.erikoisalaId)).data
      } catch (err) {
        toastFail(this, this.$t('suoritteiden-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat' })
      }
    }

    get kategoriatSorted() {
      return this.kategoriat
        .map((k) => ({ ...k, _showDetails: 'true' }))
        .sort((a, b) => sortByAsc(a.jarjestysnumero, b.jarjestysnumero, true))
    }

    get rows() {
      return this.kategoriatSorted?.length ?? 0
    }

    filteredSuoritteet(suoritteet: Suorite[]) {
      return suoritteet
        .filter((s) =>
          dateBetween(
            this.voimassaolo ?? undefined,
            s.voimassaolonAlkamispaiva,
            s.voimassaolonPaattymispaiva ?? undefined
          )
        )
        .sort((a, b) => sortByAsc(a.nimi, b.nimi))
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .voimassaolo {
    justify-content: flex-end;

    @include media-breakpoint-down(sm) {
      justify-content: flex-start;
      margin-bottom: 1rem;
    }
  }

  ::v-deep .suoritteet-table {
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

      .vaadittulkm {
        display: none !important;
      }

      .b-table-has-details {
        .voimassaolo {
          display: none !important;
        }

        .vaadittulkm {
          display: none !important;
        }
      }

      .b-table-details {
        padding-top: 0;
        padding-bottom: 0;
        margin-top: 0;
        border: 0;

        tr {
          &:first-child {
            border-top: 0 !important;
          }
        }
      }

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
