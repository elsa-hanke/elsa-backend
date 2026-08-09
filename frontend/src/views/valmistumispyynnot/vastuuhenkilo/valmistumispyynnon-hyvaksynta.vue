<template>
  <div class="valmistumispyynto">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('valmistumispyynnon-hyvaksynta') }}</h1>
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
                      {{ $t('valmistumispyynto-hyvaksynta-virkailija-tarkastanut') }}
                    </span>
                    <span v-if="valmistumispyynto.virkailijanSaate" class="d-block">
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
                          'valmistumispyynto-osaaminen-arvioitu-palautettu-erikoistujalle-hyvaksyjan-toimesta'
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
                  name: 'valmistumispyynnot'
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
              ></erikoistuva-details>
            </div>
            <hr />
            <h2 class="mb-3">{{ $t('osaamisen-arviointi') }}</h2>
            <div>
              <h5>
                {{ $t('erikoistujan-osaaminen-riittavalla-tasolla-valmistumiseen') }}
              </h5>
              <p>
                {{ $t('kylla') }}
              </p>
            </div>
            <div v-if="valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika" class="mt-3">
              <h2 class="mb-3">{{ $t('erikoisalan-vastuuhenkilo') }}</h2>
              <b-row>
                <b-col class="hyvaksynta-pvm col-xxl-1" lg="2">
                  <h5>{{ $t('paivays') }}</h5>
                  <p>
                    {{ $date(valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika) }}
                  </p>
                </b-col>
                <b-col>
                  <h5>{{ $t('vastuuhenkilon-nimi-ja-nimike') }}</h5>
                  <p>
                    {{ valmistumispyynto.vastuuhenkiloOsaamisenArvioijaNimi }}
                  </p>
                </b-col>
              </b-row>
            </div>
            <b-form @submit.stop.prevent="onSubmit">
              <div v-if="odottaaHyvaksyntaa">
                <b-row>
                  <b-col lg="4">
                    <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
                      <template #default="{ uid }">
                        <b-form-input
                          :id="uid"
                          v-model="form.sahkoposti"
                          :state="validateState('sahkoposti')"
                          @input="onSkipRouteExitConfirm"
                        />
                        <b-form-invalid-feedback
                          v-if="$v.form.sahkoposti && !$v.form.sahkoposti.required"
                          :id="`${uid}-feedback`"
                        >
                          {{ $t('pakollinen-tieto') }}
                        </b-form-invalid-feedback>
                        <b-form-invalid-feedback
                          v-if="$v.form.sahkoposti && !$v.form.sahkoposti.email"
                          :id="`${uid}-feedback`"
                          :state="validateState('sahkoposti')"
                        >
                          {{ $t('sahkopostiosoite-ei-kelvollinen') }}
                        </b-form-invalid-feedback>
                      </template>
                    </elsa-form-group>
                  </b-col>
                  <b-col lg="4">
                    <elsa-form-group :label="$t('matkapuhelinnumero')" :required="true">
                      <template #default="{ uid }">
                        <b-form-input
                          :id="uid"
                          v-model="form.puhelinnumero"
                          :state="validateState('puhelinnumero')"
                          @input="onSkipRouteExitConfirm"
                        />
                        <small class="form-text text-muted">
                          {{ $t('syota-puhelinnumero-muodossa') }}
                        </small>
                        <b-form-invalid-feedback :id="`${uid}-feedback`">
                          {{ $t('tarkista-puhelinnumeron-muoto') }}
                        </b-form-invalid-feedback>
                      </template>
                    </elsa-form-group>
                  </b-col>
                </b-row>
              </div>
              <hr />
              <h2 class="mb-3">{{ $t('maaralliset-tarkistukset') }}</h2>
              <elsa-button
                variant="outline-primary"
                class="mt-2 mb-4"
                @click="vaihdaRooli(valmistumispyynto.opintooikeusId)"
              >
                {{ $t('nayta-erikoistujan-suoritustiedot') }}
              </elsa-button>
              <h5>{{ $t('muut-koulutukset-ja-tutkinnot') }}</h5>
              <div class="mb-3">
                <p v-if="virkailijanTarkistus.yekSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('yek-suoritettu') }}
                  {{ $date(virkailijanTarkistus.yekSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('yek-ei-suoritettu') }}
                </p>
              </div>
              <div class="mb-3">
                <p v-if="virkailijanTarkistus.ptlSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('ptl-suoritettu') }}
                  {{ $date(virkailijanTarkistus.ptlSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('ptl-ei-suoritettu') }}
                </p>
              </div>
              <div class="mb-3">
                <p v-if="virkailijanTarkistus.aiempiElKoulutusSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('aiempi-el-koulutus-suoritettu') }}
                  {{ $date(virkailijanTarkistus.aiempiElKoulutusSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('aiempi-el-koulutus-ei-suoritettu') }}
                </p>
              </div>
              <div class="mb-3">
                <p v-if="virkailijanTarkistus.ltTutkintoSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('lt-tutkinto-suoritettu') }}
                  {{ $date(virkailijanTarkistus.ltTutkintoSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('lt-tutkinto-ei-suoritettu') }}
                </p>
              </div>
              <hr />
              <h2 class="mb-3">{{ $t('tyoskentelyjaksot') }}</h2>
              <b-alert variant="dark" :show="virkailijanTarkistus.tutkimustyotaTehty">
                <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                <span>
                  {{ $t('valmistumispyynto-virkailija-tyoskentelyjaksot-huomio') }}
                </span>
              </b-alert>
              <div class="my-3">
                <h5>
                  {{ $t('terveyskeskustyo') }}
                  {{
                    $duration(
                      virkailijanTarkistus.tyoskentelyjaksotTilastot.terveyskeskusVaadittuVahintaan
                    )
                  }}
                </h5>
                <p class="mb-1">
                  {{ $t('suoritettu') }}
                  {{
                    $duration(
                      virkailijanTarkistus.tyoskentelyjaksotTilastot.terveyskeskusSuoritettu
                    )
                  }}
                </p>
                <span>
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('kesto-ja-todistukset') }} {{ $t('tarkistettu') }}
                </span>
              </div>
              <div
                v-if="virkailijanTarkistus.yliopistosairaalanUlkopuolinenTyoTarkistettu"
                class="my-3"
              >
                <h5>
                  {{ $t('yliopistosairaalan-ulkopuolinen-tyo') }}
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
              <div v-if="virkailijanTarkistus.yliopistosairaalatyoTarkistettu" class="my-3">
                <h5>
                  {{ $t('yliopistosairaalatyo') }}
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
              <h2 class="mb-3">{{ $t('koulutukset') }}</h2>
              <div v-if="virkailijanTarkistus.teoriakoulutusTarkistettu" class="my-3">
                <h5>
                  {{ $t('teoriakoulutus') }}
                </h5>
                <p class="mb-1">
                  {{ $t('suoritettu') }}
                  {{ Math.round(virkailijanTarkistus.teoriakoulutusSuoritettu) }} /
                  {{ Math.round(virkailijanTarkistus.teoriakoulutusVaadittu) }}
                  {{ $t('tuntia-lyhenne') }}
                </p>
                <p class="mb-1">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('todistukset-tarkistettu') }}
                </p>
              </div>
              <div v-if="virkailijanTarkistus.sateilusuojakoulutusVaadittu > 0" class="my-3">
                <h5>
                  {{ $t('sateilysuojelukoulutus') }}
                </h5>
                <p class="mb-1">
                  <font-awesome-icon
                    v-if="
                      virkailijanTarkistus.sateilusuojakoulutusSuoritettu >=
                      virkailijanTarkistus.sateilusuojakoulutusVaadittu
                    "
                    :icon="['fas', 'check-circle']"
                    class="text-success mr-2"
                  />
                  <font-awesome-icon
                    v-else
                    :icon="['fas', 'exclamation-circle']"
                    class="text-error mr-2"
                  />
                  {{ $t('suoritettu') }}
                  {{ Math.round(virkailijanTarkistus.sateilusuojakoulutusSuoritettu) }} /
                  {{ Math.round(virkailijanTarkistus.sateilusuojakoulutusVaadittu) }}
                  {{ $t('opintopistetta-lyhenne') }}
                </p>
              </div>
              <div class="my-3">
                <h5>
                  {{ $t('johtamiskoulutus') }}
                </h5>
                <p class="mb-1">
                  <font-awesome-icon
                    v-if="
                      virkailijanTarkistus.johtamiskoulutusSuoritettu >=
                      virkailijanTarkistus.johtamiskoulutusVaadittu
                    "
                    :icon="['fas', 'check-circle']"
                    class="text-success mr-2"
                  />
                  <font-awesome-icon
                    v-else
                    :icon="['fas', 'exclamation-circle']"
                    class="text-error mr-2"
                  />
                  {{ $t('suoritettu') }}
                  {{ Math.round(virkailijanTarkistus.johtamiskoulutusSuoritettu) }} /
                  {{ Math.round(virkailijanTarkistus.johtamiskoulutusVaadittu) }}
                  {{ $t('opintopistetta-lyhenne') }}
                </p>
              </div>
              <hr />
              <h2 class="mb-3">{{ $t('muut-tarkistukset') }}</h2>
              <opintosuoritus-tab
                class="mx-2"
                variant="kuulustelu"
                :suoritukset="virkailijanTarkistus.kuulustelut"
              />
              <div class="my-3">
                <h5>
                  {{ $t('koejakso') }}
                </h5>
                <p v-if="virkailijanTarkistus.koejaksoHyvaksyttyPvm" class="mb-1">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('hyvaksytty') }}
                  {{ $date(virkailijanTarkistus.koejaksoHyvaksyttyPvm) }}
                </p>
                <p v-if="!virkailijanTarkistus.koejaksoHyvaksyttyPvm" class="mb-1">
                  {{ $t('koejakso-ei-hyvaksytty') }}
                </p>
                <p
                  v-if="
                    !virkailijanTarkistus.koejaksoHyvaksyttyPvm &&
                    virkailijanTarkistus.koejaksoEiVaadittu
                  "
                  class="mb-1"
                >
                  {{ $t('koejaksoa-ei-vaadita') }}
                </p>
              </div>
              <div class="my-3">
                <h5>
                  {{ $t('vanhat-suoritukset') }}
                </h5>
                <p v-if="valmistumispyynto.selvitysVanhentuneistaSuorituksista" class="mb-1">
                  {{ $t('erikoistuvalla-on-vanhoja-yli-10v-suorituksia') }}
                </p>
                <p v-if="!valmistumispyynto.selvitysVanhentuneistaSuorituksista" class="mb-1">
                  {{ $t('erikoistuvalla-ei-vanhoja-yli-10v-suorituksia') }}
                </p>
              </div>
              <div v-if="valmistumispyynto.selvitysVanhentuneistaSuorituksista" class="my-3">
                <h5>
                  {{ $t('selvitys-vanhentuneista-suorituksista-virkailija') }}
                </h5>
                <p class="mb-1">
                  {{ valmistumispyynto.selvitysVanhentuneistaSuorituksista }}
                </p>
              </div>
              <div v-show="virkailijanTarkistus.virkailijanYhteenveto">
                <h5 class="mt-3 mb-2">{{ $t('virkailijan-valmistumisen-yhteenveto') }}</h5>
                <div v-safe-html="virkailijanYhteenvetoSanitized"></div>
              </div>
              <div v-if="valmistumispyynto.virkailijanKuittausaika">
                <hr />
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
              </div>
              <div v-if="valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika">
                <hr />
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
              </div>
              <div v-if="yhteenvetoAsiakirjaUrl || liitteetAsiakirjaUrl">
                <hr />
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
                      :asiakirja-label="$t('erikoistumiskoulutuksen-valmistumisen-yhteenveto')"
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
              <elsa-form-group
                v-if="vastuuhenkiloOsaamisenArvioijaPalauttanut"
                class="mt-3"
                :label="$t('lisatiedot-erikoistujalle')"
              >
                <span>{{ valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus }}</span>
              </elsa-form-group>
              <div v-if="odottaaHyvaksyntaa" class="text-right">
                <elsa-button
                  variant="back"
                  :to="{
                    name: 'valmistumispyynnot'
                  }"
                >
                  {{ $t('peruuta') }}
                </elsa-button>
                <elsa-button v-b-modal.return-to-sender variant="outline-primary" class="ml-6">
                  {{ $t('palauta-erikoistujalle') }}
                </elsa-button>
                <elsa-button :loading="sending" variant="primary" type="submit" class="ml-2">
                  {{ $t('hyvaksy') }}
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
      :title="$t('palauta-erikoistuvalle-muokattavaksi')"
      @submit="onReturnToSender"
    />
    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="
        valmistumispyynto.arkistoitava
          ? $t('valmistumispyynto-hyvaksynta-arkistointi')
          : $t('valmistumispyynto-hyvaksynta-vahvistus')
      "
      :submit-text="$t('hyvaksy')"
      @submit="onSend"
    />
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import DOMPurify from 'dompurify'
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
      OpintosuoritusTab
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
        text: this.$t('valmistumispyynnon-hyvaksynta'),
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

    get virkailijanYhteenvetoSanitized(): string {
      const raw = this.virkailijanTarkistus?.virkailijanYhteenveto ?? ''
      return DOMPurify.sanitize(raw)
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

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      if (!this.validateForm()) return
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
          this.$router.replace({ name: 'valmistumispyynnot' })
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
          this.$router.replace({ name: 'valmistumispyynnot' })
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
  }
</script>

<style lang="scss" scoped>
  .valmistumispyynto {
    max-width: 970px;
  }

  .hyvaksynta-pvm {
    min-width: 7rem;
  }
</style>
