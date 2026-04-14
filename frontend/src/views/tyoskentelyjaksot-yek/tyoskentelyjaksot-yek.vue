<template>
  <div class="tyoskentelyjaksot">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('tyoskentelyjaksot') }}</h1>
          <p>
            {{ $t('yek.tyoskentelyjaksot-kuvaus') }}
            <a
              href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/opinto-oppaat/"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ $t('yek.tyoskentelyjaksot-kuvaus-opinto-oppaasta') }}
            </a>
            {{ $t('yek.tyoskentelyjaksot-kuvaus-yli-kymmenen-vuotta-vanhoja') }}
          </p>
          <elsa-vanha-asetus-varoitus />
          <div v-if="muokkausoikeudet" class="d-flex flex-wrap mb-3 mb-lg-4">
            <elsa-button
              variant="primary"
              :to="{ name: 'uusi-yek-tyoskentelyjakso' }"
              class="mb-2 mr-2"
            >
              {{ $t('lisaa-tyoskentelyjakso') }}
            </elsa-button>
            <elsa-button
              variant="outline-primary"
              :to="{ name: 'uusi-yek-poissaolo' }"
              class="mb-2"
            >
              {{ $t('lisaa-poissaolo') }}
            </elsa-button>
          </div>
          <div v-if="!loading && tyoskentelyjaksotTaulukko != null && tilastot != null">
            <b-alert
              v-if="
                terveyskeskuskoulutusjaksoPalautettuKorjattavaksi ||
                terveyskeskuskoulutusjaksoLahetetty ||
                terveyskeskuskoulutusjaksoUusi
              "
              variant="dark"
              show
            >
              <h5>{{ $t('terveyskeskuskoulutusjakson-hyvaksynta') }}</h5>
              <div v-if="terveyskeskuskoulutusjaksoPalautettuKorjattavaksi">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon
                      :icon="['fas', 'exclamation-circle']"
                      class="mr-2 text-danger"
                    />
                  </em>
                  <div>
                    {{ $t('terveyskeskuskoulutusjakso-on-palautettu-muokattavaksi') }}
                    <span class="d-block">
                      {{ $t('syy') }}&nbsp;
                      <span>
                        {{ tyoskentelyjaksotTaulukko.terveyskeskuskoulutusjaksonKorjausehdotus }}
                      </span>
                    </span>
                  </div>
                </div>
                <elsa-button
                  :to="{
                    name: 'terveyskeskuskoulutusjakson-hyvaksyntapyynto-yek'
                  }"
                  variant="primary"
                  class="mt-2"
                >
                  {{ $t('pyyda-hyvaksyntaa') }}
                </elsa-button>
              </div>
              <div v-if="terveyskeskuskoulutusjaksoUusi">
                <div class="d-flex flex-row">
                  <div>
                    {{ $t('terveyskeskuskoulutusjakso-on-hyvaksyttavissa') }}
                  </div>
                </div>
                <elsa-button
                  :to="{
                    name: 'terveyskeskuskoulutusjakson-hyvaksyntapyynto-yek'
                  }"
                  variant="primary"
                  class="mt-3"
                >
                  {{ $t('pyyda-hyvaksyntaa') }}
                </elsa-button>
              </div>
              <span v-if="terveyskeskuskoulutusjaksoLahetetty">
                {{ $t('terveyskeskuskoulutusjakso-on-lahetetty-hyvaksyttavaksi') }}
              </span>
            </b-alert>
            <div class="d-flex justify-content-center border rounded pt-3 pb-2 mb-4">
              <div class="container-fluid">
                <elsa-form-group :label="$t('tyoskentelyaika')">
                  <template #default="{ uid }">
                    <div :id="uid" class="d-flex flex-wrap">
                      <div class="d-flex flex-column mb-2 duration-card">
                        <span class="duration-text">
                          {{ $duration(tilastot.tyoskentelyaikaYhteensa) }}
                        </span>
                        <span class="text-size-sm">{{ $t('tyoskentelyaika-yhteensa') }}</span>
                      </div>
                      <div class="d-flex flex-column mb-2 duration-card">
                        <span class="duration-text">
                          {{ $duration(tilastot.arvioErikoistumiseenHyvaksyttavista) }}
                        </span>
                        <span class="text-size-sm">
                          {{ $t('yek.arvio-hyvaksyttavasta-koulutuksesta') }}
                        </span>
                      </div>
                      <div class="d-flex flex-column mb-2 duration-card">
                        <span class="duration-text">
                          {{ $duration(tilastot.arvioPuuttuvastaKoulutuksesta) }}
                        </span>
                        <span class="text-size-sm">
                          {{ $t('arvio-puuttuvasta-koulutuksesta') }}
                        </span>
                      </div>
                    </div>
                  </template>
                </elsa-form-group>
                <b-row>
                  <elsa-form-group :label="$t('koulutustyypit')" class="col-xl-12 mb-0">
                    <tyoskentelyjaksot-yek-bar-chart :tilastot="tilastot" />
                  </elsa-form-group>
                </b-row>
                <elsa-form-group
                  v-if="tyoskentelyjaksotTaulukko.terveyskeskuskoulutusjaksonHyvaksymispvm != null"
                  :label="$t('terveyskeskuskoulutusjakso')"
                >
                  <template #default="{ uid }">
                    <div :id="uid" class="d-flex flex-wrap">
                      <em class="align-middle">
                        <font-awesome-icon
                          :icon="['fas', 'check-circle']"
                          class="text-success mr-2"
                        />
                      </em>
                      <div>
                        {{
                          $t('terveyskeskuskoulutusjakso-on-hyvaksytty-pvm', {
                            pvm: $date(
                              tyoskentelyjaksotTaulukko.terveyskeskuskoulutusjaksonHyvaksymispvm
                            )
                          })
                        }}
                      </div>
                    </div>
                  </template>
                </elsa-form-group>
                <b-row class="col-xl-12 mb-0 mt-2">
                  <span>
                    <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                    {{ $t('yek.tyoskentelyjaksot-muut-koulutukset-ohje') }}
                  </span>
                </b-row>
                <b-row
                  v-if="edistyminen && edistyminen.laakarikoulutusSuoritettuSuomiTaiBelgia"
                  class="col-xl-12 mb-0 mt-2"
                >
                  <span>
                    <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                    {{ $t('yek.aiempi-hyvaksiluettu-suoritus') }}
                  </span>
                </b-row>
              </div>
            </div>
            <div class="tyoskentelyjaksot-table">
              <b-table :items="tyoskentelyjaksotFormatted" :fields="fields" stacked="md" responsive>
                <template #table-colgroup="scope">
                  <col
                    v-for="field in scope.fields"
                    :key="field.key"
                    :style="{ width: field.width }"
                  />
                </template>
                <template #cell(tyoskentelypaikkaLabel)="row">
                  <elsa-button
                    :to="{
                      name: 'yektyoskentelyjakso',
                      params: { tyoskentelyjaksoId: row.item.id }
                    }"
                    variant="link"
                    class="shadow-none px-0"
                  >
                    {{ row.item.tyoskentelypaikka.nimi }}
                  </elsa-button>
                </template>
                <template #cell(ajankohtaDate)="row">
                  {{ row.item.ajankohta }}
                </template>
                <template #cell(keskeytyksetLength)="row">
                  <elsa-button
                    v-if="row.item.keskeytyksetLength > 0"
                    variant="link"
                    class="shadow-none text-nowrap px-0"
                    @click="row.toggleDetails"
                  >
                    {{ row.item.keskeytyksetLength }}
                    <span class="text-lowercase">
                      {{ row.item.keskeytyksetLength == 1 ? $t('poissaolo') : $t('poissaoloa') }}
                    </span>
                    <font-awesome-icon
                      :icon="row.detailsShowing ? 'chevron-up' : 'chevron-down'"
                      fixed-width
                      size="lg"
                      class="ml-2 text-dark"
                    />
                  </elsa-button>
                  <span v-else>
                    {{ $t('ei-poissaoloja') }}
                  </span>
                </template>
                <template #row-details="row">
                  <div class="px-md-3">
                    <b-table-simple stacked="md">
                      <b-thead>
                        <b-tr>
                          <b-th style="width: 60%">
                            {{ $t('poissaolon-syy') }}
                            <elsa-popover :title="$t('poissaolon-syy')">
                              <elsa-poissaolon-syyt />
                            </elsa-popover>
                          </b-th>
                          <b-th style="width: 25%">{{ $t('ajankohta') }}</b-th>
                          <b-th style="width: 15%">{{ $t('poissaolo') }}</b-th>
                        </b-tr>
                      </b-thead>
                      <b-tbody>
                        <b-tr v-for="(keskeytysaika, index) in row.item.keskeytykset" :key="index">
                          <b-td :stacked-heading="$t('poissaolon-syy')">
                            <elsa-button
                              :to="{
                                name: 'yekpoissaolo',
                                params: { poissaoloId: keskeytysaika.id }
                              }"
                              variant="link"
                              class="shadow-none px-0"
                            >
                              {{ keskeytysaika.poissaolonSyy.nimi }}
                            </elsa-button>
                          </b-td>
                          <b-td :stacked-heading="$t('ajankohta')">
                            {{
                              keskeytysaika.alkamispaiva != keskeytysaika.paattymispaiva
                                ? `${$date(keskeytysaika.alkamispaiva)}-${$date(
                                    keskeytysaika.paattymispaiva
                                  )}`
                                : $date(keskeytysaika.alkamispaiva)
                            }}
                          </b-td>
                          <b-td :stacked-heading="$t('tyoaika')">
                            <span>{{ keskeytysaika.poissaoloprosentti }} %</span>
                          </b-td>
                        </b-tr>
                      </b-tbody>
                    </b-table-simple>
                  </div>
                </template>
              </b-table>
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
  import axios from 'axios'
  import { parseISO } from 'date-fns'
  import { Component, Vue } from 'vue-property-decorator'

  import { getErikoistumisenEdistyminen } from '@/api/yek-koulutettava'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPoissaolonSyyt from '@/components/poissaolon-syyt/poissaolon-syyt.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import TyoskentelyjaksotBarChart from '@/components/tyoskentelyjaksot-bar-chart.vue'
  import ElsaVanhaAsetusVaroitus from '@/components/vanha-asetus-varoitus/vanha-asetus-varoitus.vue'
  import TyoskentelyjaksotYekBarChart from '@/components/yek/tyoskentelyjaksot-yek-bar-chart.vue'
  import store from '@/store'
  import {
    ErikoistumisenEdistyminen,
    Keskeytysaika,
    TyoskentelyjaksotTable,
    TyoskentelyjaksotTilastotKaytannonKoulutus,
    TyoskentelyjaksotTilastotTyoskentelyjaksot
  } from '@/types'
  import { KaytannonKoulutusTyyppi, TerveyskeskuskoulutusjaksonTila } from '@/utils/constants'
  import { sortByDateDesc } from '@/utils/date'
  import { ELSA_ROLE } from '@/utils/roles'
  import { toastFail } from '@/utils/toast'
  import { ajankohtaLabel, tyoskentelyjaksoKaytannonKoulutusLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      TyoskentelyjaksotYekBarChart,
      ElsaButton,
      ElsaFormGroup,
      ElsaPopover,
      TyoskentelyjaksotBarChart,
      ElsaPoissaolonSyyt,
      ElsaVanhaAsetusVaroitus
    }
  })
  export default class TyoskentelyjaksotYek extends Vue {
    edistyminen: ErikoistumisenEdistyminen | null = null

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('tyoskentelyjaksot'),
        active: true
      }
    ]
    tyoskentelyjaksotTaulukko: TyoskentelyjaksotTable | null = null
    fields = [
      {
        key: 'tyoskentelypaikkaLabel',
        label: this.$t('tyoskentelypaikka'),
        sortable: true
      },
      {
        key: 'ajankohtaDate',
        label: this.$t('ajankohta'),
        sortable: true
      },
      {
        key: 'tyoskentelyaikaLabel',
        label: this.$t('tyoskentelyaika'),
        sortable: true
      },
      {
        key: 'osaaikaprosenttiLabel',
        label: this.$t('tyoaika'),
        sortable: true
      },
      {
        key: 'keskeytyksetLength',
        label: this.$t('poissaolot'),
        sortable: true
      }
    ]
    loading = true

    async mounted() {
      await this.fetchTyoskentelyjaksot()
      this.edistyminen = (await getErikoistumisenEdistyminen()).data
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async fetchTyoskentelyjaksot() {
      try {
        this.tyoskentelyjaksotTaulukko = (
          await axios.get(`yek-koulutettava/tyoskentelyjaksot-taulukko`)
        ).data
      } catch {
        toastFail(this, this.$t('tyoskentelyjaksojen-hakeminen-epaonnistui'))
      }
    }

    get tyoskentelyjaksot() {
      if (this.tyoskentelyjaksotTaulukko) {
        return this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot
      } else {
        return []
      }
    }

    get keskeytykset() {
      if (this.tyoskentelyjaksotTaulukko) {
        return this.tyoskentelyjaksotTaulukko.keskeytykset
      } else {
        return []
      }
    }

    get tilastot() {
      if (this.tyoskentelyjaksotTaulukko) {
        return this.tyoskentelyjaksotTaulukko.tilastot
      } else {
        return undefined
      }
    }

    get tilastotKaytannonKoulutus() {
      if (this.tilastot) {
        return this.tilastot.kaytannonKoulutus
      } else {
        return []
      }
    }

    get tilastotTyoskentelyjaksot() {
      if (this.tilastot) {
        return this.tilastot.tyoskentelyjaksot
      } else {
        return []
      }
    }

    get tilastotKaytannonKoulutusSorted() {
      return [
        this.tilastotKaytannonKoulutus.find(
          (kk) => kk.kaytannonKoulutus === KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS
        ),
        this.tilastotKaytannonKoulutus.find(
          (kk) => kk.kaytannonKoulutus === KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        ),
        this.tilastotKaytannonKoulutus.find(
          (kk) => kk.kaytannonKoulutus === KaytannonKoulutusTyyppi.TUTKIMUSTYO
        ),
        this.tilastotKaytannonKoulutus.find(
          (kk) => kk.kaytannonKoulutus === KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        )
      ].filter((kk) => kk !== null) as TyoskentelyjaksotTilastotKaytannonKoulutus[]
    }

    get donutSeries() {
      return this.tilastotKaytannonKoulutusSorted.map((kk) => kk.suoritettu)
    }

    get donutOptions() {
      this.tilastotKaytannonKoulutus.map(
        (kk) =>
          `${tyoskentelyjaksoKaytannonKoulutusLabel(this, kk.kaytannonKoulutus)}: ${this.$duration(
            kk.suoritettu
          )}`
      )

      return {
        colors: ['#4EBDEF', '#FF8B06', '#808080', '#FFB406'],
        labels: [
          `${this.$t('oma-erikoisala')}: ${this.$duration(
            this.tilastotKaytannonKoulutusSorted[0].suoritettu
          )}`,
          `${this.$t('omaa-erikoisalaa-tukeva')}: ${this.$duration(
            this.tilastotKaytannonKoulutusSorted[1].suoritettu
          )}`,
          `${this.$t('tutkimustyo')}: ${this.$duration(
            this.tilastotKaytannonKoulutusSorted[2].suoritettu
          )}`,
          `${this.$t('pakollinen-terveyskeskuskoulutusjakso-lyh')}: ${this.$duration(
            this.tilastotKaytannonKoulutusSorted[3].suoritettu
          )}`
        ],
        legend: {
          fontSize: '13px',
          fontFamily: 'Montserrat, Helvetica, Arial, sans-serif',
          onItemClick: {
            toggleDataSeries: false
          },
          onItemHover: {
            highlightDataSeries: false
          },
          offsetY: '1px'
        },
        chart: {
          type: 'donut',
          animations: {
            enabled: false
          }
        },
        dataLabels: {
          formatter: function (val: number) {
            return Math.round(val) + '%'
          },
          style: {
            fontSize: '8px',
            fontFamily: 'Montserrat, Helvetica, Arial, sans-serif'
          },
          dropShadow: {
            enabled: false
          }
        },
        plotOptions: {
          pie: {
            expandOnClick: false
          }
        },
        stroke: {
          show: false
        },
        states: {
          hover: {
            filter: {
              type: 'normal'
            }
          },
          active: {
            filter: {
              type: 'normal'
            }
          }
        },
        tooltip: {
          enabled: false
        },
        responsive: [
          {
            breakpoint: 768,
            options: {
              legend: {
                position: 'bottom',
                offsetY: 0
              }
            }
          }
        ]
      }
    }

    get opintooppaastaLinkki() {
      return `<a href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/opinto-oppaat/" target="_blank" rel="noopener noreferrer">${(
        this.$t('tarkemmat-vaatimukset-opinto-oppaasta') as string
      ).toLowerCase()}</a>`
    }

    get tyoskentelyjaksotFormatted() {
      const keskeytyksetGroupByTyoskentelyjakso = this.keskeytykset.reduce(
        (result: { [key: number]: Partial<Keskeytysaika>[] }, keskeytysaika: Keskeytysaika) => {
          const tyoskentelyjaksoId = keskeytysaika?.tyoskentelyjakso?.id
          if (tyoskentelyjaksoId) {
            if (tyoskentelyjaksoId in result) {
              result[tyoskentelyjaksoId].push({
                id: keskeytysaika.id,
                alkamispaiva: keskeytysaika.alkamispaiva,
                paattymispaiva: keskeytysaika.paattymispaiva,
                poissaoloprosentti: keskeytysaika.poissaoloprosentti,
                poissaolonSyy: keskeytysaika.poissaolonSyy
              })
            } else {
              result[tyoskentelyjaksoId] = [
                {
                  id: keskeytysaika.id,
                  alkamispaiva: keskeytysaika.alkamispaiva,
                  paattymispaiva: keskeytysaika.paattymispaiva,
                  poissaoloprosentti: keskeytysaika.poissaoloprosentti,
                  poissaolonSyy: keskeytysaika.poissaolonSyy
                }
              ]
            }
          }
          return result
        },
        {}
      )

      const tilastotTyoskentelyjaksotMap = this.tilastotTyoskentelyjaksot.reduce(
        (
          result: { [key: number]: number },
          tyoskentelyjakso: TyoskentelyjaksotTilastotTyoskentelyjaksot
        ) => {
          result[tyoskentelyjakso.id] = tyoskentelyjakso.suoritettu
          return result
        },
        {}
      )

      return this.tyoskentelyjaksot
        .map((tj) => ({
          ...tj,
          tyoskentelypaikkaLabel: tj.tyoskentelypaikka.nimi,
          ajankohtaDate: tj.alkamispaiva ? parseISO(tj.alkamispaiva) : null,
          ajankohta: ajankohtaLabel(this, tj),
          tyoskentelyaikaLabel: tj.id ? this.$duration(tilastotTyoskentelyjaksotMap[tj.id]) : null,
          osaaikaprosenttiLabel: `${tj.osaaikaprosentti} %`,
          hyvaksyttyAiempaanErikoisalaanLabel: tj.hyvaksyttyAiempaanErikoisalaan
            ? this.$t('kylla')
            : this.$t('ei'),
          keskeytykset: (tj.id ? keskeytyksetGroupByTyoskentelyjakso[tj.id] : undefined) || [],
          keskeytyksetLength: (
            (tj.id ? keskeytyksetGroupByTyoskentelyjakso[tj.id] : undefined) || []
          ).length
        }))
        .sort((a, b) => sortByDateDesc(a.paattymispaiva, b.paattymispaiva))
    }

    get terveyskeskuskoulutusjaksoPalautettuKorjattavaksi() {
      return (
        this.tyoskentelyjaksotTaulukko?.terveyskeskuskoulutusjaksonTila ===
        TerveyskeskuskoulutusjaksonTila.PALAUTETTU_KORJATTAVAKSI
      )
    }

    get terveyskeskuskoulutusjaksoLahetetty() {
      return (
        this.tyoskentelyjaksotTaulukko?.terveyskeskuskoulutusjaksonTila ===
          TerveyskeskuskoulutusjaksonTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA ||
        this.tyoskentelyjaksotTaulukko?.terveyskeskuskoulutusjaksonTila ===
          TerveyskeskuskoulutusjaksonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
      )
    }

    get terveyskeskuskoulutusjaksoUusi() {
      return (
        this.tyoskentelyjaksotTaulukko?.terveyskeskuskoulutusjaksonTila ===
        TerveyskeskuskoulutusjaksonTila.UUSI
      )
    }

    get muokkausoikeudet() {
      if (!this.account.impersonated) {
        return true
      }

      return !!(
        this.account.originalUser.authorities.includes(ELSA_ROLE.OpintohallinnonVirkailija) &&
        this.account.erikoistuvaLaakari.muokkausoikeudetVirkailijoilla
      )
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .tyoskentelyjaksot {
    max-width: 1024px;
  }

  .tilastot {
    padding: 200px;
  }

  .tyoskentelyjaksot-table {
    ::v-deep table {
      td {
        padding-top: 0;
        padding-bottom: 0;
        vertical-align: middle;

        .btn {
          text-align: left;
        }
      }
      .b-table-details {
        td {
          padding-left: 0;
          padding-right: 0;
          background-color: #f5f5f6;
        }
        table {
          th {
            padding-bottom: 0.3rem;
            padding-left: 0;
            padding-right: 0;
          }
          border-bottom: none;
        }
      }
    }
  }

  @include media-breakpoint-down(sm) {
    .tyoskentelyjaksot-table {
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

          & > div {
            width: 100% !important;
            padding: 0 0 0.5rem 0 !important;
          }

          .btn {
            padding: 0 0 !important;
          }

          &::before {
            text-align: left !important;
            font-weight: 500 !important;
            width: 100% !important;
            padding-right: 0 !important;
          }
        }

        .b-table-details {
          background-color: #f5f5f6;
          padding: 0 !important;

          & > td > div {
            padding: 0 !important;
          }

          table {
            tr {
              border: none;
              padding: 0 !important;
              border-bottom: $table-border-width solid $table-border-color;
              margin: 0;
              padding-top: $table-cell-padding !important;
              padding-bottom: $table-cell-padding !important;
            }
            tr:last-child {
              border-bottom: 0;
            }
            td {
              padding-left: $table-cell-padding;
              padding-right: $table-cell-padding;

              &:last-child > div {
                padding-bottom: 0 !important;
              }
            }
            margin: 0;
          }
        }
      }
    }
  }

  .duration-card {
    min-width: 300px;

    .duration-text {
      font-size: 1.25rem;
    }
  }

  .donut-chart {
    max-width: 450px;
  }
</style>
