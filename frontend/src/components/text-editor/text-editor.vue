<template>
  <div>
    <editor v-bind="editorAttrs" :init="init" v-on="editorListeners" />
  </div>
</template>

<script lang="ts">
  import 'tinymce/tinymce'
  import 'tinymce/themes/silver'
  import 'tinymce/icons/default'
  import 'tinymce/skins/ui/oxide/skin.css'
  import 'tinymce/models/dom/model'
  import 'tinymce/plugins/code/plugin'
  import 'tinymce/plugins/lists/plugin'
  import 'tinymce/plugins/link/plugin'
  import 'tinymce/plugins/table/plugin'

  import Editor from '@tinymce/tinymce-vue'
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop, Watch } from 'vue-property-decorator'

  import { restrictedRichTextEditorOptions, sanitizeRichText } from '@/utils/richText'

  @Component({
    components: {
      Editor
    }
  })
  export default class ElsaTextEditor extends Vue {
    @Prop({ type: Boolean, default: false })
    restrictedRichText!: boolean

    editorValue: string | null | undefined = undefined
    pendingEditorValue: { value: string } | null = null

    @Watch('$attrs.value', { immediate: true })
    onValueChanged(value: string | null | undefined) {
      const echo = this.pendingEditorValue !== null && this.pendingEditorValue.value === value
      this.pendingEditorValue = null
      // Preserve TinyMCE's exact serialization on the v-model round trip. Otherwise
      // named entities become literal characters and its watcher calls setContent
      // during typing, replacing the selection. External values still get cleaned.
      this.editorValue = this.restrictedRichText && !echo ? sanitizeRichText(value) : value
    }

    @Watch('restrictedRichText')
    onRestrictedModeChanged() {
      this.pendingEditorValue = null
      this.onValueChanged(this.$attrs.value as string | undefined)
    }

    onEditorInput(value: string) {
      this.pendingEditorValue = { value }
      this.editorValue = value
      this.$emit('input', value)
    }

    get editorAttrs() {
      return this.restrictedRichText ? { ...this.$attrs, value: this.editorValue } : this.$attrs
    }

    get editorListeners() {
      return this.restrictedRichText
        ? { ...this.$listeners, input: this.onEditorInput }
        : this.$listeners
    }

    get init() {
      return {
        statusbar: false,
        height: 500,
        menubar: false,
        content_css: false,
        skin: false,
        plugins: ['lists', 'link', 'table', 'code'],
        block_formats: 'Paragraph=p;Header 2=h2;Header 3=h3;Header 4=h4',
        toolbar: 'undo redo | bold | blocks | numlist bullist | link | table | code',
        ...(this.restrictedRichText ? restrictedRichTextEditorOptions : {})
      }
    }
  }
</script>
