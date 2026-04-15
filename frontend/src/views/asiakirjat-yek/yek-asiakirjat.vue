<template>
  <div class="asiakirjat">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('asiakirjat') }}</h1>
          <p>{{ $t('yek.asiakirjat-kuvaus') }}</p>
          <asiakirjat-upload
            v-if="!account.impersonated"
            :button-text="$t('lisaa-asiakirja')"
            :uploading="uploading"
            :existing-file-names-in-current-view="existingFileNames"
            @selectedFiles="onAsiakirjatAdded"
          >
            {{ $t('lisaa-asiakirja') }}
          </asiakirjat-upload>
          <asiakirjat-content
            :asiakirjat="asiakirjat"
            :loading="loading"
            :sort-by="sortBy"
            :confirm-delete-title="$t('poista-asiakirja')"
            :confirm-delete-type-text="$t('asiakirjan')"
            :enable-delete="!account.impersonated"
            @deleteAsiakirja="onDeleteAsiakirja"
          />
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios, { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import store from '@/store'
  import { Asiakirja, ElsaError } from '@/types'
  import { toastSuccess, toastFail } from '@/utils/toast'

  @Component({
    components: {
      AsiakirjatContent,
      AsiakirjatUpload
    }
  })
  export default class YekAsiakirjat extends Vue {
    endpointUrl = 'yek-koulutettava/asiakirjat'
    asiakirjat: Asiakirja[] = []
    loading = false
    uploading = false
    sortBy = 'lisattypvm'
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('asiakirjat'),
        active: true
      }
    ]

    async mounted() {
      await this.fetch()
    }

    get account() {
      return store.getters['auth/account']
    }

    async fetch() {
      try {
        this.loading = true
        this.asiakirjat = (await axios.get(this.endpointUrl)).data
      } catch {
        this.$t('asiakirjojen-haku-epaonnistui')
      }
      this.loading = false
    }

    async onAsiakirjatAdded(files: File[]) {
      this.uploading = true

      const formData = new FormData()
      files.forEach((file) => formData.append('files', file, file.name))

      try {
        const asiakirjat = (
          await axios.post(this.endpointUrl, formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            },
            timeout: 120000
          })
        ).data
        this.asiakirjat = [...asiakirjat, ...this.asiakirjat]
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        toastFail(this, this.getSubmitFailedMessage(axiosError, files.length))
        this.uploading = false
        return
      }

      toastSuccess(
        this,
        files.length > 1
          ? this.$t('asiakirjojen-tallentaminen-onnistui')
          : this.$t('asiakirjan-tallentaminen-onnistui')
      )

      this.uploading = false
    }

    private getSubmitFailedMessage(axiosError: AxiosError, filesCount: number) {
      const errorMessage =
        filesCount > 1
          ? this.$t('asiakirjojen-tallentaminen-epaonnistui')
          : this.$t('asiakirjan-tallentaminen-epaonnistui')
      const detailedMessage = axiosError?.response?.data?.message
      return detailedMessage ? `${errorMessage}: ${this.$t(detailedMessage)}` : errorMessage
    }

    async onDeleteAsiakirja(asiakirja: Asiakirja) {
      Vue.set(asiakirja, 'disableDelete', true)
      try {
        await axios.delete(this.endpointUrl + '/' + asiakirja.id)
        toastSuccess(this, this.$t('asiakirjan-poistaminen-onnistui'))
        this.asiakirjat = this.asiakirjat.filter((a) => a.id !== asiakirja.id)
      } catch {
        toastFail(this, this.$t('asiakirjan-poistaminen-epaonnistui'))
      }
      Vue.set(asiakirja, 'disableDelete', false)
    }

    get existingFileNames() {
      return this.asiakirjat.map((asiakirja) => asiakirja.nimi)
    }
  }
</script>

<style lang="scss" scoped>
  .asiakirjat {
    max-width: 1024px;
  }
</style>
