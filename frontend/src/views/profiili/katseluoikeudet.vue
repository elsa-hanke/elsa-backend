<template>
  <div>
    <p>{{ $t('katseluoikeudet-kuvaus') }}</p>
    <b-row class="kouluttajaRow">
      <b-col sm="8">
        <elsa-form-group
          :label="$t('kouluttaja')"
          :add-new-enabled="true"
          :add-new-label="$t('lisaa-kouluttaja')"
          class="mb-2"
          @submit="onKouluttajaSubmit"
        >
          <template #modal-content="{ submit, cancel }">
            <kouluttaja-form :kouluttajat="kouluttajat" @submit="submit" @cancel="cancel" />
          </template>
          <template #default="{ uid }">
            <elsa-form-multiselect
              :id="uid"
              v-model="valittuKouluttaja"
              :options="filteredKouluttajat"
              :loading="isLoading"
              label="nimi"
              track-by="nimi"
            >
              <template #option="{ option }">
                <div v-if="option.nimi">{{ optionDisplayName(option) }}</div>
              </template>
            </elsa-form-multiselect>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-col>
      <b-col cols="4" class="pl-0 myonnaOikeusCol">
        <div class="mt-sm-45">
          <elsa-button
            type="submit"
            variant="primary"
            class="ml-2 text-nowrap"
            :disabled="!valittuKouluttaja"
            @click="myonnaOikeus"
          >
            {{ $t('myonna-oikeus') }}
          </elsa-button>
        </div>
      </b-col>
    </b-row>
    <div v-if="valtuutukset.length > 0">
      <b-card-group v-if="!$screen.sm" class="mt-4" deck>
        <b-card v-for="(valtuutus, index) in valtuutukset" :key="index" class="mt-2 border">
          <b-card-text>
            <h4>{{ valtuutus.valtuutettu.nimi }}</h4>
            <dl class="mb-0">
              <dt class="font-weight-500">{{ $t('sahkoposti') }}</dt>
              <dd>{{ valtuutus.valtuutettu.sahkoposti }}</dd>
              <dt class="font-weight-500">
                {{ $t('katseluoikeuden-viimeinen-voimassaolopaiva') }}
              </dt>
              <dd class="mb-0">
                <elsa-button
                  class="pl-0 pt-1"
                  variant="link"
                  @click="muokkaaKatseluoikeutta(valtuutus)"
                >
                  {{ valtuutus.paattymispaiva ? $date(valtuutus.paattymispaiva) : '' }}
                  <font-awesome-icon icon="edit" fixed-width class="ml-1" />
                </elsa-button>
              </dd>
            </dl>
          </b-card-text>
        </b-card>
      </b-card-group>
      <b-table-simple v-else class="katseluoikeusTable mt-4" responsive>
        <b-thead>
          <b-tr>
            <b-th style="width: 33%">{{ $t('nimi') }}</b-th>
            <b-th>{{ $t('sahkoposti') }}</b-th>
            <b-th>{{ $t('katseluoikeuden-viimeinen-voimassaolopaiva') }}</b-th>
            <b-th></b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <b-tr v-for="(valtuutus, index) in valtuutukset" :key="index">
            <b-td>{{ valtuutus.valtuutettu.nimi }}</b-td>
            <b-td>{{ valtuutus.valtuutettu.sahkoposti }}</b-td>
            <b-td>
              <elsa-button variant="link" @click="muokkaaKatseluoikeutta(valtuutus)">
                {{ valtuutus.paattymispaiva ? $date(valtuutus.paattymispaiva) : '' }}
                <font-awesome-icon icon="edit" fixed-width class="ml-1" />
              </elsa-button>
            </b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
    </div>
    <div v-else>
      <b-alert :show="true" variant="dark" class="mt-3">
        <div class="d-flex flex-row">
          <em class="align-middle">
            <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
          </em>
          <div>
            {{ $t('katseluoikeuksia-kouluttajille-ei-myonnetty') }}
          </div>
        </div>
      </b-alert>
    </div>
    <b-modal id="muokkaaKatseluoikeuttaModal" :title="$t('katseluoikeuden-voimassaoloaika')">
      <div v-if="valittuValtuutus !== null && valittuValtuutus.valtuutettu" class="d-block">
        <h5 class="mt-2">{{ $t('kouluttajan-nimi') }}</h5>
        <p class="mb-4">{{ valittuValtuutus.valtuutettu.nimi }}</p>
        <elsa-form-group
          class="mb-1"
          :label="$t('katseluoikeuden-viimeinen-voimassaolopaiva')"
          :required="true"
        >
          <template #default="{ uid }">
            <elsa-form-datepicker
              :id="uid"
              ref="paattymispaiva"
              class="col-sm-6 pl-0"
              :value.sync="valittuValtuutus.paattymispaiva"
              :min="minPaattymispaiva"
              :min-error-text="$t('paivamaara-ei-voi-olla-menneisyydessa')"
            ></elsa-form-datepicker>
          </template>
        </elsa-form-group>
      </div>

      <template #modal-footer>
        <elsa-button variant="back" @click="$bvModal.hide('muokkaaKatseluoikeuttaModal')">
          {{ $t('peruuta') }}
        </elsa-button>

        <elsa-button variant="primary" @click="paivitaOikeus">
          {{ $t('tallenna') }}
        </elsa-button>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts">
  import axios, { AxiosError } from 'axios'
  import { BModal } from 'bootstrap-vue'
  import { Component, Vue } from 'vue-property-decorator'

  import { postLahikouluttaja } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import KouluttajaForm from '@/forms/kouluttaja-form.vue'
  import store from '@/store'
  import { Kayttaja, KouluttajaValtuutus, ElsaError } from '@/types'
  import { toastSuccess, toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormMultiselect,
      KouluttajaForm,
      ElsaFormDatepicker
    }
  })
  export default class Kayttooikeus extends Vue {
    $refs!: {
      paattymispaiva: ElsaFormDatepicker
    }

    get kouluttajat(): Kayttaja[] {
      return store.getters['erikoistuva/kouluttajat'] || []
    }

    get filteredKouluttajat(): Kayttaja[] {
      return this.kouluttajat.filter(
        (k) =>
          !this.valtuutukset.some((v) => {
            return k.id == v.valtuutettu.id
          })
      )
    }

    get minPaattymispaiva(): Date {
      return new Date()
    }

    valittuKouluttaja: Kayttaja | null = null

    valittuValtuutus: Partial<KouluttajaValtuutus> | null = null

    valtuutukset: KouluttajaValtuutus[] = []

    isLoading = false

    async onKouluttajaSubmit(value: Kayttaja, params: any, modal: BModal) {
      params.saving = true
      try {
        const kouluttaja = (await postLahikouluttaja(value)).data

        if (!this.kouluttajat.some((kayttaja: Kayttaja) => kayttaja.id === kouluttaja.id)) {
          this.kouluttajat.push(kouluttaja)
        }

        this.valittuKouluttaja = kouluttaja
        modal.hide('confirm')
        toastSuccess(this, this.$t('uusi-kouluttaja-lisatty'))
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

    optionDisplayName(option: any) {
      return option.nimike ? option.nimi + ', ' + option.nimike : option.nimi
    }

    async myonnaOikeus() {
      try {
        const valtuutus: KouluttajaValtuutus = (
          await axios.post('/erikoistuva-laakari/kouluttajavaltuutus', this.valittuKouluttaja)
        ).data
        this.valtuutukset.push(valtuutus)
        this.valittuKouluttaja = null
        toastSuccess(this, this.$t('katseluoikeus-myonnetty'))
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('katseluoikeuden-lisaaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('katseluoikeuden-lisaaminen-epaonnistui')
        )
      }
    }

    async paivitaOikeus() {
      if (!this.$refs.paattymispaiva.validateForm()) {
        return
      }

      try {
        await axios.put(`/erikoistuva-laakari/kouluttajavaltuutus/${this.valittuValtuutus?.id}`, {
          paattymispaiva: this.valittuValtuutus?.paattymispaiva
        })
        const valtuutus = this.valtuutukset.find((v) => v.id === this.valittuValtuutus?.id)
        if (valtuutus) valtuutus.paattymispaiva = this.valittuValtuutus?.paattymispaiva || ''
        this.$bvModal.hide('muokkaaKatseluoikeuttaModal')
        toastSuccess(this, this.$t('katseluoikeus-paivitetty'))
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('katseluoikeuden-paivittaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('katseluoikeuden-paivittaminen-epaonnistui')
        )
      }
    }

    muokkaaKatseluoikeutta(valtuutus: KouluttajaValtuutus) {
      this.valittuValtuutus = {
        id: valtuutus.id,
        paattymispaiva: valtuutus.paattymispaiva,
        valtuutettu: valtuutus.valtuutettu
      }
      this.$bvModal.show('muokkaaKatseluoikeuttaModal')
    }

    async mounted() {
      this.isLoading = true
      await store.dispatch('erikoistuva/getKouluttajat')
      this.valtuutukset = (await axios.get('/erikoistuva-laakari/kouluttajavaltuutukset')).data
      this.isLoading = false
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .katseluoikeusTable {
    table {
      td {
        vertical-align: middle;
      }
    }
  }

  @include media-breakpoint-down(xs) {
    .kouluttajaRow {
      justify-content: end;
    }

    .myonnaOikeusCol {
      max-width: 100%;
    }
  }
</style>
