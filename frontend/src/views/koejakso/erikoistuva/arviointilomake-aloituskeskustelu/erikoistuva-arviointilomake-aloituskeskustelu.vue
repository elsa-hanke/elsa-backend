<template>
  <div class="koulutussopimus col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <h1 class="mb-3">{{ $t('koejakson-aloituskeskustelu') }}</h1>
      <div v-if="!loading">
        <b-row lg>
          <b-col>
            <b-alert :show="showReturned" variant="danger" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
                </em>
                <div>
                  {{ $t('aloituskeskustelu-tila-palautettu-korjattavaksi') }}
                  <span class="d-block">
                    {{ $t('syy') }}&nbsp;
                    <span class="font-weight-500">{{ korjausehdotus }}</span>
                  </span>
                </div>
              </div>
            </b-alert>
            <div v-if="editable">
              <p>{{ $t('koejakson-aloituskeskustelu-ingressi-1') }}</p>
              <p>
                {{ $t('koejakson-aloituskeskustelu-ingressi-2') }}
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
                <div>
                  {{ $t('aloituskeskustelu-tila-odottaa-hyvaksyntaa') }}
                </div>
              </div>
            </b-alert>
            <b-alert variant="success" :show="showAcceptedByEveryone">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                </em>
                <span>{{ $t('aloituskeskustelu-tila-hyvaksytty') }}</span>
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
              :yliopisto="account.erikoistuvaLaakari.yliopisto"
              :syntymaaika="account.erikoistuvaLaakari.syntymaaika"
            ></erikoistuva-details>
          </b-col>
        </b-row>
        <hr />
        <arviointilomake-aloituskeskustelu-form
          v-if="editable"
          :account="account"
          :data="koejaksoData.aloituskeskustelu"
          :kouluttajat="kouluttajat"
          :henkilot="kouluttajat"
          @saveAndExit="onSaveDraftAndExit"
          @submit="onSubmit"
          @skipRouteExitConfirm="skipRouteExitConfirm"
        ></arviointilomake-aloituskeskustelu-form>

        <arviointilomake-aloituskeskustelu-readonly
          v-if="!editable"
          :data="koejaksoData.aloituskeskustelu"
        ></arviointilomake-aloituskeskustelu-readonly>

        <div v-if="deletable">
          <b-row>
            <b-col class="text-right">
              <elsa-button
                :loading="false"
                variant="outline-danger"
                class="ml-4 px-6"
                @click="onValidateAndConfirm('confirm-form-delete')"
              >
                {{ $t('tyhjenna-lomake') }}
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
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import store from '@/store'
  import { AloituskeskusteluLomake, Koejakso, KoejaksonVaiheButtonStates } from '@/types'
  import { LomakeTilat } from '@/utils/constants'
  import { checkCurrentRouteAndRedirect } from '@/utils/functions'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import ArviointilomakeAloituskeskusteluForm from '@/views/koejakso/erikoistuva/arviointilomake-aloituskeskustelu/arviointilomake-aloituskeskustelu-form.vue'
  import ArviointilomakeAloituskeskusteluReadonly from '@/views/koejakso/erikoistuva/arviointilomake-aloituskeskustelu/arviointilomake-aloituskeskustelu-readonly.vue'

  @Component({
    components: {
      ElsaConfirmationModal,
      ElsaButton,
      ErikoistuvaDetails,
      ArviointilomakeAloituskeskusteluForm,
      ArviointilomakeAloituskeskusteluReadonly
    }
  })
  export default class ErikoistuvaArviointilomakeAloituskeskustelu extends Vue {
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
        text: this.$t('koejakson-aloituskeskustelu'),
        active: true
      }
    ]
    loading = true
    aloituskeskusteluLomake: null | AloituskeskusteluLomake = null

    get account() {
      return store.getters['auth/account']
    }

    get editable() {
      if (this.account.impersonated) {
        return false
      }
      switch (this.koejaksoData.aloituskeskustelunTila) {
        case LomakeTilat.PALAUTETTU_KORJATTAVAKSI:
          return true
        case LomakeTilat.UUSI:
          return true
        case LomakeTilat.TALLENNETTU_KESKENERAISENA:
          return true
      }
      return false
    }

    get showWaitingForAcceptance() {
      return (
        this.koejaksoData.aloituskeskustelunTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA ||
        this.koejaksoData.aloituskeskustelunTila === LomakeTilat.ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
      )
    }

    get showReturned() {
      return this.koejaksoData.aloituskeskustelunTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get showAcceptedByEveryone() {
      return this.koejaksoData.aloituskeskustelunTila === LomakeTilat.HYVAKSYTTY
    }

    get kouluttajat() {
      return store.getters['erikoistuva/kouluttajatJaVastuuhenkilot'] || []
    }

    get koejaksoData(): Koejakso {
      return store.getters['erikoistuva/koejakso']
    }

    get korjausehdotus() {
      return this.koejaksoData.aloituskeskustelu?.korjausehdotus
    }

    setKoejaksoData() {
      if (this.koejaksoData.aloituskeskustelu) {
        this.aloituskeskusteluLomake = this.koejaksoData.aloituskeskustelu
      }
      if (!this.editable) {
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    async onSaveDraftAndExit(
      form: AloituskeskusteluLomake,
      buttonStates: KoejaksonVaiheButtonStates
    ) {
      buttonStates.secondaryButtonLoading = true
      this.aloituskeskusteluLomake = form
      try {
        if (this.koejaksoData.aloituskeskustelunTila === LomakeTilat.UUSI) {
          await store.dispatch('erikoistuva/postAloituskeskustelu', this.aloituskeskusteluLomake)
        } else if (
          this.koejaksoData.aloituskeskustelunTila === LomakeTilat.TALLENNETTU_KESKENERAISENA
        ) {
          await store.dispatch('erikoistuva/putAloituskeskustelu', this.aloituskeskusteluLomake)
        }

        toastSuccess(this, this.$t('aloituskeskustelu-tallennettu-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
      } catch {
        toastFail(this, this.$t('aloituskeskustelu-tallennus-epaonnistui'))
      }
      buttonStates.secondaryButtonLoading = false
    }

    async saveNewForm() {
      try {
        await store.dispatch('erikoistuva/postAloituskeskustelu', this.aloituskeskusteluLomake)
        toastSuccess(this, this.$t('aloituskeskustelu-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
      } catch {
        toastFail(this, this.$t('aloituskeskustelu-lisaaminen-epaonnistui'))
      }
    }

    async updateSentForm() {
      try {
        await store.dispatch('erikoistuva/putAloituskeskustelu', this.aloituskeskusteluLomake)
        toastSuccess(this, this.$t('aloituskeskustelu-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
      } catch {
        toastFail(this, this.$t('aloituskeskustelu-lisaaminen-epaonnistui'))
      }
    }

    async onSubmit(form: AloituskeskusteluLomake, buttonStates: KoejaksonVaiheButtonStates) {
      buttonStates.primaryButtonLoading = true
      this.aloituskeskusteluLomake = form
      this.aloituskeskusteluLomake.lahetetty = true

      if (
        this.koejaksoData.aloituskeskustelunTila === LomakeTilat.TALLENNETTU_KESKENERAISENA ||
        this.koejaksoData.aloituskeskustelunTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
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

    get deletable() {
      return this.koejaksoData.aloituskeskustelunTila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    async onFormDelete() {
      try {
        await store.dispatch('erikoistuva/deleteAloituskeskustelu', this.aloituskeskusteluLomake)
        toastSuccess(this, this.$t('lomake-tyhjennetty-onnistuneesti'))
      } catch {
        toastFail(this, this.$t('lomakkeen-tyhjennys-epaonnistui'))
      }
    }

    onValidateAndConfirm(modalId: string) {
      return this.$bvModal.show(modalId)
    }

    async mounted() {
      this.loading = true
      if (!this.koejaksoData) {
        await store.dispatch('erikoistuva/getKoejakso')
      }
      await store.dispatch('erikoistuva/getKouluttajatJaVastuuhenkilot')
      this.setKoejaksoData()
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
