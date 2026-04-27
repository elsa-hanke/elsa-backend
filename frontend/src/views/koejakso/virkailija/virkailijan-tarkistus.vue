<template>
  <div class="col-lg-8 px-0">
    <b-breadcrumb :items="items" class="mb-0" />

    <b-container fluid>
      <h1 class="mb-0">{{ $t('virkailijan-tarkistus') }}</h1>
      <div v-if="!loading && vastuuhenkilonArvio">
        <b-row lg>
          <b-col>
            <div v-if="editable" class="mt-3">
              {{ $t('vastuuhenkilon-arvio-ingressi-virkailija') }}
              <b-alert :show="korjausehdotus != null" variant="danger" class="mt-3">
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-2" />
                  </em>
                  <div>
                    {{ $t('vastuuhenkilon-arvio-palautettu-aiemmin-korjattavaksi') }}
                    <span v-if="vastuuhenkilonArvio.virkailijanKorjausehdotus != null">
                      {{ $t('virkailijan-toimesta') }}
                    </span>
                    <span v-else>{{ $t('vastuuhenkilon-toimesta') }}</span>
                    <span class="d-block">
                      {{ $t('syy') }}&nbsp;
                      <span class="font-weight-500">{{ korjausehdotus }}</span>
                    </span>
                  </div>
                </div>
              </b-alert>
            </div>
            <b-alert :show="waitingForVastuuhenkilo" variant="dark" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                </em>
                <div>
                  {{ $t('koejakso-on-tarkistettu-ja-odottaa-vastuuhenkilon-hyvaksyntaa') }}
                  <div v-show="vastuuhenkilonArvio.lisatiedotVirkailijalta">
                    <p class="mb-2 mt-3">
                      <strong>{{ $t('lisatiedot-virkailijalta') }}:</strong>
                      {{ vastuuhenkilonArvio.lisatiedotVirkailijalta }}
                    </p>
                  </div>
                </div>
              </div>
            </b-alert>
            <b-alert :show="returned" variant="dark" class="mt-3">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
                </em>
                <div>
                  {{ $t('vastuuhenkilon-arvio-tila-palautettu-korjattavaksi-virkailija') }}
                  <span class="d-block">{{ $t('syy') }} {{ korjausehdotus }}</span>
                </div>
              </div>
            </b-alert>
            <b-alert variant="success" :show="accepted">
              <div class="d-flex flex-row">
                <em class="align-middle">
                  <font-awesome-icon :icon="['fas', 'check-circle']" class="mr-2" />
                </em>
                {{ $t('koejakso-on-hyvaksytty-vastuuhenkilo-erikoistuja') }}
              </div>
            </b-alert>
          </b-col>
        </b-row>
        <b-row v-if="!editable">
          <b-col>
            <elsa-button variant="primary" class="mr-3" :to="{ name: 'koejakso' }">
              {{ $t('palaa-koejaksoihin') }}
            </elsa-button>
          </b-col>
        </b-row>
        <hr />
        <erikoistuva-details
          :avatar="erikoistuvanAvatar"
          :name="erikoistuvanNimi"
          :erikoisala="vastuuhenkilonArvio.erikoistuvanErikoisala"
          :opiskelijatunnus="vastuuhenkilonArvio.erikoistuvanOpiskelijatunnus"
          :yliopisto="vastuuhenkilonArvio.erikoistuvanYliopisto"
          :show-birthdate="true"
        />
        <div class="table-responsive">
          <table
            class="table table-borderless border-0 table-sm erikoistuva-details-table"
            :summary="$t('koejakson-yhteenlaskettu-kesto')"
          >
            <tr class="sr-only">
              <th scope="col">{{ $t('kentta') }}</th>
              <th scope="col">{{ $t('arvo') }}</th>
            </tr>
            <tr>
              <th scope="row" style="width: 12rem" class="align-middle font-weight-500">
                {{ $t('koejakson-yhteenlaskettu-kesto') }}
              </th>
              <td class="pl-6">
                {{ koejaksonKesto() }}
              </td>
            </tr>
          </table>
        </div>
        <hr />
        <b-row>
          <b-col lg="4">
            <h5>{{ $t('sahkopostiosoite') }}</h5>
            <p>{{ vastuuhenkilonArvio.erikoistuvanSahkoposti }}</p>
          </b-col>
          <b-col lg="4">
            <h5>{{ $t('matkapuhelinnumero') }}</h5>
            <p>{{ vastuuhenkilonArvio.erikoistuvanPuhelinnumero }}</p>
          </b-col>
        </b-row>
        <hr class="mt-2" />
        <div>
          <b-row>
            <b-col>
              <h3>{{ $t('useampi-opinto-oikeus') }}</h3>
              <p>
                {{ $t('useampi-opinto-oikeus-suostumus-vastuuhenkilo') }}
                <label class="d-block">
                  <span
                    v-for="(opintooikeus, index) in vastuuhenkilonArvio.muutOpintooikeudet"
                    :key="index"
                  >
                    {{ opintooikeus.erikoisalaNimi }},
                    {{ $t(`yliopisto-nimi.${opintooikeus.yliopistoNimi}`) }}
                    <br />
                  </span>
                </label>
              </p>
            </b-col>
          </b-row>
        </div>
        <hr />
        <div>
          <b-row>
            <b-col v-if="!vastuuhenkilonArvio.koulutussopimusHyvaksytty">
              <h3>{{ $t('koulutussopimus') }}</h3>
              <p>{{ $t('vastuuhenkilon-arvio-koulutussopimus-varmistus-vastuuhenkilo') }}</p>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <h3>{{ $t('koulutussuunnitelma') }}</h3>
              <p>{{ $t('vastuuhenkilon-arvio-koulutussuunnitelma-varmistus') }}</p>
            </b-col>
          </b-row>
        </div>
        <hr />
        <div v-if="vastuuhenkilonArvio.koejaksonSuorituspaikat">
          <div
            v-for="(sp, index) in vastuuhenkilonArvio.koejaksonSuorituspaikat.tyoskentelyjaksot"
            :key="index"
          >
            <b-row>
              <b-col>
                <h3>{{ $t('koejakson-suorituspaikka') }}</h3>
              </b-col>
            </b-row>
            <b-row>
              <b-col>
                <h5>{{ $t('tyyppi') }}</h5>
                <p>
                  {{
                    displayTyoskentelypaikkaTyyppiLabel(
                      sp.tyoskentelypaikka.muuTyyppi,
                      sp.tyoskentelypaikka.tyyppi
                    )
                  }}
                </p>
              </b-col>
            </b-row>
            <b-row>
              <b-col>
                <h5>{{ $t('tyoskentelypaikka') }}</h5>
                <p>{{ sp.tyoskentelypaikka.nimi }}</p>
              </b-col>
            </b-row>
            <b-row>
              <b-col>
                <h5>{{ $t('kunta') }}</h5>
                <p>{{ sp.tyoskentelypaikka.kunta.abbreviation }}</p>
              </b-col>
            </b-row>
            <b-row>
              <b-col>
                <h5>{{ $t('tyoaika-taydesta-tyopaivasta-prosentti') }}</h5>
                <p>{{ sp.osaaikaprosentti }}%</p>
              </b-col>
            </b-row>
            <b-row>
              <b-col lg="4">
                <h5>{{ $t('alkamispaiva') }}</h5>
                <p>{{ $date(sp.alkamispaiva) }}</p>
              </b-col>
              <b-col lg="4">
                <h5>{{ $t('paattymispaiva') }}</h5>
                <p>{{ sp.paattymispaiva ? $date(sp.paattymispaiva) : '-' }}</p>
              </b-col>
            </b-row>
            <b-row>
              <b-col>
                <h5>{{ $t('liitetiedostot') }}</h5>
                <asiakirjat-content
                  :asiakirjat="sp.asiakirjat"
                  :sorting-enabled="false"
                  :pagination-enabled="false"
                  :enable-search="false"
                  :enable-delete="false"
                  :no-results-info-text="$t('ei-liitetiedostoja')"
                  :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrlTyoskentelyjakso"
                  :loading="loading"
                />
              </b-col>
            </b-row>
            <b-row>
              <b-col>
                <h5>{{ $t('kaytannon-koulutus') }}</h5>
                <p>
                  {{ displayKaytannonKoulutus(sp.kaytannonKoulutus) }}
                  {{ sp.omaaErikoisalaaTukeva ? ': ' + sp.omaaErikoisalaaTukeva.nimi : '' }}
                </p>
              </b-col>
            </b-row>
            <b-row v-if="sp.hyvaksyttyAiempaanErikoisalaan">
              <b-col>
                <h5>{{ $t('lisatiedot') }}</h5>
                <p>{{ $t('tyoskentelyjakso-on-aiemmin-hyvaksytty-toiselle-erikoisalalle') }}</p>
              </b-col>
            </b-row>
            <b-row v-if="sp.poissaolot && sp.poissaolot.length > 0">
              <b-col>
                <h5>{{ $t('poissaolot') }}</h5>
                <elsa-poissaolot-display :poissaolot="sp.poissaolot" />
              </b-col>
            </b-row>
            <hr />
          </div>
        </div>
        <div v-if="vastuuhenkilonArvio.aloituskeskustelu">
          <b-row>
            <b-col>
              <h3>{{ $t('aloituskeskustelu-kouluttaja') }}</h3>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <b-alert variant="dark" show>
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  </em>
                  <span>{{ $t('aloituskeskustelu-tila-hyvaksytty') }}</span>
                </div>
              </b-alert>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <h5>{{ $t('koejakson-suorituspaikka') }}</h5>
              <p>{{ vastuuhenkilonArvio.aloituskeskustelu.koejaksonSuorituspaikka }}</p>
            </b-col>
          </b-row>
          <b-row v-if="vastuuhenkilonArvio.aloituskeskustelu.koejaksonToinenSuorituspaikka">
            <b-col>
              <h5>{{ $t('koejakson-toinen-suorituspaikka') }}</h5>
              <p>{{ vastuuhenkilonArvio.aloituskeskustelu.koejaksonToinenSuorituspaikka }}</p>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('koejakson-alkamispäivä') }}</h5>
              <p>{{ $date(vastuuhenkilonArvio.aloituskeskustelu.koejaksonAlkamispaiva) }}</p>
            </b-col>
            <b-col lg="4">
              <h5>{{ $t('koejakson-päättymispäivä') }}</h5>
              <p>{{ $date(vastuuhenkilonArvio.aloituskeskustelu.koejaksonPaattymispaiva) }}</p>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <h5>{{ $t('koejakso-suoritettu-kokoaikatyössä') }}</h5>
              <p v-if="vastuuhenkilonArvio.aloituskeskustelu.suoritettuKokoaikatyossa">
                {{ $t('kylla') }}
              </p>
              <p v-else>
                {{ $t('koejakso-suoritettu-osaaikatyossa') }}:
                {{ vastuuhenkilonArvio.aloituskeskustelu.tyotunnitViikossa
                }}{{ $t('tuntia-viikossa') }}
              </p>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <h5>{{ $t('koejakso-osaamistavoitteet') }}</h5>
              <p>{{ vastuuhenkilonArvio.aloituskeskustelu.koejaksonOsaamistavoitteet }}</p>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('lahikouluttaja') }}</h5>
              <p>{{ vastuuhenkilonArvio.aloituskeskustelu.lahikouluttaja.nimi }}</p>
            </b-col>
            <b-col lg="4">
              <h5>{{ $t('lahiesimies-tai-muu') }}</h5>
              <p>{{ vastuuhenkilonArvio.aloituskeskustelu.lahiesimies.nimi }}</p>
            </b-col>
          </b-row>
        </div>
        <hr />
        <div v-if="vastuuhenkilonArvio.valiarviointi">
          <b-row>
            <b-col>
              <h3>{{ $t('soveltuvuus-erikoisalalle-valiarvioinnin-perusteella') }}</h3>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <b-alert variant="dark" show>
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  </em>
                  <span>{{ $t('valiarviointi-tila-hyvaksytty') }}</span>
                </div>
              </b-alert>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('lahikouluttaja') }}</h5>
              <p>{{ vastuuhenkilonArvio.valiarviointi.lahikouluttaja.nimi }}</p>
            </b-col>
            <b-col lg="4">
              <h5>{{ $t('lahiesimies-tai-muu') }}</h5>
              <p>{{ vastuuhenkilonArvio.valiarviointi.lahiesimies.nimi }}</p>
            </b-col>
          </b-row>
        </div>
        <hr />
        <div
          v-if="
            vastuuhenkilonArvio.valiarviointi &&
            vastuuhenkilonArvio.kehittamistoimenpiteet &&
            !vastuuhenkilonArvio.valiarviointi.edistyminenTavoitteidenMukaista
          "
        >
          <b-row>
            <b-col>
              <h3>{{ $t('kehittamistoimenpiteet-otsikko') }}</h3>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <b-alert variant="dark" show>
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  </em>
                  <span>{{ $t('kehittamistoimenpiteet-tila-hyvaksytty') }}</span>
                </div>
              </b-alert>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('lahikouluttaja') }}</h5>
              <p>{{ vastuuhenkilonArvio.kehittamistoimenpiteet.lahikouluttaja.nimi }}</p>
            </b-col>
            <b-col lg="4">
              <h5>{{ $t('lahiesimies-tai-muu') }}</h5>
              <p>{{ vastuuhenkilonArvio.kehittamistoimenpiteet.lahiesimies.nimi }}</p>
            </b-col>
          </b-row>
          <hr />
        </div>
        <div v-if="vastuuhenkilonArvio.loppukeskustelu">
          <b-row>
            <b-col>
              <h3>{{ $t('loppukeskustelu') }}</h3>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <b-alert variant="dark" show>
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success mr-2" />
                  </em>
                  <span>{{ $t('loppukeskustelu-tila-hyvaksytty') }}</span>
                </div>
              </b-alert>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('lahikouluttaja') }}</h5>
              <p>{{ vastuuhenkilonArvio.loppukeskustelu.lahikouluttaja.nimi }}</p>
            </b-col>
            <b-col lg="4">
              <h5>{{ $t('lahiesimies-tai-muu') }}</h5>
              <p>{{ vastuuhenkilonArvio.loppukeskustelu.lahiesimies.nimi }}</p>
            </b-col>
          </b-row>
        </div>
        <hr />
        <elsa-form-group :label="$t('virkailijan-koejakso-yhteenveto')">
          <ElsaTextEditor
            v-if="editable"
            v-model="vastuuhenkilonArvio.virkailijanYhteenveto"
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
          <div
            v-else
            v-safe-html="virkailijanYhteenvetoSanitized"
            class="editor-readonly"
            style="white-space: normal"
          ></div>
        </elsa-form-group>
        <elsa-form-group :label="$t('liitetiedostot')">
          <asiakirjat-content
            v-if="vastuuhenkilonArvio.asiakirjat && vastuuhenkilonArvio.asiakirjat.length > 0"
            :asiakirjat="vastuuhenkilonArvio.asiakirjat"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :show-info-if-empty="false"
            :enable-delete="false"
            :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
          />
          <b-alert v-else variant="dark" show>
            <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
            <span>
              {{ $t('ei-liitetiedostoja') }}
            </span>
          </b-alert>
        </elsa-form-group>
        <hr />
        <div v-if="vastuuhenkilonArvio.vastuuhenkilo">
          <b-row>
            <b-col>
              <h3>{{ $t('erikoisala-vastuuhenkilö') }}</h3>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <h5>{{ $t('erikoisala-vastuuhenkilö-label') }}</h5>
              <p>
                {{ vastuuhenkilonArvio.vastuuhenkilo.nimi }}
                {{
                  vastuuhenkilonArvio.vastuuhenkilo.nimike
                    ? ', ' + vastuuhenkilonArvio.vastuuhenkilo.nimike
                    : ''
                }}
              </p>
            </b-col>
          </b-row>
        </div>
        <hr />
        <div v-if="vastuuhenkilonArvio.virkailija && vastuuhenkilonArvio.virkailija.kuittausaika">
          <b-row>
            <b-col>
              <h3>{{ $t('tarkistanut') }}</h3>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('paivays') }}</h5>
              <p>{{ $date(vastuuhenkilonArvio.virkailija.kuittausaika) }}</p>
            </b-col>
            <b-col lg="4">
              <h5>{{ $t('nimi-ja-nimike') }}</h5>
              <p>
                {{ vastuuhenkilonArvio.virkailija.nimi + ' '
                }}{{
                  vastuuhenkilonArvio.virkailija.nimike
                    ? ', ' + vastuuhenkilonArvio.virkailija.nimike
                    : ''
                }}
              </p>
            </b-col>
          </b-row>
          <hr />
        </div>
        <div v-if="vastuuhenkilonArvio.koejaksoHyvaksytty">
          <b-row>
            <b-col>
              <h3>{{ $t('arvio-koejaksosta') }}</h3>
            </b-col>
          </b-row>
          <b-row>
            <b-col lg="4">
              <h5>{{ $t('koejakso-on') }}</h5>
              <p>{{ vastuuhenkilonArvio.koejaksoHyvaksytty ? $t('hyvaksytty') : $t('hylatty') }}</p>
            </b-col>
          </b-row>
          <hr />
        </div>
        <div v-if="accepted">
          <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
          <hr />
        </div>
        <div v-if="editable">
          <b-row>
            <b-col class="text-right">
              <elsa-button
                variant="back"
                class="ml-1 mr-3 d-block d-md-inline-block d-lg-block d-xl-inline-block text-left"
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
                v-if="!loading"
                :loading="buttonStates.primaryButtonLoading"
                variant="primary"
                class="my-2 d-block d-md-inline-block d-lg-block d-xl-inline-block"
                @click="onValidateAndConfirm('confirm-sign')"
              >
                {{ $t('hyvaksy-laheta') }}
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
      id="confirm-sign"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :text="$t('lahetyksen-jalkeen-vastuuhenkilo-hyvaksynta')"
      :submit-text="$t('hyvaksy-laheta')"
      @submit="onSend"
    >
      <template #modal-content>
        <elsa-form-group v-if="vastuuhenkilonArvio" :label="$t('lisatiedot-vastuuhenkilolle')">
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="vastuuhenkilonArvio.lisatiedotVirkailijalta"
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
  </div>
</template>

<script lang="ts">
  import { intervalToDuration, formatDuration, isBefore, isAfter } from 'date-fns'
  import { fi, sv, enUS } from 'date-fns/locale'
  import DOMPurify from 'dompurify'
  import Component from 'vue-class-component'
  import { Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'

  import { getVastuuhenkilonArvio, putVastuuhenkilonArvio } from '@/api/virkailija'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPoissaolotDisplay from '@/components/poissaolot-display/poissaolot-display.vue'
  import ElsaTextEditor from '@/components/text-editor/text-editor.vue'
  import {
    KoejaksonVaiheHyvaksynta,
    KoejaksonVaiheButtonStates,
    VastuuhenkilonArvioLomake
  } from '@/types'
  import { KaytannonKoulutusTyyppi, LomakeTilat, TyoskentelyjaksoTyyppi } from '@/utils/constants'
  import { daysBetweenDates } from '@/utils/date'
  import { checkCurrentRouteAndRedirect } from '@/utils/functions'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import {
    tyoskentelypaikkaTyyppiLabel,
    tyoskentelyjaksoKaytannonKoulutusLabel
  } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      ErikoistuvaDetails,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaButton,
      ElsaPoissaolotDisplay,
      ElsaConfirmationModal,
      ElsaReturnToSenderModal,
      KoejaksonVaiheHyvaksynnat,
      AsiakirjatContent,
      ElsaTextEditor
    }
  })
  export default class VirkailijanTarkistus extends Mixins(validationMixin) {
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
        text: this.$t('virkailijan-tarkistus'),
        active: true
      }
    ]

    koejaksoHyvaksyttyVaihtoehdot = [
      {
        text: this.$t('hyvaksytty'),
        value: true
      },
      {
        text: this.$t('hylatty'),
        value: false
      }
    ]

    buttonStates: KoejaksonVaiheButtonStates = {
      primaryButtonLoading: false,
      secondaryButtonLoading: false
    }
    vastuuhenkilonArvio: VastuuhenkilonArvioLomake | null = null
    loading = true

    get editable() {
      return this.vastuuhenkilonArvio?.tila === LomakeTilat.ODOTTAA_HYVAKSYNTAA
    }

    get waitingForVastuuhenkilo() {
      return this.vastuuhenkilonArvio?.tila === LomakeTilat.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
    }

    get returned() {
      return this.vastuuhenkilonArvio?.tila === LomakeTilat.PALAUTETTU_KORJATTAVAKSI
    }

    get accepted() {
      return this.vastuuhenkilonArvio?.tila === LomakeTilat.HYVAKSYTTY
    }

    get vastuuhenkilonArvioId() {
      return Number(this.$route.params.id)
    }

    get erikoistuvanAvatar() {
      return this.vastuuhenkilonArvio?.erikoistuvanAvatar
    }

    get erikoistuvanNimi() {
      return this.vastuuhenkilonArvio?.erikoistuvanNimi
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      if (this.vastuuhenkilonArvio?.erikoistuvanKuittausaika) {
        const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
          this,
          this.vastuuhenkilonArvio?.erikoistuvanNimi,
          this.vastuuhenkilonArvio?.erikoistuvanKuittausaika
        )
        const hyvaksyntaVastuuhenkilo = hyvaksynnatHelper.mapHyvaksyntaVastuuhenkilo(
          this.vastuuhenkilonArvio?.vastuuhenkilo ?? null
        ) as KoejaksonVaiheHyvaksynta

        return [hyvaksyntaVastuuhenkilo, hyvaksyntaErikoistuva].filter(
          (a): a is KoejaksonVaiheHyvaksynta => a !== null
        )
      }
      return []
    }

    get asiakirjaDataEndpointUrlTyoskentelyjakso() {
      return '/virkailija/koejakso/tyoskentelyjakso-liite'
    }

    get asiakirjaDataEndpointUrl() {
      return `/virkailija/koejakso/vastuuhenkilon-arvio/${this.vastuuhenkilonArvio?.id}/liite`
    }

    get virkailijanYhteenvetoSanitized(): string {
      const raw = this.vastuuhenkilonArvio?.virkailijanYhteenveto ?? ''
      return DOMPurify.sanitize(raw)
    }

    onValidateAndConfirm(modalId: string) {
      return this.$bvModal.show(modalId)
    }

    displayTyoskentelypaikkaTyyppiLabel(muu: string | null, tyyppi: TyoskentelyjaksoTyyppi) {
      return muu ? muu : tyoskentelypaikkaTyyppiLabel(this, tyyppi)
    }

    displayKaytannonKoulutus(value: KaytannonKoulutusTyyppi) {
      return tyoskentelyjaksoKaytannonKoulutusLabel(this, value)
    }

    yhdistaPoissaolot(data: VastuuhenkilonArvioLomake) {
      let obj = data

      obj.koejaksonSuorituspaikat?.tyoskentelyjaksot.forEach((tj) => {
        tj.poissaolot = obj.koejaksonSuorituspaikat?.keskeytykset?.filter(
          (keskeytys) => keskeytys.tyoskentelyjaksoId == tj.id
        )
      })
      return obj
    }

    koejaksonKesto() {
      const koejaksonAlkamispaiva =
        this.vastuuhenkilonArvio?.aloituskeskustelu?.koejaksonAlkamispaiva
      const koejaksonLoppumispaiva = this.vastuuhenkilonArvio?.loppukeskustelu
        ?.koejaksonPaattymispaiva
        ? this.vastuuhenkilonArvio?.loppukeskustelu?.koejaksonPaattymispaiva
        : this.vastuuhenkilonArvio?.aloituskeskustelu?.koejaksonPaattymispaiva

      if (!koejaksonAlkamispaiva || !koejaksonLoppumispaiva) {
        return null
      }

      const koejaksoAlku = new Date(koejaksonAlkamispaiva)
      const koejaksoLoppu = new Date(koejaksonLoppumispaiva)
      const keskeytykset = this.vastuuhenkilonArvio?.koejaksonSuorituspaikat?.keskeytykset
      const tyoskentelyjaksot = this.vastuuhenkilonArvio?.koejaksonSuorituspaikat?.tyoskentelyjaksot
      let keskeytyksetDays = 0
      let tyojaksoDays = 0
      for (const keskeytys of keskeytykset !== undefined ? keskeytykset : []) {
        if (
          isBefore(new Date(keskeytys.alkamispaiva), koejaksoLoppu) &&
          isAfter(new Date(keskeytys.paattymispaiva), koejaksoAlku)
        ) {
          keskeytyksetDays += daysBetweenDates(
            new Date(keskeytys.alkamispaiva) < koejaksoAlku
              ? koejaksoAlku
              : new Date(keskeytys.alkamispaiva),
            new Date(keskeytys.paattymispaiva) > koejaksoLoppu
              ? koejaksoLoppu
              : new Date(keskeytys.paattymispaiva)
          )
        }
      }

      const tyoskentelyjaksojenOsaAjat = []
      for (const tyoskentelyjakso of tyoskentelyjaksot !== undefined ? tyoskentelyjaksot : []) {
        const osaaika =
          (tyoskentelyjakso.osaaikaprosentti != null ? tyoskentelyjakso.osaaikaprosentti : 100) /
          100
        const alku = new Date(tyoskentelyjakso.alkamispaiva)
        const loppu = new Date(
          tyoskentelyjakso.paattymispaiva != null ? tyoskentelyjakso.paattymispaiva : new Date()
        )
        if (isBefore(alku, koejaksoLoppu) && isAfter(loppu, koejaksoAlku)) {
          tyoskentelyjaksojenOsaAjat.push(osaaika)
        }
      }

      const osaAikaProsentti =
        tyoskentelyjaksojenOsaAjat.length > 0
          ? tyoskentelyjaksojenOsaAjat.reduce((pv, cv) => pv + cv, 0) /
            tyoskentelyjaksojenOsaAjat.length
          : 1.0
      tyojaksoDays = daysBetweenDates(koejaksoAlku, koejaksoLoppu) * osaAikaProsentti

      return formatDuration(
        intervalToDuration({
          start: new Date(),
          end: new Date().setDate(new Date().getDate() + (tyojaksoDays - keskeytyksetDays))
        }),
        {
          format: ['years', 'months', 'days'],
          locale: this.getLocaleObj()
        }
      )
    }

    getLocaleObj() {
      switch (this.currentLocale) {
        case 'fi':
          return fi
        case 'sv':
          return sv
        case 'en':
          return enUS
      }
    }

    get currentLocale() {
      if (this.$i18n) {
        return this.$i18n.locale
      } else {
        return 'fi'
      }
    }

    async onSend() {
      try {
        this.buttonStates.primaryButtonLoading = true
        if (this.vastuuhenkilonArvio != null) {
          this.vastuuhenkilonArvio.virkailijanKorjausehdotus = null
          await putVastuuhenkilonArvio(this.vastuuhenkilonArvio)
          this.buttonStates.primaryButtonLoading = false
          checkCurrentRouteAndRedirect(this.$router, '/koejakso')
          toastSuccess(this, this.$t('virkailijan-tarkistus-lahetetty-onnistuneesti'))
        }
      } catch {
        toastFail(this, this.$t('virkailijan-tarkistus-lahetys-epaonnistui'))
      }
    }

    async onReturnToSender(korjausehdotus: string) {
      const yhteenveto = this.vastuuhenkilonArvio?.virkailijanYhteenveto ?? null

      const form: VastuuhenkilonArvioLomake = {
        ...(this.vastuuhenkilonArvio as VastuuhenkilonArvioLomake),
        virkailijanKorjausehdotus: korjausehdotus,
        virkailijanYhteenveto: yhteenveto
      }

      try {
        this.buttonStates.secondaryButtonLoading = true
        await putVastuuhenkilonArvio(form)
        this.buttonStates.primaryButtonLoading = false
        checkCurrentRouteAndRedirect(this.$router, '/koejakso')
        toastSuccess(this, this.$t('koejakso-palautettu-muokattavaksi'))
      } catch {
        toastFail(this, this.$t('koejakso-palautus-epaonnistui'))
      }
    }

    async mounted() {
      this.loading = true
      try {
        const { data } = await getVastuuhenkilonArvio(this.vastuuhenkilonArvioId)
        this.vastuuhenkilonArvio = data
        this.vastuuhenkilonArvio = this.yhdistaPoissaolot(this.vastuuhenkilonArvio)
        this.loading = false
      } catch {
        toastFail(this, this.$t('vastuuhenkilon-arvion-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'koejakso' })
      }
    }

    get korjausehdotus() {
      return this.vastuuhenkilonArvio?.virkailijanKorjausehdotus != null
        ? this.vastuuhenkilonArvio?.virkailijanKorjausehdotus
        : this.vastuuhenkilonArvio?.vastuuhenkilonKorjausehdotus
    }

    beforeDestroy() {
      this.editor?.destroy()
    }
  }
</script>
