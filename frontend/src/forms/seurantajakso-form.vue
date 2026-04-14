<template>
  <b-form @submit.stop.prevent="onSubmit">
    <b-row v-if="!uusiJakso">
      <b-col>
        <div class="seurantajakso-erikoistuva-details">
          <erikoistuva-details
            :avatar="seurantajakso.erikoistuvanAvatar"
            :name="seurantajakso.erikoistuvanNimi"
            :erikoisala="seurantajakso.erikoistuvanErikoisalaNimi"
            :opiskelijatunnus="seurantajakso.erikoistuvanOpiskelijatunnus"
            :yliopisto="seurantajakso.erikoistuvanYliopistoNimi"
            :show-birthdate="false"
          ></erikoistuva-details>
          <elsa-form-group
            :label="$t('seurantajakso')"
            label-cols-md="4"
            label-cols-xl="3"
            label-cols="12"
            class="align-items-center mb-md-0 ml-md-0 kouluttaja-form-input"
          >
            <template #default="{ uid }">
              <span v-if="form.alkamispaiva && form.paattymispaiva" :id="uid">
                {{ $date(form.alkamispaiva) }} - {{ $date(form.paattymispaiva) }}
              </span>
            </template>
          </elsa-form-group>
        </div>
      </b-col>
    </b-row>
    <b-row v-else>
      <b-col>
        <elsa-form-group :label="$t('seurantajakso')" class="mb-0">
          <template #default="{ uid }">
            <div :id="uid" class="mb-1">
              <span v-if="form.alkamispaiva && form.paattymispaiva">
                {{ $date(form.alkamispaiva) }} - {{ $date(form.paattymispaiva) }}
              </span>
            </div>
          </template>
        </elsa-form-group>
      </b-col>
      <b-col>
        <elsa-button
          variant="outline-primary"
          class="mt-2 float-right uusihaku"
          @click.stop.prevent="onUusiHaku"
        >
          {{ $t('tee-uusi-haku') }}
        </elsa-button>
      </b-col>
    </b-row>

    <hr />

    <div v-if="seurantajaksonTiedot.koulutusjaksot.length > 0">
      <h3 v-b-toggle.tavoitteet-toggle class="mb-3">
        <span class="closed">
          <font-awesome-icon icon="caret-down" class="text-muted" />
        </span>
        <span class="open">
          <font-awesome-icon icon="caret-up" class="text-muted" />
        </span>
        {{ $t('suunnitellut-tavoitteet') }}
      </h3>
      <b-collapse id="tavoitteet-toggle" visible class="mb-4">
        <div v-for="koulutusjakso in seurantajaksonTiedot.koulutusjaksot" :key="koulutusjakso.id">
          <h4>{{ koulutusjakso.nimi }}</h4>
          <elsa-form-group :label="$t('tyoskentelyjaksot')">
            <template #default="{ uid }">
              <div
                v-for="tyoskentelyjakso in koulutusjakso.tyoskentelyjaksot"
                :id="uid"
                :key="tyoskentelyjakso.id"
              >
                <span class="mr-1">{{ tyoskentelyjakso.tyoskentelypaikka.nimi }}</span>
                <span>{{ ajankohtaLabel(tyoskentelyjakso) }}</span>
              </div>
            </template>
          </elsa-form-group>
          <elsa-form-group
            v-if="koulutusjakso.osaamistavoitteet.length > 0"
            :label="$t('osaamistavoitteet-omalla-erikoisalalla')"
          >
            <template #default="{ uid }">
              <b-badge
                v-for="osaamistavoite in koulutusjakso.osaamistavoitteet"
                :id="uid"
                :key="osaamistavoite.id"
                pill
                variant="light"
                class="font-weight-400 mr-2 mb-1 osaamistavoitteet"
              >
                {{ $i18n.locale == 'sv' ? osaamistavoite.nimiSv : osaamistavoite.nimi }}
              </b-badge>
            </template>
          </elsa-form-group>
          <elsa-form-group
            v-if="koulutusjakso.muutOsaamistavoitteet != null"
            :label="$t('muut-osaamistavoitteet')"
          >
            <template #default="{ uid }">
              <div :id="uid" class="mb-1">
                {{ koulutusjakso.muutOsaamistavoitteet }}
              </div>
            </template>
          </elsa-form-group>
          <hr />
        </div>
      </b-collapse>
    </div>

    <h3 v-b-toggle.arvioinnit-toggle class="mb-4">
      <span class="closed">
        <font-awesome-icon icon="caret-down" class="text-muted" />
      </span>
      <span class="open">
        <font-awesome-icon icon="caret-up" class="text-muted" />
      </span>
      {{ $t('arvioinnit') }} ({{ seurantajaksonTiedot.arviointienMaara }} {{ $t('kpl') }})
    </h3>

    <b-collapse id="arvioinnit-toggle" visible class="mb-4">
      <div v-for="(kategoria, index) in seurantajaksonTiedot.arvioinnit" :key="index">
        <div v-if="kategoria.nimi != null">
          <h4>{{ kategoria.nimi }}</h4>
          <hr class="mt-0 mb-0" />
        </div>
        <div
          v-for="(oa, kokonaisuusIndex) in kategoria.arvioitavatKokonaisuudet"
          :key="kokonaisuusIndex"
        >
          <p class="font-weight-500 pt-2 mb-1">{{ oa.nimi }}</p>

          <b-card-group v-if="!$screen.sm" class="mt-2" deck>
            <b-card
              v-for="(arviointi, arviointiIndex) in oa.arvioinnit"
              :key="`arviointi-${arviointiIndex}`"
              class="mt-2 border"
            >
              <b-card-text>
                <h5 class="mb-3">{{ arviointi.arvioitavaTapahtuma }}</h5>
                <dl class="mb-0">
                  <dt class="text-uppercase text-size-sm font-weight-400">
                    {{ $t('luottamuksen-taso') }}
                  </dt>
                  <dd class="mb-4">
                    <elsa-arviointiasteikon-taso
                      v-if="arviointi.arviointiasteikonTaso"
                      :value="arviointi.arviointiasteikonTaso"
                      :tasot="arviointi.arviointiasteikko.tasot"
                    />
                  </dd>
                  <dt class="text-uppercase text-size-sm font-weight-400">
                    {{ $t('suorituspaiva') }}
                  </dt>
                  <dd class="mb-0">
                    <elsa-button
                      variant="link"
                      class="pl-0"
                      @click="showArviointi(arviointi.suoritusarviointiId)"
                    >
                      {{ $date(arviointi.tapahtumanAjankohta) }}
                    </elsa-button>
                  </dd>
                </dl>
              </b-card-text>
            </b-card>
          </b-card-group>
          <b-table-simple v-else small fixed class="mb-4 arvioinnit-table" stacked="md" responsive>
            <b-thead>
              <b-tr class="text-size-sm">
                <b-th scope="col" style="width: 35%" class="text-uppercase">
                  {{ $t('tapahtuma') }}
                </b-th>
                <b-th scope="col" style="width: 50%" class="text-uppercase">
                  {{ $t('luottamuksen-taso') }}
                </b-th>
                <b-th scope="col" class="text-uppercase">
                  {{ $t('suorituspaiva') }}
                </b-th>
              </b-tr>
            </b-thead>
            <b-tbody>
              <b-tr
                v-for="(arviointi, arviointiIndex) in oa.arvioinnit"
                :key="`arviointi-${arviointiIndex}`"
              >
                <b-td :stacked-heading="$t('tapahtuma')">
                  {{ arviointi.arvioitavaTapahtuma }}
                </b-td>
                <b-td :stacked-heading="$t('luottamuksen-taso')">
                  <elsa-arviointiasteikon-taso
                    v-if="arviointi.arviointiasteikonTaso"
                    :value="arviointi.arviointiasteikonTaso"
                    :tasot="arviointi.arviointiasteikko.tasot"
                  />
                </b-td>
                <b-td :stacked-heading="$t('suorituspaiva')">
                  <elsa-button
                    variant="link"
                    class="pl-0"
                    @click="showArviointi(arviointi.suoritusarviointiId)"
                  >
                    {{ $date(arviointi.tapahtumanAjankohta) }}
                  </elsa-button>
                </b-td>
              </b-tr>
            </b-tbody>
          </b-table-simple>
        </div>
      </div>
    </b-collapse>

    <h3 v-b-toggle.suoritemerkinnat-toggle class="mb-4">
      <span class="closed">
        <font-awesome-icon icon="caret-down" class="text-muted" />
      </span>
      <span class="open">
        <font-awesome-icon icon="caret-up" class="text-muted" />
      </span>
      {{ $t('suoritemerkinnat') }} ({{ seurantajaksonTiedot.suoritemerkinnatMaara }}
      {{ $t('kpl') }})
    </h3>

    <b-collapse id="suoritemerkinnat-toggle" visible class="mb-4">
      <b-card-group
        v-if="!$screen.sm && seurantajaksonTiedot.suoritemerkinnat.length > 0"
        class="mt-2"
        deck
      >
        <template
          v-for="(
            seurantajaksonSuoritemerkinta, seurantajaksonSuoritemerkintaIndex
          ) in seurantajaksonTiedot.suoritemerkinnat"
        >
          <b-card
            v-for="(suoritemerkinta, index) in seurantajaksonSuoritemerkinta.suoritemerkinnat"
            :key="`suoritemerkinta-${seurantajaksonSuoritemerkintaIndex}-${index}`"
            class="mt-2 border"
          >
            <b-card-text>
              <h5 class="mb-3">{{ seurantajaksonSuoritemerkinta.suorite }}</h5>
              <dl class="mb-0">
                <dt class="text-uppercase text-size-sm font-weight-400">
                  {{ $t('luottamuksen-taso') }}
                </dt>
                <dd class="mb-4">
                  <elsa-arviointiasteikon-taso
                    v-if="
                      suoritemerkinta.arviointiasteikonTaso && suoritemerkinta.arviointiasteikko
                    "
                    :value="suoritemerkinta.arviointiasteikonTaso"
                    :tasot="suoritemerkinta.arviointiasteikko.tasot"
                  />
                </dd>
                <dt class="text-uppercase text-size-sm font-weight-400">
                  {{ $t('suorituspaiva') }}
                </dt>
                <dd class="mb-0">
                  <elsa-button
                    variant="link"
                    class="pl-0"
                    @click="showSuoritemerkinta(suoritemerkinta)"
                  >
                    {{ $date(suoritemerkinta.suorituspaiva) }}
                  </elsa-button>
                </dd>
              </dl>
            </b-card-text>
          </b-card>
        </template>
      </b-card-group>
      <b-table-simple
        v-if="$screen.sm && seurantajaksonTiedot.suoritemerkinnat.length > 0"
        small
        fixed
        class="mb-4 suoritemerkinnat-table"
        stacked="md"
        responsive
      >
        <b-thead>
          <b-tr class="text-size-sm">
            <b-th scope="col" style="width: 35%" class="text-uppercase">
              {{ $t('suorite') }}
            </b-th>
            <b-th scope="col" style="width: 50%" class="text-uppercase">
              {{ $t('luottamuksen-taso') }}
            </b-th>
            <b-th scope="col" class="text-uppercase">
              {{ $t('suorituspaiva') }}
            </b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <template
            v-for="(
              seurantajaksonSuoritemerkinta, seurantajaksonSuoritemerkintaIndex
            ) in seurantajaksonTiedot.suoritemerkinnat"
          >
            <b-tr
              v-for="(suoritemerkinta, index) in seurantajaksonSuoritemerkinta.suoritemerkinnat"
              :key="`suoritemerkinta-${seurantajaksonSuoritemerkintaIndex}-${index}`"
            >
              <b-td
                v-if="index == 0"
                :stacked-heading="$t('suorite')"
                :rowspan="seurantajaksonSuoritemerkinta.suoritemerkinnat.length"
              >
                {{ seurantajaksonSuoritemerkinta.suorite }}
              </b-td>

              <b-td :stacked-heading="$t('luottamuksen-taso')">
                <elsa-arviointiasteikon-taso
                  v-if="suoritemerkinta.arviointiasteikonTaso && suoritemerkinta.arviointiasteikko"
                  :value="suoritemerkinta.arviointiasteikonTaso"
                  :tasot="suoritemerkinta.arviointiasteikko.tasot"
                />
              </b-td>
              <b-td :stacked-heading="$t('suorituspaiva')">
                <elsa-button
                  variant="link"
                  class="pl-0"
                  @click="showSuoritemerkinta(suoritemerkinta)"
                >
                  {{ $date(suoritemerkinta.suorituspaiva) }}
                </elsa-button>
              </b-td>
            </b-tr>
          </template>
        </b-tbody>
      </b-table-simple>
    </b-collapse>

    <h3 v-b-toggle.teoriakoulutukset-toggle class="mb-5">
      <span class="closed">
        <font-awesome-icon icon="caret-down" class="text-muted" />
      </span>
      <span class="open">
        <font-awesome-icon icon="caret-up" class="text-muted" />
      </span>
      {{ $t('teoriakoulutukset') }} ({{ seurantajaksonTiedot.teoriakoulutukset.length }}
      {{ $t('kpl') }})
    </h3>

    <b-collapse id="teoriakoulutukset-toggle" visible class="mb-4">
      <b-card-group
        v-if="!$screen.sm && seurantajaksonTiedot.teoriakoulutukset.length > 0"
        class="mt-2"
        deck
      >
        <b-card
          v-for="(koulutus, index) in seurantajaksonTiedot.teoriakoulutukset"
          :key="`teoriakoulutus-${index}`"
          class="mt-2 border"
        >
          <b-card-text>
            <h5 class="mb-3">{{ koulutus.koulutuksenNimi }}</h5>
            <dl class="mb-0">
              <dt class="text-uppercase text-size-sm font-weight-400">
                {{ $t('paikka') }}
              </dt>
              <dd class="mb-4">
                {{ koulutus.koulutuksenPaikka }}
              </dd>
              <dt class="text-uppercase text-size-sm font-weight-400">
                {{ $t('pvm') }}
              </dt>
              <dd class="mb-0">
                {{ $date(koulutus.alkamispaiva) }}
                <span v-if="koulutus.paattymispaiva != null">
                  -{{ $date(koulutus.paattymispaiva) }}
                </span>
              </dd>
              <template v-if="koulutus.erikoistumiseenHyvaksyttavaTuntimaara != null">
                <dt class="mt-4 text-uppercase text-size-sm font-weight-400">
                  {{ $t('tunnit') }}
                </dt>
                <dd class="mb-0">
                  {{ koulutus.erikoistumiseenHyvaksyttavaTuntimaara }}
                </dd>
              </template>
            </dl>
          </b-card-text>
        </b-card>
      </b-card-group>
      <b-table-simple
        v-if="$screen.sm && seurantajaksonTiedot.teoriakoulutukset.length > 0"
        fixed
        class="mb-4"
        stacked="md"
        responsive
      >
        <b-thead>
          <b-tr class="text-size-sm">
            <b-th scope="col" class="text-uppercase">
              {{ $t('koulutuksen-nimi') }}
            </b-th>
            <b-th scope="col" class="text-uppercase">
              {{ $t('paikka') }}
            </b-th>
            <b-th scope="col" class="text-uppercase">
              {{ $t('pvm') }}
            </b-th>
            <b-th scope="col" class="text-uppercase">
              {{ $t('tunnit') }}
            </b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <b-tr
            v-for="(koulutus, index) in seurantajaksonTiedot.teoriakoulutukset"
            :key="`teoriakoulutus-${index}`"
          >
            <b-td :stacked-heading="$t('koulutuksen-nimi')">
              {{ koulutus.koulutuksenNimi }}
            </b-td>

            <b-td :stacked-heading="$t('paikka')">{{ koulutus.koulutuksenPaikka }}</b-td>
            <b-td :stacked-heading="$t('pvm')">
              {{ $date(koulutus.alkamispaiva) }}
              <span v-if="koulutus.paattymispaiva != null">
                -{{ $date(koulutus.paattymispaiva) }}
              </span>
            </b-td>
            <b-td :stacked-heading="$t('tunnit')">
              {{ koulutus.erikoistumiseenHyvaksyttavaTuntimaara }}
            </b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
    </b-collapse>

    <h3 class="mb-3">{{ $t('erikoistujan-oma-arviointi') }}</h3>
    <div v-if="editing && $isErikoistuva()">
      <elsa-form-group :label="$t('oma-arviointi-seurantajaksolta')" :required="true">
        <template #default="{ uid }">
          <b-form-textarea
            :id="uid"
            v-model="form.omaArviointi"
            :state="validateState('omaArviointi')"
            rows="3"
            @input="$emit('skipRouteExitConfirm', false)"
          />
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
        <template #label-help>
          <elsa-popover>
            {{ $t('oma-arviointi-seurantajaksolta-ohje') }}
          </elsa-popover>
        </template>
      </elsa-form-group>
      <elsa-form-group :label="$t('lisahuomioita')">
        <template #default="{ uid }">
          <b-form-textarea
            :id="uid"
            v-model="form.lisahuomioita"
            rows="3"
            @input="$emit('skipRouteExitConfirm', false)"
          />
        </template>
        <template #label-help>
          <elsa-popover>
            {{ $t('lisahuomioita-ohje') }}
          </elsa-popover>
        </template>
      </elsa-form-group>
      <elsa-form-group :label="$t('seuraavan-jakson-tavoitteet')">
        <template #default="{ uid }">
          <b-form-textarea
            :id="uid"
            v-model="form.seuraavanJaksonTavoitteet"
            rows="3"
            @input="$emit('skipRouteExitConfirm', false)"
          />
        </template>
        <template #label-help>
          <elsa-popover>
            {{ $t('seuraavan-jakson-tavoitteet-ohje') }}
          </elsa-popover>
        </template>
      </elsa-form-group>
    </div>
    <div v-else>
      <elsa-form-group
        v-if="form.omaArviointi != null"
        :label="$t('oma-arviointi-seurantajaksolta')"
      >
        <template #default="{ uid }">
          <div :id="uid">{{ form.omaArviointi }}</div>
        </template>
      </elsa-form-group>
      <elsa-form-group v-if="form.lisahuomioita != null" :label="$t('lisahuomioita')">
        <template #default="{ uid }">
          <div :id="uid">{{ form.lisahuomioita }}</div>
        </template>
      </elsa-form-group>
      <elsa-form-group
        v-if="form.seuraavanJaksonTavoitteet != null"
        :label="$t('seuraavan-jakson-tavoitteet')"
      >
        <template #default="{ uid }">
          <div :id="uid">{{ form.seuraavanJaksonTavoitteet }}</div>
        </template>
      </elsa-form-group>
    </div>

    <hr />

    <b-row>
      <b-col lg="10">
        <h3 class="mb-3">{{ $t('seurantakeskustelun-osapuoli') }}</h3>
        <div v-if="uusiJakso">
          <p>
            {{ $t('valitse-arvioija-help') }}
            <b-link :to="{ name: 'profiili' }">{{ $t('profiilissasi') }}</b-link>
          </p>
          <elsa-form-group
            :label="$t('lahikouluttaja')"
            :add-new-enabled="true"
            :add-new-label="$t('lisaa-kouluttaja')"
            :required="true"
            @submit="onKouluttajaSubmit"
          >
            <template #modal-content="{ submit, cancel }">
              <kouluttaja-form :kouluttajat="kouluttajat" @submit="submit" @cancel="cancel" />
            </template>
            <template #default="{ uid }">
              <elsa-form-multiselect
                :id="uid"
                v-model="form.kouluttaja"
                :options="formattedKouluttajat"
                :state="validateState('kouluttaja')"
                label="nimi"
                track-by="nimi"
                @input="$emit('skipRouteExitConfirm', false)"
              >
                <template #option="{ option }">
                  <div v-if="option.nimi != null">{{ option.nimi }}</div>
                </template>
              </elsa-form-multiselect>
              <b-form-invalid-feedback :id="`${uid}-feedback`">
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </template>
          </elsa-form-group>
        </div>
        <div v-else>
          <elsa-form-group
            v-if="form.kouluttaja != null"
            class="mb-0"
            :label="$t('lahikouluttaja')"
          >
            <template #default="{ uid }">
              <div :id="uid">{{ form.kouluttaja.nimi }}</div>
            </template>
          </elsa-form-group>
        </div>
      </b-col>
    </b-row>

    <hr />

    <div v-if="form.kouluttajanArvio != null || (editing && !$isErikoistuva())">
      <h3 class="mb-3">{{ $t('lahikouluttajan-arviointi') }}</h3>
      <div v-if="editing && !$isErikoistuva()">
        <elsa-form-group
          :label="$t('edistyminen-osaamistavoitteiden-mukaista')"
          :required="true"
          class="mb-1"
        >
          <template #default="{ uid }">
            <b-form-radio-group
              :id="uid"
              v-model="form.edistyminenTavoitteidenMukaista"
              :options="edistyminenVaihtoehdot"
              :state="validateState('edistyminenTavoitteidenMukaista')"
              stacked
              @input="$emit('skipRouteExitConfirm', false)"
            ></b-form-radio-group>
            <b-form-invalid-feedback
              :id="`${uid}-feedback`"
              :state="validateState('edistyminenTavoitteidenMukaista')"
            >
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-form-group
          v-if="form.edistyminenTavoitteidenMukaista === false"
          :label="$t('huolenaiheet')"
          :required="true"
          class="mb-0 ml-4"
        >
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="form.huolenaiheet"
              :state="validateState('huolenaiheet')"
              rows="3"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-form-group
          :label="$t('lahikouluttajan-arviointi-jaksosta')"
          :required="true"
          class="mt-4"
        >
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="form.kouluttajanArvio"
              :state="validateState('kouluttajanArvio')"
              rows="3"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
          <template #label-help>
            <elsa-popover>
              {{ $t('lahikouluttajan-arviointi-jaksosta-ohje') }}
            </elsa-popover>
          </template>
        </elsa-form-group>
        <elsa-form-group :label="$t('erikoisalan-tyoskentelyvalmiudet')" class="mt-4">
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="form.erikoisalanTyoskentelyvalmiudet"
              rows="3"
              @input="$emit('skipRouteExitConfirm', false)"
            />
          </template>
          <template #label-help>
            <elsa-popover>
              {{ $t('erikoisalan-tyoskentelyvalmiudet-ohje') }}
            </elsa-popover>
          </template>
        </elsa-form-group>
        <elsa-form-group :label="$t('jatkotoimet-ja-raportointi')" class="mt-4">
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="form.jatkotoimetJaRaportointi"
              rows="3"
              @input="$emit('skipRouteExitConfirm', false)"
            />
          </template>
          <template #label-help>
            <elsa-popover>
              {{ $t('jatkotoimet-ja-raportointi-ohje') }}
            </elsa-popover>
          </template>
        </elsa-form-group>
      </div>
      <div v-else>
        <elsa-form-group :label="$t('edistyminen-osaamistavoitteiden-mukaista')">
          <template #default="{ uid }">
            <div :id="uid">
              {{ form.edistyminenTavoitteidenMukaista ? $t('kylla') : $t('ei-huolenaiheita-on') }}
            </div>
          </template>
        </elsa-form-group>
        <elsa-form-group v-if="form.huolenaiheet != null" :label="$t('huolenaiheet')">
          <template #default="{ uid }">
            <div :id="uid">{{ form.huolenaiheet }}</div>
          </template>
        </elsa-form-group>
        <elsa-form-group :label="$t('lahikouluttajan-arviointi-jaksosta')">
          <template #default="{ uid }">
            <div :id="uid">{{ form.kouluttajanArvio }}</div>
          </template>
        </elsa-form-group>
        <elsa-form-group
          v-if="form.erikoisalanTyoskentelyvalmiudet != null"
          :label="$t('erikoisalan-tyoskentelyvalmiudet')"
        >
          <template #default="{ uid }">
            <div :id="uid">{{ form.erikoisalanTyoskentelyvalmiudet }}</div>
          </template>
        </elsa-form-group>
        <elsa-form-group
          v-if="form.jatkotoimetJaRaportointi != null"
          :label="$t('jatkotoimet-ja-raportointi')"
        >
          <template #default="{ uid }">
            <div :id="uid">{{ form.jatkotoimetJaRaportointi }}</div>
          </template>
        </elsa-form-group>
      </div>

      <hr />
    </div>

    <div v-if="$isErikoistuva() || form.seurantakeskustelunYhteisetMerkinnat != null">
      <h3 id="seurantakeskustelun_merkinnat" class="mb-2">
        {{ $t('seurantakeskustelun-merkinnat') }}
      </h3>
      <div v-if="editing && $isErikoistuva()">
        <p class="mb-3">{{ $t('seurantakeskustelun-merkinnat-kuvaus-muokkaus') }}</p>
        <elsa-form-group
          :label="$t('yhteiset-merkinnat-keskustelusta-ja-jatkosuunnitelmista')"
          :required="!uusiJakso"
        >
          <template #default="{ uid }">
            <b-form-textarea
              :id="uid"
              v-model="form.seurantakeskustelunYhteisetMerkinnat"
              :state="validateState('seurantakeskustelunYhteisetMerkinnat')"
              :disabled="uusiJakso"
              rows="3"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <b-form-row>
          <elsa-form-group
            class="col-xs-12 col-sm-4 mb-2"
            :label="$t('seuraavan-keskustelun-ajankohta')"
          >
            <template #default="{ uid }">
              <elsa-form-datepicker
                :id="uid"
                ref="seuraavanKeskustelunAjankohta"
                :value.sync="form.seuraavanKeskustelunAjankohta"
                :initial-date="minSeuraavaKeskustelu"
                :disabled="uusiJakso"
                :min="minAlkamispaiva"
                :min-error-text="$t('paivamaara-ei-voi-olla-menneisyydessa')"
                :required="false"
                @input="$emit('skipRouteExitConfirm', false)"
              ></elsa-form-datepicker>
            </template>
          </elsa-form-group>
        </b-form-row>
      </div>
      <div v-else>
        <div v-if="form.seurantakeskustelunYhteisetMerkinnat != null">
          <elsa-form-group
            class="mt-3"
            :label="$t('yhteiset-merkinnat-keskustelusta-ja-jatkosuunnitelmista')"
          >
            <template #default="{ uid }">
              <div :id="uid">{{ form.seurantakeskustelunYhteisetMerkinnat }}</div>
            </template>
          </elsa-form-group>
          <elsa-form-group
            v-if="form.seuraavanKeskustelunAjankohta != null"
            :label="$t('seuraavan-keskustelun-ajankohta')"
          >
            <template #default="{ uid }">
              <div :id="uid">{{ $date(form.seuraavanKeskustelunAjankohta) }}</div>
            </template>
          </elsa-form-group>
        </div>
        <div v-else>
          <p class="mb-1">{{ $t('seurantakeskustelun-merkinnat-kuvaus-1') }}</p>
          <ul class="pl-5">
            <li class="font-weight-bold">{{ $t('seurantakeskustelun-merkinnat-kuvaus-2') }}</li>
            <li class="font-weight-bold">{{ $t('seurantakeskustelun-merkinnat-kuvaus-3') }}</li>
          </ul>
        </div>
      </div>

      <hr />
    </div>

    <div v-if="editing" class="d-flex flex-row-reverse flex-wrap">
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2 mb-2">
        {{ $t('tallenna-ja-laheta') }}
      </elsa-button>
      <elsa-button
        v-if="
          $isErikoistuva() &&
          seurantajakso.seurantakeskustelunYhteisetMerkinnat == null &&
          !uusiJakso
        "
        :loading="params.deleting"
        variant="outline-danger"
        class="mb-2"
        @click="onSeurantajaksoDelete"
      >
        {{ $t('poista-seurantajakso') }}
      </elsa-button>
      <elsa-button
        v-if="!$isErikoistuva() && form.seurantakeskustelunYhteisetMerkinnat != null"
        v-b-modal.return-to-sender
        variant="outline-primary"
        class="mb-2"
      >
        {{ $t('palauta-muokattavaksi') }}
      </elsa-button>
      <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
        {{ $t('peruuta') }}
      </elsa-button>
    </div>
    <div class="row">
      <elsa-form-error :active="$v.$anyError" />
    </div>

    <b-modal id="arviointi-modal" :title="$t('arviointi')" size="lg">
      <arviointi-form :value="selectedArviointi" :editing="false" />

      <template #modal-footer>
        <elsa-button variant="primary" @click="hideArviointiModal">
          {{ $t('sulje-arviointi') }}
        </elsa-button>
      </template>
    </b-modal>

    <b-modal id="suoritemerkinta-modal" :title="$t('suoritemerkinta')" size="lg">
      <suoritemerkinta-details :value="selectedSuoritemerkinta" />

      <template #modal-footer>
        <elsa-button variant="primary" @click="hideSuoritemerkintaModal">
          {{ $t('sulje-suoritemerkinta') }}
        </elsa-button>
      </template>
    </b-modal>

    <elsa-confirmation-modal
      id="confirm-modal"
      :title="$t('vahvista-lomakkeen-lahetys')"
      :submit-text="$t('tallenna-ja-laheta')"
      @submit="onSend"
    >
      <template #modal-content>
        <div v-if="!form.id" class="d-block">
          <p>{{ $t('vahvista-seurantajakson-lahetys-1') }}</p>
          <p class="mb-1">{{ $t('vahvista-seurantajakson-lahetys-2') }}</p>
          <ul>
            <li>{{ $t('vahvista-seurantajakson-lahetys-3') }}</li>
            <li>{{ $t('vahvista-seurantajakson-lahetys-4') }}</li>
          </ul>
        </div>
        <div v-else-if="$isErikoistuva()">
          {{ $t('vahvista-seurantajakson-yhteiset-merkinnat') }}
        </div>
        <div v-else-if="form.seurantakeskustelunYhteisetMerkinnat == null">
          {{ $t('vahvista-seurantajakson-arviointi') }}
        </div>
        <div v-else>
          {{ $t('vahvista-seurantajakson-hyvaksynta') }}
        </div>
      </template>
    </elsa-confirmation-modal>
    <elsa-return-to-sender-modal
      id="return-to-sender"
      :title="$t('palauta-erikoistuvalle-muokattavaksi')"
      @submit="returnToSender"
    />
  </b-form>
</template>

<script lang="ts">
  import axios, { AxiosError } from 'axios'
  import { BModal } from 'bootstrap-vue'
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { Validation, validationMixin } from 'vuelidate'
  import { required, requiredIf } from 'vuelidate/lib/validators'

  import { postLahikouluttaja } from '@/api/erikoistuva'
  import ElsaArviointiasteikonTaso from '@/components/arviointiasteikon-taso/arviointiasteikon-taso.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ErikoistuvaDetails from '@/components/erikoistuva-details/erikoistuva-details.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaReturnToSenderModal from '@/components/modal/return-to-sender-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import ArviointiForm from '@/forms/arviointi-form.vue'
  import KouluttajaForm from '@/forms/kouluttaja-form.vue'
  import TyoskentelyjaksoMixin from '@/mixins/tyoskentelyjakso'
  import store from '@/store'
  import {
    ElsaError,
    Kayttaja,
    Seurantajakso,
    SeurantajaksonTiedot,
    Suoritemerkinta,
    Suoritusarviointi,
    Tyoskentelyjakso
  } from '@/types'
  import { resolveRolePath } from '@/utils/apiRolePathResolver'
  import { formatList } from '@/utils/kouluttajaAndVastuuhenkiloListFormatter'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import { ajankohtaLabel } from '@/utils/tyoskentelyjakso'
  import SuoritemerkintaDetails from '@/views/suoritemerkinnat/suoritemerkinta-details.vue'

  @Component({
    components: {
      ArviointiForm,
      ElsaArviointiasteikonTaso,
      ElsaButton,
      ElsaConfirmationModal,
      ElsaFormDatepicker,
      ElsaFormError,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaPopover,
      ElsaReturnToSenderModal,
      ErikoistuvaDetails,
      KouluttajaForm,
      SuoritemerkintaDetails
    },
    validations() {
      const vm = this as SeurantajaksoForm
      return {
        form: {
          kouluttaja: {
            required
          },
          omaArviointi: {
            required
          },
          seurantakeskustelunYhteisetMerkinnat: {
            required: requiredIf(function () {
              return vm.$isErikoistuva() && !vm.uusiJakso
            })
          },
          edistyminenTavoitteidenMukaista: {
            required: requiredIf(function () {
              return !vm.$isErikoistuva()
            })
          },
          huolenaiheet: {
            required: requiredIf(function () {
              return !vm.$isErikoistuva() && vm.form.edistyminenTavoitteidenMukaista === false
            })
          },
          kouluttajanArvio: {
            required: requiredIf(function () {
              return !vm.$isErikoistuva()
            })
          }
        }
      }
    }
  })
  export default class SeurantajaksoForm extends Mixins(validationMixin, TyoskentelyjaksoMixin) {
    $refs!: {
      seuraavanKeskustelunAjankohta: ElsaFormDatepicker
    }

    @Prop({
      required: false,
      type: Object,
      default: () => ({
        osaamistavoitteet: [],
        muutOsaamistavoitteet: [],
        arvioinnit: [],
        arviointienMaara: 0,
        suoritemerkinnat: [],
        suoritemerkinnatMaara: 0,
        teoriakoulutukset: []
      })
    })
    seurantajaksonTiedot!: SeurantajaksonTiedot

    @Prop({
      required: false,
      default: () => ({
        alkamispaiva: null,
        paattymispaiva: null,
        koulutusjaksot: []
      })
    })
    seurantajakso!: Seurantajakso

    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: false, default: () => [] })
    kouluttajat?: Kayttaja[]

    form: Partial<Seurantajakso> = {
      koulutusjaksot: [],
      omaArviointi: null,
      lisahuomioita: null,
      seuraavanJaksonTavoitteet: null,
      kouluttaja: null,
      seurantakeskustelunYhteisetMerkinnat: null,
      seuraavanKeskustelunAjankohta: null
    }

    selectedArviointi: Suoritusarviointi | null = null
    selectedSuoritemerkinta: Suoritemerkinta | null = null

    params = {
      saving: false,
      deleting: false
    }

    edistyminenVaihtoehdot = [
      {
        text: this.$t('kylla'),
        value: true
      },
      {
        text: this.$t('ei-huolenaiheita-on'),
        value: false
      }
    ]
    childDataReceived = false

    async mounted() {
      this.form = {
        ...this.seurantajakso
      }
      this.childDataReceived = true
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as Validation
      return $dirty ? ($error ? false : null) : null
    }

    get formattedKouluttajat(): Kayttaja[] {
      return formatList(this, this.kouluttajat)
    }

    get uusiJakso() {
      return this.form.id == null
    }

    get minAlkamispaiva(): Date {
      return new Date()
    }

    get minSeuraavaKeskustelu(): Date {
      const now = new Date()
      return new Date(now.setMonth(now.getMonth() + 6))
    }

    get account() {
      return store.getters['auth/account']
    }

    async onKouluttajaSubmit(value: Kayttaja, params: { saving: boolean }, modal: BModal) {
      params.saving = true
      try {
        const kouluttaja = (await postLahikouluttaja(value)).data
        this.formattedKouluttajat.push(kouluttaja)
        this.form.kouluttaja = kouluttaja
        modal.hide('confirm')
        toastSuccess(this, this.$t('uusi-kouluttaja-lisatty'))
        await store.dispatch('erikoistuva/getKouluttajat')
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('uuden-kouluttajan-lisaaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('uuden-kouluttajan-lisaaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    async showArviointi(suoritusarviointiId: number) {
      this.selectedArviointi = (
        await axios.get(`${resolveRolePath()}/suoritusarvioinnit/${suoritusarviointiId}`)
      ).data
      this.$bvModal.show('arviointi-modal')
    }

    hideArviointiModal() {
      this.$bvModal.hide('arviointi-modal')
    }

    showSuoritemerkinta(suoritemerkinta: Suoritemerkinta) {
      this.selectedSuoritemerkinta = suoritemerkinta
      this.$bvModal.show('suoritemerkinta-modal')
    }

    hideSuoritemerkintaModal() {
      this.$bvModal.hide('suoritemerkinta-modal')
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.seuraavanKeskustelunAjankohta
          ? this.$refs.seuraavanKeskustelunAjankohta.validateForm()
          : true
      ]

      if (validations.includes(false)) {
        return
      }

      this.$bvModal.show('confirm-modal')
    }

    onSend() {
      this.$emit('submit', this.form, this.params)
    }

    onCancel() {
      this.$emit('cancel')
    }

    onUusiHaku() {
      this.$emit('uusiHaku')
    }

    onSeurantajaksoDelete() {
      this.$emit('delete', this.params)
    }

    returnToSender(korjausehdotus: string) {
      this.form.korjausehdotus = korjausehdotus
      this.$emit('submit', this.form, this.params)
    }

    ajankohtaLabel(tyoskentelyjakso: Tyoskentelyjakso) {
      return `(${ajankohtaLabel(this, tyoskentelyjakso)})`
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .suoritemerkinnat-table,
  .arvioinnit-table {
    td {
      padding-top: 0.25rem;
      padding-bottom: 0.25rem;
      vertical-align: middle;
    }
  }

  .osaamistavoitteet {
    font-size: $font-size-base;
    white-space: normal;
  }

  .collapsed {
    .open {
      display: none;
    }
  }

  .not-collapsed {
    .closed {
      display: none;
    }
  }

  .seurantajakso-erikoistuva-details::v-deep {
    .table-responsive {
      margin-bottom: 0rem;
    }
  }

  .card-body {
    padding-top: 0.75rem;
    padding-bottom: 0.75rem;
  }

  .uusihaku {
    width: 50%;
  }

  @include media-breakpoint-down(xs) {
    .uusihaku {
      width: 100%;
    }
  }
</style>
