import { parseISO, differenceInMonths } from 'date-fns'
import { Component, Vue } from 'vue-property-decorator'

import { ELSA_API_LOCATION } from '@/api'
import { BarChartRow, TyoskentelyjaksotTilastot } from '@/types'
import { LomakeTilat } from '@/utils/constants'
@Component({})
export default class ErikoistujienSeurantaMixin extends Vue {
  perPage = 20
  currentPage = 1

  vaihdaRooli(id: number) {
    window.location.href = `${ELSA_API_LOCATION}/api/login/impersonate?opintooikeusId=${id}`
  }

  koejaksoTila(tila: LomakeTilat) {
    switch (tila) {
      case LomakeTilat.EI_AKTIIVINEN:
        return (this.$t('aloittamatta') as string).toLowerCase()
      case LomakeTilat.ODOTTAA_HYVAKSYNTAA:
        return (this.$t('kesken') as string).toLowerCase()
      case LomakeTilat.HYVAKSYTTY:
        return (this.$t('hyvaksytty') as string).toLowerCase()
    }
  }

  koejaksoTyyli(tila: LomakeTilat, paattymispaiva: string) {
    switch (tila) {
      case LomakeTilat.EI_AKTIIVINEN:
      case LomakeTilat.ODOTTAA_HYVAKSYNTAA:
        if (differenceInMonths(parseISO(paattymispaiva), new Date()) <= 12) {
          return 'text-danger'
        }
        break
      case LomakeTilat.HYVAKSYTTY:
        return 'text-success'
    }
  }

  opintoOikeusTyyli(paattymispaiva: string) {
    if (differenceInMonths(parseISO(paattymispaiva), new Date()) <= 6) {
      return 'text-danger'
    }
  }

  barValues(tilastot: TyoskentelyjaksotTilastot): BarChartRow[] {
    return [
      {
        text: this.$t('terveyskeskus'),
        color: '#ffb406',
        backgroundColor: '#ffe19b',
        value: tilastot.koulutustyypit.terveyskeskusSuoritettu,
        minRequired: tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan,
        highlight: false,
        showMax: false
      },
      {
        text: this.$t('yliopistosairaala'),
        color: '#0f9bd9',
        backgroundColor: '#9fd7ef',
        value: tilastot.koulutustyypit.yliopistosairaalaSuoritettu,
        minRequired: tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan,
        highlight: false,
        showMax: false
      },
      {
        text: this.$t('yo-sair-ulkopuolinen'),
        color: '#8a86fb',
        backgroundColor: '#cfcdfd',
        value: tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu,
        minRequired: tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan,
        highlight: false,
        showMax: false
      }
    ]
  }
}
