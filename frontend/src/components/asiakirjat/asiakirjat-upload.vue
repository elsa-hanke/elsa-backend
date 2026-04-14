<template>
  <div>
    <input
      v-if="allowMultiplesFiles"
      :id="uid"
      type="file"
      :disabled="uploading || disabled"
      multiple
      hidden
      @change="handleFileChange"
    />
    <input
      v-else
      :id="uid"
      type="file"
      :disabled="uploading || disabled"
      hidden
      @change="handleFileChange"
    />
    <label
      v-if="!isTextButton"
      class="user-select-none"
      :class="[isPrimaryButton ? 'primary mb-4' : 'outline-primary mb-4']"
      :for="uid"
      :disabled="uploading || disabled"
      v-on="$listeners"
    >
      <span>{{ buttonText }}</span>
      <b-spinner v-if="uploading" small class="ml-2"></b-spinner>
    </label>
    <div v-else>
      <span v-for="(item, index) in fileUploadTexts" :key="index">
        <template v-if="item.isLink">
          <a
            v-if="item.linkType === 'url'"
            :href="item.link"
            class="text-size-normal"
            rel="noreferrer noopener"
            target="_blank"
          >
            {{ item.text }}
          </a>
          <b-link
            v-if="item.linkType === 'navigation'"
            :href="getRouteHref(item.link)"
            target="_blank"
          >
            {{ item.text }}
          </b-link>
          <label v-else class="p-0" :for="uid" :disabled="uploading || disabled" v-on="$listeners">
            <span class="page-link font-weight-normal p-0" style="cursor: pointer">
              {{ item.text }}
            </span>
          </label>
        </template>
        <template v-else>
          {{ item.text }}
        </template>
        <span v-if="index < fileUploadTexts.length - 1"></span>
      </span>
      <b-spinner v-if="uploading" small class="ml-2"></b-spinner>
    </div>
    <b-alert variant="danger" :show="hasErrors" dismissible @dismissed="onDismissAlert">
      <div class="d-flex flex-row">
        <em class="align-middle">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
        </em>
        <div v-if="selectedFilesCount === 1">
          <h4>
            {{ $t('asiakirjan-tallentaminen-epaonnistui') }}
          </h4>
          <ul>
            <li v-if="duplicateFilesInCurrentView.length > 0">
              {{ $t('asiakirja-samanniminen-tiedosto') }}
            </li>
            <li v-if="duplicateFilesInOtherViews.length > 0">
              {{ $t('asiakirja-samanniminen-tiedosto-toisessa-nakymassa') }}
            </li>
            <li v-if="filesOfWrongType.length > 0">
              {{
                wrongFileTypeErrorMessage
                  ? wrongFileTypeErrorMessage
                  : $t('sallitut-tiedostoformaatit-default')
              }}
            </li>
            <li v-if="filesExceedingMaxSize.length > 0">
              {{ $t('asiakirjan-maksimi-tiedostokoko-ylitetty') }}
            </li>
            <li v-if="filesBelowMinSize.length > 0">
              {{ $t('asiakirjan-minimi-tiedostokoko-alitettu') }}
            </li>
          </ul>
        </div>
        <div v-else>
          <h4>
            {{ $t('asiakirjojen-tallentaminen-epaonnistui') }}
          </h4>
          <div class="mb-3">{{ $t('yhtakaan-tiedostoa-ei-tallennettu') }}</div>
          <div v-if="maxFilesTotalSizeExceeded" class="mb-2">
            {{ $t('asiakirjojen-yhteenlaskettu-koko-ylitetty') }}
          </div>
          <span v-if="duplicateFilesInCurrentView.length > 0">
            {{ $t('asiakirja-samanniminen-tiedosto') }}
            <ul>
              <li v-for="(file, index) in duplicateFilesInCurrentView" :key="index">
                {{ file.name }}
              </li>
            </ul>
          </span>
          <span v-if="duplicateFilesInOtherViews.length > 0">
            {{ $t('asiakirja-samanniminen-tiedosto-toisessa-nakymassa') }}
            <ul>
              <li v-for="(file, index) in duplicateFilesInOtherViews" :key="index">
                {{ file.name }}
              </li>
            </ul>
          </span>
          <span v-if="filesOfWrongType.length > 0">
            {{
              wrongFileTypeErrorMessage
                ? wrongFileTypeErrorMessage
                : $t('sallitut-tiedostoformaatit-default')
            }}
            <ul>
              <li v-for="(file, index) in filesOfWrongType" :key="index">
                {{ file.name }}
              </li>
            </ul>
          </span>
          <span v-if="filesExceedingMaxSize.length > 0">
            {{ $t('asiakirjan-maksimi-tiedostokoko-ylitetty') }}
            <ul>
              <li v-for="(file, index) in filesExceedingMaxSize" :key="index">
                {{ file.name }}
              </li>
            </ul>
          </span>
          <span v-if="filesBelowMinSize.length > 0">
            {{ $t('asiakirjan-minimi-tiedostokoko-alitettu') }}
            <ul>
              <li v-for="(file, index) in filesExceedingMaxSize" :key="index">
                {{ file.name }}
              </li>
            </ul>
          </span>
        </div>
      </div>
    </b-alert>
  </div>
</template>

<script lang="ts">
  import { Vue, Component, Prop } from 'vue-property-decorator'

  import { FileUploadText } from '@/types'

  // Maksimi tiedostokoko 20 Mt
  const maxFileSize = 20 * 1024 * 1024
  const maxFilesTotalSize = 100 * 1024 * 1024
  const minFileSize = 100
  const minPdfFileSize = 10 * 1024

  @Component
  export default class AsiakirjatUpload extends Vue {
    maxFilesTotalSizeExceeded = false
    filesExceedingMaxSize: File[] = []
    filesOfWrongType: File[] = []
    duplicateFilesInCurrentView: File[] = []
    duplicateFilesInOtherViews: File[] = []
    selectedFilesCount = 0
    filesBelowMinSize: File[] = []

    @Prop({ required: false })
    uploading?: boolean

    @Prop({ required: false, type: Boolean, default: true })
    isPrimaryButton!: boolean

    @Prop({ required: true, type: String })
    buttonText!: string

    @Prop({
      required: false,
      default: () => ['application/pdf', 'image/jpg', 'image/jpeg', 'image/png']
    })
    allowedFileTypes!: string[]

    @Prop({ required: false, type: String })
    wrongFileTypeErrorMessage?: string

    @Prop({ required: false, type: Boolean, default: true })
    allowMultiplesFiles!: boolean

    @Prop({ required: false, default: () => [] })
    existingFileNamesInCurrentView!: string[]

    @Prop({ required: false, default: () => [] })
    existingFileNamesInOtherViews!: string[]

    @Prop({ required: false, type: Boolean, default: false })
    disabled!: boolean

    @Prop({ required: false, type: Boolean, default: false })
    isTextButton!: boolean

    @Prop({ required: false, type: Array, default: () => [] })
    fileUploadTexts!: FileUploadText[]

    handleFileChange(e: Event) {
      const inputElement = e.target as HTMLInputElement
      const fileArray = [...(inputElement?.files ?? [])]
      this.selectedFilesCount = fileArray.length
      // Chromea varten. Muutoin heti perään valittu sama tiedosto ei laukaise koko eventtiä.
      inputElement.value = ''
      this.maxFilesTotalSizeExceeded = this.getIsTotalFileSizeExceeded(fileArray)
      this.filesExceedingMaxSize = this.getFilesExceedingMaxSize(fileArray)
      this.filesOfWrongType = this.getFilesOfWrongType(fileArray)
      this.duplicateFilesInCurrentView = this.getduplicateFilesInCurrentView(fileArray)
      this.duplicateFilesInOtherViews = this.getduplicateFilesInOtherViews(fileArray)
      this.filesBelowMinSize = this.getFilesBelowMinSize(fileArray)

      if (!this.hasErrors) {
        this.selectedFilesCount = 0
        this.$emit('selectedFiles', fileArray)
      }
    }

    getIsTotalFileSizeExceeded(files: File[]): boolean {
      return files.reduce((sum: number, current: File) => sum + current.size, 0) > maxFilesTotalSize
    }

    getFilesExceedingMaxSize(files: File[]): File[] {
      return files.filter((file) => file.size > maxFileSize)
    }

    getFilesOfWrongType(files: File[]): File[] {
      return files.filter((file) => !this.allowedFileTypes.includes(file.type))
    }

    getduplicateFilesInCurrentView(files: File[]): File[] {
      return files.filter((file) => this.existingFileNamesInCurrentView?.includes(file.name))
    }

    getduplicateFilesInOtherViews(files: File[]): File[] {
      return files.filter((file) => this.existingFileNamesInOtherViews?.includes(file.name))
    }

    // Yritetään suodattaa tyhjät tiedostot pois (PDF/10kt, muut 100b)
    getFilesBelowMinSize(files: File[]): File[] {
      return files.filter(
        (file) => file.size < (file.type === 'application/pdf' ? minPdfFileSize : minFileSize)
      )
    }

    onDismissAlert() {
      this.maxFilesTotalSizeExceeded = false
      this.filesExceedingMaxSize = []
      this.filesOfWrongType = []
      this.duplicateFilesInCurrentView = []
      this.duplicateFilesInOtherViews = []
      this.filesBelowMinSize = []
    }

    get uid() {
      return `elsa-asiakirjat-upload-${(this as any)._uid}`
    }

    get hasErrors() {
      return (
        this.maxFilesTotalSizeExceeded ||
        this.filesExceedingMaxSize.length > 0 ||
        this.filesOfWrongType.length > 0 ||
        this.duplicateFilesInCurrentView.length > 0 ||
        this.duplicateFilesInOtherViews.length > 0 ||
        this.filesBelowMinSize.length > 0
      )
    }

    getRouteHref(routeName: string) {
      return this.$router.resolve({ name: routeName }).href
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  label {
    display: inline-block;
    font-weight: 500;
    padding: 0.375rem 1.625rem;
    border-radius: 50rem;
    &[disabled] {
      opacity: 0.6;
    }
    &.primary {
      color: $white;
      background-color: $primary;
      border: 2px solid transparent;
      &:hover,
      &:active {
        &:not([disabled]) {
          background-color: darken($primary, 15);
          cursor: pointer;
        }
      }
    }
    &.outline-primary {
      color: $primary;
      background-color: transparent;
      border: 2px solid $primary;
      &:hover,
      &:active {
        &:not([disabled]) {
          color: $btn-primary-hover-background-color;
          border-color: $btn-primary-hover-border-color;
          cursor: pointer;
        }
      }
    }
  }
</style>
