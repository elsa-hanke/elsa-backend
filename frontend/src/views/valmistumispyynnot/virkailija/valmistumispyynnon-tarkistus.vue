<template>
  <div class="valmistumispyynto">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('valmistumispyynto') }}</h1>
          <div v-if="!loading && valmistumispyynnonTarkistus">
            <div v-if="editable">
              <p class="mt-1 mb-3">
                {{ $t('valmistumispyynto-virkailijan-ingressi') }}
              </p>
              <b-alert
                :show="
                  valmistumispyynto.virkailijanKorjausehdotus != null ||
                  valmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus != null
                "
                variant="danger"
                class="mt-3"
              >
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
                  </em>
                  <div>
                    {{ $t('valmistumispyynto-palautettu-aiemmin-korjattavaksi') }}
                    <span v-if="valmistumispyynto.virkailijanKorjausehdotus != null">
                      {{ $t('virkailijan-toimesta') }}
                    </span>
                    <span v-else>{{ $t('vastuuhenkilon-toimesta') }}</span>
                    <span class="d-block">
                      {{ $t('syy') }}&nbsp;
                      <span class="font-weight-500">
                        {{
                          valmistumispyynto.virkailijanKorjausehdotus != null
                            ? valmistumispyynto.virkailijanKorjausehdotus
                            : valmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus
                        }}
                      </span>
                    </span>
                  </div>
                </div>
              </b-alert>
            </div>
            <div v-else class="mt-3">
              <b-alert :show="!hyvaksytty" variant="dark">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon
                      :icon="['fas', 'info-circle']"
                      class="text-muted text-size-md mr-2"
                    />
                  </em>
                  <div>
                    <span v-if="odottaaHyvaksyntaa">
                      {{ $t('valmistumispyynto-tarkistettu-odottaa-vastuuhenkiloa') }}
                    </span>
                    <span
                      v-if="odottaaHyvaksyntaa && valmistumispyynto.virkailijanSaate"
                      class="d-block"
                    >
                      <strong>{{ $t('lisatiedot-vastuuhenkilolle') }}:</strong>
                      {{ valmistumispyynto.virkailijanSaate }}
                    </span>
                    <span v-else-if="virkailijaPalauttanut">
                      {{
                        $t(
                          'valmistumispyynto-osaaminen-arvioitu-palautettu-erikoistujalle-virkailijan-toimesta'
                        )
                      }}
                    </span>
                    <span v-else-if="vastuuhenkiloHyvaksyjaPalauttanut">
                      {{
                        $t(
                          'valmistumispyynto-osaaminen-arvioitu-palautettu-erikoistujalle-hyvaksyjan-toimesta'
                        )
                      }}
                    </span>
                    <span
                      v-if="virkailijaPalauttanut || vastuuhenkiloHyvaksyjaPalauttanut"
                      class="d-block"
                    >
                      {{ $t('syy') }}&nbsp;
                      <span>
                        {{ korjausehdotus }}
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
                :laillistamisen-muokkaus-sallittu="editable"
                :opintooikeuden-myontamispaiva="valmistumispyynto.opintooikeudenMyontamispaiva"
                :asetus="valmistumispyynto.erikoistujanAsetus"
                @muokkaaLaillistamista="muokkaaLaillistamista"
              />
              <div v-if="laillistaminenMuokattavissa">
                <elsa-form-group
                  class="col-xs-12 col-sm-3 pl-0"
                  :label="$t('laillistamispaiva')"
                  :required="true"
                >
                  <template #default="{ uid }">
                    <elsa-form-datepicker
                      :id="uid"
                      ref="laillistamispaiva"
                      :value.sync="form.laillistamispaiva"
                      @input="onSkipRouteExitConfirm"
                    ></elsa-form-datepicker>
                    <b-form-invalid-feedback>
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </template>
                </elsa-form-group>
                <elsa-form-group :label="$t('laillistamispaivan-liitetiedosto')" :required="true">
                  <span>
                    {{ $t('lisaa-liite-joka-todistaa-laillistamispaivan') }}
                  </span>
                  <asiakirjat-upload
                    class="mt-3"
                    :is-primary-button="false"
                    :allow-multiples-files="false"
                    :button-text="$t('lisaa-liitetiedosto')"
                    :disabled="laillistamispaivaAsiakirjat.length > 0"
                    @selectedFiles="onLaillistamistodistusFilesAdded"
                  />
                  <asiakirjat-content
                    :asiakirjat="laillistamispaivaAsiakirjat"
                    :sorting-enabled="false"
                    :pagination-enabled="false"
                    :enable-search="false"
                    :enable-delete="true"
                    :no-results-info-text="$t('ei-liitetiedostoja')"
                    :state="validateState('laillistamistodistus')"
                    @deleteAsiakirja="onDeletelaillistamistodistus"
                  />
                  <b-form-invalid-feedback :state="validateState('laillistamistodistus')">
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                </elsa-form-group>
              </div>
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
              <div v-if="yekSuorituspaivaTila" class="mb-3">
                <p v-if="form.yekSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('yek-suoritettu') }}
                  {{ $date(form.yekSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('yek-ei-suoritettu') }}
                </p>
              </div>
              <div v-if="editable && !yekSuorituspaivaTila" class="my-3">
                <b-form-checkbox v-model="form.yekSuoritettu" @input="onSkipRouteExitConfirm">
                  {{ $t('yek-suoritettu') }}
                </b-form-checkbox>
                <elsa-form-group
                  v-if="form.yekSuoritettu"
                  :label="$t('suorituspaiva')"
                  class="col-xs-6 col-sm-4 col-md-4 pr-sm-3 ml-2 mt-2"
                >
                  <template #default="{ uid }">
                    <elsa-form-datepicker
                      :id="uid"
                      v-model="form.yekSuorituspaiva"
                      @input="onSkipRouteExitConfirm"
                    ></elsa-form-datepicker>
                  </template>
                </elsa-form-group>
              </div>
              <div v-if="ptlSuorituspaivaTila" class="mb-3">
                <p v-if="form.ptlSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('ptl-suoritettu') }}
                  {{ $date(form.ptlSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('ptl-ei-suoritettu') }}
                </p>
              </div>
              <div v-if="editable && !ptlSuorituspaivaTila" class="my-3">
                <b-form-checkbox v-model="form.ptlSuoritettu" @input="onSkipRouteExitConfirm">
                  {{ $t('ptl-suoritettu') }}
                </b-form-checkbox>
                <elsa-form-group
                  v-if="form.ptlSuoritettu"
                  :label="$t('suorituspaiva')"
                  class="col-xs-6 col-sm-4 col-md-4 pr-sm-3 ml-2 mt-2"
                >
                  <template #default="{ uid }">
                    <elsa-form-datepicker
                      :id="uid"
                      v-model="form.ptlSuorituspaiva"
                      @input="onSkipRouteExitConfirm"
                    ></elsa-form-datepicker>
                  </template>
                </elsa-form-group>
              </div>
              <div v-if="aiempiElKoulutusSuorituspaivaTila" class="mb-3">
                <p v-if="form.aiempiElKoulutusSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('aiempi-el-koulutus-suoritettu') }}
                  {{ $date(form.aiempiElKoulutusSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('aiempi-el-koulutus-ei-suoritettu') }}
                </p>
              </div>
              <div v-if="editable && !aiempiElKoulutusSuorituspaivaTila" class="my-3">
                <b-form-checkbox
                  v-model="form.aiempiElKoulutusSuoritettu"
                  @input="onSkipRouteExitConfirm"
                >
                  {{ $t('aiempi-el-koulutus-suoritettu') }}
                </b-form-checkbox>
                <elsa-form-group
                  v-if="form.aiempiElKoulutusSuoritettu"
                  :label="$t('suorituspaiva')"
                  class="col-xs-6 col-sm-4 col-md-4 pr-sm-3 ml-2 mt-2"
                >
                  <template #default="{ uid }">
                    <elsa-form-datepicker
                      :id="uid"
                      v-model="form.aiempiElKoulutusSuorituspaiva"
                      @input="onSkipRouteExitConfirm"
                    ></elsa-form-datepicker>
                  </template>
                </elsa-form-group>
              </div>
              <div v-if="ltTutkintoSuorituspaivaTila" class="mb-3">
                <p v-if="form.ltTutkintoSuorituspaiva" class="mb-0">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('lt-tutkinto-suoritettu') }}
                  {{ $date(form.ltTutkintoSuorituspaiva) }}
                </p>
                <p v-else>
                  {{ $t('lt-tutkinto-ei-suoritettu') }}
                </p>
              </div>
              <div v-if="editable && !ltTutkintoSuorituspaivaTila" class="my-3">
                <b-form-checkbox
                  v-model="form.ltTutkintoSuoritettu"
                  @input="onSkipRouteExitConfirm"
                >
                  {{ $t('lt-tutkinto-suoritettu') }}
                </b-form-checkbox>
                <elsa-form-group
                  v-if="form.ltTutkintoSuoritettu"
                  :label="$t('suorituspaiva')"
                  class="col-xs-6 col-sm-4 col-md-4 pr-sm-3 ml-2 mt-2"
                >
                  <template #default="{ uid }">
                    <elsa-form-datepicker
                      :id="uid"
                      v-model="form.ltTutkintoSuorituspaiva"
                      @input="onSkipRouteExitConfirm"
                    ></elsa-form-datepicker>
                  </template>
                </elsa-form-group>
              </div>
              <hr />
              <h2 class="mb-3">{{ $t('tyoskentelyjaksot') }}</h2>
              <b-alert variant="dark" :show="valmistumispyynnonTarkistus.tutkimustyotaTehty">
                <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                <span>
                  {{ $t('valmistumispyynto-virkailija-tyoskentelyjaksot-huomio') }}
                </span>
              </b-alert>
              <elsa-form-group
                v-if="editable"
                :label="$t('terveyskeskustyo')"
                class="mt-3"
                :required="true"
              >
                <template #default="{ uid }">
                  <p class="mb-1">
                    {{ $t('suoritettu') }}
                    {{
                      $duration(
                        valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot
                          .terveyskeskusSuoritettu
                      )
                    }}
                  </p>
                  <b-form-checkbox
                    :id="uid"
                    v-model="form.terveyskeskustyoTarkistettu"
                    :state="validateState('terveyskeskustyoTarkistettu')"
                    @input="onSkipRouteExitConfirm"
                  >
                    {{ $t('kesto-ja-todistukset') }} {{ $t('tarkistettu') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
              <div v-if="!editable && form.terveyskeskustyoTarkistettu" class="my-3">
                <h5>
                  {{ $t('terveyskeskustyo') }}
                  {{
                    $duration(
                      valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot
                        .terveyskeskusVaadittuVahintaan
                    )
                  }}
                </h5>
                <p class="mb-1">
                  {{ $t('suoritettu') }}
                  {{
                    $duration(
                      valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot.terveyskeskusSuoritettu
                    )
                  }}
                </p>
                <span>
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('kesto-ja-todistukset') }} {{ $t('tarkistettu') }}
                </span>
              </div>
              <elsa-form-group
                v-if="editable"
                :label="$t('yliopistosairaalan-ulkopuolinen-tyo')"
                class="mt-3"
                :required="true"
              >
                <template #default="{ uid }">
                  <p class="mb-1">
                    {{ $t('suoritettu') }}
                    {{
                      $duration(
                        valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot
                          .yliopistosairaaloidenUlkopuolinenSuoritettu
                      )
                    }}
                  </p>
                  <b-form-checkbox
                    :id="uid"
                    v-model="form.yliopistosairaalanUlkopuolinenTyoTarkistettu"
                    :state="validateState('yliopistosairaalanUlkopuolinenTyoTarkistettu')"
                    @input="onSkipRouteExitConfirm"
                  >
                    {{ $t('kesto-poissaolot-ja-vanheneminen-tarkistettu-tyotodistuksesta') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
              <div
                v-if="!editable && form.yliopistosairaalanUlkopuolinenTyoTarkistettu"
                class="my-3"
              >
                <h5>
                  {{ $t('yliopistosairaalan-ulkopuolinen-tyo') }}
                </h5>
                <p class="mb-1">
                  {{ $t('suoritettu') }}
                  {{
                    $duration(
                      valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot
                        .yliopistosairaaloidenUlkopuolinenSuoritettu
                    )
                  }}
                </p>
                <p class="mb-1">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('kesto-poissaolot-ja-vanheneminen-tarkistettu-tyotodistuksesta') }}
                </p>
              </div>
              <elsa-form-group
                v-if="editable"
                :label="$t('yliopistosairaalatyo')"
                class="mt-3"
                :required="true"
              >
                <template #default="{ uid }">
                  <p class="mb-1">
                    {{ $t('suoritettu') }}
                    {{
                      $duration(
                        valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot
                          .yliopistosairaalaSuoritettu
                      )
                    }}
                  </p>
                  <b-form-checkbox
                    :id="uid"
                    v-model="form.yliopistosairaalatyoTarkistettu"
                    :state="validateState('yliopistosairaalatyoTarkistettu')"
                    @input="onSkipRouteExitConfirm"
                  >
                    {{ $t('kesto-poissaolot-ja-vanheneminen-tarkistettu-tyotodistuksesta') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
              <div v-if="!editable && form.yliopistosairaalatyoTarkistettu" class="my-3">
                <h5>
                  {{ $t('yliopistosairaalatyo') }}
                </h5>
                <p class="mb-1">
                  {{ $t('suoritettu') }}
                  {{
                    $duration(
                      valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot
                        .yliopistosairaalaSuoritettu
                    )
                  }}
                </p>
                <p class="mb-1">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('kesto-poissaolot-ja-vanheneminen-tarkistettu-tyotodistuksesta') }}
                </p>
              </div>
              <elsa-form-group
                v-if="editable"
                :label="$t('kokonaistyoaika')"
                class="mt-3"
                :required="true"
              >
                <template #default="{ uid }">
                  <p class="mb-1">
                    {{ $t('suoritettu') }}
                    {{
                      $duration(
                        valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot.yhteensaSuoritettu
                      )
                    }}
                  </p>
                  <b-form-checkbox
                    :id="uid"
                    v-model="form.kokonaistyoaikaTarkistettu"
                    :state="validateState('kokonaistyoaikaTarkistettu')"
                    @input="onSkipRouteExitConfirm"
                  >
                    {{ $t('kesto-tarkistettu-tyotodistuksista') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
              <div v-if="!editable && form.kokonaistyoaikaTarkistettu" class="my-3">
                <h5>
                  {{ $t('kokonaistyoaika') }}
                </h5>
                <p class="mb-1">
                  {{ $t('suoritettu') }}
                  {{
                    $duration(
                      valmistumispyynnonTarkistus.tyoskentelyjaksotTilastot.yhteensaSuoritettu
                    )
                  }}
                </p>
                <p class="mb-1">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('kesto-tarkistettu-tyotodistuksista') }}
                </p>
              </div>
              <hr />
              <h2 class="mb-3">{{ $t('koulutukset') }}</h2>
              <elsa-form-group
                v-if="editable"
                :label="$t('teoriakoulutus')"
                class="mt-3"
                :required="true"
              >
                <template #default="{ uid }">
                  <p class="mb-1">
                    {{ $t('suoritettu') }}
                    {{ Math.round(valmistumispyynnonTarkistus.teoriakoulutusSuoritettu) }} /
                    {{ Math.round(valmistumispyynnonTarkistus.teoriakoulutusVaadittu) }}
                    {{ $t('tuntia-lyhenne') }}
                  </p>
                  <b-form-checkbox
                    :id="uid"
                    v-model="form.teoriakoulutusTarkistettu"
                    :state="validateState('teoriakoulutusTarkistettu')"
                    @input="onSkipRouteExitConfirm"
                  >
                    {{ $t('todistukset-tarkistettu') }}
                  </b-form-checkbox>
                </template>
              </elsa-form-group>
              <div v-if="!editable && form.teoriakoulutusTarkistettu" class="my-3">
                <h5>
                  {{ $t('teoriakoulutus') }}
                </h5>
                <p class="mb-1">
                  {{ $t('suoritettu') }}
                  {{ Math.round(valmistumispyynnonTarkistus.teoriakoulutusSuoritettu) }} /
                  {{ Math.round(valmistumispyynnonTarkistus.teoriakoulutusVaadittu) }}
                  {{ $t('tuntia-lyhenne') }}
                </p>
                <p class="mb-1">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('todistukset-tarkistettu') }}
                </p>
              </div>
              <div v-if="valmistumispyynnonTarkistus.sateilusuojakoulutusVaadittu > 0" class="my-3">
                <h5>
                  {{ $t('sateilysuojelukoulutus') }}
                </h5>
                <p class="mb-1">
                  <font-awesome-icon
                    v-if="
                      valmistumispyynnonTarkistus.sateilusuojakoulutusSuoritettu >=
                      valmistumispyynnonTarkistus.sateilusuojakoulutusVaadittu
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
                  {{ Math.round(valmistumispyynnonTarkistus.sateilusuojakoulutusSuoritettu) }} /
                  {{ Math.round(valmistumispyynnonTarkistus.sateilusuojakoulutusVaadittu) }}
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
                      valmistumispyynnonTarkistus.johtamiskoulutusSuoritettu >=
                      valmistumispyynnonTarkistus.johtamiskoulutusVaadittu
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
                  {{ Math.round(valmistumispyynnonTarkistus.johtamiskoulutusSuoritettu) }} /
                  {{ Math.round(valmistumispyynnonTarkistus.johtamiskoulutusVaadittu) }}
                  {{ $t('opintopistetta-lyhenne') }}
                </p>
              </div>
              <hr />
              <h2 class="mb-3">{{ $t('muut-tarkistukset') }}</h2>
              <opintosuoritus-tab
                class="mx-2"
                variant="kuulustelu"
                :suoritukset="valmistumispyynnonTarkistus.kuulustelut"
              />
              <div class="my-3">
                <h5>
                  {{ $t('koejakso') }}
                </h5>
                <p v-if="valmistumispyynnonTarkistus.koejaksoHyvaksyttyPvm" class="mb-1">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  {{ $t('hyvaksytty') }}
                  {{ $date(valmistumispyynnonTarkistus.koejaksoHyvaksyttyPvm) }}
                </p>
                <p
                  v-if="!valmistumispyynnonTarkistus.koejaksoHyvaksyttyPvm && editable"
                  class="mb-1"
                >
                  {{ $t('koejakso-ei-hyvaksytty') }}
                  <b-form-checkbox
                    v-model="form.koejaksoEiVaadittu"
                    class="mt-3"
                    @input="onSkipRouteExitConfirm"
                  >
                    {{ $t('koejaksoa-ei-vaadita') }}
                  </b-form-checkbox>
                </p>
                <p
                  v-if="!valmistumispyynnonTarkistus.koejaksoHyvaksyttyPvm && !editable"
                  class="mb-1"
                >
                  {{ $t('koejakso-ei-hyvaksytty') }}
                </p>
                <p v-if="form.koejaksoEiVaadittu && !editable" class="mb-1">
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
              <hr />
              <elsa-form-group :label="$t('virkailijan-valmistumisen-yhteenveto')">
                <ElsaTextEditor
                  v-if="editable"
                  v-model="form.virkailijanYhteenveto"
                  :init-options="{
                    plugins: ['lists'],
                    toolbar: 'undo redo | bold | bullist numlist',
                    block_formats: 'Paragraph=p',
                    valid_elements: 'p,br,strong/b,ul,ol,li',
                    statusbar: false,
                    height: 300,
                    menubar: false,
                    content_css: false,
                    skin: false
                  }"
                />
                <!-- eslint-disable vue/no-v-html -->
                <div
                  v-else
                  class="editor-readonly"
                  style="white-space: normal"
                  v-html="virkailijanYhteenvetoSanitized"
                ></div>
                <!-- eslint-enable vue/no-v-html -->
              </elsa-form-group>
              <h2 class="mb-3">{{ $t('huomiot') }}</h2>
              <elsa-form-group
                v-if="editable"
                :label="$t('kommentit-muille-virkailijoille-ei-nayteta-muille')"
              >
                <template #default="{ uid }">
                  <b-form-textarea
                    :id="uid"
                    v-model="form.kommentitVirkailijoille"
                    rows="7"
                    class="textarea-min-height"
                  ></b-form-textarea>
                </template>
              </elsa-form-group>
              <div v-if="!editable" class="my-3">
                <h5>{{ $t('kommentit-muille-virkailijoille-ei-nayteta-muille') }}</h5>
                <p v-if="form.kommentitVirkailijoille">
                  {{ form.kommentitVirkailijoille }}
                </p>
                <p v-else>
                  {{ $t('ei-kommentteja') }}
                </p>
              </div>
              <div v-if="valmistumispyynto.virkailijanKuittausaika && !editable">
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
                <hr />
              </div>
              <div v-if="valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika && !editable">
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
              <hr v-if="odottaaOsaamisenArviointia" />
              <div v-if="editable" class="text-right">
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
                <elsa-button
                  v-b-modal.confirm-save
                  style="min-width: 14rem"
                  variant="outline-primary"
                  class="ml-2"
                >
                  {{ $t('tallenna-keskeneraisena') }}
                </elsa-button>
                <elsa-button :loading="sending" type="submit" variant="primary" class="ml-2">
                  {{ $t('hyvaksy-laheta') }}
                </elsa-button>
              </div>
            </b-form>
          </div>
          <div v-else class="text-center mt-3">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
      <b-row>
        <elsa-form-error :active="$v.$anyError" />
      </b-row>
    </b-container>
    <elsa-confirmation-modal
      id="confirm-send"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('lahetyksen-jalkeen-vastuuhenkilo-hyvaksynta')"
      :submit-text="$t('hyvaksy-laheta')"
      @submit="onSend"
    >
      <template #modal-content>
        <elsa-form-group :label="$t('lisatiedot-vastuuhenkilolle')">
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="form.lisatiedotVastuuhenkilolle"
              rows="7"
            ></b-form-textarea>
          </template>
        </elsa-form-group>
      </template>
    </elsa-confirmation-modal>
    <elsa-return-to-sender-modal
      id="return-to-sender"
      :title="$t('palauta-erikoistuvalle-muokattavaksi')"
      @submit="onReturnToSender"
    />
    <elsa-confirmation-modal
      id="confirm-save"
      :title="$t('vahvista-tallennus-keskeneraisena-title')"
      :text="$t('vahvista-tallennus-keskeneraisena-body')"
      :submit-text="$t('tallenna-keskeneraisena')"
      @submit="saveAndExit"
    />
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import DOMPurify from 'dompurify'
  import { Component, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required, requiredIf } from 'vuelidate/lib/validators'

  import { ELSA_API_LOCATION } from '@/api'
  import { getValmistumispyyntoTarkistus, putValmistumispyynto } from '@/api/virkailija'
  import AsiakirjaButton from '@/components/asiakirjat/asiakirja-button.vue'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import ElsaTextEditor from '@/components/text-editor/text-editor.vue'
  import ValmistumispyyntoMixin from '@/mixins/valmistumispyynto'
  import {
    ValmistumispyyntoArviointienTila,
    ValmistumispyyntoVirkailijanTarkistus,
    ValmistumispyynnonVirkailijanTarkistusLomake,
    Asiakirja,
    ElsaError
  } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { ValmistumispyynnonTila } from '@/utils/constants'
  import { mapFile, mapFiles } from '@/utils/fileMapper'
  import { toastSuccess, toastFail } from '@/utils/toast'
  import OpintosuoritusTab from '@/views/opintosuoritukset/opintosuoritus-tab.vue'

  @Component({
    components: {
      AsiakirjatContent,
      AsiakirjatUpload,
      ElsaButton,
      ElsaFormGroup,
      ElsaFormDatepicker,
      ElsaFormError,
      ErikoistuvaDetails,
      ElsaConfirmationModal,
      ElsaReturnToSenderModal,
      OpintosuoritusTab,
      AsiakirjaButton,
      ElsaTextEditor
    }
  })
  export default class ValmistumispyynnonTarkistus extends Mixins<ValmistumispyyntoMixin>(
    validationMixin,
    ValmistumispyyntoMixin
  ) {
    $refs!: {
      laillistamispaiva: ElsaFormDatepicker
    }
    validations() {
      return {
        form: {
          terveyskeskustyoTarkistettu: { checked: (value: boolean) => value === true },
          yliopistosairaalanUlkopuolinenTyoTarkistettu: {
            checked: (value: boolean) => value === true
          },
          yliopistosairaalatyoTarkistettu: { checked: (value: boolean) => value === true },
          kokonaistyoaikaTarkistettu: { checked: (value: boolean) => value === true },
          teoriakoulutusTarkistettu: { checked: (value: boolean) => value === true },
          laillistamispaiva: {
            required
          },
          laillistamistodistus: {
            required: requiredIf(() => {
              return this.laillistamispaivaAsiakirjat.length === 0
            })
          }
        }
      }
    }

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
        text: this.$t('valmistumispyynto'),
        active: true
      }
    ]

    form: ValmistumispyynnonVirkailijanTarkistusLomake = {
      id: null,
      yekSuoritettu: false,
      yekSuorituspaiva: null,
      ptlSuoritettu: false,
      ptlSuorituspaiva: null,
      aiempiElKoulutusSuoritettu: false,
      aiempiElKoulutusSuorituspaiva: null,
      ltTutkintoSuoritettu: false,
      ltTutkintoSuorituspaiva: null,
      terveyskeskustyoTarkistettu: false,
      yliopistosairaalanUlkopuolinenTyoTarkistettu: false,
      yliopistosairaalatyoTarkistettu: false,
      kokonaistyoaikaTarkistettu: false,
      teoriakoulutusTarkistettu: false,
      koejaksoEiVaadittu: false,
      kommentitVirkailijoille: null,
      lisatiedotVastuuhenkilolle: null,
      keskenerainen: false,
      laillistamispaiva: null,
      laillistamistodistus: null,
      virkailijanYhteenveto: null
    }

    valmistumispyynnonTarkistus: ValmistumispyyntoVirkailijanTarkistus | null = null
    valmistumispyyntoArviointienTila: ValmistumispyyntoArviointienTila | null = null
    response: ValmistumispyynnonVirkailijanTarkistusLomake | null = null
    loading = true
    sending = false
    editable = true
    draft = false
    yekSuorituspaivaTila = false
    ptlSuorituspaivaTila = false
    aiempiElKoulutusSuorituspaivaTila = false
    ltTutkintoSuorituspaivaTila = false
    yhteenvetoAsiakirjaUrl: string | null = null
    liitteetAsiakirjaUrl: string | null = null
    laillistamispaivaAsiakirjat: Asiakirja[] = []
    laillistaminenMuokattavissa = false
    skipRouteExitConfirm = true

    get virkailijanYhteenvetoSanitized(): string {
      const raw = this.valmistumispyynnonTarkistus?.virkailijanYhteenveto ?? ''
      return DOMPurify.sanitize(raw)
    }

    async mounted() {
      const valmistumispyyntoId = this.$route?.params?.valmistumispyyntoId
      if (valmistumispyyntoId) {
        try {
          await getValmistumispyyntoTarkistus(parseInt(valmistumispyyntoId)).then((response) => {
            this.valmistumispyynnonTarkistus = response.data
            this.form = {
              ...this.valmistumispyynnonTarkistus,
              laillistamispaiva: null,
              laillistamistodistus: null,
              keskenerainen: false
            }
            if (response.data.valmistumispyynto) {
              this.valmistumispyynto = response.data.valmistumispyynto
            }
            if (
              this.valmistumispyynto.tila ==
                ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA ||
              this.valmistumispyynto.tila ==
                ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU ||
              this.valmistumispyynto.tila ==
                ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU ||
              this.valmistumispyynto.tila == ValmistumispyynnonTila.HYVAKSYTTY
            ) {
              this.editable = false
            }
            if (
              this.valmistumispyynto.tila == ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN
            ) {
              this.draft = true
              this.editable = true
            }
            if (!this.draft || !this.editable) {
              this.yekSuorituspaivaTila = response.data.yekSuorituspaiva ? true : false
              this.ptlSuorituspaivaTila = response.data.ptlSuorituspaiva ? true : false
              this.aiempiElKoulutusSuorituspaivaTila = response.data.aiempiElKoulutusSuorituspaiva
                ? true
                : false
              this.ltTutkintoSuorituspaivaTila = response.data.ltTutkintoSuorituspaiva
                ? true
                : false
            } else {
              this.yekSuorituspaivaTila = false
              this.ptlSuorituspaivaTila = false
              this.aiempiElKoulutusSuorituspaivaTila = false
              this.ltTutkintoSuorituspaivaTila = false
            }
            if (this.valmistumispyynto.yhteenvetoAsiakirjaId) {
              this.yhteenvetoAsiakirjaUrl = `/virkailija/valmistumispyynto/${this.valmistumispyynto.id}/asiakirja/`
            }
            if (this.valmistumispyynto.liitteetAsiakirjaId) {
              this.liitteetAsiakirjaUrl = `/virkailija/valmistumispyynto/${this.valmistumispyynto.id}/asiakirja/`
            }
          })
          if (this.valmistumispyynto.erikoistujanLaillistamistodistus) {
            const data = Uint8Array.from(
              atob(this.valmistumispyynto.erikoistujanLaillistamistodistus),
              (c) => c.charCodeAt(0)
            )
            this.laillistamispaivaAsiakirjat.push(
              mapFile(
                new File(
                  [data],
                  this.valmistumispyynto?.erikoistujanLaillistamistodistusNimi || '',
                  {
                    type: this.valmistumispyynto?.erikoistujanLaillistamistodistusTyyppi
                  }
                )
              )
            )
          }
          if (this.valmistumispyynto.erikoistujanLaillistamispaiva) {
            this.form.laillistamispaiva = this.valmistumispyynto.erikoistujanLaillistamispaiva
          }
          this.loading = false
        } catch {
          toastFail(this, this.$t('valmistumispyynnon-hakeminen-epaonnistui'))
          this.loading = false
          this.$router.replace({ name: 'valmistumispyynnot' })
        }
      }
    }

    async vaihdaRooli(id: number | undefined) {
      if (this.editable && !this.skipRouteExitConfirm) {
        if (!(await confirmExit(this))) {
          return
        }
      }

      this.$emit('skipRouteExitConfirm', true)
      window.location.href = `${ELSA_API_LOCATION}/api/login/impersonate?opintooikeusId=${id}&originalUrl=${window.location.href}`
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.laillistamispaiva ? this.$refs.laillistamispaiva.validateForm() : true
      ]
      if (validations.includes(false)) return
      return this.$bvModal.show('confirm-send')
    }

    saveAndExit() {
      this.form.keskenerainen = true
      this.onSend()
    }

    async onSend() {
      try {
        this.sending = true
        if (this.valmistumispyynto?.id) {
          this.form.id = this.valmistumispyynto.id
        }
        const form: ValmistumispyynnonVirkailijanTarkistusLomake = {
          id: this.form.id,
          yekSuoritettu: this.form.yekSuoritettu,
          yekSuorituspaiva: this.form.yekSuorituspaiva,
          ptlSuoritettu: this.form.ptlSuoritettu,
          ptlSuorituspaiva: this.form.ptlSuorituspaiva,
          aiempiElKoulutusSuoritettu: this.form.aiempiElKoulutusSuoritettu,
          aiempiElKoulutusSuorituspaiva: this.form.aiempiElKoulutusSuorituspaiva,
          ltTutkintoSuoritettu: this.form.ltTutkintoSuoritettu,
          ltTutkintoSuorituspaiva: this.form.ltTutkintoSuorituspaiva,
          terveyskeskustyoTarkistettu: this.form.terveyskeskustyoTarkistettu,
          yliopistosairaalanUlkopuolinenTyoTarkistettu:
            this.form.yliopistosairaalanUlkopuolinenTyoTarkistettu,
          yliopistosairaalatyoTarkistettu: this.form.yliopistosairaalatyoTarkistettu,
          kokonaistyoaikaTarkistettu: this.form.kokonaistyoaikaTarkistettu,
          teoriakoulutusTarkistettu: this.form.teoriakoulutusTarkistettu,
          kommentitVirkailijoille: this.form.kommentitVirkailijoille,
          koejaksoEiVaadittu: this.form.koejaksoEiVaadittu,
          lisatiedotVastuuhenkilolle: this.form.lisatiedotVastuuhenkilolle || null,
          keskenerainen: this.form.keskenerainen,
          laillistamispaiva: this.laillistaminenMuokattavissa ? this.form.laillistamispaiva : null,
          laillistamistodistus: this.laillistaminenMuokattavissa
            ? this.form.laillistamistodistus
            : null,
          virkailijanYhteenveto: this.form.virkailijanYhteenveto
        }
        this.response = (await putValmistumispyynto(form)).data
        this.$emit('skipRouteExitConfirm', true)
        if (this.form.keskenerainen) {
          toastSuccess(this, this.$t('virkailijan-tarkistus-tallennettu-keskeneraisena'))
        } else {
          toastSuccess(this, this.$t('virkailijan-tarkistus-lahetetty-onnistuneesti'))
        }
        this.$router.replace({ name: 'valmistumispyynnot' })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('virkailijan-tarkistus-lahetys-epaonnistui')}: ${this.$t(message)}`
            : this.$t('virkailijan-tarkistus-lahetys-epaonnistui')
        )
      }
      this.sending = false
    }

    async onReturnToSender(korjausehdotus: string) {
      if (this.valmistumispyynto?.id) {
        this.form.id = this.valmistumispyynto.id
      }
      const form: ValmistumispyynnonVirkailijanTarkistusLomake = {
        id: this.form.id,
        yekSuoritettu: this.form.yekSuoritettu,
        yekSuorituspaiva: this.form.yekSuorituspaiva,
        ptlSuoritettu: this.form.ptlSuoritettu,
        ptlSuorituspaiva: this.form.ptlSuorituspaiva,
        aiempiElKoulutusSuoritettu: this.form.aiempiElKoulutusSuoritettu,
        aiempiElKoulutusSuorituspaiva: this.form.aiempiElKoulutusSuorituspaiva,
        ltTutkintoSuoritettu: this.form.ltTutkintoSuoritettu,
        ltTutkintoSuorituspaiva: this.form.ltTutkintoSuorituspaiva,
        terveyskeskustyoTarkistettu: this.form.terveyskeskustyoTarkistettu,
        yliopistosairaalanUlkopuolinenTyoTarkistettu:
          this.form.yliopistosairaalanUlkopuolinenTyoTarkistettu,
        yliopistosairaalatyoTarkistettu: this.form.yliopistosairaalatyoTarkistettu,
        kokonaistyoaikaTarkistettu: this.form.kokonaistyoaikaTarkistettu,
        teoriakoulutusTarkistettu: this.form.teoriakoulutusTarkistettu,
        koejaksoEiVaadittu: this.form.koejaksoEiVaadittu,
        kommentitVirkailijoille: this.form.kommentitVirkailijoille,
        virkailijanYhteenveto: this.form.virkailijanYhteenveto,
        keskenerainen: false,
        korjausehdotus: korjausehdotus,
        laillistamispaiva: this.laillistaminenMuokattavissa ? this.form.laillistamispaiva : null,
        laillistamistodistus: this.laillistaminenMuokattavissa
          ? this.form.laillistamistodistus
          : null
      }
      try {
        this.sending = true
        this.response = (await putValmistumispyynto(form)).data
        this.$emit('skipRouteExitConfirm', true)
        toastSuccess(this, this.$t('virkailijan-tarkistus-palautettu-onnistuneesti'))
        this.$router.replace({ name: 'valmistumispyynnot' })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('virkailijan-tarkistus-palautus-epaonnistui')}: ${this.$t(message)}`
            : this.$t('virkailijan-tarkistus-palautus-epaonnistui')
        )
      }
      this.sending = false
    }

    muokkaaLaillistamista() {
      this.laillistaminenMuokattavissa = true
    }

    onLaillistamistodistusFilesAdded(files: File[]) {
      this.form.laillistamistodistus = files[0]
      this.laillistamispaivaAsiakirjat.push(...mapFiles(files))
    }

    async onDeletelaillistamistodistus() {
      this.form.laillistamistodistus = null
      this.laillistamispaivaAsiakirjat = []
      this.onSkipRouteExitConfirm()
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
