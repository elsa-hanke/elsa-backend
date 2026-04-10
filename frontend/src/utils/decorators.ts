import { ComponentOptions } from 'vue'
import { createDecorator, VueDecorator } from 'vue-class-component'
import { Vue } from 'vue-property-decorator'

export const Meta: VueDecorator = createDecorator((options: ComponentOptions<Vue>, key: string) => {
  if (!options.methods) {
    return
  }
  options['metaInfo'] = options.methods[key]
})
