<template>
  <div>
    <b-row lg>
      <b-col cols="12" lg="5">
        <elsa-search-input
          class="mb-4 hakutermi"
          :hakutermi.sync="hakutermi"
          :placeholder="$t('hae-erikoistujan-nimella')"
        />
      </b-col>
      <b-col cols="12" lg="4" md="6">
        <div v-if="erikoisalat != null && erikoisalat.length > 1" class="filter">
          <elsa-form-group :label="$t('erikoisala')" class="mb-4">
            <template #default="{ uid }">
              <elsa-form-multiselect
                :id="uid"
                v-model="filtered.erikoisala"
                :options="erikoisalatSorted"
                label="nimi"
                @select="onErikoisalaSelect"
                @clearMultiselect="onErikoisalaReset"
              ></elsa-form-multiselect>
            </template>
          </elsa-form-group>
        </div>
      </b-col>
    </b-row>
    <b-row>
      <b-col>
        <h3>{{ $t('avoimet') }}</h3>
        <div v-if="!loading">
          <b-alert v-if="avoimetRows === 0" variant="dark" show>
            <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
            <span v-if="hakutermi.length > 0">
              {{ $t('ei-hakutuloksia') }}
            </span>
            <span v-else>
              {{ $t('ei-avoimia-terveyskoulutusjaksoja') }}
            </span>
          </b-alert>
          <div v-else>
            <b-table
              v-if="avoimetJaksot != null"
              fixed
              :items="avoimetJaksot.content"
              :fields="fields"
              class="terveyskeskuskoulutusjakson-vaiheet-table"
              stacked="md"
            >
              <template #table-colgroup>
                <col span="1" width="43%" />
                <col span="1" width="27%" />
                <col span="1" width="10%" />
                <col span="1" width="20%" />
              </template>

              <template #head()="scope">
                <div class="border-top-0">{{ scope.label }}</div>
              </template>

              <template #cell(erikoistuvanNimi)="data">
                <b-link
                  :to="{
                    name: $isVirkailija()
                      ? 'terveyskeskuskoulutusjakson-tarkistus'
                      : 'terveyskeskuskoulutusjakson-hyvaksynta',
                    params: { terveyskeskuskoulutusjaksoId: data.item.id }
                  }"
                  class="task-type"
                >
                  {{ data.item.erikoistuvanNimi }}
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
                  {{ data.item.pvm ? $date(data.item.pvm) : '' }}
                </span>
              </template>

              <template #cell(actions)="row">
                <elsa-button
                  variant="primary"
                  :to="{
                    name: $isVirkailija()
                      ? 'terveyskeskuskoulutusjakson-tarkistus'
                      : 'terveyskeskuskoulutusjakson-hyvaksynta',
                    params: { terveyskeskuskoulutusjaksoId: row.item.id }
                  }"
                >
                  {{ $isVirkailija() ? $t('tarkista') : $t('hyvaksy') }}
                </elsa-button>
              </template>
            </b-table>
            <elsa-pagination
              :current-page="currentAvoinPage"
              :per-page="perPage"
              :rows="avoimetRows"
              @update:currentPage="onAvoinPageInput"
            />
          </div>
        </div>
        <div v-else class="text-center">
          <b-spinner variant="primary" :label="$t('ladataan')" />
        </div>
        <h3 class="mt-4">{{ $t('valmiit-hyvaksytyt-palautetut') }}</h3>
        <div v-if="!loading">
          <b-alert v-if="muutRows === 0" variant="dark" show>
            <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
            <span v-if="hakutermi.length > 0">
              {{ $t('ei-hakutuloksia') }}
            </span>
            <span v-else>
              {{ $t('ei-terveyskeskuskoulutusjaksoja') }}
            </span>
          </b-alert>
          <div v-else>
            <b-table
              v-if="muutJaksot != null"
              fixed
              :items="muutJaksot.content"
              :fields="fields"
              class="terveyskeskuskoulutusjakson-vaiheet-table"
              stacked="md"
            >
              <template #table-colgroup>
                <col span="1" width="43%" />
                <col span="1" width="27%" />
                <col span="1" width="10%" />
                <col span="1" width="20%" />
              </template>

              <template #head()="scope">
                <div class="border-top-0">{{ scope.label }}</div>
              </template>

              <template #cell(erikoistuvanNimi)="data">
                <b-link
                  :to="{
                    name: $isVirkailija()
                      ? 'terveyskeskuskoulutusjakson-tarkistus'
                      : 'terveyskeskuskoulutusjakson-hyvaksynta',
                    params: { terveyskeskuskoulutusjaksoId: data.item.id }
                  }"
                  class="task-type"
                >
                  {{ data.item.erikoistuvanNimi }}
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
                  {{ data.item.pvm ? $date(data.item.pvm) : '' }}
                </span>
              </template>
            </b-table>
            <elsa-pagination
              :current-page="currentMuutPage"
              :per-page="perPage"
              :rows="muutRows"
              @update:currentPage="onMuutPageInput"
            />
          </div>
        </div>
        <div v-else class="text-center">
          <b-spinner variant="primary" :label="$t('ladataan')" />
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop, Watch } from 'vue-property-decorator'

  import { getTerveyskeskuskoulutusjaksot as getTerveyskeskuskoulutusjaksotVastuuhenkilo } from '@/api/vastuuhenkilo'
  import {
    getErikoisalat,
    getTerveyskeskuskoulutusjaksot as getTerveyskeskuskoulutusjaksotVirkailija
  } from '@/api/virkailija'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import { Erikoisala, Page, TerveyskeskuskoulutusjaksonVaihe } from '@/types'
  import { LomakeTilat, TaskStatus, TerveyskeskuskoulutusjaksonTila } from '@/utils/constants'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaSearchInput,
      ElsaPagination,
      ElsaButton,
      ElsaFormGroup,
      ElsaFormMultiselect
    }
  })
  export default class TerveyskeskuskoulutusjaksotEl extends Vue {
    avoimetJaksot: Page<TerveyskeskuskoulutusjaksonVaihe> | null = null
    muutJaksot: Page<TerveyskeskuskoulutusjaksonVaihe> | null = null
    hakutermi = ''

    @Prop({ required: true, default: () => [] })
    erikoisalat!: Erikoisala[]

    fields = [
      {
        key: 'erikoistuvanNimi',
        label: this.$t('erikoistuja'),
        class: 'nimi',
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

    filtered: {
      nimi: string | null
      erikoisala: Erikoisala | null
      sortBy: string | null
    } = {
      nimi: null,
      erikoisala: null,
      sortBy: null
    }
    currentAvoinPage = 1
    currentMuutPage = 1
    perPage = 10
    debounce?: number
    loading = true

    async mounted() {
      try {
        await this.fetch()
      } catch {
        toastFail(this, this.$t('terveyskeskuskoulutusjaksojen-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    async fetchRajaimet() {
      this.erikoisalat = this.$isVirkailija() ? (await getErikoisalat()).data : []
    }

    async fetch() {
      await this.fetchAvoimet()
      await this.fetchMuut()
    }

    async fetchAvoimet() {
      const avoinParams = {
        page: this.currentAvoinPage - 1,
        size: this.perPage,
        sort: this.filtered.sortBy ?? 'muokkauspaiva,asc',
        ...(this.filtered.nimi ? { 'erikoistujanNimi.contains': this.filtered.nimi } : {}),
        ...(this.filtered.erikoisala?.id
          ? { 'erikoisalaId.equals': this.filtered.erikoisala.id }
          : {}),
        avoin: true
      }
      this.avoimetJaksot = (
        await (this.$isVirkailija()
          ? getTerveyskeskuskoulutusjaksotVirkailija(avoinParams)
          : getTerveyskeskuskoulutusjaksotVastuuhenkilo(avoinParams))
      ).data
    }

    async fetchMuut() {
      const muutParams = {
        page: this.currentMuutPage - 1,
        size: this.perPage,
        sort: this.filtered.sortBy ?? 'muokkauspaiva,desc',
        ...(this.filtered.nimi ? { 'nimi.contains': this.filtered.nimi } : {}),
        ...(this.filtered.erikoisala?.id
          ? { 'erikoisalaId.equals': this.filtered.erikoisala.id }
          : {}),
        avoin: false
      }
      this.muutJaksot = (
        await (this.$isVirkailija()
          ? getTerveyskeskuskoulutusjaksotVirkailija(muutParams)
          : getTerveyskeskuskoulutusjaksotVastuuhenkilo(muutParams))
      ).data
    }

    @Watch('hakutermi')
    onPropertyChanged(value: string) {
      this.debounceSearch(value)
    }

    debounceSearch(value: string) {
      clearTimeout(this.debounce)
      this.debounce = setTimeout(() => {
        this.filtered.nimi = value
        this.onResultsFiltered()
      }, 400)
    }

    async onAvoinPageInput(value: number) {
      this.currentAvoinPage = value
      await this.fetchAvoimet()
    }

    onMuutPageInput(value: number) {
      this.currentMuutPage = value
      this.fetchMuut()
    }

    onErikoisalaSelect(erikoisala: Erikoisala) {
      this.filtered.erikoisala = erikoisala
      this.onResultsFiltered()
    }

    onErikoisalaReset() {
      this.filtered.erikoisala = null
      this.onResultsFiltered()
    }

    private async onResultsFiltered() {
      this.loading = true
      this.currentAvoinPage = 1
      this.currentMuutPage = 1
      await this.fetch()
      this.loading = false
    }

    get avoimetRows() {
      return this.avoimetJaksot?.page.totalElements ?? 0
    }

    get muutRows() {
      return this.muutJaksot?.page.totalElements ?? 0
    }

    get erikoisalatSorted() {
      return this.erikoisalat?.sort((a, b) => sortByAsc(a.nimi, b.nimi))
    }

    taskIcon(status: string) {
      switch (status) {
        case TerveyskeskuskoulutusjaksonTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA:
          return ['far', 'clock']
        case TerveyskeskuskoulutusjaksonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          return this.$isVirkailija() ? ['far', 'check-circle'] : ['far', 'clock']
        case TerveyskeskuskoulutusjaksonTila.PALAUTETTU_KORJATTAVAKSI:
          return ['fas', 'undo-alt']
        case LomakeTilat.HYVAKSYTTY:
          return ['fas', 'check-circle']
      }
    }

    taskClass(status: string) {
      switch (status) {
        case TerveyskeskuskoulutusjaksonTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA:
          return 'text-warning'
        case TerveyskeskuskoulutusjaksonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          return this.$isVirkailija() ? 'text-success' : 'text-warning'
        case LomakeTilat.PALAUTETTU_KORJATTAVAKSI:
          return ''
        case LomakeTilat.HYVAKSYTTY:
          return 'text-success'
      }
    }

    taskStatus(status: string) {
      switch (status) {
        case TerveyskeskuskoulutusjaksonTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA:
          return this.$t('lomake-tila-' + TaskStatus.AVOIN)
        case TerveyskeskuskoulutusjaksonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          return this.$isVirkailija()
            ? this.$t('lomake-tila-' + TaskStatus.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA)
            : this.$t('lomake-tila-' + TaskStatus.AVOIN)
        case TerveyskeskuskoulutusjaksonTila.PALAUTETTU_KORJATTAVAKSI:
          return this.$t('lomake-tila-' + TaskStatus.PALAUTETTU)
        case TerveyskeskuskoulutusjaksonTila.HYVAKSYTTY:
          return this.$t('lomake-tila-' + TaskStatus.HYVAKSYTTY)
      }
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .task-type {
    text-transform: capitalize;
  }

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

  ::v-deep .terveyskeskuskoulutusjakson-vaiheet-table {
    td {
      vertical-align: middle;
    }

    @include media-breakpoint-up(md) {
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
</style>
