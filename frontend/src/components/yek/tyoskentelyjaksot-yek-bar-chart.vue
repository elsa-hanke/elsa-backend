<template>
  <div class="bar-chart">
    <elsa-bar-chart :value="barValues" />
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaBarChart from '@/components/bar-chart/bar-chart.vue'
  import { BarChartRow, TyoskentelyjaksotTilastot } from '@/types'

  @Component({
    components: {
      ElsaBarChart
    }
  })
  export default class TyoskentelyjaksotYekBarChart extends Vue {
    @Prop({ required: true, type: Object })
    tilastot!: TyoskentelyjaksotTilastot

    get barValues(): BarChartRow[] {
      if (this.tilastot) {
        return [
          {
            text: this.$t('terveyskeskus'),
            color: '#ffb406',
            backgroundColor: '#ffe19b',
            value: this.tilastot.koulutustyypit.terveyskeskusSuoritettu,
            minRequired: this.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan,
            maxLength: this.tilastot.koulutustyypit.terveyskeskusMaksimipituus,
            highlight: false,
            showMax: false
          },
          {
            text: this.$t('yek.sairaala'),
            color: '#0f9bd9',
            backgroundColor: '#9fd7ef',
            value: this.tilastot.koulutustyypit.yliopistosairaalaSuoritettu,
            minRequired: this.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan,
            highlight: false,
            showMax: false
          },
          {
            text: this.$t('muut'),
            color: '#8a86fb',
            backgroundColor: '#cfcdfd',
            value: this.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu,
            minRequired:
              this.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan,
            maxLength: this.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenMaksimipituus,
            highlight: false,
            showMax: true
          },
          {
            text: this.$t('yhteensa'),
            color: '#41b257',
            backgroundColor: '#b3e1bc',
            value: this.tilastot.koulutustyypit.yhteensaSuoritettu,
            minRequired: this.tilastot.koulutustyypit.yhteensaVaadittuVahintaan,
            highlight: true,
            showMax: false
          }
        ]
      } else {
        return []
      }
    }
  }
</script>
