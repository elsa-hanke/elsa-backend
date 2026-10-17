import Vue from 'vue'
import VueScreen from 'vue-screen'
import Vuelidate from 'vuelidate'

import App from '@/app.vue'
import '@/plugins/apexcharts'
import '@/plugins/bootstrap-vue'
import '@/plugins/composition-api'
import '@/plugins/date'
import '@/plugins/filters'
import '@/plugins/fontawesome'
import i18n from '@/plugins/i18n'
import '@/plugins/registerComponentHooks'
import '@/plugins/roles'
import router from '@/router'
import store from '@/store'
import { installActiveContextSync } from '@/utils/active-context-sync'

import '@/styles/app.scss'
import 'mutationobserver-shim'

Vue.use(Vuelidate)
Vue.use(VueScreen, 'bootstrap')
Vue.config.productionTip = false

// ELSAINSI-73: pidetään aktiivinen rooli / opinto-oikeus synkronoituna välilehtien ja palvelimen välillä
installActiveContextSync(router)

new Vue({
  router,
  store,
  i18n,
  render: (h) => h(App)
}).$mount('#app')
