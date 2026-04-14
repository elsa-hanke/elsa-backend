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
            {{
              `${kategoria.nimi} (${suoritemerkinnatCountByKategoria(kategoria)} ${$t(
                'suoritetta'
              )})`
            }}
          </div>
        </elsa-button>
        <div v-if="kategoria.visible">
          <div
            v-for="(s, suoriteIndex) in kategoria.suoritteet"
            :key="suoriteIndex"
            :class="{ 'mb-3': s.merkinnat.length === 0 }"
          >
            <hr v-if="index !== 0" class="mt-1 mb-2" />
            <div class="d-flex suorite-title mb-0 mt-2 p-2">
              <span class="font-weight-500">{{ s.nimi }}</span>
              <span v-if="s.merkinnat.length === 0" class="mr-2 text-nowrap">
                {{ $t('suoritettu') }}
                {{ s.suoritettulkm }}
                <span :class="{ 'pr-1': s.vaadittulkm }">
                  {{ s.vaadittulkm ? `/ ${s.vaadittulkm}` : '' }}
                </span>
              </span>
            </div>
            <div v-if="s.merkinnat.length > 0">
              <b-table-simple small fixed class="mb-0" stacked="md" responsive>
                <b-thead>
                  <b-tr class="text-size-sm">
                    <b-th scope="col" style="width: 20%" class="text-uppercase">
                      {{ $t('pvm') }}
                    </b-th>
                    <b-th scope="col" style="width: 25%" class="text-uppercase">
                      {{ arviointiAsteikonNimi(arviointiasteikko) }}
                    </b-th>
                    <b-th></b-th>
                  </b-tr>
                </b-thead>
                <b-tbody>
                  <b-tr
                    v-for="(merkinta, merkintaIndex) in s.visible
                      ? s.merkinnat
                      : s.merkinnat.slice(0, 1)"
                    :key="`merkinta-${merkintaIndex}`"
                  >
                    <b-td :stacked-heading="$t('pvm')">
                      <div class="d-flex align-items-center">
                        <elsa-button
                          :to="{
                            name: 'suoritemerkinta',
                            params: {
                              suoritemerkintaId: merkinta.id
                            }
                          }"
                          variant="link"
                          class="shadow-none p-0"
                        >
                          {{ merkinta.suorituspaiva ? $date(merkinta.suorituspaiva) : '' }}
                        </elsa-button>
                      </div>
                    </b-td>
                    <b-td :stacked-heading="arviointiAsteikonNimi(arviointiasteikko)">
                      <div class="d-flex align-items-center">
                        <elsa-arviointiasteikon-taso
                          v-if="merkinta.arviointiasteikonTaso"
                          :value="merkinta.arviointiasteikonTaso"
                          :tasot="arviointiasteikko.tasot"
                        />
                      </div>
                    </b-td>
                    <b-td>
                      <div v-if="merkintaIndex === 0" class="d-flex align-items-center suoritettu">
                        <elsa-button
                          v-if="s.merkinnat.length > 1"
                          variant="link"
                          class="p-0"
                          @click="s.visible = !s.visible"
                        >
                          <span
                            :class="{
                              success:
                                s.vaadittulkm && s.suoritettulkm && s.suoritettulkm >= s.vaadittulkm
                            }"
                          >
                            {{ $t('suoritettu') }}
                            {{ s.suoritettulkm }}
                          </span>
                          <span :class="{ 'pr-1': s.vaadittulkm }">
                            {{ s.vaadittulkm ? `/ ${s.vaadittulkm}` : '' }}
                          </span>
                          <font-awesome-icon
                            :icon="s.visible ? 'chevron-up' : 'chevron-down'"
                            fixed-width
                            size="lg"
                          />
                        </elsa-button>
                        <div v-else>
                          <span
                            :class="{
                              success:
                                s.vaadittulkm && s.suoritettulkm && s.suoritettulkm >= s.vaadittulkm
                            }"
                          >
                            {{ $t('suoritettu') }}
                            {{ s.suoritettulkm }}
                          </span>
                          <span :class="{ 'pr-1': s.vaadittulkm }" class="mr-2">
                            {{ s.vaadittulkm ? `/ ${s.vaadittulkm}` : '' }}
                          </span>
                        </div>
                      </div>
                    </b-td>
                  </b-tr>
                </b-tbody>
              </b-table-simple>
            </div>
          </div>
        </div>
      </div>
    </div>
    <b-alert v-else variant="dark" show>
      <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
      <span>
        {{ $t('ei-suoritemerkintoja') }}
      </span>
    </b-alert>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaArviointiasteikonTaso from '../arviointiasteikon-taso/arviointiasteikon-taso.vue'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import store from '@/store'
  import { Arviointiasteikko, SuoriteRow, SuoritteenKategoriaRow } from '@/types'
  import { ArviointiasteikkoTyyppi } from '@/utils/constants'

  @Component({
    components: {
      ElsaArviointiasteikonTaso,
      ElsaButton,
      ElsaSearchInput,
      ElsaPagination
    }
  })
  export default class SuoritemerkintojenKategoriat extends Vue {
    @Prop({ required: true, default: undefined })
    kategoriat!: SuoritteenKategoriaRow[]

    @Prop({ required: true, default: undefined })
    arviointiasteikko!: Arviointiasteikko

    suoritemerkinnatCountByKategoria(kategoria: SuoritteenKategoriaRow) {
      return kategoria.suoritteet
        .map((a: SuoriteRow) => a.suoritettulkm || 0)
        .reduce((a, b) => a + b)
    }

    arviointiAsteikonNimi(arviointiasteikko: Arviointiasteikko) {
      return arviointiasteikko.nimi === ArviointiasteikkoTyyppi.EPA
        ? this.$t('luottamuksen-taso')
        : this.$t('etappi')
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

  .suoritettu {
    justify-content: flex-end;
  }

  .suorite-title {
    justify-content: space-between;
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
