<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container v-if="!loading && aloituskeskustelu" fluid>
      <h1 class="mb-3">{{ $t('aloituskeskustelu-kouluttaja') }}</h1>

      <div v-if="editable && isCurrentUserLahiesimies">
        <p>{{ $t('aloituskeskustelu-esimies-ingressi') }}</p>
      </div>
      <div v-else-if="editable">
        <p>{{ $t('aloituskeskustelu-kouluttaja-ingressi') }}</p>
      </div>

      <b-alert :show="showWaitingForLahiesimies" variant="dark" class="mt-3">
        <div class="d-flex flex-row">
          <em class="align-middle">
            <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
          </em>
          <div>
            {{ $t('aloituskeskustelu-kouluttaja-hyvaksytty') }}
          </div>
        </div>
      </b-alert>

      <b-alert :show="returned" variant="dark" class="mt-3">
        <div class="d-flex flex-row">
          <em class="align-middle">
            <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
          </em>
          <div>
            {{ $t('aloituskeskustelu-palautettu-erikoistuvalle-muokattavaksi') }}
            <span class="d-block">{{ $t('syy') }} {{ aloituskeskustelu.korjausehdotus }}</span>
          </div>
        </div>
      </b-alert>

      <b-alert variant="success" :show="acceptedByEveryone">
        <div class="d-flex flex-row">
          <em class="align-middle">
            <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
          </em>
          <span>{{ $t('aloituskeskustelu-tila-hyvaksytty') }}</span>
        </div>
      </b-alert>
      <hr />
      <erikoistuva-details
        :avatar="erikoistuvanAvatar"
        :name="erikoistuvanNimi"
        :erikoisala="aloituskeskustelu.erikoistuvanErikoisala"
        :opiskelijatunnus="aloituskeskustelu.erikoistuvanOpiskelijatunnus"
        :yliopisto="aloituskeskustelu.erikoistuvanYliopisto"
        :show-birthdate="false"
      />
      <hr />
      <div>
        <b-row>
          <b-col>
            <h5>{{ $t('sahkopostiosoite') }}</h5>
            <p>{{ aloituskeskustelu.erikoistuvanSahkoposti }}</p>
          </b-col>
        </b-row>
        <b-row>
          <b-col>
            <h5>{{ $t('koejakson-suorituspaikka') }}</h5>
            <p>{{ aloituskeskustelu.koejaksonSuorituspaikka }}</p>
          </b-col>
        </b-row>
        <b-row>
          <b-col lg="4">
            <h5>{{ $t('koejakson-alkamispäivä') }}</h5>
            <p>
              {{
                aloituskeskustelu.koejaksonAlkamispaiva
                  ? $date(aloituskeskustelu.koejaksonAlkamispaiva)
                  : ''
              }}
            </p>
          </b-col>
          <b-col lg="4">
            <h5>{{ $t('koejakson-päättymispäivä') }}</h5>
            <p>
              {{
                aloituskeskustelu.koejaksonPaattymispaiva
                  ? $date(aloituskeskustelu.koejaksonPaattymispaiva)
                  : ''
              }}
            </p>
          </b-col>
        </b-row>
        <b-row>
          <b-col lg="8">
            <h5>{{ $t('koejakso-suoritettu-kokoaikatyössä') }}</h5>
            <p v-if="aloituskeskustelu.suoritettuKokoaikatyossa">{{ $t('kylla') }}</p>
            <p v-else>
              {{ $t('suoritetaan-osa-aikatyossa-tuntia-viikossa', { tyotunnitViikossa }) }}
            </p>
          </b-col>
        </b-row>
        <hr />
        <koulutuspaikan-arvioijat
          :lahikouluttaja="aloituskeskustelu.lahikouluttaja"
          :lahiesimies="aloituskeskustelu.lahiesimies"
          :is-readonly="true"
        />
        <hr />
        <b-row>
          <b-col>
            <h3>{{ $t('aloituskeskustelu-tavoitteet') }}</h3>
            <h5>{{ $t('koejakso-osaamistavoitteet') }}</h5>
            <p>{{ aloituskeskustelu.koejaksonOsaamistavoitteet }}</p>
          </b-col>
        </b-row>
        <hr />
        <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
      </div>
      <hr v-if="showHyvaksynnat && editable" />
      <b-row v-if="editable">
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
            :disabled="buttonStates.primaryButtonLoading"
            :loading="buttonStates.secondaryButtonLoading"
            variant="outline-primary"
          >
            {{ $t('palauta-muokattavaksi') }}
          </elsa-button>
          <elsa-button
            v-b-modal.confirm-send
            class="my-2 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block"
            style="min-width: 14rem"
            :disabled="buttonStates.secondaryButtonLoading"
            :loading="buttonStates.primaryButtonLoading"
            variant="primary"
          >
            {{ $t('hyvaksy-laheta') }}
          </elsa-button>
        </b-col>
      </b-row>
    </b-container>

    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="
        isCurrentUserLahiesimies
          ? $t('vahvista-koejakson-vaihe-hyvaksytty', { koejaksonVaihe })
          : $t('vahvista-koejakson-vaihe-esimiehelle')
      "
      :submit-text="$t('hyvaksy-laheta')"
      @submit="onSubmit"
    />
    <elsa-return-to-sender-modal
      id="return-to-sender"
      :title="$t('palauta-erikoistuvalle-muokattavaksi')"
      @submit="returnToSender"
    />
  </div>
</template>

<script lang="ts">
  import { format } from 'date-fns'
  import Component from 'vue-class-component'
  import { Vue } from 'vue-property-decorator'

  import { getAloituskeskustelu as getAloituskeskusteluKouluttaja } from '@/api/kouluttaja'
  import { getAloituskeskustelu as getAloituskeskusteluVastuuhenkilo } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import store from '@/store'
  import {
    KoejaksonVaiheHyvaksynta,
    KoejaksonVaiheButtonStates,
    AloituskeskusteluLomake,
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
      ElsaButton,
      KoejaksonVaiheHyvaksynnat,
      ElsaConfirmationModal,
      ElsaReturnToSenderModal,
      KoulutuspaikanArvioijat
    }
  })
  export default class ArviointilomakeAloituskeskustelu extends Vue {
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
        text: this.$t('aloituskeskustelu-kouluttaja'),
        active: true
      }
    ]
    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }
    loading = true
    aloituskeskustelu: null | AloituskeskusteluLomake = null
    koejaksonVaihe = this.$t('aloituskeskustelu')

    get aloituskeskusteluId() {
      return Number(this.$route.params.id)
    }

    get aloituskeskustelunTila() {
      return store.getters[`${resolveRolePath()}/koejaksot`].find(
        (k: KoejaksonVaihe) =>
          k.id === this.aloituskeskusteluId && k.tyyppi === LomakeTyypit.ALOITUSKESKUSTELU
      )?.tila
    }

    get showHyvaksynnat() {
      return (
        this.aloituskeskustelu?.erikoistuvanKuittausaika !== null ||
        this.aloituskeskustelu?.lahikouluttaja.sopimusHyvaksytty ||
        this.aloituskeskustelu?.lahiesimies.sopimusHyvaksytty
      )
    }

    get returned() {
      return this.aloituskeskustelunTila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get isCurrentUserLahiesimies() {
      const currentUser = store.getters['auth/account']
      return this.aloituskeskustelu?.lahiesimies.kayttajaUserId === currentUser.id
    }

    get editable() {
      return (
        (this.$isKouluttaja() || this.$isVastuuhenkilo()) &&
        this.aloituskeskustelunTila !== LomakeTilat.PALAUTETTU_KORJATTAVAKSI &&
        ((this.isCurrentUserLahiesimies &&
          !this.aloituskeskustelu?.lahiesimies.sopimusHyvaksytty) ||
          !this.aloituskeskustelu?.lahikouluttaja.sopimusHyvaksytty)
      )
    }

    get acceptedByEveryone() {
      return (
        this.aloituskeskustelunTila !== LomakeTilat.PALAUTETTU_KORJATTAVAKSI &&
        this.aloituskeskustelu?.lahikouluttaja.sopimusHyvaksytty &&
        this.aloituskeskustelu?.lahiesimies.sopimusHyvaksytty
      )
    }

    get tyotunnitViikossa() {
      return this.aloituskeskustelu?.tyotunnitViikossa?.toString().replace('.', ',')
    }

    get showWaitingForLahiesimies() {
      return (
        !this.isCurrentUserLahiesimies &&
        this.aloituskeskustelunTila !== LomakeTilat.PALAUTETTU_KORJATTAVAKSI &&
        this.aloituskeskustelu?.lahikouluttaja.sopimusHyvaksytty &&
        !this.aloituskeskustelu?.lahiesimies.sopimusHyvaksytty
      )
    }

    get erikoistuvanAvatar() {
      return this.aloituskeskustelu?.erikoistuvanAvatar
    }

    get erikoistuvanNimi() {
      return this.aloituskeskustelu?.erikoistuvanNimi
    }

    get hyvaksynnat() {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.aloituskeskustelu?.erikoistuvanNimi,
        this.aloituskeskustelu?.erikoistuvanKuittausaika
      ) as KoejaksonVaiheHyvaksynta
      const hyvaksyntaLahikouluttaja = hyvaksynnatHelper.mapHyvaksyntaLahikouluttaja(
        this,
        this.aloituskeskustelu?.lahikouluttaja
      )
      const hyvaksyntaLahiesimies = hyvaksynnatHelper.mapHyvaksyntaLahiesimies(
        this,
        this.aloituskeskustelu?.lahiesimies
      )

      return [hyvaksyntaErikoistuva, hyvaksyntaLahikouluttaja, hyvaksyntaLahiesimies].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }

    async returnToSender(korjausehdotus: string) {
      const form = {
        ...this.aloituskeskustelu,
        korjausehdotus: korjausehdotus,
        lahetetty: false
      }
      try {
        this.buttonStates.secondaryButtonLoading = true
        await store.dispatch('kouluttaja/putAloituskeskustelu', form)
        this.buttonStates.secondaryButtonLoading = false
        this.$emit('skipRouteExitConfirm', true)
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('aloituskeskustelu-palautettu-erikoistuvalle-muokattavaksi'))
      } catch {
        toastFail(this, this.$t('aloituskeskustelu-palautus-epaonnistui'))
      }
    }

    async onSubmit() {
      const form = this.isCurrentUserLahiesimies
        ? {
            ...this.aloituskeskustelu,
            lahiesimies: {
              sopimusHyvaksytty: true,
              kuittausaika: format(new Date(), 'yyyy-MM-dd')
            }
          }
        : {
            ...this.aloituskeskustelu,
            lahikouluttaja: {
              sopimusHyvaksytty: true,
              kuittausaika: format(new Date(), 'yyyy-MM-dd')
            }
          }
      try {
        this.buttonStates.primaryButtonLoading = true
        await store.dispatch('kouluttaja/putAloituskeskustelu', form)
        this.buttonStates.primaryButtonLoading = false
        this.$emit('skipRouteExitConfirm', true)
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('aloituskeskustelu-lisatty-onnistuneesti'))
      } catch {
        toastFail(this, this.$t('aloituskeskustelu-lisaaminen-epaonnistui'))
      }
    }

    async mounted() {
      this.loading = true
      await store.dispatch(`${resolveRolePath()}/getKoejaksot`)

      try {
        const { data } = await (this.$isVastuuhenkilo()
          ? getAloituskeskusteluVastuuhenkilo(this.aloituskeskusteluId)
          : getAloituskeskusteluKouluttaja(this.aloituskeskusteluId))
        this.aloituskeskustelu = data
        this.loading = false

        if (!this.editable || this.returned) {
          this.$emit('skipRouteExitConfirm', true)
        }
      } catch {
        toastFail(this, this.$t('aloituskeskustelun-hakeminen-epaonnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.replace({ name: 'koejakso' })
      }
    }
  }
</script>
