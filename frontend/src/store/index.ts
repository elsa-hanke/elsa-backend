import { createStore } from 'vuex'

import auth from './auth'
import erikoistuva from './erikoistuva'
import kouluttaja from './kouluttaja'
import vastuuhenkilo from './vastuuhenkilo'

export default createStore({
  modules: {
    auth,
    erikoistuva,
    kouluttaja,
    vastuuhenkilo
  }
})
