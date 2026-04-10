<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <h1 class="mb-3">{{ $t('loppukeskustelu-kouluttaja') }}</h1>

      <div v-if="!loading && loppukeskustelu">
        <div v-if="editableForEsimies">
          <p>{{ $t('loppukeskustelu-esimies-ingressi') }}</p>
        </div>
        <div v-else-if="editable">
          <p>{{ $t('loppukeskustelu-kouluttaja-ingressi') }}</p>
        </div>

        <b-alert :show="showWaitingForLahiesimiesOrErikoistuva" variant="dark" class="mt-3">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
            </em>
            <div>
              {{ $t('loppukeskustelu-kouluttaja-hyvaksytty') }}
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
                {{ $t('loppukeskustelu-palautettu-kouluttajalle-muokattavaksi-esimies') }}
              </span>
              <span v-else>
                {{ $t('loppukeskustelu-palautettu-kouluttajalle-muokattavaksi-kouluttaja') }}
              </span>
              <span class="d-block">{{ $t('syy') }} {{ loppukeskustelu.korjausehdotus }}</span>
            </div>
          </div>
        </b-alert>

        <b-alert variant="success" :show="acceptedByEveryone">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
            </em>
            <span>{{ $t('loppukeskustelu-tila-hyvaksytty') }}</span>
          </div>
        </b-alert>

        <hr />

        <erikoistuva-details
          :avatar="erikoistuvanAvatar"
          :name="erikoistuvanNimi"
          :erikoisala="loppukeskustelu.erikoistuvanErikoisala"
          :opiskelijatunnus="loppukeskustelu.erikoistuvanOpiskelijatunnus"
          :yliopisto="loppukeskustelu.erikoistuvanYliopisto"
          :show-birthdate="false"
        />

        <hr />

        <div>
          <h3 class="mb-3">{{ $t('aloituskeskustelu-tavoitteet') }}</h3>
          <h5>{{ $t('koejakso-osaamistavoitteet') }}</h5>
          <p>{{ loppukeskustelu.koejaksonOsaamistavoitteet }}</p>
          <hr />
        </div>

        <div>
          <h3 class="mb-3">{{ $t('soveltuvuus-erikoisalalle-valiarvioinnin-perusteella') }}</h3>
          <b-row>
            <b-col lg="8">
              <h5>{{ $t('edistyminen-osaamistavoitteiden-mukaista') }}</h5>
              <p>
                {{
                  loppukeskustelu.edistyminenTavoitteidenMukaista
                    ? $t('kylla')
                    : $t('ei-huolenaiheita-on')
                }}
              </p>
            </b-col>
          </b-row>
          <b-row v-if="loppukeskustelu.edistyminenTavoitteidenMukaista === false">
            <b-col lg="8">
              <h5>{{ $t('keskustelu-ja-toimenpiteet-tarpeen-ennen-hyvaksymista') }}</h5>
              <ul class="pl-4">
                <li
                  v-for="kategoria in loppukeskustelu.kehittamistoimenpideKategoriat"
                  :key="kategoria"
                >
                  {{ naytaKehittamistoimenpideKategoria(kategoria, loppukeskustelu.muuKategoria) }}
                </li>
              </ul>
            </b-col>
          </b-row>
          <b-row>
            <b-col v-if="loppukeskustelu.vahvuudet">
              <h5>{{ $t('vahvuudet') }}</h5>
              <p>{{ loppukeskustelu.vahvuudet }}</p>
            </b-col>
          </b-row>
          <b-row>
            <b-col v-if="loppukeskustelu.kehittamistoimenpiteet">
              <h5>{{ $t('selvitys-kehittamistoimenpiteista') }}</h5>
              <p>{{ loppukeskustelu.kehittamistoimenpiteet }}</p>
            </b-col>
          </b-row>
          <hr />
        </div>

        <div v-if="loppukeskustelu.kehittamistoimenpiteetRiittavat != null">
          <h3 class="mb-3">{{ $t('kehittamistoimenpiteiden-arviointi') }}</h3>
          <p v-if="loppukeskustelu.kehittamistoimenpiteetRiittavat">
            {{ $t('kehittamistoimenpiteet-riittavat') }}
          </p>
          <p v-else>{{ $t('kehittamistoimenpiteet-ei-riittavat') }}</p>
          <hr />
        </div>

        <div>
          <b-row>
            <b-col lg="8">
              <div v-if="!editable">
                <h3>{{ $t('koejakson-tavoitteet-on-kasitelty-loppukeskustelussa') }}</h3>
                <p>
                  {{
                    loppukeskustelu.esitetaanKoejaksonHyvaksymista
                      ? $t('loppukeskustelu-kayty-hyvaksytty')
                      : $t('loppukeskustelu-kayty-jatkotoimenpiteet')
                  }}
                </p>
                <div v-if="!loppukeskustelu.esitetaanKoejaksonHyvaksymista">
                  <h5>{{ $t('selvitys-jatkotoimista') }}</h5>
                  <p>{{ loppukeskustelu.jatkotoimenpiteet }}</p>
                </div>
              </div>
              <elsa-form-group
                v-else
                :label="$t('koejakson-tavoitteet-on-kasitelty-loppukeskustelussa')"
                :required="true"
              >
                <template #default="{ uid }">
                  <b-form-radio-group
                    :id="uid"
                    v-model="loppukeskustelu.esitetaanKoejaksonHyvaksymista"
                    :options="hyvaksyttyVaihtoehdot"
                    :state="validateState('esitetaanKoejaksonHyvaksymista')"
                    stacked
                  ></b-form-radio-group>
                  <b-form-invalid-feedback
                    :id="`${uid}-feedback`"
                    :state="validateState('esitetaanKoejaksonHyvaksymista')"
                  >
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                  <elsa-form-group
                    v-if="loppukeskustelu.esitetaanKoejaksonHyvaksymista === false"
                    class="mt-4"
                    :label="$t('selvitys-jatkotoimista')"
                    :required="true"
                  >
                    <template #default="{ jatkotoimenpiteetUid }">
                      <b-form-textarea
                        :id="jatkotoimenpiteetUid"
                        v-model="loppukeskustelu.jatkotoimenpiteet"
                        :state="validateState('jatkotoimenpiteet')"
                        rows="7"
                        class="textarea-min-height"
                      ></b-form-textarea>
                      <b-form-invalid-feedback :id="`${uid}-feedback`">
                        {{ $t('pakollinen-tieto') }}
                      </b-form-invalid-feedback>
                    </template>
                  </elsa-form-group>
                </template>
              </elsa-form-group>
            </b-col>
          </b-row>
          <hr />
        </div>
        <koulutuspaikan-arvioijat
          :lahikouluttaja="loppukeskustelu.lahikouluttaja"
          :lahiesimies="loppukeskustelu.lahiesimies"
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
                  loppukeskustelu.lahiesimies.id != loppukeskustelu.lahikouluttaja.id
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
  import { required, requiredIf } from 'vuelidate/lib/validators'

  import { getLoppukeskustelu as getLoppukeskusteluKouluttaja } from '@/api/kouluttaja'
  import { getLoppukeskustelu as getLoppukeskusteluVastuuhenkilo } from '@/api/vastuuhenkilo'
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
    LoppukeskusteluLomake,
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
      loppukeskustelu: {
        esitetaanKoejaksonHyvaksymista: {
          required
        },
        jatkotoimenpiteet: {
          required: requiredIf((value) => {
            return value.esitetaanKoejaksonHyvaksymista === false
          })
        }
      }
    }
  })
  export default class ArviointilomakeLoppukeskustelu extends Mixins(validationMixin) {
    skipRouteExitConfirm!: boolean
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
        text: this.$t('loppukeskustelu-kouluttaja'),
        active: true
      }
    ]

    hyvaksyttyVaihtoehdot = [
      {
        text: this.$t('loppukeskustelu-kayty-hyvaksytty'),
        value: true
      },
      {
        text: this.$t('loppukeskustelu-kayty-jatkotoimenpiteet'),
        value: false
      }
    ]

    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }

    loading = true

    loppukeskustelu: LoppukeskusteluLomake | null = null

    validateState(value: string) {
      const form = this.$v.loppukeskustelu
      const { $dirty, $error } = _get(form, value) as any
      return $dirty ? ($error ? false : null) : null
    }

    get loppukeskustelunTila() {
      return store.getters[`${resolveRolePath()}/koejaksot`].find(
        (k: KoejaksonVaihe) =>
          k.id === this.loppukeskustelu?.id && k.tyyppi === LomakeTyypit.LOPPUKESKUSTELU
      )?.tila
    }

    get loppukeskusteluId() {
      return Number(this.$route.params.id)
    }

    get editable() {
      if (
        this.loppukeskustelu?.lahiesimies.id == this.loppukeskustelu?.lahikouluttaja.id &&
        !this.loppukeskustelu?.lahiesimies.sopimusHyvaksytty &&
        !this.loppukeskustelu?.lahikouluttaja.sopimusHyvaksytty
      ) {
        return true
      }
      return (
        (this.$isKouluttaja() || this.$isVastuuhenkilo()) &&
        !this.isCurrentUserLahiesimies &&
        !this.loppukeskustelu?.lahikouluttaja.sopimusHyvaksytty
      )
    }

    get editableForEsimies() {
      return (
        this.isCurrentUserLahiesimies &&
        this.loppukeskustelu?.lahikouluttaja.sopimusHyvaksytty &&
        !this.loppukeskustelu?.lahiesimies.sopimusHyvaksytty
      )
    }

    get isCurrentUserLahiesimies() {
      const currentUser = store.getters['auth/account']
      return this.loppukeskustelu?.lahiesimies.kayttajaUserId === currentUser.id
    }

    get erikoistuvanAvatar() {
      return this.loppukeskustelu?.erikoistuvanAvatar
    }

    get erikoistuvanNimi() {
      return this.loppukeskustelu?.erikoistuvanNimi
    }

    get showWaitingForLahiesimiesOrErikoistuva() {
      return (
        !this.isCurrentUserLahiesimies &&
        this.loppukeskustelu?.lahikouluttaja.sopimusHyvaksytty &&
        !this.loppukeskustelu?.lahiesimies.sopimusHyvaksytty
      )
    }

    get acceptedByEveryone() {
      return this.loppukeskustelu?.lahiesimies.sopimusHyvaksytty
    }

    get returned() {
      return this.loppukeskustelunTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.loppukeskustelu?.erikoistuvanNimi,
        this.loppukeskustelu?.erikoistuvanKuittausaika
      )
      const hyvaksyntaLahikouluttaja = hyvaksynnatHelper.mapHyvaksyntaLahikouluttaja(
        this,
        this.loppukeskustelu?.lahikouluttaja
      )
      const hyvaksyntaLahiesimies = hyvaksynnatHelper.mapHyvaksyntaLahiesimies(
        this,
        this.loppukeskustelu?.lahiesimies
      )

      return [hyvaksyntaLahikouluttaja, hyvaksyntaLahiesimies, hyvaksyntaErikoistuva].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    async onReturnToSender(korjausehdotus: string) {
      const form = {
        ...this.loppukeskustelu,
        korjausehdotus: korjausehdotus,
        lahetetty: false
      }

      try {
        this.buttonStates.secondaryButtonLoading = true
        await store.dispatch('kouluttaja/putLoppukeskustelu', form)
        this.buttonStates.secondaryButtonLoading = false
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('loppukeskustelu-palautettu-muokattavaksi'))
      } catch {
        toastFail(this, this.$t('loppukeskustelu-palautus-epaonnistui'))
      }
    }

    async onSign() {
      if (!this.loppukeskustelu) return
      if (this.loppukeskustelu.esitetaanKoejaksonHyvaksymista === true) {
        this.loppukeskustelu.jatkotoimenpiteet = null
      }
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch('kouluttaja/putLoppukeskustelu', this.loppukeskustelu)
        this.buttonStates.primaryButtonLoading = false

        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('loppukeskustelu-lahetys-onnistui'))
      } catch {
        toastFail(this, this.$t('loppukeskustelun-tallennus-epaonnistui'))
      }
    }

    naytaKehittamistoimenpideKategoria(kategoria: string, muuKategoria: string | null) {
      if (kategoria === 'MUU') return muuKategoria
      return this.$t('kehittamistoimenpidekategoria-' + kategoria)
    }

    onValidateAndConfirm() {
      this.$v.$touch()
      if (this.$v.$anyError) {
        return
      }
      return this.$bvModal.show('confirm-send')
    }

    async mounted() {
      this.loading = true
      await store.dispatch(`${resolveRolePath()}/getKoejaksot`)

      try {
        const { data } = await (this.$isVastuuhenkilo()
          ? getLoppukeskusteluVastuuhenkilo(this.loppukeskusteluId)
          : getLoppukeskusteluKouluttaja(this.loppukeskusteluId))
        this.loppukeskustelu = data
        this.loading = false
      } catch {
        toastFail(this, this.$t('loppukeskustelun-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'koejakso' })
      }
    }
  }
</script>

<style lang="scss" scoped>
  .textarea-min-height {
    min-height: 100px;
  }
</style>
