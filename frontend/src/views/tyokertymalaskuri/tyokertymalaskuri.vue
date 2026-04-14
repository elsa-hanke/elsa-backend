<template>
  <b-container class="mt-4 mb-6">
    <div class="tyoskentelyjaksot">
      <b-breadcrumb :items="items" class="mb-0" />
      <b-container fluid>
        <b-row lg>
          <b-col>
            <h1>{{ $t('tyokertymalaskuri') }}</h1>
            <!-- eslint-disable vue/no-v-html -->
            <p>{{ $t('tyokertymalaskuri-kuvaus') }}</p>
            <div class="d-flex flex-wrap mb-3 mb-lg-4">
              <elsa-button
                variant="primary"
                class="mb-2 mr-2"
                @click="openLisaaTyoskentelyjaksoFormModal"
              >
                {{ $t('lisaa-tyoskentelyjakso') }}
              </elsa-button>
              <tyokertymalaskuri-modal
                v-model="lisaaTyoskentelyjaksoFormModal"
                :tyoskentelyjakso="editTyoskentelyjakso"
                @delete="onDelete"
                @submit="onSubmit"
              ></tyokertymalaskuri-modal>
            </div>
            <div v-if="tyoskentelyjaksotTaulukko != null && tilastot != null">
              <h2>{{ $t('laskennan-yhteenveto') }}</h2>
              <div
                v-if="tyoskentelyjaksotTaulukko.tyoskentelyjaksot.length > 0"
                class="d-flex flex-wrap mb-3 mb-lg-4"
              >
                <elsa-button variant="outline-primary" class="mb-2 mr-2" @click="printPage">
                  {{ $t('tallenna-pdf') }}
                </elsa-button>
                <elsa-button variant="outline-primary" class="mb-2 mr-2" disabled="disabled">
                  {{ $t('jaa-linkkina') }}
                </elsa-button>
              </div>
              <div class="d-flex justify-content-center border rounded pt-3 pb-2 mb-4">
                <div class="container-fluid">
                  <elsa-form-group :label="$t('tyokertyma')">
                    <template #default="{ uid }">
                      <div :id="uid" class="d-flex flex-wrap">
                        <div class="d-flex flex-column mb-2 duration-card">
                          <span class="duration-text">
                            {{ $duration(tilastot.tyoskentelyaikaYhteensa) }}
                          </span>
                          <span class="text-size-sm">{{ $t('tyoskentelyaika') }}</span>
                        </div>
                        <div class="d-flex flex-column mb-2 duration-card">
                          <span class="duration-text">
                            {{ $duration(tilastot.poissaoloaikaYhteensa) }}
                          </span>
                          <span class="text-size-sm">
                            {{ $t('poissaolot') }}
                          </span>
                        </div>
                        <div class="d-flex flex-column mb-2 duration-card">
                          <span class="duration-text">
                            {{ $duration(tilastot.tyokertymaYhteensa) }}
                          </span>
                          <span class="text-size-sm">
                            {{ $t('tyokertyma-yhteensa') }}
                          </span>
                        </div>
                      </div>
                    </template>
                  </elsa-form-group>
                  <b-row>
                    <elsa-form-group :label="$t('kaytannon-koulutus')" class="col-xl-6 mb-0">
                      <template #default="{ uid }">
                        <div :id="uid" class="donut-chart">
                          <apexchart
                            v-if="showChart"
                            :key="chartKey"
                            :options="donutOptions"
                            :series="donutSeries"
                            style="width: 500px"
                          ></apexchart>
                        </div>
                      </template>
                    </elsa-form-group>
                  </b-row>
                </div>
              </div>
              <div class="tyoskentelyjaksot-table">
                <b-table
                  ref="tyoskentelyjaksotTable"
                  :items="tyoskentelyjaksotFormatted"
                  :fields="fields"
                  stacked="md"
                  responsive
                >
                  <template #table-colgroup="scope">
                    <col
                      v-for="field in scope.fields"
                      :key="field.key"
                      :style="{ width: field.width }"
                    />
                  </template>
                  <template #cell(tyoskentelypaikkaLabel)="row">
                    <elsa-button
                      variant="link"
                      class="shadow-none px-0"
                      @click="openTyoskentelyjaksoFormModalForEdit(row.item)"
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
                    <div v-if="row.item.keskeytykset.length > 0" class="px-md-3">
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
                          <b-tr
                            v-for="(keskeytysaika, index) in row.item.keskeytykset"
                            :key="index"
                          >
                            <b-td :stacked-heading="$t('poissaolon-syy')">
                              <elsa-button
                                variant="link"
                                class="shadow-none px-0"
                                @click="openTyoskentelyjaksoFormModalForEdit(row.item)"
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
              <elsa-button variant="link" class="shadow-none p-0" :to="{ name: 'etusivu' }">
                {{ $t('siirry-elsan-etusivulle') }}
              </elsa-button>
            </div>
            <div v-else class="text-center">
              <b-spinner variant="primary" :label="$t('ladataan')" />
            </div>
          </b-col>
        </b-row>
      </b-container>
    </div>
  </b-container>
</template>

<script lang="ts">
  import { parseISO } from 'date-fns'
  import { Component, Vue } from 'vue-property-decorator'

  import { daysBetween } from './dateUtils'
  import { tyoskentelyjaksotTaulukkoData } from './tyoskentelyjaksot-offline-data'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPoissaolonSyyt from '@/components/poissaolon-syyt/poissaolon-syyt.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import TyokertymalaskuriModal from '@/components/tyokertymalaskuri/tyokertymalaskuri-modal.vue'
  import TyoskentelyjaksotBarChart from '@/components/tyoskentelyjaksot-bar-chart.vue'
  import ElsaVanhaAsetusVaroitus from '@/components/vanha-asetus-varoitus/vanha-asetus-varoitus.vue'
  import {
    TyokertymaLaskuriTyoskentelyjakso,
    TyokertymaLaskuriTyoskentelyjaksotTable,
    TyoskentelyjaksotTilastotKaytannonKoulutus,
    TyoskentelyjaksotTilastotTyoskentelyjaksot
  } from '@/types'
  import { confirmExitWithTexts } from '@/utils/confirm'
  import { KaytannonKoulutusTyyppi } from '@/utils/constants'
  import { sortByDateDesc } from '@/utils/date'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import { ajankohtaLabel } from '@/utils/tyoskentelyjakso'
  import { validateTyoskentelyaika } from '@/views/tyokertymalaskuri/overlapping-tyoskentelyjakso-validator'
  import { getVahennettavatPaivat } from '@/views/tyokertymalaskuri/tyoskentelyjakson-pituus-counter'

  @Component({
    components: {
      TyokertymalaskuriModal,
      ElsaButton,
      ElsaFormGroup,
      ElsaPopover,
      TyoskentelyjaksotBarChart,
      ElsaPoissaolonSyyt,
      ElsaVanhaAsetusVaroitus
    }
  })
  export default class Tyokertymalaskuri extends Vue {
    items = [
      {
        text: this.$t('kirjautuminen-linkki'),
        to: { name: 'login' }
      },
      {
        text: this.$t('tyokertymalaskuri'),
        active: true
      }
    ]
    tyoskentelyjaksotTaulukko: TyokertymaLaskuriTyoskentelyjaksotTable =
      tyoskentelyjaksotTaulukkoData
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
        label: this.$t('tyokertyma'),
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
    lisaaTyoskentelyjaksoFormModal = false
    editTyoskentelyjakso: TyokertymaLaskuriTyoskentelyjakso | null = null
    tyoskentelyjaksotLocalStorageKey = 'laskuri-tyoskentelyjaksot'
    showChart = false
    chartKey = 0
    showDetails = false

    async mounted() {
      this.loadFromLocalStorage()
      window.addEventListener('beforeunload', this.handleBeforeUnload)
    }

    beforeDestroy() {
      window.removeEventListener('beforeunload', this.handleBeforeUnload)
    }

    refreshChart() {
      this.chartKey += 1
    }

    get tyoskentelyjaksot() {
      if (this.tyoskentelyjaksotTaulukko) {
        return this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot
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
      if (this.tilastot && this.showChart) {
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
          (kk) => kk.kaytannonKoulutus === KaytannonKoulutusTyyppi.MUU_ERIKOISALA
        ),
        this.tilastotKaytannonKoulutus.find(
          (kk) =>
            kk.kaytannonKoulutus === KaytannonKoulutusTyyppi.KAHDEN_VUODEN_KLIININEN_TYOKOKEMUS
        ),
        this.tilastotKaytannonKoulutus.find(
          (kk) => kk.kaytannonKoulutus === KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        )
      ].filter((kk) => kk !== null) as TyoskentelyjaksotTilastotKaytannonKoulutus[]
    }

    get donutSeries() {
      const hasTyoskentelyjaksot = this.tyoskentelyjaksot.length > 0
      return hasTyoskentelyjaksot
        ? this.tilastotKaytannonKoulutusSorted.map((kk) => kk.suoritettu)
        : [100]
    }

    get donutOptions() {
      const hasTyoskentelyjaksot = this.tyoskentelyjaksot.length > 0
      return {
        colors: hasTyoskentelyjaksot
          ? ['#097BB9', '#FF8B06', '#808080', '#FFB406']
          : ['#E8E9EC', '#E8E9EC', '#E8E9EC', '#E8E9EC'],
        labels: [
          `${this.$t('oma-erikoisala')}: ${
            this.tilastotKaytannonKoulutusSorted[0]?.suoritettu
              ? this.$duration(this.tilastotKaytannonKoulutusSorted[0].suoritettu)
              : '0'
          }`,
          `${this.$t('muu-erikoisala')}: ${
            this.tilastotKaytannonKoulutusSorted[1]?.suoritettu
              ? this.$duration(this.tilastotKaytannonKoulutusSorted[1].suoritettu)
              : '0'
          }`,
          `${this.$t('kahden-vuoden-kliininen-tyokokemus')}: ${
            this.tilastotKaytannonKoulutusSorted[2]?.suoritettu
              ? this.$duration(this.tilastotKaytannonKoulutusSorted[2].suoritettu)
              : '0'
          }`,
          `${this.$t('terveyskeskustyo')}: ${
            this.tilastotKaytannonKoulutusSorted[3]?.suoritettu
              ? this.$duration(this.tilastotKaytannonKoulutusSorted[3].suoritettu)
              : '0'
          }`
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

    get tyoskentelyjaksotFormatted() {
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
        .map((tj, index) => ({
          ...tj,
          tyoskentelypaikkaLabel: tj.tyoskentelypaikka.nimi,
          ajankohtaDate: tj.alkamispaiva ? parseISO(tj.alkamispaiva) : null,
          ajankohta: ajankohtaLabel(this, tj),
          tyoskentelyaikaLabel: this.$duration(tilastotTyoskentelyjaksotMap[index + 1]),
          osaaikaprosenttiLabel: `${
            String(tj.kaytannonKoulutus) !==
            String(KaytannonKoulutusTyyppi.KAHDEN_VUODEN_KLIININEN_TYOKOKEMUS)
              ? tj.osaaikaprosentti
              : tj.kahdenvuodenosaaikaprosentti
          } %`,
          keskeytykset: tj.poissaolot,
          keskeytyksetLength: tj.poissaolot.length,
          _showDetails: this.showDetails
        }))
        .sort((a, b) => sortByDateDesc(a.paattymispaiva, b.paattymispaiva))
    }

    openLisaaTyoskentelyjaksoFormModal() {
      this.editTyoskentelyjakso = null
      this.lisaaTyoskentelyjaksoFormModal = true
    }

    openTyoskentelyjaksoFormModalForEdit(tj: TyokertymaLaskuriTyoskentelyjakso) {
      this.editTyoskentelyjakso = tj
      this.lisaaTyoskentelyjaksoFormModal = true
    }

    async onSubmit(formData: TyokertymaLaskuriTyoskentelyjakso) {
      const isValid = validateTyoskentelyaika(
        formData.id > 0 ? formData.id : null,
        parseISO(formData.alkamispaiva),
        parseISO(formData.paattymispaiva || this.getISODateNow()),
        this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot,
        formData.kaytannonKoulutus !==
          String(KaytannonKoulutusTyyppi.KAHDEN_VUODEN_KLIININEN_TYOKOKEMUS)
          ? formData.osaaikaprosentti
          : formData.kahdenvuodenosaaikaprosentti
      )
      if (!isValid) {
        toastFail(this, this.$t('paallekkaiset-tyoskentelyjaksot-yhteenlaskettu-tyoaika-virhe'))
        return
      }

      if (formData.id > 0) {
        const index: number = this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot.findIndex(
          (item) => item.id === formData.id
        )
        this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot[index] = formData
        toastSuccess(this, this.$t('tyoskentelyjakson-muutokset-tallennettu'))
      } else {
        const maxId = this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot.reduce(
          (max, item) => Math.max(max, item.id),
          0
        )
        formData.id = maxId + 1
        this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot.push(formData)
        toastSuccess(this, this.$t('tyoskentelyjakso-lisatty'))
      }
      this.saveToLocalStorage()
      this.lisaaTyoskentelyjaksoFormModal = false
    }

    onDelete(id: number) {
      const indexToRemove: number = this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot.findIndex(
        (item) => item.id === id
      )
      if (indexToRemove !== -1) {
        this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot.splice(indexToRemove, 1)
      }
      this.saveToLocalStorage()
      this.editTyoskentelyjakso = null
      this.lisaaTyoskentelyjaksoFormModal = false
      toastSuccess(this, this.$t('tyoskentelyjakso-poistettu'))
    }

    saveToLocalStorage() {
      localStorage.setItem(
        this.tyoskentelyjaksotLocalStorageKey,
        JSON.stringify(this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot)
      )
      this.laskeTilastot()
    }

    loadFromLocalStorage() {
      const savedData = localStorage.getItem(this.tyoskentelyjaksotLocalStorageKey)
      if (savedData) {
        this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot = JSON.parse(savedData)
        this.laskeTilastot()
      }
    }

    laskeTilastot() {
      this.showChart = false
      const vahennettavatPaivat = getVahennettavatPaivat(
        JSON.parse(JSON.stringify(this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot))
      )

      this.tyoskentelyjaksotTaulukko.tilastot = {
        arvioErikoistumiseenHyvaksyttavista: 0,
        arvioPuuttuvastaKoulutuksesta: 0,
        kaytannonKoulutus: [
          {
            kaytannonKoulutus: KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS,
            suoritettu: 0
          },
          {
            kaytannonKoulutus: KaytannonKoulutusTyyppi.MUU_ERIKOISALA,
            suoritettu: 0
          },
          {
            kaytannonKoulutus: KaytannonKoulutusTyyppi.KAHDEN_VUODEN_KLIININEN_TYOKOKEMUS,
            suoritettu: 0
          },
          {
            kaytannonKoulutus: KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO,
            suoritettu: 0
          }
        ],
        koulutustyypit: {
          terveyskeskusVaadittuVahintaan: 0,
          terveyskeskusMaksimipituus: 0,
          terveyskeskusSuoritettu: 0,
          yliopistosairaalaVaadittuVahintaan: 0,
          yliopistosairaalaSuoritettu: 0,
          yliopistosairaaloidenUlkopuolinenVaadittuVahintaan: 0,
          yliopistosairaaloidenUlkopuolinenSuoritettu: 0,
          yhteensaVaadittuVahintaan: 0,
          yhteensaSuoritettu: 0
        },
        poissaoloaikaYhteensa: 0,
        tyokertymaYhteensa: 0,
        tyoskentelyaikaYhteensa: 0,
        tyoskentelyjaksot: []
      }
      this.tyoskentelyjaksotTaulukko.tyoskentelyjaksot.forEach(
        (tj: TyokertymaLaskuriTyoskentelyjakso, index: number) => {
          const vahennettavat = vahennettavatPaivat.get(tj.id) || 0

          let tyoskentelyaika = 0
          let tyokertyma = 0

          tyokertyma = daysBetween(
            parseISO(tj.alkamispaiva),
            parseISO(tj.paattymispaiva || this.getISODateNow())
          )

          const osaaikaProsentti =
            (String(tj.kaytannonKoulutus) !==
            String(KaytannonKoulutusTyyppi.KAHDEN_VUODEN_KLIININEN_TYOKOKEMUS)
              ? tj.osaaikaprosentti
              : tj.kahdenvuodenosaaikaprosentti) / 100

          const tyokertymaOsaajalla = tyokertyma * osaaikaProsentti - vahennettavat

          tyoskentelyaika = daysBetween(
            parseISO(tj.alkamispaiva),
            parseISO(tj.paattymispaiva || this.getISODateNow())
          )
          const tyoskentelyaikaOsaajalla = tyoskentelyaika * osaaikaProsentti

          this.tyoskentelyjaksotTaulukko.tilastot.tyoskentelyaikaYhteensa +=
            tyoskentelyaikaOsaajalla
          this.tyoskentelyjaksotTaulukko.tilastot.poissaoloaikaYhteensa += vahennettavat
          this.tyoskentelyjaksotTaulukko.tilastot.tyokertymaYhteensa += tyokertymaOsaajalla
          switch (tj.kaytannonKoulutus) {
            case KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS:
              this.tyoskentelyjaksotTaulukko.tilastot.kaytannonKoulutus[0].suoritettu +=
                tyokertymaOsaajalla
              break
            case KaytannonKoulutusTyyppi.MUU_ERIKOISALA:
              this.tyoskentelyjaksotTaulukko.tilastot.kaytannonKoulutus[1].suoritettu +=
                tyokertymaOsaajalla
              break
            case KaytannonKoulutusTyyppi.KAHDEN_VUODEN_KLIININEN_TYOKOKEMUS:
              this.tyoskentelyjaksotTaulukko.tilastot.kaytannonKoulutus[2].suoritettu +=
                tyokertymaOsaajalla
              break
            case KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO:
              this.tyoskentelyjaksotTaulukko.tilastot.kaytannonKoulutus[3].suoritettu +=
                tyokertymaOsaajalla
              break
          }
          this.tyoskentelyjaksotTaulukko.tilastot.tyoskentelyjaksot.push({
            id: index + 1,
            suoritettu: tyokertymaOsaajalla
          })
        }
      )
      this.showChart = true
      this.refreshChart()
    }

    getISODateNow() {
      const date = new Date()
      return date.toISOString().split('T')[0]
    }

    printPage() {
      this.showDetails = true
      setTimeout(() => {
        const styleElement = document.createElement('style')
        document.head.appendChild(styleElement)
        window.print()
        document.head.removeChild(styleElement)
      }, 500)
    }

    beforeRouteLeave(to: any, from: any, next: any) {
      const handleNavigation = async () => {
        const shouldProceed = await confirmExitWithTexts(
          this,
          this.$t('vahvista-poistuminen') as string,
          this.$t('tyokertymalaskuri-poistuminen-ohje') as string,
          this.$t('poistu-laskurista') as string,
          this.$t('peruuta') as string
        )
        if (shouldProceed) {
          localStorage.removeItem(this.tyoskentelyjaksotLocalStorageKey)
          window.location.href = '/kirjautuminen'
        } else {
          next(false)
        }
      }
      handleNavigation().catch(() => {
        next(false)
      })
    }

    async handleBeforeUnload() {
      if (process.env.NODE_ENV !== 'development') {
        localStorage.removeItem(this.tyoskentelyjaksotLocalStorageKey)
      }
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

  .bar-chart {
    max-width: 450px;
  }

  .donut-chart {
    max-width: 500px;
  }
</style>
