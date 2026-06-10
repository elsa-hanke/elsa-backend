import { createApp } from 'vue'

import App from '@/app.vue'
import apexchartsPlugin from '@/plugins/apexcharts'
import bootstrapVuePlugin from '@/plugins/bootstrap-vue'
import datePlugin from '@/plugins/date'
import fontawesomePlugin from '@/plugins/fontawesome'
import i18n from '@/plugins/i18n'
import rolesPlugin from '@/plugins/roles'
import screenPlugin from '@/plugins/screen'
import validationPlugin from '@/plugins/vuelidate'
import router from '@/router'
import store from '@/store'

import '@/styles/app.scss'

const app = createApp(App)

app.use(router)
app.use(store)
app.use(i18n)
app.use(bootstrapVuePlugin)
app.use(apexchartsPlugin)
app.use(fontawesomePlugin)
app.use(datePlugin)
app.use(rolesPlugin)
app.use(screenPlugin)
app.use(validationPlugin)

app.mount('#app')
