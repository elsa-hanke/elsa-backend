<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <h1 class="mb-3">{{ $t('kehittamistoimenpiteet-otsikko') }}</h1>
      <div v-if="!loading && kehittamistoimenpiteet">
        <div v-if="editableForEsimies">
          <p>{{ $t('kehittamistoimenpiteet-esimies-ingressi') }}</p>
        </div>
        <div v-else-if="editable">
          <p>{{ $t('kehittamistoimenpiteet-kouluttaja-ingressi') }}</p>
        </div>
        <b-alert :show="waitingForLahiesimiesOrErikoistuva" variant="dark" class="mt-3">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
            </em>
            <div>
              {{ $t('kehittamistoimenpiteet-kouluttaja-hyvaksytty') }}
            </div>
          </div>
        </b-alert>
        <b-alert
          :show="returned"
          :variant="isCurrentUserLahiesimies ? 'dark' : 'danger'"
          class="mt-3"
        >
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon
                v-if="isCurrentUserLahiesimies"
                :icon="['fas', 'info-circle']"
                class="text-muted mr-2"
              />
              <font-awesome-icon v-else :icon="['fas', 'exclamation-circle']" class="mr-2" />
            </em>
            <div>
              <span v-if="isCurrentUserLahiesimies">
                {{ $t('kehittamistoimenpiteet-palautettu-kouluttajalle-muokattavaksi-esimies') }}
              </span>
              <span v-else>
                {{ $t('kehittamistoimenpiteet-palautettu-kouluttajalle-muokattavaksi-kouluttaja') }}
              </span>
              <span class="d-block">
                {{ $t('syy') }} {{ kehittamistoimenpiteet.korjausehdotus }}
              </span>
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
        <hr />
        <erikoistuva-details
          :avatar="erikoistuvanAvatar"
          :name="erikoistuvanNimi"
          :erikoisala="kehittamistoimenpiteet.erikoistuvanErikoisala"
          :opiskelijatunnus="kehittamistoimenpiteet.erikoistuvanOpiskelijatunnus"
          :yliopisto="kehittamistoimenpiteet.erikoistuvanYliopisto"
          :show-birthdate="false"
        />
        <hr />
        <b-row v-if="kehittamistoimenpiteet.kehittamistoimenpideKategoriat.length > 0">
          <b-col lg="8">
            <h5>{{ $t('keskustelu-ja-toimenpiteet-tarpeen-ennen-hyvaksymista') }}</h5>
            <ul class="pl-4">
              <li
                v-for="kategoria in kehittamistoimenpiteet.kehittamistoimenpideKategoriat"
                :key="kategoria"
              >
                {{
                  naytaKehittamistoimenpideKategoria(kategoria, kehittamistoimenpiteet.muuKategoria)
                }}
              </li>
            </ul>
          </b-col>
        </b-row>
        <b-row>
          <b-col v-if="kehittamistoimenpiteet.kehittamistoimenpiteetKuvaus">
            <h5>{{ $t('selvitys-kehittamistoimenpiteista') }}</h5>
            <p>{{ kehittamistoimenpiteet.kehittamistoimenpiteetKuvaus }}</p>
          </b-col>
        </b-row>
        <hr />
        <h3 class="mb-3">{{ $t('kehittamistoimenpiteiden-arviointi') }}</h3>
        <b-row>
          <b-col lg="8">
            <div v-if="editable">
              <b-form-group>
                <template #default="{ uid }">
                  <b-form-radio-group
                    :id="uid"
                    v-model="kehittamistoimenpiteet.kehittamistoimenpiteetRiittavat"
                    :options="kehittamistoimenpiteidenArviointiOptions"
                    :state="validateState('kehittamistoimenpiteetRiittavat')"
                    stacked
                  ></b-form-radio-group>
                  <b-form-invalid-feedback
                    :id="`${uid}-feedback`"
                    :state="validateState('kehittamistoimenpiteetRiittavat')"
                  >
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                </template>
              </b-form-group>
            </div>
            <div v-else>
              <p v-if="kehittamistoimenpiteet.kehittamistoimenpiteetRiittavat">
                {{ $t('kehittamistoimenpiteet-riittavat') }}
              </p>
              <p v-else>{{ $t('kehittamistoimenpiteet-ei-riittavat') }}</p>
            </div>
          </b-col>
        </b-row>
        <hr />
        <koulutuspaikan-arvioijat
          :lahikouluttaja="kehittamistoimenpiteet.lahikouluttaja"
          :lahiesimies="kehittamistoimenpiteet.lahiesimies"
          :is-readonly="true"
        />
        <hr />
        <div v-if="hyvaksynnat.length > 0">
          <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
        </div>
        <div v-if="editable || editableForEsimies">
          <hr v-if="hyvaksynnat.length > 0" />
          <b-row>
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
                v-if="
                  isCurrentUserLahiesimies &&
                  kehittamistoimenpiteet.lahiesimies.id != kehittamistoimenpiteet.lahikouluttaja.id
                "
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
                class="my-2 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block"
                style="min-width: 14rem"
                variant="primary"
                :disabled="buttonStates.secondaryButtonLoading"
                :loading="buttonStates.primaryButtonLoading"
                @click="onValidateAndConfirm"
              >
                {{ $t('hyvaksy-laheta') }}
              </elsa-button>
            </b-col>
          </b-row>
          <b-row>
            <elsa-form-error :active="$v.$anyError" />
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
      :text="
        isCurrentUserLahiesimies
          ? $t('vahvista-koejakson-vaihe-erikoistuvalle')
          : $t('vahvista-koejakson-vaihe-esimiehelle')
      "
      :submit-text="$t('hyvaksy-laheta')"
      @submit="onSign"
    />
    <elsa-return-to-sender-modal
      id="return-to-sender"
      :title="$t('palauta-kouluttajalle-muokattavaksi')"
      @submit="onReturnToSender"
    />
  </div>
</template>

<script lang="ts">
  import _get from 'lodash/get'
  import Component from 'vue-class-component'
  import { Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import { getKehittamistoimenpiteet as getKehittamistoimenpiteetKouluttaja } from '@/api/kouluttaja'
  import { getKehittamistoimenpiteet as getKehittamistoimenpiteetVastuuhenkilo } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import store from '@/store'
  import {
    KehittamistoimenpiteetLomake,
    KoejaksonVaiheButtonStates,
    KoejaksonVaiheHyvaksynta,
    KoejaksonVaihe
  } from '@/types'
  import { resolveRolePath } from '@/utils/apiRolePathResolver'
  import { LomakeTilat, LomakeTyypit } from '@/utils/constants'
  import { checkCurrentRouteAndRedirect } from '@/utils/functions'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ErikoistuvaDetails,
      ElsaFormGroup,
      ElsaFormError,
      ElsaButton,
      ElsaConfirmationModal,
      ElsaReturnToSenderModal,
      KoulutuspaikanArvioijat,
      KoejaksonVaiheHyvaksynnat
    },
    validations: {
      kehittamistoimenpiteet: {
        kehittamistoimenpiteetRiittavat: {
          required
        }
      }
    }
  })
  export default class ArviointilomakeKehittamistoimenpiteet extends Mixins(validationMixin) {
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
        text: this.$t('kehittamistoimenpiteet-otsikko'),
        active: true
      }
    ]

    kehittamistoimenpiteidenArviointiOptions = [
      {
        text: this.$t('kehittamistoimenpiteet-riittavat'),
        value: true
      },
      {
        text: this.$t('kehittamistoimenpiteet-ei-riittavat'),
        value: false
      }
    ]

    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }

    loading = true

    kehittamistoimenpiteet: KehittamistoimenpiteetLomake | null = null

    validateState(value: string) {
      const form = this.$v.kehittamistoimenpiteet
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    get kehittamistoimenpiteetTila() {
      return store.getters[`${resolveRolePath()}/koejaksot`].find(
        (k: KoejaksonVaihe) =>
          k.id === this.kehittamistoimenpiteet?.id &&
          k.tyyppi === LomakeTyypit.KEHITTAMISTOIMENPITEET
      )?.tila
    }

    get kehittamistoimenpiteetId() {
      return Number(this.$route.params.id)
    }

    get editable() {
      if (
        this.kehittamistoimenpiteet?.lahiesimies.id ==
          this.kehittamistoimenpiteet?.lahikouluttaja.id &&
        !this.kehittamistoimenpiteet?.lahiesimies.sopimusHyvaksytty &&
        !this.kehittamistoimenpiteet?.lahikouluttaja.sopimusHyvaksytty
      ) {
        return true
      }
      return (
        (this.$isKouluttaja() || this.$isVastuuhenkilo()) &&
        !this.isCurrentUserLahiesimies &&
        !this.kehittamistoimenpiteet?.lahikouluttaja.sopimusHyvaksytty
      )
    }

    get editableForEsimies() {
      return (
        this.isCurrentUserLahiesimies &&
        this.kehittamistoimenpiteet?.lahikouluttaja.sopimusHyvaksytty &&
        !this.kehittamistoimenpiteet?.lahiesimies.sopimusHyvaksytty
      )
    }

    get isCurrentUserLahiesimies() {
      const currentUser = store.getters['auth/account']
      return this.kehittamistoimenpiteet?.lahiesimies.kayttajaUserId === currentUser.id
    }

    get erikoistuvanAvatar() {
      return this.kehittamistoimenpiteet?.erikoistuvanAvatar
    }

    get erikoistuvanNimi() {
      return this.kehittamistoimenpiteet?.erikoistuvanNimi
    }

    get waitingForLahiesimiesOrErikoistuva() {
      return (
        !this.isCurrentUserLahiesimies &&
        this.kehittamistoimenpiteet?.lahikouluttaja.sopimusHyvaksytty &&
        !this.kehittamistoimenpiteet?.lahiesimies.sopimusHyvaksytty
      )
    }

    get acceptedByEveryone() {
      return this.kehittamistoimenpiteet?.lahiesimies.sopimusHyvaksytty
    }

    get returned() {
      return this.kehittamistoimenpiteetTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.kehittamistoimenpiteet?.erikoistuvanNimi,
        this.kehittamistoimenpiteet?.erikoistuvanKuittausaika
      ) as KoejaksonVaiheHyvaksynta
      const hyvaksyntaLahikouluttaja = hyvaksynnatHelper.mapHyvaksyntaLahikouluttaja(
        this,
        this.kehittamistoimenpiteet?.lahikouluttaja
      )
      const hyvaksyntaLahiesimies = hyvaksynnatHelper.mapHyvaksyntaLahiesimies(
        this,
        this.kehittamistoimenpiteet?.lahiesimies
      )

      return [hyvaksyntaLahikouluttaja, hyvaksyntaLahiesimies, hyvaksyntaErikoistuva].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    async onReturnToSender(korjausehdotus: string) {
      const form = {
        ...this.kehittamistoimenpiteet,
        korjausehdotus: korjausehdotus,
        lahetetty: false
      }

      try {
        this.buttonStates.secondaryButtonLoading = true
        await store.dispatch('kouluttaja/putKehittamistoimenpiteet', form)
        this.buttonStates.secondaryButtonLoading = false
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('kehittamistoimenpiteet-palautus-onnistui'))
      } catch {
        toastFail(this, this.$t('kehittamistoimenpiteet-palautus-epaonnistui'))
      }
    }

    async onSign() {
      if (!this.kehittamistoimenpiteet) return
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch('kouluttaja/putKehittamistoimenpiteet', this.kehittamistoimenpiteet)
        this.buttonStates.primaryButtonLoading = false
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('kehittamistoimenpiteet-lahetys-onnistui'))
      } catch {
        toastFail(this, this.$t('kehittamistoimenpiteet-lahetys-epaonnistui'))
      }
    }

    naytaKehittamistoimenpideKategoria(kategoria: string, muuKategoria: string | null) {
      if (kategoria === 'MUU') return muuKategoria
      return this.$t('kehittamistoimenpidekategoria-' + kategoria)
    }

    onValidateAndConfirm() {
      if (!this.isCurrentUserLahiesimies) {
        this.$v.$touch()
        if (this.$v.$anyError) {
          return
        }
      }
      return this.$bvModal.show('confirm-send')
    }

    async mounted() {
      this.loading = true
      await store.dispatch(`${resolveRolePath()}/getKoejaksot`)

      try {
        const { data } = await (this.$isVastuuhenkilo()
          ? getKehittamistoimenpiteetVastuuhenkilo(this.kehittamistoimenpiteetId)
          : getKehittamistoimenpiteetKouluttaja(this.kehittamistoimenpiteetId))
        this.kehittamistoimenpiteet = data
        this.loading = false
      } catch {
        toastFail(this, this.$t('kehittamistoimenpiteiden-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'koejakso' })
      }
    }
  }
</script>
