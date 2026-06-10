/* eslint-disable @typescript-eslint/ban-types */
import { App } from 'vue'

import store from '@/store'
import { ELSA_ROLE } from '@/utils/roles'

export default {
  install(app: App) {
    app.config.globalProperties.$isErikoistuva = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.ErikoistuvaLaakari
    }

    app.config.globalProperties.$hasErikoistujaRole = (): boolean => {
      const authorities = store.getters['auth/account'].authorities
      return authorities.includes(ELSA_ROLE.ErikoistuvaLaakari)
    }

    app.config.globalProperties.$isYekKoulutettava = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.YEKKoulutettava
    }

    app.config.globalProperties.$hasYekRole = (): boolean => {
      const authorities = store.getters['auth/account'].authorities
      return authorities.includes(ELSA_ROLE.YEKKoulutettava)
    }

    app.config.globalProperties.$isKouluttaja = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.Kouluttaja
    }

    app.config.globalProperties.$hasKouluttajaRole = (): boolean => {
      const authorities = store.getters['auth/account'].authorities
      return authorities.includes(ELSA_ROLE.Kouluttaja)
    }

    app.config.globalProperties.$isVastuuhenkilo = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.Vastuuhenkilo
    }

    app.config.globalProperties.$isTerveyskeskuskoulutusjaksoVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return (
        data.activeAuthority === ELSA_ROLE.Vastuuhenkilo &&
        data.terveyskeskuskoulutusjaksoVastuuhenkilo
      )
    }

    app.config.globalProperties.$isYekTerveyskeskuskoulutusjaksoVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return (
        data.activeAuthority === ELSA_ROLE.Vastuuhenkilo &&
        data.yekTerveyskeskuskoulutusjaksoVastuuhenkilo
      )
    }

    app.config.globalProperties.$isValmistumisenVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return data.activeAuthority === ELSA_ROLE.Vastuuhenkilo && data.valmistumisenVastuuhenkilo
    }

    app.config.globalProperties.$isYekValmistumisenVastuuhenkilo = (): boolean => {
      const data = store.getters['auth/account']
      return data.activeAuthority === ELSA_ROLE.Vastuuhenkilo && data.yekValmistumisenVastuuhenkilo
    }

    app.config.globalProperties.$isTekninenPaakayttaja = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.TekninenPaakayttaja
    }

    app.config.globalProperties.$isVirkailija = (): boolean => {
      return store.getters['auth/account'].activeAuthority === ELSA_ROLE.OpintohallinnonVirkailija
    }
  }
}

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
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
