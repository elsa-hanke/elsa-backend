<template>
  <div class="suoritemerkinnat">
    <b-breadcrumb :items="items" class="mb-0"></b-breadcrumb>
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('suoritemerkinnat') }}</h1>
          <p class="mb-3">{{ $t('suoritemerkinnat-kuvaus') }}</p>
          <elsa-button
            v-if="!account.impersonated"
            variant="primary"
            :to="{ name: 'uusi-suoritemerkinta' }"
          >
            {{ $t('lisaa-suoritemerkinta') }}
          </elsa-button>
          <hr />
          <small class="text-uppercase">{{ $t('rajaa-suoritteita') }}</small>
          <b-container fluid class="px-0 mt-2">
            <b-row :class="{ 'mb-3': !isFiltered }">
              <b-col md="4">
                <elsa-form-group :label="$t('tyoskentelyjakso')" class="mb-md-0">
                  <template #default="{ uid }">
                    <elsa-form-multiselect
                      :id="uid"
                      v-model="selected.tyoskentelyjakso"
                      :options="tyoskentelyjaksotFormatted"
                      label="label"
                      track-by="id"
                      @select="onTyoskentelyjaksoSelect"
                      @clearMultiselect="onTyoskentelyjaksoReset"
                    ></elsa-form-multiselect>
                  </template>
                </elsa-form-group>
              </b-col>
              <b-col md="4">
                <elsa-form-group :label="$t('suorite')" class="mb-md-0">
                  <template #default="{ uid }">
                    <elsa-form-multiselect
                      :id="uid"
                      v-model="selected.suorite"
                      :options="suoritteetSorted"
                      label="nimi"
                      track-by="id"
                      @select="onSuoriteSelect"
                      @clearMultiselect="onSuoriteReset"
                    ></elsa-form-multiselect>
                  </template>
                </elsa-form-group>
              </b-col>
              <b-col md="4">
                <elsa-form-group :label="$t('suorituspaiva')" class="mb-0">
                  <template #default="{ uid }">
                    <div class="d-flex">
                      <elsa-form-datepicker
                        :id="uid"
                        ref="suorituspaivaAlkaa"
                        :value="selected.suorituspaivaAlkaa"
                        class="flex-nowrap"
                        :required="false"
                        @input="onSuorituspaivaAlkaaSelect"
                      ></elsa-form-datepicker>
                      <span class="mr-2 mt-1">-</span>
                      <elsa-form-datepicker
                        :id="uid"
                        ref="suorituspaivaPaattyy"
                        :value="selected.suorituspaivaPaattyy"
                        class="flex-nowrap"
                        :required="false"
                        @input="onSuorituspaivaPaattyySelect"
                      ></elsa-form-datepicker>
                    </div>
                  </template>
                </elsa-form-group>
              </b-col>
            </b-row>
            <b-row>
              <b-col>
                <div class="d-flex flex-row-reverse">
                  <elsa-button
                    v-if="isFiltered"
                    variant="link"
                    class="shadow-none text-size-sm font-weight-500"
                    @click="resetFilters"
                  >
                    {{ $t('tyhjenna-valinnat') }}
                  </elsa-button>
                </div>
              </b-col>
            </b-row>
          </b-container>
          <div v-if="suoritteetTable && !loading">
            <suoritemerkintojen-kategoriat
              :kategoriat="suoritteenKategoriat"
              :arviointiasteikko="suoritteetTable.arviointiasteikko"
            />
          </div>
          <div v-if="suoritteetTable && aiemmatKategoriat && aiemmatKategoriat.length > 0">
            <h2 class="mt-6">{{ $t('aiemmat-suoritemerkinnat') }}</h2>
            <div>
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              {{ $t('aiemmat-suoritemerkinnat-kuvaus') }}
            </div>
            <suoritemerkintojen-kategoriat
              :kategoriat="aiemmatKategoriat"
              :arviointiasteikko="suoritteetTable.arviointiasteikko"
            />
          </div>
          <div v-if="loading" class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaArviointiasteikonTasoTooltipContent from '@/components/arviointiasteikon-taso/arviointiasteikon-taso-tooltip.vue'
  import ElsaArviointiasteikonTaso from '@/components/arviointiasteikon-taso/arviointiasteikon-taso.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import SuoritemerkintojenKategoriat from '@/components/suoritemerkintojen-kategoriat/suoritemerkintojen-kategoriat.vue'
  import store from '@/store'
  import {
    Arviointiasteikko,
    Suorite,
    SuoritteenKategoria,
    SuoritteetTable,
    SuoritemerkintaRow,
    SuoritemerkintaFilter,
    SuoritemerkinnatOptions,
    Tyoskentelyjakso,
    SuoriteRow,
    SuoritteenKategoriaRow,
    Suoritemerkinta
  } from '@/types'
  import { ArviointiasteikkoTyyppi } from '@/utils/constants'
  import { dateBetween, sortByDateDesc } from '@/utils/date'
  import { sortByAsc } from '@/utils/sort'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      ElsaButton,
      ElsaFormDatepicker,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaPopover,
      ElsaArviointiasteikonTaso,
      ElsaArviointiasteikonTasoTooltipContent,
      SuoritemerkintojenKategoriat
    }
  })
  export default class Suoritemerkinnat extends Vue {
    $refs!: {
      suorituspaivaAlkaa: ElsaFormDatepicker
      suorituspaivaPaattyy: ElsaFormDatepicker
    }

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('suoritemerkinnat'),
        active: true
      }
    ]
    selected: SuoritemerkintaFilter = {
      tyoskentelyjakso: null,
      suorite: null,
      suorituspaivaAlkaa: null,
      suorituspaivaPaattyy: null
    }
    suoritemerkinnatOptions: SuoritemerkinnatOptions = {
      tyoskentelyjaksot: [],
      suoritteet: []
    }
    suoritteetTable: SuoritteetTable | null = null
    suoritemerkinnat: Record<number, Suoritemerkinta[]> | null = null
    suoritteenKategoriat: SuoritteenKategoriaRow[] = []
    aiemmatKategoriat: SuoritteenKategoriaRow[] = []
    loading = true

    async mounted() {
      this.loading = true
      await this.fetchOptions()
      await this.fetch()
      this.loading = false
    }

    async fetchOptions() {
      this.suoritemerkinnatOptions = (
        await axios.get('erikoistuva-laakari/suoritemerkinnat-rajaimet')
      ).data
    }

    async fetch() {
      this.loading = true
      this.suoritteetTable = (await axios.get('erikoistuva-laakari/suoritteet-taulukko')).data

      if (this.suoritteetTable) {
        this.suoritemerkinnat = this.suoritteetTable.suoritemerkinnat.reduce(
          (result: Record<number, Suoritemerkinta[]>, suoritemerkinta: Suoritemerkinta) => {
            const suoriteId = suoritemerkinta.suorite.id
            if (suoriteId in result) {
              result[suoriteId].push({
                ...suoritemerkinta
              })
            } else {
              result[suoriteId] = [
                {
                  ...suoritemerkinta
                }
              ]
            }
            return result
          },
          {}
        )
      }
      this.solveKategoriat()
      this.solveAiemmatKategoriat()
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async onTyoskentelyjaksoSelect(selected: Tyoskentelyjakso) {
      this.selected.tyoskentelyjakso = selected
      this.solveKategoriat()
    }

    async onTyoskentelyjaksoReset() {
      this.selected.tyoskentelyjakso = null
      this.solveKategoriat()
    }

    async onSuoriteSelect(selected: Suorite) {
      this.selected.suorite = selected
      this.solveKategoriat()
    }

    async onSuoriteReset() {
      this.selected.suorite = null
      this.solveKategoriat()
    }

    onSuorituspaivaAlkaaSelect(value: string) {
      this.selected.suorituspaivaAlkaa = value

      if (!this.$refs.suorituspaivaAlkaa.validateForm()) {
        return
      }
      this.solveKategoriat()
    }

    onSuorituspaivaPaattyySelect(value: string) {
      this.selected.suorituspaivaPaattyy = value

      if (!this.$refs.suorituspaivaPaattyy.validateForm()) {
        return
      }
      this.solveKategoriat()
    }

    async resetFilters() {
      this.selected = {
        tyoskentelyjakso: null,
        suorite: null,
        suorituspaivaAlkaa: null,
        suorituspaivaPaattyy: null
      }
      this.$refs.suorituspaivaAlkaa.resetValue()
      this.$refs.suorituspaivaPaattyy.resetValue()
      this.solveKategoriat()
    }

    toggleDetails(row: SuoritemerkintaRow) {
      if (row.suoritemerkinta) {
        row.suoritemerkinta.showDetails = !row.suoritemerkinta.showDetails
      }
    }

    arviointiAsteikonNimi(arviointiasteikko: Arviointiasteikko) {
      return arviointiasteikko.nimi === ArviointiasteikkoTyyppi.EPA
        ? this.$t('luottamuksen-taso')
        : this.$t('etappi')
    }

    solveKategoriat() {
      if (this.suoritteetTable) {
        this.suoritteenKategoriat = this.suoritteetTable.suoritteenKategoriat
          .map((kategoria: SuoritteenKategoria) => {
            const rows: SuoriteRow[] = kategoria.suoritteet.map((suorite) => {
              // Kerätään suoritteen suoritemerkinnät ja järjestetään ne aikajärjestykseen
              const suoritemerkinnat = (
                this.suoritemerkinnat ? this.suoritemerkinnat[suorite.id] || [] : []
              )
                .filter(
                  (s) =>
                    (this.selected.tyoskentelyjakso == null ||
                      s.tyoskentelyjaksoId === this.selected.tyoskentelyjakso.id) &&
                    (this.selected.suorite == null || s.suoriteId === this.selected.suorite.id) &&
                    ((this.selected.suorituspaivaAlkaa == null &&
                      this.selected.suorituspaivaPaattyy == null) ||
                      dateBetween(
                        s.suorituspaiva,
                        this.selected.suorituspaivaAlkaa || undefined,
                        this.selected.suorituspaivaPaattyy
                      ))
                )
                .sort((a: Suoritemerkinta, b: Suoritemerkinta) =>
                  sortByDateDesc(a.suorituspaiva, b.suorituspaiva)
                )
              suorite.suoritettulkm = suoritemerkinnat.length

              return {
                ...suorite,
                visible: false,
                merkinnat: suoritemerkinnat
              }
            }, [])

            return {
              ...kategoria,
              visible: true,
              suoritteet: rows.filter((s) => s.merkinnat.length > 0 || !this.isFiltered)
            }
          })
          .filter((k) => k.suoritteet.length > 0 || !this.isFiltered)
      }
    }

    solveAiemmatKategoriat() {
      if (this.suoritteetTable && this.suoritemerkinnat) {
        const aiemmatKategoriat: Map<number, SuoriteRow[]> = new Map()
        const suoriteIds = this.suoritteetTable.suoritteenKategoriat.flatMap((k) =>
          k.suoritteet.map((s) => s.id)
        )
        for (const suoriteId in this.suoritemerkinnat) {
          const suoritemerkinnat = this.suoritemerkinnat[suoriteId].sort(
            (a: Suoritemerkinta, b: Suoritemerkinta) =>
              sortByDateDesc(a.suorituspaiva, b.suorituspaiva)
          )
          if (!suoriteIds.find((id) => id === parseInt(suoriteId))) {
            const suoritemerkinta = suoritemerkinnat[0]
            const suorite = suoritemerkinta.suorite
            suorite.suoritettulkm = suoritemerkinnat.length

            if (!aiemmatKategoriat.get(suorite.kategoriaId)) {
              aiemmatKategoriat.set(suorite.kategoriaId, [])
            }
            aiemmatKategoriat.get(suorite.kategoriaId)?.push({
              ...suorite,
              visible: true,
              merkinnat: suoritemerkinnat
            })
          }
        }

        aiemmatKategoriat.forEach((suoritteet: SuoriteRow[], kategoriaId: number) => {
          this.aiemmatKategoriat.push({
            nimi:
              this.suoritteetTable?.aiemmatKategoriat.find((k) => k.id === kategoriaId)?.nimi || '',
            visible: true,
            suoritteet: suoritteet
          })
        })
      }
    }

    get isFiltered() {
      return (
        this.selected.tyoskentelyjakso ||
        this.selected.suorite ||
        this.selected.suorituspaivaAlkaa ||
        this.selected.suorituspaivaPaattyy
      )
    }

    get tyoskentelyjaksotFormatted() {
      return this.suoritemerkinnatOptions?.tyoskentelyjaksot.map((tj: Tyoskentelyjakso) => ({
        ...tj,
        label: tyoskentelyjaksoLabel(this, tj)
      }))
    }

    get suoritteetSorted() {
      const suoritteet = this.suoritteetTable?.suoritteenKategoriat.flatMap((s) => s.suoritteet)
      return suoritteet?.sort((a: Suorite, b: Suorite) => sortByAsc(a.nimi, b.nimi))
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .suoritemerkinnat {
    max-width: 1024px;
  }
</style>
