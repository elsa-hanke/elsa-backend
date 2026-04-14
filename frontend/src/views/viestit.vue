<template>
  <div class="viestit">
    {{ $t('viestit') }}
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  @Component({
    components: {}
  })
  export default class Viestit extends Vue {}
</script>
