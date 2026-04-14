import Vue from 'vue'
import { helpers } from 'vuelidate/lib/validators'

import { ELSA_ROLE } from './roles'

export const clamp = (num: number, clamp: number, higher: number) =>
  higher ? Math.min(Math.max(num, clamp), higher) : Math.min(num, clamp)

export function checkCurrentRouteAndRedirect(router: any, path: any) {
  const {
    currentRoute: { path: curPath }
  } = router
  if (curPath !== path) router.push({ path })
}

export function getTitleFromAuthorities(vue: Vue, authority: string) {
  if (authority === ELSA_ROLE.YEKKoulutettava) {
    return vue.$t('yek.yek-koulutettava')
  } else if (authority === ELSA_ROLE.ErikoistuvaLaakari) {
    return vue.$t('erikoistuja')
  } else if (authority === ELSA_ROLE.Kouluttaja) {
    return vue.$t('kouluttaja')
  } else if (authority === ELSA_ROLE.Vastuuhenkilo) {
    return vue.$t('vastuuhenkilo')
  } else if (authority === ELSA_ROLE.TekninenPaakayttaja) {
    return vue.$t('tekninen-paakayttaja')
  } else if (authority === ELSA_ROLE.OpintohallinnonVirkailija) {
    return vue.$t('virkailija')
  }

  return undefined
}

export function wrapToFormData(form: { [key: string]: any }): FormData {
  const formData = new FormData()
  for (const [key, value] of Object.entries(form)) {
    if (value !== null) {
      if (value instanceof File) {
        formData.append(key, value)
      } else if (value instanceof Object) {
        formData.append(key, JSON.stringify(value))
      } else {
        formData.append(key, value)
      }
    }
  }
  return formData
}

export function phoneNumberValidation() {
  return helpers.regex('serial', RegExp('/^\\+[0-9]*$/'))
}
