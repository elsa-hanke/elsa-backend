declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
  export default component
}

// vue-avatar is replaced by a local component - kept for compatibility
declare module 'vue-avatar' {
  const Avatar: DefineComponent<any, any, any>
  export default Avatar
}
