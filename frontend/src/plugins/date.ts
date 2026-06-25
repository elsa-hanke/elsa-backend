/* eslint-disable no-unused-vars */
import { format, parseISO, formatISO } from 'date-fns'
import { enUS, fi, sv } from 'date-fns/locale'
import { reactive, App } from 'vue'

import VueI18n from '@/plugins/i18n'

export const durationOptions = reactive({
  showInDays: false
})

export default {
  install(app: App) {
    function parseAndFormat(value: string, pattern: string) {
      const date = parseISO(value)
      let locale
      const currentLocale = (VueI18n as any).global?.locale?.value ?? (VueI18n as any).locale
      switch (currentLocale) {
        case 'sv':
          locale = sv
          break
        case 'en':
          locale = enUS
          break
        default:
          locale = fi
          break
      }
      return format(date, pattern, { locale })
    }

    app.config.globalProperties.$date = function (value: string) {
      return parseAndFormat(value, 'P')
    }
    app.config.globalProperties.$today = function () {
      return parseAndFormat(formatISO(new Date()), 'P')
    }

    app.config.globalProperties.$datetime = function (value: string) {
      if (!value) {
        return ''
      }
      return parseAndFormat(value, 'Pp')
    }

    app.config.globalProperties.$duration = function (value: number) {
      if (durationOptions.showInDays) {
        return `${Math.round(value)} vrk`
      }

      const years = value / 365
      const months = (years - Math.trunc(years)) * 12
      const days = (months - Math.trunc(months)) * 30.5

      const res = []
      if (Math.trunc(years) > 0) {
        res.push(`${Math.trunc(years)} v`)
      }
      if (Math.trunc(months) > 0) {
        res.push(`${Math.trunc(months)} kk`)
      }
      if (Math.round(days) > 0) {
        res.push(`${Math.round(days)} vrk`)
      }
      if (res.length > 0) {
        return res.join(' ')
      } else {
        return '0 vrk'
      }
    }
  }
}

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $date: (value: string) => string
    $today: () => string
    $datetime: (value: string) => string
    $duration: (value: number) => string
  }
}
