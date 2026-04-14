<template>
  <div>
    <p class="mb-3">
      {{ $t('valitse-listasta-poissaolon-syy') }}
    </p>
    <h4 class="mb-0">
      {{ $t('poissaolon-syyt-enintaan-30-pv.otsikko') }}
    </h4>
    <ol class="pt-0 pb-0">
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.sairausloma-palkallinen') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.tilapainen-hoitovapaa') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.pakottavat-perhesyyt') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.vanhempainvapaa') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.opintovapaa') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.opiskelu') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.lukuloma') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.kertausharjoitukset') }}</li>
      <li>{{ $t('poissaolon-syyt-enintaan-30-pv.lomautukset') }}</li>
    </ol>
    <h4 class="mb-0">
      {{ $t('poissaolon-syyt-vahennetaan-suoraan.otsikko') }}
    </h4>
    <ol class="pt-0 pb-0">
      <li>{{ $t('poissaolon-syyt-vahennetaan-suoraan.yksityisasia') }}</li>
      <li>{{ $t('poissaolon-syyt-vahennetaan-suoraan.virkavapaus') }}</li>
      <li>{{ $t('poissaolon-syyt-vahennetaan-suoraan.sairausloma-palkaton') }}</li>
      <li>{{ $t('poissaolon-syyt-vahennetaan-suoraan.hoitovapaa') }}</li>
      <li>{{ $t('poissaolon-syyt-vahennetaan-suoraan.vuorotteluvapaa') }}</li>
      <li>{{ $t('poissaolon-syyt-vahennetaan-suoraan.sopeuttamisvapaa') }}</li>
    </ol>
    <p class="mb-3">
      {{ $t('koulutusjaksoista-on-laskelmia-varten-esitettava-tyotodistukset-kuvaus') }}
    </p>
    <h4 class="mb-0">
      {{ $t('keskeytyspaiviksi-ei-lasketa.otsikko') }}
    </h4>
    <ol class="pt-0 pb-0">
      <li>{{ $t('keskeytyspaiviksi-ei-lasketa.vuosilomapaivat') }}</li>
      <li>{{ $t('keskeytyspaiviksi-ei-lasketa.lomarahavapaat') }}</li>
      <li>{{ $t('keskeytyspaiviksi-ei-lasketa.saastovapaat-palkalliset') }}</li>
      <li>{{ $t('keskeytyspaiviksi-ei-lasketa.aktiivivapaat') }}</li>
      <li>{{ $t('keskeytyspaiviksi-ei-lasketa.paivystysvapaat') }}</li>
      <li>{{ $t('keskeytyspaiviksi-ei-lasketa.taydennyskoulutus') }}</li>
      <li>{{ $t('keskeytyspaiviksi-ei-lasketa.ulkomainen-koulutus') }}</li>
    </ol>
    <p class="mb-2">{{ $t('jos-aktiivivapaiden-maara-poikkeuksellisen-suuri-kuvaus') }}</p>
    <p class="pb-2 mb-0">
      {{ $t('tarvittaessa-vastuuhenkilo-voi-maarata-lisaa-koulutusta-kuvaus') }}
    </p>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  @Component({})
  export default class ElsaPoissaolonSyyt extends Vue {}
</script>

<style lang="scss" scoped>
  ol {
    padding: 1rem;
  }
</style>
