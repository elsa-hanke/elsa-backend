import {
  FontAwesomeIcon,
  FontAwesomeLayers,
  FontAwesomeLayersText
} from '@fortawesome/vue-fontawesome'
import { App } from 'vue'

import './fontawesome-solid'
import './fontawesome-regular'

export default {
  install(app: App) {
    app.component('FontAwesomeIcon', FontAwesomeIcon)
    app.component('FontAwesomeLayers', FontAwesomeLayers)
    app.component('FontAwesomeLayersText', FontAwesomeLayersText)
  }
}
