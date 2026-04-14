<template>
  <div>
    <b-table-simple borderless class="arviointi-table">
      <b-tr>
        <b-th scope="row" class="align-middle font-weight-500">
          {{ $t('erikoistuja') }}
        </b-th>
        <b-td>
          <user-avatar
            :src-base64="value.arvioinninSaaja.avatar"
            src-content-type="image/jpeg"
            :display-name="value.arvioinninSaaja.nimi"
          />
        </b-td>
      </b-tr>
      <b-tr>
        <b-th scope="row" class="font-weight-500">{{ $t('tyoskentelyjakso') }}</b-th>
        <b-td>
          {{ value.tyoskentelyjakso.tyoskentelypaikka.nimi }} ({{
            value.tyoskentelyjakso.alkamispaiva ? $date(value.tyoskentelyjakso.alkamispaiva) : ''
          }}
          –
          <span :class="value.tyoskentelyjakso.paattymispaiva ? '' : 'text-lowercase'">
            {{
              value.tyoskentelyjakso.paattymispaiva
                ? $date(value.tyoskentelyjakso.paattymispaiva)
                : $t('kesken')
            }})
          </span>
        </b-td>
      </b-tr>
      <b-tr>
        <b-th scope="row" class="font-weight-500">{{ $t('arvioitava-tapahtuma') }}</b-th>
        <b-td>
          {{ value.arvioitavaTapahtuma }}
          <span v-if="$isErikoistuva() && !value.arviointiAika && !account.impersonated">
            <span>(</span>
            <elsa-button
              variant="link"
              class="text-decoration-none shadow-none d-table-row border-0 m-0 p-0 text-lowercase"
              :to="{ name: 'arviointipyynto-muokkaus', params: { arviointiId: value.id } }"
            >
              {{ $t('muokkaa-arviointipyyntoa') }}
            </elsa-button>
            <span>)</span>
          </span>
        </b-td>
      </b-tr>
      <b-tr>
        <b-th scope="row" class="font-weight-500">{{ $t('tapahtuman-ajankohta') }}</b-th>
        <b-td>{{ $date(value.tapahtumanAjankohta) }}</b-td>
      </b-tr>
      <b-tr v-if="value.lisatiedot">
        <b-th scope="row" class="font-weight-500">{{ $t('lisatiedot') }}</b-th>
        <b-td>
          {{ value.lisatiedot }}
        </b-td>
      </b-tr>
      <b-tr>
        <b-th scope="row" class="align-middle font-weight-500">{{ $t('arvioija') }}</b-th>
        <b-td>
          <user-avatar
            :src-base64="value.arvioinninAntaja.avatar"
            src-content-type="image/jpeg"
            :display-name="value.arvioinninAntaja.nimi"
          />
        </b-td>
      </b-tr>
      <b-tr v-if="!editing">
        <b-th scope="row" class="align-middle font-weight-500">{{ $t('arviointityokalu') }}</b-th>
        <b-td>
          {{ value.arviointityokalut.map((el) => el.nimi).join(', ') }}
        </b-td>
      </b-tr>
      <b-tr v-if="!editing">
        <b-th scope="row" class="align-middle font-weight-500">{{ $t('arviointi-perustuu') }}</b-th>
        <b-td>
          {{ muuValittu ? value.muuPeruste : $t(value.arviointiPerustuu) }}
        </b-td>
      </b-tr>
    </b-table-simple>
    <hr />

    <b-form @submit.stop.prevent="onSubmit">
      <div v-if="!editing">
        <b-table-simple class="mt-2 mb-4" responsive bordered>
          <b-thead>
            <b-tr>
              <b-th scope="col" style="width: 40%">
                {{ $t('arvioitava-kokonaisuus') }},
                {{ arviointiAsteikonNimi }}
                <elsa-popover :title="arviointiAsteikonNimi">
                  <elsa-arviointiasteikon-taso-tooltip-content
                    :arviointiasteikon-nimi="arviointiAsteikonNimi"
                    :arviointiasteikon-tasot="arviointiasteikonTasot"
                  />
                </elsa-popover>
              </b-th>
              <b-th scope="col" style="width: 30%">
                {{ $t('arviointi') }}
                <span v-if="value.arviointiAika" class="font-weight-400">
                  {{ ` (${$date(value.arviointiAika)})` }}
                </span>
              </b-th>
              <b-th scope="col" style="width: 30%">
                {{ $t('itsearviointi') }}
                <span v-if="value.itsearviointiAika" class="font-weight-400">
                  {{ ` (${$date(value.itsearviointiAika)})` }}
                </span>
              </b-th>
            </b-tr>
          </b-thead>
          <b-tbody>
            <b-tr
              v-for="(kokonaisuus, index) in value.arvioitavatKokonaisuudet"
              :key="kokonaisuus.id"
            >
              <b-td>
                {{ kokonaisuus.arvioitavaKokonaisuus.kategoria.nimi }}:
                {{ kokonaisuus.arvioitavaKokonaisuus.nimi }}
                <elsa-popover :title="kokonaisuus.arvioitavaKokonaisuus.nimi">
                  <!-- eslint-disable-next-line vue/no-v-html -->
                  <p v-html="kokonaisuus.arvioitavaKokonaisuus.kuvaus" />
                </elsa-popover>
              </b-td>
              <b-td>
                <div v-if="!value.arviointiAika && index === 0" class="d-inline-flex">
                  <elsa-button
                    v-if="$isKouluttaja() || $isVastuuhenkilo()"
                    variant="primary"
                    class="d-flex align-items-center text-decoration-none"
                    :to="{
                      name: 'muokkaa-arviointia',
                      params: { arviointiId: value.id }
                    }"
                  >
                    {{ $t('tee-arviointi') }}
                  </elsa-button>
                  <span v-else class="text-size-sm text-lighter ml-2">
                    {{ $t('arviointia-ei-ole-viela-annettu') }}
                  </span>
                </div>
                <elsa-arviointiasteikon-taso
                  v-if="value.arviointiAika"
                  :value="kokonaisuus.arviointiasteikonTaso"
                  :tasot="value.arviointiasteikko.tasot"
                />
              </b-td>
              <b-td>
                <div v-if="!value.itsearviointiAika && index === 0" class="d-inline-flex">
                  <elsa-button
                    v-if="$isErikoistuva() && !value.lukittu && !account.impersonated"
                    variant="primary"
                    class="d-flex align-items-center text-decoration-none"
                    :to="{
                      name: 'itsearviointi',
                      params: { arviointiId: value.id }
                    }"
                  >
                    {{ $t('tee-itsearviointi') }}
                  </elsa-button>
                  <span v-else-if="!value.lukittu" class="text-size-sm text-lighter ml-2">
                    {{ $t('itsearviointia-ei-viela-tehty') }}
                  </span>
                  <span v-else-if="$isErikoistuva()" class="text-size-sm text-lighter ml-2">
                    {{ $t('suoritusarviointi-on-lukittu-itsearviointi-luonti') }}
                  </span>
                  <span v-else class="text-size-sm text-lighter ml-2">
                    {{ $t('itsearviointia-ei-tehty') }}
                  </span>
                </div>
                <elsa-arviointiasteikon-taso
                  v-if="value.itsearviointiAika"
                  :value="kokonaisuus.itsearviointiArviointiasteikonTaso"
                  :tasot="value.arviointiasteikko.tasot"
                />
              </b-td>
            </b-tr>
          </b-tbody>
        </b-table-simple>
        <b-table-simple class="mt-2 mb-4" responsive bordered>
          <b-tbody>
            <b-tr>
              <b-th scope="row" style="width: 40%">
                {{ $t('vaativuustaso') }}
                <elsa-popover :title="$t('vaativuustaso')">
                  <elsa-vaativuustaso-tooltip-content />
                </elsa-popover>
              </b-th>
              <b-td style="width: 30%">
                <elsa-vaativuustaso v-if="value.vaativuustaso" :value="value.vaativuustaso" />
              </b-td>
              <b-td style="width: 30%">
                <elsa-vaativuustaso
                  v-if="value.itsearviointiVaativuustaso"
                  :value="value.itsearviointiVaativuustaso"
                />
              </b-td>
            </b-tr>
          </b-tbody>
        </b-table-simple>
        <b-row
          v-for="(arviointityokalu, index) in listattavatArviointityokalut"
          :key="arviointityokalu.id || index"
          class="mb-2"
        >
          <b-col>
            <elsa-accordian :ref="arviointityokalu.nimi" :visible="true">
              <template #title>
                {{ arviointityokalu.nimi }}
              </template>
              <arviointityokalu-kysymys-lomake-vastauksilla
                v-for="(kysymys, kysymysIndex) in arviointityokalu.kysymykset"
                :key="kysymysIndex"
                :kysymys="kysymys"
                :vastaus="getKysymyksenVastaus(kysymys.id, arviointityokalu.id)"
              />
            </elsa-accordian>
          </b-col>
        </b-row>
        <elsa-form-group :label="$t('sanallinen-kokonaisarviointi')">
          <template #default="{ uid }">
            <p :id="uid" class="text-preline text-break">{{ value.sanallinenArviointi }}</p>
          </template>
        </elsa-form-group>
        <div
          v-if="value.arviointiAika && ($isKouluttaja() || $isVastuuhenkilo())"
          class="text-right"
        >
          <elsa-button
            variant="primary"
            :disabled="value.lukittu"
            :to="{
              name: 'muokkaa-arviointia',
              params: { arviointiId: value.id }
            }"
          >
            {{ $t('muokkaa-arviointia') }}
          </elsa-button>
          <b-row v-if="value.lukittu">
            <b-col>
              <div class="d-flex flex-row mb-4">
                <em class="align-middle">
                  <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-1" />
                </em>
                <div>
                  <span class="text-size-sm">{{ $t('suoritusarviointi-on-lukittu') }}</span>
                </div>
              </div>
            </b-col>
          </b-row>
        </div>
        <elsa-form-group v-if="value.arviointiAsiakirjat.length > 0" :label="$t('liitetiedostot')">
          <asiakirjat-content
            class="px-0 col-md-8 col-lg-12 col-xl-8 border-bottom-none"
            :asiakirjat="value.arviointiAsiakirjat"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :show-info-if-empty="false"
            :enable-delete="false"
            :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
          />
        </elsa-form-group>
        <hr />
        <elsa-form-group :label="$t('sanallinen-itsearviointi')">
          <template #default="{ uid }">
            <p :id="uid" class="text-preline text-break">
              {{ value.sanallinenItsearviointi }}
            </p>
          </template>
        </elsa-form-group>
        <elsa-form-group
          v-if="value.itsearviointiAsiakirjat.length > 0"
          :label="$t('liitetiedostot')"
          class="col-lg-12"
        >
          <asiakirjat-content
            class="px-0 col-lg-12 border-bottom-none"
            :asiakirjat="value.itsearviointiAsiakirjat"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :show-info-if-empty="false"
            :enable-delete="false"
            :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
          />
        </elsa-form-group>
        <div v-if="value.itsearviointiAika && $isErikoistuva()" class="text-right">
          <elsa-button
            variant="primary"
            :disabled="value.lukittu"
            :to="{
              name: 'itsearviointi',
              params: { arviointiId: value.id }
            }"
          >
            {{ $t('muokkaa-itsearviointia') }}
          </elsa-button>
          <b-row v-if="value.lukittu">
            <b-col>
              <div class="d-flex flex-row mb-4">
                <em class="align-middle">
                  <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-1" />
                </em>
                <div>
                  <span class="text-size-sm">
                    {{ $t('suoritusarviointi-on-lukittu-itsearviointi-muokkaus') }}
                  </span>
                </div>
              </div>
            </b-col>
          </b-row>
        </div>
        <hr />
      </div>
      <div v-else>
        <div v-for="kokonaisuus in form.arvioitavatKokonaisuudet" :key="kokonaisuus.id">
          <elsa-form-group :label="$t('arvioitava-kokonaisuus')">
            {{ kokonaisuus.arvioitavaKokonaisuus.kategoria.nimi }}:
            {{ kokonaisuus.arvioitavaKokonaisuus.nimi }}
            <elsa-popover :title="kokonaisuus.arvioitavaKokonaisuus.nimi">
              <!-- eslint-disable-next-line vue/no-v-html -->
              <p v-html="kokonaisuus.arvioitavaKokonaisuus.kuvaus" />
            </elsa-popover>
          </elsa-form-group>
        </div>
        <b-form-row v-if="$isKouluttaja() || $isVastuuhenkilo()">
          <elsa-form-group :label="$t('arviointityokalu')" :required="false" class="col-lg-12">
            <template #default="{ uid }">
              <b-row class="align-items-end">
                <b-col>
                  <elsa-form-multiselect
                    :id="uid"
                    v-model="form.arviointityokalut"
                    :options="formattedArviointityokalut"
                    :multiple="true"
                    group-values="nimi"
                    group-label="kategoria"
                    :group-select="true"
                    track-by="id"
                    :custom-label="arviointityokaluLabel"
                    :state="pakollisetVastauksetValid"
                    :allow-empty="true"
                    @input="onArviointityokalutChange"
                  />
                </b-col>
                <b-col cols="auto">
                  <elsa-button
                    variant="outline-primary"
                    class="mt-2"
                    :disabled="arviointityokalujaEiValittuna"
                    @click="openArviointityokalutModal"
                  >
                    {{ $t('avaa-arviointityokalu') }}
                  </elsa-button>
                </b-col>
              </b-row>
              <b-form-invalid-feedback
                :id="`${uid}-feedback`"
                :force-show="!pakollisetVastauksetValid"
              >
                {{ $t('arviointityokalu-kaikkia-pakollisia-vastauksia-ei-ole-annettu') }}
              </b-form-invalid-feedback>
              <asiakirjat-upload
                class="mt-1"
                :is-primary-button="false"
                :allow-multiples-files="false"
                :button-text="$t('lisaa-liitetiedosto')"
                :wrong-file-type-error-message="$t('sallitut-tiedostoformaatit-pdf')"
                :allowed-file-types="['application/pdf']"
                :is-text-button="true"
                :file-upload-texts="fileUploadTexts"
                @selectedFiles="onArviointiFileAdded"
              />
              <asiakirjat-content
                class="col-lg-12"
                :asiakirjat="asiakirjatTableItems"
                :sorting-enabled="false"
                :pagination-enabled="false"
                :enable-search="false"
                :show-info-if-empty="false"
                :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
                @deleteAsiakirja="onArviointiFileDeleted"
              />
              <arviointityokalut-modal
                v-model="isArviointityokalutModalOpen"
                :valitut-arviointityokalut="form.arviointityokalut"
                :arviointityokalu-vastaukset="form.arviointityokaluVastaukset"
                :can-validate="canValidate"
                @submit="lisaaArviointityokaluVastaukset"
              ></arviointityokalut-modal>
            </template>
          </elsa-form-group>
        </b-form-row>
        <b-form-row>
          <elsa-form-group :label="$t('vaativuustaso')" class="col-lg-12">
            <template #label-help>
              <elsa-popover :title="$t('vaativuustaso')">
                <elsa-vaativuustaso-tooltip-content />
              </elsa-popover>
            </template>
            <template #default="{ uid }">
              <elsa-form-multiselect
                :id="uid"
                v-model="form.vaativuustaso"
                :options="vaativuustasot"
                :custom-label="vaativuustasoLabel"
                track-by="arvo"
                @input="$emit('skipRouteExitConfirm', false)"
              ></elsa-form-multiselect>
              <b-form-invalid-feedback :id="`${uid}-feedback`">
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </template>
          </elsa-form-group>
        </b-form-row>
        <div v-for="(kokonaisuus, index) in form.arvioitavatKokonaisuudet" :key="kokonaisuus.id">
          <hr v-if="form.arvioitavatKokonaisuudet && form.arvioitavatKokonaisuudet.length > 1" />
          <b-form-row>
            <elsa-form-group :label="$t('arvioitava-kokonaisuus')" class="col-lg-12">
              {{ kokonaisuus.arvioitavaKokonaisuus.kategoria.nimi }}:
              {{ kokonaisuus.arvioitavaKokonaisuus.nimi }}
              <elsa-popover :title="kokonaisuus.arvioitavaKokonaisuus.nimi">
                <!-- eslint-disable-next-line vue/no-v-html -->
                <p v-html="kokonaisuus.arvioitavaKokonaisuus.kuvaus" />
              </elsa-popover>
            </elsa-form-group>
            <elsa-form-group :label="arviointiAsteikonNimi" :required="true" class="col-lg-12">
              <template #label-help>
                <elsa-popover :title="arviointiAsteikonNimi">
                  <elsa-arviointiasteikon-taso-tooltip-content
                    :arviointiasteikon-tasot="arviointiasteikonTasot"
                  />
                </elsa-popover>
              </template>
              <template #default="{ uid }">
                <elsa-form-multiselect
                  :id="uid"
                  v-model="kokonaisuus.arviointiasteikonTaso"
                  :options="arviointiasteikonTasot"
                  :state="validateArvioitavaKokonaisuusState(index)"
                  label="name"
                  :custom-label="arviointiasteikonTasoLabel"
                  track-by="taso"
                  @input="$emit('skipRouteExitConfirm', false)"
                ></elsa-form-multiselect>
                <b-form-invalid-feedback :id="`${uid}-feedback`">
                  {{ $t('pakollinen-tieto') }}
                </b-form-invalid-feedback>
              </template>
            </elsa-form-group>
          </b-form-row>
          <hr
            v-if="
              form.arvioitavatKokonaisuudet &&
              form.arvioitavatKokonaisuudet.length > 0 &&
              index === form.arvioitavatKokonaisuudet.length - 1
            "
          />
        </div>
        <div v-if="form.arvioitavatKokonaisuudet && form.arvioitavatKokonaisuudet.length > 1">
          <h3>{{ $t('yhteiset-arviointisisallot') }}</h3>
          <p>{{ $t('yhteiset-arviointisisallot-kuvaus') }}</p>
        </div>
        <elsa-form-group
          v-if="editing"
          :label="$t('sanallinen-kokonaisarviointi')"
          :required="true"
        >
          <template #label-help>
            <elsa-popover :title="$t('arvioinnin-osa-alueita')">
              {{ $t('canmeds-ohjeteksti') }}
            </elsa-popover>
          </template>
          <template #default="{ uid }">
            <div v-if="itsearviointi">
              <p class="mb-2">
                {{ $t('sanallinen-itsearviointi-ohjeteksti') }}
              </p>
            </div>
            <b-form-textarea
              :id="uid"
              v-model="form.sanallinenArviointi"
              :state="validateState('sanallinenArviointi')"
              rows="5"
              @input="$emit('skipRouteExitConfirm', false)"
            ></b-form-textarea>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-form-group v-if="$isErikoistuva()" :label="$t('liitetiedostot')">
          <template #label-help>
            <elsa-popover>
              {{ $t('arviointi-liite-tooltip') }}
            </elsa-popover>
          </template>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <span>{{ $t('arviointi-liitetiedostot-kuvaus-itsearviointi') }}</span>
          <asiakirjat-upload
            class="mt-3"
            :is-primary-button="false"
            :allow-multiples-files="false"
            :button-text="$t('lisaa-liitetiedosto')"
            :wrong-file-type-error-message="$t('sallitut-tiedostoformaatit-pdf')"
            :allowed-file-types="['application/pdf']"
            @selectedFiles="onArviointiFileAdded"
          />
          <asiakirjat-content
            class="px-0 col-md-8 col-lg-12 col-xl-8"
            :asiakirjat="asiakirjatTableItems"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :show-info-if-empty="false"
            :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
            @deleteAsiakirja="onArviointiFileDeleted"
          />
        </elsa-form-group>
        <elsa-form-group
          v-if="editing && ($isKouluttaja() || $isVastuuhenkilo())"
          :label="$t('lisatiedot')"
        >
          <template #default="{ uid }">
            <b-form-checkbox
              v-model="form.perustuuMuuhun"
              @input="$emit('skipRouteExitConfirm', false)"
            >
              {{ $t('arviointi-perustuu-lasna') }}
            </b-form-checkbox>
            <div v-if="form.perustuuMuuhun" class="arviointi-perustuu">
              <b-form-radio-group
                :id="uid"
                v-model="form.arviointiPerustuu"
                :options="arviointiperusteet"
                :state="validateState('arviointiPerustuu')"
                stacked
                @input="$emit('skipRouteExitConfirm', false)"
              ></b-form-radio-group>
              <b-form-invalid-feedback
                :id="`${uid}-feedback`"
                :state="validateState('arviointiPerustuu')"
              >
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
              <div v-if="muuValittu">
                <b-form-input
                  v-model="form.muuPeruste"
                  maxlength="255"
                  :state="validateState('muuPeruste')"
                  @input="$emit('skipRouteExitConfirm', false)"
                ></b-form-input>
                <p v-if="muuPerustePituus() >= 5">{{ $t('maksimi-pituus') }} 255</p>
              </div>
              <b-form-invalid-feedback>{{ $t('pakollinen-tieto') }}</b-form-invalid-feedback>
            </div>
          </template>
        </elsa-form-group>
        <div class="text-right">
          <elsa-button
            variant="back"
            :to="{
              name: 'arviointi',
              params: { arviointiId: $route.params.arviointiId }
            }"
          >
            {{ $t('peruuta') }}
          </elsa-button>
          <elsa-button
            v-if="!value.arviointiAika"
            v-b-modal.confirm-save
            style="min-width: 14rem"
            variant="outline-primary"
            class="ml-2"
          >
            {{ $t('tallenna-keskeneraisena') }}
          </elsa-button>
          <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2">
            {{ itsearviointi ? $t('tallenna') : $t('laheta') }}
          </elsa-button>
        </div>
      </div>
      <div class="row">
        <elsa-form-error :active="$v.$anyError || !pakollisetVastauksetValid" />
      </div>
    </b-form>
    <elsa-confirmation-modal
      id="confirm-save"
      :title="$t('vahvista-tallennus-keskeneraisena-title')"
      :text="$t('vahvista-tallennus-keskeneraisena-body')"
      :submit-text="$t('tallenna-keskeneraisena')"
      @submit="saveUnfinishedAndExit"
    />
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { Validation, validationMixin } from 'vuelidate'
  import { required, requiredIf } from 'vuelidate/lib/validators'

  import ElsaAccordian from '@/components/accordian/accordian.vue'
  import ElsaArviointiasteikonTasoTooltipContent from '@/components/arviointiasteikon-taso/arviointiasteikon-taso-tooltip.vue'
  import ElsaArviointiasteikonTaso from '@/components/arviointiasteikon-taso/arviointiasteikon-taso.vue'
  import ArviointityokaluKysymysLomakeVastauksilla from '@/components/arviointityokalut/arviointityokalu-kysymys-lomake-vastauksilla.vue'
  import ArviointityokalutModal from '@/components/arviointityokalut/arviointityokalut-modal.vue'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaBadge from '@/components/badge/badge.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import TyokertymalaskuriModal from '@/components/tyokertymalaskuri/tyokertymalaskuri-modal.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import ElsaVaativuustasoTooltipContent from '@/components/vaativuustaso/vaativuustaso-tooltip-content.vue'
  import ElsaVaativuustaso from '@/components/vaativuustaso/vaativuustaso.vue'
  import ArviointityokaluLomakeKysymysForm from '@/forms/arviointityokalu-lomake-kysymys-form.vue'
  import store from '@/store'
  import {
    ArviointiasteikonTaso,
    Arviointityokalu,
    Asiakirja,
    FileUploadText,
    Suoritusarviointi,
    SuoritusarviointiArviointityokaluVastaus,
    SuoritusarviointiForm,
    Vaativuustaso
  } from '@/types'
  import { resolveRolePath } from '@/utils/apiRolePathResolver'
  import {
    ArvioinninPerustuminen,
    ArviointiasteikkoTyyppi,
    MUU_ARVIOINTITYOKALU_ID,
    vaativuustasot
  } from '@/utils/constants'
  import { mapFile } from '@/utils/fileMapper'
  import { sortByAsc } from '@/utils/sort'

  @Component({
    components: {
      ElsaConfirmationModal,
      ElsaAccordian,
      ArviointityokaluKysymysLomakeVastauksilla,
      ArviointityokaluLomakeKysymysForm,
      TyokertymalaskuriModal,
      ArviointityokalutModal,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      UserAvatar,
      ElsaArviointiasteikonTaso,
      ElsaBadge,
      ElsaPopover,
      ElsaButton,
      ElsaVaativuustaso,
      AsiakirjatUpload,
      AsiakirjatContent,
      ElsaVaativuustasoTooltipContent,
      ElsaArviointiasteikonTasoTooltipContent
    },
    validations: {
      form: {
        arvioitavatKokonaisuudet: {
          $each: {
            arviointiasteikonTaso: {
              required
            }
          }
        },
        sanallinenArviointi: {
          required
        },
        arviointiPerustuu: {
          required: requiredIf((value) => {
            return value.perustuuMuuhun === true
          })
        },
        muuPeruste: {
          required: requiredIf((value) => {
            return value.arviointiPerustuu === ArvioinninPerustuminen.MUU
          })
        }
      }
    }
  })
  export default class ArviointiForm extends Mixins(validationMixin) {
    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: true, type: Object })
    value!: Suoritusarviointi

    @Prop({ required: false, default: false })
    itsearviointi!: boolean

    // Joko arviointi tai itsearviointi
    form: SuoritusarviointiForm = {
      vaativuustaso: null,
      arvioitavatKokonaisuudet: null,
      sanallinenArviointi: null,
      arviointityokalut: [],
      arviointityokaluVastaukset: [],
      arviointiPerustuu: null,
      muuPeruste: null,
      perustuuMuuhun: false,
      keskenerainen: false
    }
    vaativuustasot = vaativuustasot
    arviointiasteikonTasot: ArviointiasteikonTaso[] = []
    arviointityokalut: Arviointityokalu[] = []
    addedFiles: File[] = []
    newAsiakirjatMapped: Asiakirja[] = []
    deletedAsiakirjat: Asiakirja[] = []
    params = {
      saving: false
    }
    arviointiperusteet = [
      {
        text: this.$t('arviointi-perustuu-kirjallinen'),
        value: ArvioinninPerustuminen.KIRJALLINEN
      },
      {
        text: this.$t('arviointi-perustuu-etayhteys'),
        value: ArvioinninPerustuminen.ETA
      },
      {
        text: this.$t('arviointi-perustuu-muu'),
        value: ArvioinninPerustuminen.MUU
      }
    ]
    fileUploadTexts: FileUploadText[] = [
      {
        text: this.$t('voit-myos-lisata-valmiin-arvioinnin') as string,
        isLink: false,
        link: '',
        linkType: null
      },
      {
        text: this.$t('liitteena') as string,
        isLink: true,
        link: '',
        linkType: 'upload'
      },
      {
        text: this.$t('erillisessa-nakymassa-voit') as string,
        isLink: false,
        link: '',
        linkType: null
      },
      {
        text: this.$t('tutustua-arviointityokaluihin') as string,
        isLink: true,
        link: 'arviointityokalu-esittely',
        linkType: 'navigation'
      }
    ]
    isArviointityokalutModalOpen = false
    previousArviointityokaluCount = 0
    canValidate = false

    async mounted() {
      this.arviointiasteikonTasot = this.value.arviointiasteikko.tasot
      if (this.itsearviointi) {
        this.form = {
          vaativuustaso: vaativuustasot.find(
            (taso) => taso.arvo === this.value.itsearviointiVaativuustaso
          ),
          arvioitavatKokonaisuudet: this.value.arvioitavatKokonaisuudet.map((k) => {
            return {
              ...k,
              arviointiasteikonTaso: this.arviointiasteikonTasot.find(
                (asteikonTaso) => asteikonTaso.taso === k.itsearviointiArviointiasteikonTaso
              )
            }
          }),
          sanallinenArviointi: this.value.sanallinenItsearviointi,
          arviointityokaluVastaukset: this.value.arviointityokaluVastaukset,
          keskenerainen: false
        }
      } else {
        this.form = {
          ...this.value,
          vaativuustaso: vaativuustasot.find((taso) => taso.arvo === this.value.vaativuustaso),
          arvioitavatKokonaisuudet: this.value.arvioitavatKokonaisuudet.map((k) => {
            return {
              ...k,
              arviointiasteikonTaso: this.arviointiasteikonTasot.find(
                (asteikonTaso) => asteikonTaso.taso === k.arviointiasteikonTaso
              )
            }
          }),
          sanallinenArviointi: this.value.sanallinenArviointi,
          arviointityokalut: this.value.arviointityokalut,
          arviointiPerustuu:
            this.value.arviointiPerustuu === ArvioinninPerustuminen.LASNA
              ? null
              : this.value.arviointiPerustuu,
          muuPeruste: this.value.muuPeruste,
          perustuuMuuhun:
            this.value.arviointiPerustuu !== null &&
            this.value.arviointiPerustuu !== ArvioinninPerustuminen.LASNA,
          arviointityokaluVastaukset: this.value.arviointityokaluVastaukset,
          keskenerainen: false
        }
        this.arviointityokalut = (
          (await axios.get(`/arviointityokalut`)).data as Arviointityokalu[]
        )?.sort((a, b) => {
          if (a.nimi === 'Muu') {
            return 1
          } else {
            return sortByAsc(a.nimi, b.nimi)
          }
        })
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    get muuValittu() {
      return this.form.arviointiPerustuu === ArvioinninPerustuminen.MUU
    }

    get arviointiAsteikonNimi() {
      return this.value.arviointiasteikko.nimi === ArviointiasteikkoTyyppi.EPA
        ? this.$t('luottamuksen-taso')
        : this.$t('etappi')
    }

    get asiakirjatTableItems() {
      return (
        this.itsearviointi ? this.value.itsearviointiAsiakirjat : this.value.arviointiAsiakirjat
      )
        .filter((a) => !this.deletedAsiakirjat.includes(a))
        .concat(this.newAsiakirjatMapped)
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateArvioitavaKokonaisuusState(index: number) {
      const { $dirty, $error } = this.$v.form?.arvioitavatKokonaisuudet?.$each[index]
        ?.arviointiasteikonTaso as Validation
      return $dirty ? ($error ? false : null) : null
    }

    get pakollisetVastauksetValid() {
      if (!this.canValidate) return true
      if (!this.form.arviointityokalut) return true
      return this.form.arviointityokalut.every((tool) =>
        tool.kysymykset.every((kysymys) => {
          if (!kysymys.pakollinen) return true
          if (!this.form.arviointityokaluVastaukset) return false
          const vastaus = this.form.arviointityokaluVastaukset.find(
            (v) => v.arviointityokaluKysymysId === kysymys.id
          )
          return vastaus && (vastaus.tekstiVastaus || vastaus.valittuVaihtoehtoId)
        })
      )
    }

    onArviointiFileAdded(files: File[]) {
      const file = files[0]
      this.newAsiakirjatMapped.push(mapFile(file))
      this.addedFiles.push(file)
    }

    onArviointiFileDeleted(asiakirja: Asiakirja) {
      if (asiakirja.id) {
        this.deletedAsiakirjat = [asiakirja, ...this.deletedAsiakirjat]
      } else {
        this.addedFiles = this.addedFiles?.filter((file) => file.name !== asiakirja.nimi)
        this.newAsiakirjatMapped = this.newAsiakirjatMapped?.filter(
          (a) => a.nimi !== asiakirja.nimi
        )
      }
    }

    onSubmit(validate = true) {
      if (validate) {
        this.canValidate = true
        this.$v.form.$touch()
        if (this.$v.form.$anyError) {
          return
        }
        if (!this.pakollisetVastauksetValid) {
          return
        }
      }
      if (this.itsearviointi) {
        const submitData = {
          suoritusarviointi: {
            ...this.value,
            arvioitavatKokonaisuudet: this.form.arvioitavatKokonaisuudet?.map((k) => {
              return {
                ...k,
                itsearviointiArviointiasteikonTaso: (
                  k.arviointiasteikonTaso as ArviointiasteikonTaso
                ).taso,
                arviointiasteikonTaso: null
              }
            }),
            itsearviointiVaativuustaso: this.form.vaativuustaso?.arvo,
            sanallinenItsearviointi: this.form.sanallinenArviointi,
            arviointiasteikko: null,
            arviointiAsiakirjat: null,
            itsearviointiAsiakirjat: null,
            keskenerainen: this.form.keskenerainen
          },
          addedFiles: this.addedFiles,
          deletedAsiakirjaIds: this.deletedAsiakirjat.map((asiakirja) => asiakirja.id)
        }
        this.$emit('submit', submitData, this.params)
      } else {
        const submitData = {
          suoritusarviointi: {
            ...this.value,
            arvioitavatKokonaisuudet:
              this.form.arvioitavatKokonaisuudet?.map((k) => {
                return {
                  ...k,
                  arviointiasteikonTaso:
                    (k.arviointiasteikonTaso as ArviointiasteikonTaso)?.taso ?? null,
                  itsearviointiArviointiasteikonTaso: null
                }
              }) ?? [],
            vaativuustaso: this.form.vaativuustaso?.arvo,
            sanallinenArviointi: this.form.sanallinenArviointi,
            arviointityokalut: this.form.arviointityokalut,
            arviointityokaluVastaukset: this.form.arviointityokaluVastaukset,
            arviointiPerustuu: this.form.perustuuMuuhun
              ? this.form.arviointiPerustuu
              : ArvioinninPerustuminen.LASNA,
            muuPeruste: this.muuValittu ? this.form.muuPeruste : null,
            arviointiAsiakirjat: null,
            itsearviointiAsiakirjat: null,
            arviointiasteikko: null,
            keskenerainen: this.form.keskenerainen
          },
          addedFiles: this.addedFiles,
          deletedAsiakirjaIds: this.deletedAsiakirjat.map((asiakirja) => asiakirja.id)
        }
        this.$emit('submit', submitData, this.params)
      }
    }

    get asiakirjaDataEndpointUrl() {
      return this.value.id
        ? `${resolveRolePath()}/suoritusarvioinnit/${this.value.id}/arviointi-liite`
        : ''
    }

    get linkki() {
      return `<a
                href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/elsa"
                target="_blank"
                rel="noopener noreferrer"
              >${this.$t('arviointi-liitetiedostot-kuvaus-linkki')}</a>`
    }

    arviointiasteikonTasoLabel(value: ArviointiasteikonTaso) {
      return `${value.taso} ${this.$t('arviointiasteikon-taso-' + value.nimi)}`
    }

    vaativuustasoLabel(value: Vaativuustaso) {
      return `${value.arvo} ${this.$t(value.nimi)}`
    }

    muuPerustePituus() {
      return this.form.muuPeruste != null ? this.form.muuPeruste.length : 0
    }

    get formattedArviointityokalut() {
      const grouped = this.arviointityokalut
        .filter((item) => item.id !== MUU_ARVIOINTITYOKALU_ID)
        .reduce<Record<string, { kategoria: string; nimi: Arviointityokalu[] }>>(
          (acc, arviointityokalu) => {
            const categoryName =
              arviointityokalu.kategoria?.nimi || (this.$t('ei-kategoriaa') as string)
            if (!acc[categoryName]) {
              acc[categoryName] = { kategoria: categoryName, nimi: [] }
            }
            acc[categoryName].nimi.push(arviointityokalu)
            return acc
          },
          {}
        )

      const muuArviointityokalu = this.arviointityokalut.find((item) => item.id === 9)
      if (muuArviointityokalu) {
        const muuCategoryName = this.$t('muu') as string
        if (!grouped[muuCategoryName]) {
          grouped[muuCategoryName] = { kategoria: muuCategoryName, nimi: [] }
        }
        grouped[muuCategoryName].nimi.push(muuArviointityokalu)
      }

      return Object.values(grouped)
    }

    get arviointityokalujaEiValittuna() {
      if (this.form.arviointityokalut && this.form.arviointityokalut.length === 0) return true
      return (
        this.form.arviointityokalut &&
        this.form.arviointityokalut.length === 1 &&
        this.form.arviointityokalut[0].id === MUU_ARVIOINTITYOKALU_ID
      )
    }

    arviointityokaluLabel(arviointityokalu: Arviointityokalu) {
      return arviointityokalu.nimi || ''
    }

    openArviointityokalutModal() {
      this.isArviointityokalutModalOpen = true
    }

    async lisaaArviointityokaluVastaukset(formData: SuoritusarviointiArviointityokaluVastaus[]) {
      this.form.arviointityokaluVastaukset = formData
      this.isArviointityokalutModalOpen = false
    }

    getKysymyksenVastaus(kysymysId: number | undefined, arviointityokaluId: number | undefined) {
      if (this.form.arviointityokaluVastaukset === undefined) {
        return null
      }
      const vastaus = this.form.arviointityokaluVastaukset.find(
        (v) =>
          v.arviointityokaluKysymysId === kysymysId && v.arviointityokaluId === arviointityokaluId
      )
      return vastaus || null
    }

    saveUnfinishedAndExit() {
      this.form.keskenerainen = true
      this.onSubmit(false)
    }

    get listattavatArviointityokalut() {
      if (!this.form.arviointityokalut) return []
      return this.form.arviointityokalut.filter((at) => at.id !== MUU_ARVIOINTITYOKALU_ID)
    }

    onArviointityokalutChange() {
      const at = this.listattavatArviointityokalut
      if (at.length > 0) {
        if (at.length > this.previousArviointityokaluCount) {
          this.openArviointityokalutModal()
        }
        this.previousArviointityokaluCount = at.length
      } else {
        this.previousArviointityokaluCount = 0
      }
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .border-bottom-none {
    ::v-deep table {
      border-bottom: none;
    }
  }

  ::v-deep {
    table {
      thead th {
        border: none;
      }
      th:first-child {
        padding-left: 0;
        border-left: none;
      }
      th:last-child {
        padding-right: 0;
        border-right: none;
      }
      tbody td,
      tbody th {
        vertical-align: middle;
      }
      tbody th {
        font-weight: $font-weight-500;
      }
      tbody td:first-child {
        border-left: none;
      }
      tbody td:last-child {
        border-right: none;
      }
    }

    .arviointi-table {
      border-bottom: none;

      th {
        width: 25%;
      }

      @include media-breakpoint-down(sm) {
        th {
          padding-left: 0;
        }
        tr {
          margin-top: 0.5rem !important;
        }
        tr,
        td {
          display: block;
          padding-left: 0 !important;
        }
      }
    }
  }

  .canmeds-container {
    max-width: 450px;
  }

  .kouluttaja-form-input {
    margin-top: 10px;
  }

  .arviointi-perustuu {
    margin-left: 20px;
  }

  .card-header-custom {
    color: #222222;
    background-color: white;
    cursor: pointer;
  }

  .card {
    border: 1px solid #e8e9ec;
    border-radius: 8px;
  }
</style>
