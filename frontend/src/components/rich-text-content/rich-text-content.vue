<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div v-html="safeHtml"></div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import { sanitizeRichText } from '@/utils/richText'

  @Component
  export default class RichTextContent extends Vue {
    @Prop({ type: String, default: null })
    html!: string | null

    get safeHtml() {
      return sanitizeRichText(this.html)
    }
  }
</script>

<style lang="scss" scoped>
  ::v-deep table {
    border-collapse: collapse;
    max-width: 100%;
  }

  ::v-deep th,
  ::v-deep td {
    border: 1px solid #dee2e6;
    padding: 0.25rem 0.5rem;
  }
</style>
