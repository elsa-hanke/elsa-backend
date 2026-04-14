<template>
  <b-card-skeleton :header="$t('seurantajaksot')" :loading="loading" class="mb-4">
    <div v-if="rows > 0">
      <b-table fixed :items="seurantajaksot" :fields="fields" stacked="md">
        <template #cell(erikoistujanNimi)="row">
          <b-link
            :to="{
              name: 'muokkaa-seurantajaksoa',
              params: { seurantajaksoId: row.item.id }
            }"
            class="task-type"
          >
            {{ row.item.erikoistujanNimi }}
          </b-link>
        </template>
        <template #cell(pvm)="row">
          <span class="text-nowrap">
            {{ $date(row.item.tallennettu) }}
          </span>
        </template>
        <template #cell(seurantajakso)="data">
          {{ $date(data.item.alkamispaiva) }} - {{ $date(data.item.paattymispaiva) }}
        </template>
        <template #cell(actions)="row">
          <elsa-button
            variant="primary"
            class="pt-1 pb-1"
            :to="{
              name: 'muokkaa-seurantajaksoa',
              params: { seurantajaksoId: row.item.id }
            }"
          >
            {{ getActionTitle(row.item.tila) }}
          </elsa-button>
        </template>
      </b-table>
    </div>
    <div v-else>
      <b-alert variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        {{ $t('ei-seurantajaksoja') }}
      </b-alert>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getEtusivuSeurantajaksot as getEtusivuSeurantajaksotKouluttaja } from '@/api/kouluttaja'
  import { getEtusivuSeurantajaksot as getEtusivuSeurantajaksotVastuuhenkilo } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import { Seurantajakso } from '@/types'
  import { SeurantajaksoTila } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class SeurantajaksotCard extends Vue {
    seurantajaksot: Seurantajakso[] | null = null
    loading = true

    async mounted() {
      try {
        if (this.$isVastuuhenkilo()) {
          this.seurantajaksot = (await getEtusivuSeurantajaksotVastuuhenkilo()).data
        } else {
          this.seurantajaksot = (await getEtusivuSeurantajaksotKouluttaja()).data
        }
      } catch (err) {
        toastFail(this, this.$t('seurantajaksojen-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get fields() {
      return [
        {
          key: 'erikoistujanNimi',
          label: this.$t('erikoistuja'),
          sortable: true
        },
        {
          key: 'pvm',
          label: this.$t('pvm'),
          sortable: true
        },
        {
          key: 'seurantajakso',
          label: this.$t('seurantajakso'),
          class: 'seurantajakso',
          sortable: true
        },
        {
          key: 'actions',
          label: '',
          sortable: false,
          class: 'actions'
        }
      ]
    }

    get rows() {
      return this.seurantajaksot?.length ?? 0
    }

    getActionTitle(tila: SeurantajaksoTila) {
      if (
        tila === SeurantajaksoTila.ODOTTAA_ARVIOINTIA ||
        tila === SeurantajaksoTila.ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA
      ) {
        return this.$t('tee-arviointi')
      }
      return this.$t('hyvaksy')
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep {
    .card-body {
      padding-top: 0.75rem;
      padding-bottom: 0.5rem;
    }

    table {
      margin-bottom: 0;
      border-bottom: none;

      td {
        padding-top: 0.25rem;
        padding-bottom: 0.25rem;
        vertical-align: middle;
      }

      .actions {
        text-align: right;
        padding-right: 1.5rem;
        .btn {
          min-width: 8rem;
        }
      }
    }

    @include media-breakpoint-down(sm) {
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

        padding: 0.25rem 0 0.25rem 0.25rem;
        border: none;
        &::before {
          text-align: left !important;
          padding-left: 0.5rem !important;
          font-weight: 500 !important;
          width: 100% !important;
        }
      }

      table {
        .actions {
          text-align: left;
        }
      }
    }
  }
</style>
