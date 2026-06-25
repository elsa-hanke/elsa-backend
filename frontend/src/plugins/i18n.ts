import { createI18n } from 'vue-i18n'

function loadLocaleMessages() {
  const locales = require.context('../locales', true, /[A-Za-z0-9-_,\s]+\.json$/i)
  const messages: Record<string, any> = {}
  locales.keys().forEach((key: string) => {
    const matched = key.match(/([A-Za-z0-9-_]+)\./i)
    if (matched && matched.length > 1) {
      const locale = matched[1]
      messages[locale] = locales(key)
    }
  })
  return messages
}

const i18n = createI18n({
  legacy: true,
  locale: 'fi',
  fallbackLocale: 'fi',
  messages: loadLocaleMessages()
})

export default i18n
