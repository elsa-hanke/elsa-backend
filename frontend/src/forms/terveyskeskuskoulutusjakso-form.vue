<template>
  <b-form v-if="hyvaksynta != null" @submit.stop.prevent="onSubmit">
    <erikoistuva-details
      :avatar="hyvaksynta.erikoistuvanAvatar"
      :name="hyvaksynta.erikoistuvanNimi"
      :erikoisala="hyvaksynta.erikoistuvanErikoisala"
      :opiskelijatunnus="hyvaksynta.erikoistuvanOpiskelijatunnus"
      :yliopisto="hyvaksynta.erikoistuvanYliopisto"
      :syntymaaika="hyvaksynta.erikoistuvanSyntymaaika"
      :show-birthdate="true"
      :yek="yek"
    />
    <div class="table-responsive">
      <table
        class="table table-borderless border-0 table-sm erikoistuva-details-table"
        :summary="$t('terveyskeskuskoulutuksen-yhteenlaskettu-kesto')"
      >
        <tr class="sr-only">
          <th scope="col">{{ $t('kentta') }}</th>
          <th scope="col">{{ $t('arvo') }}</th>
        </tr>
        <tr>
          <th scope="row" style="width: 12rem" class="align-middle font-weight-500">
            {{ $t('terveyskeskuskoulutuksen-yhteenlaskettu-kesto') }}
          </th>
          <td class="erikoistuja-details-padding">
            {{ $duration(hyvaksynta.terveyskeskuskoulutusjaksonKesto) }}
          </td>
        </tr>
        <tr
          v-if="
            hyvaksynta.laillistamispaiva != null &&
            hyvaksynta.laillistamispaivanLiite != null &&
            !laillistaminenMuokattavissa
          "
        >
          <th scope="row" class="align-middle font-weight-500">
            {{ $t('laillistamispaiva') }}
          </th>
          <td class="erikoistuja-details-padding">
            {{ $date(hyvaksynta.laillistamispaiva) }} -
            <elsa-button variant="link" class="pl-0" @click="onDownloadLaillistamistodistus">
              {{ hyvaksynta.laillistamispaivanLiitteenNimi }}
            </elsa-button>
            <div v-if="editable && (isHakija || $isVirkailija())">
              <b-link @click="muokkaaLaillistamista">
                <font-awesome-icon class="feedback-icon" :icon="['fa', 'edit']" fixed-width />
                {{ $t('muokkaa') }}
              </b-link>
            </div>
          </td>
        </tr>
        <tr v-if="!isHakija && hyvaksynta.asetus != null">
          <th scope="row" class="align-middle font-weight-500">
            {{ $t('asetus') }}
          </th>
          <td class="erikoistuja-details-padding">
            {{ hyvaksynta.asetus }}
          </td>
        </tr>
      </table>
    </div>
    <div v-if="laillistaminenMuokattavissa">
      <elsa-form-group
        class="col-xs-12 col-sm-3 pl-0"
        :label="$t('laillistamispaiva')"
        :required="true"
      >
        <template #default="{ uid }">
          <elsa-form-datepicker
            :id="uid"
            ref="laillistamispaiva"
            :value.sync="form.laillistamispaiva"
            @input="onSkipRouteExitConfirm"
          ></elsa-form-datepicker>
          <b-form-invalid-feedback>
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
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
        <asiakirjat-content
          :asiakirjat="laillistamispaivaAsiakirjat"
          :sorting-enabled="false"
          :pagination-enabled="false"
          :enable-search="false"
          :enable-delete="true"
          :no-results-info-text="$t('ei-liitetiedostoja')"
          :state="validateState('laillistamispaivanLiite')"
          @deleteAsiakirja="onDeleteLaillistamispaivanLiite"
        />
        <b-form-invalid-feedback :state="validateState('laillistamispaivanLiite')">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </elsa-form-group>
    </div>
    <elsa-button
      v-if="$isVirkailija()"
      variant="outline-primary"
      class="mt-2 mb-4"
      @click="vaihdaRooli(hyvaksynta ? hyvaksynta.opintooikeusId : null)"
    >
      <span v-if="yek">
        {{ $t('yek.nayta-koulutettavan-suoritustiedot') }}
      </span>
      <span v-else>
        {{ $t('nayta-erikoistujan-suoritustiedot') }}
      </span>
    </elsa-button>
    <hr />
    <div
      v-if="
        editable &&
        isHakija &&
        (hyvaksynta.laillistamispaiva == null || hyvaksynta.laillistamispaivanLiite == null)
      "
    >
      <h3>
        {{ $t('laillistaminen') }}
      </h3>
      <elsa-form-group
        class="col-xs-12 col-sm-3 pl-0"
        :label="$t('laillistamispaiva')"
        :required="true"
      >
        <template #default="{ uid }">
          <elsa-form-datepicker
            :id="uid"
            ref="laillistamispaiva"
            :value.sync="form.laillistamispaiva"
            @input="onSkipRouteExitConfirm"
          ></elsa-form-datepicker>
          <b-form-invalid-feedback>{{ $t('pakollinen-tieto') }}</b-form-invalid-feedback>
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
        <asiakirjat-content
          :asiakirjat="laillistamispaivaAsiakirjat"
          :sorting-enabled="false"
          :pagination-enabled="false"
          :enable-search="false"
          :enable-delete="true"
          :no-results-info-text="$t('ei-liitetiedostoja')"
          :state="validateState('laillistamispaivanLiite')"
          @deleteAsiakirja="onDeleteLaillistamispaivanLiite"
        />
        <b-form-invalid-feedback :state="validateState('laillistamispaivanLiite')">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </elsa-form-group>
      <hr />
    </div>
    <div v-for="(sp, index) in hyvaksynta.tyoskentelyjaksot" :key="index">
      <b-row>
        <b-col>
          <h3>{{ $t('tyoskentelyjakso') }}</h3>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('tyyppi') }}</h5>
          <p>
            {{
              sp.tyoskentelypaikka.tyyppi
                ? displayTyoskentelypaikkaTyyppiLabel(
                    sp.tyoskentelypaikka.muuTyyppi,
                    sp.tyoskentelypaikka.tyyppi
                  )
                : '-'
            }}
          </p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('tyoskentelypaikka') }}</h5>
          <p>{{ sp.tyoskentelypaikka.nimi }}</p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('kunta') }}</h5>
          <p>
            {{ sp.tyoskentelypaikka.kunta ? sp.tyoskentelypaikka.kunta.abbreviation : '' }}
          </p>
        </b-col>
      </b-row>
      <b-row>
        <b-col lg="2">
          <h5>{{ $t('alkamispaiva') }}</h5>
          <p>{{ sp.alkamispaiva ? $date(sp.alkamispaiva) : '' }}</p>
        </b-col>
        <b-col lg="2">
          <h5>{{ $t('paattymispaiva') }}</h5>
          <p>{{ sp.paattymispaiva ? $date(sp.paattymispaiva) : '' }}</p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('tyoaika-taydesta-tyopaivasta-prosentti') }}</h5>
          <p>{{ sp.osaaikaprosentti }}%</p>
        </b-col>
      </b-row>
      <b-row v-if="!yek">
        <b-col>
          <h5>{{ $t('kaytannon-koulutus') }}</h5>
          <p>
            {{ sp.kaytannonKoulutus ? displayKaytannonKoulutus(sp.kaytannonKoulutus) : '' }}
            {{ sp.omaaErikoisalaaTukeva ? ': ' + sp.omaaErikoisalaaTukeva.nimi : '' }}
          </p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <elsa-form-group :label="$t('liitetiedostot')" :required="true">
            <div v-if="editable && isHakija">
              <span>
                {{ $t('lisaa-tyoskentelyjaksoon-tyojakoulutustodistus') }}
              </span>
              <asiakirjat-upload
                class="mt-3"
                :is-primary-button="false"
                :allow-multiples-files="false"
                :button-text="$t('lisaa-liitetiedosto')"
                :existing-file-names-in-current-view="existingFileNamesInCurrentView"
                :existing-file-names-in-other-views="reservedAsiakirjaNimetMutable"
                @selectedFiles="onFilesAdded(sp, $event)"
              />
            </div>
            <asiakirjat-content
              :asiakirjat="sp.asiakirjat"
              :sorting-enabled="false"
              :pagination-enabled="false"
              :enable-search="false"
              :enable-delete="editable && isHakija"
              :no-results-info-text="$t('ei-liitetiedostoja')"
              :state="validateTyoskentelyjaksoState(index)"
              :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
              @deleteAsiakirja="onDeleteLiitetiedosto(sp, $event)"
            />
            <b-form-invalid-feedback :state="validateTyoskentelyjaksoState(index)">
              {{ $t('pakollinen-tieto-tkjakso-liitetiedostot') }}
            </b-form-invalid-feedback>
          </elsa-form-group>
        </b-col>
      </b-row>
      <b-row v-if="sp.hyvaksyttyAiempaanErikoisalaan">
        <b-col>
          <h5>{{ $t('lisatiedot') }}</h5>
          <p>{{ $t('tyoskentelyjakso-on-aiemmin-hyvaksytty-toiselle-erikoisalalle') }}</p>
        </b-col>
      </b-row>
      <b-row v-if="sp.poissaolot != null && sp.poissaolot.length > 0">
        <b-col>
          <h5>{{ $t('poissaolot') }}</h5>
          <elsa-poissaolot-display :poissaolot="sp.poissaolot" />
        </b-col>
      </b-row>
      <hr />
    </div>
    <b-row>
      <b-col lg="8">
        <h3>{{ $t('yleislaaketieteen-vastuuhenkilo') }}</h3>
        <h5>{{ $t('erikoisala-vastuuhenkilö-label') }}</h5>
        <p>
          {{ hyvaksynta.yleislaaketieteenVastuuhenkilonNimi }}
          {{
            hyvaksynta.yleislaaketieteenVastuuhenkilonNimike
              ? ', ' + hyvaksynta.yleislaaketieteenVastuuhenkilonNimike
              : ''
          }}
        </p>
      </b-col>
    </b-row>
    <hr />
    <div v-if="showVirkailijaKuittaus">
      <b-row>
        <b-col>
          <h3>{{ $t('tarkistanut') }}</h3>
        </b-col>
      </b-row>
      <b-row>
        <b-col lg="4">
          <h5>{{ $t('paivays') }}</h5>
          <p>{{ $date(hyvaksynta.virkailijanKuittausaika) }}</p>
        </b-col>
        <b-col lg="4">
          <h5>{{ $t('nimi-ja-nimike') }}</h5>
          <p>
            {{ hyvaksynta.virkailijanNimi + ' '
            }}{{ hyvaksynta.virkailijanNimike ? ', ' + hyvaksynta.virkailijanNimike : '' }}
          </p>
        </b-col>
      </b-row>
      <hr />
    </div>
    <div v-if="showVastuuhenkiloKuittaus">
      <b-row>
        <b-col>
          <h3>{{ $t('hyvaksynyt') }}</h3>
        </b-col>
      </b-row>
      <b-row>
        <b-col lg="4">
          <h5>{{ $t('paivays') }}</h5>
          <p>{{ $date(hyvaksynta.vastuuhenkilonKuittausaika) }}</p>
        </b-col>
        <b-col lg="4">
          <h5>{{ $t('nimi-ja-nimike') }}</h5>
          <p>
            {{ hyvaksynta.vastuuhenkilonNimi + ' '
            }}{{ hyvaksynta.vastuuhenkilonNimike ? ', ' + hyvaksynta.vastuuhenkilonNimike : '' }}
          </p>
        </b-col>
      </b-row>
      <hr />
    </div>
    <div v-if="editable" class="d-flex flex-row-reverse flex-wrap">
      <elsa-button
        v-if="isHakija"
        :loading="params.saving"
        variant="primary"
        class="ml-2"
        @click="onValidateAndConfirm('confirm-erikoistuja')"
      >
        {{ $t('laheta') }}
      </elsa-button>
      <elsa-button
        v-if="$isVirkailija()"
        :loading="params.saving"
        variant="primary"
        class="ml-3"
        @click="onValidateAndConfirm('confirm-virkailija')"
      >
        {{ $t('hyvaksy-laheta') }}
      </elsa-button>
      <elsa-button
        v-if="$isVastuuhenkilo()"
        :loading="params.saving"
        variant="primary"
        class="ml-3"
        @click="onValidateAndConfirm('confirm-vastuuhenkilo')"
      >
        {{ $t('hyvaksy') }}
      </elsa-button>
      <elsa-button
        v-if="!isHakija"
        v-b-modal.return-to-sender
        variant="outline-primary"
        class="ml-2"
      >
        {{ $t('palauta-muokattavaksi') }}
      </elsa-button>
      <elsa-button variant="back" class="font-weight-500" @click.stop.prevent="onCancel">
        {{ $t('peruuta') }}
      </elsa-button>
    </div>
    <div class="row">
      <elsa-form-error :active="$v.$anyError" />
    </div>
    <elsa-confirmation-modal
      id="confirm-erikoistuja"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('lahetyksen-jalkeen-virkailijan-tarkistus')"
      :submit-text="$t('laheta')"
      @submit="onSubmit"
    />
    <elsa-confirmation-modal
      id="confirm-virkailija"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('lahetyksen-jalkeen-vastuuhenkilo-hyvaksynta')"
      :submit-text="$t('hyvaksy-laheta')"
      @submit="onSubmit"
    >
      <template #modal-content>
        <elsa-form-group :label="$t('lisatiedot-vastuuhenkilolle')">
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="hyvaksynta.lisatiedotVirkailijalta"
              rows="7"
            ></b-form-textarea>
          </template>
        </elsa-form-group>
      </template>
    </elsa-confirmation-modal>
    <elsa-confirmation-modal
      id="confirm-vastuuhenkilo"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('vahvistuksen-jalkeen-terveyskeskuskoulutusjakso-hyvaksytty')"
      :submit-text="$t('laheta')"
      @submit="onSubmit"
    />
    <elsa-return-to-sender-modal
      id="return-to-sender"
      :title="$t('palauta-erikoistuvalle-muokattavaksi')"
      @submit="returnToSender"
    />
  </b-form>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { Validation, validationMixin } from 'vuelidate'
  import { required, requiredIf, minLength } from 'vuelidate/lib/validators'

  import { ELSA_API_LOCATION } from '@/api'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import ElsaPoissaolotDisplay from '@/components/poissaolot-display/poissaolot-display.vue'
  import {
    Asiakirja,
    TerveyskeskuskoulutusjaksonHyvaksyminen,
    TerveyskeskuskoulutusjaksonHyvaksyntaForm,
    Tyoskentelyjakso
  } from '@/types'
  import { saveBlob } from '@/utils/blobs'
  import { confirmExit } from '@/utils/confirm'
  import {
    KaytannonKoulutusTyyppi,
    TerveyskeskuskoulutusjaksonTila,
    TyoskentelyjaksoTyyppi
  } from '@/utils/constants'
  import { mapFile, mapFiles } from '@/utils/fileMapper'
  import {
    tyoskentelyjaksoKaytannonKoulutusLabel,
    tyoskentelypaikkaTyyppiLabel
  } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      AsiakirjatContent,
      AsiakirjatUpload,
      ElsaConfirmationModal,
      ElsaButton,
      ElsaFormError,
      ElsaFormDatepicker,
      ElsaFormGroup,
      ElsaReturnToSenderModal,
      ElsaPoissaolotDisplay,
      ErikoistuvaDetails
    }
  })
  export default class TerveyskeskuskoulutusjaksoForm extends Mixins(validationMixin) {
    $refs!: {
      laillistamispaiva: ElsaFormDatepicker
    }

    validations() {
      return {
        hyvaksynta: {
          tyoskentelyjaksot: {
            $each: {
              asiakirjat: {
                required,
                minLength: minLength(3)
              }
            }
          }
        },
        form: {
          laillistamispaiva: {
            required: requiredIf(() => {
              return this.hyvaksynta?.laillistamispaiva == null
            })
          },
          laillistamispaivanLiite: {
            required: requiredIf(() => {
              return this.laillistamispaivaAsiakirjat.length === 0
            })
          }
        }
      }
    }

    @Prop({ required: false, default: null })
    hyvaksynta!: TerveyskeskuskoulutusjaksonHyvaksyminen | null

    @Prop({ required: false, default: () => [] })
    reservedAsiakirjaNimetMutable!: string[] | undefined

    @Prop({ required: false, default: false })
    editable!: boolean

    @Prop({ required: false, default: '' })
    asiakirjaDataEndpointUrl!: string

    @Prop({ required: false, default: false })
    yek!: boolean

    form: TerveyskeskuskoulutusjaksonHyvaksyntaForm = {
      laillistamispaiva: null,
      laillistamispaivanLiite: null,
      tyoskentelyjaksoAsiakirjat: []
    }

    laillistamispaivaAsiakirjat: Asiakirja[] = []
    laillistaminenMuokattavissa = false
    skipRouteExitConfirm = true

    params = {
      saving: false
    }

    async mounted() {
      if (this.hyvaksynta?.laillistamispaivanLiite) {
        const data = Uint8Array.from(atob(this.hyvaksynta?.laillistamispaivanLiite), (c) =>
          c.charCodeAt(0)
        )
        this.laillistamispaivaAsiakirjat.push(
          mapFile(
            new File([data], this.hyvaksynta?.laillistamispaivanLiitteenNimi || '', {
              type: this.hyvaksynta?.laillistamispaivanLiitteenTyyppi
            })
          )
        )
      }
      if (this.hyvaksynta?.laillistamispaiva) {
        this.form.laillistamispaiva = this.hyvaksynta?.laillistamispaiva
      }
    }

    get existingFileNamesInCurrentView() {
      return this.hyvaksynta
        ? this.hyvaksynta.tyoskentelyjaksot?.flatMap((item) =>
            item.asiakirjat?.map((asiakirja) => asiakirja.nimi)
          )
        : []
    }

    get showVirkailijaKuittaus() {
      return (
        this.hyvaksynta?.tila ===
          TerveyskeskuskoulutusjaksonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA ||
        this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.HYVAKSYTTY
      )
    }

    get showVastuuhenkiloKuittaus() {
      return this.hyvaksynta?.tila === TerveyskeskuskoulutusjaksonTila.HYVAKSYTTY
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      this.$v.hyvaksynta.$touch()
      return !this.$v.$anyError
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as Validation
      return $dirty ? ($error ? false : null) : null
    }

    validateTyoskentelyjaksoState(index: number) {
      const { $dirty, $error } = this.$v.hyvaksynta?.tyoskentelyjaksot?.$each[index] as Validation
      return $dirty ? ($error ? false : null) : null
    }

    onValidateAndConfirm(modalId: string) {
      const validations = [
        this.validateForm(),
        this.$refs.laillistamispaiva ? this.$refs.laillistamispaiva.validateForm() : true
      ]

      if (validations.includes(false) || this.hyvaksynta == null) {
        return
      }
      return this.$bvModal.show(modalId)
    }

    onSubmit() {
      this.params.saving = true

      const submitData = {
        lisatiedotVirkailijalta: this.hyvaksynta?.lisatiedotVirkailijalta,
        form: this.form
      }

      if (this.$isVirkailija() && !this.laillistaminenMuokattavissa) {
        this.form.laillistamispaiva = null
        this.form.laillistamispaivanLiite = null
      }

      this.$emit('submit', submitData, this.params)
    }

    onCancel() {
      this.$emit('cancel')
    }

    returnToSender(korjausehdotus: string) {
      if (this.$isVirkailija() && !this.laillistaminenMuokattavissa) {
        this.form.laillistamispaiva = null
        this.form.laillistamispaivanLiite = null
      }

      this.$emit(
        'submit',
        {
          korjausehdotus: korjausehdotus,
          form: this.form
        },
        this.params
      )
    }

    kaytannonKoulutusLabel(tyoskentelyjakso: Tyoskentelyjakso) {
      if (tyoskentelyjakso.kaytannonKoulutus) {
        return tyoskentelyjaksoKaytannonKoulutusLabel(this, tyoskentelyjakso.kaytannonKoulutus)
      }
      return undefined
    }

    tyyppiLabel(tyoskentelyjakso: Tyoskentelyjakso) {
      if (tyoskentelyjakso.tyoskentelypaikka?.tyyppi) {
        return tyoskentelypaikkaTyyppiLabel(this, tyoskentelyjakso.tyoskentelypaikka?.tyyppi)
      }
      return undefined
    }

    displayTyoskentelypaikkaTyyppiLabel(muu: string | null, tyyppi: TyoskentelyjaksoTyyppi) {
      return muu ? muu : tyoskentelypaikkaTyyppiLabel(this, tyyppi)
    }

    displayKaytannonKoulutus(value: KaytannonKoulutusTyyppi) {
      return tyoskentelyjaksoKaytannonKoulutusLabel(this, value)
    }

    onFilesAdded(tyoskentelyjakso: Tyoskentelyjakso, files: File[]) {
      const tjaksoAsiakirjat = this.form.tyoskentelyjaksoAsiakirjat.filter(
        (tjakso) => tjakso.id === tyoskentelyjakso.id
      )

      if (tjaksoAsiakirjat?.length === 0) {
        this.form.tyoskentelyjaksoAsiakirjat.push({
          id: tyoskentelyjakso.id,
          addedFiles: files,
          deletedFiles: []
        })
      } else {
        tjaksoAsiakirjat[0].addedFiles.push(...files)
      }

      tyoskentelyjakso.asiakirjat?.push(...mapFiles(files))
    }

    async onDeleteLiitetiedosto(tyoskentelyjakso: Tyoskentelyjakso, asiakirja: Asiakirja) {
      const tjaksoAsiakirjat = this.form.tyoskentelyjaksoAsiakirjat.filter(
        (tjakso) => tjakso.id === tyoskentelyjakso.id
      )

      if (asiakirja.id) {
        if (tjaksoAsiakirjat?.length === 0) {
          this.form.tyoskentelyjaksoAsiakirjat.push({
            id: tyoskentelyjakso.id,
            addedFiles: [],
            deletedFiles: [asiakirja.id]
          })
        } else {
          tjaksoAsiakirjat[0].deletedFiles.push(asiakirja.id)
        }
        tyoskentelyjakso.asiakirjat = tyoskentelyjakso.asiakirjat?.filter(
          (a) => a.nimi !== asiakirja.nimi
        )
      } else {
        tjaksoAsiakirjat[0].addedFiles = tjaksoAsiakirjat[0].addedFiles.filter(
          (file) => file.name !== asiakirja.nimi
        )
        tyoskentelyjakso.asiakirjat = tyoskentelyjakso.asiakirjat?.filter(
          (a) => a.nimi !== asiakirja.nimi
        )
      }
      this.onSkipRouteExitConfirm()
    }

    onLaillistamispaivaFilesAdded(files: File[]) {
      this.form.laillistamispaivanLiite = files[0]
      this.laillistamispaivaAsiakirjat.push(...mapFiles(files))
    }

    async onDeleteLaillistamispaivanLiite() {
      this.form.laillistamispaivanLiite = null
      this.laillistamispaivaAsiakirjat = []
      this.onSkipRouteExitConfirm()
    }

    async onDownloadLaillistamistodistus() {
      if (this.hyvaksynta?.laillistamispaivanLiite != null) {
        const data = Uint8Array.from(atob(this.hyvaksynta?.laillistamispaivanLiite), (c) =>
          c.charCodeAt(0)
        )
        saveBlob(
          this.hyvaksynta?.laillistamispaivanLiitteenNimi || '',
          data,
          this.hyvaksynta?.laillistamispaivanLiitteenTyyppi || ''
        )
      }
    }

    muokkaaLaillistamista() {
      this.laillistaminenMuokattavissa = true
    }

    async vaihdaRooli(id: number | undefined | null) {
      if (this.editable && !this.skipRouteExitConfirm) {
        if (!(await confirmExit(this))) {
          return
        }
      }

      this.$emit('skipRouteExitConfirm', true)
      window.location.href = `${ELSA_API_LOCATION}/api/login/impersonate?opintooikeusId=${id}&originalUrl=${window.location.href}`
    }

    onSkipRouteExitConfirm() {
      this.skipRouteExitConfirm = false
      this.$emit('skipRouteExitConfirm', false)
    }

    get isHakija() {
      return this.$isErikoistuva() || this.$isYekKoulutettava()
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .datepicker-range::before {
    content: '–';
    position: absolute;
    left: -1rem;
    padding: 0 0.75rem;
    @include media-breakpoint-down(sm) {
      display: none;
    }
  }

  .erikoistuja-details-padding {
    padding-left: 0.7rem;
  }
</style>
