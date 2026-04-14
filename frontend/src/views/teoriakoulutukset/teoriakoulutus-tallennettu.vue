<template>
  <div class="teoriakoulutus-tallennettu">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1 class="text-center mb-4">
            {{ $t('teoriakoulutus-tallennettu') }}
          </h1>
          <p v-if="!account.impersonated" class="text-center">
            {{ $t('teoriakoulutus-tallennettu-kuvaus') }}
          </p>
          <div class="d-flex justify-content-center">
            <elsa-button variant="primary" class="mr-3" :to="{ name: 'teoriakoulutukset' }">
              {{ $t('palaa-teoriakoulutuksiin') }}
            </elsa-button>
            <elsa-button
              v-if="!account.impersonated"
              variant="primary"
              :to="{
                name: 'uusi-paivittainen-merkinta',
                params: { teoriakoulutusId }
              }"
            >
              {{ $t('lisaa-paivittainen-merkinta') }}
            </elsa-button>
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import store from '@/store'

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class TeoriakoulutusTallennettu extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('teoriakoulutukset'),
        to: { name: 'teoriakoulutukset' }
      },
      {
        text: this.$t('lisaa-teoriakoulutus'),
        active: true
      }
    ]

    get teoriakoulutusId() {
      return this.$route.params.teoriakoulutusId
    }

    get account() {
      return store.getters['auth/account']
    }
  }
</script>

<style lang="scss" scoped>
  .teoriakoulutus-tallennettu {
    max-width: 1024px;
  }
</style>
