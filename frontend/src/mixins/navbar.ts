import { Component, Vue } from 'vue-property-decorator'

import { ELSA_API_LOCATION, vaihdaRooli } from '@/api'
import { patchOpintooikeusKaytossa, getErikoistuvaLaakari } from '@/api/erikoistuva'
import store from '@/store'
import { Opintooikeus } from '@/types'
import { sortByDateDesc } from '@/utils/date'
import { getTitleFromAuthorities } from '@/utils/functions'
import { ELSA_ROLE } from '@/utils/roles'

@Component({})
export default class NavbarMixin extends Vue {
  opintooikeusKaytossa?: null | Opintooikeus = null
  opintooikeudet: null | Opintooikeus[] = null

  async mounted() {
    if (this.isErikoistuvaLaakari) {
      const erikoistuvaLaakari = (await getErikoistuvaLaakari()).data
      this.opintooikeudet = erikoistuvaLaakari.opintooikeudet
        .filter((o: Opintooikeus) => o.erikoisalaId !== 61)
        .sort((a: Opintooikeus, b: Opintooikeus) =>
          sortByDateDesc(a.opintooikeudenMyontamispaiva, b.opintooikeudenMyontamispaiva)
        )
      this.opintooikeusKaytossa = this.getOpintooikeusKaytossa(
        erikoistuvaLaakari.opintooikeusKaytossaId
      )
    } else if (this.isYekKoulutettava) {
      const erikoistuvaLaakari = (await getErikoistuvaLaakari()).data
      this.opintooikeudet = erikoistuvaLaakari.opintooikeudet
        .filter((o: Opintooikeus) => o.erikoisalaId === 61)
        .sort((a: Opintooikeus, b: Opintooikeus) =>
          sortByDateDesc(a.opintooikeudenMyontamispaiva, b.opintooikeudenMyontamispaiva)
        )
      this.opintooikeusKaytossa = this.getOpintooikeusKaytossa(
        erikoistuvaLaakari.opintooikeusKaytossaId
      )
    }
  }

  get isErikoistuvaLaakari() {
    return this.activeRole === ELSA_ROLE.ErikoistuvaLaakari
  }

  get isYekKoulutettava() {
    return this.activeRole === ELSA_ROLE.YEKKoulutettava
  }

  get currentLocale() {
    return this.$i18n.locale
  }

  get account() {
    return store.getters['auth/account']
  }

  get authorities() {
    if (this.account) {
      if (this.account.impersonated) {
        return this.account.originalUser.authorities
      }
      return this.account.authorities
    }
    return []
  }

  get activeRole(): string {
    if (this.account) {
      return this.account.activeAuthority
    }
    return ''
  }

  get erikoisalaNimi() {
    return this.opintooikeusKaytossa?.erikoisalaNimi
  }

  get avatar() {
    if (this.account) {
      if (this.account.impersonated) {
        return this.account.originalUser.avatar
      }
      return this.account.avatar
    }
    return undefined
  }

  get locales() {
    return Object.keys(this.$i18n.messages)
  }

  get logoutUrl() {
    return ELSA_API_LOCATION + '/api/logout'
  }

  get displayName() {
    if (this.account) {
      if (this.account.impersonated) {
        return `${this.account.originalUser.firstName} ${this.account.originalUser.lastName}`
      }
      return `${this.account.firstName} ${this.account.lastName}`
    }
    return ''
  }

  get title() {
    if (this.account.impersonated) {
      return getTitleFromAuthorities(this, this.account.originalUser.activeAuthority)
    }
    if (this.isErikoistuvaLaakari || this.isYekKoulutettava) {
      return this.erikoisalaNimi
    }
    return getTitleFromAuthorities(this, this.activeRole)
  }

  getOpintooikeusKaytossa(id: number) {
    return this.opintooikeudet?.find((o) => o.id === id)
  }

  changeLocale(lang: string) {
    this.$i18n.locale = lang
  }

  async logout() {
    await store.dispatch('auth/logout')

    if (store.getters['auth/sloEnabled'] === true) {
      const logoutForm = this.$refs.logoutForm as HTMLFormElement
      logoutForm.submit()
    }
  }

  async changeOpintooikeus(opintooikeus: Opintooikeus) {
    if (this.opintooikeusKaytossa === opintooikeus) return
    this.opintooikeusKaytossa = opintooikeus
    await patchOpintooikeusKaytossa(opintooikeus.id)
    this.$router.go(0)
  }

  async changeToErikoistuja() {
    if (this.$isErikoistuva()) return
    await vaihdaRooli(ELSA_ROLE.ErikoistuvaLaakari)
    this.routeToEtusivu()
  }

  async changeToYekKoulutettava() {
    if (this.$isYekKoulutettava()) return
    await vaihdaRooli(ELSA_ROLE.YEKKoulutettava)
    this.routeToEtusivu()
  }

  async changeToKouluttaja() {
    if (this.$isKouluttaja()) return
    await vaihdaRooli(ELSA_ROLE.Kouluttaja)
    this.routeToEtusivu()
  }

  routeToEtusivu() {
    if (this.$router.currentRoute.path.includes('etusivu')) {
      this.$router.go(0)
    } else {
      this.$router.push({ name: 'etusivu' }).then(() => {
        this.$router.go(0)
      })
    }
  }
}
