import { App } from 'vue'
import VueApexCharts from 'vue3-apexcharts'

export default {
  install(app: App) {
    app.use(VueApexCharts)
    // eslint-disable-next-line vue/multi-word-component-names
    app.component('Apexchart', VueApexCharts)
  }
}
