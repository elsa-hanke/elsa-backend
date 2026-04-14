<template>
  <b-card-skeleton :header="$t('vanhenevat-katseluoikeudet')" :loading="loading" class="mb-4">
    <div v-if="rows > 0">
      <p class="mb-3">{{ $t('vanhenevat-katseluoikeudet-ingressi') }}</p>
      <b-table :items="katseluoikeudet" :fields="fields" stacked="md" responsive>
        <template #cell(erikoistujanNimi)="row">
          {{ row.item.erikoistujanNimi }}
        </template>
        <template #cell(katseluoikeusVanhenee)="row">
          <span class="text-nowrap">
            {{ $date(row.item.katseluoikeusVanhenee) }}
          </span>
        </template>
      </b-table>
    </div>
    <div v-else>
      <b-alert variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        {{ $t('ei-vanhenevia-katseluoikeuksia') }}
      </b-alert>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getEtusivuVanhenevatKatseluoikeudet } from '@/api/kouluttaja'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import { Katseluoikeus } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class ArviointipyynnotCard extends Vue {
    katseluoikeudet: Katseluoikeus[] | null = null
    loading = true

    async mounted() {
      try {
        this.katseluoikeudet = (await getEtusivuVanhenevatKatseluoikeudet()).data
      } catch (err) {
        toastFail(this, this.$t('katseluoikeuksien-hakeminen-epaonnistui'))
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
          key: 'katseluoikeusVanhenee',
          label: this.$t('katseluoikeus-vanhenee'),
          sortable: true
        }
      ]
    }

    get rows() {
      return this.katseluoikeudet?.length ?? 0
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
    }
  }
</style>
