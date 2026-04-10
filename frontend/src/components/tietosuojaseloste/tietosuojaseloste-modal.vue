<template>
  <b-modal
    id="tietosuoja-modal"
    :visible="value"
    :title="$t('tietosuojaseloste')"
    size="xl"
    :scrollable="true"
    :hide-footer="true"
    @hidden="$emit('input', false)"
  >
    <tietosuojaseloste-content />
  </b-modal>
</template>
<script lang="ts">
  import { Vue, Component, Prop } from 'vue-property-decorator'

  import TietosuojaselosteContent from '@/components/tietosuojaseloste/tietosuojaseloste-content.vue'

  @Component({
    components: { TietosuojaselosteContent }
  })
  export default class TietosuojaselosteModal extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('tietosuojaseloste'),
        active: true
      }
    ]

    @Prop({ required: true, type: Boolean, default: false })
    value!: boolean
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .tietosuojaseloste {
    max-width: 970px;
  }
</style>
