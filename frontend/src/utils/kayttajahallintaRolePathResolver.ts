import Vue from 'vue'

export function resolveRolePath() {
  if (Vue.prototype.$isVirkailija()) {
    return 'virkailija'
  } else if (Vue.prototype.$isTekninenPaakayttaja()) {
    return 'tekninen-paakayttaja'
  } else {
    return ''
  }
}
