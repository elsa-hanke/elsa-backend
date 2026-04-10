<template>
  <b-card-skeleton :header="$t('yek.koulutuksen-edistyminen')" :loading="initializing" class="mb-4">
    <b-container v-if="!initializing && edistyminen" fluid class="p-0">
      <b-row>
        <b-col xl="8">
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{
                  name: 'yektyoskentelyjaksot'
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
                    {{ $t('yek.arvio-hyvaksyttavasta-koulutuksesta') }}
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
                  <tyoskentelyjaksot-yek-bar-chart
                    :tilastot="edistyminen.tyoskentelyjaksoTilastot"
                  />
                </b-col>
              </div>
              <div
                v-if="edistyminen.laakarikoulutusSuoritettuSuomiTaiBelgia"
                class="d-flex flex-row"
              >
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                </em>
                <div>
                  {{ $t('yek.aiempi-hyvaksiluettu-suoritus') }}
                </div>
              </div>
            </div>
          </div>
        </b-col>
        <b-col xl="4">
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
          <div class="border rounded pt-3 pb-2 mb-4">
            <div class="container-fluid">
              <elsa-button
                :to="{ name: 'yekteoriakoulutukset' }"
                variant="link"
                class="pl-0 border-0 pt-0"
              >
                <h3 class="mb-0">
                  {{ $t('teoriakoulutus') }}
                </h3>
              </elsa-button>
              <div>
                <span
                  v-if="
                    edistyminen.teoriakoulutuksetSuoritettu >= edistyminen.teoriakoulutuksetVaadittu
                  "
                  class="text-success"
                >
                  <font-awesome-icon :icon="['fas', 'check-circle']" />
                  {{ $t('suoritettu') }}
                </span>
                <span v-else>
                  {{ $t('ei-suoritettu') }}
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

  import { getErikoistumisenEdistyminen } from '@/api/yek-koulutettava'
  import ElsaArviointiasteikonTasoTooltipContent from '@/components/arviointiasteikon-taso/arviointiasteikon-taso-tooltip.vue'
  import ElsaArviointiasteikonTaso from '@/components/arviointiasteikon-taso/arviointiasteikon-taso.vue'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import ElsaProgressBar from '@/components/progress-bar/progress-bar.vue'
  import TyoskentelyjaksotYekBarChart from '@/components/yek/tyoskentelyjaksot-yek-bar-chart.vue'
  import { ErikoistumisenEdistyminen } from '@/types'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton,
      ElsaFormGroup,
      ElsaPopover,
      TyoskentelyjaksotYekBarChart,
      ElsaProgressBar,
      ElsaArviointiasteikonTaso,
      ElsaArviointiasteikonTasoTooltipContent
    }
  })
  export default class YekKoulutuksenEdistyminenCard extends Vue {
    initializing = true

    edistyminen: ErikoistumisenEdistyminen | null = null

    async mounted() {
      this.edistyminen = (await getErikoistumisenEdistyminen()).data
      this.initializing = false
    }

    showOpintooikeusAlert(opintooikeudenPaattymispaiva: string) {
      return differenceInMonths(parseISO(opintooikeudenPaattymispaiva), new Date()) <= 6
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .col-min-width {
    min-width: 250px;
  }
</style>
