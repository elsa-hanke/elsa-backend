<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div v-html="safeHtml"></div>
</template>

<script lang="ts">
  import DOMPurify from 'dompurify'
  import { Component, Prop, Vue } from 'vue-property-decorator'

  @Component
  export default class IlmoitusContent extends Vue {
    @Prop({ type: String, default: null })
    html!: string | null

    get safeHtml() {
      // Keep this allowlist aligned with IlmoitusHtmlSanitizer. CSS and active content are excluded,
      // including for historical records that have never passed through server-side sanitization.
      return DOMPurify.sanitize(this.html || '', {
        ALLOWED_TAGS: [
          'p',
          'br',
          'h2',
          'h3',
          'h4',
          'strong',
          'b',
          'em',
          'i',
          'u',
          's',
          'ul',
          'ol',
          'li',
          'blockquote',
          'a',
          'table',
          'caption',
          'colgroup',
          'col',
          'thead',
          'tbody',
          'tfoot',
          'tr',
          'th',
          'td'
        ],
        ALLOWED_ATTR: ['href', 'title', 'colspan', 'rowspan', 'span', 'scope', 'start'],
        ALLOW_DATA_ATTR: false,
        ALLOW_ARIA_ATTR: false,
        ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[^a-z]|[a-z+.-]+(?:[^a-z+.:-]|$))/i
      })
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
