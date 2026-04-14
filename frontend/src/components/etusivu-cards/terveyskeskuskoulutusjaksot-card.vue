<template>
  <b-card-skeleton :header="$t('terveyskeskuskoulutusjaksot')" :loading="loading" class="mb-4">
    <div v-if="rows > 0">
      <b-table fixed :items="vaiheet" :fields="fields" stacked="md">
        <template #cell(pvm)="data">
          <span class="text-nowrap">
            {{ data.item.pvm ? $date(data.item.pvm) : '' }}
          </span>
        </template>

        <template #cell(tyyppi)="data">
          {{ $t('lomake-tyyppi-' + data.item.tyyppi) }}
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

        <template #cell(actions)="data">
          <elsa-button
            variant="primary"
            class="pt-1 pb-1"
            :to="{
              name: $isVirkailija()
                ? 'terveyskeskuskoulutusjakson-tarkistus'
                : 'terveyskeskuskoulutusjakson-hyvaksynta',
              params: { terveyskeskuskoulutusjaksoId: data.item.id }
            }"
          >
            {{ $isVirkailija() ? $t('tarkista') : $t('hyvaksy') }}
          </elsa-button>
        </template>
      </b-table>
    </div>
    <div v-else>
      <b-alert variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        {{ $t('ei-avoimia-terveyskoulutusjaksoja') }}
      </b-alert>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getTerveyskeskuskoulutusjaksot as getTerveyskeskuskoulutusjaksotVastuuhenkilo } from '@/api/vastuuhenkilo'
  import { getTerveyskeskuskoulutusjaksot as getTerveyskeskuskoulutusjaksotVirkailija } from '@/api/virkailija'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import { TerveyskeskuskoulutusjaksonVaihe } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class TerveyskeskuskoulutusjaksotCard extends Vue {
    vaiheet: TerveyskeskuskoulutusjaksonVaihe[] | null = null

    get fields() {
      return [
        {
          key: 'erikoistuvanNimi',
          label: this.$t('erikoistuja'),
          sortable: true
        },
        {
          key: 'pvm',
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

    loading = true

    async mounted() {
      try {
        const avoinParams = {
          page: 0,
          size: 10,
          sort: 'muokkauspaiva,asc',
          avoin: true
        }
        this.vaiheet = (
          await (this.$isVirkailija()
            ? getTerveyskeskuskoulutusjaksotVirkailija(avoinParams)
            : getTerveyskeskuskoulutusjaksotVastuuhenkilo(avoinParams))
        ).data.content
      } catch (err) {
        toastFail(this, this.$t('terveyskeskuskoulutusjaksojen-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get rows() {
      return this.vaiheet?.length ?? 0
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
      border-bottom: none;
      margin-bottom: 0;

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

        .actions {
          text-align: left;
        }
      }
    }
  }
</style>
