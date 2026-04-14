import { parseISO, differenceInMonths } from 'date-fns'
import { Component, Vue } from 'vue-property-decorator'

import { ELSA_API_LOCATION } from '@/api'
import { BarChartRow, TyoskentelyjaksotTilastot } from '@/types'
@Component({})
export default class KoulutettavienSeurantaMixin extends Vue {
  perPage = 20
  currentPage = 1

  vaihdaRooli(id: number) {
    window.location.href = `${ELSA_API_LOCATION}/api/login/impersonate?opintooikeusId=${id}`
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
        text: this.$t('yek.sairaala'),
        color: '#0f9bd9',
        backgroundColor: '#9fd7ef',
        value: tilastot.koulutustyypit.yliopistosairaalaSuoritettu,
        minRequired: tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan,
        highlight: false,
        showMax: false
      },
      {
        text: this.$t('muut'),
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
