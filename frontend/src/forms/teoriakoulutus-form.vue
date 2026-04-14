<template>
  <b-form @submit.stop.prevent="onSubmit">
    <elsa-form-group :label="$t('koulutuksen-nimi')" :required="true">
      <template #default="{ uid }">
        <b-form-input
          :id="uid"
          v-model="form.koulutuksenNimi"
          :state="validateState('koulutuksenNimi')"
          @input="$emit('skipRouteExitConfirm', false)"
        ></b-form-input>
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('paikka')" :required="true">
      <template #default="{ uid }">
        <b-form-input
          :id="uid"
          v-model="form.koulutuksenPaikka"
          :state="validateState('koulutuksenPaikka')"
          @input="$emit('skipRouteExitConfirm', false)"
        ></b-form-input>
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <b-form-row>
      <elsa-form-group
        :label="$t('alkamispaiva')"
        class="col-xs-12 col-sm-6 pr-sm-3"
        :required="true"
      >
        <template #default="{ uid }">
          <div>
            <elsa-form-datepicker
              v-if="childDataReceived"
              :id="uid"
              ref="alkamispaiva"
              :value.sync="form.alkamispaiva"
              :max="form.paattymispaiva"
              :max-error-text="$t('alkamispaiva-ei-voi-olla-paattymispaivan-jalkeen')"
              @input="$emit('skipRouteExitConfirm', false)"
            ></elsa-form-datepicker>
          </div>
        </template>
      </elsa-form-group>
      <elsa-form-group :label="$t('paattymispaiva')" class="col-xs-12 col-sm-6 pl-sm-3">
        <template #default="{ uid }">
          <elsa-form-datepicker
            v-if="childDataReceived"
            :id="uid"
            ref="paattymispaiva"
            :value.sync="form.paattymispaiva"
            :min="form.alkamispaiva"
            :min-error-text="$t('paattymispaiva-ei-voi-olla-ennen-alkamispaivaa')"
            :required="false"
            :aria-describedby="`${uid}-help`"
            class="datepicker-range"
            @input="$emit('skipRouteExitConfirm', false)"
          />
        </template>
      </elsa-form-group>
    </b-form-row>
    <elsa-form-group :label="$t('erikoistumiseen-hyvaksyttava-tuntimaara')">
      <template #default="{ uid }">
        <div>
          <div class="d-flex align-items-center">
            <b-form-input
              :id="uid"
              :value="form.erikoistumiseenHyvaksyttavaTuntimaara"
              type="number"
              step="any"
              class="col-sm-3"
              @input="onErikoistumiseenHyvaksyttavaTuntimaaraInput"
            />
            <span class="mx-3">{{ $t('t') }}</span>
          </div>
        </div>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('todistus')">
      <template #default="{ uid }">
        <div :id="uid">
          <p>{{ $t('teoriakoulutus-todistus-kuvaus') }}</p>
          <asiakirjat-upload
            :is-primary-button="false"
            :button-text="$t('lisaa-liitetiedosto')"
            :existing-file-names-in-current-view="existingFileNamesInCurrentView"
            :existing-file-names-in-other-views="existingFileNamesInOtherViews"
            :disabled="reservedAsiakirjaNimetMutable === undefined"
            @selectedFiles="onFilesAdded"
          />
          <asiakirjat-content
            :asiakirjat="asiakirjatTableItems"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :show-info-if-empty="false"
            @deleteAsiakirja="onDeleteLiitetiedosto"
          />
        </div>
      </template>
    </elsa-form-group>
    <hr v-if="asiakirjatTableItems.length === 0" />
    <div class="d-flex flex-row-reverse flex-wrap">
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2 mb-2">
        {{ $t('tallenna-teoriakoulutus') }}
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
  import axios from 'axios'
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import { Asiakirja, Teoriakoulutus } from '@/types'
  import { mapFiles } from '@/utils/fileMapper'

  @Component({
    components: {
      ElsaFormError,
      ElsaFormGroup,
      ElsaFormDatepicker,
      ElsaButton,
      AsiakirjatContent,
      AsiakirjatUpload
    },
    validations: {
      form: {
        koulutuksenNimi: {
          required
        },
        koulutuksenPaikka: {
          required
        }
      }
    }
  })
  export default class TeoriakoulutusForm extends Mixins(validationMixin) {
    $refs!: {
      alkamispaiva: ElsaFormDatepicker
      paattymispaiva: ElsaFormDatepicker
    }

    @Prop({
      required: false,
      type: Object
    })
    value!: Teoriakoulutus

    form: Partial<Teoriakoulutus> = {
      koulutuksenNimi: null,
      koulutuksenPaikka: null,
      paattymispaiva: null,
      erikoistumiseenHyvaksyttavaTuntimaara: null,
      todistukset: []
    }
    params = {
      saving: false
    }

    addedFiles: File[] = []
    reservedAsiakirjaNimetMutable: string[] = []
    newAsiakirjatMapped: Asiakirja[] = []
    deletedAsiakirjat: Asiakirja[] = []
    childDataReceived = false

    async mounted() {
      if (this.value?.id) {
        this.form = {
          ...this.value
        }
      } else {
        this.form = {
          koulutuksenNimi: null,
          koulutuksenPaikka: null,
          paattymispaiva: null,
          erikoistumiseenHyvaksyttavaTuntimaara: null,
          todistukset: []
        }
      }
      this.childDataReceived = true

      this.reservedAsiakirjaNimetMutable = (
        await axios.get('erikoistuva-laakari/asiakirjat/nimet')
      ).data
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    onErikoistumiseenHyvaksyttavaTuntimaaraInput(value: string) {
      if (value === '') {
        this.form.erikoistumiseenHyvaksyttavaTuntimaara = null
      } else {
        this.form.erikoistumiseenHyvaksyttavaTuntimaara = parseFloat(value)
      }
      this.$emit('skipRouteExitConfirm', false)
    }

    onFilesAdded(files: File[]) {
      const addedFilesInDeletedArray = files.filter((added) =>
        this.deletedAsiakirjat.map((deleted) => deleted.nimi).includes(added.name)
      )
      const addedFilesNotInDeletedArray = files.filter(
        (added) => !addedFilesInDeletedArray.includes(added)
      )

      this.deletedAsiakirjat = this.deletedAsiakirjat?.filter(
        (deletedAsiakirja) =>
          !addedFilesInDeletedArray
            .map((addedFile) => addedFile.name)
            .includes(deletedAsiakirja.nimi)
      )
      this.addedFiles = [...this.addedFiles, ...addedFilesNotInDeletedArray]
      this.newAsiakirjatMapped = [
        ...mapFiles(addedFilesNotInDeletedArray),
        ...this.newAsiakirjatMapped
      ]
      this.$emit('skipRouteExitConfirm', false)
    }

    async onDeleteLiitetiedosto(asiakirja: Asiakirja) {
      if (asiakirja.id) {
        this.deletedAsiakirjat = [asiakirja, ...this.deletedAsiakirjat]
      } else {
        this.addedFiles = this.addedFiles?.filter((file) => file.name !== asiakirja.nimi)
        this.newAsiakirjatMapped = this.newAsiakirjatMapped?.filter(
          (a) => a.nimi !== asiakirja.nimi
        )
      }
      this.$emit('skipRouteExitConfirm', false)
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.alkamispaiva.validateForm(),
        this.$refs.paattymispaiva.validateForm()
      ]

      if (validations.includes(false)) {
        return
      }

      this.$emit(
        'submit',
        {
          teoriakoulutus: {
            ...(this.form.id ? { id: this.form.id } : {}),
            koulutuksenNimi: this.form.koulutuksenNimi,
            koulutuksenPaikka: this.form.koulutuksenPaikka,
            alkamispaiva: this.form.alkamispaiva,
            paattymispaiva: this.form.paattymispaiva,
            erikoistumiseenHyvaksyttavaTuntimaara: this.form.erikoistumiseenHyvaksyttavaTuntimaara
          },
          addedFiles: this.addedFiles,
          deletedAsiakirjaIds: this.deletedAsiakirjat.map((asiakirja) => asiakirja.id)
        },
        this.params
      )
    }

    onCancel() {
      this.$emit('cancel')
    }

    get existingFileNamesInCurrentView() {
      return this.asiakirjatTableItems?.map((item) => item.nimi)
    }

    get existingFileNamesInOtherViews() {
      return this.reservedAsiakirjaNimetMutable?.filter(
        (nimi) => !this.existingFileNamesInCurrentView.includes(nimi)
      )
    }

    get asiakirjatTableItems() {
      return [...this.newAsiakirjatMapped, ...this.asiakirjatExcludingDeleted()]
    }

    private asiakirjatExcludingDeleted(): Asiakirja[] {
      return (this.value?.todistukset ?? []).filter(
        (asiakirja) =>
          !this.deletedAsiakirjat.map((deleted) => deleted.nimi).includes(asiakirja.nimi)
      )
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .datepicker-range::before {
    content: '–';
    position: absolute;
    left: -1.5rem;
    padding: 0.375rem 0.75rem;
    @include media-breakpoint-down(xs) {
      display: none;
    }
  }
</style>
