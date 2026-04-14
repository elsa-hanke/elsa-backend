<template>
  <div>
    <b-row>
      <b-col lg="8">
        <h5>{{ $t('opinto-oikeuden-alkamispäivä') }}</h5>
        <p>
          {{ data.opintooikeudenMyontamispaiva ? $date(data.opintooikeudenMyontamispaiva) : '' }}
        </p>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="8">
        <h5>{{ $t('koejakson-alkamispäivä') }}</h5>
        <p>{{ data.koejaksonAlkamispaiva ? $date(data.koejaksonAlkamispaiva) : '' }}</p>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="4">
        <h5>{{ $t('sahkopostiosoite') }}</h5>
        <p>{{ data.erikoistuvanSahkoposti }}</p>
      </b-col>
      <b-col lg="4">
        <h5>{{ $t('matkapuhelinnumero') }}</h5>
        <p>{{ data.erikoistuvanPuhelinnumero }}</p>
      </b-col>
    </b-row>
    <hr />
    <b-row>
      <b-col lg="8">
        <h3>{{ $t('koulutuspaikan-tiedot') }}</h3>
        <div v-for="(koulutuspaikka, index) in data.koulutuspaikat" :key="index">
          <h5>{{ $t('toimipaikan-nimi') }}</h5>
          <p>{{ koulutuspaikka.nimi }}</p>
          <h5>{{ $t('toimipaikalla-koulutussopimus.header') }}</h5>
          <p v-if="!koulutuspaikka.yliopisto">{{ $t('kylla') }}</p>
          <p v-else>
            {{ $t('toimipaikalla-koulutussopimus.ei-sopimusta') }}:
            {{ $t(`yliopisto-nimi.${koulutuspaikka.yliopisto}`) }}
          </p>
        </div>
      </b-col>
    </b-row>
    <hr />
    <b-row>
      <b-col lg="8">
        <h3>{{ $t('koulutuspaikan-lahikouluttaja') }}</h3>
        <div v-for="(kouluttaja, index) in data.kouluttajat" :key="index">
          <h5>{{ $t('lahikouluttaja') }}</h5>
          <p>{{ kouluttaja.nimi }}</p>
        </div>
      </b-col>
    </b-row>
    <hr />
    <b-row v-if="data.vastuuhenkilo">
      <b-col lg="8">
        <h3>{{ $t('erikoisala-vastuuhenkilö') }}</h3>
        <h5>{{ $t('erikoisala-vastuuhenkilö-label') }}</h5>
        <p>
          {{ data.vastuuhenkilo.nimi }}
          {{ data.vastuuhenkilo.nimike ? ', ' + data.vastuuhenkilo.nimike : '' }}
        </p>
      </b-col>
    </b-row>
    <hr />
    <koejakson-vaihe-hyvaksynnat :hyvaksynnat="hyvaksynnat" title="hyvaksymispaivamaarat" />
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import KoejaksonVaiheHyvaksynnat from '@/components/koejakson-vaiheet/koejakson-vaihe-hyvaksynnat.vue'
  import { KoulutussopimusLomake, KoejaksonVaiheHyvaksynta } from '@/types'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'

  @Component({
    components: {
      KoejaksonVaiheHyvaksynnat
    }
  })
  export default class KoulutussopimusReadonly extends Vue {
    @Prop({ required: true, default: {} })
    data!: KoulutussopimusLomake

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.data.erikoistuvanNimi,
        this.data.erikoistuvanAllekirjoitusaika
      ) as KoejaksonVaiheHyvaksynta
      const hyvaksynnatKouluttajat = hyvaksynnatHelper.mapHyvaksynnatSopimuksenKouluttajat(
        this.data.kouluttajat
      ) as KoejaksonVaiheHyvaksynta[]
      const hyvaksyntaVastuuhenkilo = hyvaksynnatHelper.mapHyvaksyntaVastuuhenkilo(
        this.data.vastuuhenkilo
      ) as KoejaksonVaiheHyvaksynta

      return [hyvaksyntaErikoistuva, ...hyvaksynnatKouluttajat, hyvaksyntaVastuuhenkilo].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }
  }
</script>
