<template>
  <b-card-skeleton :header="$t('koejaksot')" :loading="loading" class="mb-4">
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
              name: linkComponent(data.item.tyyppi),
              params: { id: data.item.id }
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
              name: linkComponent(data.item.tyyppi),
              params: { id: data.item.id }
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
        {{ $t('ei-avoimia-koejakson-vaiheita') }}
      </b-alert>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import { getEtusivuKoejaksot as getEtusivuKoejaksotKouluttaja } from '@/api/kouluttaja'
  import { getEtusivuKoejaksot as getEtusivuKoejaksotVastuuhenkilo } from '@/api/vastuuhenkilo'
  import { getEtusivuKoejaksot as getEtusivuKoejaksotVirkailija } from '@/api/virkailija'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import { KoejaksonVaihe } from '@/types'
  import { LomakeTyypit } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class KoejaksotCard extends Vue {
    vaiheet: KoejaksonVaihe[] | null = null

    @Prop({ required: false, default: true })
    showVaihe!: boolean
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
          key: 'tyyppi',
          label: this.$t('vaihe'),
          sortable: true,
          thClass: this.showVaihe ? '' : 'd-none',
          tdClass: this.showVaihe ? '' : 'd-none'
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

    private componentLinks = new Map([
      [LomakeTyypit.KOULUTUSSOPIMUS, 'koulutussopimus'],
      [LomakeTyypit.ALOITUSKESKUSTELU, 'aloituskeskustelu-kouluttaja'],
      [LomakeTyypit.VALIARVIOINTI, 'valiarviointi-kouluttaja'],
      [LomakeTyypit.KEHITTAMISTOIMENPITEET, 'kehittamistoimenpiteet-kouluttaja'],
      [LomakeTyypit.LOPPUKESKUSTELU, 'loppukeskustelu-kouluttaja'],
      [LomakeTyypit.VASTUUHENKILON_ARVIO, 'vastuuhenkilon-arvio-vastuuhenkilo']
    ])

    async mounted() {
      try {
        if (this.$isVastuuhenkilo()) {
          this.vaiheet = (await getEtusivuKoejaksotVastuuhenkilo()).data
        } else if (this.$isVirkailija()) {
          this.vaiheet = (await getEtusivuKoejaksotVirkailija()).data
        } else {
          this.vaiheet = (await getEtusivuKoejaksotKouluttaja()).data
        }
      } catch (err) {
        toastFail(this, this.$t('koejaksojen-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get rows() {
      return this.vaiheet?.length ?? 0
    }

    linkComponent(type: LomakeTyypit) {
      if (this.$isVirkailija() && type === LomakeTyypit.VASTUUHENKILON_ARVIO) {
        return 'virkailijan-tarkistus'
      }
      return this.componentLinks.get(type)
    }

    buttonText(type: LomakeTyypit) {
      switch (type) {
        case LomakeTyypit.KOULUTUSSOPIMUS:
          return 'hyvaksy'
        case LomakeTyypit.ALOITUSKESKUSTELU:
          return 'hyvaksy'
        case LomakeTyypit.VALIARVIOINTI:
          return 'tee-arviointi'
        case LomakeTyypit.KEHITTAMISTOIMENPITEET:
          return 'tee-arviointi'
        case LomakeTyypit.LOPPUKESKUSTELU:
          return 'tee-arviointi'
        case LomakeTyypit.VASTUUHENKILON_ARVIO:
          return 'tee-arviointi'
      }
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
