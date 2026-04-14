<template>
  <b-card-skeleton :header="$t('yekvalmistumispyynnot')" :loading="loading" class="mb-4">
    <div v-if="rows > 0">
      <b-table fixed :items="valmistumispyynnot" :fields="fields" stacked="md">
        <template #cell(pvm)="data">
          <span class="text-nowrap">
            {{ data.item.tapahtumanAjankohta ? $date(data.item.tapahtumanAjankohta) : '' }}
          </span>
        </template>

        <template #cell(tyyppi)="data">
          {{ taskStatus(data.item.tila) }}
        </template>

        <template #cell(erikoistuvanNimi)="data">
          <b-link
            :to="{
              name: linkComponent(),
              params: { valmistumispyyntoId: data.item.id }
            }"
            class="task-type"
          >
            {{ data.item.erikoistujanNimi }}
          </b-link>
        </template>

        <template #cell(actions)="data">
          <elsa-button
            v-if="showButton(data.item.tila)"
            variant="primary"
            class="pt-1 pb-1"
            :to="{
              name: linkComponent(),
              params: { valmistumispyyntoId: data.item.id }
            }"
          >
            {{ $t(buttonText(data.item.tyyppi)) }}
          </elsa-button>
        </template>
      </b-table>
    </div>
    <div v-else>
      <b-alert variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
        {{ $t('ei-avoimia-valmistumispyyntoja') }}
      </b-alert>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getEtusivuKoulutettavienValmistumispyynnot as getEtusivuKoulutettavienValmistumispyynnotVastuuhenkilo } from '@/api/vastuuhenkilo'
  import { getEtusivuKoulutettavienValmistumispyynnot as getEtusivuKoulutettavienValmistumispyynnotVirkailija } from '@/api/virkailija'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import { ValmistumispyyntoListItem } from '@/types'
  import { ValmistumispyynnonTila } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class YekValmistumispyynnotCard extends Vue {
    valmistumispyynnot: ValmistumispyyntoListItem[] | null = null
    loading = true

    async mounted() {
      try {
        this.valmistumispyynnot = this.$isVastuuhenkilo()
          ? (await getEtusivuKoulutettavienValmistumispyynnotVastuuhenkilo()).data
          : (await getEtusivuKoulutettavienValmistumispyynnotVirkailija()).data
      } catch (err) {
        toastFail(this, this.$t('valmistumispyyntojen-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get fields() {
      return [
        {
          key: 'erikoistuvanNimi',
          label: this.$t('yek.koulutettava'),
          sortable: true
        },
        {
          key: 'pvm',
          label: this.$t('pvm'),
          sortable: true
        },
        {
          key: 'tyyppi',
          label: this.$t('vaihe'),
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
      return this.valmistumispyynnot?.length ?? 0
    }

    linkComponent() {
      if (this.$isVirkailija()) {
        return 'valmistumispyynnon-tarkistus-yek'
      } else {
        return 'valmistumispyynnon-hyvaksynta-yek'
      }
    }

    buttonText(tila: ValmistumispyynnonTila) {
      if (this.$isVirkailija()) {
        return 'tarkista'
      } else {
        if (tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA) {
          return 'tee-arviointi'
        } else {
          return 'hyvaksy'
        }
      }
    }

    taskStatus(status: string) {
      switch (status) {
        case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA:
          return this.$t('valmistumispyynnon-tila.avoimet-asiat-odottaa-osaamisen-arviointia')
        case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          return this.$t('valmistumispyynnon-tila.avoimet-asiat-odottaa-hyvaksyntaa')
        case ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA:
          return this.$t('valmistumispyynnon-tila.avoimet-asiat-odottaa-virkailijan-tarkastusta')
        case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN:
          return this.$t('valmistumispyynnon-tila.avoimet-asiat-virkailijan-tarkastus-kesken')
      }
    }
    showButton(tila: ValmistumispyynnonTila) {
      if (this.$isVirkailija())
        return (
          tila === ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA ||
          tila === ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN
        )
      else
        return (
          tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA ||
          tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
        )
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
