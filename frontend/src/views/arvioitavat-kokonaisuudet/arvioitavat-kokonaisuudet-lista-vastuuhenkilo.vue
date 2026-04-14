<template>
  <div>
    <b-row v-for="(ak, index) in arvioitavatKokonaisuudet" :key="index" lg>
      <b-col>
        <elsa-accordian :ref="ak.nimi" :visible="false">
          <template #title>
            {{ locale == 'sv' ? ak.nimiSv : ak.nimi }}
            <span v-if="!voimassaolevat">
              ({{ $date(ak.voimassaoloAlkaa) }} - {{ $date(ak.voimassaoloLoppuu) }})
            </span>
          </template>
          <div class="mt-3 mb-3">
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p v-html="locale == 'sv' ? ak.kuvausSv : ak.kuvaus"></p>
          </div>
        </elsa-accordian>
      </b-col>
    </b-row>
    <elsa-pagination
      v-if="!loading"
      :current-page.sync="currentPage"
      :per-page="20"
      :rows="rows"
      :style="{ 'max-width': '1420px' }"
      @update:currentPage="onPageInput"
    />
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Component, Vue, Prop, Watch } from 'vue-property-decorator'

  import ElsaAccordian from '@/components/accordian/accordian.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import { ArvioitavaKokonaisuus, Page } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaAccordian,
      ElsaPagination
    }
  })
  export default class ArvioitavatKokonaisuudetListaVastuuhenkilo extends Vue {
    @Prop({ required: true, type: String })
    url!: string

    @Prop({ required: true, type: String })
    locale!: string

    @Prop({ required: false, type: Number })
    erikoisala!: number

    @Prop({ required: false, type: Boolean, default: true })
    voimassaolevat!: boolean

    loading = false
    arvioitavatKokonaisuudetPageable: Page<ArvioitavaKokonaisuus> | null = null
    arvioitavatKokonaisuudet: ArvioitavaKokonaisuus[] = []
    currentPage = 1
    rows = 0

    async mounted() {
      await this.fetch()
    }

    async fetch() {
      try {
        this.arvioitavatKokonaisuudetPageable = (
          await axios.get<Page<ArvioitavaKokonaisuus>>(this.url, {
            params: {
              page: this.currentPage - 1,
              size: 20,
              sort: this.voimassaolevat ? 'nimi,asc' : ['nimi,asc', 'paivamaara,desc'],
              erikoisalaId: this.erikoisala,
              voimassaolevat: this.voimassaolevat
            }
          })
        ).data
        this.arvioitavatKokonaisuudet = this.arvioitavatKokonaisuudetPageable.content
        this.rows = this.arvioitavatKokonaisuudetPageable.page.totalElements
        this.loading = false
      } catch {
        toastFail(this, this.$t('arvioitavien-kokonaisuuksien-hakeminen-epaonnistui'))
        this.loading = false
      }
    }

    @Watch('erikoisala')
    onErikoisalaChanged() {
      this.currentPage = 1
      this.fetch()
    }

    onPageInput(value: number) {
      this.currentPage = value
      this.fetch()
    }
  }
</script>
