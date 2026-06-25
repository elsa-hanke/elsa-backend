/**
 * Shim for vue-property-decorator to fix Jest compatibility.
 *
 * vue-property-decorator@9.1.2 incorrectly re-exports `Vue` and `Component`
 * as the plain Vue 3 namespace object (not constructors/functions) from its UMD
 * build. This shim overrides them with the proper exports from vue-class-component.
 */
const vcc = require('vue-class-component')
const umd = require('vue-property-decorator/lib/index.umd.js')

module.exports = {
  ...umd,
  Vue: vcc.Vue,
  Component: vcc.Options
}
