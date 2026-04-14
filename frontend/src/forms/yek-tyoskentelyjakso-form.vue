<template>
  <b-form @submit.stop.prevent="onSubmit">
    <div v-if="!laillistamisTiedotForm.laillistamistiedotAdded">
      <elsa-form-group
        class="col-xs-12 col-sm-4 pl-0"
        :label="$t('yek.valviran-laillistamispaiva')"
      >
        <template #default="{ uid }">
          <elsa-form-datepicker
            :id="uid"
            ref="laillistamispaiva"
            :value.sync="laillistamisTiedotForm.laillistamispaiva"
            @input="$emit('skipRouteExitConfirm', false)"
          ></elsa-form-datepicker>
        </template>
      </elsa-form-group>
      <elsa-form-group :label="$t('laillistamispaivan-liitetiedosto')" :required="true">
        <span>
          {{ $t('lisaa-liite-joka-todistaa-laillistamispaivan') }}
        </span>
        <asiakirjat-upload
          class="mt-3"
          :is-primary-button="false"
          :allow-multiples-files="false"
          :button-text="$t('lisaa-liitetiedosto')"
          :disabled="laillistamispaivaAsiakirjat.length > 0"
          @selectedFiles="onLaillistamispaivaFilesAdded"
        />
        <div v-if="laillistamispaivaAsiakirjat.length > 0">
          <asiakirjat-content
            :asiakirjat="laillistamispaivaAsiakirjat"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :enable-delete="true"
            :enable-lisatty="false"
            :no-results-info-text="$t('ei-liitetiedostoja')"
            @deleteAsiakirja="onDeleteLaillistamispaivanLiite"
          />
        </div>
        <div v-else>
          <b-alert variant="dark" class="mt-3" show>
            <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
            <span>
              {{ $t('ei-asiakirjoja') }}
            </span>
          </b-alert>
        </div>
      </elsa-form-group>
    </div>
    <div v-if="isEnsimmainenTyoskentelyjakso">
      <elsa-form-group :label="$t('yek.aiempi-laakarikoulutus')" :required="true">
        <template #default="{ uid }">
          <div>
            <b-form-radio
              key="laakarikoulutus-suomi-belgia"
              v-model="laillistamisTiedotForm.laakarikoulutusSuoritettuSuomiTaiBelgia"
              name="laakarikoulutus"
              :value="true"
              @input="$emit('skipRouteExitConfirm', false)"
              @change="aiempiLaakarikoulutusChange('suomibelgia')"
            >
              <span>
                {{ $t('yek.aiempi-laakarikoulutus-olen-suorittanut-suomi-belgia') }}
              </span>
            </b-form-radio>
            <b-form-radio
              key="laakarikoulutus-muu"
              v-model="laillistamisTiedotForm.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia"
              name="laakarikoulutus"
              :value="true"
              @input="$emit('skipRouteExitConfirm', false)"
              @change="aiempiLaakarikoulutusChange('muu')"
            >
              <span>
                {{ $t('yek.aiempi-laakarikoulutus-olen-suorittanut-muu-kuin-suomi-belgia') }}
              </span>
            </b-form-radio>
            <b-form-invalid-feedback
              :id="`${uid}-feedback`"
              :style="{
                display:
                  !laillistamisTiedotForm.laakarikoulutusSuoritettuSuomiTaiBelgia &&
                  !laillistamisTiedotForm.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
                    ? 'block'
                    : 'none'
              }"
            >
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </div>
        </template>
      </elsa-form-group>
    </div>
    <hr v-if="!laillistamisTiedotForm.laillistamistiedotAdded || isEnsimmainenTyoskentelyjakso" />
    <elsa-form-group :label="$t('tyyppi')" :required="!value.tapahtumia">
      <template #default="{ uid }">
        <div>
          <b-form-radio
            v-for="(tyyppi, index) in tyypit"
            :key="index"
            v-model="form.tyoskentelypaikka.tyyppi"
            :state="validateState('tyoskentelypaikka.tyyppi')"
            name="tyoskentelyjakso-tyyppi"
            :value="tyyppi.value"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            <span>
              {{ tyyppi.text }}
            </span>
          </b-form-radio>
          <b-form-radio
            v-model="form.tyoskentelypaikka.tyyppi"
            :state="validateState('tyoskentelypaikka.tyyppi')"
            name="tyoskentelyjakso-tyyppi"
            value="MUU"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            <span v-if="form.tyoskentelypaikka.tyyppi === 'MUU'">
              {{ $t('muu') }},
              <span class="text-lowercase">{{ $t('kerro-mika') }}</span>
              <span class="text-primary">*</span>
            </span>
            <span v-else>
              {{ $t('muu') }}
            </span>
          </b-form-radio>
          <div v-if="form.tyoskentelypaikka.tyyppi === 'MUU'" class="pl-4">
            <b-form-input
              v-model="form.tyoskentelypaikka.muuTyyppi"
              :state="validateState('tyoskentelypaikka.muuTyyppi')"
              @input="$emit('skipRouteExitConfirm', false)"
            ></b-form-input>
            <b-form-invalid-feedback>{{ $t('pakollinen-tieto') }}</b-form-invalid-feedback>
          </div>
          <span v-if="form.tyoskentelypaikka.tyyppi === 'MUU'" class="text-muted small">
            {{ $t('yek.työskentelyjakson-lisays-muu-ohje') }}
          </span>
          <b-form-invalid-feedback
            :id="`${uid}-feedback`"
            :style="{
              display: validateState('tyoskentelypaikka.tyyppi') === false ? 'block' : 'none'
            }"
          >
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('tyoskentelypaikka')" :required="!value.tapahtumia">
      <template #default="{ uid }">
        <div>
          <b-form-input
            :id="uid"
            v-model="form.tyoskentelypaikka.nimi"
            :state="validateState('tyoskentelypaikka.nimi')"
            @input="$emit('skipRouteExitConfirm', false)"
          ></b-form-input>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('kunta')" :required="!value.tapahtumia">
      <template #default="{ uid }">
        <div>
          <elsa-form-multiselect
            :id="uid"
            v-model="form.tyoskentelypaikka.kunta"
            :options="kunnatFormatted"
            :state="validateState('tyoskentelypaikka.kunta')"
            label="abbreviation"
            track-by="id"
            @input="$emit('skipRouteExitConfirm', false)"
          />
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <b-form-row>
      <elsa-form-group
        :label="$t('alkamispaiva')"
        class="col-xs-12 col-sm-6 pr-sm-3"
        :required="!value.tapahtumia"
      >
        <template #default="{ uid }">
          <div>
            <elsa-form-datepicker
              v-if="childDataReceived"
              :id="uid"
              ref="alkamispaiva"
              :value.sync="form.alkamispaiva"
              :max="maxAlkamispaiva"
              :max-error-text="
                value.tapahtumia
                  ? $t('tyoskentelyjakso-datepicker-help')
                  : $t('alkamispaiva-ei-voi-olla-paattymispaivan-jalkeen')
              "
              @input="$emit('skipRouteExitConfirm', false)"
            ></elsa-form-datepicker>
            <small v-if="value.tapahtumia" class="form-text text-muted">
              {{ $t('tyoskentelyjakso-paattymispaiva-help') }}
            </small>
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
            :min="minPaattymispaiva"
            :min-error-text="
              value.tapahtumia
                ? $t('tyoskentelyjakso-datepicker-help')
                : $t('paattymispaiva-ei-voi-olla-ennen-alkamispaivaa')
            "
            :required="false"
            :aria-describedby="`${uid}-help`"
            class="datepicker-range"
            @input="$emit('skipRouteExitConfirm', false)"
          />
          <small v-if="value.tapahtumia" class="form-text text-muted">
            {{ $t('tyoskentelyjakso-paattymispaiva-help') }}
          </small>
          <small v-else class="form-text text-muted">
            {{ $t('jata-tyhjaksi-jos-ei-tiedossa') }}
          </small>
        </template>
      </elsa-form-group>
    </b-form-row>
    <elsa-form-group
      :label="$t('tyoaika-taydesta-tyopaivasta') + ' (50–100 %)'"
      :required="!value.tapahtumia"
    >
      <template #default="{ uid }">
        <div>
          <div class="d-flex align-items-center">
            <b-form-input
              :id="uid"
              :value="form.osaaikaprosentti"
              :state="validateState('osaaikaprosentti')"
              type="number"
              step="any"
              class="col-sm-3"
              @input="onOsaaikaprosenttiInput"
            />
            <span class="mx-3">%</span>
          </div>
          <small class="d-flex form-text text-muted">
            {{ $t('alle-50-osaaikaisuus-ei-kerryta') }}
          </small>
          <b-form-invalid-feedback
            :id="`${uid}-feedback`"
            :style="{
              display: validateState('osaaikaprosentti') === false ? 'block' : 'none'
            }"
          >
            {{ $t('osaaikaprosentti-validointivirhe') }} 50–100 %
          </b-form-invalid-feedback>
        </div>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('liitetiedostot')">
      <template #label-help>
        <elsa-popover>
          {{ $t('sallitut-tiedostoformaatit-default') }}
        </elsa-popover>
      </template>
      <span>
        {{ $t('tyoskentelyjakson-liitetiedostot-kuvaus') }}
      </span>
      <asiakirjat-upload
        class="mt-3"
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
    </elsa-form-group>
    <hr v-if="asiakirjatTableItems.length === 0" />
    <div
      :class="{ 'mt-4': asiakirjatTableItems.length > 0 }"
      class="d-flex flex-row-reverse flex-wrap"
    >
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2 mb-2">
        {{ editing ? $t('tallenna') : $t('lisaa') }}
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
  import { Prop, Vue } from 'vue-property-decorator'
  import { required, between, requiredIf, integer } from 'vuelidate/lib/validators'

  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import {
    Asiakirja,
    Laillistamistiedot,
    LaillistamistiedotLomakeKoulutettava,
    Tyoskentelyjakso,
    TyoskentelyjaksoForm
  } from '@/types'
  import { TyoskentelyjaksoTyyppi } from '@/utils/constants'
  import { mapFile, mapFiles } from '@/utils/fileMapper'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'
  import { tyoskentelypaikkaTyyppiLabel } from '@/utils/tyoskentelyjakso'
  import KouluttajaKoulutussopimusForm from '@/views/koejakso/kouluttaja/kouluttaja-koulutussopimus-form.vue'

  @Component({
    components: {
      KouluttajaKoulutussopimusForm,
      AsiakirjatContent,
      AsiakirjatUpload,
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      ElsaFormDatepicker,
      ElsaPopover
    },
    validations: {
      form: {
        tyoskentelypaikka: {
          required,
          nimi: {
            required
          },
          kunta: {
            required
          },
          tyyppi: {
            required
          },
          muuTyyppi: {
            required: requiredIf((value) => {
              return value.tyyppi === 'MUU'
            })
          }
        },
        osaaikaprosentti: {
          required,
          integer,
          between: between(50, 100)
        }
      }
    }
  })
  export default class YekTyoskentelyjaksoForm extends Vue {
    $refs!: {
      laillistamispaiva: ElsaFormDatepicker
      alkamispaiva: ElsaFormDatepicker
      paattymispaiva: ElsaFormDatepicker
    }
    laillistamispaivaAsiakirjat: Asiakirja[] = []

    @Prop({ required: false, default: true })
    allowHyvaksyttyAiemminToiselleErikoisalalleOption!: boolean

    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: false, default: () => [] })
    kunnat!: any[]

    @Prop({ required: false, default: undefined })
    asiakirjat?: Asiakirja[]

    @Prop({ required: false, default: undefined })
    reservedAsiakirjaNimet?: string[]

    @Prop({
      required: false,
      type: Object,
      default: () => ({
        alkamispaiva: null,
        paattymispaiva: null,
        osaaikaprosentti: null,
        tyoskentelypaikka: {
          nimi: null,
          kunta: {},
          tyyppi: null,
          muuTyyppi: null
        },
        kaytannonKoulutus: null,
        omaaErikoisalaaTukeva: null,
        hyvaksyttyAiempaanErikoisalaan: null
      })
    })
    value!: Tyoskentelyjakso

    addedFiles: File[] = []
    newAsiakirjatMapped: Asiakirja[] = []
    deletedAsiakirjat: Asiakirja[] = []
    reservedAsiakirjaNimetMutable: string[] | undefined = []
    ensimmainenTyoskentelyjakso: Tyoskentelyjakso | null = null
    isEnsimmainenTyoskentelyjakso = false

    form: TyoskentelyjaksoForm = {
      alkamispaiva: null,
      paattymispaiva: null,
      minPaattymispaiva: null,
      maxAlkamispaiva: null,
      osaaikaprosentti: 100,
      tyoskentelypaikka: {
        nimi: null,
        kunta: { abbreviation: null },
        tyyppi: null,
        muuTyyppi: null
      },
      kaytannonKoulutus: null,
      omaaErikoisalaaTukeva: null,
      hyvaksyttyAiempaanErikoisalaan: null
    }
    tyypit = [
      { text: this.$t('terveyskeskus'), value: TyoskentelyjaksoTyyppi.TERVEYSKESKUS },
      {
        text: this.$t('keskussairaala-tai-aluesairaala'),
        value: TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
      },
      {
        text: this.$t('yliopistollinen-sairaala'),
        value: TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
      }
    ]
    params = {
      saving: false,
      deleting: false
    }
    childDataReceived = false

    laillistamisTiedotForm: LaillistamistiedotLomakeKoulutettava = {
      laillistamistiedotAdded: false,
      ensimmainenTyoskentelyjakso: false,
      laillistamispaiva: null,
      laillistamispaivanLiite: null,
      laakarikoulutusSuoritettuSuomiTaiBelgia: false,
      laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: false
    }

    async mounted() {
      this.form = {
        ...this.value
      }

      this.reservedAsiakirjaNimetMutable = this.reservedAsiakirjaNimet
      if (!this.reservedAsiakirjaNimetMutable) {
        this.reservedAsiakirjaNimetMutable = (
          await axios.get('yek-koulutettava/asiakirjat/nimet')
        ).data
      }

      this.loadLaillistamisTiedot()
      this.fetchEnsimmainenTyoskentelyjakso()

      this.childDataReceived = true
    }

    async fetchEnsimmainenTyoskentelyjakso() {
      try {
        this.ensimmainenTyoskentelyjakso = (
          await axios.get(`yek-koulutettava/ensimmainen-tyoskentelyjakso`)
        ).data
        this.isEnsimmainenTyoskentelyjakso = this.ensimmainenTyoskentelyjakso?.id === this.form.id
        this.laillistamisTiedotForm.ensimmainenTyoskentelyjakso = this.isEnsimmainenTyoskentelyjakso
      } catch {
        this.isEnsimmainenTyoskentelyjakso = false
        toastFail(this, this.$t('yek.ensimmaisen-tyoskentelyjakson-hakeminen-epaonnistui'))
      }
    }

    validateState(name: string) {
      const get = (obj: any, path: any, defaultValue = undefined) => {
        const travel = (regexp: any) =>
          String.prototype.split
            .call(path, regexp)
            .filter(Boolean)
            .reduce((res, key) => (res !== null && res !== undefined ? res[key] : res), obj)
        const result = travel(/[,[\]]+?/) || travel(/[,[\].]+?/)
        return result === undefined || result === obj ? defaultValue : result
      }
      const { $dirty, $error } = get(this.$v.form, name)
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    async onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.alkamispaiva ? this.$refs.alkamispaiva.validateForm() : true,
        this.$refs.paattymispaiva.validateForm(),
        this.$refs.laillistamispaiva ? this.$refs.laillistamispaiva.validateForm() : true
      ]

      if (
        this.isEnsimmainenTyoskentelyjakso &&
        !this.laillistamisTiedotForm.laakarikoulutusSuoritettuSuomiTaiBelgia &&
        !this.laillistamisTiedotForm.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
      ) {
        return
      }

      if (validations.includes(false)) {
        return
      }

      if (
        !this.laillistamisTiedotForm.laillistamistiedotAdded &&
        this.laillistamisTiedotForm.laillistamispaivanLiite == null &&
        this.laillistamispaivaAsiakirjat.length > 0
      ) {
        const file = this.laillistamispaivaAsiakirjat[0]
        const data = await file.data
        if (data) {
          this.laillistamisTiedotForm.laillistamispaivanLiite = new File([data], file.nimi || '', {
            type: file.contentType || ''
          })
        }
      }

      const submitData = {
        tyoskentelyjakso: {
          ...this.form,
          tyoskentelypaikka: {
            ...this.form.tyoskentelypaikka,
            kuntaId: this.form.tyoskentelypaikka.kunta?.id
          },
          omaaErikoisalaaTukevaId: this.form.omaaErikoisalaaTukeva?.id
        },
        addedFiles: this.addedFiles,
        deletedAsiakirjaIds: this.deletedAsiakirjat.map((asiakirja) => asiakirja.id),
        laillistamistiedot: this.laillistamisTiedotForm
      }

      delete submitData.tyoskentelyjakso.asiakirjat

      this.$emit('submit', submitData, this.params)
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
      // Jos asiakirjalla on id, on se tallennettu kantaan jo aiemmin, joten
      // lisää asiakirja poistettaviin asiakirjoihin.
      if (asiakirja.id) {
        this.deletedAsiakirjat = [asiakirja, ...this.deletedAsiakirjat]
      } else {
        // Jos asiakirjalla ei ole id:tä, on se lisätty ensimmäistä kertaa
        // tässä näkymässä, joten poista asiakirja lisättävistä tiedostoista.
        this.addedFiles = this.addedFiles?.filter((file) => file.name !== asiakirja.nimi)
        this.newAsiakirjatMapped = this.newAsiakirjatMapped?.filter(
          (a) => a.nimi !== asiakirja.nimi
        )
      }
      this.$emit('skipRouteExitConfirm', false)
    }

    onOsaaikaprosenttiInput(value: string) {
      if (value === '') {
        this.form.osaaikaprosentti = null
      } else {
        this.form.osaaikaprosentti = parseFloat(value)
      }
      this.$emit('skipRouteExitConfirm', false)
    }

    onCancel() {
      this.$emit('cancel')
    }

    get maxAlkamispaiva() {
      if (this.value.tapahtumia) {
        return this.form.maxAlkamispaiva
      } else {
        return this.form.paattymispaiva
      }
    }

    get minPaattymispaiva() {
      if (this.value.tapahtumia) {
        return this.form.minPaattymispaiva
      } else {
        return this.form.alkamispaiva
      }
    }

    get kunnatFormatted() {
      return this.kunnat
        .filter((k) => !k.korvaavaKoodi) // Rajattu pois entiset kunnat
        .filter((k) => !['000', '198', '199'].includes(k.id)) // Rajattu pois muut kuin kunnat
        .sort((a, b) => sortByAsc(a.abbreviation, b.abbreviation))
    }

    get asiakirjatTableItems() {
      return [...this.newAsiakirjatMapped, ...this.asiakirjatExcludingDeleted()]
    }

    get existingFileNamesInCurrentView() {
      return this.asiakirjatTableItems?.map((item) => item.nimi)
    }

    get existingFileNamesInOtherViews() {
      return this.reservedAsiakirjaNimetMutable?.filter(
        (nimi) => !this.existingFileNamesInCurrentView.includes(nimi)
      )
    }

    get tyyppiLabel() {
      if (this.form?.tyoskentelypaikka?.tyyppi) {
        return tyoskentelypaikkaTyyppiLabel(this, this.form?.tyoskentelypaikka?.tyyppi)
      }
      return undefined
    }

    private asiakirjatExcludingDeleted(): Asiakirja[] {
      return (this.asiakirjat ?? []).filter(
        (asiakirja) =>
          !this.deletedAsiakirjat.map((deleted) => deleted.nimi).includes(asiakirja.nimi)
      )
    }

    async loadLaillistamisTiedot() {
      try {
        const laillistamistiedot: Laillistamistiedot = (
          await axios.get('/yek-koulutettava/laillistamispaiva')
        ).data
        this.laillistamisTiedotForm.laillistamispaiva = laillistamistiedot.laillistamispaiva
        this.laillistamisTiedotForm.laillistamistiedotAdded =
          laillistamistiedot.laillistamispaiva !== null

        if (laillistamistiedot.laillistamistodistus) {
          const data = Uint8Array.from(atob(laillistamistiedot.laillistamistodistus), (c) =>
            c.charCodeAt(0)
          )
          this.laillistamispaivaAsiakirjat.push(
            mapFile(
              new File([data], laillistamistiedot.laillistamistodistusNimi || '', {
                type: laillistamistiedot.laillistamistodistusTyyppi || ''
              })
            )
          )
        }

        if (laillistamistiedot.laakarikoulutusSuoritettuSuomiTaiBelgia) {
          this.laillistamisTiedotForm.laakarikoulutusSuoritettuSuomiTaiBelgia =
            laillistamistiedot.laakarikoulutusSuoritettuSuomiTaiBelgia
        }
        if (laillistamistiedot.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia) {
          this.laillistamisTiedotForm.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia =
            laillistamistiedot.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
        }
      } catch {
        toastFail(this, this.$t('laillistamispaivan-hakeminen-epaonnistui'))
      }
    }

    onLaillistamispaivaFilesAdded(files: File[]) {
      this.laillistamisTiedotForm.laillistamispaivanLiite = files[0]
      this.laillistamispaivaAsiakirjat.push(...mapFiles(files))
    }

    async onDeleteLaillistamispaivanLiite() {
      this.laillistamisTiedotForm.laillistamispaivanLiite = null
      this.laillistamispaivaAsiakirjat = []
    }

    aiempiLaakarikoulutusChange(option: string) {
      if (option === 'suomibelgia') {
        this.laillistamisTiedotForm.laakarikoulutusSuoritettuSuomiTaiBelgia = true
        this.laillistamisTiedotForm.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia = false
      } else {
        this.laillistamisTiedotForm.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia = true
        this.laillistamisTiedotForm.laakarikoulutusSuoritettuSuomiTaiBelgia = false
      }
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
