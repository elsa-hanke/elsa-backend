<template>
  <b-card-skeleton :header="$t('arviointipyynnot')" :loading="loading" class="mb-4">
    <div v-if="rows > 0">
      <b-table fixed :items="arviointipyynnot" :fields="fields" stacked="md">
        <template #cell(erikoistujanNimi)="row">
          <b-link
            :to="{
              name: 'muokkaa-arviointia',
              params: { arviointiId: row.item.id }
            }"
            class="task-type"
          >
            {{ row.item.erikoistujanNimi }}
          </b-link>
        </template>
        <template #cell(pyynnonAika)="row">
          <span class="text-nowrap">
            {{ $date(row.item.pyynnonAika) }}
          </span>
        </template>
        <template #cell(actions)="row">
          <elsa-button
            variant="primary"
            class="pt-1 pb-1"
            :to="{
              name: 'muokkaa-arviointia',
              params: { arviointiId: row.item.id }
            }"
          >
            {{ $t('tee-arviointi') }}
          </elsa-button>
        </template>
      </b-table>
    </div>
    <div v-else>
      <b-alert variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        {{ $t('ei-arviointipyyntoja') }}
      </b-alert>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getEtusivuArviointipyynnot as getArviointipyynnotKouluttaja } from '@/api/kouluttaja'
  import { getEtusivuArviointipyynnot as getArviointipyynnotVastuuhenkilo } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import { Arviointipyynto } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class ArviointipyynnotCard extends Vue {
    arviointipyynnot: Arviointipyynto[] | null = null
    loading = true

    async mounted() {
      try {
        this.arviointipyynnot = this.$isVastuuhenkilo()
          ? (await getArviointipyynnotVastuuhenkilo()).data
          : (await getArviointipyynnotKouluttaja()).data
      } catch (err) {
        toastFail(this, this.$t('arviointipyyntojen-hakeminen-epaonnistui'))
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
          key: 'pyynnonAika',
          label: this.$t('pvm'),
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
      return this.arviointipyynnot?.length ?? 0
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
