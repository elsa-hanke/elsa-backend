<template>
  <div class="koejakso-tavoitteet">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col lg="8">
          <h1>{{ $t('koejakso-tavoitteet') }}</h1>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <p v-html="$t('koejakso-tavoitteet-sisalto.kuvaus1')" />
          <h3>{{ $t('koejakso-tavoitteet-sisalto.lista1.otsikko') }}</h3>
          <ul>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item1') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item2') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item3') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item4') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item5') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item6') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item7') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista1.item8') }}</li>
          </ul>
          <h3>{{ $t('koejakso-tavoitteet-sisalto.lista2.otsikko') }}</h3>
          <ul>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista2.item1') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista2.item2') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista2.item3') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista2.item4') }}</li>
            <li>{{ $t('koejakso-tavoitteet-sisalto.lista2.item5') }}</li>
          </ul>
          <p>{{ $t('koejakso-tavoitteet-sisalto.kuvaus2') }}</p>
          <p>{{ $t('koejakso-tavoitteet-sisalto.kuvaus3') }}</p>
          <p>{{ $t('koejakso-tavoitteet-sisalto.kuvaus4') }}</p>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Vue, Component } from 'vue-property-decorator'

  @Component({})
  export default class KoejaksoTavoitteet extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koejakso'),
        to: { name: 'koejakso' }
      },
      {
        text: this.$t('koejakso-tavoitteet'),
        active: true
      }
    ]
  }
</script>
