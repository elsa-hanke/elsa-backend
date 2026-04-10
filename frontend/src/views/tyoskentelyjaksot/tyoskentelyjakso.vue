<template>
  <div class="tyoskentelyjakso">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('tyoskentelyjakso') }}</h1>
          <hr />
          <div v-if="tyoskentelyjakso">
            <elsa-form-group :label="$t('tyyppi')">
              <template #default="{ uid }">
                <span :id="uid">{{ tyyppiLabel }}</span>
                <span v-if="tyoskentelyjakso.tyoskentelypaikka.muuTyyppi">
                  : {{ tyoskentelyjakso.tyoskentelypaikka.muuTyyppi }}
                </span>
              </template>
            </elsa-form-group>
            <elsa-form-group :label="$t('tyoskentelypaikka')">
              <template #default="{ uid }">
                <span :id="uid">{{ tyoskentelyjakso.tyoskentelypaikka.nimi }}</span>
              </template>
            </elsa-form-group>
            <elsa-form-group :label="$t('kunta')">
              <template #default="{ uid }">
                <span :id="uid">
                  {{ tyoskentelyjakso.tyoskentelypaikka.kunta.abbreviation }}
                </span>
              </template>
            </elsa-form-group>
            <elsa-form-group :label="$t('tyoaika-taydesta-tyopaivasta') + ' (50–100 %)'">
              <template #default="{ uid }">
                <span :id="uid">{{ tyoskentelyjakso.osaaikaprosentti }} %</span>
              </template>
            </elsa-form-group>
            <b-form-row>
              <elsa-form-group :label="$t('alkamispaiva')" class="col-sm-12 col-md-6 pr-md-3">
                <template #default="{ uid }">
                  <span :id="uid">{{ $date(tyoskentelyjakso.alkamispaiva) }}</span>
                </template>
              </elsa-form-group>
              <elsa-form-group
                v-if="tyoskentelyjakso.paattymispaiva"
                :label="$t('paattymispaiva')"
                class="col-sm-12 col-md-6 pl-md-3"
              >
                <template #default="{ uid }">
                  <span :id="uid" class="datepicker-range">
                    {{ $date(tyoskentelyjakso.paattymispaiva) }}
                  </span>
                </template>
              </elsa-form-group>
            </b-form-row>
            <elsa-form-group :label="$t('kaytannon-koulutus')">
              <template #default="{ uid }">
                <span :id="uid">{{ kaytannonKoulutusLabel }}</span>
                <span v-if="tyoskentelyjakso.omaaErikoisalaaTukeva" class="text-lowercase">
                  : {{ tyoskentelyjakso.omaaErikoisalaaTukeva.nimi }}
                </span>
                <span
                  v-if="
                    tyoskentelyjakso.kaytannonKoulutus === omaaErikoisalaaTukeva &&
                    !tyoskentelyjakso.omaaErikoisalaaTukeva
                  "
                  class="text-lowercase"
                >
                  : {{ $t('muu') }}
                </span>
              </template>
            </elsa-form-group>
            <elsa-form-group
              v-if="tyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan"
              :label="$t('lisatiedot')"
            >
              <template #default="{ uid }">
                <span :id="uid">
                  {{ $t('tyoskentelyjakso-on-aiemmin-hyvaksytty-toiselle-erikoisalalle') }}
                </span>
              </template>
            </elsa-form-group>
            <elsa-form-group :label="$t('liitetiedostot')">
              <template #default="{ uid }">
                <asiakirjat-content
                  :id="uid"
                  :asiakirjat="tyoskentelyjakso.asiakirjat"
                  :sorting-enabled="false"
                  :pagination-enabled="false"
                  :enable-search="false"
                  :enable-delete="false"
                  :no-results-info-text="$t('ei-liitetiedostoja')"
                  :loading="loading"
                />
              </template>
            </elsa-form-group>
            <elsa-button
              v-if="!account.impersonated"
              variant="outline-primary"
              :to="{ name: 'uusi-poissaolo', params: { tyoskentelyjaksoId: tyoskentelyjakso.id } }"
              :disabled="tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon"
              class="mt-3"
            >
              {{ $t('lisaa-poissaolo') }}
            </elsa-button>
            <hr />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="muokkausoikeudet"
                :to="{ name: 'muokkaa-tyoskentelyjaksoa' }"
                variant="primary"
                :disabled="tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-jaksoa') }}
              </elsa-button>
              <elsa-button
                v-if="muokkausoikeudet"
                :loading="deleting"
                :variant="tyoskentelyjakso.tapahtumia ? 'outline-primary' : 'outline-danger'"
                :disabled="
                  tyoskentelyjakso.tapahtumia ||
                  tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon
                "
                class="mb-3"
                @click="onTyoskentelyjaksoDelete"
              >
                {{ $t('poista-jakso') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'tyoskentelyjaksot' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 tyoskentelyjaksot-link"
              >
                {{ $t('palaa-tyoskentelyjaksoihin') }}
              </elsa-button>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
      <b-row
        v-if="
          tyoskentelyjakso &&
          (tyoskentelyjakso.tapahtumia || tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon) &&
          !account.impersonated
        "
      >
        <b-col>
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-1" />
            </em>
            <div>
              <span class="text-size-sm">
                {{
                  tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon
                    ? $t('tyoskentelyjaksoa-ei-voi-muokata-tai-poistaa-tooltip')
                    : $t('tyoskentelyjaksoa-ei-voi-poistaa-tooltip')
                }}
              </span>
            </div>
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Vue, Component } from 'vue-property-decorator'

  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import TyoskentelyjaksoForm from '@/forms/tyoskentelyjakso-form.vue'
  import store from '@/store'
  import { Tyoskentelyjakso } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { KaytannonKoulutusTyyppi } from '@/utils/constants'
  import { ELSA_ROLE } from '@/utils/roles'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import {
    tyoskentelyjaksoKaytannonKoulutusLabel,
    tyoskentelypaikkaTyyppiLabel
  } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      TyoskentelyjaksoForm,
      ElsaFormGroup,
      ElsaButton,
      AsiakirjatContent
    }
  })
  export default class TyoskentelyjaksoView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('tyoskentelyjaksot'),
        to: { name: 'tyoskentelyjaksot' }
      },
      {
        text: this.$t('tyoskentelyjakso'),
        active: true
      }
    ]
    tyoskentelyjakso: Tyoskentelyjakso | null = null
    loading = false
    deleting = false

    async mounted() {
      const tyoskentelyjaksoId = this.$route?.params?.tyoskentelyjaksoId
      if (tyoskentelyjaksoId) {
        this.loading = true
        try {
          this.tyoskentelyjakso = (
            await axios.get(`erikoistuva-laakari/tyoskentelyjaksot/${tyoskentelyjaksoId}`)
          ).data
        } catch {
          toastFail(this, this.$t('tyoskentelyjakson-hakeminen-epaonnistui'))
          this.$router.replace({ name: 'tyoskentelyjaksot' })
        }
        this.loading = false
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    async onTyoskentelyjaksoDelete() {
      if (!this.tyoskentelyjakso?.id) return
      if (
        await confirmDelete(
          this,
          this.$t('poista-tyoskentelyjakso') as string,
          (this.$t('tyoskentelyjakson') as string).toLowerCase()
        )
      ) {
        this.deleting = true
        try {
          await axios.delete(`erikoistuva-laakari/tyoskentelyjaksot/${this.tyoskentelyjakso.id}`)
          toastSuccess(this, this.$t('tyoskentelyjakso-poistettu-onnistuneesti'))
          this.$router.push({
            name: 'tyoskentelyjaksot'
          })
        } catch {
          toastFail(this, this.$t('tyoskentelyjakson-poistaminen-epaonnistui'))
        }
        this.deleting = false
      }
    }

    get omaaErikoisalaaTukeva() {
      return KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
    }

    get kaytannonKoulutusLabel() {
      if (this.tyoskentelyjakso?.kaytannonKoulutus) {
        return tyoskentelyjaksoKaytannonKoulutusLabel(
          this,
          this.tyoskentelyjakso?.kaytannonKoulutus
        )
      }
      return undefined
    }

    get tyyppiLabel() {
      if (this.tyoskentelyjakso?.tyoskentelypaikka?.tyyppi) {
        return tyoskentelypaikkaTyyppiLabel(this, this.tyoskentelyjakso?.tyoskentelypaikka?.tyyppi)
      }
      return undefined
    }

    get muokkausoikeudet() {
      if (!this.account.impersonated) {
        return true
      }

      if (
        this.account.originalUser.authorities.includes(ELSA_ROLE.OpintohallinnonVirkailija) &&
        this.account.erikoistuvaLaakari.muokkausoikeudetVirkailijoilla
      ) {
        return true
      }

      return false
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .tyoskentelyjakso {
    max-width: 768px;
  }

  .datepicker-range::before {
    content: '–';
    position: absolute;
    left: -1rem;
    padding: 0 0.75rem;
    @include media-breakpoint-down(sm) {
      display: none;
    }
  }

  .tyoskentelyjaksot-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
