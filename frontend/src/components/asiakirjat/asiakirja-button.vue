<template>
  <elsa-button variant="link" class="shadow-none p-0 mt-2 d-block" @click="onDownloadAsiakirja()">
    <font-awesome-icon :icon="['far', 'file-alt']" fixed-width size="lg" />
    {{ asiakirjaLabel }}
  </elsa-button>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import { fetchAndSaveBlob } from '@/utils/blobs'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaSearchInput,
      ElsaPagination
    }
  })
  export default class AsiakirjaButton extends Vue {
    @Prop({ required: true, type: String, default: 'erikoistuva-laakari/asiakirjat/' })
    asiakirjaDataEndpointUrl!: string

    @Prop({ required: true, type: String })
    asiakirjaLabel!: string

    @Prop({ required: true, type: Number })
    id!: number

    async onDownloadAsiakirja() {
      const success = await fetchAndSaveBlob(
        this.asiakirjaDataEndpointUrl,
        this.asiakirjaLabel,
        this.id
      )
      if (!success) {
        toastFail(this, this.$t('asiakirjan-sisallon-hakeminen-epaonnistui'))
      }
    }
  }
</script>
