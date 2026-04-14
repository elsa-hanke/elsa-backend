<template>
  <div>
    <editor v-bind="$attrs" :init="init" v-on="$listeners" />
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

  @Component({
    components: {
      Editor
    }
  })
  export default class ElsaTextEditor extends Vue {
    get init() {
      return {
        statusbar: false,
        height: 500,
        menubar: false,
        content_css: false,
        skin: false,
        plugins: ['lists', 'link', 'table', 'code'],
        block_formats: 'Paragraph=p;Header 2=h2;Header 3=h3;Header 4=h4',
        toolbar: 'undo redo | bold | blocks | numlist bullist | link | table | code'
      }
    }
  }
</script>
