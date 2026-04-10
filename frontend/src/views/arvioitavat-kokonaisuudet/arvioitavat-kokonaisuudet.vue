<template>
  <div v-if="!loading" class="arvioitavat-kokonaisuudet">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1 class="mt-3">{{ $t('arvioitavat-kokonaisuudet') }}</h1>
        </b-col>
      </b-row>
      <div v-if="erikoisalat.length > 1 && !$isErikoistuva()" class="filter mt-3">
        <elsa-form-group :label="$t('erikoisala')" class="mb-4">
          <template #default="{ uid }">
            <elsa-form-multiselect
              :id="uid"
              v-model="erikoisala"
              :options="erikoisalat"
              label="nimi"
              @select="onErikoisalaSelect"
              @clearMultiselect="onErikoisalaReset"
            ></elsa-form-multiselect>
          </template>
        </elsa-form-group>
      </div>
      <div v-if="!$isKouluttaja() && !$isVastuuhenkilo()">
        <arvioitavat-kokonaisuudet-lista
          :arvioitavat-kokonaisuudet="arvioitavatKokonaisuudet"
          :locale="$i18n.locale"
        />
      </div>
      <div v-else>
        <h3 class="mt-6">{{ selectedErikoisalaName }}</h3>
        <b-tabs
          v-if="!erikoisalatLoading && endpointUrl"
          v-model="tabIndex"
          content-class="mt-3"
          :no-fade="true"
        >
          <b-tab :title="$t('voimassa-olevat-kokonaisuudet')">
            <arvioitavat-kokonaisuudet-lista-vastuuhenkilo
              :url="endpointUrl"
              :erikoisala="selectedErikoisala"
              :locale="$i18n.locale"
            />
          </b-tab>
          <b-tab :title="$t('voimassaolo-paattynyt')" lazy>
            <arvioitavat-kokonaisuudet-lista-vastuuhenkilo
              :url="endpointUrl"
              :erikoisala="selectedErikoisala"
              :voimassaolevat="false"
              :locale="$i18n.locale"
            />
          </b-tab>
        </b-tabs>
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaAccordian from '@/components/accordian/accordian.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import store from '@/store'
  import { ArvioitavaKokonaisuus } from '@/types'
  import { toastFail } from '@/utils/toast'
  import ArvioitavatKokonaisuudetListaVastuuhenkilo from '@/views/arvioitavat-kokonaisuudet/arvioitavat-kokonaisuudet-lista-vastuuhenkilo.vue'
  import ArvioitavatKokonaisuudetLista from '@/views/arvioitavat-kokonaisuudet/arvioitavat-kokonaisuudet-lista.vue'

  type ErikoisalaSelectItem = {
    nimi: string
    id: number
  }

  @Component({
    components: {
      ElsaAccordian,
      ArvioitavatKokonaisuudetLista,
      ArvioitavatKokonaisuudetListaVastuuhenkilo,
      ElsaFormMultiselect,
      ElsaFormGroup
    }
  })
  export default class ArvioitavatKokonaisuudet extends Vue {
    endpointUrl = ''
    erikoisalatUrl = ''
    erikoisala: ErikoisalaSelectItem | null = null
    arvioitavatKokonaisuudet: ArvioitavaKokonaisuus[] = []
    loading = false
    erikoisalatLoading = false
    showErikoisalaDropdown = false
    erikoisalat: ErikoisalaSelectItem[] = []
    selectedErikoisala: number | null = null
    selectedErikoisalaName = ''
    tabIndex = 0
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arvioitavat-kokonaisuudet'),
        active: true
      }
    ]

    async mounted() {
      await this.fetch()
    }

    get account() {
      return store.getters['auth/account']
    }

    async fetch() {
      try {
        this.loading = true
        this.erikoisalatLoading = true
        if (this.$isKouluttaja()) {
          this.endpointUrl = 'kouluttaja/arvioitavatkokonaisuudet'
          this.erikoisalatUrl = 'kouluttaja/erikoisalat'
        } else if (this.$isVastuuhenkilo()) {
          this.endpointUrl = 'vastuuhenkilo/arvioitavatkokonaisuudet'
          this.erikoisalatUrl = 'vastuuhenkilo/erikoisalat'
        } else {
          this.endpointUrl = 'erikoistuva-laakari/arvioitavatkokonaisuudet'
        }
        if (this.endpointUrl && (this.$isKouluttaja() || this.$isVastuuhenkilo())) {
          this.erikoisalat = (await axios.get(this.erikoisalatUrl)).data
          if (this.erikoisalat[0].id) {
            this.erikoisala = this.erikoisalat[0]
            this.selectedErikoisala = this.erikoisalat[0].id
            this.selectedErikoisalaName = this.erikoisalat[0].nimi
          }
          this.erikoisalatLoading = false
        } else {
          this.arvioitavatKokonaisuudet = (await axios.get(this.endpointUrl)).data
        }
        this.loading = false
      } catch {
        toastFail(this, this.$t('arvioitavien-kokonaisuuksien-hakeminen-epaonnistui'))
        this.loading = false
      }
    }

    onErikoisalaSelect(erikoisala: ErikoisalaSelectItem) {
      this.selectedErikoisala = erikoisala.id
      this.selectedErikoisalaName = erikoisala.nimi
    }

    onErikoisalaReset() {
      this.selectedErikoisala = null
      this.selectedErikoisalaName = ''
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  .arvioitavat-kokonaisuudet {
    max-width: 1024px;
  }
</style>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .filter::v-deep .form-group {
    label {
      font-weight: 300;
      text-transform: uppercase;
      font-size: $font-size-sm;
      margin-bottom: 0;
    }
  }
</style>
