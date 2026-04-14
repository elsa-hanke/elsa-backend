import { Component, Mixins, Prop } from 'vue-property-decorator'

import KayttajahallintaMixin from './kayttajahallinta'

import {
  SortByEnum,
  Erikoisala,
  KayttajahallintaRajaimet,
  KayttajahallintaKayttajaListItem,
  Page
} from '@/types'
import { KayttajaJarjestys } from '@/utils/constants'

@Component({})
export default class KayttajahallintaListMixin extends Mixins(KayttajahallintaMixin) {
  @Prop({ required: true, type: Object })
  rajaimet!: KayttajahallintaRajaimet

  fields = [
    {
      key: 'nimi',
      label: this.$t('nimi'),
      sortable: false
    },
    {
      key: 'yliopistotAndErikoisalat',
      label: this.$t('yliopisto-ja-erikoisala'),
      sortable: false
    },
    {
      key: 'tila',
      label: this.$t('tilin-tila'),
      sortable: false
    }
  ]

  sortFields: SortByEnum[] = [
    {
      name: this.$t('sukunimi-a-o'),
      value: KayttajaJarjestys.SUKUNIMI_ASC
    } as SortByEnum,
    {
      name: this.$t('sukunimi-o-a'),
      value: KayttajaJarjestys.SUKUNIMI_DESC
    } as SortByEnum
  ]
  sortBy = this.sortFields[0]

  loading = false
  currentPage = 1
  perPage = 20
  debounce?: number
  hakutermi = ''
  filtered: {
    nimi: string | null
    erikoisala: Erikoisala | null
    useaOpintooikeus: boolean
    sortBy: string | null
  } = {
    nimi: null,
    erikoisala: null,
    useaOpintooikeus: false,
    sortBy: null
  }

  kayttajat: Page<KayttajahallintaKayttajaListItem> | null = null
}
