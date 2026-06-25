import { Component } from 'vue-property-decorator'

// Register custom component hooks (for vue-class-component v8 / Vue 3)
Component.registerHooks(['beforeRouteEnter', 'beforeRouteLeave', 'beforeRouteUpdate'])
