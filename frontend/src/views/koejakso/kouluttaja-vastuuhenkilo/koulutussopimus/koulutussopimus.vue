<template>
  <div class="koulutussopimus col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <h1 class="mb-3">{{ $t('koulutussopimus') }}</h1>
      <div v-if="!loading">
        <div v-if="editable">
          <p v-if="!$isVastuuhenkilo()">{{ $t('koulutussopimus-kouluttaja-ingressi') }}</p>
          <p v-if="$isVastuuhenkilo()">{{ $t('koulutussopimus-vastuuhenkilo-ingressi') }}</p>
        </div>
        <b-alert :show="showWaitingForVastuuhenkilo" variant="dark" class="mt-3">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
            </em>
            <div>{{ $t('koulutussopimus-tila-odottaa-vastuuhenkilon-hyvaksyntaa') }}</div>
          </div>
        </b-alert>
        <b-alert :show="showWaitingForAnotherKouluttaja" variant="dark" class="mt-3">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
            </em>
            <div>{{ $t('koulutussopimus-tila-odottaa-toisen-kouluttajan-hyvaksyntaa') }}</div>
          </div>
        </b-alert>
        <b-alert :show="showSigned" variant="success" class="mt-3">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="mr-2" />
            </em>
            <div>
              {{ $t('koulutussopimus-tila-hyvaksytty') }}
            </div>
          </div>
        </b-alert>
        <b-alert :show="returned" variant="dark" class="mt-3">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
            </em>
            <div>
              {{ $t('koulutussopimus-kouluttaja-palautettu') }}
              <span class="d-block">
                {{ $t('syy') }}&nbsp;{{ form.korjausehdotus || form.vastuuhenkilonKorjausehdotus }}
              </span>
            </div>
          </div>
        </b-alert>
        <hr />
        <b-row>
          <b-col>
            <erikoistuva-details
              :avatar="erikoistuvanAvatar"
              :name="erikoistuvanNimi"
              :erikoisala="erikoistuvanErikoisala"
              :opiskelijatunnus="form.erikoistuvanOpiskelijatunnus"
              :syntymaaika="form.erikoistuvanSyntymaaika"
              :yliopisto="form.erikoistuvanYliopisto"
            ></erikoistuva-details>
          </b-col>
        </b-row>
        <hr />
        <b-form @submit.stop.prevent="onSubmit">
          <b-row>
            <b-col lg="8">
              <h5>{{ $t('opinto-oikeuden-alkamispäivä') }}</h5>
              <p>
                {{
                  form.opintooikeudenMyontamispaiva ? $date(form.opintooikeudenMyontamispaiva) : ''
                }}
              </p>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="8">
              <h5>{{ $t('koejakson-alkamispäivä') }}</h5>
              <p>{{ form.koejaksonAlkamispaiva ? $date(form.koejaksonAlkamispaiva) : '' }}</p>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('sahkopostiosoite') }}</h5>
              <p>{{ form.erikoistuvanSahkoposti }}</p>
            </b-col>
            <b-col lg="4">
              <h5>{{ $t('matkapuhelinnumero') }}</h5>
              <p>{{ form.erikoistuvanPuhelinnumero }}</p>
            </b-col>
          </b-row>

          <hr />

          <b-row>
            <b-col lg="12">
              <h3>{{ $t('koulutuspaikan-tiedot') }}</h3>
              <div v-for="(koulutuspaikka, index) in form.koulutuspaikat" :key="index">
                <h5>{{ $t('toimipaikan-nimi') }}</h5>
                <p>{{ koulutuspaikka.nimi }}</p>
                <h5>{{ $t('toimipaikalla-koulutussopimus.header') }}</h5>
                <p v-if="!koulutuspaikka.yliopisto">{{ $t('kylla') }}</p>
                <p v-else>
                  {{ $t('toimipaikalla-koulutussopimus.ei-sopimusta') }}:
                  {{ $t(`yliopisto-nimi.${koulutuspaikka.yliopisto}`) }}
                </p>
              </div>
            </b-col>
          </b-row>

          <hr />

          <b-row>
            <b-col>
              <h3>{{ $t('koulutuspaikan-lahikouluttaja') }}</h3>
              <div
                v-for="(kouluttaja, index) in form.kouluttajat"
                :key="index"
                class="kouluttaja-section"
              >
                <kouluttaja-koulutussopimus-form
                  v-if="isCurrentKouluttaja(kouluttaja) && editable"
                  ref="kouluttajaKoulutussopimusForm"
                  v-model="form.kouluttajat[index]"
                  :kouluttaja="kouluttaja"
                  :index="index"
                  @ready="onChildKouluttajaFormValid"
                  @skipRouteExitConfirm="onSkipRouteExitConfirm"
                ></kouluttaja-koulutussopimus-form>

                <koulutussopimus-readonly
                  v-if="!isCurrentKouluttaja(kouluttaja) || !editable"
                  :kouluttaja="kouluttaja"
                ></koulutussopimus-readonly>
              </div>
            </b-col>
          </b-row>
          <hr />

          <b-row v-if="form.vastuuhenkilo">
            <b-col lg="8">
              <h3>{{ $t('erikoisala-vastuuhenkilö') }}</h3>
              <h5>{{ $t('erikoisala-vastuuhenkilö-label') }}</h5>
              <p>
                {{ form.vastuuhenkilo.nimi }}
                {{ form.vastuuhenkilo.nimike ? ', ' + form.vastuuhenkilo.nimike : '' }}
              </p>
            </b-col>
          </b-row>
          <b-row v-if="form.vastuuhenkilo && !editable">
            <b-col lg="4">
              <h5>{{ $t('sahkopostiosoite') }}</h5>
              <p>{{ form.vastuuhenkilo.sahkoposti }}</p>
            </b-col>

            <b-col lg="4">
              <h5>{{ $t('matkapuhelinnumero') }}</h5>
              <p>{{ form.vastuuhenkilo.puhelin }}</p>
            </b-col>
          </b-row>
          <vastuuhenkilo-koulutussopimus-form
            v-if="$isVastuuhenkilo() && editable"
            ref="vastuuhenkiloKoulutussopimusForm"
            v-model="form.vastuuhenkilo"
            :vastuuhenkilo="form.vastuuhenkilo"
            @ready="onChildVastuuhenkiloFormValid"
            @skipRouteExitConfirm="onSkipRouteExitConfirm"
          ></vastuuhenkilo-koulutussopimus-form>
          <hr />
          <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
          <hr v-if="hyvaksynnat.length > 0" />
          <b-row v-if="editable && !signed">
            <b-col class="text-right">
              <elsa-button
                class="ml-1 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block text-left"
                style="max-width: 14rem"
                variant="back"
                :to="{ name: 'koejakso' }"
              >
                {{ $t('peruuta') }}
              </elsa-button>
              <elsa-button
                v-b-modal.return-to-sender
                class="my-2 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block"
                style="min-width: 14rem"
                variant="outline-primary"
                :disabled="buttonStates.primaryButtonLoading"
                :loading="buttonStates.secondaryButtonLoading"
              >
                {{ $t('palauta-muokattavaksi') }}
              </elsa-button>
              <elsa-button
                v-if="!$isVastuuhenkilo()"
                class="my-2 d-block d-md-inline-block d-lg-block d-xl-inline-block"
                style="min-width: 14rem"
                :disabled="buttonStates.secondaryButtonLoading"
                :loading="buttonStates.primaryButtonLoading"
                type="submit"
                variant="primary"
              >
                {{ $t('hyvaksy-laheta') }}
              </elsa-button>
              <elsa-button
                v-if="$isVastuuhenkilo()"
                class="my-2 d-block d-md-inline-block d-lg-block d-xl-inline-block"
                style="min-width: 14rem"
                :disabled="buttonStates.secondaryButtonLoading"
                :loading="buttonStates.primaryButtonLoading"
                type="submit"
                variant="primary"
              >
                {{ $t('hyvaksy') }}
              </elsa-button>
            </b-col>
          </b-row>
        </b-form>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>

    <elsa-confirmation-modal
      id="confirm-send-kouluttaja"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="
        sendToVastuuhenkilo
          ? $t('vahvista-koulutussopimus-kouluttajat-hyvaksytty')
          : $t('vahvista-koulutussopimus-kouluttaja-hyvaksytty')
      "
      :submit-text="$t('hyvaksy-laheta')"
      @submit="updateSentForm"
    />
    <elsa-confirmation-modal
      id="confirm-send-vastuuhenkilo"
      :title="$t('vahvista-lomakkeen-hyvaksynta')"
      :text="$t('vahvista-koulutussopimus-hyvaksytty')"
      :submit-text="$t('hyvaksy')"
      @submit="updateSentForm"
    />
    <elsa-return-to-sender-modal
      id="return-to-sender"
      :title="$t('palauta-erikoistuvalle-muokattavaksi')"
      @submit="returnToSender"
    />
  </div>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'

  import { getKoulutussopimus as getKoulutussopimusKouluttaja } from '@/api/kouluttaja'
  import { getKoulutussopimus as getKoulutussopimusVastuuhenkilo } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import store from '@/store'
  import {
    KoejaksonVaiheHyvaksynta,
    KoulutussopimusLomake,
    Kouluttaja,
    KoejaksonVaiheButtonStates,
    Vastuuhenkilo,
    KoejaksonVaihe
  } from '@/types'
  import { resolveRolePath } from '@/utils/apiRolePathResolver'
  import { defaultKoulutuspaikka, LomakeTilat, LomakeTyypit } from '@/utils/constants'
  import { checkCurrentRouteAndRedirect } from '@/utils/functions'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import KoulutussopimusReadonly from '@/views/koejakso/kouluttaja-vastuuhenkilo/koulutussopimus/koulutussopimus-readonly.vue'
  import KouluttajaKoulutussopimusForm from '@/views/koejakso/kouluttaja/kouluttaja-koulutussopimus-form.vue'
  import VastuuhenkiloKoulutussopimusForm from '@/views/koejakso/vastuuhenkilo/vastuuhenkilo-koulutussopimus-form.vue'

  @Component({
    components: {
      KouluttajaKoulutussopimusForm,
      VastuuhenkiloKoulutussopimusForm,
      ElsaFormGroup,
      ElsaFormError,
      ElsaButton,
      KoulutussopimusReadonly,
      ErikoistuvaDetails,
      KoejaksonVaiheHyvaksynnat,
      ElsaConfirmationModal,
      ElsaReturnToSenderModal
    }
  })
  export default class Koulutussopimus extends Mixins(validationMixin) {
    skipRouteExitConfirm!: boolean
    $refs!: {
      kouluttajaKoulutussopimusForm: Array<KouluttajaKoulutussopimusForm>
      vastuuhenkiloKoulutussopimusForm: VastuuhenkiloKoulutussopimusForm
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
        text: this.$t('koulutussopimus-kouluttaja'),
        active: true
      }
    ]

    form: KoulutussopimusLomake = {
      id: null,
      erikoistuvanNimi: '',
      erikoistuvanErikoisala: '',
      erikoistuvanOpiskelijatunnus: '',
      erikoistuvanPuhelinnumero: '',
      erikoistuvanSahkoposti: '',
      erikoistuvanSyntymaaika: '',
      erikoistuvanYliopisto: '',
      koejaksonAlkamispaiva: '',
      korjausehdotus: '',
      vastuuhenkilonKorjausehdotus: '',
      kouluttajat: [],
      koulutuspaikat: [defaultKoulutuspaikka],
      lahetetty: false,
      muokkauspaiva: '',
      opintooikeudenMyontamispaiva: '',
      opintooikeudenPaattymispaiva: '',
      vastuuhenkilo: null,
      yliopistot: []
    }

    loading = true
    childKouluttajaFormValid = false
    childVastuuhenkiloFormValid = false
    showVastuuhenkiloSubmitButton = false
    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }

    onChildKouluttajaFormValid(index: number, form: Kouluttaja) {
      this.form.kouluttajat[index] = form
      this.childKouluttajaFormValid = true
    }

    onChildVastuuhenkiloFormValid(form: Vastuuhenkilo) {
      this.form.vastuuhenkilo = form
      this.childVastuuhenkiloFormValid = true
    }

    get koulutussopimusData() {
      return store.getters[`${resolveRolePath()}/koejaksot`].find(
        (k: KoejaksonVaihe) =>
          k.id === this.koulutussopimusId && k.tyyppi === LomakeTyypit.KOULUTUSSOPIMUS
      )
    }

    get koulutussopimusId() {
      return Number(this.$route.params.id)
    }

    get account() {
      return store.getters['auth/account']
    }

    get editable() {
      return !this.loading && this.koulutussopimusData.tila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get signed() {
      let signed = false
      this.form.kouluttajat.forEach((k: Kouluttaja) => {
        if (this.isCurrentKouluttaja(k)) {
          signed = k.sopimusHyvaksytty
        }
      })
      return signed
    }

    get allKouluttajatSigned() {
      return this.form.kouluttajat.every((k: Kouluttaja) => k.sopimusHyvaksytty)
    }

    get returned() {
      return !this.loading && this.koulutussopimusData.tila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get showWaitingForVastuuhenkilo() {
      return this.koulutussopimusData.tila === LomakeTilat.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
    }

    get showWaitingForAnotherKouluttaja() {
      return this.koulutussopimusData.tila === LomakeTilat.ODOTTAA_TOISEN_KOULUTTAJAN_HYVAKSYNTAA
    }

    get showSigned() {
      return this.koulutussopimusData.tila === LomakeTilat.HYVAKSYTTY
    }

    get erikoistuvanAvatar() {
      return this.form.erikoistuvanAvatar
    }

    get erikoistuvanNimi() {
      return this.form.erikoistuvanNimi
    }

    get erikoistuvanErikoisala() {
      return this.form.erikoistuvanErikoisala ?? ''
    }

    get sendToVastuuhenkilo() {
      return (
        this.form.kouluttajat.length === 1 ||
        this.form.kouluttajat.filter((k) => k.sopimusHyvaksytty).length === 1
      )
    }

    isCurrentKouluttaja(kouluttaja: any) {
      return this.account.id === kouluttaja.kayttajaUserId
    }

    async returnToSender(korjausehdotus: string) {
      const form = {
        ...this.form,
        korjausehdotus: korjausehdotus,
        lahetetty: false
      }
      try {
        this.buttonStates.secondaryButtonLoading = true
        await store.dispatch(`${resolveRolePath()}/putKoulutussopimus`, form)
        this.buttonStates.secondaryButtonLoading = false
        this.$emit('skipRouteExitConfirm', true)
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('koulutussopimus-palautettu-onnistuneesti'))
      } catch {
        toastFail(this, this.$t('koulutussopimus-palautus-epaonnistui'))
      }
    }

    async updateSentForm() {
      this.$emit('skipRouteExitConfirm', true)
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch(`${resolveRolePath()}/putKoulutussopimus`, this.form)
        this.buttonStates.primaryButtonLoading = false
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('koulutussopimus-lisatty-onnistuneesti'))
      } catch {
        toastFail(this, this.$t('koulutussopimuksen-lisaaminen-epaonnistui'))
      }
    }

    onSubmit() {
      if (this.$isVastuuhenkilo()) {
        this.handleSubmitVastuuhenkilo()
      } else {
        this.handleSubmitKouluttaja()
      }
    }

    private handleSubmitKouluttaja() {
      if (this.$refs.kouluttajaKoulutussopimusForm.length === 2) {
        this.$refs.kouluttajaKoulutussopimusForm[0].validateForm()
        this.$refs.kouluttajaKoulutussopimusForm[1].validateForm()
      } else {
        this.$refs.kouluttajaKoulutussopimusForm[0].validateForm()
      }

      if (this.childKouluttajaFormValid) {
        this.$bvModal.show('confirm-send-kouluttaja')
      }
    }

    private handleSubmitVastuuhenkilo() {
      this.$refs.vastuuhenkiloKoulutussopimusForm.validateForm()

      if (this.childVastuuhenkiloFormValid) {
        this.$bvModal.show('confirm-send-vastuuhenkilo')
      }
    }

    async mounted() {
      this.loading = true
      await store.dispatch(`${resolveRolePath()}/getKoejaksot`)

      try {
        const { data } = await (this.$isVastuuhenkilo()
          ? getKoulutussopimusVastuuhenkilo(this.koulutussopimusId)
          : getKoulutussopimusKouluttaja(this.koulutussopimusId))
        this.form = data
        if (this.form.vastuuhenkilo) {
          this.form.vastuuhenkilo.puhelin = this.account.phoneNumber
          this.form.vastuuhenkilo.sahkoposti = this.account.email
        }
        this.loading = false

        if (!this.editable || this.returned) {
          this.$emit('skipRouteExitConfirm', true)
        }
      } catch {
        toastFail(this, this.$t('koulutussopimuksen-hakeminen-epaonnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.replace({ name: 'koejakso' })
      }
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.form.erikoistuvanNimi,
        this.form.erikoistuvanAllekirjoitusaika
      ) as KoejaksonVaiheHyvaksynta
      const hyvaksynnatKouluttajat = hyvaksynnatHelper.mapHyvaksynnatSopimuksenKouluttajat(
        this.form.kouluttajat
      ) as KoejaksonVaiheHyvaksynta[]
      const hyvaksyntaVastuuhenkilo = hyvaksynnatHelper.mapHyvaksyntaVastuuhenkilo(
        this.form.vastuuhenkilo ?? null
      ) as KoejaksonVaiheHyvaksynta

      return [hyvaksyntaErikoistuva, ...hyvaksynnatKouluttajat, hyvaksyntaVastuuhenkilo].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    onSkipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .kouluttaja-section + .kouluttaja-section {
    margin-top: 2.5rem;
  }
</style>
