<template>
  <div>
    <elsa-search-input
      :hakutermi.sync="hakutermi"
      :visible="searchVisible"
      :placeholder="$t('hae-asiakirjoja')"
    />
    <div v-if="!loading">
      <b-alert v-if="showInfoIfEmpty && rows === 0" variant="dark" class="mt-3" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        <span v-if="asiakirjat.length === 0">
          {{ noResultsInfoText ? noResultsInfoText : $t('ei-asiakirjoja') }}
        </span>
        <span v-else>
          {{ noSearchResultsInfoText ? noSearchResultsInfoText : $t('ei-hakutuloksia') }}
        </span>
      </b-alert>
    </div>
    <div v-else class="text-center">
      <b-spinner variant="primary" :label="$t('ladataan')" />
    </div>
    <b-table
      v-if="rows > 0"
      class="asiakirjat-table"
      :items="tulokset"
      :fields="computedFields"
      :per-page="perPage"
      :current-page="currentPage"
      :sort-by.sync="sortBy"
      :sort-desc.sync="sortDesc"
      stacked="sm"
    >
      <template #table-colgroup="scope">
        <col v-for="field in scope.fields" :key="field.key" :style="{ width: `${field.width}%` }" />
      </template>
      <template #cell(nimi)="row">
        <elsa-button
          variant="link"
          class="shadow-none p-0"
          :loading="row.item.disablePreview"
          @click="onViewAsiakirja(row.item)"
        >
          {{ row.item.nimi }}
        </elsa-button>
      </template>
      <template #cell(download)="row">
        <elsa-button
          variant="outline-primary"
          class="border-0 p-0"
          :loading="row.item.disableDownload"
          @click="onDownloadAsiakirja(row.item)"
        >
          <font-awesome-icon
            :hidden="row.item.disableDownload"
            :icon="['fas', 'file-download']"
            fixed-width
            size="lg"
          />
        </elsa-button>
      </template>
      <template v-if="enableDelete" #cell(delete)="row">
        <elsa-button
          variant="outline-primary"
          class="border-0 p-0"
          :loading="row.item.disableDelete"
          @click="onDeleteAsiakirja(row.item)"
        >
          <font-awesome-icon
            :hidden="row.item.disableDelete"
            :icon="['far', 'trash-alt']"
            fixed-width
            size="lg"
          />
        </elsa-button>
      </template>
    </b-table>
    <elsa-pagination
      v-if="!loading"
      :current-page.sync="currentPage"
      :per-page="perPage"
      :rows="rows"
    />
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import { Asiakirja, AsiakirjatTableField } from '@/types'
  import { saveBlob, fetchAndSaveBlob, openBlob, fetchAndOpenBlob } from '@/utils/blobs'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaSearchInput,
      ElsaPagination
    }
  })
  export default class AsiakirjatContent extends Vue {
    hakutermi = ''
    currentPage = 1

    @Prop({ required: true, default: () => [] })
    asiakirjat!: Asiakirja[]

    @Prop({ required: false, type: String, default: 'erikoistuva-laakari/asiakirjat/' })
    asiakirjaDataEndpointUrl!: string

    @Prop({ required: false, type: String, default: 'lisattypvm' })
    sortBy!: string

    @Prop({ required: false, type: Boolean, default: true })
    sortingEnabled!: boolean

    @Prop({ required: false, type: Boolean, default: true })
    sortDesc!: boolean

    @Prop({ required: false, type: Boolean, default: true })
    paginationEnabled!: boolean

    @Prop({ required: false, type: Number, default: 20 })
    perPage!: number

    @Prop({ required: false, type: Boolean, default: true })
    enableSearch!: boolean

    @Prop({ required: false, type: Boolean, default: true })
    enableDelete!: boolean

    @Prop({ required: false, type: Boolean, default: true })
    enableLisatty!: boolean

    @Prop({ required: false, type: Boolean, default: true })
    showInfoIfEmpty!: boolean

    @Prop({ required: false, type: String })
    noResultsInfoText?: string

    @Prop({ required: false, type: String })
    noSearchResultsInfoText?: string

    @Prop({ required: false, type: Boolean, default: false })
    loading!: boolean

    @Prop({ required: false, type: String })
    confirmDeleteTitle?: string

    @Prop({ required: false, type: String })
    confirmDeleteTypeText?: string

    fields: AsiakirjatTableField[] = []

    mounted() {
      this.fields = [
        {
          key: 'nimi',
          label: this.$t('tiedoston-nimi'),
          class: 'file-name'
        },
        {
          key: 'lisattypvm',
          label: this.$t('lisatty'),
          class: 'created-date',
          sortable: this.sortingEnabled,
          width: 10,
          formatter: (val: string) => {
            return new Date(val).toLocaleDateString(this.$i18n.locale)
          },
          disabled: !this.enableLisatty
        },
        {
          key: 'download',
          label: '',
          width: 5,
          class: this.enableDelete ? 'download-btn float-left-xs' : 'download-btn'
        },
        {
          key: 'delete',
          label: '',
          width: 5,
          disabled: !this.enableDelete
        }
      ] as AsiakirjatTableField[]
    }

    async onDeleteAsiakirja(asiakirja: Asiakirja) {
      if (
        await confirmDelete(
          this,
          this.confirmDeleteTitle ?? (this.$t('poista-liitetiedosto') as string),
          this.confirmDeleteTypeText?.toLowerCase() ??
            (this.$t('liitetiedoston') as string).toLowerCase()
        )
      ) {
        this.$emit('deleteAsiakirja', asiakirja)
      }
    }

    async onViewAsiakirja(asiakirja: Asiakirja) {
      Vue.set(asiakirja, 'disablePreview', true)

      if (!asiakirja.isDirty) {
        const success = await fetchAndOpenBlob(this.asiakirjaDataEndpointUrl, asiakirja.id)
        if (!success) {
          toastFail(this, this.$t('asiakirjan-sisallon-hakeminen-epaonnistui'))
        }
      } else {
        asiakirja.data?.then((data) => openBlob(data, asiakirja.contentType ?? '')) ??
          toastFail(this, this.$t('asiakirjan-sisallon-hakeminen-epaonnistui'))
      }

      Vue.set(asiakirja, 'disablePreview', false)
    }

    async onDownloadAsiakirja(asiakirja: Asiakirja) {
      Vue.set(asiakirja, 'disableDownload', true)
      if (!asiakirja.isDirty) {
        const success = await fetchAndSaveBlob(
          this.asiakirjaDataEndpointUrl,
          asiakirja.nimi,
          asiakirja.id
        )
        if (!success) {
          toastFail(this, this.$t('asiakirjan-sisallon-hakeminen-epaonnistui'))
        }
      } else {
        asiakirja.data?.then((data) =>
          saveBlob(asiakirja.nimi, data, asiakirja.contentType ?? '')
        ) ?? toastFail(this, this.$t('asiakirjan-sisallon-hakeminen-epaonnistui'))
      }
      Vue.set(asiakirja, 'disableDownload', false)
    }

    get tulokset() {
      if (this.hakutermi) {
        this.currentPage = 1
        return this.asiakirjat?.filter((item) =>
          item.nimi.toLowerCase().includes(this.hakutermi.toLowerCase())
        )
      }

      return this.asiakirjat
    }

    get rows() {
      return this.tulokset?.length ?? 0
    }

    get searchVisible() {
      return this.enableSearch && this.asiakirjat?.length > 0
    }

    get computedFields() {
      return this.fields.filter((field) => !field.disabled)
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep {
    @include media-breakpoint-down(xs) {
      .asiakirjat-table {
        border-bottom: none;
        tr {
          padding: 0.375rem 0 0.375rem 0;
          border: $table-border-width solid $table-border-color;
          border-radius: 0.25rem;
          margin-bottom: 0.5rem;
        }

        td {
          padding: 0.25rem 0 0.25rem 0.25rem;
          &.file-name {
            > div {
              width: 100% !important;
              padding: 0 0.375rem 0 0.375rem !important;
            }
            &::before {
              display: none;
            }
          }
          &.created-date::before {
            text-align: left !important;
            padding-left: 0.375rem !important;
            font-weight: 500 !important;
            width: 100% !important;
          }
          &.download-btn {
            width: 2rem;
            > div {
              padding: 0 0 0 0.25rem !important;
            }
          }
          border: none;
        }
      }
      .float-left-xs {
        float: left;
      }
    }
  }
</style>
