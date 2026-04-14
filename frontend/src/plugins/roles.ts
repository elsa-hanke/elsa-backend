/* eslint-disable @typescript-eslint/ban-types */
import Vue from 'vue'

import store from '@/store'
import { ELSA_ROLE } from '@/utils/roles'

export class RolesPlugin {
  public install(vue: typeof Vue) {
    vue.prototype.$isErikoistuva = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.ErikoistuvaLaakari
    }

    vue.prototype.$hasErikoistujaRole = (): boolean => {
      const authorities = store.getters['auth/account'].authorities
      return authorities.includes(ELSA_ROLE.ErikoistuvaLaakari)
    }

    vue.prototype.$isYekKoulutettava = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.YEKKoulutettava
    }

    vue.prototype.$hasYekRole = (): boolean => {
      const authorities = store.getters['auth/account'].authorities
      return authorities.includes(ELSA_ROLE.YEKKoulutettava)
    }

    vue.prototype.$isKouluttaja = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.Kouluttaja
    }

    vue.prototype.$hasKouluttajaRole = (): boolean => {
      const authorities = store.getters['auth/account'].authorities
      return authorities.includes(ELSA_ROLE.Kouluttaja)
    }

    vue.prototype.$isVastuuhenkilo = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.Vastuuhenkilo
    }

    vue.prototype.$isTerveyskeskuskoulutusjaksoVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return (
        data.activeAuthority === ELSA_ROLE.Vastuuhenkilo &&
        data.terveyskeskuskoulutusjaksoVastuuhenkilo
      )
    }

    vue.prototype.$isYekTerveyskeskuskoulutusjaksoVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return (
        data.activeAuthority === ELSA_ROLE.Vastuuhenkilo &&
        data.yekTerveyskeskuskoulutusjaksoVastuuhenkilo
      )
    }

    vue.prototype.$isValmistumisenVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return data.activeAuthority === ELSA_ROLE.Vastuuhenkilo && data.valmistumisenVastuuhenkilo
    }

    vue.prototype.$isYekValmistumisenVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return data.activeAuthority === ELSA_ROLE.Vastuuhenkilo && data.yekValmistumisenVastuuhenkilo
    }

    vue.prototype.$isTekninenPaakayttaja = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.TekninenPaakayttaja
    }

    vue.prototype.$isVirkailija = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.OpintohallinnonVirkailija
    }
  }
}

declare module 'vue/types/vue' {
  interface Vue {
    $isErikoistuva: Function
    $isYekKoulutettava: Function
    $isKouluttaja: Function
    $isVastuuhenkilo: Function
    $isTerveyskeskuskoulutusjaksoVastuuhenkilo: Function
    $isYekTerveyskeskuskoulutusjaksoVastuuhenkilo: Function
    $isValmistumisenVastuuhenkilo: Function
    $isYekValmistumisenVastuuhenkilo: Function
    $isTekninenPaakayttaja: Function
    $isVirkailija: Function
    $hasKouluttajaRole: Function
    $hasYekRole: Function
    $hasErikoistujaRole: Function
  }
}

Vue.use(new RolesPlugin())
