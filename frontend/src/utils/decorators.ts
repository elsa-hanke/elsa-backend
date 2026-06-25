// Vue-meta has been removed. The @Meta decorator is now a no-op.
// Use document.title directly or @unhead/vue for page title management.
import { createDecorator, VueDecorator } from 'vue-property-decorator'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const Meta: VueDecorator = createDecorator((options: any, key: string) => {
  if (!options.methods) {
    return
  }
  // Store metaInfo for potential future use but it won't be processed by vue-meta
  options['metaInfo'] = options.methods[key]
})
