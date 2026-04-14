<template>
  <div class="koulutussuunnitelma">
    <b-breadcrumb :items="items" class="mb-0 d-print-none" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <div v-if="!loading && koulutussuunnitelma" class="mb-4">
            <h1 class="d-print-none">{{ $t('koulutussuunnitelma') }}</h1>
            <p class="d-print-none">{{ $t('koulutussuunnitelma-kuvaus') }}</p>
            <hr class="d-print-none" />
            <section class="mb-5 d-print-none">
              <h2>{{ $t('koulutusjaksot') }}</h2>
              <p>{{ $t('koulutusjaksot-kuvaus') }}</p>
              <elsa-button
                v-if="!account.impersonated"
                variant="primary"
                :to="{ name: 'uusi-koulutusjakso' }"
                class="mb-3"
              >
                {{ $t('lisaa-koulutusjakso') }}
              </elsa-button>
              <div v-if="koulutusjaksot && koulutusjaksot.length > 0" class="koulutusjaksot-table">
                <b-table-simple responsive stacked="md">
                  <b-thead>
                    <b-tr>
                      <b-th style="width: 25%">{{ $t('koulutusjakso') }}</b-th>
                      <b-th style="width: 35%">{{ $t('tyoskentelyjaksot') }}</b-th>
                      <b-th>{{ $t('osaamistavoitteet-omalta-erikoisalalta') }}</b-th>
                    </b-tr>
                  </b-thead>
                  <b-tbody>
                    <b-tr v-for="koulutusjakso in koulutusjaksot" :key="koulutusjakso.id">
                      <b-td>
                        <elsa-button
                          :to="{
                            name: 'koulutusjakso',
                            params: { koulutusjaksoId: koulutusjakso.id }
                          }"
                          variant="link"
                          class="shadow-none p-0 border-0 text-left"
                        >
                          {{ koulutusjakso.nimi }}
                        </elsa-button>
                      </b-td>
                      <b-td :stacked-heading="$t('tyoskentelyjaksot')">
                        <div
                          v-for="tyoskentelyjakso in koulutusjakso.tyoskentelyjaksot"
                          :key="tyoskentelyjakso.id"
                        >
                          {{ tyoskentelyjakso.tyoskentelypaikka.nimi }} ({{
                            tyoskentelyjakso.alkamispaiva
                              ? $date(tyoskentelyjakso.alkamispaiva)
                              : ''
                          }}
                          –
                          <span :class="tyoskentelyjakso.paattymispaiva ? '' : 'text-lowercase'">
                            {{
                              tyoskentelyjakso.paattymispaiva
                                ? $date(tyoskentelyjakso.paattymispaiva)
                                : $t('kesken')
                            }})
                          </span>
                        </div>
                      </b-td>
                      <b-td :stacked-heading="$t('osaamistavoitteet-omalta-erikoisalalta')">
                        <b-badge
                          v-for="osaamistavoite in koulutusjakso.osaamistavoitteet"
                          :key="osaamistavoite.id"
                          pill
                          variant="light"
                          class="font-weight-400 mr-2"
                        >
                          {{ osaamistavoite.nimi }}
                        </b-badge>
                      </b-td>
                    </b-tr>
                  </b-tbody>
                </b-table-simple>
              </div>
              <hr v-else />
            </section>
            <section>
              <h2>{{ $t('henkilokohtainen-koulutussuunnitelma') }}</h2>
              <!-- eslint-disable-next-line vue/no-v-html -->
              <p v-html="$t('henkilokohtainen-koulutussuunnitelma-kuvaus', { linkki })" />
              <div class="d-flex flex-wrap justify-content-between d-print-none">
                <elsa-button
                  v-if="!account.impersonated"
                  variant="primary"
                  :to="{ name: 'muokkaa-koulutussuunnitelma' }"
                  class="mr-2 mb-3"
                >
                  {{ $t('muokkaa-tietoja') }}
                </elsa-button>
                <elsa-button
                  variant="link"
                  class="text-decoration-none mb-3"
                  @click="onPrintSuunnitelma"
                >
                  <font-awesome-icon icon="print" fixed-width class="mr-1" />
                  {{ $t('tulosta-suunnitelma') }}
                </elsa-button>
              </div>
              <b-card
                v-if="koulutussuunnitelma.koulutussuunnitelmaAsiakirja"
                no-body
                class="border mb-3"
              >
                <b-card-text class="p-2">
                  <asiakirjat-content
                    v-if="koulutussuunnitelma.koulutussuunnitelmaAsiakirja"
                    class="asiakirjat-table asiakirjat-table--border-xs-0"
                    :asiakirjat="koulutussuunnitelmaAsiakirjatTableItems"
                    :sorting-enabled="false"
                    :pagination-enabled="false"
                    :enable-search="false"
                    :enable-delete="false"
                    :show-info-if-empty="false"
                  />
                </b-card-text>
              </b-card>
              <elsa-accordian ref="motivaatiokirjeAccordian" icon="envelope-open-text">
                <template #title>
                  {{ $t('motivaatiokirje') }}
                  <span
                    v-if="koulutussuunnitelma.motivaatiokirjeYksityinen"
                    class="text-size-sm font-weight-400"
                  >
                    <span class="text-lowercase">({{ $t('yksityinen') }})</span>
                  </span>
                </template>
                <asiakirjat-content
                  v-if="koulutussuunnitelma.motivaatiokirjeAsiakirja"
                  class="asiakirjat-table"
                  :asiakirjat="motivaatiokirjeAsiakirjatTableItems"
                  :sorting-enabled="false"
                  :pagination-enabled="false"
                  :enable-search="false"
                  :enable-delete="false"
                  :show-info-if-empty="false"
                />
                <div v-if="koulutussuunnitelma.motivaatiokirje" class="text-preline">
                  {{ koulutussuunnitelma.motivaatiokirje }}
                </div>
              </elsa-accordian>
              <elsa-accordian ref="opiskeluJaTyohistoriaYksityinenAccordian" icon="toolbox">
                <template #title>
                  {{ $t('opiskelu-ja-tyohistoria') }}
                  <span
                    v-if="koulutussuunnitelma.opiskeluJaTyohistoriaYksityinen"
                    class="text-size-sm font-weight-400"
                  >
                    <span class="text-lowercase">({{ $t('yksityinen') }})</span>
                  </span>
                </template>
                <div v-if="koulutussuunnitelma.opiskeluJaTyohistoria" class="text-preline">
                  {{ koulutussuunnitelma.opiskeluJaTyohistoria }}
                </div>
              </elsa-accordian>
              <elsa-accordian ref="vahvuudetYksityinenAccordian" icon="dumbbell">
                <template #title>
                  {{ $t('vahvuudet') }}
                  <span
                    v-if="koulutussuunnitelma.vahvuudetYksityinen"
                    class="text-size-sm font-weight-400"
                  >
                    <span class="text-lowercase">({{ $t('yksityinen') }})</span>
                  </span>
                </template>
                <div v-if="koulutussuunnitelma.vahvuudet" class="text-preline">
                  {{ koulutussuunnitelma.vahvuudet }}
                </div>
              </elsa-accordian>
              <elsa-accordian
                ref="tulevaisuudenVisiointiYksityinenAccordian"
                :icon="['far', 'eye']"
              >
                <template #title>
                  {{ $t('tulevaisuuden-visiointi') }}
                  <span
                    v-if="koulutussuunnitelma.tulevaisuudenVisiointiYksityinen"
                    class="text-size-sm font-weight-400"
                  >
                    <span class="text-lowercase">({{ $t('yksityinen') }})</span>
                  </span>
                </template>
                <div v-if="koulutussuunnitelma.tulevaisuudenVisiointi" class="text-preline">
                  {{ koulutussuunnitelma.tulevaisuudenVisiointi }}
                </div>
              </elsa-accordian>
              <elsa-accordian ref="osaamisenKartuttaminenYksityinenAccordian" icon="chart-line">
                <template #title>
                  {{ $t('osaamisen-kartuttaminen') }}
                  <span
                    v-if="koulutussuunnitelma.osaamisenKartuttaminenYksityinen"
                    class="text-size-sm font-weight-400"
                  >
                    <span class="text-lowercase">({{ $t('yksityinen') }})</span>
                  </span>
                </template>
                <div v-if="koulutussuunnitelma.osaamisenKartuttaminen" class="text-preline">
                  {{ koulutussuunnitelma.osaamisenKartuttaminen }}
                </div>
              </elsa-accordian>
              <elsa-accordian ref="elamankenttaYksityinenAccordian" icon="theater-masks">
                <template #title>
                  {{ $t('elamankentta') }}
                  <span
                    v-if="koulutussuunnitelma.elamankenttaYksityinen"
                    class="text-size-sm font-weight-400"
                  >
                    <span class="text-lowercase">({{ $t('yksityinen') }})</span>
                  </span>
                </template>
                <div v-if="koulutussuunnitelma.elamankentta" class="text-preline">
                  {{ koulutussuunnitelma.elamankentta }}
                </div>
              </elsa-accordian>
            </section>
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
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { getKoulutusjaksot } from '@/api/erikoistuva'
  import ElsaAccordian from '@/components/accordian/accordian.vue'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import ElsaButton from '@/components/button/button.vue'
  import store from '@/store'
  import { Koulutusjakso, Koulutussuunnitelma } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaAccordian,
      ElsaButton,
      AsiakirjatContent
    }
  })
  export default class KoulutussuunnitelmaView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koulutussuunnitelma'),
        active: true
      }
    ]

    koulutussuunnitelma: Koulutussuunnitelma | null = null
    koulutusjaksot: Koulutusjakso[] | null = null
    loading = true

    async mounted() {
      await Promise.all([this.fetchKoulutusjaksot(), this.fetchKoulutussuunnitelma()])
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async fetchKoulutussuunnitelma() {
      try {
        this.koulutussuunnitelma = (await axios.get(`erikoistuva-laakari/koulutussuunnitelma`)).data
      } catch {
        toastFail(this, this.$t('koulutussuunnitelman-hakeminen-epaonnistui'))
      }
    }

    async fetchKoulutusjaksot() {
      try {
        this.koulutusjaksot = (await getKoulutusjaksot()).data
      } catch {
        toastFail(this, this.$t('koulutusjaksojen-hakeminen-epaonnistui'))
      }
    }

    onPrintSuunnitelma() {
      ;(this.$refs.motivaatiokirjeAccordian as ElsaAccordian).open()
      ;(this.$refs.opiskeluJaTyohistoriaYksityinenAccordian as ElsaAccordian).open()
      ;(this.$refs.vahvuudetYksityinenAccordian as ElsaAccordian).open()
      ;(this.$refs.tulevaisuudenVisiointiYksityinenAccordian as ElsaAccordian).open()
      ;(this.$refs.osaamisenKartuttaminenYksityinenAccordian as ElsaAccordian).open()
      ;(this.$refs.elamankenttaYksityinenAccordian as ElsaAccordian).open()
      setTimeout(() => window.print(), 500) // Varmistetaan, että sisältöjen avaamisanimaatiot ovat suoritettu.
    }

    get linkki() {
      return `<a
                href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/opinto-oppaat/"
                target="_blank"
                rel="noopener noreferrer"
              >${this.$t('henkilokohtainen-koulutussuunnitelma-linkki')}</a>`
    }

    get koulutussuunnitelmaAsiakirjatTableItems() {
      return this.koulutussuunnitelma?.koulutussuunnitelmaAsiakirja
        ? [this.koulutussuunnitelma.koulutussuunnitelmaAsiakirja]
        : []
    }

    get motivaatiokirjeAsiakirjatTableItems() {
      return this.koulutussuunnitelma?.motivaatiokirjeAsiakirja
        ? [this.koulutussuunnitelma.motivaatiokirjeAsiakirja]
        : []
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .koulutussuunnitelma {
    max-width: 1024px;
  }

  .koulutussuunnitelma-table,
  .motivaatiokirje-table,
  .asiakirjat-table {
    ::v-deep table {
      border-bottom: 0;
    }
  }

  ::v-deep .table-responsive.mb-0 {
    margin-bottom: 0;
  }

  @include media-breakpoint-down(sm) {
    .koulutusjaksot-table {
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
          padding: 0 $table-cell-padding;

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
        }
      }
    }
    .asiakirjat-table--border-xs-0 {
      ::v-deep tr {
        border: 0;
      }
    }
  }
</style>
