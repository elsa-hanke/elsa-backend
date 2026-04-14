<template>
  <div>
    <div v-for="(sp, index) in tyoskentelyjaksot" :key="index">
      <b-row>
        <b-col>
          <h3>{{ $t('koejakson-suorituspaikka') }}</h3>
        </b-col>
      </b-row>
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
        <b-col>
          <h5>{{ $t('tyoaika-taydesta-tyopaivasta-prosentti') }}</h5>
          <p>{{ sp.osaaikaprosentti }}%</p>
        </b-col>
      </b-row>
      <b-row>
        <b-col lg="4">
          <h5>{{ $t('alkamispaiva') }}</h5>
          <p>{{ $date(sp.alkamispaiva) }}</p>
        </b-col>
        <b-col lg="4">
          <h5>{{ $t('paattymispaiva') }}</h5>
          <p>{{ $date(sp.alkamispaiva) }}</p>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('liitetiedostot') }}</h5>
          <asiakirjat-content
            :asiakirjat="sp.asiakirjat"
            :sorting-enabled="false"
            :pagination-enabled="false"
            :enable-search="false"
            :enable-delete="false"
            :no-results-info-text="$t('ei-liitetiedostoja')"
          />
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <h5>{{ $t('kaytannon-koulutus') }}</h5>
          <p>
            {{ displayKaytannonKoulutus(sp.kaytannonKoulutus) }}
            {{ sp.omaaErikoisalaaTukeva ? ': ' + sp.omaaErikoisalaaTukeva.nimi : '' }}
          </p>
        </b-col>
      </b-row>
      <b-row v-if="sp.hyvaksyttyAiempaanErikoisalaan">
        <b-col>
          <h5>{{ $t('lisatiedot') }}</h5>
          <p>{{ $t('tyoskentelyjakso-on-aiemmin-hyvaksytty-toiselle-erikoisalalle') }}</p>
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

  import ElsaBarChart from '@/components/bar-chart/bar-chart.vue'
  import { Tyoskentelyjakso } from '@/types'
  import { KaytannonKoulutusTyyppi, TyoskentelyjaksoTyyppi } from '@/utils/constants'
  import {
    tyoskentelyjaksoKaytannonKoulutusLabel,
    tyoskentelypaikkaTyyppiLabel
  } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      ElsaBarChart
    }
  })
  export default class TyoskentelyjaksotList extends Vue {
    @Prop({ required: true, type: Array, default: () => [] })
    tyoskentelyjaksot!: Tyoskentelyjakso[]

    displayTyoskentelypaikkaTyyppiLabel(muu: string | null, tyyppi: TyoskentelyjaksoTyyppi) {
      return muu ? muu : tyoskentelypaikkaTyyppiLabel(this, tyyppi)
    }

    displayKaytannonKoulutus(value: KaytannonKoulutusTyyppi) {
      return tyoskentelyjaksoKaytannonKoulutusLabel(this, value)
    }
  }
</script>
