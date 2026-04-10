<template>
  <div>
    <b-card-skeleton :header="yliopistoNimi" :loading="initializing">
      <div v-if="!initializing">
        <b-row lg>
          <b-col cols="12" lg="4">
            <elsa-search-input
              class="mt-lg-3 hakutermi mb-0"
              :hakutermi.sync="hakutermi"
              :placeholder="$t('hae-koulutettavan-nimella')"
            />
          </b-col>
          <b-col cols="12" lg="3"></b-col>

          <b-col cols="12" lg="3" class="">
            <div class="filter">
              <elsa-form-group :label="$t('jarjestys')" class="mb-0">
                <template #default="{ uid }">
                  <elsa-form-multiselect
                    :id="uid"
                    v-model="sortBy"
                    :options="sortFields"
                    label="name"
                    :taggable="true"
                    @select="onSortBySelect"
                  >
                    <template #option="{ option }">
                      <div v-if="option.name">{{ option.name }}</div>
                    </template>
                  </elsa-form-multiselect>
                </template>
              </elsa-form-group>
            </div>
          </b-col>
        </b-row>
        <b-row lg>
          <b-col cols="12" lg="4">
            <b-form-checkbox
              v-model="filtered.naytaPaattyneet"
              class="mb-4"
              @input="onNaytaPaattyneetSelect"
            >
              {{ $t('nayta-paattyneet-opintooikeudet') }}
            </b-form-checkbox>
          </b-col>
        </b-row>
        <div v-if="!loadingResults && koulutettavat">
          <b-row>
            <b-col>
              <b-alert v-if="rows === 0" variant="dark" show>
                <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                <span v-if="hakutermi.length > 0 || filtered.erikoisala || filtered.asetus">
                  {{ $t('ei-hakutuloksia') }}
                </span>
                <span v-else>
                  {{ $t('ei-seurattavia-koulutettavia') }}
                </span>
              </b-alert>
              <b-list-group>
                <b-list-group-item
                  v-for="(eteneminen, index) in koulutettavat.content"
                  :key="index"
                >
                  <b-row>
                    <b-col cols="12" lg="6">
                      <elsa-button
                        variant="link"
                        class="p-0 border-0"
                        @click="vaihdaRooli(eteneminen.opintooikeusId)"
                      >
                        {{ eteneminen.etunimi }}
                        {{ eteneminen.sukunimi }}
                        <span v-if="eteneminen.syntymaaika != null">
                          ({{ $date(eteneminen.syntymaaika) }})
                        </span>
                      </elsa-button>
                      <div class="mb-2 text-size-sm">
                        {{ eteneminen.erikoisala }}. {{ eteneminen.asetus }}
                      </div>
                    </b-col>
                    <b-col cols="12" lg="6">
                      <div class="text-lg-right text-size-sm mb-2">
                        <span class="d-block d-lg-inline-block">
                          {{ $t('opintooikeus') }}:
                          {{ $date(eteneminen.opintooikeudenMyontamispaiva) }} -
                          <span :class="opintoOikeusTyyli(eteneminen.opintooikeudenPaattymispaiva)">
                            {{ $date(eteneminen.opintooikeudenPaattymispaiva) }}
                          </span>
                        </span>
                      </div>
                    </b-col>
                  </b-row>
                  <b-row>
                    <b-col cols="12" lg="5" class="pr-3 mb-4">
                      <div class="text-uppercase font-weight-normal mb-1 text-size-sm">
                        {{ $t('tyoskentelyaika-yht') }}
                      </div>
                      <div v-b-toggle="`tyoskentelyaika-toggle-${index}`">
                        <b-row>
                          <b-col cols="11" class="pr-2">
                            <elsa-progress-bar
                              :value="
                                eteneminen.tyoskentelyjaksoTilastot.koulutustyypit
                                  .yhteensaSuoritettu
                              "
                              :min-required="
                                eteneminen.tyoskentelyjaksoTilastot.koulutustyypit
                                  .yhteensaVaadittuVahintaan
                              "
                              :color="'#41b257'"
                              :background-color="'#b3e1bc'"
                              :text-color="'black'"
                              :show-required-duration="true"
                            />
                          </b-col>
                          <b-col cols="1" class="pl-0 pr-0">
                            <span class="closed">
                              <font-awesome-icon icon="chevron-down" class="text-muted" />
                            </span>
                            <span class="open">
                              <font-awesome-icon icon="chevron-up" class="text-muted" />
                            </span>
                          </b-col>
                        </b-row>
                      </div>
                      <b-collapse :id="`tyoskentelyaika-toggle-${index}`">
                        <b-row>
                          <b-col cols="11" class="pr-2">
                            <span
                              v-for="(row, tilastotIndex) in barValues(
                                eteneminen.tyoskentelyjaksoTilastot
                              )"
                              :key="tilastotIndex"
                            >
                              <div class="text-size-sm mt-1">{{ row.text }}</div>
                              <elsa-progress-bar
                                :value="row.value"
                                :min-required="row.minRequired"
                                :color="row.color"
                                :background-color="row.backgroundColor"
                                :show-required-duration="true"
                              />
                            </span>
                          </b-col>
                        </b-row>
                      </b-collapse>
                    </b-col>
                    <b-col cols="6" lg="auto" class="pl-lg-4 pr-1 pr-lg-4">
                      <div class="text-uppercase font-weight-normal mb-1 text-size-sm">
                        {{ $t('teoriakoulutus') }}
                      </div>
                      <span :class="eteneminen.teoriakoulutuksetSuoritettu ? 'text-success' : ''">
                        {{
                          eteneminen.teoriakoulutuksetSuoritettu
                            ? $t('suoritettu')
                            : $t('ei-suoritettu')
                        }}
                      </span>
                    </b-col>
                    <b-col cols="6" lg="auto" class="pr-1 pl-1 pl-lg-4">
                      <div class="text-uppercase font-weight-normal mb-1 text-size-sm">
                        {{ $t('yek.koulutuksen-tila') }}
                      </div>
                      <span :class="eteneminen.yekSuoritettu ? 'text-success' : ''">
                        {{ eteneminen.yekSuoritettu ? $t('yek.valmistunut') : $t('yek.kesken') }}
                      </span>
                    </b-col>
                  </b-row>
                </b-list-group-item>
              </b-list-group>
              <elsa-pagination
                :current-page="currentPage"
                :per-page="perPage"
                :rows="rows"
                @update:currentPage="onPageInput"
              />
            </b-col>
          </b-row>
        </div>
        <div v-else class="text-center mt-3">
          <b-spinner variant="primary" :label="$t('ladataan')" />
        </div>
      </div>
    </b-card-skeleton>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Mixins, Watch } from 'vue-property-decorator'

  import { getKoulutettavienSeurantaList as getKoulutettavienSeurantaListVastuuhenkilo } from '@/api/vastuuhenkilo'
  import { getKoulutettavienSeurantaList as getKoulutettavienSeurantaListVirkailija } from '@/api/virkailija'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaProgressBar from '@/components/progress-bar/progress-bar.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import KoulutettavienSeurantaMixin from '@/mixins/koulutettavien-seuranta'
  import { Page, Erikoisala, Asetus, SortByEnum, KoulutettavanEteneminen } from '@/types'
  import { ERIKOISALA_YEK_ID, ErikoistuvanSeurantaJarjestys } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaPagination,
      ElsaProgressBar,
      ElsaSearchInput
    }
  })
  export default class YekKoulutettavienSeurantaCard extends Mixins(KoulutettavienSeurantaMixin) {
    @Prop({ required: true })
    yliopisto!: string

    sortFields: SortByEnum[] = [
      {
        name: this.$t('opintooikeus-paattymassa'),
        value: ErikoistuvanSeurantaJarjestys.OPINTOOIKEUS_PAATTYMASSA
      } as SortByEnum,
      {
        name: this.$t('opintooikeus-alkaen'),
        value: ErikoistuvanSeurantaJarjestys.OPINTOOIKEUS_ALKAEN
      } as SortByEnum,
      {
        name: this.$t('sukunimi-a-o'),
        value: ErikoistuvanSeurantaJarjestys.SUKUNIMI_ASC
      } as SortByEnum,
      {
        name: this.$t('sukunimi-o-a'),
        value: ErikoistuvanSeurantaJarjestys.SUKUNIMI_DESC
      } as SortByEnum
    ]
    sortBy = this.sortFields[0]

    initializing = true
    loadingResults = false

    filtered: {
      nimi: string | null
      erikoisala: Erikoisala | null
      asetus: Asetus | null
      naytaPaattyneet: boolean | null
      sortBy: string | null
    } = {
      nimi: null,
      erikoisala: null,
      asetus: null,
      naytaPaattyneet: null,
      sortBy: null
    }
    currentPage = 1
    perPage = 20
    debounce?: number

    hakutermi = ''
    koulutettavat: Page<KoulutettavanEteneminen> | null = null

    async mounted() {
      try {
        await this.fetch()
      } catch {
        toastFail(this, this.$t('yek.koulutettavien-seurannan-hakeminen-epaonnistui'))
      }
      this.initializing = false
    }

    async fetch() {
      const params = {
        page: this.currentPage - 1,
        size: this.perPage,
        sort: this.filtered.sortBy ?? 'opintooikeudenPaattymispaiva,asc',
        ...(this.filtered.nimi ? { 'nimi.contains': this.filtered.nimi } : {}),
        ...{ 'erikoisalaId.equals': ERIKOISALA_YEK_ID },
        ...(this.filtered.asetus?.id ? { 'asetusId.equals': this.filtered.asetus.id } : {}),
        ...(this.filtered.naytaPaattyneet ? { naytaPaattyneet: this.filtered.naytaPaattyneet } : {})
      }
      this.koulutettavat = this.$isVirkailija()
        ? (await getKoulutettavienSeurantaListVirkailija(params)).data
        : (await getKoulutettavienSeurantaListVastuuhenkilo(params)).data
    }

    get rows() {
      return this.koulutettavat?.page.totalElements ?? 0
    }

    @Watch('hakutermi')
    onPropertyChanged(value: string) {
      this.debounceSearch(value)
    }

    debounceSearch(value: string) {
      clearTimeout(this.debounce)
      this.debounce = setTimeout(() => {
        this.filtered.nimi = value
        this.onResultsFiltered()
      }, 400)
    }

    async onErikoisalaSelect(erikoisala: Erikoisala) {
      this.filtered.erikoisala = erikoisala
      await this.onResultsFiltered()
    }

    async onErikoisalaReset() {
      this.filtered.erikoisala = null
      await this.onResultsFiltered()
    }

    async onAsetusSelect(asetus: Asetus) {
      this.filtered.asetus = asetus
      await this.onResultsFiltered()
    }

    async onAsetusReset() {
      this.filtered.asetus = null
      await this.onResultsFiltered()
    }

    async onNaytaPaattyneetSelect() {
      await this.onResultsFiltered()
    }

    async onSortBySelect(sortByEnum: SortByEnum) {
      switch (sortByEnum.value) {
        case ErikoistuvanSeurantaJarjestys.OPINTOOIKEUS_PAATTYMASSA:
          this.filtered.sortBy = 'opintooikeudenPaattymispaiva,asc'
          break
        case ErikoistuvanSeurantaJarjestys.OPINTOOIKEUS_ALKAEN:
          this.filtered.sortBy = 'opintooikeudenMyontamispaiva,asc'
          break
        case ErikoistuvanSeurantaJarjestys.SUKUNIMI_ASC:
          this.filtered.sortBy = 'erikoistuvaLaakari.kayttaja.user.lastName,asc'
          break
        case ErikoistuvanSeurantaJarjestys.SUKUNIMI_DESC:
          this.filtered.sortBy = 'erikoistuvaLaakari.kayttaja.user.lastName,desc'
          break
      }
      this.onResultsFiltered()
    }

    onPageInput(value: number) {
      this.currentPage = value
      this.fetch()
    }

    private async onResultsFiltered() {
      this.loadingResults = true
      this.currentPage = 1
      await this.fetch()
      this.loadingResults = false
    }

    get yliopistoNimi() {
      if (this.yliopisto == null) {
        return ''
      } else return this.$t(`yliopisto-nimi.${this.yliopisto}`)
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

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

  .hakutermi::v-deep .search-input {
    margin-top: 0 !important;
  }

  .filter::v-deep .form-group {
    label {
      font-weight: 300;
      text-transform: uppercase;
      font-size: $font-size-sm;
      margin-bottom: 0;
    }
  }
</style>
