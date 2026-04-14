import { Component, Vue } from 'vue-property-decorator'

import store from '@/store'
import { KayttajatiliTila } from '@/utils/constants'
import { ELSA_ROLE } from '@/utils/roles'

@Component({})
export default class KayttajahallintaMixin extends Vue {
  get account() {
    return store.getters['auth/account']
  }

  get isVirkailija() {
    return this.account.authorities.includes(ELSA_ROLE.OpintohallinnonVirkailija)
  }

  getTilaColor(tila: KayttajatiliTila) {
    switch (tila) {
      case KayttajatiliTila.AKTIIVINEN:
        return 'text-success'
      case KayttajatiliTila.PASSIIVINEN:
        return 'text-danger'
      default:
        return ''
    }
  }
}
