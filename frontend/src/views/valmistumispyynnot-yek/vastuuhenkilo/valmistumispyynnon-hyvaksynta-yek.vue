<template>
  <div class="valmistumispyynto">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('yek.valmistumispyynnon-hyvaksynta') }}</h1>
          <div v-if="!loading && virkailijanTarkistus">
            <div class="mt-3">
              <b-alert :show="odottaaHyvaksyntaa" variant="dark">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon
                      :icon="['fas', 'info-circle']"
                      class="text-muted text-size-md mr-2"
                    />
                  </em>
                  <div>
                    <span>
                      {{ $t('yek.valmistumispyynto-hyvaksynta-virkailija-tarkastanut') }}
                    </span>
                    <span v-if="valmistumispyynto.virkailijanSaate" class="mt-2 d-block">
                      <strong>{{ $t('lisatiedot-virkailijalta') }}: &nbsp;</strong>
                      <span>
                        {{ valmistumispyynto.virkailijanSaate }}
                      </span>
                    </span>
                  </div>
                </div>
              </b-alert>
              <b-alert :show="hyvaksytty" variant="success">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                  </em>
                  <div>
                    <span>
                      {{ $t('valmistumispyynto-hyvaksytty-vastuuhenkilon-toimesta') }}
                    </span>
                  </div>
                </div>
              </b-alert>
              <b-alert :show="vastuuhenkiloHyvaksyjaPalauttanut" variant="dark">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon
                      :icon="['fas', 'info-circle']"
                      class="text-muted text-size-md mr-2"
                    />
                  </em>
                  <div>
                    <span>
                      {{
                        $t(
                          'yek.valmistumispyynto-palautettu-koulutettavalle-vastuuhenkilon-toimesta'
                        )
                      }}
                    </span>
                    <span class="d-block">
                      {{ $t('syy') }}&nbsp;
                      <span>
                        {{ korjausehdotus }}
                      </span>
                    </span>
                  </div>
                </div>
              </b-alert>
              <elsa-button
                variant="primary"
                class="mt-2 mb-1"
                :to="{
                  name: 'valmistumispyynnot',
                  hash: '#yek'
                }"
              >
                {{ $t('palaa-valmistumispyyntoihin') }}
              </elsa-button>
            </div>
            <hr />
            <div>
              <erikoistuva-details
                :avatar="valmistumispyynto.erikoistujanAvatar"
                :name="valmistumispyynto.erikoistujanNimi"
                :erikoisala="valmistumispyynto.erikoistujanErikoisala"
                :opiskelijatunnus="valmistumispyynto.erikoistujanOpiskelijatunnus"
                :syntymaaika="valmistumispyynto.erikoistujanSyntymaaika"
                :yliopisto="valmistumispyynto.erikoistujanYliopisto"
                :laillistamispaiva="valmistumispyynto.erikoistujanLaillistamispaiva"
                :laillistamistodistus="valmistumispyynto.erikoistujanLaillistamistodistus"
                :laillistamistodistus-nimi="valmistumispyynto.erikoistujanLaillistamistodistusNimi"
                :laillistamistodistus-tyyppi="
                  valmistumispyynto.erikoistujanLaillistamistodistusTyyppi
                "
                :opintooikeuden-myontamispaiva="valmistumispyynto.opintooikeudenMyontamispaiva"
                :asetus="valmistumispyynto.erikoistujanAsetus"
                :yek="true"
              ></erikoistuva-details>
            </div>
            <hr />
            <h2 class="mb-3">{{ $t('yek.koulutettavan-suoritustiedot') }}</h2>
            <elsa-button
              variant="outline-primary"
              class="mt-2"
              @click="vaihdaRooli(valmistumispyynto.opintooikeusId)"
            >
              {{ $t('yek.nayta-koulutettavan-suoritustiedot') }}
            </elsa-button>
            <hr />
            <h2 class="mb-3">{{ $t('teoriakoulutus') }}</h2>
            <h5>
              {{ $t('teoriakoulutus') }}
            </h5>
            <p v-if="teoriakoulutusSuoritettu" class="mb-1">
              <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
              {{ $t('suoritettu') }}
            </p>
            <p v-else class="mb-1">
              <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="text-error mr-2" />
              {{ $t('ei-suoritettu') }}
            </p>
            <hr />
            <h2 class="mb-3">{{ $t('tyoskentelyjaksot') }}</h2>
            <b-alert variant="dark" :show="virkailijanTarkistus.tutkimustyotaTehty">
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span>
                {{ $t('valmistumispyynto-virkailija-tyoskentelyjaksot-huomio') }}
              </span>
            </b-alert>
            <div class="mt-3">
              <h5>
                {{ terveyskeskustyoLabel }}
              </h5>
              <p class="mb-1">
                {{ $t('suoritettu') }}
                {{
                  $duration(virkailijanTarkistus.tyoskentelyjaksotTilastot.terveyskeskusSuoritettu)
                }}
              </p>
              <span>
                <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                {{ $t('kesto-ja-todistukset') }} {{ $t('tarkistettu') }}
              </span>
            </div>
            <elsa-button
              v-if="
                virkailijanTarkistus.tyoskentelyjaksot &&
                virkailijanTarkistus.tyoskentelyjaksot.terveyskeskus.length > 0
              "
              variant="link"
              class="pl-0"
              @click="showTerveystyo = !showTerveystyo"
            >
              {{ $t('nayta-tyoskentelyjaksot') }}
              <font-awesome-icon
                :icon="showTerveystyo ? 'chevron-up' : 'chevron-down'"
                fixed-width
                size="lg"
                class="ml-2 text-dark"
              />
            </elsa-button>
            <div v-if="showTerveystyo && virkailijanTarkistus.tyoskentelyjaksot">
              <hr />
              <yek-tyoskentelyjaksot-list
                :tyoskentelyjaksot="virkailijanTarkistus.tyoskentelyjaksot.terveyskeskus"
                :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
                class="ml-3"
              />
            </div>
            <div v-if="virkailijanTarkistus.yliopistosairaalatyoTarkistettu" class="mt-3">
              <h5>
                {{ sairaalatyoLabel }}
              </h5>
              <p class="mb-1">
                {{ $t('suoritettu') }}
                {{
                  $duration(
                    virkailijanTarkistus.tyoskentelyjaksotTilastot.yliopistosairaalaSuoritettu
                  )
                }}
              </p>
              <p class="mb-1">
                <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                {{ $t('kesto-poissaolot-ja-vanheneminen-tarkistettu-tyotodistuksesta') }}
              </p>
            </div>
            <elsa-button
              v-if="
                virkailijanTarkistus.tyoskentelyjaksot &&
                virkailijanTarkistus.tyoskentelyjaksot.yliopistosairaala.length > 0
              "
              variant="link"
              class="pl-0"
              @click="showSairaalatyo = !showSairaalatyo"
            >
              {{ $t('nayta-tyoskentelyjaksot') }}
              <font-awesome-icon
                :icon="showSairaalatyo ? 'chevron-up' : 'chevron-down'"
                fixed-width
                size="lg"
                class="ml-2 text-dark"
              />
            </elsa-button>
            <div v-if="showSairaalatyo && virkailijanTarkistus.tyoskentelyjaksot">
              <hr />
              <yek-tyoskentelyjaksot-list
                :tyoskentelyjaksot="virkailijanTarkistus.tyoskentelyjaksot.yliopistosairaala"
                :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
                class="ml-3"
              />
            </div>
            <div
              v-if="virkailijanTarkistus.yliopistosairaalanUlkopuolinenTyoTarkistettu"
              class="my-3"
            >
              <h5>
                {{ $t('muut') }}
              </h5>
              <p class="mb-1">
                {{ $t('suoritettu') }}
                {{
                  $duration(
                    virkailijanTarkistus.tyoskentelyjaksotTilastot
                      .yliopistosairaaloidenUlkopuolinenSuoritettu
                  )
                }}
              </p>
              <p class="mb-1">
                <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                {{ $t('kesto-poissaolot-ja-vanheneminen-tarkistettu-tyotodistuksesta') }}
              </p>
            </div>
            <elsa-button
              v-if="
                virkailijanTarkistus.tyoskentelyjaksot &&
                virkailijanTarkistus.tyoskentelyjaksot.yliopistosairaaloidenUlkopuolinen.length > 0
              "
              variant="link"
              class="pl-0"
              @click="showMuu = !showMuu"
            >
              {{ $t('nayta-tyoskentelyjaksot') }}
              <font-awesome-icon
                :icon="showMuu ? 'chevron-up' : 'chevron-down'"
                fixed-width
                size="lg"
                class="ml-2 text-dark"
              />
            </elsa-button>
            <div v-if="showMuu && virkailijanTarkistus.tyoskentelyjaksot">
              <hr />
              <yek-tyoskentelyjaksot-list
                :tyoskentelyjaksot="
                  virkailijanTarkistus.tyoskentelyjaksot.yliopistosairaaloidenUlkopuolinen
                "
                :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
                class="ml-3"
              />
            </div>
            <div v-if="virkailijanTarkistus.kokonaistyoaikaTarkistettu" class="my-3">
              <h5>
                {{ $t('kokonaistyoaika') }}
              </h5>
              <p class="mb-1">
                {{ $t('suoritettu') }}
                {{ $duration(virkailijanTarkistus.tyoskentelyjaksotTilastot.yhteensaSuoritettu) }}
              </p>
              <p class="mb-1">
                <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                {{ $t('kesto-tarkistettu-tyotodistuksista') }}
              </p>
            </div>
            <hr />
            <h2 class="mb-3">{{ $t('muut-tarkistukset') }}</h2>
            <div class="my-3">
              <h5>
                {{ $t('vanhat-suoritukset') }}
              </h5>
              <p v-if="valmistumispyynto.selvitysVanhentuneistaSuorituksista" class="mb-1">
                {{ $t('yek.koulutettavalla-on-vanhoja-yli-10v-suorituksia') }}
              </p>
              <p v-if="!valmistumispyynto.selvitysVanhentuneistaSuorituksista" class="mb-1">
                {{ $t('yek.koulutettavalla-ei-vanhoja-yli-10v-suorituksia') }}
              </p>
            </div>
            <div v-if="valmistumispyynto.selvitysVanhentuneistaSuorituksista" class="my-3">
              <h5>
                {{ $t('yek.selvitys-vanhentuneista-suorituksista-virkailija') }}
              </h5>
              <p class="mb-1">
                {{ valmistumispyynto.selvitysVanhentuneistaSuorituksista }}
              </p>
            </div>
            <hr />
            <div v-if="valmistumispyynto.virkailijanKuittausaika">
              <b-row>
                <b-col>
                  <h3>{{ $t('tarkistanut') }}</h3>
                </b-col>
              </b-row>
              <b-row>
                <b-col lg="4">
                  <h5>{{ $t('paivays') }}</h5>
                  <p>{{ $date(valmistumispyynto.virkailijanKuittausaika) }}</p>
                </b-col>
                <b-col lg="4">
                  <h5>{{ $t('nimi-ja-nimike') }}</h5>
                  <p>
                    {{ valmistumispyynto.virkailijaNimi }}
                  </p>
                </b-col>
              </b-row>
              <hr />
            </div>
            <div v-if="valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika">
              <b-row>
                <b-col>
                  <h3>{{ $t('hyvaksynyt') }}</h3>
                </b-col>
              </b-row>
              <b-row>
                <b-col lg="4">
                  <h5>{{ $t('paivays') }}</h5>
                  <p>{{ $date(valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika) }}</p>
                </b-col>
                <b-col lg="4">
                  <h5>{{ $t('nimi-ja-nimike') }}</h5>
                  <p>
                    {{ valmistumispyynto.vastuuhenkiloHyvaksyjaNimi
                    }}{{
                      valmistumispyynto.vastuuhenkiloHyvaksyjaNimike
                        ? ', ' + valmistumispyynto.vastuuhenkiloHyvaksyjaNimike
                        : ''
                    }}
                  </p>
                </b-col>
              </b-row>
              <hr />
            </div>
            <div v-if="yhteenvetoAsiakirjaUrl || liitteetAsiakirjaUrl">
              <b-row>
                <b-col>
                  <h3>{{ $t('dokumentit') }}</h3>
                </b-col>
              </b-row>
              <b-row>
                <b-col>
                  <asiakirja-button
                    v-if="yhteenvetoAsiakirjaUrl"
                    :id="valmistumispyynto.yhteenvetoAsiakirjaId"
                    :asiakirja-data-endpoint-url="yhteenvetoAsiakirjaUrl"
                    :asiakirja-label="$t('yek.valmistumisen-yhteenveto')"
                  />
                  <asiakirja-button
                    v-if="liitteetAsiakirjaUrl"
                    :id="valmistumispyynto.liitteetAsiakirjaId"
                    :asiakirja-data-endpoint-url="liitteetAsiakirjaUrl"
                    :asiakirja-label="$t('valmistumispyynnon-liitteet')"
                  />
                </b-col>
              </b-row>
              <hr />
            </div>
            <b-form @submit.stop.prevent="onSubmit">
              <div v-if="odottaaHyvaksyntaa" class="text-right">
                <elsa-button
                  variant="back"
                  :to="{
                    name: 'valmistumispyynnot',
                    hash: '#yek'
                  }"
                >
                  {{ $t('peruuta') }}
                </elsa-button>
                <elsa-button v-b-modal.return-to-sender variant="outline-primary" class="ml-6">
                  {{ $t('yek.palauta-koulutettavalle') }}
                </elsa-button>
                <elsa-button :loading="sending" variant="primary" type="submit" class="ml-2">
                  {{ $t('hyvaksy-valmistumispyynto') }}
                </elsa-button>
              </div>
            </b-form>
          </div>
          <div v-else class="text-center mt-3">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
    <elsa-return-to-sender-modal
      id="return-to-sender"
      :title="$t('yek.palauta-koulutettavalle')"
      @submit="onReturnToSender"
    />
    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('yek.valmistumispyynto-hyvaksynta-vahvistus')"
      :submit-text="$t('hyvaksy-valmistumispyynto')"
      @submit="onSend"
    />
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required, email } from 'vuelidate/lib/validators'

  import { ELSA_API_LOCATION } from '@/api'
  import {
    getValmistumispyyntoHyvaksynta,
    putValmistumispyyntoHyvaksynta
  } from '@/api/vastuuhenkilo'
  import AsiakirjaButton from '@/components/asiakirjat/asiakirja-button.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import YekTyoskentelyjaksotList from '@/components/tyoskentelyjaksot-list/yek-tyoskentelyjaksot-list.vue'
  import ValmistumispyyntoMixin from '@/mixins/valmistumispyynto'
  import store from '@/store'
  import {
    ValmistumispyyntoArviointienTila,
    ValmistumispyyntoVirkailijanTarkistus,
    ValmistumispyyntoHyvaksynta,
    ElsaError
  } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { phoneNumber } from '@/utils/constants'
  import { toastSuccess, toastFail } from '@/utils/toast'
  import OpintosuoritusTab from '@/views/opintosuoritukset/opintosuoritus-tab.vue'

  @Component({
    components: {
      AsiakirjaButton,
      ElsaButton,
      ElsaFormGroup,
      ErikoistuvaDetails,
      ElsaConfirmationModal,
      ElsaReturnToSenderModal,
      OpintosuoritusTab,
      YekTyoskentelyjaksotList
    }
  })
  export default class ValmistumispyynnonHyvaksynta extends Mixins<ValmistumispyyntoMixin>(
    validationMixin,
    ValmistumispyyntoMixin
  ) {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('valmistumispyynnot'),
        to: { name: 'valmistumispyynnot' }
      },
      {
        text: this.$t('yek.valmistumispyynnon-hyvaksynta'),
        active: true
      }
    ]

    form: ValmistumispyyntoHyvaksynta = {
      id: null,
      korjausehdotus: null,
      sahkoposti: null,
      puhelinnumero: null
    }

    virkailijanTarkistus: ValmistumispyyntoVirkailijanTarkistus | null = null

    valmistumispyyntoArviointienTila: ValmistumispyyntoArviointienTila | null = null
    loading = true
    sending = false
    yhteenvetoAsiakirjaUrl: string | null = null
    liitteetAsiakirjaUrl: string | null = null
    skipRouteExitConfirm = true
    showTerveystyo = false
    showSairaalatyo = false
    showMuu = false

    validations() {
      return {
        form: {
          sahkoposti: {
            required,
            email
          },
          puhelinnumero: {
            required,
            phoneNumber
          }
        }
      }
    }

    async mounted() {
      const valmistumispyyntoId = this.$route?.params?.valmistumispyyntoId
      if (valmistumispyyntoId) {
        try {
          await getValmistumispyyntoHyvaksynta(parseInt(valmistumispyyntoId)).then((response) => {
            this.virkailijanTarkistus = response.data
            if (response.data.valmistumispyynto) {
              this.valmistumispyynto = response.data.valmistumispyynto
              if (this.valmistumispyynto.yhteenvetoAsiakirjaId) {
                this.yhteenvetoAsiakirjaUrl = `/vastuuhenkilo/valmistumispyynto/${this.valmistumispyynto.id}/asiakirja/`
              }
              if (this.valmistumispyynto.liitteetAsiakirjaId) {
                this.liitteetAsiakirjaUrl = `/vastuuhenkilo/valmistumispyynto/${this.valmistumispyynto.id}/asiakirja/`
              }
            }
            this.form.sahkoposti = this.account.email
            this.form.puhelinnumero = this.account.phoneNumber
          })
          this.loading = false
        } catch {
          toastFail(this, this.$t('valmistumispyynnon-hakeminen-epaonnistui'))
          this.$router.replace({ name: 'valmistumispyynnot' })
        }
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? !$error : null
    }

    async vaihdaRooli(id: number | undefined) {
      if (this.odottaaHyvaksyntaa && !this.skipRouteExitConfirm) {
        if (!(await confirmExit(this))) {
          return
        }
      }

      this.$emit('skipRouteExitConfirm', true)
      window.location.href = `${ELSA_API_LOCATION}/api/login/impersonate?opintooikeusId=${id}&originalUrl=${window.location.href}`
    }

    onSubmit() {
      return this.$bvModal.show('confirm-send')
    }

    async onSend() {
      try {
        this.sending = true
        if (this.valmistumispyynto.id) {
          const puhelinnumero = this.form.puhelinnumero
          const sahkoposti = this.form.sahkoposti
          this.form = (
            await putValmistumispyyntoHyvaksynta({
              id: this.valmistumispyynto.id,
              puhelinnumero,
              sahkoposti
            })
          ).data
          const account = store.getters['auth/account']
          account.email = sahkoposti
          account.phoneNumber = puhelinnumero
          this.$emit('skipRouteExitConfirm', true)
          toastSuccess(this, this.$t('valmistumispyynto-hyvaksynta-lahetys-onnistui'))
          this.$router.replace({ name: 'valmistumispyynnot', hash: '#yek' })
        }
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('valmistumispyynto-hyvaksynta-lahetys-epaonnistui')}: ${this.$t(message)}`
            : this.$t('valmistumispyynto-hyvaksynta-lahetys-epaonnistui')
        )
      }
      this.sending = false
    }

    async onReturnToSender(korjausehdotus: string) {
      try {
        this.sending = true
        if (this.valmistumispyynto.id) {
          this.form = (
            await putValmistumispyyntoHyvaksynta({
              id: this.valmistumispyynto.id,
              korjausehdotus: korjausehdotus,
              puhelinnumero: this.form.puhelinnumero,
              sahkoposti: this.form.sahkoposti
            })
          ).data
          this.$emit('skipRouteExitConfirm', true)
          toastSuccess(this, this.$t('valmistumispyynto-hyvaksynta-lahetys-palautettu'))
          this.$router.replace({ name: 'valmistumispyynnot', hash: '#yek' })
        }
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('valmistumispyynto-hyvaksynta-palautus-epaonnistui')}: ${this.$t(message)}`
            : this.$t('valmistumispyynto-hyvaksynta-palautus-epaonnistui')
        )
      }
      this.sending = false
    }

    onSkipRouteExitConfirm() {
      this.skipRouteExitConfirm = false
      this.$emit('skipRouteExitConfirm', false)
    }

    get teoriakoulutusSuoritettu(): boolean {
      const suoritettu = Math.round(this.virkailijanTarkistus?.teoriakoulutusSuoritettu || 0)
      const vaadittu = Math.round(this.virkailijanTarkistus?.teoriakoulutusVaadittu || 0)
      return suoritettu >= vaadittu
    }

    get terveyskeskustyoLabel() {
      return `${this.$t('yek.terveyskeskustyo')} ${this.$t('yek.vah')} ${this.$duration(
        this.virkailijanTarkistus?.tyoskentelyjaksotTilastot.terveyskeskusVaadittuVahintaan || 0
      )}`
    }

    get sairaalatyoLabel() {
      return `${this.$t('yek.sairaalatyo')} ${this.$t('yek.vah')} ${this.$duration(
        this.virkailijanTarkistus?.tyoskentelyjaksotTilastot.yliopistosairaalaVaadittuVahintaan || 0
      )}`
    }

    get asiakirjaDataEndpointUrl() {
      return `vastuuhenkilo/valmistumispyynto/${this.valmistumispyynto.id}/tyoskentelyjakso-liite`
    }
  }
</script>

<style lang="scss" scoped>
  .valmistumispyynto {
    max-width: 970px;
  }
</style>
