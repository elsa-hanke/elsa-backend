<template>
  <div>
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('seurantakeskustelut') }}</h1>
          <p>{{ $t('erikoistujan-pidemman-aikavalin-arviointi') }}</p>
          <search-input
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
                {{ $t('ei-seurantakeskusteluja') }}
              </span>
            </b-alert>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
          <b-table
            v-if="!loading && rows > 0"
            fixed
            :items="tulokset"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            class="seurantakeskustelun-vaiheet-table"
            details-td-class="row-details"
            tbody-tr-class="outer-table"
            stacked="md"
          >
            <template #table-colgroup>
              <col span="1" width="18%" />
              <col span="1" width="25%" />
              <col span="1" width="27%" />
              <col span="1" width="10%" />
              <col span="1" width="20%" />
            </template>

            <template #head()="scope">
              <div class="border-top-0">{{ scope.label }}</div>
            </template>

            <template #cell(erikoistuvanNimi)="data">
              {{ data.item.erikoistuvanNimi }}
            </template>

            <template #cell(seurantajakso)="data">
              <b-link
                :to="{
                  name: linkComponent(data.item.tila),
                  params: { seurantajaksoId: data.item.id }
                }"
                class="task-type"
              >
                {{ $date(data.item.alkamispaiva) }} - {{ $date(data.item.paattymispaiva) }}
              </b-link>
            </template>

            <template #cell(tila)="data">
              <font-awesome-icon
                :icon="taskIcon(data.item.tila)"
                :class="taskClass(data.item.tila)"
              />
              {{ taskStatus(data.item.tila) }}
            </template>

            <template #cell(pvm)="data">
              <span class="text-nowrap">
                {{ $date(data.item.tallennettu) }}
              </span>
            </template>

            <template #cell(actions)="row">
              <b-link
                v-if="row.item.aiemmatJaksot.length > 0"
                class="text-decoration-none"
                @click="row.toggleDetails"
              >
                {{ row.detailsShowing ? $t('piilota') : $t('nayta') }} {{ $t('aiemmat') }}
                <font-awesome-icon
                  :icon="row.detailsShowing ? 'chevron-up' : 'chevron-down'"
                  fixed-width
                  size="lg"
                  class="text-dark"
                />
              </b-link>
            </template>

            <template #row-details="row">
              <b-table
                :items="row.item.aiemmatJaksot"
                :fields="fields"
                stacked="md"
                fixed
                tbody-tr-class="inner-table"
              >
                <template #table-colgroup>
                  <col span="1" width="18%" />
                  <col span="1" width="25%" />
                  <col span="1" width="27%" />
                  <col span="1" width="10%" />
                  <col span="1" width="20%" />
                </template>

                <template #cell(seurantajakso)="data">
                  <b-link
                    :to="{
                      name: linkComponent(data.item.tila),
                      params: { seurantajaksoId: data.item.id }
                    }"
                    class="task-type"
                  >
                    {{ $date(data.item.alkamispaiva) }} - {{ $date(data.item.paattymispaiva) }}
                  </b-link>
                </template>

                <template #cell(tila)="data">
                  <font-awesome-icon
                    :icon="taskIcon(data.item.tila)"
                    :class="taskClass(data.item.tila)"
                  />
                  {{ taskStatus(data.item.tila) }}
                </template>

                <template #cell(pvm)="data">
                  {{ $date(data.item.tallennettu) }}
                </template>
              </b-table>
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

  import { getSeurantajaksot } from '@/api/kouluttaja'
  import Pagination from '@/components/pagination/pagination.vue'
  import SearchInput from '@/components/search-input/search-input.vue'
  import { Seurantajakso } from '@/types'
  import { SeurantajaksoTila, TaskStatus } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      SearchInput,
      Pagination
    }
  })
  export default class SeurantakeskustelutViewKouluttajaVastuuhenkilo extends Vue {
    seurantajaksot: Seurantajakso[] = []

    loading = true

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('seurantakeskustelut'),
        active: true
      }
    ]

    fields = [
      {
        key: 'erikoistuvanNimi',
        label: this.$t('erikoistuja'),
        class: 'nimi',
        sortable: true
      },
      {
        key: 'seurantajakso',
        label: this.$t('seurantajakso'),
        class: 'seurantajakso',
        sortable: true
      },
      {
        key: 'tila',
        label: this.$t('tila'),
        class: 'tila',
        sortable: true
      },
      {
        key: 'pvm',
        label: this.$t('pvm'),
        class: 'pvm',
        sortable: true
      },
      {
        key: 'actions',
        label: '',
        sortable: false,
        class: 'actions'
      }
    ]
    perPage = 20
    currentPage = 1
    hakutermi = ''

    async mounted() {
      this.loading = true
      try {
        this.seurantajaksot = (await getSeurantajaksot()).data
      } catch {
        toastFail(this, this.$t('seurantakeskustelujen-hakeminen-epaonnistui'))
        this.seurantajaksot = []
      }
      this.loading = false
    }

    get rows() {
      return this.tulokset?.length ?? 0
    }

    linkComponent(tila: SeurantajaksoTila) {
      if (
        tila === SeurantajaksoTila.ODOTTAA_ARVIOINTIA ||
        tila === SeurantajaksoTila.ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA ||
        tila === SeurantajaksoTila.ODOTTAA_HYVAKSYNTAA
      ) {
        return 'muokkaa-seurantajaksoa'
      }
      return 'seurantajakso'
    }

    taskIcon(tila: SeurantajaksoTila) {
      switch (tila) {
        case SeurantajaksoTila.HYVAKSYTTY:
          return ['fas', 'check-circle']
        case SeurantajaksoTila.PALAUTETTU_KORJATTAVAKSI:
          return ['fas', 'undo-alt']
        case SeurantajaksoTila.ODOTTAA_YHTEISIA_MERKINTOJA:
          return ['far', 'check-circle']
        default:
          return ['far', 'clock']
      }
    }

    taskClass(tila: SeurantajaksoTila) {
      switch (tila) {
        case SeurantajaksoTila.HYVAKSYTTY:
          return 'text-success'
        case SeurantajaksoTila.PALAUTETTU_KORJATTAVAKSI:
          return ''
        case SeurantajaksoTila.ODOTTAA_YHTEISIA_MERKINTOJA:
          return 'text-success'
        default:
          return 'text-warning'
      }
    }

    taskStatus(tila: SeurantajaksoTila) {
      switch (tila) {
        case SeurantajaksoTila.HYVAKSYTTY:
          return this.$t('lomake-tila-' + TaskStatus.HYVAKSYTTY)
        case SeurantajaksoTila.PALAUTETTU_KORJATTAVAKSI:
          return this.$t('lomake-tila-' + TaskStatus.PALAUTETTU)
        case SeurantajaksoTila.ODOTTAA_YHTEISIA_MERKINTOJA:
          return this.$t('lomake-tila-' + TaskStatus.ODOTTAA_YHTEISIA_MERKINTOJA)
        default:
          return this.$t('lomake-tila-' + TaskStatus.AVOIN)
      }
    }

    get tulokset() {
      if (this.hakutermi) {
        this.currentPage = 1
        return this.seurantajaksot?.filter((item: Seurantajakso) =>
          item.erikoistuvanNimi?.toLowerCase().includes(this.hakutermi.toLowerCase())
        )
      }

      return this.seurantajaksot
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .task-type {
    text-transform: capitalize;
  }

  ::v-deep {
    .seurantakeskustelun-vaiheet-table {
      .row-details {
        padding: 0;
        background-color: #f5f5f6;
        table {
          margin: 0.375rem 0 0.375rem 0;

          border: none;
          thead,
          &tr {
            display: none;
          }
          td {
            word-wrap: break-all;
            padding-top: 0.375rem;
            padding-bottom: 0.375rem;
            border-top: none;
          }
        }
      }
      @include media-breakpoint-up(xl) {
        .actions {
          text-align: right;
        }
      }

      @include media-breakpoint-down(sm) {
        border-bottom: none;

        tr {
          padding: 0.375rem 0 0.375rem 0;
          border: $table-border-width solid $table-border-color;

          &.outer-table {
            margin-bottom: 0.75rem;
            border-radius: 0.25rem;
          }

          &.b-table-has-details {
            margin-bottom: 0;
            border-radius: 0.25rem 0.25rem 0 0;
          }

          &.b-table-details {
            border: none;
            padding: 0;
            margin-bottom: 0.75rem;
            :last-of-type {
              border-radius: 0 0 0.25rem 0.25rem;
            }

            table {
              margin: 0;

              tr {
                border-top: none;
                margin-top: 0;
                padding-top: 0;
                td {
                  padding-top: 0.75rem;
                  &.nimi,
                  &.actions {
                    display: none;
                  }
                }
              }
            }
          }
        }

        td {
          padding: 0.25rem 0 0.25rem 0.25rem;
          border: none;

          &.nimi {
            font-size: $h4-font-size;
            > div {
              width: 100% !important;
              padding: 0.25rem 0.375rem 0 0.375rem !important;
            }
            &::before {
              display: none;
            }
          }

          &.seurantajakso,
          &.tila,
          &.pvm,
          &.actions {
            > div {
              &:empty {
                display: none !important;
              }
              padding: 0 0.375rem 0 0.375rem !important;
            }
            &::before {
              text-align: left !important;
              padding-left: 0.375rem !important;
              font-weight: 500 !important;
              width: 100% !important;
            }
            text-align: left;
          }
        }
      }
    }
  }
</style>
