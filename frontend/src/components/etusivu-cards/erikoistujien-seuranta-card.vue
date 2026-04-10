<template>
  <div>
    <b-card-skeleton :header="seurantaTitle" :loading="loading">
      <div v-if="rajaimet != null && erikoistujat">
        <div v-if="showKouluttajaKuvaus" class="mb-4 mb-lg-3">
          {{ $t('erikoistujien-seuranta-kouluttaja-kuvaus') }}
        </div>
        <b-row lg>
          <b-col cols="12" lg="4">
            <elsa-search-input
              class="mt-lg-3 mb-3 hakutermi"
              :hakutermi.sync="hakutermi"
              :placeholder="$t('hae-erikoistujan-nimella')"
            />
          </b-col>
          <b-col cols="12" lg="4">
            <!--
            <div v-if="rajaimet?.erikoisalat.length > 1" class="erikoisalat">
              <elsa-form-group :label="$t('erikoisala')" class="mb-3">
                <template #default="{ uid }">
                  <elsa-form-multiselect
                    :id="uid"
                    v-model="filtered.erikoisala"
                    :options="erikoisalatSorted"
                  ></elsa-form-multiselect>
                </template>
              </elsa-form-group>
            </div>
            -->
          </b-col>
          <b-col cols="12" lg="4">
            <div class="jarjestys">
              <elsa-form-group :label="$t('jarjestys')" class="mb-3">
                <template #default="{ uid }">
                  <elsa-form-multiselect
                    :id="uid"
                    v-model="sortBy"
                    :options="sortFields"
                    label="name"
                    track-by="name"
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
        <b-row>
          <b-col>
            <b-alert v-if="rows === 0" variant="dark" show>
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span v-if="hakutermi.length > 0">
                {{ $t('ei-hakutuloksia') }}
              </span>
              <span v-else>
                {{ $t('ei-seurattavia-erikoistujia') }}
              </span>
            </b-alert>
            <b-list-group>
              <b-list-group-item v-for="(eteneminen, index) in erikoistujat.content" :key="index">
                <b-row>
                  <b-col cols="12" lg="6">
                    <elsa-button
                      variant="link"
                      class="p-0"
                      @click="vaihdaRooli(eteneminen.opintooikeusId)"
                    >
                      <div>
                        {{ eteneminen.erikoistuvaLaakariEtuNimi }}
                        {{ eteneminen.erikoistuvaLaakariSukuNimi }}
                        <span v-if="eteneminen.erikoistuvaLaakariSyntymaaika != null">
                          ({{ $date(eteneminen.erikoistuvaLaakariSyntymaaika) }})
                        </span>
                      </div>
                    </elsa-button>
                    <div class="mb-2 text-size-sm">
                      {{ eteneminen.erikoisala }}. {{ eteneminen.asetus }}
                    </div>
                  </b-col>
                  <b-col cols="12" lg="6">
                    <div class="text-lg-right text-size-sm mb-2">
                      <span class="d-block d-lg-inline-block">
                        {{ $t('koejakso') }}:
                        <span
                          :class="
                            koejaksoTyyli(
                              eteneminen.koejaksoTila,
                              eteneminen.opintooikeudenPaattymispaiva
                            )
                          "
                        >
                          {{ koejaksoTila(eteneminen.koejaksoTila) }}
                        </span>
                      </span>
                      <span v-if="$screen.lg">{{ ' | ' }}</span>
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
                  <b-col cols="12" lg="3" class="pr-3 mb-4">
                    <div class="text-uppercase font-weight-normal mb-1 text-size-sm">
                      {{ $t('tyoskentelyaika-yht') }}
                    </div>
                    <div v-b-toggle="`tyoskentelyaika-toggle-${index}`">
                      <b-row>
                        <b-col cols="11" class="pr-2">
                          <elsa-progress-bar
                            :value="
                              eteneminen.tyoskentelyjaksoTilastot.koulutustyypit.yhteensaSuoritettu
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
                  <b-col cols="6" lg="auto" class="pl-lg-3 pr-1 pr-lg-3 mb-2">
                    <div class="text-uppercase font-weight-normal mb-1 text-size-sm">
                      {{ $t('arviointien-ka') }}
                    </div>
                    <div v-if="eteneminen.arviointienKeskiarvo != null">
                      <span class="font-weight-bold">
                        {{ keskiarvoFormatted(eteneminen.arviointienKeskiarvo) }}
                      </span>
                      / 5
                    </div>
                    <div v-else>- / 5</div>
                  </b-col>
                  <b-col cols="6" lg="auto" class="mb-2 pl-1 pl-lg-3">
                    <div class="text-uppercase font-weight-normal text-size-sm">
                      {{ $t('arv-kokonaisuutta') }}
                    </div>
                    <div class="mb-1 text-size-sm">({{ $t('sis-vah-1-arvion') }})</div>
                    <span class="font-weight-bold">
                      {{ eteneminen.arviointienLkm }} /
                      {{ eteneminen.arvioitavienKokonaisuuksienLkm }}
                    </span>
                  </b-col>
                  <b-col cols="6" lg="auto" class="pr-1 pr-lg-3">
                    <div class="text-uppercase font-weight-normal mb-1 text-size-sm">
                      {{ $t('seurantajaksot') }}
                    </div>
                    <span class="font-weight-bold">{{ eteneminen.seurantajaksotLkm }}</span>
                    <span>{{ ' ' }}{{ $t('kpl') }}</span>
                    <span v-if="eteneminen.seurantajaksonHuoletLkm > 0">
                      <span>,</span>
                      {{ eteneminen.seurantajaksonHuoletLkm }} {{ $t('sis-huolia') }}
                    </span>
                  </b-col>
                  <b-col cols="6" lg="auto" class="pr-1 pl-1 pl-lg-3">
                    <div class="text-uppercase font-weight-normal mb-1 text-size-sm">
                      {{ $t('suoritemerkinnat') }}
                    </div>
                    <span class="font-weight-bold">{{ eteneminen.suoritemerkinnatLkm }}</span>
                    <span v-if="eteneminen.vaaditutSuoritemerkinnatLkm > 0">
                      / {{ eteneminen.vaaditutSuoritemerkinnatLkm }}
                    </span>
                    {{ $t('kpl') }}
                  </b-col>
                </b-row>
              </b-list-group-item>
            </b-list-group>
            <elsa-pagination
              :current-page.sync="currentPage"
              :per-page="perPage"
              :rows="rows"
              :style="{ 'max-width': '1420px' }"
              @update:currentPage="onPageInput"
            />
          </b-col>
        </b-row>
      </div>
    </b-card-skeleton>
  </div>
</template>

<script lang="ts">
  import { Component, Mixins, Prop, Watch } from 'vue-property-decorator'

  import {
    getErikoistujienSeuranta as getErikoistujienSeurantaKouluttaja,
    getErikoistujienSeurantaKouluttajaRajaimet
  } from '@/api/kouluttaja'
  import {
    getErikoistujienSeuranta as getErikoistujienSeurantaVastuuhenkilo,
    getErikoistujienSeurantaVastuuhenkiloRajaimet
  } from '@/api/vastuuhenkilo'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import ElsaProgressBar from '@/components/progress-bar/progress-bar.vue'
  import ElsaSearchInput from '@/components/search-input/search-input.vue'
  import ErikoistujienSeurantaMixin from '@/mixins/erikoistujien-seuranta'
  import {
    ErikoistujanEteneminen,
    ErikoistujienSeurantaVastuuhenkiloRajaimet,
    Page,
    SortByEnum
  } from '@/types'
  import { ErikoistuvanSeurantaJarjestys } from '@/utils/constants'
  import { getKeskiarvoFormatted } from '@/utils/keskiarvoFormatter'
  import { sortByAsc } from '@/utils/sort'
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
  export default class ErikoistujienSeurantaCard extends Mixins(ErikoistujienSeurantaMixin) {
    rajaimet: ErikoistujienSeurantaVastuuhenkiloRajaimet | null = null
    erikoistujat: Page<ErikoistujanEteneminen> | null = null

    @Prop({ required: false, default: false })
    showKouluttajaKuvaus!: boolean

    sortFields: SortByEnum[] = [
      {
        name: this.$t('opintooikeus-paattymassa'),
        value: ErikoistuvanSeurantaJarjestys.OPINTOOIKEUS_PAATTYMASSA
      } as SortByEnum,
      {
        name: this.$t('opintooikeus-alkaen'),
        value: ErikoistuvanSeurantaJarjestys.OPINTOOIKEUS_ALKAEN
      } as SortByEnum,
      // {
      //   name: this.$t('tyoskentelyaikaa-vahiten'),
      //   value: ErikoistuvanSeurantaJarjestys.TYOSKENTELYAIKAA_VAHITEN
      // } as SortByEnum,
      // {
      //   name: this.$t('tyoskentelyaikaa-eniten'),
      //   value: ErikoistuvanSeurantaJarjestys.TYOSKENTELYAIKAA_ENITEN
      // } as SortByEnum,
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

    filtered: {
      nimi: string | null
      naytaPaattyneet: boolean | null
      sortBy: string | null
    } = {
      nimi: null,
      naytaPaattyneet: null,
      sortBy: null
    }
    currentPage = 1
    perPage = 20
    debounce?: number

    hakutermi = ''
    loading = true

    async mounted() {
      try {
        await Promise.all([this.fetchRajaimet(), this.fetch()])
      } catch {
        toastFail(this, this.$t('erikoistujien-seurannan-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    async fetchRajaimet() {
      this.rajaimet = this.$isVastuuhenkilo()
        ? (await getErikoistujienSeurantaVastuuhenkiloRajaimet()).data
        : (await getErikoistujienSeurantaKouluttajaRajaimet()).data
    }

    async fetch() {
      try {
        this.erikoistujat = this.$isVastuuhenkilo()
          ? (
              await getErikoistujienSeurantaVastuuhenkilo({
                page: this.currentPage - 1,
                size: this.perPage,
                sort: this.filtered.sortBy ?? 'opintooikeudenPaattymispaiva,asc',
                ...(this.filtered.nimi ? { 'nimi.contains': this.filtered.nimi } : {}),
                ...(this.filtered.naytaPaattyneet
                  ? { naytaPaattyneet: this.filtered.naytaPaattyneet }
                  : {})
              })
            ).data
          : (
              await getErikoistujienSeurantaKouluttaja({
                page: this.currentPage - 1,
                size: this.perPage,
                sort: this.filtered.sortBy ?? 'opintooikeudenPaattymispaiva,asc',
                ...(this.filtered.nimi ? { 'nimi.contains': this.filtered.nimi } : {}),
                ...(this.filtered.naytaPaattyneet
                  ? { naytaPaattyneet: this.filtered.naytaPaattyneet }
                  : {})
              })
            ).data
      } catch (err) {
        toastFail(this, this.$t('erikoistujien-seurannan-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get seurantaTitle() {
      if (this.rajaimet == null) {
        return ''
      }
      let result = ''
      this.rajaimet.kayttajaYliopistoErikoisalat.forEach((kayttajaErikoisala) => {
        result +=
          this.$t(`yliopisto-nimi.${kayttajaErikoisala.yliopistoNimi}`) +
          ': ' +
          kayttajaErikoisala.erikoisalat.join(', ') +
          '. '
      })
      return result
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
        case ErikoistuvanSeurantaJarjestys.TYOSKENTELYAIKAA_VAHITEN:
          this.filtered.sortBy = 'tyoskentelyaikaYhteensa,asc'
          break
        case ErikoistuvanSeurantaJarjestys.TYOSKENTELYAIKAA_ENITEN:
          this.filtered.sortBy = 'tyoskentelyaikaYhteensa,desc'
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

    get erikoisalatSorted() {
      return this.rajaimet?.erikoisalat.sort((a, b) => sortByAsc(a, b))
    }

    onPageInput(value: number) {
      this.currentPage = value
      this.fetch()
    }

    private async onResultsFiltered() {
      this.loading = true
      this.currentPage = 1
      await this.fetch()
      this.loading = false
    }

    get rows() {
      return this.erikoistujat?.page.totalElements ?? 0
    }

    keskiarvoFormatted(keskiarvo: number) {
      return getKeskiarvoFormatted(keskiarvo)
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

  .jarjestys::v-deep .form-group,
  .erikoisalat::v-deep .form-group {
    label {
      font-weight: 300;
      text-transform: uppercase;
      font-size: $font-size-sm;
      margin-bottom: 0;
    }
  }
</style>
