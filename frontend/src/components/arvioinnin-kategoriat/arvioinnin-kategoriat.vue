<template>
  <div>
    <div v-if="kategoriat.length > 0">
      <div v-for="(kategoria, index) in kategoriat" :key="index" class="mt-3">
        <elsa-button
          variant="link"
          class="text-decoration-none shadow-none border-0 text-dark p-0 w-100"
          @click="kategoria.visible = !kategoria.visible"
        >
          <div class="kategoria-collapse text-left p-2 mt-2 font-weight-500 d-flex">
            <font-awesome-icon
              :icon="kategoria.visible ? 'caret-up' : 'caret-down'"
              fixed-width
              size="lg"
              class="text-muted mr-2"
            />
            {{ `${kategoria.nimi} (${arvioinnitCountByKategoria(kategoria)} ${$t('arviointia')})` }}
          </div>
        </elsa-button>
        <div v-if="kategoria.visible">
          <div
            v-for="(a, kokonaisuusIndex) in kategoria.arvioitavatKokonaisuudet"
            :key="kokonaisuusIndex"
            :class="{ 'mb-3': a.arvioinnit.length === 0 }"
          >
            <hr v-if="index !== 0" class="mt-1 mb-2" />
            <p class="font-weight-500 p-2 mb-0 mt-2">{{ a.nimi }}</p>
            <div v-if="a.arvioinnit.length > 0">
              <b-table-simple small fixed class="mb-0" stacked="md" responsive>
                <b-thead>
                  <b-tr class="text-size-sm">
                    <b-th scope="col" style="width: 12%" class="text-uppercase">
                      {{ $t('pvm') }}
                    </b-th>
                    <b-th scope="col" style="width: 15%" class="text-uppercase">
                      {{ $t('tapahtuma') }}
                    </b-th>
                    <b-th scope="col" style="width: 13%" class="text-uppercase">
                      {{ $t('arviointi') }}
                    </b-th>
                    <b-th scope="col" style="width: 18%" class="text-uppercase">
                      {{ $t('itsearviointi') }}
                    </b-th>
                    <b-th scope="col" style="width: 22%" class="text-uppercase">
                      {{ $t('tyoskentelypaikka') }}
                    </b-th>
                    <b-th scope="col" style="width: 20%" class="text-uppercase">
                      {{ $t('arvioinnin-antaja') }}
                    </b-th>
                  </b-tr>
                </b-thead>
                <b-tbody>
                  <b-tr
                    v-for="(arviointi, arviointiIndex) in a.visible
                      ? a.arvioinnit
                      : a.arvioinnit.slice(0, 1)"
                    :key="`arviointi-${arviointiIndex}`"
                  >
                    <b-td :stacked-heading="$t('pvm')">
                      <elsa-button
                        variant="link"
                        :to="{
                          name: 'arviointi',
                          params: { arviointiId: arviointi.id }
                        }"
                        class="shadow-none p-0"
                      >
                        {{ $date(arviointi.tapahtumanAjankohta) }}
                      </elsa-button>
                    </b-td>
                    <b-td :stacked-heading="$t('tapahtuma')">
                      {{ arviointi.arvioitavaTapahtuma }}
                    </b-td>
                    <b-td :stacked-heading="$t('arviointi')">
                      <elsa-badge
                        v-if="arviointi.arviointiasteikonTaso"
                        :value="arviointi.arviointiasteikonTaso"
                        :variant="arviointi.arviointiasteikonTaso > 3 ? 'success' : 'light'"
                      />
                      <span v-else class="text-size-sm text-light-muted">
                        {{ $t('ei-tehty-viela') }}
                      </span>
                    </b-td>
                    <b-td :stacked-heading="$t('itsearviointi')">
                      <elsa-badge
                        v-if="arviointi.itsearviointiArviointiasteikonTaso"
                        :value="arviointi.itsearviointiArviointiasteikonTaso"
                      />
                      <elsa-button
                        v-else-if="!arviointi.lukittu && !account.impersonated"
                        variant="primary"
                        :to="{
                          name: 'itsearviointi',
                          params: { arviointiId: arviointi.id }
                        }"
                      >
                        {{ $t('tee-itsearviointi') }}
                      </elsa-button>
                      <span v-else class="text-size-sm text-light-muted">
                        {{ $t('ei-tehty') }}
                      </span>
                    </b-td>
                    <b-td :stacked-heading="$t('tyoskentelypaikka')">
                      {{ arviointi.tyoskentelyjakso.tyoskentelypaikka.nimi }}
                    </b-td>
                    <b-td :stacked-heading="$t('arvioinnin-antaja')">
                      {{ arviointi.arvioinninAntaja.nimi }}
                    </b-td>
                  </b-tr>
                </b-tbody>
              </b-table-simple>
              <div class="text-right">
                <elsa-button
                  v-if="a.arvioinnit.length > 1"
                  variant="link"
                  class="shadow-none font-weight-500"
                  @click="a.visible = !a.visible"
                >
                  {{ `${$t('kaikki-arvioinnit')} (${a.arvioinnit.length})` }}
                  <font-awesome-icon
                    :icon="a.visible ? 'chevron-up' : 'chevron-down'"
                    fixed-width
                    class="ml-1 text-dark"
                  />
                </elsa-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <b-alert v-else variant="dark" show>
      <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
      <span>
        {{ $t('ei-suoritusarviointeja') }}
      </span>
    </b-alert>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaBadge from '@/components/badge/badge.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import store from '@/store'
  import { ArvioitavaKokonaisuus, ArvioitavanKokonaisuudenKategoria } from '@/types'

  @Component({
    components: {
      ElsaBadge,
      ElsaButton,
      ElsaSearchInput,
      ElsaPagination
    }
  })
  export default class ArvioinninKategoriat extends Vue {
    @Prop({ required: true, default: undefined })
    kategoriat!: ArvioitavanKokonaisuudenKategoria[]

    arvioinnitCountByKategoria(kategoria: ArvioitavanKokonaisuudenKategoria) {
      return kategoria.arvioitavatKokonaisuudet
        .map((a: ArvioitavaKokonaisuus) => a.arvioinnit.length)
        .reduce((a, b) => a + b)
    }

    get account() {
      return store.getters['auth/account']
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .kategoria-collapse {
    background: #f5f5f6;
  }

  ::v-deep table {
    border-bottom: none;

    thead tr th {
      border-top: none;
      border-bottom: none;
    }
    tbody tr:first-child td {
      border-top: none;
    }
    td {
      vertical-align: middle;
    }
    td,
    th {
      padding-left: 0.5rem;
      padding-right: 0.5rem;
    }
    th {
      font-weight: 400;
    }
  }

  @include media-breakpoint-down(sm) {
    ::v-deep table {
      tr {
        border: $table-border-width solid $table-border-color;
        border-radius: $border-radius;
        margin-top: 0.5rem;
        padding-top: $table-cell-padding;
        padding-bottom: $table-cell-padding;
      }

      td {
        border: none;
        padding: 0 $table-cell-padding;

        & > div {
          width: 100% !important;
          padding: 0 0 0.5rem 0 !important;
        }

        &::before {
          text-align: left !important;
          font-weight: 400 !important;
          width: 100% !important;
          padding-right: 0 !important;
        }

        &:last-child > div {
          padding-bottom: 0 !important;
        }
      }
    }
  }
</style>
