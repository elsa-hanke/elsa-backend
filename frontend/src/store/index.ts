import Vue from 'vue'
import Vuex, { Store } from 'vuex'

import auth from './auth'
import erikoistuva from './erikoistuva'
import kouluttaja from './kouluttaja'
import vastuuhenkilo from './vastuuhenkilo'

Vue.use(Vuex)

export default new Store({
  modules: {
    auth,
    erikoistuva,
    kouluttaja,
    vastuuhenkilo
  }
})
