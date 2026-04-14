<template>
  <div class="teoriakoulutukset">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('teoriakoulutukset') }}</h1>
          <p>{{ $t('yek.teoriakoulutukset-ingressi') }}</p>
          <p>
            {{ $t('yek.teoriakoulutus-ei-nay') }}
            <a
              href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/yhteystiedot"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ $t('sahkopostitse') }}.
            </a>
          </p>
          <div v-if="!loading">
            <div v-if="teoriakoulutuksetFormatted.length > 0" class="teoriakoulutukset-table">
              <b-table :items="teoriakoulutuksetFormatted" :fields="fields" stacked="md" responsive>
                <template #cell(merkinta)="data">
                  <span v-if="data.value" class="text-success">
                    <font-awesome-icon :icon="['fas', 'check-circle']" />
                    {{ $t('yek.suoritettu') }}
                  </span>
                  <span v-else>{{ $t('yek.hylatty') }}</span>
                </template>
              </b-table>
            </div>
            <div v-else>
              <b-alert variant="dark" show>
                <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                <span>
                  {{ $t('yek.ei-teoriakoulutuksia') }}
                </span>
              </b-alert>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getYekTeoriakoulutukset } from '@/api/yek-koulutettava'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaProgressBar from '@/components/progress-bar/progress-bar.vue'
  import ElsaVanhaAsetusVaroitus from '@/components/vanha-asetus-varoitus/vanha-asetus-varoitus.vue'
  import store from '@/store'
  import { Opintosuoritus } from '@/types'
  import { sortByDateDesc } from '@/utils/date'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaProgressBar,
      ElsaVanhaAsetusVaroitus
    }
  })
  export default class YekTeoriakoulutukset extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('teoriakoulutukset'),
        active: true
      }
    ]
    loading = true
    fields = [
      {
        key: 'nimi',
        label: this.$t('yek.koulutus'),
        sortable: true
      },
      {
        key: 'pvm',
        label: this.$t('yek.suorituspvm'),
        sortable: true
      },
      {
        key: 'merkinta',
        label: this.$t('yek.merkinta'),
        sortable: false
      }
    ]
    opintosuoritukset: Opintosuoritus[] = []

    async mounted() {
      await this.fetchTeoriakoulutukset()
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async fetchTeoriakoulutukset() {
      try {
        this.opintosuoritukset = (await getYekTeoriakoulutukset()).data
      } catch (err) {
        toastFail(this, this.$t('teoriakoulutuksien-hakeminen-epaonnistui'))
      }
    }

    get teoriakoulutuksetFormatted() {
      return this.opintosuoritukset
        .sort((a, b) => sortByDateDesc(a.suorituspaiva, b.suorituspaiva))
        .map((opintosuoritus) => ({
          ...opintosuoritus,
          nimi: `${opintosuoritus.nimi_fi}`,
          pvm: this.$date(opintosuoritus.suorituspaiva),
          merkinta: opintosuoritus.hyvaksytty
        }))
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .teoriakoulutukset {
    max-width: 1024px;
  }

  @include media-breakpoint-down(sm) {
    .teoriakoulutukset-table {
      ::v-deep table {
        border-bottom: 0;

        tr {
          border: $table-border-width solid $table-border-color;
          border-radius: $border-radius;
          margin-top: 0.5rem;
          padding-top: $table-cell-padding;
          padding-bottom: $table-cell-padding;
        }

        td {
          border: none;
          padding: 0 0.5rem;

          & > div {
            width: 100% !important;
            padding: 0 0 0.5rem 0 !important;
          }

          &::before {
            text-align: left !important;
            font-weight: 500 !important;
            width: 100% !important;
            padding-right: 0 !important;
          }
          &:last-child > div {
            padding-bottom: 0 !important;
          }

          &.last > div {
            padding-bottom: 0 !important;
          }
        }
      }
    }
  }
</style>
