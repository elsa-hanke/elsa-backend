import { BToastPlugin, BootstrapVueNext } from 'bootstrap-vue-next'
import { App } from 'vue'
import '@/styles/bootstrap.scss'
import 'bootstrap-vue-next/dist/bootstrap-vue-next.css'

export default {
  install(app: App) {
    app.use(BootstrapVueNext)
    app.use(BToastPlugin)
  }
}
