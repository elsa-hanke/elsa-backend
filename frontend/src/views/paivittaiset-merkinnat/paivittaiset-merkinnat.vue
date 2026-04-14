<template>
  <div class="paivittaiset-merkinnat">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <div class="mb-4">
            <h1>{{ $t('paivittaiset-merkinnat') }}</h1>
            <p>{{ $t('paivittaiset-merkinnat-ingressi') }}</p>
            <elsa-button
              v-if="!account.impersonated"
              variant="primary"
              :to="{ name: 'uusi-paivittainen-merkinta' }"
              class="mb-2"
            >
              {{ $t('lisaa-merkinta') }}
            </elsa-button>
            <hr />
            <div v-if="!loading && merkinnat">
              <small class="text-uppercase">{{ $t('rajaa-merkintoja') }}</small>
              <b-container fluid class="px-0 mt-2" :class="{ 'mb-md-5': !anyFilterSelected }">
                <b-row>
                  <b-col md="4">
                    <elsa-form-group :label="$t('aihe')" class="mb-md-0">
                      <template #default="{ uid }">
                        <elsa-form-multiselect
                          :id="uid"
                          v-model="selected.aihe"
                          :options="aihekategoriat"
                          label="nimi"
                          track-by="jarjestysnumero"
                          @select="onAihekategoriaSelect"
                          @clearMultiselect="resetFilters"
                        ></elsa-form-multiselect>
                      </template>
                    </elsa-form-group>
                  </b-col>
                  <b-col md="4">
                    <elsa-form-group
                      :label="$t('ajankohta-alkaa')"
                      class="mb-md-0"
                      :class="{ 'mb-0': anyFilterSelected }"
                    >
                      <template #default="{ uid }">
                        <elsa-form-datepicker
                          :id="uid"
                          ref="ajankohtaAlkaa"
                          :value="selected.ajankohtaAlkaa"
                          :max="maxAjankohtaAlkaa"
                          :max-error-text="$t('ajankohta-ei-voi-alkaa-paattymisen-jalkeen')"
                          :required="false"
                          @input="onAjankohtaAlkaaSelect"
                        />
                      </template>
                    </elsa-form-group>
                  </b-col>
                  <b-col md="4">
                    <elsa-form-group
                      :label="$t('ajankohta-paattyy')"
                      class="mb-md-0"
                      :class="{ 'mb-0': anyFilterSelected }"
                    >
                      <template #default="{ uid }">
                        <elsa-form-datepicker
                          :id="uid"
                          ref="ajankohtaPaattyy"
                          :value="selected.ajankohtaPaattyy"
                          :min="minAjankohtaPaattyy"
                          :min-error-text="$t('paattymispaiva-ei-voi-olla-ennen-alkamispaivaa')"
                          :required="false"
                          class="datepicker-range"
                          @input="onAjankohtaPaattyySelect"
                        />
                      </template>
                    </elsa-form-group>
                  </b-col>
                </b-row>
                <b-row>
                  <b-col>
                    <div class="d-flex flex-row-reverse">
                      <elsa-button
                        v-if="anyFilterSelected"
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
              <b-alert v-if="merkinnat.page.totalElements === 0" variant="dark" show>
                <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                <span>
                  {{ $t('ei-paivittaisia-merkintoja') }}
                </span>
              </b-alert>
              <div v-else>
                <div
                  v-for="(paivittainenMerkinta, index) in merkinnat.content"
                  :key="index"
                  class="d-flex flex-column border rounded p-2 mb-2"
                >
                  <elsa-button
                    :to="{
                      name: 'paivittainen-merkinta',
                      params: { paivakirjamerkintaId: paivittainenMerkinta.id }
                    }"
                    variant="link"
                    class="d-flex p-0 border-0 shadow-none font-weight-500 text-left"
                  >
                    {{ paivittainenMerkinta.oppimistapahtumanNimi }}
                  </elsa-button>
                  <div class="d-flex flex-wrap">
                    <small class="mr-2">{{ $date(paivittainenMerkinta.paivamaara) }}</small>
                    <b-badge
                      v-for="aihe in paivittainenMerkinta.aihekategoriat"
                      :key="aihe.id"
                      pill
                      variant="light"
                      class="font-weight-400 mr-2 mb-2"
                    >
                      {{
                        `${aihe.nimi}${
                          aihe.teoriakoulutus && paivittainenMerkinta.teoriakoulutus
                            ? `: ${paivittainenMerkinta.teoriakoulutus.koulutuksenNimi}`
                            : ''
                        }${aihe.muunAiheenNimi ? `: ${paivittainenMerkinta.muunAiheenNimi}` : ''}`
                      }}
                    </b-badge>
                  </div>
                  <div class="line-clamp">
                    {{ paivittainenMerkinta.reflektio }}
                  </div>
                </div>
                <elsa-pagination
                  :current-page="currentPage"
                  :per-page="perPage"
                  :rows="rows"
                  @update:currentPage="onPageInput"
                />
              </div>
            </div>
            <div v-else class="text-center">
              <b-spinner variant="primary" :label="$t('ladataan')" />
            </div>
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Vue, Component } from 'vue-property-decorator'

  import { getPaivakirjamerkinnatRajaimet, getPaivittaisetMerkinnat } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import store from '@/store'
  import {
    Page,
    PaivakirjaAihekategoria,
    Paivakirjamerkinta,
    PaivakirjamerkintaRajaimet
  } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaFormDatepicker,
      ElsaPagination
    }
  })
  export default class PaivittaisetMerkinnat extends Vue {
    $refs!: {
      ajankohtaAlkaa: ElsaFormDatepicker
      ajankohtaPaattyy: ElsaFormDatepicker
    }

    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('paivittaiset-merkinnat'),
        active: true
      }
    ]
    loading = true
    selected: {
      aihekategoriaId: number | null
      ajankohtaAlkaa: string | null
      ajankohtaPaattyy: string | null
      aihe: PaivakirjaAihekategoria | null
    } = {
      aihekategoriaId: null,
      ajankohtaAlkaa: null,
      ajankohtaPaattyy: null,
      aihe: null
    }
    currentPage = 1
    perPage = 20
    rajaimet: PaivakirjamerkintaRajaimet | null = null
    merkinnat: Page<Paivakirjamerkinta> | null = null

    async mounted() {
      await Promise.all([this.fetchRajaimet(), this.fetch()])
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async fetchRajaimet() {
      try {
        this.rajaimet = (await getPaivakirjamerkinnatRajaimet()).data
      } catch {
        toastFail(this, this.$t('paivittaisten-merkintojen-hakeminen-epaonnistui'))
      }
    }

    async fetch() {
      try {
        const merkinnat = (
          await getPaivittaisetMerkinnat({
            page: this.currentPage - 1,
            size: this.perPage,
            sort: 'paivamaara,id,desc',
            ...(this.selected.aihekategoriaId
              ? { 'aihekategoriaId.equals': this.selected.aihekategoriaId }
              : {}),
            ...(this.selected.ajankohtaAlkaa
              ? { 'paivamaara.greaterThanOrEqual': this.selected.ajankohtaAlkaa }
              : {}),
            ...(this.selected.ajankohtaPaattyy
              ? { 'paivamaara.lessThanOrEqual': this.selected.ajankohtaPaattyy }
              : {})
          })
        ).data
        this.merkinnat = {
          ...merkinnat,
          content: merkinnat.content.map((merkinta) => ({
            ...merkinta,
            aihekategoriat: merkinta.aihekategoriat.sort(
              (a, b) => (a.jarjestysnumero ?? 0) - (b.jarjestysnumero ?? 0)
            )
          }))
        }
      } catch {
        toastFail(this, this.$t('paivittaisten-merkintojen-hakeminen-epaonnistui'))
      }
    }

    onPageInput(value: number) {
      this.currentPage = value
      this.fetch()
    }

    onAihekategoriaSelect(selected: PaivakirjaAihekategoria) {
      this.currentPage = 1
      this.selected.aihekategoriaId = selected.id ?? null
      this.fetch()
    }

    onAjankohtaAlkaaSelect(value: string) {
      this.selected.ajankohtaAlkaa = value

      if (!this.$refs.ajankohtaAlkaa.validateForm()) {
        return
      }
      this.currentPage = 1
      this.fetch()
    }

    onAjankohtaPaattyySelect(value: string) {
      this.selected.ajankohtaPaattyy = value

      if (!this.$refs.ajankohtaPaattyy.validateForm()) {
        return
      }
      this.currentPage = 1
      this.fetch()
    }

    resetFilters() {
      this.currentPage = 1
      this.selected = {
        aihekategoriaId: null,
        ajankohtaAlkaa: null,
        ajankohtaPaattyy: null,
        aihe: null
      }
      this.$refs.ajankohtaAlkaa.resetValue()
      this.$refs.ajankohtaPaattyy.resetValue()
      this.fetch()
    }

    get maxAjankohtaAlkaa() {
      return this.selected.ajankohtaPaattyy
    }

    get minAjankohtaPaattyy() {
      return this.selected.ajankohtaAlkaa
    }

    get rows() {
      return this.merkinnat?.page.totalElements ?? 0
    }

    get aihekategoriat() {
      return this.rajaimet?.aihekategoriat ?? []
    }

    get anyFilterSelected() {
      return (
        this.selected.aihekategoriaId ||
        this.selected.ajankohtaAlkaa ||
        this.selected.ajankohtaPaattyy
      )
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .paivittaiset-merkinnat {
    max-width: 1024px;
  }

  .line-clamp {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    @include media-breakpoint-down(sm) {
      -webkit-line-clamp: 4;
    }
  }

  .datepicker-range::before {
    content: '–';
    position: absolute;
    left: -1.5rem;
    padding: 0.375rem 0.75rem;
    @include media-breakpoint-down(xs) {
      display: none;
    }
  }
</style>
