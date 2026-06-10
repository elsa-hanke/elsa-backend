import { App } from 'vue'

import { useVuelidate } from '@/utils/validators'

export default {
  install(app: App) {
    app.provide('$vuelidate', useVuelidate)
  }
}
