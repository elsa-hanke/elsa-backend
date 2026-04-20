/* eslint-disable @typescript-eslint/no-var-requires */
const { gitDescribeSync } = require('git-describe')

try {
  process.env.VUE_APP_COMMIT_HASH = gitDescribeSync().hash
} catch {
  console.warn('Cannot get Git commit hash')
}

if (!process.env.VUE_APP_VERSION) {
  try {
    const hash = gitDescribeSync().hash
    process.env.VUE_APP_VERSION = hash ? hash.replace(/^g/, '').substring(0, 6) : 'dev'
  } catch {
    process.env.VUE_APP_VERSION = 'dev'
  }
}

module.exports = {
  lintOnSave: 'default',

  configureWebpack: {
    resolve: {
      alias: {
        vue$: 'vue/dist/vue.esm.js'
      }
    }
  },

  chainWebpack: (config) => {
    config.plugin('html').tap((args) => {
      args[0].title = 'ELSA-palvelu'
      return args
    })
  },

  devServer: {
    client: {
      overlay: {
        errors: true,
        warnings: true
      }
    },
    port: 9060,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        secure: false
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        secure: false
      },
      '/auth': {
        target: 'http://localhost:8080',
        secure: false
      },
      '/management': {
        target: 'http://localhost:8080',
        secure: false
      }
    }
  },

  pluginOptions: {
    i18n: {
      locale: 'fi',
      fallbackLocale: 'fi',
      localeDir: 'locales',
      enableInSFC: true
    }
  }
}
