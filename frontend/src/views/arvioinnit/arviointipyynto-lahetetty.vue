<template>
  <div class="arviointipyynto-lahetetty">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1 class="text-center mb-4">
            {{ $t('arviointipyynto-lahetetty') }}
          </h1>
          <p class="text-center">
            {{ $t('arviointipyynto-lahetetty-ilmoitus') }}
          </p>
          <p class="text-center">
            {{ $t('arviointipyynto-lahetetty-itsearviointi-muistutus') }}
          </p>
          <div class="d-flex justify-content-center">
            <elsa-button variant="primary" class="mr-3" :to="{ name: 'arvioinnit' }">
              {{ $t('palaa-arviointeihin') }}
            </elsa-button>
            <elsa-button v-if="itsearviointiLinkki" variant="primary" :to="itsearviointiLinkki">
              {{ $t('tee-tapahtumasta-itsearviointi') }}
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

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class ArviointipyyntoLahetetty extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arvioinnit'),
        to: { name: 'arvioinnit' }
      },
      {
        text: this.$t('arviointipyynto-lahetetty'),
        active: true
      }
    ]

    mounted() {
      if (this.$router && !(this.$route && this.$route.params && this.$route.params.arviointiId)) {
        this.$router.replace({ name: 'arvioinnit' })
      }
    }

    get itsearviointiLinkki() {
      if (this.$route && this.$route.params && this.$route.params.arviointiId) {
        return {
          name: 'itsearviointi',
          params: { arviointiId: this.$route.params.arviointiId }
        }
      } else {
        return false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .arviointipyynto-lahetetty {
    max-width: 1024px;
  }
</style>
