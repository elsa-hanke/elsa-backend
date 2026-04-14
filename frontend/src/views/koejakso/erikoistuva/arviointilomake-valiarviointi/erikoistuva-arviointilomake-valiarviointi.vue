<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />

    <b-container fluid>
      <h1 class="mb-3">{{ $t('koejakson-valiarviointi') }}</h1>
      <div v-if="!loading">
        <b-row lg>
          <b-col>
            <b-alert :show="showWaitingForAcceptance" variant="dark" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                </em>
                <div>
                  {{ $t('valiarviointi-tila-odottaa-hyvaksyntaa') }}
                </div>
              </div>
            </b-alert>
            <b-alert variant="success" :show="showAcceptedByEveryone">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                </em>
                <span>{{ $t('valiarviointi-tila-hyvaksytty') }}</span>
              </div>
            </b-alert>
            <div v-if="editable">
              <p>{{ $t('koejakson-valiarviointi-ingressi') }}</p>
            </div>
          </b-col>
        </b-row>
        <hr />
        <b-row>
          <b-col>
            <erikoistuva-details
              :avatar="account.avatar"
              :name="`${account.firstName} ${account.lastName}`"
              :erikoisala="account.erikoistuvaLaakari.erikoisalaNimi"
              :opiskelijatunnus="account.erikoistuvaLaakari.opiskelijatunnus"
              :syntymaaika="account.erikoistuvaLaakari.syntymaaika"
              :yliopisto="account.erikoistuvaLaakari.yliopisto"
            ></erikoistuva-details>
          </b-col>
        </b-row>

        <hr />

        <b-form v-if="editable">
          <koulutuspaikan-arvioijat
            ref="koulutuspaikanArvioijat"
            :lahikouluttaja="valiarviointiLomake.lahikouluttaja"
            :lahiesimies="valiarviointiLomake.lahiesimies"
            :allow-duplicates="true"
            :kouluttajat="kouluttajat"
            @lahikouluttajaSelect="onLahikouluttajaSelect"
            @lahiesimiesSelect="onLahiesimiesSelect"
          />
          <hr />
        </b-form>
        <div v-if="showAcceptedByEveryone">
          <h3 class="mb-3">{{ $t('soveltuvuus-erikoisalalle-valiarvioinnin-perusteella') }}</h3>
          <b-row>
            <b-col lg="8">
              <h5>{{ $t('edistyminen-osaamistavoitteiden-mukaista') }}</h5>
              <p>
                {{
                  valiarviointiLomake.edistyminenTavoitteidenMukaista
                    ? $t('kylla')
                    : $t('ei-huolenaiheita-on')
                }}
              </p>
            </b-col>
          </b-row>
          <b-row v-if="valiarviointiLomake.edistyminenTavoitteidenMukaista === false">
            <b-col lg="8">
              <h5>{{ $t('keskustelu-ja-toimenpiteet-tarpeen-ennen-hyvaksymista') }}</h5>
              <ul class="pl-4">
                <li v-for="kategoria in sortedKategoriat" :key="kategoria">
                  {{ naytaKehittamistoimenpideKategoria(kategoria) }}
                </li>
              </ul>
            </b-col>
          </b-row>
          <b-row>
            <b-col v-if="valiarviointiLomake.vahvuudet">
              <h5>{{ $t('vahvuudet') }}</h5>
              <p>{{ valiarviointiLomake.vahvuudet }}</p>
            </b-col>
          </b-row>
          <b-row>
            <b-col v-if="valiarviointiLomake.kehittamistoimenpiteet">
              <h5>{{ $t('selvitys-kehittamistoimenpiteista') }}</h5>
              <p>{{ valiarviointiLomake.kehittamistoimenpiteet }}</p>
            </b-col>
          </b-row>
          <hr />
        </div>

        <div v-if="!editable">
          <koulutuspaikan-arvioijat
            :lahikouluttaja="valiarviointiLomake.lahikouluttaja"
            :lahiesimies="valiarviointiLomake.lahiesimies"
            :is-readonly="true"
          />
          <hr />
        </div>

        <div v-if="showAcceptedByEveryone">
          <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
        </div>

        <div v-if="deletable">
          <b-row>
            <b-col class="text-right">
              <elsa-button
                v-if="!loading"
                :loading="buttonStates.primaryButtonLoading"
                variant="outline-danger"
                class="ml-4 px-6"
                @click="onValidateAndConfirm('confirm-form-delete')"
              >
                {{ $t('tyhjenna-lomake') }}
              </elsa-button>
            </b-col>
          </b-row>
        </div>

        <div v-if="!account.impersonated && editable">
          <hr v-if="hyvaksynnat.length > 0" />
          <b-row>
            <b-col class="text-right">
              <elsa-button variant="back" :to="{ name: 'koejakso' }">
                {{ $t('peruuta') }}
              </elsa-button>
              <elsa-button
                v-if="!loading"
                :loading="buttonStates.primaryButtonLoading"
                variant="primary"
                class="ml-4 px-6"
                @click="onValidateAndConfirm('confirm-send')"
              >
                {{ $t('laheta') }}
              </elsa-button>
            </b-col>
          </b-row>
        </div>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>

    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('vahvista-koejakson-vaihe-lahetys')"
      :submit-text="$t('laheta')"
      @submit="onSend"
    />

    <elsa-confirmation-modal
      id="confirm-form-delete"
      :title="$t('vahvista-lomakkeen-tyhjennys')"
      :text="$t('vahvista-lomakkeen-tyhjennys-selite')"
      :submit-text="$t('tyhjenna-lomake')"
      @submit="onFormDelete"
    />
  </div>
</template>

<script lang="ts">
  import _get from 'lodash/get'
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import store from '@/store'
  import {
    ValiarviointiLomake,
    Koejakso,
    KoejaksonVaiheHyvaksynta,
    KoejaksonVaiheHyvaksyja,
    KoejaksonVaiheButtonStates
  } from '@/types'
  import { KehittamistoimenpideKategoria, LomakeTilat } from '@/utils/constants'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ErikoistuvaDetails,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaButton,
      ElsaConfirmationModal,
      KoulutuspaikanArvioijat,
      KoejaksonVaiheHyvaksynnat
    }
  })
  export default class ErikoistuvaArviointilomakeValiarviointi extends Vue {
    $refs!: {
      koulutuspaikanArvioijat: KoulutuspaikanArvioijat
    }
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koejakso'),
        to: { name: 'koejakso' }
      },
      {
        text: this.$t('koejakson-valiarviointi'),
        active: true
      }
    ]

    loading = true

    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }

    koejaksonVaihe = this.$t('valiarviointi')

    valiarviointiLomake: Partial<ValiarviointiLomake> = {
      edistyminenTavoitteidenMukaista: null,
      erikoistuvanErikoisala: this.account.erikoistuvaLaakari.erikoisalaNimi,
      erikoistuvanNimi: `${this.account.firstName} ${this.account.lastName}`,
      erikoistuvanOpiskelijatunnus: this.account.erikoistuvaLaakari.opiskelijatunnus,
      erikoistuvanYliopisto: this.account.erikoistuvaLaakari.yliopisto,
      id: null,
      kehittamistoimenpideKategoriat: [],
      kehittamistoimenpiteet: '',
      korjausehdotus: '',
      lahiesimies: {
        id: null,
        kayttajaUserId: null,
        kuittausaika: '',
        nimi: '',
        nimike: null,
        sopimusHyvaksytty: false
      },
      lahikouluttaja: {
        id: 0,
        kayttajaUserId: null,
        kuittausaika: '',
        nimi: '',
        nimike: null,
        sopimusHyvaksytty: false
      },
      muokkauspaiva: '',
      muuKategoria: '',
      vahvuudet: ''
    }

    kategoriaOrder = [
      KehittamistoimenpideKategoria.TYOSSASUORIUTUMINEN,
      KehittamistoimenpideKategoria.TYOKAYTTAYTYMINEN,
      KehittamistoimenpideKategoria.POTILASPALAUTE,
      KehittamistoimenpideKategoria.MUU
    ]

    validateState(value: string) {
      const form = this.$v.valiarviointiLomake
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    get account() {
      return store.getters['auth/account']
    }

    get kouluttajat() {
      return store.getters['erikoistuva/kouluttajatJaVastuuhenkilot'] || []
    }

    get editable() {
      if (this.account.impersonated) {
        return false
      }
      return this.koejaksoData.valiarvioinninTila === LomakeTilat.UUSI
    }

    get deletable() {
      return this.koejaksoData.valiarvioinninTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get showWaitingForAcceptance() {
      return this.koejaksoData.valiarvioinninTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get showAcceptedByEveryone() {
      return this.koejaksoData.valiarvioinninTila === LomakeTilat.HYVAKSYTTY
    }

    get koejaksoData(): Koejakso {
      return store.getters['erikoistuva/koejakso']
    }

    setKoejaksoData() {
      if (this.koejaksoData.valiarviointi) {
        this.valiarviointiLomake = this.koejaksoData.valiarviointi
      }
    }

    naytaKehittamistoimenpideKategoria(kategoria: string) {
      if (kategoria === KehittamistoimenpideKategoria.MUU)
        return this.valiarviointiLomake?.muuKategoria
      return this.$t('kehittamistoimenpidekategoria-' + kategoria)
    }

    get sortedKategoriat() {
      return this.valiarviointiLomake?.kehittamistoimenpideKategoriat?.sort(
        (a, b) => this.kategoriaOrder.indexOf(a) - this.kategoriaOrder.indexOf(b)
      )
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.valiarviointiLomake?.erikoistuvanNimi,
        this.valiarviointiLomake?.erikoistuvanKuittausaika
      )
      const hyvaksyntaLahikouluttaja = hyvaksynnatHelper.mapHyvaksyntaLahikouluttaja(
        this,
        this.valiarviointiLomake?.lahikouluttaja
      )
      const hyvaksyntaLahiesimies = hyvaksynnatHelper.mapHyvaksyntaLahiesimies(
        this,
        this.valiarviointiLomake?.lahiesimies
      )

      return [hyvaksyntaLahikouluttaja, hyvaksyntaLahiesimies, hyvaksyntaErikoistuva].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    optionDisplayName(option: any) {
      return option.nimike ? option.nimi + ', ' + option.nimike : option.nimi
    }

    hideModal(id: string) {
      return this.$bvModal.hide(id)
    }

    onLahikouluttajaSelect(lahikouluttaja: KoejaksonVaiheHyvaksyja) {
      this.valiarviointiLomake.lahikouluttaja = lahikouluttaja
    }

    onLahiesimiesSelect(lahiesimies: KoejaksonVaiheHyvaksyja) {
      this.valiarviointiLomake.lahiesimies = lahiesimies
    }

    onValidateAndConfirm(modalId: string) {
      if (
        this.$refs.koulutuspaikanArvioijat &&
        !this.$refs.koulutuspaikanArvioijat.validateForm()
      ) {
        return
      }
      return this.$bvModal.show(modalId)
    }

    async onSend() {
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch('erikoistuva/postValiarviointi', this.valiarviointiLomake)
        this.buttonStates.primaryButtonLoading = false
        toastSuccess(this, this.$t('valiarviointi-lahetetty-onnistuneesti'))
        this.setKoejaksoData()
      } catch {
        toastFail(this, this.$t('valiarviointi-tallennus-epaonnistui'))
      }
    }

    async onFormDelete() {
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch('erikoistuva/deleteValiarviointi', this.valiarviointiLomake)
        this.buttonStates.primaryButtonLoading = false
        toastSuccess(this, this.$t('lomake-tyhjennetty-onnistuneesti'))
        this.valiarviointiLomake.id = null
      } catch {
        toastFail(this, this.$t('lomakkeen-tyhjennys-epaonnistui'))
      }
    }

    async mounted() {
      this.loading = true
      if (!this.koejaksoData) {
        await store.dispatch('erikoistuva/getKoejakso')
      }
      await store.dispatch('erikoistuva/getKouluttajatJaVastuuhenkilot')
      this.setKoejaksoData()
      this.loading = false
    }
  }
</script>
