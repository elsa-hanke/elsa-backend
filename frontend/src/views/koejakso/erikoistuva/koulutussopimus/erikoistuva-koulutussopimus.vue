<template>
  <div class="koulutussopimus col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <h1 class="mb-3">{{ $t('koulutussopimus') }}</h1>
      <div v-if="!loading">
        <b-row lg>
          <b-col>
            <b-alert :show="showReturned" variant="danger" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
                </em>
                <div>
                  <span v-if="kouluttajanKorjausehdotus">
                    {{ $t('koulutussopimus-tila-palautettu-korjattavaksi') }}
                  </span>
                  <span v-else>
                    {{ $t('koulutussopimus-tila-palautettu-korjattavaksi-vastuuhenkilo') }}
                  </span>
                  <span class="d-block">
                    {{ $t('syy') }}&nbsp;
                    <span class="font-weight-500">{{ korjausehdotus }}</span>
                  </span>
                </div>
              </div>
            </b-alert>
            <div v-if="editable">
              <p class="mb-4">{{ $t('koulutussopimus-ingressi-1') }}</p>
              <p class="mb-0">
                {{ $t('koulutussopimus-ingressi-2') }}
                <b-link :to="{ name: 'koejakso-yleiset-tavoitteet' }">
                  {{ $t('koejakso-tavoitteet-linkki') }}
                </b-link>
              </p>
            </div>
            <b-alert :show="showWaitingForAcceptance" variant="dark" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                </em>
                <div>{{ $t('koulutussopimus-tila-odottaa-hyvaksyntaa') }}</div>
              </div>
            </b-alert>
            <b-alert variant="success" :show="showAcceptedByEveryone">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                </em>
                <div>
                  {{ $t('koulutussopimus-tila-hyvaksytty') }}
                </div>
              </div>
            </b-alert>
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
        <b-row>
          <b-col>
            <koulutussopimus-form
              v-if="editable"
              :data="koejaksoData.koulutussopimus"
              :account="account"
              :kouluttajat="kouluttajat"
              :vastuuhenkilo="vastuuhenkilo"
              :yliopistot="yliopistot"
              @saveAndExit="onSaveDraftAndExit"
              @submit="onSubmit"
              @skipRouteExitConfirm="skipRouteExitConfirm"
            ></koulutussopimus-form>

            <koulutussopimus-readonly
              v-if="!editable"
              :data="koejaksoData.koulutussopimus"
            ></koulutussopimus-readonly>
          </b-col>
        </b-row>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>
    <b-row>
      <b-col class="text-right">
        <div v-if="deletable">
          <elsa-button
            v-if="!loading"
            variant="outline-danger"
            class="ml-4 px-6"
            @click="onValidateAndConfirm('confirm-form-delete')"
          >
            {{ $t('tyhjenna-lomake') }}
          </elsa-button>
        </div>
      </b-col>
    </b-row>
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
  import Component from 'vue-class-component'
  import { Vue } from 'vue-property-decorator'

  import { getKoulutussopimusLomake } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import store from '@/store'
  import {
    KoulutussopimusLomake,
    Koejakso,
    KoejaksonVaiheButtonStates,
    Vastuuhenkilo,
    Yliopisto
  } from '@/types'
  import { LomakeTilat } from '@/utils/constants'
  import { checkCurrentRouteAndRedirect } from '@/utils/functions'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import KoulutussopimusForm from '@/views/koejakso/erikoistuva/koulutussopimus/koulutussopimus-form.vue'
  import KoulutussopimusReadonly from '@/views/koejakso/erikoistuva/koulutussopimus/koulutussopimus-readonly.vue'

  @Component({
    components: {
      ElsaConfirmationModal,
      ElsaButton,
      ErikoistuvaDetails,
      KoulutussopimusForm,
      KoulutussopimusReadonly
    }
  })
  export default class ErikoistuvaKoulutussopimus extends Vue {
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
        text: this.$t('koulutussopimus'),
        active: true
      }
    ]
    loading = true
    koulutussopimusLomake: KoulutussopimusLomake | null = null
    vastuuhenkilo: Vastuuhenkilo | null = null
    yliopistot: Yliopisto[] = []

    get account() {
      return store.getters['auth/account']
    }

    get editable() {
      if (this.account.impersonated) {
        return false
      }
      switch (this.koejaksoData.koulutusSopimuksenTila) {
        case LomakeTilat.PALAUTETTU_KORJATTAVAKSI:
          return true
        case LomakeTilat.UUSI:
          return true
        case LomakeTilat.TALLENNETTU_KESKENERAISENA:
          return true
      }
      return false
    }

    get deletable() {
      if (this.koejaksoData === null) {
        return false
      }
      return this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get showWaitingForAcceptance() {
      return (
        this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA ||
        this.koejaksoData.koulutusSopimuksenTila ===
          LomakeTilat.ODOTTAA_TOISEN_KOULUTTAJAN_HYVAKSYNTAA ||
        this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
      )
    }

    onValidateAndConfirm(modalId: string) {
      return this.$bvModal.show(modalId)
    }

    get showReturned() {
      return this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get showAcceptedByEveryone() {
      return this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.HYVAKSYTTY
    }

    get kouluttajat() {
      return store.getters['erikoistuva/kouluttajat']
    }

    get koejaksoData(): Koejakso {
      return store.getters['erikoistuva/koejakso']
    }

    get korjausehdotus() {
      return (
        this.koejaksoData.koulutussopimus?.korjausehdotus ||
        this.koejaksoData.koulutussopimus?.vastuuhenkilonKorjausehdotus
      )
    }

    get kouluttajanKorjausehdotus() {
      return this.koejaksoData.koulutussopimus?.korjausehdotus
    }

    setKoejaksoData() {
      if (this.koejaksoData.koulutussopimus) {
        this.koulutussopimusLomake = this.koejaksoData.koulutussopimus
      }

      if (!this.editable) {
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    async onSaveDraftAndExit(
      form: KoulutussopimusLomake,
      buttonStates: KoejaksonVaiheButtonStates
    ) {
      buttonStates.secondaryButtonLoading = true
      this.koulutussopimusLomake = form
      try {
        if (this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.UUSI) {
          await store.dispatch('erikoistuva/postKoulutussopimus', this.koulutussopimusLomake)
        } else if (
          this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.TALLENNETTU_KESKENERAISENA ||
          this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
        ) {
          await store.dispatch('erikoistuva/putKoulutussopimus', this.koulutussopimusLomake)
        }

        toastSuccess(this, this.$t('koulutussopimus-tallennettu-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
      } catch {
        toastFail(this, this.$t('koulutussopimuksen-tallennus-epaonnistui'))
      }
      buttonStates.secondaryButtonLoading = false
    }

    async saveNewForm() {
      try {
        await store.dispatch('erikoistuva/postKoulutussopimus', this.koulutussopimusLomake)
        toastSuccess(this, this.$t('koulutussopimus-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
      } catch {
        toastFail(this, this.$t('koulutussopimuksen-lisaaminen-epaonnistui'))
      }
    }

    async updateSentForm() {
      try {
        await store.dispatch('erikoistuva/putKoulutussopimus', this.koulutussopimusLomake)
        toastSuccess(this, this.$t('koulutussopimus-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        this.setKoejaksoData()
      } catch {
        toastFail(this, this.$t('koulutussopimuksen-lisaaminen-epaonnistui'))
      }
    }

    async onSubmit(form: KoulutussopimusLomake, buttonStates: KoejaksonVaiheButtonStates) {
      buttonStates.primaryButtonLoading = true
      this.koulutussopimusLomake = form
      this.koulutussopimusLomake.lahetetty = true

      if (
        this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.TALLENNETTU_KESKENERAISENA ||
        this.koejaksoData.koulutusSopimuksenTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
      ) {
        await this.updateSentForm()
      } else {
        await this.saveNewForm()
      }
      buttonStates.primaryButtonLoading = false
    }

    watch() {
      if (!this.editable) {
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    async onFormDelete() {
      try {
        await store.dispatch('erikoistuva/deleteKoulutussopimus', this.koulutussopimusLomake)
        toastSuccess(this, this.$t('lomake-tyhjennetty-onnistuneesti'))
        if (this.koulutussopimusLomake?.id) {
          this.koulutussopimusLomake.id = null
        }
      } catch {
        toastFail(this, this.$t('lomakkeen-tyhjennys-epaonnistui'))
      }
    }

    async mounted() {
      this.loading = true

      if (!this.koejaksoData) {
        await store.dispatch('erikoistuva/getKoejakso')
      }
      await store.dispatch('erikoistuva/getKouluttajat')

      this.setKoejaksoData()

      const koulutussopimusLomake = (await getKoulutussopimusLomake()).data
      this.yliopistot = koulutussopimusLomake.yliopistot.filter(
        (y: any) => y.nimi !== this.account.erikoistuvaLaakari.yliopisto
      )
      this.vastuuhenkilo = koulutussopimusLomake.vastuuhenkilo

      this.loading = false

      if (!this.editable) {
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>
