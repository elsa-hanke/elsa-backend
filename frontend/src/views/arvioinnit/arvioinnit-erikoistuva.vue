<template>
  <div class="arvioinnit">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('arvioinnit') }}</h1>
          <p class="mb-3">{{ $t('arvioinnit-kuvaus') }}</p>
          <elsa-button
            v-if="!account.impersonated"
            variant="primary"
            :to="{ name: 'arviointipyynto' }"
            class="mb-4"
          >
            {{ $t('pyyda-arviointia') }}
          </elsa-button>
          <b-tabs content-class="mt-3" :no-fade="true" @input="onTabChange">
            <b-tab :title="$t('arvioinnit')" active>
              <small class="text-uppercase">{{ $t('rajaa-arviointeja') }}</small>
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
                    <elsa-form-group :label="$t('arvioitava-kokonaisuus')" class="mb-md-0">
                      <template #default="{ uid }">
                        <elsa-form-multiselect
                          :id="uid"
                          v-model="selected.arvioitavaKokonaisuus"
                          :options="arvioitavatKokonaisuudetSorted"
                          label="nimi"
                          track-by="id"
                          @select="onArvioitavaKokonaisuusSelect"
                          @clearMultiselect="onArvioitavaKokonaisuusReset"
                        ></elsa-form-multiselect>
                      </template>
                    </elsa-form-group>
                  </b-col>
                  <b-col md="4">
                    <elsa-form-group :label="$t('kouluttaja-tai-vastuuhenkilo')" class="mb-0">
                      <template #default="{ uid }">
                        <elsa-form-multiselect
                          :id="uid"
                          v-model="selected.kouluttajaOrVastuuhenkilo"
                          :options="suoritusArvioinnitOptions.kouluttajatAndVastuuhenkilot"
                          label="nimi"
                          track-by="id"
                          @select="onKouluttajaOrVastuuhenkiloSelect"
                          @clearMultiselect="onKouluttajaOrVastuuhenkiloReset"
                        ></elsa-form-multiselect>
                      </template>
                    </elsa-form-group>
                  </b-col>
                </b-row>
                <b-row>
                  <b-col>
                    <div class="d-flex flex-row-reverse">
                      <elsa-button
                        v-if="
                          selected.tyoskentelyjakso ||
                          selected.arvioitavaKokonaisuus ||
                          selected.kouluttajaOrVastuuhenkilo
                        "
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
              <div class="arvioinnit">
                <div v-if="kategoriat">
                  <arvioinnin-kategoriat :kategoriat="kategoriat" />
                </div>
                <div v-if="aiemmatKategoriat && aiemmatKategoriat.length > 0">
                  <h2 class="mt-6">{{ $t('aiemmat-arvioinnit') }}</h2>
                  <div>
                    <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                    {{ $t('aiemmat-arvioinnit-kuvaus') }}
                  </div>
                  <arvioinnin-kategoriat :kategoriat="aiemmatKategoriat" />
                </div>
                <div v-if="!kategoriat || !aiemmatKategoriat" class="text-center mt-3">
                  <b-spinner variant="primary" :label="$t('ladataan')" />
                </div>
              </div>
            </b-tab>
            <b-tab :title="$t('arviointipyynnot')">
              <div v-if="arvioinnit">
                <div v-for="(arviointipyynto, index) in arvioinnit" :key="index">
                  <arviointipyynto-card :value="arviointipyynto" />
                </div>
                <b-alert v-if="arvioinnit.length === 0" variant="dark" show>
                  <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                  {{ $t('kaikkiin-arviointipyyntoihisi-on-tehty-arviointi') }}
                </b-alert>
              </div>
              <div v-else class="text-center mt-3">
                <b-spinner variant="primary" :label="$t('ladataan')" />
              </div>
            </b-tab>
          </b-tabs>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import ArvioinninKategoriat from '@/components/arvioinnin-kategoriat/arvioinnin-kategoriat.vue'
  import ArviointipyyntoCard from '@/components/arviointipyynto-card/arviointipyynto-card.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import store from '@/store'
  import {
    Suoritusarviointi,
    SuoritusarviointiFilter,
    SuoritusarvioinnitOptions,
    ArvioitavaKokonaisuus,
    ArvioitavanKokonaisuudenKategoria,
    Tyoskentelyjakso,
    Kayttaja,
    SuoritusarvioinninArvioitavaKokonaisuus
  } from '@/types'
  import { sortByDateDesc } from '@/utils/date'
  import { formatList } from '@/utils/kouluttajaAndVastuuhenkiloListFormatter'
  import { sortByAsc } from '@/utils/sort'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      ArvioinninKategoriat,
      ArviointipyyntoCard,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaButton
    }
  })
  export default class ArvioinnitErikoistuva extends Vue {
    selected = {
      tyoskentelyjakso: null,
      arvioitavaKokonaisuus: null,
      kouluttajaOrVastuuhenkilo: null
    } as SuoritusarviointiFilter
    suoritusArvioinnitOptions = {
      tyoskentelyjaksot: [],
      arvioitavatKokonaisuudet: [],
      tapahtumat: [],
      kouluttajatAndVastuuhenkilot: []
    } as SuoritusarvioinnitOptions
    arvioinnit: null | Suoritusarviointi[] = null
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arvioinnit'),
        active: true
      }
    ]
    kategoriat: null | ArvioitavanKokonaisuudenKategoria[] = null
    aiemmatKategoriat: null | ArvioitavanKokonaisuudenKategoria[] = null

    async mounted() {
      await this.fetchOptions()
      await this.fetch()
    }

    get account() {
      return store.getters['auth/account']
    }

    onTabChange(tabIndex: number) {
      this.selected = {
        tyoskentelyjakso: null,
        arvioitavaKokonaisuus: null,
        kouluttajaOrVastuuhenkilo: null
      }
      this.arvioinnit = null
      if (tabIndex === 0) {
        this.fetch()
      } else if (tabIndex === 1) {
        // Hetaan arviointipyynnöt pelkästään
        this.fetch({
          'arviointiAika.specified': false
        })
      }
    }

    async onTyoskentelyjaksoSelect(selected: Tyoskentelyjakso) {
      this.selected.tyoskentelyjakso = selected
      await this.fetch()
    }

    async onTyoskentelyjaksoReset() {
      this.selected.tyoskentelyjakso = null
      await this.fetch()
    }

    async onArvioitavaKokonaisuusSelect(selected: ArvioitavaKokonaisuus) {
      this.selected.arvioitavaKokonaisuus = selected
      await this.fetch()
    }

    async onArvioitavaKokonaisuusReset() {
      this.selected.arvioitavaKokonaisuus = null
      await this.fetch()
    }

    async onKouluttajaOrVastuuhenkiloSelect(selected: Kayttaja) {
      this.selected.kouluttajaOrVastuuhenkilo = selected
      await this.fetch()
    }

    async onKouluttajaOrVastuuhenkiloReset() {
      this.selected.kouluttajaOrVastuuhenkilo = null
      await this.fetch()
    }

    async resetFilters() {
      this.selected = {
        tyoskentelyjakso: null,
        arvioitavaKokonaisuus: null,
        kouluttajaOrVastuuhenkilo: null
      }
      await this.fetch()
    }

    async fetchOptions() {
      this.suoritusArvioinnitOptions = (
        await axios.get('erikoistuva-laakari/suoritusarvioinnit-rajaimet')
      ).data
      if (this.suoritusArvioinnitOptions) {
        this.suoritusArvioinnitOptions.kouluttajatAndVastuuhenkilot = formatList(
          this,
          this.suoritusArvioinnitOptions?.kouluttajatAndVastuuhenkilot
        )
      }
    }

    async fetch(options: any = {}) {
      try {
        this.arvioinnit = (
          await axios.get('erikoistuva-laakari/suoritusarvioinnit', {
            params: {
              ...options,
              'tyoskentelyjaksoId.equals': this.selected.tyoskentelyjakso?.id,
              arvioitavaKokonaisuusId: this.selected.arvioitavaKokonaisuus?.id,
              'arvioinninAntajaId.equals': this.selected.kouluttajaOrVastuuhenkilo?.id
            }
          })
        ).data?.sort((s1: Suoritusarviointi, s2: Suoritusarviointi) => {
          return sortByDateDesc(s1?.tapahtumanAjankohta, s2?.tapahtumanAjankohta)
        })
        this.kategoriat = this.solveKategoriat()
      } catch {
        this.arvioinnit = []
      }
    }

    solveKategoriat() {
      // Muodostetaan arvioitavat kokonaisuudet -lista
      const arvioitavatKokonaisuudetArray = (
        this.selected.tyoskentelyjakso || this.selected.kouluttajaOrVastuuhenkilo
          ? this.arvioinnit?.flatMap((arviointi: Suoritusarviointi) =>
              arviointi.arvioitavatKokonaisuudet.map(
                (kokonaisuus: SuoritusarvioinninArvioitavaKokonaisuus) =>
                  kokonaisuus.arvioitavaKokonaisuus
              )
            )
          : this.suoritusArvioinnitOptions?.arvioitavatKokonaisuudet.filter(
              (a: ArvioitavaKokonaisuus) =>
                this.selected.arvioitavaKokonaisuus
                  ? a.id === this.selected.arvioitavaKokonaisuus?.id
                  : true
            )
      )
        ?.filter((x) => x != null)
        .map((a: ArvioitavaKokonaisuus | null) => ({
          ...a,
          arvioinnit: [],
          visible: false
        })) as ArvioitavaKokonaisuus[]
      const arvioitavatKokonaisuudetMap = [
        ...new Map(
          arvioitavatKokonaisuudetArray.map((a: ArvioitavaKokonaisuus) => [a['id'], a])
        ).values()
      ]

      // Muodostetaan kategoriat lista
      let kategoriat: ArvioitavanKokonaisuudenKategoria[] = []
      arvioitavatKokonaisuudetMap?.forEach((a: ArvioitavaKokonaisuus) => {
        kategoriat.push({
          ...a.kategoria,
          arvioitavatKokonaisuudet: [],
          visible: false
        })
      })
      kategoriat = [
        ...new Map(
          kategoriat.map((kategoria: ArvioitavanKokonaisuudenKategoria) => [
            kategoria['id'],
            kategoria
          ])
        ).values()
      ]

      const aiemmatKokonaisuudet: ArvioitavaKokonaisuus[] = []

      // Liitetään arvioinnit arvioitaviin kokonaisuuksiin
      if (this.arvioinnit) {
        this.arvioinnit.forEach((arviointi) => {
          arviointi.arvioitavatKokonaisuudet.forEach((kokonaisuus) => {
            const arvioitavaKokonaisuus = arvioitavatKokonaisuudetMap?.find(
              (a: ArvioitavaKokonaisuus | null) => a?.id === kokonaisuus.arvioitavaKokonaisuusId
            )
            if (arvioitavaKokonaisuus) {
              arvioitavaKokonaisuus.arvioinnit.push({
                ...arviointi,
                arviointiasteikonTaso: kokonaisuus.arviointiasteikonTaso as number,
                itsearviointiArviointiasteikonTaso:
                  kokonaisuus.itsearviointiArviointiasteikonTaso as number,
                arvioitavaKokonaisuusId: kokonaisuus.arvioitavaKokonaisuusId
              })
            } else {
              // Aiemmat arvioinnit
              let aiempiKokonaisuus = aiemmatKokonaisuudet.find(
                (a) => a.id === kokonaisuus.arvioitavaKokonaisuusId
              )
              if (!aiempiKokonaisuus) {
                aiempiKokonaisuus = {
                  ...kokonaisuus.arvioitavaKokonaisuus,
                  arvioinnit: [],
                  visible: false
                }
                aiemmatKokonaisuudet.push(aiempiKokonaisuus)
              }
              aiempiKokonaisuus.arvioinnit.push({
                ...arviointi,
                arviointiasteikonTaso: kokonaisuus.arviointiasteikonTaso as number,
                itsearviointiArviointiasteikonTaso:
                  kokonaisuus.itsearviointiArviointiasteikonTaso as number,
                arvioitavaKokonaisuusId: kokonaisuus.arvioitavaKokonaisuusId
              })
            }
          })
        })
      }

      const aiemmatKategoriat = new Map<number, ArvioitavanKokonaisuudenKategoria>()
      aiemmatKokonaisuudet.forEach((k) => {
        if (k.kategoria.id) {
          if (!aiemmatKategoriat.get(k.kategoria.id)) {
            aiemmatKategoriat.set(k.kategoria.id, {
              ...k.kategoria,
              arvioitavatKokonaisuudet: [],
              visible: true
            })
          }
          aiemmatKategoriat.get(k.kategoria.id)?.arvioitavatKokonaisuudet.push(k)
        }
      })
      this.aiemmatKategoriat = [...aiemmatKategoriat.values()]
        .map((kategoria) => ({
          ...kategoria,
          arvioitavatKokonaisuudet: kategoria.arvioitavatKokonaisuudet.sort(
            (a: ArvioitavaKokonaisuus, b: ArvioitavaKokonaisuus) => sortByAsc(a.nimi, b.nimi)
          )
        }))
        .sort(
          (a: ArvioitavanKokonaisuudenKategoria, b: ArvioitavanKokonaisuudenKategoria) =>
            sortByAsc(a.jarjestysnumero, b.jarjestysnumero) || sortByAsc(a.nimi, b.nimi)
        )

      // Liitetään arvioitavat kokonaisuudet kategorioihin
      arvioitavatKokonaisuudetMap?.forEach((a: ArvioitavaKokonaisuus) => {
        const kategoria = kategoriat.find(
          (k: ArvioitavanKokonaisuudenKategoria) => k.id === a.kategoria.id
        )
        if (kategoria) {
          if (a.arvioinnit.length > 0) {
            kategoria.visible = true
          }
          kategoria.arvioitavatKokonaisuudet.push(a)
        }
      })
      return kategoriat
        .map((kategoria) => ({
          ...kategoria,
          arvioitavatKokonaisuudet: kategoria.arvioitavatKokonaisuudet.sort(
            (a: ArvioitavaKokonaisuus, b: ArvioitavaKokonaisuus) => sortByAsc(a.nimi, b.nimi)
          )
        }))
        .sort(
          (a: ArvioitavanKokonaisuudenKategoria, b: ArvioitavanKokonaisuudenKategoria) =>
            sortByAsc(a.jarjestysnumero, b.jarjestysnumero) || sortByAsc(a.nimi, b.nimi)
        )
    }

    get tyoskentelyjaksotFormatted() {
      return this.suoritusArvioinnitOptions?.tyoskentelyjaksot.map((tj: Tyoskentelyjakso) => ({
        ...tj,
        label: tyoskentelyjaksoLabel(this, tj)
      }))
    }

    get isFiltered() {
      return (
        this.selected.tyoskentelyjakso ||
        this.selected.arvioitavaKokonaisuus ||
        this.selected.kouluttajaOrVastuuhenkilo
      )
    }

    get arvioitavatKokonaisuudetSorted() {
      return this.suoritusArvioinnitOptions.arvioitavatKokonaisuudet.sort(
        (a: ArvioitavaKokonaisuus, b: ArvioitavaKokonaisuus) => sortByAsc(a.nimi, b.nimi)
      )
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .arvioinnit {
    max-width: 1024px;
  }

  ::v-deep .multiselect {
    .multiselect__option::after {
      display: none;
    }
  }
</style>
