<template>
  <div>
    <div v-for="(sp, index) in tyoskentelyjaksot" :key="index">
      <b-row>
        <b-col>
          <h5>{{ $t('tyyppi') }}</h5>
          <p>
            {{
              displayTyoskentelypaikkaTyyppiLabel(
                sp.tyoskentelypaikka.muuTyyppi,
                sp.tyoskentelypaikka.tyyppi
              )
            }}
          </p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('tyoskentelypaikka') }}</h5>
          <p>{{ sp.tyoskentelypaikka.nimi }}</p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('kunta') }}</h5>
          <p>{{ sp.tyoskentelypaikka.kunta.abbreviation }}</p>
        </b-col>
      </b-row>
      <b-row>
        <b-col lg="4">
          <h5>{{ $t('alkamispaiva') }}</h5>
          <p>{{ $date(sp.alkamispaiva) }}</p>
        </b-col>
        <b-col lg="4">
          <h5>{{ $t('paattymispaiva') }}</h5>
          <p v-if="sp.paattymispaiva">{{ $date(sp.paattymispaiva) }}</p>
          <p v-else>Kesken</p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('tyoaika-taydesta-tyopaivasta-prosentti') }}</h5>
          <p>{{ sp.osaaikaprosentti }}%</p>
        </b-col>
      </b-row>
      <b-row v-if="sp.asiakirjat != null && sp.asiakirjat.length > 0">
        <b-col>
          <h5>{{ $t('liitetiedostot') }}</h5>
          <asiakirjat-content
            :asiakirjat="sp.asiakirjat"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :enable-delete="false"
            :no-results-info-text="$t('ei-liitetiedostoja')"
            :asiakirja-data-endpoint-url="asiakirjaDataEndpointUrl"
          />
        </b-col>
      </b-row>
      <b-row v-if="sp.poissaolot != null && sp.poissaolot.length > 0">
        <b-col>
          <h5>{{ $t('poissaolot') }}</h5>
          <elsa-poissaolot-display :poissaolot="sp.poissaolot" />
        </b-col>
      </b-row>
      <hr />
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import AsiakirjatContent from '../asiakirjat/asiakirjat-content.vue'
  import ElsaPoissaolotDisplay from '../poissaolot-display/poissaolot-display.vue'

  import { Tyoskentelyjakso } from '@/types'
  import { KaytannonKoulutusTyyppi, TyoskentelyjaksoTyyppi } from '@/utils/constants'
  import {
    tyoskentelyjaksoKaytannonKoulutusLabel,
    tyoskentelypaikkaTyyppiLabel
  } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      AsiakirjatContent,
      ElsaPoissaolotDisplay
    }
  })
  export default class YekTyoskentelyjaksotList extends Vue {
    @Prop({ required: true, type: Array, default: () => [] })
    tyoskentelyjaksot!: Tyoskentelyjakso[]

    @Prop({ required: false, default: '' })
    asiakirjaDataEndpointUrl!: string

    displayTyoskentelypaikkaTyyppiLabel(muu: string | null, tyyppi: TyoskentelyjaksoTyyppi) {
      return muu ? muu : tyoskentelypaikkaTyyppiLabel(this, tyyppi)
    }

    displayKaytannonKoulutus(value: KaytannonKoulutusTyyppi) {
      return tyoskentelyjaksoKaytannonKoulutusLabel(this, value)
    }
  }
</script>
