import Vue from 'vue'

export function resolveRolePath() {
  if (Vue.prototype.$isErikoistuva()) {
    return 'erikoistuva-laakari'
  } else if (Vue.prototype.$isVastuuhenkilo()) {
    return 'vastuuhenkilo'
  } else if (Vue.prototype.$isKouluttaja()) {
    return 'kouluttaja'
  } else if (Vue.prototype.$isVirkailija()) {
    return 'virkailija'
  } else {
    return ''
  }
}
