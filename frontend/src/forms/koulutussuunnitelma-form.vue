<template>
  <b-form @submit.stop.prevent="onSubmit">
    <p class="mb-3">
      {{ $t('henkilokohtainen-koulutussuunnitelma-liitteena-kuvaus') }}
      <elsa-popover>
        {{ $t('sallitut-tiedostoformaatit-pdf') }}
      </elsa-popover>
    </p>
    <asiakirjat-upload
      :is-primary-button="false"
      :allow-multiples-files="false"
      :existing-file-names-in-current-view="motivaatiokirjeAsiakirjatTableItems.map((k) => k.nimi)"
      :existing-file-names-in-other-views="reservedAsiakirjaNimet"
      :button-text="$t('lisaa-liitetiedosto')"
      :wrong-file-type-error-message="$t('sallitut-tiedostoformaatit-pdf')"
      :allowed-file-types="['application/pdf']"
      @selectedFiles="onKoulutussuunnitelmaFileAdded"
    />
    <asiakirjat-content
      class="px-0 col-md-6 col-lg-12 col-xl-6"
      :asiakirjat="koulutussuunnitelmaAsiakirjatTableItems"
      :sorting-enabled="false"
      :pagination-enabled="false"
      :enable-search="false"
      :show-info-if-empty="false"
      @deleteAsiakirja="onKoulutussuunnitelmaFileDeleted"
    />
    <hr class="mt-0" />
    <elsa-form-group :label="$t('motivaatiokirje')">
      <template #label-help>
        <elsa-popover>
          {{ $t('sallitut-tiedostoformaatit-pdf') }}
        </elsa-popover>
      </template>
      <template #default="{ uid }">
        <p class="mb-3">{{ $t('motivaatiokirje-kuvas') }}</p>
        <asiakirjat-upload
          :is-primary-button="false"
          :allow-multiples-files="false"
          :existing-file-names-in-current-view="
            koulutussuunnitelmaAsiakirjatTableItems.map((k) => k.nimi)
          "
          :existing-file-names-in-other-views="reservedAsiakirjaNimet"
          :button-text="$t('lisaa-liitetiedosto')"
          :wrong-file-type-error-message="$t('sallitut-tiedostoformaatit-pdf')"
          :allowed-file-types="['application/pdf']"
          @selectedFiles="onMotivaatiokirjeFileAdded"
        />
        <asiakirjat-content
          class="px-0 col-md-6 col-lg-12 col-xl-6"
          :asiakirjat="motivaatiokirjeAsiakirjatTableItems"
          :sorting-enabled="false"
          :pagination-enabled="false"
          :enable-search="false"
          :show-info-if-empty="false"
          @deleteAsiakirja="onMotivaatiokirjeFileDeleted"
        />
        <b-form-textarea
          :id="uid"
          v-model="form.motivaatiokirje"
          rows="3"
          @input="$emit('skipRouteExitConfirm', false)"
        />
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('opiskelu-ja-tyohistoria')">
      <template #label-help>
        <elsa-popover>
          {{ $t('opiskelu-ja-tyohistoria-tooltip') }}
        </elsa-popover>
      </template>
      <template #label-right>
        <b-form-checkbox
          v-model="form.opiskeluJaTyohistoriaYksityinen"
          class="py-0"
          @input="$emit('skipRouteExitConfirm', false)"
        >
          {{ $t('piilota-kouluttajilta-kuvaus') }}
        </b-form-checkbox>
      </template>
      <template #default="{ uid }">
        <b-form-textarea
          :id="uid"
          v-model="form.opiskeluJaTyohistoria"
          rows="3"
          @input="$emit('skipRouteExitConfirm', false)"
        />
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('vahvuudet')">
      <template #label-help>
        <elsa-popover>
          {{ $t('vahvuudet-tooltip') }}
        </elsa-popover>
      </template>
      <template #label-right>
        <b-form-checkbox v-model="form.vahvuudetYksityinen" class="py-0">
          {{ $t('piilota-kouluttajilta-kuvaus') }}
        </b-form-checkbox>
      </template>
      <template #default="{ uid }">
        <b-form-textarea
          :id="uid"
          v-model="form.vahvuudet"
          rows="3"
          @input="$emit('skipRouteExitConfirm', false)"
        />
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('tulevaisuuden-visiointi')">
      <template #label-help>
        <elsa-popover>
          {{ $t('tulevaisuuden-visiointi-tooltip') }}
        </elsa-popover>
      </template>
      <template #label-right>
        <b-form-checkbox
          v-model="form.tulevaisuudenVisiointiYksityinen"
          class="py-0"
          @input="$emit('skipRouteExitConfirm', false)"
        >
          {{ $t('piilota-kouluttajilta-kuvaus') }}
        </b-form-checkbox>
      </template>
      <template #default="{ uid }">
        <b-form-textarea
          :id="uid"
          v-model="form.tulevaisuudenVisiointi"
          rows="3"
          @input="$emit('skipRouteExitConfirm', false)"
        />
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('osaamisen-kartuttaminen')">
      <template #label-help>
        <elsa-popover>
          {{ $t('osaamisen-kartuttaminen-tooltip') }}
        </elsa-popover>
      </template>
      <template #label-right>
        <b-form-checkbox
          v-model="form.osaamisenKartuttaminenYksityinen"
          class="py-0"
          @input="$emit('skipRouteExitConfirm', false)"
        >
          {{ $t('piilota-kouluttajilta-kuvaus') }}
        </b-form-checkbox>
      </template>
      <template #default="{ uid }">
        <b-form-textarea
          :id="uid"
          v-model="form.osaamisenKartuttaminen"
          rows="3"
          @input="$emit('skipRouteExitConfirm', false)"
        />
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('elamankentta')">
      <template #label-help>
        <elsa-popover>
          {{ $t('elamankentta-tooltip') }}
        </elsa-popover>
      </template>
      <template #label-right>
        <b-form-checkbox
          v-model="form.elamankenttaYksityinen"
          class="py-0"
          @input="$emit('skipRouteExitConfirm', false)"
        >
          {{ $t('piilota-kouluttajilta-kuvaus') }}
        </b-form-checkbox>
      </template>
      <template #default="{ uid }">
        <b-form-textarea
          :id="uid"
          v-model="form.elamankentta"
          rows="3"
          @input="$emit('skipRouteExitConfirm', false)"
        />
      </template>
    </elsa-form-group>
    <div class="d-flex flex-row-reverse flex-wrap">
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2 mb-2">
        {{ $t('tallenna') }}
      </elsa-button>
      <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
        {{ $t('peruuta') }}
      </elsa-button>
    </div>
    <div class="row">
      <elsa-form-error :active="$v.$anyError" />
    </div>
  </b-form>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Vue, Mixins, Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'

  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import { Koulutussuunnitelma } from '@/types'
  import { mapFile } from '@/utils/fileMapper'

  @Component({
    components: {
      AsiakirjatUpload,
      AsiakirjatContent,
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError,
      ElsaPopover
    },
    validations: {
      form: {}
    }
  })
  export default class KoulutussuunnitelmaForm extends Mixins(validationMixin) {
    @Prop({
      required: true
    })
    value!: Koulutussuunnitelma

    @Prop({ required: true })
    reservedAsiakirjaNimet!: string[]

    form: Koulutussuunnitelma = {
      id: null,
      motivaatiokirje: null,
      motivaatiokirjeYksityinen: false,
      opiskeluJaTyohistoria: null,
      opiskeluJaTyohistoriaYksityinen: false,
      vahvuudet: null,
      vahvuudetYksityinen: false,
      tulevaisuudenVisiointi: null,
      tulevaisuudenVisiointiYksityinen: false,
      osaamisenKartuttaminen: null,
      osaamisenKartuttaminenYksityinen: false,
      elamankentta: null,
      elamankenttaYksityinen: false,
      koulutussuunnitelmaAsiakirja: null,
      motivaatiokirjeAsiakirja: null,
      koulutussuunnitelmaFile: null,
      koulutussuunnitelmaAsiakirjaUpdated: false,
      motivaatiokirjeFile: null,
      motivaatiokirjeAsiakirjaUpdated: false,
      muokkauspaiva: null
    }

    params = {
      saving: false
    }

    mounted() {
      this.form = {
        ...this.value,
        koulutussuunnitelmaAsiakirjaUpdated: false,
        motivaatiokirjeAsiakirjaUpdated: false
      }
    }

    onKoulutussuunnitelmaFileAdded(files: File[]) {
      const file = files[0]
      Vue.set(this.form, 'koulutussuunnitelmaAsiakirja', mapFile(file))
      this.form.koulutussuunnitelmaFile = file
      this.form.koulutussuunnitelmaAsiakirjaUpdated = true
      this.$emit('skipRouteExitConfirm', false)
    }

    onKoulutussuunnitelmaFileDeleted() {
      Vue.set(this.form, 'koulutussuunnitelmaAsiakirja', null)
      this.form.koulutussuunnitelmaAsiakirjaUpdated = true
      this.$emit('skipRouteExitConfirm', false)
    }

    onMotivaatiokirjeFileAdded(files: File[]) {
      const file = files[0]
      Vue.set(this.form, 'motivaatiokirjeAsiakirja', mapFile(file))
      this.form.motivaatiokirjeFile = file
      this.form.motivaatiokirjeAsiakirjaUpdated = true
      this.$emit('skipRouteExitConfirm', false)
    }

    onMotivaatiokirjeFileDeleted() {
      Vue.set(this.form, 'motivaatiokirjeAsiakirja', null)
      this.form.motivaatiokirjeAsiakirjaUpdated = true
      this.$emit('skipRouteExitConfirm', false)
    }

    onSubmit() {
      this.$v.form.$touch()
      if (this.$v.form.$anyError) {
        return
      }
      this.$emit(
        'submit',
        {
          ...this.form,
          koulutussuunnitelmaAsiakirja: null,
          motivaatiokirjeAsiakirja: null
        },
        this.params
      )
    }

    onCancel() {
      this.$emit('cancel')
    }

    get koulutussuunnitelmaAsiakirjatTableItems() {
      return this.form.koulutussuunnitelmaAsiakirja ? [this.form.koulutussuunnitelmaAsiakirja] : []
    }

    get motivaatiokirjeAsiakirjatTableItems() {
      return this.form.motivaatiokirjeAsiakirja ? [this.form.motivaatiokirjeAsiakirja] : []
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep table {
    border-bottom: 0;
  }
</style>
