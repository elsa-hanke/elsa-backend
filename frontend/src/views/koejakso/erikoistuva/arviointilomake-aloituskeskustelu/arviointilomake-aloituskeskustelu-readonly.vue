<template>
  <div>
    <b-row>
      <b-col>
        <h5>{{ $t('sahkopostiosoite') }}</h5>
        <p>{{ data.erikoistuvanSahkoposti }}</p>
      </b-col>
    </b-row>
    <b-row>
      <b-col>
        <h5>{{ $t('koejakson-suorituspaikka') }}</h5>
        <p>{{ data.koejaksonSuorituspaikka }}</p>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="4">
        <h5>{{ $t('koejakson-alkamispäivä') }}</h5>
        <p>{{ data.koejaksonAlkamispaiva ? $date(data.koejaksonAlkamispaiva) : '' }}</p>
      </b-col>
      <b-col lg="4">
        <h5>{{ $t('koejakson-päättymispäivä') }}</h5>
        <p>{{ data.koejaksonPaattymispaiva ? $date(data.koejaksonPaattymispaiva) : '' }}</p>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="8">
        <h5>{{ $t('koejakso-suoritettu-kokoaikatyössä') }}</h5>
        <p v-if="data.suoritettuKokoaikatyossa">{{ $t('kylla') }}</p>
        <p v-else>
          {{ $t('suoritetaan-osa-aikatyossa-tuntia-viikossa', { tyotunnitViikossa }) }}
        </p>
      </b-col>
    </b-row>
    <hr />
    <koulutuspaikan-arvioijat
      :lahikouluttaja="data.lahikouluttaja"
      :lahiesimies="data.lahiesimies"
      :is-readonly="true"
    />
    <hr />
    <b-row>
      <b-col>
        <h3>{{ $t('aloituskeskustelu-tavoitteet') }}</h3>
        <h5>{{ $t('koejakso-osaamistavoitteet') }}</h5>
        <p>{{ data.koejaksonOsaamistavoitteet }}</p>
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
  import KoulutuspaikanArvioijat from '@/components/koejakson-vaiheet/koulutuspaikan-arvioijat.vue'
  import { AloituskeskusteluLomake, KoejaksonVaiheHyvaksynta } from '@/types'
  import * as hyvaksynnatHelper from '@/utils/koejaksonVaiheHyvaksyntaMapper'

  @Component({
    components: {
      KoulutuspaikanArvioijat,
      KoejaksonVaiheHyvaksynnat
    }
  })
  export default class ArviointilomakeAloituskeskusteluReadonly extends Vue {
    @Prop({ required: true, default: {} })
    data!: AloituskeskusteluLomake

    get tyotunnitViikossa() {
      return this.data?.tyotunnitViikossa?.toString().replace('.', ',')
    }

    get hyvaksynnat(): KoejaksonVaiheHyvaksynta[] {
      const hyvaksyntaErikoistuva = hyvaksynnatHelper.mapHyvaksyntaErikoistuva(
        this,
        this.data.erikoistuvanNimi,
        this.data.erikoistuvanKuittausaika
      ) as KoejaksonVaiheHyvaksynta
      const hyvaksyntaLahikouluttaja = hyvaksynnatHelper.mapHyvaksyntaLahikouluttaja(
        this,
        this.data.lahikouluttaja
      )
      const hyvaksyntaLahiesimies = hyvaksynnatHelper.mapHyvaksyntaLahiesimies(
        this,
        this.data.lahiesimies
      )

      return [hyvaksyntaErikoistuva, hyvaksyntaLahikouluttaja, hyvaksyntaLahiesimies].filter(
        (a): a is KoejaksonVaiheHyvaksynta => a !== null
      )
    }
  }
</script>
