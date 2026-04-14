<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />

    <b-container fluid>
      <h1 class="mb-3">{{ $t('koejakson-kehittamistoimenpiteet') }}</h1>
      <div v-if="!loading">
        <b-row lg>
          <b-col>
            <div v-if="editable">
              <p>{{ $t('koejakson-kehittamistoimenpiteet-ingressi') }}</p>
            </div>
            <div v-else>
              <b-alert :show="waitingForKouluttaja" variant="dark" class="mt-3">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                  </em>
                  <div>
                    {{ $t('kehittamistoimenpiteet-tila-odottaa-hyvaksyntaa') }}
                  </div>
                </div>
              </b-alert>
              <b-alert variant="success" :show="acceptedByEveryone">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                  </em>
                  <span>{{ $t('kehittamistoimenpiteet-tila-hyvaksytty') }}</span>
                </div>
              </b-alert>
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
              :yliopisto="account.erikoistuvaLaakari.yliopisto"
              :syntymaaika="account.erikoistuvaLaakari.syntymaaika"
              :kehittamistoimenpiteet="koejaksoData.valiarviointi.kehittamistoimenpiteet"
              :show-kehittamistoimenpiteet="true"
            ></erikoistuva-details>
          </b-col>
        </b-row>
        <hr />
        <div v-if="acceptedByEveryone">
          <b-row>
            <b-col lg="10">
              <h3>{{ $t('kehittamistoimenpiteiden-arviointi') }}</h3>
              <p v-if="koejaksoData.kehittamistoimenpiteet.kehittamistoimenpiteetRiittavat">
                {{ $t('kehittamistoimenpiteet-riittavat') }}
              </p>
              <p v-else>{{ $t('kehittamistoimenpiteet-ei-riittavat') }}</p>
            </b-col>
          </b-row>
          <hr />
        </div>

        <koulutuspaikan-arvioijat
          ref="koulutuspaikanArvioijat"
          :lahikouluttaja="kehittamistoimenpiteetLomake.lahikouluttaja"
          :lahiesimies="kehittamistoimenpiteetLomake.lahiesimies"
          :is-readonly="!editable"
          :allow-duplicates="true"
          :kouluttajat="kouluttajat"
          @lahikouluttajaSelect="onLahikouluttajaSelect"
          @lahiesimiesSelect="onLahiesimiesSelect"
        />
        <hr />

        <div v-if="acceptedByEveryone">
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
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import store from '@/store'
  import {
    KehittamistoimenpiteetLomake,
    Koejakso,
    KoejaksonVaiheHyvaksyja,
    KoejaksonVaiheHyvaksynta,
    KoejaksonVaiheButtonStates
  } from '@/types'
  import { LomakeTilat } from '@/utils/constants'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ErikoistuvaDetails,
      ElsaFormGroup,
      ElsaButton,
      ElsaConfirmationModal,
      ElsaReturnToSenderModal,
      KoulutuspaikanArvioijat,
      KoejaksonVaiheHyvaksynnat
    }
  })
  export default class ErikoistuvaArviointilomakeKehittamistoimenpiteet extends Vue {
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
        text: this.$t('koejakson-kehittamistoimenpiteet'),
        active: true
      }
    ]

    loading = true

    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }

    koejaksonVaihe = (this.$t('kehittamistoimenpiteiden-arviointi') as string).toLowerCase()

    kehittamistoimenpiteetLomake: KehittamistoimenpiteetLomake = {
      erikoistuvanKuittausaika: undefined,
      erikoistuvanErikoisala: '',
      erikoistuvanNimi: '',
      erikoistuvanOpiskelijatunnus: '',
      erikoistuvanYliopisto: '',
      id: null,
      kehittamistoimenpiteetRiittavat: null,
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
        id: null,
        kayttajaUserId: null,
        kuittausaika: '',
        nimi: '',
        nimike: null,
        sopimusHyvaksytty: false
      },
      muokkauspaiva: '',
      kehittamistoimenpideKategoriat: [],
      muuKategoria: null,
      kehittamistoimenpiteetKuvaus: null
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
      return this.koejaksoData.kehittamistoimenpiteidenTila === LomakeTilat.UUSI
    }

    get deletable() {
      return this.koejaksoData.kehittamistoimenpiteidenTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get acceptedByEveryone() {
      return this.koejaksoData.kehittamistoimenpiteidenTila == LomakeTilat.HYVAKSYTTY
    }

    get waitingForKouluttaja() {
      return (
        this.koejaksoData.kehittamistoimenpiteidenTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA ||
        this.koejaksoData.kehittamistoimenpiteidenTila === LomakeTilat.ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
      )
    }

    get koejaksoData(): Koejakso {
      return store.getters['erikoistuva/koejakso']
    }

    setKoejaksoData() {
      if (this.koejaksoData.kehittamistoimenpiteet) {
        this.kehittamistoimenpiteetLomake = this.koejaksoData.kehittamistoimenpiteet
      }
      this.kehittamistoimenpiteetLomake.erikoistuvanNimi = `${this.account.firstName} ${this.account.lastName}`
      this.kehittamistoimenpiteetLomake.erikoistuvanOpiskelijatunnus =
        this.account.erikoistuvaLaakari.opiskelijatunnus
      this.kehittamistoimenpiteetLomake.erikoistuvanErikoisala =
        this.account.erikoistuvaLaakari.erikoisalaNimi
      this.kehittamistoimenpiteetLomake.erikoistuvanYliopisto =
        this.account.erikoistuvaLaakari.yliopisto
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaLahikouluttaja = hyvaksynnatHelper.mapHyvaksyntaLahikouluttaja(
        this,
        this.kehittamistoimenpiteetLomake.lahikouluttaja
      )
      const hyvaksyntaLahiesimies = hyvaksynnatHelper.mapHyvaksyntaLahiesimies(
        this,
        this.kehittamistoimenpiteetLomake.lahiesimies
      )
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.kehittamistoimenpiteetLomake.erikoistuvanNimi,
        this.kehittamistoimenpiteetLomake.erikoistuvanKuittausaika
      )

      return [hyvaksyntaLahikouluttaja, hyvaksyntaLahiesimies, hyvaksyntaErikoistuva].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    hideModal(id: string) {
      return this.$bvModal.hide(id)
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

    onLahikouluttajaSelect(lahikouluttaja: KoejaksonVaiheHyvaksyja) {
      this.kehittamistoimenpiteetLomake.lahikouluttaja = lahikouluttaja
    }

    onLahiesimiesSelect(lahiesimies: KoejaksonVaiheHyvaksyja) {
      this.kehittamistoimenpiteetLomake.lahiesimies = lahiesimies
    }

    async onSend() {
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch(
          'erikoistuva/postKehittamistoimenpiteet',
          this.kehittamistoimenpiteetLomake
        )
        this.buttonStates.primaryButtonLoading = false
        toastSuccess(this, this.$t('kehittamistoimenpiteet-arviointipyynnon-lahetys-onnistui'))
        this.setKoejaksoData()
      } catch {
        toastFail(this, this.$t('kehittamistoimenpiteet-arviointipyynnon-lahetys-epaonnistui'))
      }
    }

    async onFormDelete() {
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch(
          'erikoistuva/deleteKehittamistoimenpiteet',
          this.kehittamistoimenpiteetLomake
        )
        this.buttonStates.primaryButtonLoading = false
        toastSuccess(this, this.$t('lomake-tyhjennetty-onnistuneesti'))
        this.kehittamistoimenpiteetLomake.id = null
      } catch {
        toastFail(this, this.$t('lomakkeen-tyhjennys-epaonnistui'))
      }
    }

    async mounted() {
      if (!this.koejaksoData) {
        await store.dispatch('erikoistuva/getKoejakso')
      }
      await store.dispatch('erikoistuva/getKouluttajatJaVastuuhenkilot')
      this.setKoejaksoData()
      this.loading = false
    }
  }
</script>
