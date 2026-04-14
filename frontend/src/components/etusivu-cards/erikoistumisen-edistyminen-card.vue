<template>
  <b-card-skeleton :header="$t('erikoistumisen-edistyminen')" :loading="initializing" class="mb-4">
    <b-container v-if="!initializing && edistyminen" fluid class="p-0">
      <b-row>
        <b-col xl="8">
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{
                  name: 'arvioinnit'
                }"
                variant="link"
                class="pl-0 border-0 pt-0"
              >
                <h3 class="mb-0">
                  {{ $t('arvioinnit') }}
                </h3>
              </elsa-button>
              <div class="d-flex flex-wrap">
                <div class="d-flex flex-column mb-2 col-min-width">
                  <span v-if="edistyminen.arviointienKeskiarvo" class="text-size-lg">
                    {{ keskiarvoFormatted(edistyminen.arviointienKeskiarvo) }} / 5
                  </span>
                  <span v-else class="text-size-lg">- / 5</span>
                  <span class="text-size-sm">
                    {{ $t('arviointien-ka') }}
                    <elsa-popover :title="arviointiAsteikonNimi">
                      <elsa-arviointiasteikon-taso-tooltip-content
                        :selite="arviointiAsteikonSelite"
                        :arviointiasteikon-tasot="edistyminen.arviointiasteikko.tasot"
                      />
                    </elsa-popover>
                  </span>
                </div>
                <div class="d-flex flex-column mb-2">
                  <span class="text-size-lg">
                    {{ edistyminen.arvioitavatKokonaisuudetVahintaanYksiArvioLkm }} /
                    {{ edistyminen.arvioitavienKokonaisuuksienLkm }}
                  </span>
                  <span class="text-size-sm">
                    {{ $t('arv-kokonaisuutta-sis-vah-yhden-arvion') }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{
                  name: 'suoritemerkinnat'
                }"
                variant="link"
                class="pl-0 border-0 pt-0"
              >
                <h3 class="mb-0">
                  {{ $t('suoritemerkinnat') }}
                </h3>
              </elsa-button>
              <div class="d-flex flex-wrap">
                <div class="d-flex flex-column mb-2 col-min-width">
                  <span v-if="edistyminen.vaaditutSuoritemerkinnatLkm > 0" class="text-size-lg">
                    {{ edistyminen.suoritemerkinnatLkm }} /
                    {{ edistyminen.vaaditutSuoritemerkinnatLkm }}
                    {{ $t('kpl') }}
                  </span>
                  <span v-else class="text-size-lg">
                    {{ edistyminen.suoritemerkinnatLkm }}
                    {{ $t('kpl') }}
                  </span>
                  <span class="text-size-sm">
                    {{ $t('suoritettu') }}
                  </span>
                </div>
                <div v-if="edistyminen.osaalueetVaadittuLkm > 0" class="d-flex flex-column mb-2">
                  <span class="text-size-lg">
                    {{ edistyminen.osaalueetSuoritettuLkm }} /
                    {{ edistyminen.osaalueetVaadittuLkm }}
                  </span>
                  <span class="text-size-sm">
                    {{ $t('valmista-osa-aluetta') }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{
                  name: 'tyoskentelyjaksot'
                }"
                variant="link"
                class="pl-0 border-0 pt-0"
              >
                <h3 class="mb-0">
                  {{ $t('tyoskentelyjaksot') }}
                </h3>
              </elsa-button>
              <div class="d-flex flex-wrap">
                <div class="d-flex flex-column mb-2 w-50">
                  <span class="text-size-lg">
                    {{
                      $duration(
                        edistyminen.tyoskentelyjaksoTilastot.arvioErikoistumiseenHyvaksyttavista
                      )
                    }}
                  </span>
                  <span class="text-size-sm">
                    {{ $t('arvio-erikoistumiseen-hyvaksyttavista') }}
                  </span>
                </div>
                <div class="d-flex flex-column mb-2 pl-4 w-50">
                  <span class="text-size-lg">
                    {{
                      $duration(edistyminen.tyoskentelyjaksoTilastot.arvioPuuttuvastaKoulutuksesta)
                    }}
                  </span>
                  <span class="text-size-sm">
                    {{ $t('arvio-puuttuvasta-koulutuksesta') }}
                  </span>
                </div>
              </div>
              <div class="d-flex justify-content-center">
                <b-col class="p-0" md="10">
                  <tyoskentelyjaksot-bar-chart :tilastot="edistyminen.tyoskentelyjaksoTilastot" />
                </b-col>
              </div>
            </div>
          </div>
          <div v-if="edistyminen.yekSuoritusPvm" class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <h3 class="mb-0">
                {{ $t('muut-koulutukset') }}
              </h3>
              <div class="d-flex align-items-center">
                <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-1" />
                <span>
                  {{ $t('yek-suoritettu') }}
                  {{ $date(edistyminen.yekSuoritusPvm) }}
                </span>
              </div>
            </div>
          </div>
        </b-col>
        <b-col xl="4">
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{
                  name: 'teoriakoulutukset'
                }"
                variant="link"
                class="pl-0 border-0 pt-0 pb-1"
              >
                <h3 class="mb-0">
                  {{ $t('teoriakoulutus') }}
                </h3>
              </elsa-button>
              <elsa-progress-bar
                :value="edistyminen.teoriakoulutuksetSuoritettu"
                :min-required="edistyminen.teoriakoulutuksetVaadittu"
                :show-required-text="true"
                color="#41b257"
                background-color="#b3e1bc"
                text-color="#000"
                class="mb-3"
                :custom-unit="$t('t')"
              />
              <elsa-button
                :to="{ name: 'opintosuoritukset', hash: '#johtamisopinnot' }"
                variant="link"
                class="pl-0 border-0 pt-0 pb-1"
              >
                <h3 class="mb-0">
                  {{ $t('johtamisopinnot') }}
                </h3>
              </elsa-button>
              <elsa-progress-bar
                :value="edistyminen.johtamisopinnotSuoritettu"
                :min-required="edistyminen.johtamisopinnotVaadittu"
                :show-required-text="true"
                color="#41b257"
                background-color="#b3e1bc"
                text-color="#000"
                class="mb-3"
                :custom-unit="$t('opintopistetta-lyhenne')"
              />
              <div v-if="edistyminen.sateilysuojakoulutuksetVaadittu > 0">
                <elsa-button
                  :to="{ name: 'opintosuoritukset', hash: '#sateilysuojakoulutukset' }"
                  variant="link"
                  class="pl-0 border-0 pt-0 pb-1"
                >
                  <h3 class="mb-0">
                    {{ $t('sateilysuojelukoulutus') }}
                  </h3>
                </elsa-button>
                <elsa-progress-bar
                  :value="edistyminen.sateilysuojakoulutuksetSuoritettu"
                  :min-required="edistyminen.sateilysuojakoulutuksetVaadittu"
                  :show-required-text="true"
                  color="#41b257"
                  background-color="#b3e1bc"
                  text-color="#000"
                  class="mb-3"
                  :custom-unit="$t('opintopistetta-lyhenne')"
                />
              </div>
            </div>
          </div>
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{
                  name: koejaksoComponent(edistyminen.koejaksonSuoritusmerkintaExists),
                  hash: koejaksoComponentHash(edistyminen.koejaksonSuoritusmerkintaExists)
                }"
                variant="link"
                class="pl-0 border-0 pt-0"
              >
                <h3 class="mb-0">
                  {{ $t('koejakso') }}
                </h3>
              </elsa-button>
              <div class="d-flex align-items-center">
                <font-awesome-icon
                  :icon="
                    koejaksoIcon(edistyminen.koejaksoTila, edistyminen.opintooikeudenPaattymispaiva)
                  "
                  :class="
                    koejaksoIconClass(
                      edistyminen.koejaksoTila,
                      edistyminen.opintooikeudenPaattymispaiva
                    )
                  "
                />
                <span
                  :class="
                    koejaksoStatusClass(
                      edistyminen.koejaksoTila,
                      edistyminen.opintooikeudenPaattymispaiva
                    )
                  "
                  class="ml-1"
                >
                  {{ koejaksoStatusText(edistyminen.koejaksoTila) }}
                </span>
              </div>
            </div>
          </div>
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{
                  name: terveyskeskusjaksoComponent(
                    edistyminen.terveyskeskuskoulutusjaksoSuoritettu
                  ),
                  hash: terveyskeskusjaksoComponentHash(
                    edistyminen.terveyskeskuskoulutusjaksoSuoritettu
                  )
                }"
                variant="link"
                class="pl-0 border-0 pt-0"
              >
                <h3 class="mb-0">
                  {{ $t('terveyskeskuskoulutusjakso') }}
                </h3>
              </elsa-button>
              <div class="d-flex align-items-center">
                <font-awesome-icon
                  v-if="terveyskeskusjaksoIcon(edistyminen.terveyskeskuskoulutusjaksoSuoritettu)"
                  :icon="terveyskeskusjaksoIcon(edistyminen.terveyskeskuskoulutusjaksoSuoritettu)"
                  :class="
                    terveyskeskusjaksoIconClass(edistyminen.terveyskeskuskoulutusjaksoSuoritettu)
                  "
                />
                <span>
                  {{
                    terveyskeskusjaksoStatusText(edistyminen.terveyskeskuskoulutusjaksoSuoritettu)
                  }}
                </span>
              </div>
            </div>
          </div>
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{ name: 'opintosuoritukset', hash: '#kuulustelu' }"
                variant="link"
                class="pl-0 border-0 pt-0"
              >
                <h3 class="mb-0">
                  {{ $t('kuulustelu') }}
                </h3>
              </elsa-button>
              <div>
                {{ edistyminen.valtakunnallisetKuulustelutSuoritettuLkm }}
                {{ $t('kpl') }}
              </div>
            </div>
          </div>
          <div class="border rounded pt-3 pb-2 mb-2">
            <div class="container-fluid">
              <h3>{{ $t('opintooikeus') }}</h3>
              <div class="d-flex align-items-center">
                <font-awesome-icon
                  v-if="showOpintooikeusAlert(edistyminen.opintooikeudenPaattymispaiva)"
                  :icon="['fas', 'exclamation-circle']"
                  class="text-danger"
                />
                <span
                  :class="{
                    'text-danger ml-1': showOpintooikeusAlert(
                      edistyminen.opintooikeudenPaattymispaiva
                    )
                  }"
                >
                  {{ $date(edistyminen.opintooikeudenMyontamispaiva) }} -
                  {{ $date(edistyminen.opintooikeudenPaattymispaiva) }}
                </span>
              </div>
            </div>
          </div>
        </b-col>
      </b-row>
    </b-container>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { parseISO, differenceInMonths } from 'date-fns'
  import { Vue, Component } from 'vue-property-decorator'

  import { getErikoistumisenEdistyminen } from '@/api/erikoistuva'
  import ElsaArviointiasteikonTasoTooltipContent from '@/components/arviointiasteikon-taso/arviointiasteikon-taso-tooltip.vue'
  import ElsaArviointiasteikonTaso from '@/components/arviointiasteikon-taso/arviointiasteikon-taso.vue'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import ElsaProgressBar from '@/components/progress-bar/progress-bar.vue'
  import TyoskentelyjaksotBarChart from '@/components/tyoskentelyjaksot-bar-chart.vue'
  import { ErikoistumisenEdistyminen } from '@/types'
  import { LomakeTilat, ArviointiasteikkoTyyppi } from '@/utils/constants'
  import { getKeskiarvoFormatted } from '@/utils/keskiarvoFormatter'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton,
      ElsaFormGroup,
      ElsaPopover,
      TyoskentelyjaksotBarChart,
      ElsaProgressBar,
      ElsaArviointiasteikonTaso,
      ElsaArviointiasteikonTasoTooltipContent
    }
  })
  export default class ErikoistujienSeurantaCard extends Vue {
    initializing = true

    edistyminen: ErikoistumisenEdistyminen | null = null

    async mounted() {
      this.edistyminen = (await getErikoistumisenEdistyminen()).data
      this.initializing = false
    }

    koejaksoComponent(suoritusmerkintaExists: boolean) {
      return suoritusmerkintaExists ? 'opintosuoritukset' : 'koejakso'
    }

    koejaksoComponentHash(suoritusmerkintaExists: boolean) {
      return suoritusmerkintaExists ? '#muut' : ''
    }

    koejaksoIcon(status: string, opintooikeudenPaattymispaiva: string) {
      switch (status) {
        case LomakeTilat.HYVAKSYTTY:
          return ['fas', 'check-circle']
        case LomakeTilat.ODOTTAA_HYVAKSYNTAA:
          return ['far', 'clock']
        case LomakeTilat.EI_AKTIIVINEN:
          if (differenceInMonths(parseISO(opintooikeudenPaattymispaiva), new Date()) <= 12) {
            return ['fas', 'exclamation-circle']
          } else return ['fas', 'info-circle']
      }
    }

    koejaksoIconClass(status: string, opintooikeudenPaattymispaiva: string) {
      switch (status) {
        case LomakeTilat.HYVAKSYTTY:
          return 'text-success'
        case LomakeTilat.ODOTTAA_HYVAKSYNTAA:
          return 'text-warning'
        case LomakeTilat.EI_AKTIIVINEN:
          if (differenceInMonths(parseISO(opintooikeudenPaattymispaiva), new Date()) <= 12) {
            return 'text-danger'
          } else return 'text-warning'
      }
    }

    koejaksoStatusText(status: string) {
      switch (status) {
        case LomakeTilat.HYVAKSYTTY:
          return this.$t('hyvaksytty')
        case LomakeTilat.ODOTTAA_HYVAKSYNTAA:
          return this.$t('kesken')
        case LomakeTilat.EI_AKTIIVINEN:
          return this.$t('aloittamatta')
      }
    }

    koejaksoStatusClass(status: string, opintooikeudenPaattymispaiva: string) {
      if (
        status === LomakeTilat.EI_AKTIIVINEN &&
        differenceInMonths(parseISO(opintooikeudenPaattymispaiva), new Date()) <= 12
      )
        return 'text-danger'
    }

    terveyskeskusjaksoComponent(suoritusmerkintaExists: boolean) {
      return suoritusmerkintaExists ? 'opintosuoritukset' : 'tyoskentelyjaksot'
    }

    terveyskeskusjaksoComponentHash(suoritusmerkintaExists: boolean) {
      return suoritusmerkintaExists ? '#muut' : ''
    }

    terveyskeskusjaksoIcon(suoritusmerkintaExists: boolean) {
      if (suoritusmerkintaExists) {
        return ['fas', 'check-circle']
      }
    }

    terveyskeskusjaksoIconClass(suoritusmerkintaExists: boolean) {
      if (suoritusmerkintaExists) {
        return 'text-success mr-1'
      }
    }

    terveyskeskusjaksoStatusText(suoritusmerkintaExists: boolean) {
      if (suoritusmerkintaExists) {
        return this.$t('hyvaksytty')
      } else {
        return this.$t('ei-suoritettu')
      }
    }

    showOpintooikeusAlert(opintooikeudenPaattymispaiva: string) {
      return differenceInMonths(parseISO(opintooikeudenPaattymispaiva), new Date()) <= 6
    }

    get arviointiAsteikonNimi() {
      return this.edistyminen?.arviointiasteikko.nimi === ArviointiasteikkoTyyppi.EPA
        ? this.$t('luottamuksen-tason-keskiarvo')
        : this.$t('etapin-keskiarvo')
    }

    get arviointiAsteikonSelite() {
      return this.edistyminen?.arviointiasteikko.nimi === ArviointiasteikkoTyyppi.EPA
        ? this.$t('arviointien-ka-selite-epa')
        : this.$t('arviointien-ka-selite-etappi')
    }

    keskiarvoFormatted(keskiarvo: number) {
      return getKeskiarvoFormatted(keskiarvo)
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .col-min-width {
    min-width: 250px;
  }
</style>
