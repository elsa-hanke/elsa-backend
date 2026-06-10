/**
 * Vue 3 replacement for vue-screen.
 * Provides a reactive $screen object with Bootstrap breakpoint booleans.
 */
import { App, reactive } from 'vue'

function getBreakpoints() {
  const width = window.innerWidth
  return {
    xs: width < 576,
    sm: width >= 576,
    md: width >= 768,
    lg: width >= 992,
    xl: width >= 1200,
    xxl: width >= 1400
  }
}

const screen = reactive(getBreakpoints())

function updateScreen() {
  const bp = getBreakpoints()
  Object.assign(screen, bp)
}

export default {
  install(app: App) {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', updateScreen)
    }
    app.config.globalProperties.$screen = screen
  }
}

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $screen: {
      xs: boolean
      sm: boolean
      md: boolean
      lg: boolean
      xl: boolean
      xxl: boolean
    }
  }
}
