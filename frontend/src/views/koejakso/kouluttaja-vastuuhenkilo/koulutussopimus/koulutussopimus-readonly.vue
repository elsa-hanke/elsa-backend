<template>
  <div>
    <b-row class="mb-3">
      <b-col>
        <h5>{{ $t('lahikouluttajan-nimi') }}</h5>
        <p>{{ kouluttaja.nimi }}</p>
      </b-col>
    </b-row>
    <b-row>
      <b-col>
        <h5>{{ $t('lahikouluttajan-nimike') }}</h5>
        <p>{{ kouluttaja.nimike }}</p>
      </b-col>
    </b-row>

    <b-row>
      <b-col lg="4">
        <h5>{{ $t('toimipaikka') }}</h5>
        <p>{{ kouluttaja.toimipaikka }}</p>
      </b-col>
    </b-row>

    <b-row>
      <b-col lg="4">
        <h5>{{ $t('lahiosoite') }}</h5>
        <p>{{ kouluttaja.lahiosoite }}</p>
      </b-col>

      <b-col lg="4">
        <h5>{{ $t('postitoimipaikka') }}</h5>
        <p>{{ kouluttaja.postitoimipaikka }}</p>
      </b-col>
    </b-row>

    <b-row>
      <b-col lg="4">
        <h5>{{ $t('sahkopostiosoite') }}</h5>
        <p>{{ kouluttaja.sahkoposti }}</p>
      </b-col>

      <b-col lg="4">
        <h5>{{ $t('matkapuhelinnumero') }}</h5>
        <p>{{ kouluttaja.puhelin }}</p>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import { Kouluttaja } from '@/types'

  @Component({})
  export default class KoulutussopimusReadonly extends Vue {
    @Prop({ required: true, default: {} })
    kouluttaja!: Kouluttaja
  }
</script>
