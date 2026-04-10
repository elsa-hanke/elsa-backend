<template>
  <div class="teoriakoulutukset">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('teoriakoulutukset') }}</h1>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <p v-html="$t('teoriakoulutukset-ingressi', { opintooppaastaLinkki, kopiLinkki })" />
          <elsa-vanha-asetus-varoitus />
          <elsa-button
            v-if="muokkausoikeudet"
            variant="primary"
            :to="{ name: 'uusi-teoriakoulutus' }"
            class="mb-4"
          >
            {{ $t('lisaa-teoriakoulutus') }}
          </elsa-button>
          <div v-if="!loading">
            <div class="d-flex justify-content-center border rounded pt-3 mb-4">
              <b-container fluid>
                <elsa-form-group :label="$t('teoriakoulutusaika-yhteensa')">
                  <template #default="{ uid }">
                    <div :id="uid">
                      <elsa-progress-bar
                        :value="suoritettuTeoriakoulutusMaara"
                        :min-required="erikoisalanVaatimaTeoriakoulutustenVahimmaismaara"
                        :show-required-text="true"
                        color="#41b257"
                        background-color="#b3e1bc"
                        :custom-unit="$t('t')"
                      />
                    </div>
                  </template>
                </elsa-form-group>
              </b-container>
            </div>
            <div v-if="teoriakoulutuksetFormatted.length > 0" class="teoriakoulutukset-table">
              <b-table :items="teoriakoulutuksetFormatted" :fields="fields" stacked="md" responsive>
                <template #cell(nimi)="row">
                  <elsa-button
                    :to="{
                      name: 'teoriakoulutus',
                      params: { teoriakoulutusId: row.item.id }
                    }"
                    variant="link"
                    class="p-0 border-0 shadow-none"
                  >
                    {{ row.item.nimi }}
                  </elsa-button>
                </template>
                <template #cell(todistus)="row">
                  <div v-for="todistus in row.item.todistukset" :key="todistus.id">
                    <elsa-button
                      variant="link"
                      class="p-0 border-0 shadow-none"
                      :loading="todistus.disablePreview"
                      @click="onViewAsiakirja(todistus)"
                    >
                      {{ todistus.nimi }}
                    </elsa-button>
                  </div>
                </template>
              </b-table>
            </div>
            <div v-else>
              <b-alert variant="dark" show>
                <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                <span>
                  {{ $t('ei-teoriakoulutuksia') }}
                </span>
              </b-alert>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Vue, Component } from 'vue-property-decorator'

  import { getTeoriakoulutukset } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaProgressBar from '@/components/progress-bar/progress-bar.vue'
  import ElsaVanhaAsetusVaroitus from '@/components/vanha-asetus-varoitus/vanha-asetus-varoitus.vue'
  import store from '@/store'
  import { Asiakirja, Teoriakoulutus } from '@/types'
  import { fetchAndOpenBlob } from '@/utils/blobs'
  import { sortByDateDesc } from '@/utils/date'
  import { ELSA_ROLE } from '@/utils/roles'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaProgressBar,
      ElsaVanhaAsetusVaroitus
    }
  })
  export default class Teoriakoulutukset extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('teoriakoulutukset'),
        active: true
      }
    ]
    loading = true
    fields = [
      {
        key: 'nimi',
        label: this.$t('koulutuksen-nimi'),
        sortable: true
      },
      {
        key: 'koulutuksenPaikka',
        label: this.$t('paikka'),
        sortable: true
      },
      {
        key: 'pvm',
        label: this.$t('pvm'),
        sortable: true
      },
      {
        key: 'erikoistumiseenHyvaksyttavaTuntimaara',
        label: this.$t('tunnit'),
        sortable: true
      },
      {
        key: 'todistus',
        label: this.$t('todistus'),
        sortable: false
      }
    ]
    teoriakoulutukset: Teoriakoulutus[] = []
    erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: number | null = null

    async mounted() {
      await this.fetchTeoriakoulutukset()
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async fetchTeoriakoulutukset() {
      try {
        const { teoriakoulutukset, erikoisalanVaatimaTeoriakoulutustenVahimmaismaara } = (
          await getTeoriakoulutukset()
        ).data
        this.teoriakoulutukset = teoriakoulutukset
        this.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara =
          erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
      } catch (err) {
        toastFail(this, this.$t('teoriakoulutuksien-hakeminen-epaonnistui'))
      }
    }

    get teoriakoulutuksetFormatted() {
      return this.teoriakoulutukset
        .sort((a, b) => sortByDateDesc(a.alkamispaiva, b.alkamispaiva))
        .map((teoriakoulutus) => ({
          ...teoriakoulutus,
          nimi: teoriakoulutus.koulutuksenNimi,
          pvm: teoriakoulutus.alkamispaiva
            ? `${this.$date(teoriakoulutus.alkamispaiva)}${
                teoriakoulutus.paattymispaiva ? `-${this.$date(teoriakoulutus.paattymispaiva)}` : ''
              }`
            : null,
          todistus: teoriakoulutus.todistukset
        }))
    }

    get suoritettuTeoriakoulutusMaara() {
      return this.teoriakoulutukset
        .map((teoriakoulutus) => teoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara)
        .reduce((a, b) => (a ?? 0) + (b ?? 0), 0)
    }

    get opintooppaastaLinkki() {
      return `<a href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/opinto-oppaat/" target="_blank" rel="noopener noreferrer">${(
        this.$t('opinto-oppaasta') as string
      ).toLowerCase()}</a>`
    }

    get kopiLinkki() {
      return `<a href="https://www.kopi.fi/" target="_blank" rel="noopener noreferrer">www.kopi.fi</a>`
    }

    async onViewAsiakirja(asiakirja: Asiakirja) {
      if (asiakirja.id) {
        Vue.set(asiakirja, 'disablePreview', true)
        const success = await fetchAndOpenBlob('erikoistuva-laakari/asiakirjat/', asiakirja.id)
        if (!success) {
          toastFail(this, this.$t('asiakirjan-sisallon-hakeminen-epaonnistui'))
        }

        Vue.set(asiakirja, 'disablePreview', false)
      }
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

  .teoriakoulutukset {
    max-width: 1024px;
  }

  @include media-breakpoint-down(sm) {
    .teoriakoulutukset-table {
      ::v-deep table {
        border-bottom: 0;

        tr {
          border: $table-border-width solid $table-border-color;
          border-radius: $border-radius;
          margin-top: 0.5rem;
          padding-top: $table-cell-padding;
          padding-bottom: $table-cell-padding;
        }

        td {
          border: none;
          padding: 0 0.5rem;

          & > div {
            width: 100% !important;
            padding: 0 0 0.5rem 0 !important;
          }

          &::before {
            text-align: left !important;
            font-weight: 500 !important;
            width: 100% !important;
            padding-right: 0 !important;
          }
          &:last-child > div {
            padding-bottom: 0 !important;
          }

          &.last > div {
            padding-bottom: 0 !important;
          }
        }
      }
    }
  }
</style>
