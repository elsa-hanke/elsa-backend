<template>
  <div v-if="rows === 0">
    <b-alert class="mb-4 pt-3 pb-3" variant="light" show>
      <p class="d-flex mb-0">
        <font-awesome-icon
          icon="info-circle"
          fixed-width
          class="text-muted text-size-md mr-2 mt-1"
        />
        <span>
          {{ $t('ei-avoimia-asioita-erikoistuja-info.voit-esimerkiksi') }}
          <b-link :to="{ path: '/arvioinnit/arviointipyynto' }">
            {{ $t('ei-avoimia-asioita-erikoistuja-info.pyytaa-arviointeja') }}
          </b-link>
          {{ $t('ei-avoimia-asioita-erikoistuja-info.osaamisestasi') }},
          <b-link :to="{ path: '/suoritemerkinnat/uusi' }">
            {{ $t('ei-avoimia-asioita-erikoistuja-info.lisata-suoritemerkintoja') }}
          </b-link>
          <span>,</span>
          <b-link :to="{ name: 'koulutussuunnitelma' }">
            {{ $t('ei-avoimia-asioita-erikoistuja-info.paivittaa-koulutussuunnitelmaasi') }}
          </b-link>
          <span>,</span>
          <b-link :to="{ path: '/seurantakeskustelut/seurantajakso/uusi' }">
            {{ $t('ei-avoimia-asioita-erikoistuja-info.lisata-seurantajakson') }}
          </b-link>
          {{ $t('ei-avoimia-asioita-erikoistuja-info.tai-tehda') }}
          <b-link :to="{ path: '/paivittaiset-merkinnat/uusi' }">
            {{ $t('ei-avoimia-asioita-erikoistuja-info.uuden-paivittaisen-merkinnan') }}
          </b-link>
          <span>.</span>
          {{ $t('ei-avoimia-asioita-erikoistuja-info.muista-myos-lisata') }}
          <b-link :to="{ name: 'tyoskentelyjaksot' }">
            {{ $t('ei-avoimia-asioita-erikoistuja-info.tyoskentelyjaksot') }}
          </b-link>
          {{ $t('ei-avoimia-asioita-erikoistuja-info.ja') }}
          <b-link :to="{ name: 'teoriakoulutukset' }">
            {{ $t('ei-avoimia-asioita-erikoistuja-info.teoriakoulutukset') }}
          </b-link>
          <span>.</span>
        </span>
      </p>
    </b-alert>
  </div>
  <div v-else>
    <b-card-skeleton :header="$t('avoimet-asiat')" :loading="loading" class="mb-5">
      <b-table :items="avoimetAsiat" :fields="fields" stacked="md" responsive>
        <template #cell(asia)="row">
          <b-link
            :to="{
              name: getComponentName(row.item.tyyppi),
              path: getComponentPath(row.item.tyyppi, row.item.id),
              hash: getHash(row.item.tyyppi)
            }"
            class="task-type"
          >
            {{ row.item.asia }}
          </b-link>
        </template>
        <template #cell(pvm)="row">
          <span class="text-nowrap">
            {{ row.item.pvm ? $date(row.item.pvm) : '' }}
          </span>
        </template>
        <template #cell(actions)="row">
          <elsa-button
            variant="primary"
            class="pt-1 pb-1"
            :to="{
              name: getComponentName(row.item.tyyppi),
              path: getComponentPath(row.item.tyyppi, row.item.id),
              hash: getHash(row.item.tyyppi)
            }"
          >
            {{ $t('avaa') }}
          </elsa-button>
        </template>
      </b-table>
    </b-card-skeleton>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getAvoimetAsiat } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import { AvoinAsia } from '@/types'
  import { AvoinAsiaTyyppi } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class AvoimetAsiatCard extends Vue {
    avoimetAsiat: AvoinAsia[] | null = null
    loading = true

    async mounted() {
      try {
        this.avoimetAsiat = (await getAvoimetAsiat()).data
      } catch (err) {
        toastFail(this, this.$t('avoimien-asioiden-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get fields() {
      return [
        {
          key: 'asia',
          label: this.$t('asia'),
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

    getComponentName(tyyppi: AvoinAsiaTyyppi) {
      switch (tyyppi) {
        case AvoinAsiaTyyppi.KOULUTUSSOPIMUS:
          return 'koulutussopimus-erikoistuva'
        case AvoinAsiaTyyppi.ALOITUSKESKUSTELU:
          return 'koejakson-aloituskeskustelu'
        case AvoinAsiaTyyppi.VASTUUHENKILON_ARVIO:
          return 'koejakson-vastuuhenkilon-arvio'
        case AvoinAsiaTyyppi.VALMISTUMISPYYNTO:
          return 'valmistumispyynto'
        case AvoinAsiaTyyppi.KOULUTTAJAVALTUUTUS:
          return 'profiili'
        default:
          return null
      }
    }

    getComponentPath(tyyppi: AvoinAsiaTyyppi, id: number) {
      switch (tyyppi) {
        case AvoinAsiaTyyppi.SEURANTAJAKSO:
          return `/seurantakeskustelut/seurantajakso/${id}`
        case AvoinAsiaTyyppi.TERVEYSKESKUSKOULUTUSJAKSO:
          return '/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta'
        default:
          return ''
      }
    }

    getHash(tyyppi: AvoinAsiaTyyppi) {
      return tyyppi === AvoinAsiaTyyppi.KOULUTTAJAVALTUUTUS ? '#katseluoikeudet' : ''
    }

    get rows() {
      return this.avoimetAsiat?.length ?? 0
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep {
    .alert {
      border: 0 solid rgba(34, 34, 34, 0.125);
      border-radius: 0.25rem;
      span {
        margin-left: -0.23em;
      }
    }

    .alert-light {
      color: $black;
      background-color: $white;
    }

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
